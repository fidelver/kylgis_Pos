//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.hardware;

import com.mx.kylgis.pos.forms.AppProperties;
import com.mx.kylgis.pos.printer.service.PrintServiceConfig;
import com.mx.kylgis.pos.scale.service.ScaleServiceConfig;
import java.util.Locale;

/** Preflight for legacy native hardware that still depends on RXTX. */
public final class NativeHardwareSupport {

    private NativeHardwareSupport() { }

    public static void validatePos(AppProperties properties) {
        validatePos(properties, System.getProperty("os.name", ""),
                System.getProperty("os.arch", ""));
    }

    public static void validatePrintService(AppProperties properties,
            String serviceId, String allowedPrinters) {
        if (isRxtxSupportedCurrentPlatform()) return;
        for (int i = 1; i <= 6; i++) {
            String index = Integer.toString(i);
            if (!PrintServiceConfig.isPrinterAllowed(allowedPrinters, index)) continue;
            String value = printerProperty(properties, index);
            if (usesRxtxConnection(value)) {
                throw unsupported("printer." + index, value,
                        System.getProperty("os.name", ""),
                        System.getProperty("os.arch", ""),
                        "El servicio " + serviceId + " debe ejecutarse en un nodo con driver RXTX compatible");
            }
        }
    }

    static void validatePos(AppProperties properties, String osName, String arch) {
        if (properties == null || isRxtxSupported(osName, arch)) return;

        String display = properties.getProperty("machine.display");
        if (HardwareOwnership.canOpen(properties, "display") && usesRxtxConnection(display)) {
            throw unsupported("display", display, osName, arch,
                    "Use un display virtual o mueva el periférico a un servicio/nodo compatible");
        }

        for (int i = 1; i <= 6; i++) {
            String index = Integer.toString(i);
            if (PrintServiceConfig.getServiceId(properties, index) != null) continue;
            if (!HardwareOwnership.canOpen(properties, "printer." + index)) continue;
            String value = printerProperty(properties, index);
            if (usesRxtxConnection(value)) {
                throw unsupported("printer." + index, value, osName, arch,
                        "Configure device.printer." + index + ".service para usar un servicio de impresión remoto");
            }
        }

        String scale = properties.getProperty("machine.scale");
        if (ScaleServiceConfig.getServiceId(properties) == null
                && HardwareOwnership.canOpen(properties, "scale") && usesSerialScale(scale)) {
            throw unsupported("scale", scale, osName, arch,
                    "Configure device.scale.service para usar un servicio de báscula remoto");
        }

        String scanner = properties.getProperty("machine.scanner");
        if (HardwareOwnership.canOpen(properties, "scanner") && usesSerialScanner(scanner)) {
            throw unsupported("scanner", scanner, osName, arch,
                    "Mueva el scanner serie a un nodo/servicio de hardware con driver compatible");
        }
    }


    public static void validateScaleService(AppProperties properties, String serviceId) {
        if (isRxtxSupportedCurrentPlatform()) return;
        String scale = properties == null ? null : properties.getProperty("machine.scale");
        if (usesSerialScale(scale)) {
            throw unsupported("scale", scale,
                    System.getProperty("os.name", ""), System.getProperty("os.arch", ""),
                    "El servicio " + serviceId + " debe ejecutarse en un nodo con driver RXTX compatible");
        }
    }

    public static boolean isRxtxSupportedCurrentPlatform() {
        return isRxtxSupported(System.getProperty("os.name", ""),
                System.getProperty("os.arch", ""));
    }

    public static boolean isRxtxSupported(String osName, String arch) {
        String os = normalize(osName);
        String cpu = normalize(arch);
        if (os.contains("linux")) {
            return cpu.matches("(x86|i[3-6]86|x86_64|amd64|ia64)");
        }
        if (os.contains("windows")) {
            return cpu.matches("(x86|i[3-6]86|x86_64|amd64)");
        }
        if (os.contains("mac") || os.contains("darwin")) {
            return cpu.matches("(x86|i[3-6]86|x86_64|amd64)");
        }
        return false;
    }

    static boolean usesRxtxConnection(String configured) {
        if (configured == null) return false;
        String value = configured.trim().toLowerCase(Locale.ENGLISH);
        if (value.isEmpty() || value.startsWith("not defined")) return false;
        int colon = value.indexOf(':');
        String type = colon < 0 ? value : value.substring(0, colon).trim();
        if ("serial".equals(type) || "rxtx".equals(type)) return true;
        if (colon < 0) return false;
        String parameters = value.substring(colon + 1);
        int comma = parameters.indexOf(',');
        String connection = (comma < 0 ? parameters : parameters.substring(0, comma)).trim();
        return "serial".equals(connection) || "rxtx".equals(connection);
    }

    private static boolean usesSerialScale(String configured) {
        if (configured == null) return false;
        String value = configured.trim().toLowerCase(Locale.ENGLISH);
        if (value.isEmpty() || value.startsWith("not defined") || value.startsWith("screen")) return false;
        return value.indexOf(':') > 0;
    }

    private static boolean usesSerialScanner(String configured) {
        if (configured == null) return false;
        String value = configured.trim().toLowerCase(Locale.ENGLISH);
        return value.startsWith("scanpal2:");
    }

    private static String printerProperty(AppProperties properties, String index) {
        return "1".equals(index) ? properties.getProperty("machine.printer")
                : properties.getProperty("machine.printer." + index);
    }

    private static IllegalStateException unsupported(String deviceId,
            String configured, String osName, String arch, String remediation) {
        return new IllegalStateException("Hardware serie local " + deviceId
                + " requiere RXTX, pero KylGis no incluye un binario nativo para "
                + platform(osName, arch) + ". Configuración: " + configured
                + ". " + remediation + ".");
    }

    private static String platform(String osName, String arch) {
        return (osName == null ? "unknown" : osName.trim()) + "/"
                + (arch == null ? "unknown" : arch.trim());
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ENGLISH);
    }
}
