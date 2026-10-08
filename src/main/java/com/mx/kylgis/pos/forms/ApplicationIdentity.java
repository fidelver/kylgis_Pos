//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.forms;

/**
 * Resolves the runtime identity of a KylGis POS node.
 *
 * Product identity and database compatibility identity are deliberately kept
 * separate. Existing schemas may keep historical IDs such as
 * kylgisplataformas while running exactly the same KylGis POS JAR.
 */
public final class ApplicationIdentity {

    public static final String ID_KEY = "application.id";
    public static final String NAME_KEY = "application.name";
    public static final String DISPLAY_NAME_KEY = "application.displayname";

    public static final String SYSTEM_ID = "kylgis.application.id";
    public static final String SYSTEM_NAME = "kylgis.application.name";
    public static final String SYSTEM_DISPLAY_NAME = "kylgis.application.displayname";

    private ApplicationIdentity() {
    }

    public static String getDatabaseId(AppProperties properties) {
        return firstNonBlank(value(properties, ID_KEY),
                System.getProperty(SYSTEM_ID), AppLocal.APP_ID);
    }

    public static String getDatabaseName(AppProperties properties) {
        return firstNonBlank(value(properties, NAME_KEY),
                System.getProperty(SYSTEM_NAME), AppLocal.APP_NAME);
    }

    public static String getDisplayName(AppProperties properties) {
        return firstNonBlank(value(properties, DISPLAY_NAME_KEY),
                System.getProperty(SYSTEM_DISPLAY_NAME),
                getDatabaseName(properties), AppLocal.APP_NAME);
    }

    public static String getVersion() {
        return AppLocal.APP_VERSION;
    }

    /**
     * Supplies compatibility defaults for legacy deployments. Explicit
     * application.* values in the active configuration always have priority.
     * Existing -D overrides also remain authoritative.
     */
    public static void setCompatibilityFallback(String databaseId,
            String databaseName, String displayName) {
        setSystemDefault(SYSTEM_ID, databaseId);
        setSystemDefault(SYSTEM_NAME, databaseName);
        setSystemDefault(SYSTEM_DISPLAY_NAME, displayName);
    }

    private static void setSystemDefault(String key, String value) {
        if (firstNonBlank(System.getProperty(key)) == null
                && firstNonBlank(value) != null) {
            System.setProperty(key, value.trim());
        }
    }

    private static String value(AppProperties properties, String key) {
        return properties == null ? null : properties.getProperty(key);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }
}
