//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.hardware;

import com.mx.kylgis.pos.forms.AppProperties;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Side-effect-free capability report for local peripherals.
 * It only inspects configuration and ownership; it never opens hardware.
 */
public final class PeripheralCapabilityReport {

    private PeripheralCapabilityReport() {
    }

    public static Map<String, String> inspect(AppProperties properties) {
        Map<String, String> result = new LinkedHashMap<>();
        inspect(result, properties, "printer", "machine.printer");
        inspect(result, properties, "scale", "machine.scale");
        inspect(result, properties, "scanner", "machine.scanner");
        inspect(result, properties, "display", "machine.display");
        inspect(result, properties, "cashdrawer", "machine.printer");
        return Collections.unmodifiableMap(result);
    }

    private static void inspect(Map<String, String> result, AppProperties properties,
            String capability, String configKey) {
        String configured = trimToNull(properties == null ? null : properties.getProperty(configKey));
        if (configured == null || "Not defined".equalsIgnoreCase(configured)) {
            result.put(capability, "NOT_CONFIGURED");
            return;
        }
        try {
            result.put(capability, HardwareOwnership.canOpen(properties, capability)
                    ? "AVAILABLE" : "OWNED_BY_OTHER_NODE");
        } catch (RuntimeException ex) {
            result.put(capability, "INVALID_CONFIGURATION");
        }
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
