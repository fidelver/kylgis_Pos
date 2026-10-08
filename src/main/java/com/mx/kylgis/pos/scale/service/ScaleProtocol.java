//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.scale.service;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Tiny versioned protocol for one remote scale reading. */
final class ScaleProtocol {
    static final int MAGIC = 0x4B475343; // KGSC
    static final int VERSION = 1;
    static final int MAX_STRING_BYTES = 4096;

    private ScaleProtocol() { }

    static void writeRequest(DataOutputStream out, String serviceId, String token)
            throws IOException {
        out.writeInt(MAGIC); out.writeInt(VERSION);
        writeString(out, serviceId); writeString(out, token); out.flush();
    }

    static Request readRequest(DataInputStream in) throws IOException {
        checkHeader(in);
        return new Request(readString(in), readString(in));
    }

    static void writeResponse(DataOutputStream out, boolean ok, Double weight,
            String message) throws IOException {
        out.writeInt(MAGIC); out.writeInt(VERSION); out.writeBoolean(ok);
        out.writeBoolean(weight != null);
        if (weight != null) out.writeDouble(weight);
        writeString(out, message == null ? "" : message); out.flush();
    }

    static Response readResponse(DataInputStream in) throws IOException {
        checkHeader(in);
        boolean ok = in.readBoolean();
        Double weight = in.readBoolean() ? in.readDouble() : null;
        return new Response(ok, weight, readString(in));
    }

    private static void checkHeader(DataInputStream in) throws IOException {
        if (in.readInt() != MAGIC) throw new IOException("Invalid KylGis scale protocol magic");
        int version = in.readInt();
        if (version != VERSION) throw new IOException("Unsupported scale protocol version: " + version);
    }

    private static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] data = (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
        if (data.length > MAX_STRING_BYTES) throw new IOException("Scale protocol field too long");
        out.writeInt(data.length); out.write(data);
    }

    private static String readString(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length < 0 || length > MAX_STRING_BYTES) throw new IOException("Invalid scale protocol field length");
        byte[] data = new byte[length]; in.readFully(data);
        return new String(data, StandardCharsets.UTF_8);
    }

    static final class Request {
        final String serviceId, token;
        Request(String serviceId, String token) { this.serviceId = serviceId; this.token = token; }
    }
    static final class Response {
        final boolean ok; final Double weight; final String message;
        Response(boolean ok, Double weight, String message) {
            this.ok = ok; this.weight = weight; this.message = message;
        }
    }
}
