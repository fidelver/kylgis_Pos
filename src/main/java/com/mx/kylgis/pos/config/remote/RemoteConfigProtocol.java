//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.config.remote;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Properties;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Binary authenticated/encrypted protocol for remote node provisioning. */
public final class RemoteConfigProtocol {
    static final int MAGIC = 0x4B434647; // KCFG
    static final short VERSION = 1;
    static final int NONCE_BYTES = 16;
    static final int IV_BYTES = 12;
    static final int HMAC_BYTES = 32;
    static final int MAX_TEXT_BYTES = 512;
    static final int MAX_PAYLOAD_BYTES = 2 * 1024 * 1024;
    public static final long MAX_CLOCK_SKEW_MS = 5L * 60L * 1000L;
    private static final SecureRandom RANDOM = new SecureRandom();

    private RemoteConfigProtocol() { }

    public static Request createRequest(String serviceId, String nodeId, String token)
            throws RemoteConfigException {
        byte[] nonce = new byte[NONCE_BYTES];
        RANDOM.nextBytes(nonce);
        long timestamp = System.currentTimeMillis();
        return new Request(serviceId, nodeId, timestamp, nonce,
                hmac(token, requestAuthMaterial(serviceId, nodeId,
                        timestamp, nonce)));
    }

    public static void writeRequest(DataOutputStream out, Request r) throws IOException {
        out.writeInt(MAGIC);
        out.writeShort(VERSION);
        writeString(out, r.serviceId);
        writeString(out, r.nodeId);
        out.writeLong(r.timestamp);
        out.writeInt(r.nonce.length);
        out.write(r.nonce);
        out.writeInt(r.mac.length);
        out.write(r.mac);
        out.flush();
    }

    public static Request readRequest(DataInputStream in) throws IOException {
        checkHeader(in);
        String serviceId = readString(in, MAX_TEXT_BYTES);
        String nodeId = readString(in, MAX_TEXT_BYTES);
        validateIdentifier(serviceId, "service id");
        validateIdentifier(nodeId, "node id");
        long timestamp = in.readLong();
        byte[] nonce = readExact(in, NONCE_BYTES, NONCE_BYTES, "nonce");
        byte[] mac = readExact(in, HMAC_BYTES, HMAC_BYTES, "HMAC");
        return new Request(serviceId, nodeId, timestamp, nonce, mac);
    }

    public static boolean authenticate(Request r, String token) throws RemoteConfigException {
        byte[] expected = hmac(token,
                requestAuthMaterial(r.serviceId, r.nodeId,
                        r.timestamp, r.nonce));
        return MessageDigest.isEqual(expected, r.mac);
    }

    public static void writeSuccess(DataOutputStream out, Request request,
            String token, Properties properties)
            throws IOException, RemoteConfigException {
        byte[] clear = serialize(properties);
        if (clear.length > MAX_PAYLOAD_BYTES) {
            throw new RemoteConfigException("Configuration payload too large");
        }
        byte[] iv = new byte[IV_BYTES];
        RANDOM.nextBytes(iv);
        byte[] encrypted = crypt(Cipher.ENCRYPT_MODE, token, iv,
                responseAad(request), clear);
        out.writeInt(MAGIC);
        out.writeShort(VERSION);
        out.writeByte(1);
        out.writeInt(iv.length);
        out.write(iv);
        out.writeInt(encrypted.length);
        out.write(encrypted);
        out.flush();
    }

    public static void writeError(DataOutputStream out, String message)
            throws IOException {
        out.writeInt(MAGIC);
        out.writeShort(VERSION);
        out.writeByte(0);
        writeString(out, safeError(message));
        out.flush();
    }

    public static Properties readResponse(DataInputStream in, Request request,
            String token) throws IOException, RemoteConfigException {
        checkHeader(in);
        int status = in.readUnsignedByte();
        if (status == 0) {
            throw new RemoteConfigException(readString(in, MAX_TEXT_BYTES));
        }
        if (status != 1) {
            throw new RemoteConfigException("Invalid provisioning response status");
        }
        byte[] iv = readExact(in, IV_BYTES, IV_BYTES, "IV");
        int length = in.readInt();
        if (length < 16 || length > MAX_PAYLOAD_BYTES + 64) {
            throw new RemoteConfigException("Invalid encrypted payload length");
        }
        byte[] encrypted = new byte[length];
        in.readFully(encrypted);
        byte[] clear = crypt(Cipher.DECRYPT_MODE, token, iv,
                responseAad(request), encrypted);
        if (clear.length > MAX_PAYLOAD_BYTES) {
            throw new RemoteConfigException("Decrypted payload too large");
        }
        Properties properties = new Properties();
        try (ByteArrayInputStream input = new ByteArrayInputStream(clear)) {
            properties.load(input);
        }
        return properties;
    }

