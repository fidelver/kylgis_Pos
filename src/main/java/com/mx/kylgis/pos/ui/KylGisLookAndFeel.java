//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.ui;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * Central boundary for Swing Look & Feel discovery and application.
 *
 * Legacy Substance support is resolved reflectively so application entry
 * points and configuration screens have no compile-time dependency on a
 * particular UI library. Future FlatLaf adoption can stay behind this API.
 */
public final class KylGisLookAndFeel {

    private static final String SUBSTANCE_SKIN =
            "org.pushingpixels.substance.api.SubstanceSkin";
    private static final String SUBSTANCE_LAF =
            "org.pushingpixels.substance.api.SubstanceLookAndFeel";
    private static final String WEB_LAF = "com.alee.laf.WebLookAndFeel";

    private KylGisLookAndFeel() {
    }

    /**
     * Discovers standard Swing Look & Feels plus legacy Substance skins when
     * the Substance library is present. Results are de-duplicated by class.
     */
    public static List<Option> availableOptions(Logger logger) {
        Map<String, Option> options = new LinkedHashMap<>();
        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            options.putIfAbsent(info.getClassName(),
                    new Option(info.getName(), info.getClassName()));
        }
        addLegacySubstanceOptions(options, logger);
        return new ArrayList<>(options.values());
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

    public static String currentClassName() {
        LookAndFeel current = UIManager.getLookAndFeel();
        return current == null ? "" : current.getClass().getName();
    }

    /**
     * Preserves the legacy standalone configurator appearance without exposing
     * WebLaF classes to that entry point. This method is intentionally isolated
     * so it can disappear when the configurator migrates to the common LAF.
     */
    public static boolean installLegacyWebLookAndFeel(Logger logger) {
        try {
            Class<?> webLookAndFeel = Class.forName(WEB_LAF);
            Method install = webLookAndFeel.getMethod("install");
            install.invoke(null);
            return true;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            log(logger, Level.WARNING, "Cannot install legacy WebLaF", ex);
            return false;
        }
    }

    /**
     * Initializes legacy WebLaF managers for screens that still embed WebLaF
     * widgets. Kept reflective so callers are no longer coupled to WebLaF.
     */
    public static boolean initializeLegacyWebManagers(Logger logger) {
        try {
            Class<?> webLookAndFeel = Class.forName(WEB_LAF);
            Method initializeManagers = webLookAndFeel.getMethod("initializeManagers");
            initializeManagers.invoke(null);
            return true;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            log(logger, Level.WARNING, "Cannot initialize legacy WebLaF managers", ex);
            return false;
        }
    }

    private static void addLegacySubstanceOptions(
            Map<String, Option> options, Logger logger) {
        try {
            Class<?> substanceLaf = Class.forName(SUBSTANCE_LAF);
            Method getAllSkins = substanceLaf.getMethod("getAllSkins");
            Object result = getAllSkins.invoke(null);
            if (!(result instanceof Map<?, ?>)) {
                return;
            }
            for (Object skinInfo : ((Map<?, ?>) result).values()) {
                if (skinInfo == null) {
                    continue;
                }
                Method getDisplayName = skinInfo.getClass().getMethod("getDisplayName");
                Method getClassName = skinInfo.getClass().getMethod("getClassName");
                String displayName = String.valueOf(getDisplayName.invoke(skinInfo));
                String className = String.valueOf(getClassName.invoke(skinInfo));
                if (!className.isBlank()) {
                    options.putIfAbsent(className, new Option(displayName, className));
                }
            }
        } catch (ClassNotFoundException ex) {
            // Substance is optional at this boundary; standard Swing LAFs remain valid.
        } catch (ReflectiveOperationException | RuntimeException ex) {
            log(logger, Level.FINE,
                    "Legacy Substance Look and Feel catalogue is unavailable", ex);
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

    public static final class Option {
        private final String displayName;
        private final String className;

        public Option(String displayName, String className) {
            this.displayName = displayName;
            this.className = className;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getClassName() {
            return className;
        }
    }
}
