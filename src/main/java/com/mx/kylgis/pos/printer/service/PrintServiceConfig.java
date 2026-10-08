//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.printer.service;

import com.mx.kylgis.pos.forms.AppProperties;

/** Configuration contract shared by print-service clients and servers. */
public final class PrintServiceConfig {

    public static final int DEFAULT_PORT = 19101;
    public static final int DEFAULT_CONNECT_TIMEOUT_MS = 2500;
    public static final int DEFAULT_READ_TIMEOUT_MS = 15000;

    private PrintServiceConfig() {
    }

    public static String deviceServiceKey(String printerIndex) {
        return "device.printer." + normalizePrinterIndex(printerIndex) + ".service";
    }

    public static String deviceTargetKey(String printerIndex) {
        return deviceServiceKey(printerIndex) + ".target";
    }

    public static String servicePrefix(String serviceId) {
        return "print.service." + normalizeServiceId(serviceId) + ".";
    }

    public static String getServiceId(AppProperties properties, String printerIndex) {
        return trimToNull(properties == null ? null
                : properties.getProperty(deviceServiceKey(printerIndex)));
    }

    public static String getTargetPrinter(AppProperties properties, String printerIndex) {
        String value = trimToNull(properties == null ? null
                : properties.getProperty(deviceTargetKey(printerIndex)));
        return value == null ? normalizePrinterIndex(printerIndex)
                : normalizePrinterIndex(value);
    }

    public static Endpoint endpoint(AppProperties properties, String serviceId) {
        if (properties == null) {
            throw new IllegalArgumentException("properties are required");
        }
        String id = normalizeServiceId(serviceId);
        String prefix = servicePrefix(id);
        String host = trimToNull(properties.getProperty(prefix + "host"));
        if (host == null) {
            host = "127.0.0.1";
        }
        int port = parseInt(properties.getProperty(prefix + "port"), DEFAULT_PORT, 1, 65535);
        int connectTimeout = parseInt(properties.getProperty(prefix + "connect.timeout.ms"),
                DEFAULT_CONNECT_TIMEOUT_MS, 100, 120000);
        int readTimeout = parseInt(properties.getProperty(prefix + "read.timeout.ms"),
                DEFAULT_READ_TIMEOUT_MS, 100, 300000);
        String token = trimToNull(properties.getProperty(prefix + "token"));
        if (token == null) {
            throw new IllegalArgumentException("Missing " + prefix + "token");
        }
        return new Endpoint(id, host, port, token, connectTimeout, readTimeout);
    }

    public static String bindAddress(AppProperties properties, String serviceId) {
        String value = trimToNull(properties.getProperty(servicePrefix(serviceId) + "bind"));
        return value == null ? "127.0.0.1" : value;
    }

    public static int serverPort(AppProperties properties, String serviceId) {
        return endpoint(properties, serviceId).getPort();
    }

    public static String allowedPrinters(AppProperties properties, String serviceId) {
        String value = trimToNull(properties.getProperty(servicePrefix(serviceId) + "allowed.printers"));
        return value == null ? "1,2,3,4,5,6" : value;
    }

    public static boolean isPrinterAllowed(String configured, String printerIndex) {
        String target = normalizePrinterIndex(printerIndex);
        if (configured == null) {
            return true;
        }
        for (String raw : configured.split("[,;\\s]+")) {
            String token = trimToNull(raw);
            if (token == null) {
                continue;
            }
            if ("*".equals(token) || target.equals(token)) {
                return true;
            }
        }
        return false;
    }

    private static int parseInt(String value, int defaultValue, int min, int max) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(normalized);
            if (parsed < min || parsed > max) {
                throw new IllegalArgumentException("Value out of range: " + parsed);
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid integer: " + value, ex);
        }
    }

    private static String normalizePrinterIndex(String value) {
        String normalized = trimToNull(value);
        if (normalized == null || !normalized.matches("[1-6]")) {
            throw new IllegalArgumentException("Printer index must be between 1 and 6: " + value);
        }
        return normalized;
    }

    private static String normalizeServiceId(String value) {
        String normalized = trimToNull(value);
        if (normalized == null || !normalized.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException("Invalid print service id: " + value);
        }
        return normalized;
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static final class Endpoint {
        private final String serviceId;
        private final String host;
        private final int port;
        private final String token;
        private final int connectTimeoutMs;
        private final int readTimeoutMs;

        private Endpoint(String serviceId, String host, int port, String token,
                int connectTimeoutMs, int readTimeoutMs) {
            this.serviceId = serviceId;
            this.host = host;
            this.port = port;
            this.token = token;
            this.connectTimeoutMs = connectTimeoutMs;
            this.readTimeoutMs = readTimeoutMs;
        }
        public String getServiceId() { return serviceId; }
        public String getHost() { return host; }
        public int getPort() { return port; }
        public String getToken() { return token; }
        public int getConnectTimeoutMs() { return connectTimeoutMs; }
        public int getReadTimeoutMs() { return readTimeoutMs; }
    }
}
