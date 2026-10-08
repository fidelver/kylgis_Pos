//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.config.remote;

import com.mx.kylgis.pos.forms.AppProperties;
import java.io.File;

/** Configuration keys for the encrypted MASTER provisioning service. */
public final class ConfigServiceConfig {
    public static final String SERVICE_ID_KEY = "config.service.id";
    public static final String REMOTE_SERVICE_KEY = "config.remote.service";
    public static final String REMOTE_HOST_KEY = "config.remote.host";
    public static final String REMOTE_PORT_KEY = "config.remote.port";
    public static final String REMOTE_TOKEN_KEY = "config.remote.token";
    public static final String REMOTE_CACHE_ENABLED_KEY = "config.remote.cache.enabled";
    public static final String REMOTE_CACHE_MAXAGE_HOURS_KEY = "config.remote.cache.maxage.hours";
    public static final String REMOTE_CACHE_FILE_KEY = "config.remote.cache.file";

    private ConfigServiceConfig() { }

    public static String serviceId(AppProperties p) {
        return required(p.getProperty(SERVICE_ID_KEY), SERVICE_ID_KEY);
    }
    public static String bind(AppProperties p, String id) {
        return first(p.getProperty(prefix(id)+".bind"), "127.0.0.1");
    }
    public static int port(AppProperties p, String id) {
        return intRange(first(p.getProperty(prefix(id)+".port"), "19120"), 1, 65535,
                prefix(id)+".port");
    }
    public static String host(AppProperties p, String id) {
        return first(p.getProperty(prefix(id)+".host"), "127.0.0.1");
    }
    public static String tokenForNode(AppProperties p, String id, String nodeId) {
        return required(p.getProperty(prefix(id)+".token."+nodeId),
                prefix(id)+".token."+nodeId);
    }
    public static String prefix(String id) { return "config.service." + required(id, "service id"); }

    public static RemoteEndpoint remoteEndpoint(AppProperties p) {
        String id = required(p.getProperty(REMOTE_SERVICE_KEY), REMOTE_SERVICE_KEY);
        String host = required(p.getProperty(REMOTE_HOST_KEY), REMOTE_HOST_KEY);
        int port = intRange(required(p.getProperty(REMOTE_PORT_KEY), REMOTE_PORT_KEY), 1, 65535, REMOTE_PORT_KEY);
        String token = required(p.getProperty(REMOTE_TOKEN_KEY), REMOTE_TOKEN_KEY);
        return new RemoteEndpoint(id, host, port, token);
    }

    public static boolean remoteCacheEnabled(AppProperties p) {
        String value = p.getProperty(REMOTE_CACHE_ENABLED_KEY);
        return value == null || value.trim().isEmpty() || Boolean.parseBoolean(value.trim());
    }

    public static long remoteCacheMaxAgeMs(AppProperties p) {
        String value = first(p.getProperty(REMOTE_CACHE_MAXAGE_HOURS_KEY), "168");
        int hours = intRange(value, 1, 8760, REMOTE_CACHE_MAXAGE_HOURS_KEY);
        return hours * 60L * 60L * 1000L;
    }

    public static File remoteCacheFile(AppProperties p) {
        File bootstrap = p.getConfigFile();
        String configured = p.getProperty(REMOTE_CACHE_FILE_KEY);
        if (configured == null || configured.trim().isEmpty()) {
            return new File(bootstrap.getAbsolutePath() + ".cache");
        }
        File candidate = new File(configured.trim());
        if (candidate.isAbsolute()) return candidate;
        File parent = bootstrap.getAbsoluteFile().getParentFile();
        return new File(parent == null ? new File(".") : parent, configured.trim()).getAbsoluteFile();
    }

    private static int intRange(String value, int min, int max, String key) {
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed < min || parsed > max) throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid " + key + ": " + value);
        }
    }
    private static String first(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
    private static String required(String value, String key) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("Missing " + key);
        return value.trim();
    }

    public static final class RemoteEndpoint {
        private final String serviceId, host, token;
        private final int port;
        RemoteEndpoint(String serviceId, String host, int port, String token) {
            this.serviceId=serviceId; this.host=host; this.port=port; this.token=token;
        }
        public String getServiceId(){ return serviceId; }
        public String getHost(){ return host; }
        public int getPort(){ return port; }
        public String getToken(){ return token; }
    }
}
