//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
//
//    This file is part of KylGis POS.
package com.mx.kylgis.pos.config.provisioning;

import java.io.IOException;
import java.util.Properties;

/** Resolves !secret.name references without modifying the raw configuration. */
public final class SecretResolver {

    public static final String PREFIX = "!secret.";

    private SecretResolver() {
    }

    public static Properties resolve(Properties raw, Properties secrets) throws IOException {
        Properties resolved = new Properties();
        if (raw == null) {
            return resolved;
        }
        for (String key : raw.stringPropertyNames()) {
            String rawValue = raw.getProperty(key);
            if (isSecretReference(rawValue)) {
                String secretKey = getSecretKey(rawValue);
                String secretValue = secrets == null ? null : secrets.getProperty(secretKey);
                if (secretValue == null) {
                    throw new IOException("Missing KylGis secret: " + secretKey
                            + " required by property " + key);
                }
                resolved.setProperty(key, secretValue);
            } else if (rawValue != null) {
                resolved.setProperty(key, rawValue);
            }
        }
        return resolved;
    }

    public static boolean containsSecretReferences(Properties properties) {
        if (properties == null) {
            return false;
        }
        for (String key : properties.stringPropertyNames()) {
            if (isSecretReference(properties.getProperty(key))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSecretReference(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        return trimmed.startsWith(PREFIX) && trimmed.length() > PREFIX.length();
    }

    public static String getSecretKey(String reference) {
        if (!isSecretReference(reference)) {
            return null;
        }
        return reference.trim().substring(PREFIX.length());
    }
}
