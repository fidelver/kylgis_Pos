//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
//    Portions Copyright (c) 2015-2021 John Lewis (Chromis POS / ChromisKitchenScreen)
//    Portions Copyright (c) 2010-2021 Hugh Clayson / uniCenta (https://unicenta.com)
//    Portions Copyright (c) 2006-2010 Adrián Romero / Openbravo S.L.
//
//    This file is part of KylGis POS
//
//    KylGis POS is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
package com.mx.kylgis.pos.forms;

import com.mx.kylgis.pos.basic.BasicException;
import com.mx.kylgis.pos.config.DatabaseSettings;
import com.mx.kylgis.pos.data.loader.Session;
import com.mx.kylgis.pos.util.AltEncrypter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

/**
 * Creates the application JDBC session from the effective node configuration.
 */
public final class AppViewConnection {

    private AppViewConnection() {
    }

    public static Session createSession(AppProperties props) throws BasicException {
        try {
            DatabaseSettings selected = selectDatabase(props);
            JdbcDriverLoader.ensureLoaded(
                    selected.getDriver(), selected.getDriverLibraryFile());

            String user = selected.getUser();
            String password = decryptPassword(user, selected.getPassword());
            return new Session(selected.getJdbcUrl(), user, password);
        } catch (SQLException ex) {
            throw new BasicException(AppLocal.getIntString(
                    "message.databaseconnectionerror"), ex);
        } catch (Exception ex) {
            throw new BasicException(AppLocal.getIntString(
                    "message.databasedrivererror"), ex);
        }
    }

    private static DatabaseSettings selectDatabase(AppProperties props) {
        DatabaseSettings primary = DatabaseSettings.primary(props);
        if (!"true".equalsIgnoreCase(props.getProperty("db.multi"))) {
            return primary;
        }

        Path forcePrimary = Paths.get(System.getProperty("user.home"), "open.db");
        if (Files.exists(forcePrimary)) {
            return primary;
        }

        DatabaseSettings secondary = DatabaseSettings.secondary(props);
        ImageIcon icon = new ImageIcon(AppViewConnection.class.getResource(
                "/com/mx/kylgis/pos/images/kylgis_pos.png"));
        Object[] databases = {
            "0 - " + primary.getLabel(),
            "1 - " + secondary.getLabel()
        };

        Object selection = JOptionPane.showInputDialog(
                null,
                AppLocal.getIntString("message.databasechoose"),
                AppLocal.getIntString("message.databasechoose"),
                JOptionPane.QUESTION_MESSAGE,
                icon,
                databases,
                databases[0]);

        return selection != null && selection.toString().startsWith("1")
                ? secondary : primary;
    }

    private static String decryptPassword(String user, String password) {
        if (user != null && password != null && password.startsWith("crypt:")) {
            AltEncrypter cypher = new AltEncrypter("cypherkey" + user);
            return cypher.decrypt(password.substring(6));
        }
        return password;
    }
}
