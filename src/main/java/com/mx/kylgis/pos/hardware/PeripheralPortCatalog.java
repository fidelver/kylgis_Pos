//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.hardware;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import javax.swing.JComboBox;

/**
 * Portable suggestions for serial/parallel device endpoints.
 *
 * Enumeration deliberately does not use RXTX: opening the configuration panel
 * must never require a native library for the current CPU architecture.
 */
public final class PeripheralPortCatalog {

    private PeripheralPortCatalog() { }

    public static void populate(JComboBox<String> combo) {
        if (combo == null) return;
        combo.removeAllItems();
        for (String port : suggestions(System.getProperty("os.name", ""), new File("/dev"))) {
            combo.addItem(port);
        }
        combo.setEditable(true);
        combo.setToolTipText(tooltip(System.getProperty("os.name", "")));
    }

    static Set<String> suggestions(String osName, File devRoot) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        String os = osName == null ? "" : osName.toLowerCase(Locale.ENGLISH);
        if (os.contains("win")) {
            for (int i = 1; i <= 32; i++) result.add("COM" + i);
            for (int i = 1; i <= 4; i++) result.add("LPT" + i);
        } else if (os.contains("mac") || os.contains("darwin")) {
            addMatches(result, devRoot, "cu.");
            addMatches(result, devRoot, "tty.");
            if (result.isEmpty()) {
                result.add("/dev/cu.usbserial");
                result.add("/dev/cu.usbmodem");
            }
        } else {
            addSerialById(result, devRoot);
            addMatches(result, devRoot, "ttyUSB");
            addMatches(result, devRoot, "ttyACM");
            addMatches(result, devRoot, "rfcomm");
            File usb = new File(devRoot, "usb");
            addMatches(result, usb, "lp");
            addMatches(result, devRoot, "ttyS");
            if (result.isEmpty()) {
                result.add("/dev/ttyUSB0");
                result.add("/dev/ttyACM0");
                result.add("/dev/rfcomm0");
                result.add("/dev/usb/lp0");
            }
        }
        return result;
    }

    private static void addSerialById(Set<String> target, File devRoot) {
        File directory = new File(new File(devRoot, "serial"), "by-id");
        File[] files = directory.listFiles();
        if (files == null) return;
        java.util.Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        for (File file : files) target.add(file.getAbsolutePath());
    }

    private static void addMatches(Set<String> target, File directory, String prefix) {
        File[] files = directory == null ? null : directory.listFiles();
        if (files == null) return;
        java.util.Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        for (File file : files) {
            if (file.getName().startsWith(prefix)) target.add(file.getAbsolutePath());
        }
    }

    public static String tooltip(String osName) {
        String os = osName == null ? "" : osName.toLowerCase(Locale.ENGLISH);
        if (os.contains("win")) {
            return "Puerto serie/paralelo de Windows. Puede escribir manualmente otro COM/LPT.";
        }
        if (os.contains("mac") || os.contains("darwin")) {
            return "Dispositivo serie de macOS. Puede escribir manualmente una ruta /dev/cu.* o /dev/tty.*.";
        }
        return "<html>Dispositivo de Linux detectado. Puede escribir manualmente otra ruta.<br>"
                + "Se prefieren rutas estables /dev/serial/by-id/... cuando existen.</html>";
    }
}
