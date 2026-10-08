//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.printer.service;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Small versioned binary protocol; deliberately avoids Java object deserialization. */
final class PrintProtocol {

    static final int MAGIC = 0x4B475052; // KGPR
    static final int VERSION = 1;
    static final int MAX_COMMANDS = 10000;
    static final int MAX_STRING_BYTES = 65535;
    static final int MAX_BINARY_BYTES = 4 * 1024 * 1024;
    static final int MAX_JOB_BYTES = 8 * 1024 * 1024;

    private PrintProtocol() {
    }

    static void writeRequest(DataOutputStream out, String serviceId, String token,
            String printerIndex, List<PrintCommand> commands) throws IOException {
        if (commands == null || commands.isEmpty() || commands.size() > MAX_COMMANDS) {
            throw new IOException("Invalid print command count");
        }
        out.writeInt(MAGIC);
        out.writeInt(VERSION);
        writeString(out, serviceId);
        writeString(out, token);
        writeString(out, printerIndex);
        int estimated = utf8Length(serviceId) + utf8Length(token) + utf8Length(printerIndex) + 64;
        for (PrintCommand command : commands) {
            estimated += commandSize(command);
            if (estimated > MAX_JOB_BYTES) throw new IOException("Print job payload too large");
        }
        out.writeInt(commands.size());
        for (PrintCommand command : commands) writeCommand(out, command);
        out.flush();
    }

    static Request readRequest(DataInputStream in) throws IOException {
        int magic = in.readInt();
        if (magic != MAGIC) throw new IOException("Invalid KylGis print protocol magic");
        int version = in.readInt();
        if (version != VERSION) throw new IOException("Unsupported print protocol version: " + version);
        String serviceId = readString(in);
        String token = readString(in);
        String printerIndex = readString(in);
        int count = in.readInt();
        if (count <= 0 || count > MAX_COMMANDS) throw new IOException("Invalid command count: " + count);
        List<PrintCommand> commands = new ArrayList<>(count);
        int estimated = utf8Length(serviceId) + utf8Length(token) + utf8Length(printerIndex) + 64;
        for (int i = 0; i < count; i++) {
            PrintCommand command = readCommand(in);
            estimated += commandSize(command);
            if (estimated > MAX_JOB_BYTES) throw new IOException("Print job payload too large");
            commands.add(command);
        }
        return new Request(serviceId, token, printerIndex, commands);
    }

    static void writeResponse(DataOutputStream out, boolean ok, String message) throws IOException {
        out.writeInt(MAGIC);
        out.writeInt(VERSION);
        out.writeBoolean(ok);
        writeString(out, message == null ? "" : message);
        out.flush();
    }

    static Response readResponse(DataInputStream in) throws IOException {
        if (in.readInt() != MAGIC) throw new IOException("Invalid print response magic");
        int version = in.readInt();
        if (version != VERSION) throw new IOException("Unsupported response version: " + version);
        return new Response(in.readBoolean(), readString(in));
    }

    private static void writeCommand(DataOutputStream out, PrintCommand command) throws IOException {
        out.writeByte(command.getType().ordinal());
        out.writeInt(command.getIntValue());
        writeNullableString(out, command.getValue1());
        writeNullableString(out, command.getValue2());
        writeNullableString(out, command.getValue3());
        byte[] binary = command.getBinary();
        if (binary == null) {
            out.writeInt(-1);
        } else {
            if (binary.length > MAX_BINARY_BYTES) throw new IOException("Print binary payload too large");
            out.writeInt(binary.length);
            out.write(binary);
        }
    }

    private static PrintCommand readCommand(DataInputStream in) throws IOException {
        int ordinal = in.readUnsignedByte();
        PrintCommand.Type[] values = PrintCommand.Type.values();
        if (ordinal >= values.length) throw new IOException("Unknown print command: " + ordinal);
        PrintCommand.Type type = values[ordinal];
        int intValue = in.readInt();
        String value1 = readNullableString(in);
        String value2 = readNullableString(in);
        String value3 = readNullableString(in);
        int length = in.readInt();
        byte[] binary = null;
        if (length >= 0) {
            if (length > MAX_BINARY_BYTES) throw new IOException("Print binary payload too large: " + length);
            binary = new byte[length];
            in.readFully(binary);
        }
        return PrintCommand.raw(type, intValue, value1, value2, value3, binary);
    }

    private static int commandSize(PrintCommand command) {
        int size = 32 + utf8Length(command.getValue1())
                + utf8Length(command.getValue2()) + utf8Length(command.getValue3());
        byte[] binary = command.getBinary();
        return size + (binary == null ? 0 : binary.length);
    }

    private static int utf8Length(String value) {
        return value == null ? 0 : value.getBytes(StandardCharsets.UTF_8).length;
    }

    private static void writeNullableString(DataOutputStream out, String value) throws IOException {
        out.writeBoolean(value != null);
        if (value != null) writeString(out, value);
    }

    private static String readNullableString(DataInputStream in) throws IOException {
        return in.readBoolean() ? readString(in) : null;
    }

    private static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_STRING_BYTES) throw new IOException("String payload too large");
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static String readString(DataInputStream in) throws IOException {
        int length;
        try {
            length = in.readInt();
        } catch (EOFException ex) {
            throw new IOException("Unexpected end of print request", ex);
        }
        if (length < 0 || length > MAX_STRING_BYTES) throw new IOException("Invalid string length: " + length);
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    static final class Request {
        final String serviceId;
        final String token;
        final String printerIndex;
        final List<PrintCommand> commands;
        Request(String serviceId, String token, String printerIndex, List<PrintCommand> commands) {
            this.serviceId = serviceId;
            this.token = token;
            this.printerIndex = printerIndex;
            this.commands = commands;
        }
    }

    static final class Response {
        final boolean ok;
        final String message;
        Response(boolean ok, String message) { this.ok = ok; this.message = message; }
    }
}
