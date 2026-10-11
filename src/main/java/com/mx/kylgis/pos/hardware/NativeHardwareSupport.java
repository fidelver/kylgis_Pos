//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.hardware;

import com.mx.kylgis.pos.forms.AppProperties;
import com.mx.kylgis.pos.scale.service.ScaleServiceConfig;
import com.mx.kylgis.pos.scanpal2.service.ScannerServiceConfig;
import java.util.Locale;

/**
 * Preflight for hardware that still depends on legacy RXTX native binaries.
 *
 * Serial printers and customer displays no longer belong to this gate: their
 * serial transport uses jSerialComm and can load a native implementation for
 * the current supported platform/architecture from the dependency JAR itself.
 * Scale and ScanPal implementations remain on RXTX until their dedicated
 * drivers are migrated and therefore still require this compatibility check.
 */
public final class NativeHardwareSupport {

    private NativeHardwareSupport() { }

    public static void validatePos(AppProperties properties) {
        validatePos(properties, System.getProperty("os.name", ""),
                System.getProperty("os.arch", ""));
    }

    /**
     * Kept as a stable service API. Serial printing is portable now, so no
     * RXTX architecture gate is required for print services.
     */
    public static void validatePrintService(AppProperties properties,
            String serviceId, String allowedPrinters) {
        // Intentionally no-op: local serial printing uses jSerialComm.
    }

    static void validatePos(AppProperties properties, String osName, String arch) {
        if (properties == null || isRxtxSupported(osName, arch)) return;

        String scale = properties.getProperty("machine.scale");
        if (ScaleServiceConfig.getServiceId(properties) == null
                && HardwareOwnership.canOpen(properties, "scale") && usesSerialScale(scale)) {
            throw unsupported("scale", scale, osName, arch,
                    "Configure device.scale.service para usar un servicio de báscula remoto");
        }

        String scanner = properties.getProperty("machine.scanner");
        if (ScannerServiceConfig.getServiceId(properties) == null
                && HardwareOwnership.canOpen(properties, "scanner") && usesSerialScanner(scanner)) {
            throw unsupported("scanner", scanner, osName, arch,
                    "Configure device.scanner.service para usar un servicio de scanner remoto");
        }
    }

    /**
     * Kept as a stable service API. Serial customer displays use the same
     * portable writer as serial ESC/POS printers and no longer require RXTX.
     */
    public static void validateDisplayService(AppProperties properties, String serviceId) {
        // Intentionally no-op: local serial displays use jSerialComm.
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

    public static void validateScannerService(AppProperties properties, String serviceId) {
        if (isRxtxSupportedCurrentPlatform()) return;
        String scanner = properties == null ? null : properties.getProperty("machine.scanner");
        if (usesSerialScanner(scanner)) {
            throw unsupported("scanner", scanner,
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
            return cpu.matches("(x86|i[3-6]86|x86_64|amd64)");
        }
        if (os.contains("windows")) {
            return cpu.matches("(x86|i[3-6]86|x86_64|amd64)");
        }
        if (os.contains("mac") || os.contains("darwin")) {
            return cpu.matches("(x86|i[3-6]86|x86_64|amd64)");
        }
        return false;
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
