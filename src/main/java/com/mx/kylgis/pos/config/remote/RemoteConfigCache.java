//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.config.remote;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Properties;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Encrypted last-known-good cache for one remote-provisioned node. */
public final class RemoteConfigCache {
    private static final int MAGIC = 0x4B434348; // KCCH
    private static final short VERSION = 1;
    private static final int IV_BYTES = 12;
    private static final int MAX_PAYLOAD_BYTES = 2 * 1024 * 1024;
    private static final SecureRandom RANDOM = new SecureRandom();

    private RemoteConfigCache() { }

    public static void store(File file, String serviceId, String nodeId,
            String token, Properties properties) throws IOException {
        if (file == null) throw new IllegalArgumentException("cache file is required");
        long timestamp = System.currentTimeMillis();
        byte[] clear = serialize(properties);
        if (clear.length > MAX_PAYLOAD_BYTES) throw new IOException("Remote configuration cache payload too large");
        byte[] iv = new byte[IV_BYTES];
        RANDOM.nextBytes(iv);
        byte[] encrypted = crypt(Cipher.ENCRYPT_MODE, token, iv,
                aad(serviceId, nodeId, timestamp), clear);

        File parent = file.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Cannot create remote cache directory: " + parent);
        }
        File temp = File.createTempFile(file.getName() + ".", ".tmp", parent);
        boolean moved = false;
        try {
            try (DataOutputStream out = new DataOutputStream(new FileOutputStream(temp))) {
                out.writeInt(MAGIC);
                out.writeShort(VERSION);
                out.writeLong(timestamp);
                out.writeInt(iv.length);
                out.write(iv);
                out.writeInt(encrypted.length);
                out.write(encrypted);
                out.flush();
            }
            restrictPermissions(temp);
            try {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            restrictPermissions(file);
            moved = true;
        } finally {
            if (!moved) temp.delete();
        }
    }

    public static CachedConfiguration load(File file, String serviceId,
            String nodeId, String token, long maxAgeMs) throws IOException {
        if (file == null || !file.isFile()) throw new IOException("Remote configuration cache not found");
        try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
            if (in.readInt() != MAGIC) throw new IOException("Invalid remote configuration cache");
            if (in.readShort() != VERSION) throw new IOException("Unsupported remote configuration cache version");
            long timestamp = in.readLong();
            long now = System.currentTimeMillis();
            if (timestamp > now + 5L * 60L * 1000L) throw new IOException("Remote configuration cache has invalid timestamp");
            if (maxAgeMs >= 0 && now - timestamp > maxAgeMs) throw new IOException("Remote configuration cache expired");
            int ivLength = in.readInt();
            if (ivLength != IV_BYTES) throw new IOException("Invalid remote configuration cache IV");
            byte[] iv = new byte[ivLength];
            in.readFully(iv);
            int length = in.readInt();
            if (length < 16 || length > MAX_PAYLOAD_BYTES + 64) throw new IOException("Invalid remote configuration cache payload length");
            byte[] encrypted = new byte[length];
            in.readFully(encrypted);
            if (in.read() != -1) throw new IOException("Trailing data in remote configuration cache");
            byte[] clear = crypt(Cipher.DECRYPT_MODE, token, iv,
                    aad(serviceId, nodeId, timestamp), encrypted);
            if (clear.length > MAX_PAYLOAD_BYTES) throw new IOException("Remote configuration cache payload too large");
            Properties properties = new Properties();
            try (ByteArrayInputStream bin = new ByteArrayInputStream(clear)) {
                properties.load(bin);
            }
            return new CachedConfiguration(timestamp, properties);
        } catch (EOFException ex) {
            throw new IOException("Truncated remote configuration cache", ex);
        }
    }

    private static byte[] serialize(Properties p) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        p.store(buffer, "KylGis cached remote configuration");
        return buffer.toByteArray();
    }

    private static byte[] crypt(int mode, String token, byte[] iv,
            byte[] aad, byte[] input) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update("KylGisRemoteCacheV1".getBytes(StandardCharsets.UTF_8));
            digest.update(requiredToken(token));
            byte[] hash = digest.digest();
            byte[] key = new byte[16];
            System.arraycopy(hash, 0, key, 0, key.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(mode, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            cipher.updateAAD(aad);
            return cipher.doFinal(input);
        } catch (GeneralSecurityException ex) {
            throw new IOException("Remote configuration cache authentication failed", ex);
        }
    }

    private static byte[] aad(String serviceId, String nodeId, long timestamp) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(buffer);
        out.writeInt(MAGIC);
        out.writeShort(VERSION);
        writeString(out, serviceId);
        writeString(out, nodeId);
        out.writeLong(timestamp);
        out.flush();
        return buffer.toByteArray();
    }

    private static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] b = required(value, "cache identity").getBytes(StandardCharsets.UTF_8);
        if (b.length > 512) throw new IOException("Remote configuration cache identity too long");
        out.writeInt(b.length);
        out.write(b);
    }

    private static byte[] requiredToken(String token) throws IOException {
        String value = required(token, "remote token");
        if (value.length() < 16) throw new IOException("Remote token too short");
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static String required(String value, String label) throws IOException {
        if (value == null || value.trim().isEmpty()) throw new IOException("Missing " + label);
        return value.trim();
    }

    private static void restrictPermissions(File file) {
        file.setReadable(false, false);
        file.setWritable(false, false);
        file.setExecutable(false, false);
        file.setReadable(true, true);
        file.setWritable(true, true);
    }

    public static final class CachedConfiguration {
        private final long timestamp;
        private final Properties properties;
        CachedConfiguration(long timestamp, Properties properties) {
            this.timestamp = timestamp;
            this.properties = new Properties();
            this.properties.putAll(properties);
        }
        public long getTimestamp() { return timestamp; }
        public Properties getProperties() {
            Properties p = new Properties();
            p.putAll(properties);
            return p;
        }
    }
}
