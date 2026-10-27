//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
//
//    This file is part of KylGis POS.
package com.mx.kylgis.pos.node;

import java.util.regex.Pattern;

/**
 * Validates the stable identity used by a KylGis node.
 *
 * <p>The identity is persisted in bootstrap/configuration file names and is
 * exchanged by the remote provisioning protocol. Restricting it to a small
 * portable alphabet keeps the same configuration valid on Linux, Windows and
 * other supported filesystems and prevents path traversal through node.id.</p>
 */
public final class NodeIdentity {

    private static final int MAX_LENGTH = 64;
    private static final Pattern SAFE = Pattern.compile(
            "[A-Za-z0-9](?:[A-Za-z0-9._-]{0,62}[A-Za-z0-9])?");

    private NodeIdentity() {
    }

    /**
     * Returns null for an absent value and rejects a non-empty unsafe value.
     */
    public static String optional(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : require(normalized);
    }

    /**
     * Returns a normalized node id or throws a descriptive exception.
     */
    public static String require(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException("node.id is required");
        }
        if (normalized.length() > MAX_LENGTH
                || ".".equals(normalized)
                || "..".equals(normalized)
                || !SAFE.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "Invalid node.id; use 1-64 letters, numbers, '.', '_' or '-' "
                    + "and do not start or end with punctuation: " + normalized);
        }
        return normalized;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
