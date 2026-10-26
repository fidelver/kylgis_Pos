//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.scale;

/**
 * Catálogo único de comandos de solicitud y capacidades de las básculas.
 *
 * Mantiene los valores técnicos fuera de la UI para que el selector y los
 * controladores compartan exactamente los mismos valores predeterminados.
 */
public final class ScaleRequestCatalog {

    private static final String[] PRESETS = {
        ScaleRequestCommand.AUTO,
        "ASCII:P",
        "ASCII:W",
        "HEX:57 0D",
        "ASCII:S",
        "ASCII:SI",
        "ASCII:$",
        "HEX:05"
    };

    private ScaleRequestCatalog() {
    }

    public static String[] presets() {
        return PRESETS.clone();
    }

    /**
     * Indica si el controlador acepta el comando configurable común.
     */
    public static boolean supportsCustomRequest(String scaleType) {
        if (scaleType == null) {
            return false;
        }
        switch (scaleType) {
            case "acompc100":
            case "averyberkel6720":
            case "casiopd1":
            case "dialog1":
            case "samsungesp":
            case "caspdii":
            case "mtind221":
            case "torreymetrologylatam":
                return true;
            default:
                return false;
        }
    }

    /**
     * Solicitud nativa del controlador, mostrada sólo como referencia.
     */
    public static String nativeRequest(String scaleType) {
        if (scaleType == null) {
            return ScaleRequestCommand.AUTO;
        }
        switch (scaleType) {
            case "acompc100":
            case "averyberkel6720":
            case "casiopd1":
                return "W+CR (HEX:57 0D)";
            case "dialog1":
                return "ENQ (HEX:05)";
            case "samsungesp":
                return "$ (HEX:24)";
            case "caspdii":
                return "W (HEX:57)";
            case "mtind221":
            case "torreymetrologylatam":
                return "P (HEX:50)";
            default:
                return ScaleRequestCommand.AUTO;
        }
    }
}
