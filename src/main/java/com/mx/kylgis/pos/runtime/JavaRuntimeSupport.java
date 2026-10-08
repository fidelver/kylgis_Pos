//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.runtime;

/** Runtime compatibility policy for KylGis POS 1.0. */
public final class JavaRuntimeSupport {
    public static final int CERTIFIED_MAJOR = 8;

    private JavaRuntimeSupport() { }

    public static int currentMajor() {
        return parseMajor(System.getProperty("java.specification.version",
                System.getProperty("java.version", "")));
    }

    public static int parseMajor(String version) {
        if (version == null) return -1;
        String value = version.trim();
        if (value.isEmpty()) return -1;
        try {
            if (value.startsWith("1.")) {
                int end = value.indexOf('.', 2);
                String major = end < 0 ? value.substring(2) : value.substring(2, end);
                return Integer.parseInt(digits(major));
            }
            int end = value.indexOf('.');
            String major = end < 0 ? value : value.substring(0, end);
            return Integer.parseInt(digits(major));
        } catch (RuntimeException ex) {
            return -1;
        }
    }

    public static String classification(int major) {
        if (major < CERTIFIED_MAJOR) return "UNSUPPORTED";
        if (major == CERTIFIED_MAJOR) return "CERTIFIED";
        return "UNVERIFIED_NEWER";
    }

    private static String digits(String value) {
        int end = 0;
        while (end < value.length() && Character.isDigit(value.charAt(end))) end++;
        if (end == 0) throw new NumberFormatException(value);
        return value.substring(0, end);
    }
}
