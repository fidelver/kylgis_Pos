//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.scale.service;

import com.mx.kylgis.pos.forms.AppProperties;

/** Configuration contract shared by remote scale clients and servers. */
public final class ScaleServiceConfig {
    public static final int DEFAULT_PORT = 19102;
    public static final int DEFAULT_CONNECT_TIMEOUT_MS = 2500;
    public static final int DEFAULT_READ_TIMEOUT_MS = 5000;
    public static final String DEVICE_SERVICE_KEY = "device.scale.service";

    private ScaleServiceConfig() { }

    public static String getServiceId(AppProperties properties) {
        return trimToNull(properties == null ? null : properties.getProperty(DEVICE_SERVICE_KEY));
    }

    public static String prefix(String serviceId) {
        return "scale.service." + normalizeServiceId(serviceId) + ".";
    }

    public static Endpoint endpoint(AppProperties properties, String serviceId) {
        if (properties == null) throw new IllegalArgumentException("properties are required");
        String id = normalizeServiceId(serviceId);
        String prefix = prefix(id);
        String host = trimToNull(properties.getProperty(prefix + "host"));
        if (host == null) host = "127.0.0.1";
        int port = parseInt(properties.getProperty(prefix + "port"), DEFAULT_PORT, 1, 65535);
        int connectTimeout = parseInt(properties.getProperty(prefix + "connect.timeout.ms"),
                DEFAULT_CONNECT_TIMEOUT_MS, 100, 120000);
        int readTimeout = parseInt(properties.getProperty(prefix + "read.timeout.ms"),
                DEFAULT_READ_TIMEOUT_MS, 100, 120000);
        String token = trimToNull(properties.getProperty(prefix + "token"));
        if (token == null) throw new IllegalArgumentException("Missing " + prefix + "token");
        return new Endpoint(id, host, port, token, connectTimeout, readTimeout);
    }

    public static String bindAddress(AppProperties properties, String serviceId) {
        String value = trimToNull(properties.getProperty(prefix(serviceId) + "bind"));
        return value == null ? "127.0.0.1" : value;
    }

    private static int parseInt(String value, int fallback, int min, int max) {
        String normalized = trimToNull(value);
        if (normalized == null) return fallback;
        try {
            int parsed = Integer.parseInt(normalized);
            if (parsed < min || parsed > max) throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid numeric scale service setting: " + value, ex);
        }
    }

    private static String normalizeServiceId(String value) {
        String normalized = trimToNull(value);
        if (normalized == null || !normalized.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException("Invalid scale service id: " + value);
        }
        return normalized;
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static final class Endpoint {
        private final String serviceId, host, token;
        private final int port, connectTimeoutMs, readTimeoutMs;
        private Endpoint(String serviceId, String host, int port, String token,
                int connectTimeoutMs, int readTimeoutMs) {
            this.serviceId = serviceId; this.host = host; this.port = port;
            this.token = token; this.connectTimeoutMs = connectTimeoutMs;
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
