//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
//
//    This file is part of KylGis POS and is distributed under the GPL.
package com.mx.kylgis.pos.forms;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads JDBC drivers from the application class path first and keeps the
 * historical external-driver-JAR mechanism as a compatibility fallback.
 */
public final class JdbcDriverLoader {

    private static final List<URLClassLoader> EXTERNAL_LOADERS = new ArrayList<>();

    private JdbcDriverLoader() {
    }

    public static synchronized void ensureLoaded(String driverClass,
            File externalLibrary) throws Exception {
        if (driverClass == null || driverClass.trim().isEmpty()) {
            throw new ClassNotFoundException("JDBC driver class is empty");
        }

        String className = driverClass.trim();
        try {
            Class.forName(className, true, Thread.currentThread().getContextClassLoader());
            return;
        } catch (ClassNotFoundException notOnClasspath) {
            if (externalLibrary == null || !externalLibrary.isFile()) {
                throw notOnClasspath;
            }

            URLClassLoader loader = new URLClassLoader(
                    new URL[]{externalLibrary.toURI().toURL()},
                    Thread.currentThread().getContextClassLoader());
            try {
                Object instance = Class.forName(className, true, loader)
                        .getDeclaredConstructor().newInstance();
                if (!(instance instanceof Driver)) {
                    throw new IllegalArgumentException(className + " is not a java.sql.Driver");
                }
                DriverManager.registerDriver(new DriverWrapper((Driver) instance));
                EXTERNAL_LOADERS.add(loader);
            } catch (Exception ex) {
                try {
                    loader.close();
                } catch (Exception ignored) {
                    // Preserve the original loading failure.
                }
                throw ex;
            }
        }
    }
}
