//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.config;

import com.mx.kylgis.pos.data.loader.Session;
import com.mx.kylgis.pos.forms.AppConfig;
import com.mx.kylgis.pos.forms.DriverWrapper;
import com.mx.kylgis.pos.util.AltEncrypter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

/**
 * Minimal automatic database status panel for provisioned nodes.
 *
 * Connection settings are resolved from MASTER + node module + secrets. There
 * are deliberately no editable connection fields or manual test buttons here.
 */
public final class JPanelConfigDatabaseStatus extends JPanel implements PanelConfig {

    private static final Logger LOGGER = Logger.getLogger(JPanelConfigDatabaseStatus.class.getName());

    private final JLabel indicator = new JLabel("●", SwingConstants.CENTER);
    private SwingWorker<Boolean, Void> worker;

    public JPanelConfigDatabaseStatus() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(70, 20, 70, 20));

        indicator.setFont(new Font("SansSerif", Font.BOLD, 72));
        indicator.setForeground(Color.GRAY);
        indicator.setToolTipText("Comprobando conexión con la base de datos");
        add(indicator, BorderLayout.CENTER);
    }

    @Override
    public void loadProperties(final AppConfig config) {
        indicator.setForeground(Color.GRAY);
        indicator.setToolTipText("Comprobando conexión con la base de datos");

        if (worker != null && !worker.isDone()) {
            worker.cancel(true);
        }

        worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return testConnection(config);
            }

            @Override
            protected void done() {
                if (isCancelled()) {
                    return;
                }
                boolean connected = false;
                try {
                    connected = Boolean.TRUE.equals(get());
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING, "Automatic database status check failed", ex);
                }
                indicator.setForeground(connected ? new Color(0, 140, 0) : Color.RED);
                indicator.setToolTipText(connected
                        ? "Conexión con la base de datos establecida"
                        : "Base de datos no disponible");
            }
        };
        worker.execute();
    }

    private boolean testConnection(AppConfig config) {
        Session session = null;
        try {
            DatabaseSettings settings = DatabaseSettings.primary(config);
            loadDriver(settings);

            String user = settings.getUser();
            String password = settings.getPassword();
            if (user != null && password != null && password.startsWith("crypt:")) {
                AltEncrypter cypher = new AltEncrypter("cypherkey" + user);
                password = cypher.decrypt(password.substring(6));
            }

            session = new Session(withStatusTimeouts(settings.getJdbcUrl()), user, password);
            Connection connection = session.getConnection();
            return connection != null && connection.isValid(3);
        } catch (Exception ex) {
            LOGGER.log(Level.FINE, "Provisioned database is not reachable", ex);
            return false;
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    private void loadDriver(DatabaseSettings settings) throws Exception {
        try {
            Class.forName(settings.getDriver(), true,
                    Thread.currentThread().getContextClassLoader());
            return;
        } catch (ClassNotFoundException ex) {
            File library = settings.getDriverLibraryFile();
            if (library == null || !library.isFile()) {
                throw ex;
            }
            ClassLoader loader = new URLClassLoader(new URL[]{library.toURI().toURL()});
            DriverManager.registerDriver(new DriverWrapper((Driver)
                    Class.forName(settings.getDriver(), true, loader).newInstance()));
        }
    }

    private String withStatusTimeouts(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.trim().isEmpty()) {
            return jdbcUrl;
        }
        String separator = jdbcUrl.contains("?") ? "&" : "?";
        return jdbcUrl + separator + "connectTimeout=3000&socketTimeout=3000";
    }

    @Override
    public void saveProperties(AppConfig config) {
        // The connection is managed by MASTER/node provisioning; nothing to save.
    }

    @Override
    public boolean hasChanged() {
        return false;
    }

    @Override
    public Component getConfigComponent() {
        return this;
    }
}
