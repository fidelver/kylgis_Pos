//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.scale;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Parses the optional request command used by request/response scales.
 *
 * Stored values are deliberately independent from machine.scale so existing
 * model:port configurations remain backward compatible.
 */
public final class ScaleRequestCommand {

    public static final String PROPERTY = "machine.scale.requestCommand";
    public static final String AUTO = "AUTO";
    private static final int MAX_BYTES = 8;

    private ScaleRequestCommand() {
    }

    public static String normalize(String value) {
        if (value == null || value.trim().isEmpty() || AUTO.equalsIgnoreCase(value.trim())) {
            return AUTO;
        }

        String trimmed = value.trim();
        int separator = trimmed.indexOf(':');
        if (separator < 0) {
            // Friendly editable-combo shorthand: P becomes ASCII:P.
            return normalizeAscii(trimmed);
        }

        String type = trimmed.substring(0, separator).trim().toUpperCase(Locale.ROOT);
        String payload = trimmed.substring(separator + 1);
        switch (type) {
            case "ASCII":
                return normalizeAscii(payload);
            case "HEX":
                return normalizeHex(payload);
            default:
                throw new IllegalArgumentException(
                        "Use AUTO, ASCII:texto o HEX:50 0D (1 a 8 bytes).");
        }
    }

    public static byte[] resolve(String value, byte[] defaultCommand) {
        String normalized = normalize(value);
        if (AUTO.equals(normalized)) {
            if (defaultCommand == null || defaultCommand.length == 0) {
                throw new IllegalArgumentException("El controlador no define un comando predeterminado.");
            }
            return defaultCommand.clone();
        }
        if (normalized.startsWith("ASCII:")) {
            return normalized.substring("ASCII:".length()).getBytes(StandardCharsets.US_ASCII);
        }

        String[] tokens = normalized.substring("HEX:".length()).split(" ");
        byte[] result = new byte[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            result[i] = (byte) Integer.parseInt(tokens[i], 16);
        }
        return result;
    }

    private static String normalizeAscii(String payload) {
        if (payload == null || payload.length() < 1 || payload.length() > MAX_BYTES) {
            throw new IllegalArgumentException("ASCII debe contener entre 1 y 8 caracteres.");
        }
        for (int i = 0; i < payload.length(); i++) {
            char ch = payload.charAt(i);
            if (ch < 0x20 || ch > 0x7e) {
                throw new IllegalArgumentException("ASCII sólo admite caracteres imprimibles.");
            }
        }
        return "ASCII:" + payload;
    }

    private static String normalizeHex(String payload) {
        String clean = payload == null ? "" : payload.trim();
        if (clean.isEmpty()) {
            throw new IllegalArgumentException("HEX debe contener entre 1 y 8 bytes.");
        }
        String[] tokens = clean.split("\\s+");
        if (tokens.length < 1 || tokens.length > MAX_BYTES) {
            throw new IllegalArgumentException("HEX debe contener entre 1 y 8 bytes.");
        }
        StringBuilder canonical = new StringBuilder("HEX:");
        for (int i = 0; i < tokens.length; i++) {
            if (!tokens[i].matches("[0-9a-fA-F]{2}")) {
                throw new IllegalArgumentException("Cada byte HEX debe tener dos dígitos, por ejemplo HEX:50 0D.");
            }
            if (i > 0) {
                canonical.append(' ');
            }
            canonical.append(tokens[i].toUpperCase(Locale.ROOT));
        }
        return canonical.toString();
    }
}
