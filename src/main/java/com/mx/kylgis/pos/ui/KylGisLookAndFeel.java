//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.ui;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * Central boundary for applying the configured Swing look and feel.
 *
 * Legacy Substance support is resolved reflectively so application entry
 * points and this API have no compile-time dependency on a particular UI
 * library. Future FlatLaf adoption can therefore stay behind this boundary.
 */
public final class KylGisLookAndFeel {

    private static final String SUBSTANCE_SKIN =
            "org.pushingpixels.substance.api.SubstanceSkin";
    private static final String SUBSTANCE_LAF =
            "org.pushingpixels.substance.api.SubstanceLookAndFeel";

    private KylGisLookAndFeel() {
    }

    /**
     * Applies a configured Look & Feel class.
     *
     * @return true when a supported Look & Feel or legacy Substance skin was
     *         applied; false when configuration is blank, unsupported or could
     *         not load.
     */
    public static boolean apply(String className, Logger logger) {
        if (className == null || className.trim().isEmpty()) {
            log(logger, Level.WARNING, "Look and Feel class is not configured", null);
            return false;
        }

        String configuredClass = className.trim();
        try {
            Object laf = Class.forName(configuredClass)
                    .getDeclaredConstructor().newInstance();
            if (laf instanceof LookAndFeel) {
                UIManager.setLookAndFeel((LookAndFeel) laf);
                return true;
            }
            if (applyLegacySubstanceSkin(laf)) {
                return true;
            }
            log(logger, Level.WARNING,
                    "Configured class is not a supported Look and Feel: " + configuredClass,
                    null);
            return false;
        } catch (ReflectiveOperationException | UnsupportedLookAndFeelException | RuntimeException ex) {
            log(logger, Level.WARNING,
                    "Cannot set Look and Feel: " + configuredClass, ex);
            return false;
        }
    }

    private static boolean applyLegacySubstanceSkin(Object candidate)
            throws ReflectiveOperationException {
        Class<?> skinType;
        try {
            skinType = Class.forName(SUBSTANCE_SKIN);
        } catch (ClassNotFoundException ex) {
            return false;
        }
        if (!skinType.isInstance(candidate)) {
            return false;
        }

        Class<?> substanceLaf = Class.forName(SUBSTANCE_LAF);
        Method setSkin = substanceLaf.getMethod("setSkin", skinType);
        setSkin.invoke(null, candidate);
        return true;
    }

    private static void log(Logger logger, Level level, String message, Throwable error) {
        if (logger == null) {
            return;
        }
        if (error == null) {
            logger.log(level, message);
        } else {
            logger.log(level, message, error);
        }
    }
}