    private static byte[] serialize(Properties properties) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        properties.store(buffer, "KylGis remote node configuration");
        return buffer.toByteArray();
    }

    private static byte[] hmac(String token, byte[] material)
            throws RemoteConfigException {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(requiredToken(token), "HmacSHA256"));
            return mac.doFinal(material);
        } catch (GeneralSecurityException ex) {
            throw new RemoteConfigException(
                    "Cannot authenticate provisioning protocol", ex);
        }
    }

    private static byte[] crypt(int mode, String token, byte[] iv,
            byte[] aad, byte[] input) throws RemoteConfigException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update("KylGisConfigAES1".getBytes(StandardCharsets.UTF_8));
            digest.update(requiredToken(token));
            byte[] hash = digest.digest();
            byte[] key = new byte[16];
            System.arraycopy(hash, 0, key, 0, key.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(mode, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(128, iv));
            cipher.updateAAD(aad);
            return cipher.doFinal(input);
        } catch (GeneralSecurityException ex) {
            throw new RemoteConfigException(
                    "Provisioning payload authentication failed", ex);
        }
    }

    private static byte[] requestAuthMaterial(String serviceId, String nodeId,
            long timestamp, byte[] nonce) throws RemoteConfigException {
        return framed("REQ", serviceId, nodeId, timestamp, nonce);
    }

    private static byte[] responseAad(Request request)
            throws RemoteConfigException {
        return framed("RESP", request.serviceId, request.nodeId,
                request.timestamp, request.nonce);
    }

    private static byte[] framed(String kind, String serviceId, String nodeId,
            long timestamp, byte[] nonce) throws RemoteConfigException {
        validateIdentifier(serviceId, "service id");
        validateIdentifier(nodeId, "node id");
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(buffer);
            out.writeInt(MAGIC);
            out.writeShort(VERSION);
            writeString(out, kind);
            writeString(out, serviceId);
            writeString(out, nodeId);
            out.writeLong(timestamp);
            out.writeInt(nonce.length);
            out.write(nonce);
            out.flush();
            return buffer.toByteArray();
        } catch (IOException ex) {
            throw new RemoteConfigException(
                    "Cannot frame provisioning request", ex);
        }
    }

    private static void checkHeader(DataInputStream in) throws IOException {
        if (in.readInt() != MAGIC) {
            throw new IOException("Invalid provisioning protocol magic");
        }
        if (in.readShort() != VERSION) {
            throw new IOException("Unsupported provisioning protocol version");
        }
    }

    private static void writeString(DataOutputStream out, String value)
            throws IOException {
        byte[] bytes = (value == null ? "" : value)
                .getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_TEXT_BYTES) {
            throw new IOException("Protocol text field too long");
        }
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static String readString(DataInputStream in, int max)
            throws IOException {
        int length = in.readInt();
        if (length < 0 || length > max) {
            throw new IOException("Invalid protocol string length");
        }
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static byte[] readExact(DataInputStream in, int expected,
            int max, String label) throws IOException {
        int length = in.readInt();
        if (length != expected || length > max) {
            throw new IOException("Invalid " + label + " length");
        }
        byte[] value = new byte[length];
        in.readFully(value);
        return value;
    }

    private static byte[] requiredToken(String token)
            throws RemoteConfigException {
        if (token == null || token.length() < 16) {
            throw new RemoteConfigException(
                    "Provisioning token is missing or too short");
        }
        return token.getBytes(StandardCharsets.UTF_8);
    }

    static void validateIdentifier(String value, String label)
            throws RemoteConfigException {
        if (value == null || !value.matches("[A-Za-z0-9_.-]{1,128}")) {
            throw new RemoteConfigException("Invalid " + label);
        }
    }

    private static String safeError(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "Provisioning unavailable";
        }
        String clean = message.replace('\n', ' ').replace('\r', ' ').trim();
        return clean.length() > 240 ? clean.substring(0, 240) : clean;
    }

    public static final class Request {
        final String serviceId;
        final String nodeId;
        final long timestamp;
        final byte[] nonce;
        final byte[] mac;

        Request(String serviceId, String nodeId, long timestamp,
                byte[] nonce, byte[] mac) {
            this.serviceId = serviceId;
            this.nodeId = nodeId;
            this.timestamp = timestamp;
            this.nonce = nonce.clone();
            this.mac = mac.clone();
        }

        public String getServiceId() { return serviceId; }
        public String getNodeId() { return nodeId; }
        public long getTimestamp() { return timestamp; }
        public byte[] getNonce() { return nonce.clone(); }
    }
}
