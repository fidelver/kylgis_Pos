//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
//
//    This file is part of KylGis POS.
package com.mx.kylgis.pos.config.provisioning;

import java.io.IOException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Resolves !secret.name references without modifying the raw configuration. */
public final class SecretResolver {

    public static final String PREFIX = "!secret.";
    private static final Pattern REFERENCE_PATTERN =
            Pattern.compile("!secret\\.([A-Za-z0-9_.-]+)");

    private SecretResolver() {
    }

    public static Properties resolve(Properties raw, Properties secrets) throws IOException {
        Properties resolved = new Properties();
        if (raw == null) {
            return resolved;
        }
        for (String key : raw.stringPropertyNames()) {
            String rawValue = raw.getProperty(key);
            if (rawValue != null) {
                resolved.setProperty(key, resolveValue(key, rawValue, secrets));
            }
        }
        return resolved;
    }

    public static boolean containsSecretReferences(Properties properties) {
        if (properties == null) {
            return false;
        }
        for (String key : properties.stringPropertyNames()) {
            if (containsSecretReference(properties.getProperty(key))) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsSecretReference(String value) {
        return value != null && REFERENCE_PATTERN.matcher(value).find();
    }

    public static boolean isSecretReference(String value) {
        if (value == null) {
            return false;
        }
        Matcher matcher = REFERENCE_PATTERN.matcher(value.trim());
        return matcher.matches();
    }

    public static String getSecretKey(String reference) {
        if (reference == null) {
            return null;
        }
        Matcher matcher = REFERENCE_PATTERN.matcher(reference.trim());
        return matcher.matches() ? matcher.group(1) : null;
    }

    private static String resolveValue(String propertyKey, String rawValue,
            Properties secrets) throws IOException {
        Matcher matcher = REFERENCE_PATTERN.matcher(rawValue);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            String secretKey = matcher.group(1);
            String secretValue = secrets == null ? null : secrets.getProperty(secretKey);
            if (secretValue == null) {
                throw new IOException("Missing KylGis secret: " + secretKey
                        + " required by property " + propertyKey);
            }
            matcher.appendReplacement(output, Matcher.quoteReplacement(secretValue));
        }
        matcher.appendTail(output);
        return output.toString();
    }
}
