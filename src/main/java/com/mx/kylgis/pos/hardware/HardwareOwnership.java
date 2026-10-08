//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.hardware;

import com.mx.kylgis.pos.forms.AppProperties;
import com.mx.kylgis.pos.node.NodeContext;

/**
 * Prevents a KylGis process from opening physical hardware owned by another
 * node or service.
 *
 * Ownership is opt-in for backwards compatibility. If device.<id>.owner is
 * absent, the legacy behavior is preserved. Supported owner tokens:
 *
 *   *                  any process
 *   caja-rif           shorthand for node:caja-rif
 *   node:caja-rif      one logical node
 *   service:printer-01 one process exposing service.id=printer-01
 *
 * Multiple owners may be separated by commas or semicolons.
 */
public final class HardwareOwnership {

    public static final String SERVICE_ID_KEY = "service.id";

    private HardwareOwnership() {
    }

    public static String ownerKey(String deviceId) {
        return "device." + normalizeDeviceId(deviceId) + ".owner";
    }

    public static boolean canOpen(AppProperties properties, String deviceId) {
        if (properties == null) {
            return true;
        }
        String configured = trimToNull(properties.getProperty(ownerKey(deviceId)));
        if (configured == null) {
            return true;
        }

        String nodeId = trimToNull(NodeContext.from(properties).getNodeId());
        String serviceId = trimToNull(properties.getProperty(SERVICE_ID_KEY));
        for (String raw : configured.split("[,;]+")) {
            String owner = trimToNull(raw);
            if (owner == null) {
                continue;
            }
            if ("*".equals(owner) || "any".equalsIgnoreCase(owner)) {
                return true;
            }
            if (owner.regionMatches(true, 0, "node:", 0, 5)) {
                if (same(owner.substring(5), nodeId)) {
                    return true;
                }
                continue;
            }
            if (owner.regionMatches(true, 0, "service:", 0, 8)) {
                if (same(owner.substring(8), serviceId)) {
                    return true;
                }
                continue;
            }
            if (same(owner, nodeId)) {
                return true;
            }
        }
        return false;
    }

    public static String getConfiguredOwner(AppProperties properties, String deviceId) {
        if (properties == null) {
            return null;
        }
        return trimToNull(properties.getProperty(ownerKey(deviceId)));
    }

    public static String getCurrentNodeId(AppProperties properties) {
        return properties == null ? null : NodeContext.from(properties).getNodeId();
    }

    public static String getCurrentServiceId(AppProperties properties) {
        return properties == null ? null : trimToNull(properties.getProperty(SERVICE_ID_KEY));
    }

    private static String normalizeDeviceId(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException("deviceId is required");
        }
        return normalized;
    }

    private static boolean same(String left, String right) {
        String a = trimToNull(left);
        String b = trimToNull(right);
        return a != null && b != null && a.equalsIgnoreCase(b);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
