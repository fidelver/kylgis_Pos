//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.hardware;

import com.mx.kylgis.pos.forms.AppProperties;
import java.util.Locale;

/**
 * Compatibility facade for historical native-hardware preflight checks.
 *
 * Serial printers, customer displays, scanners and scale drivers now use
 * jSerialComm, whose native transport is provided for the supported platforms
 * by the dependency JAR. RXTX architecture gates are therefore no longer
 * applicable to local POS hardware or hardware-service nodes.
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
        // Intentionally no-op: all local serial hardware supported here uses jSerialComm.
    }

    /**
     * Kept as a stable service API. Serial customer displays use the same
     * portable writer as serial ESC/POS printers and no longer require RXTX.
     */
    public static void validateDisplayService(AppProperties properties, String serviceId) {
        // Intentionally no-op: local serial displays use jSerialComm.
    }

    /**
     * Kept as a stable service API. Scale transports now use jSerialComm and
     * no longer require an RXTX architecture gate.
     */
    public static void validateScaleService(AppProperties properties, String serviceId) {
        // Intentionally no-op: local scale transport uses jSerialComm.
    }

    /**
     * ScanPal serial transport uses jSerialComm, so scanner services no longer
     * require an RXTX architecture gate. The method remains as a stable API.
     */
    public static void validateScannerService(AppProperties properties, String serviceId) {
        // Intentionally no-op: local ScanPal transport uses jSerialComm.
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

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ENGLISH);
    }
}
