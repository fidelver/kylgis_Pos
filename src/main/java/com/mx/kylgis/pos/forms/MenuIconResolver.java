//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.forms;

import java.net.URL;
import javax.swing.ImageIcon;

/** Resolves current and legacy persisted menu icon resource paths safely. */
final class MenuIconResolver {

    private static final String KYLGIS_IMAGES = "/com/mx/kylgis/pos/images/";
    private static final String OPENBRAVO_IMAGES = "/com/openbravo/images/";
    private static final String UNICENTA_IMAGES = "/com/unicenta/images/";

    private MenuIconResolver() {
    }

    static ImageIcon resolve(String icon) {
        if (icon == null || icon.trim().isEmpty()) {
            return null;
        }

        URL resource = JPrincipalApp.class.getResource(icon);
        if (resource == null) {
            String migrated = migrateLegacyPath(icon);
            if (!migrated.equals(icon)) {
                resource = JPrincipalApp.class.getResource(migrated);
            }
        }
        return resource == null ? null : new ImageIcon(resource);
    }

    static String migrateLegacyPath(String icon) {
        if (icon.startsWith(OPENBRAVO_IMAGES)) {
            return KYLGIS_IMAGES + icon.substring(OPENBRAVO_IMAGES.length());
        }
        if (icon.startsWith(UNICENTA_IMAGES)) {
            return KYLGIS_IMAGES + icon.substring(UNICENTA_IMAGES.length());
        }
        return icon;
    }
}
