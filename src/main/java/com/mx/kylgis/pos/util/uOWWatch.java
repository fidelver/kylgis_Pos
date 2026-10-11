//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
//    Portions Copyright (c) 2015-2021 John Lewis (Chromis POS / ChromisKitchenScreen)
//    Portions Copyright (c) 2010-2021 Hugh Clayson / uniCenta
//
//    This file is part of KylGis POS and is distributed under the GPL.
package com.mx.kylgis.pos.util;

/**
 * Compatibility holder for the last detected iButton identifier.
 *
 * <p>The legacy class previously started a second Dallas/Maxim monitor. The
 * monitor is now owned by JRootApp through {@link OptionalIButtonMonitor}, so
 * only one optional 1-Wire watcher is active per POS instance.</p>
 */
public final class uOWWatch {

    private static volatile String ibuttonid = "";

    private uOWWatch() {
    }

    /**
     * Kept for binary/source compatibility. Monitoring is started centrally by
     * JRootApp when machine.iButton is enabled.
     */
    @Deprecated
    public static void iButtonOn() {
        // Intentionally empty: JRootApp owns the single monitor lifecycle.
    }

    public static void setIButtonId(String value) {
        ibuttonid = value == null ? "" : value;
    }

    public static void clearIButtonId(String value) {
        if (value == null || value.equals(ibuttonid)) {
            ibuttonid = "";
        }
    }

    public static String getibuttonid() {
        return ibuttonid;
    }
}
