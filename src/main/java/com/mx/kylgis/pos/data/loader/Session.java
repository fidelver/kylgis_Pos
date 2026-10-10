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
//
//    KylGis POS is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with KylGis POS.  If not, see <http://www.gnu.org/licenses/>.
package com.mx.kylgis.pos.data.loader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

/**
 *
 * @author adrianromero
 * Created on February 6, 2007, 4:06 PM
 *
 */
public final class Session {

    private static final int CONNECTION_VALIDATION_TIMEOUT_SECONDS = 2;
    private static final long DEFAULT_CONNECTION_VALIDATION_INTERVAL_MILLIS = 60000L;
    private static final long CONNECTION_VALIDATION_INTERVAL_NANOS =
            validationIntervalMillis() * 1000000L;
    
    private final String m_surl;
    private final String m_sappuser;
    private final String m_spassword;
    
    private Connection m_c;
    private boolean m_bInTransaction;
    private long m_lastConnectionValidationNanos;

    /**
     *
     */
    public final SessionDB DB;
    
    /** Creates a new instance of Session
     * @param url
     * @param user
     * @param password
     * @throws java.sql.SQLException */
    public Session(String url, String user, String password) throws SQLException {
        m_surl = url;
        m_sappuser = user;
        m_spassword = password;
        
        m_c = null;
        m_bInTransaction = false;
        m_lastConnectionValidationNanos = 0L;
        
        connect(); // no lazy connection

        DB = getDiff();
    }
    
    /**
     *
     * @throws SQLException
     */
    public void connect() throws SQLException {
        
        // primero cerramos si no estabamos cerrados
        close();
        
        // creamos una nueva conexion.
        m_c = (m_sappuser == null && m_spassword == null)
        ? DriverManager.getConnection(m_surl)
        : DriverManager.getConnection(m_surl, m_sappuser, m_spassword);         
        m_c.setAutoCommit(true);
        m_bInTransaction = false;
        markConnectionValidation();
    }     

    /**
     *
     */
    public void close() {

        Connection connection = m_c;
        boolean rollbackPending = m_bInTransaction;
        m_c = null;
        m_bInTransaction = false;
        m_lastConnectionValidationNanos = 0L;

        if (connection != null) {
            try {
                if (rollbackPending) {
                    connection.rollback();
                }
            } catch (SQLException e) {
                // Closing the physical connection below is the safest fallback.
            } finally {
                try {
                    connection.close();
                } catch (SQLException e) {
                    // Nothing else can safely be done during close().
                }
            }
        }
    }

    /**
     *
     * @return
     * @throws SQLException
     */
    public Connection getConnection() throws SQLException {
        
        if (!m_bInTransaction) {
            ensureConnection();
        }
        return m_c;
    }
    
    /**
     *
     * @throws SQLException
     */
    public void begin() throws SQLException {
        
        if (m_bInTransaction) {
            throw new SQLException("Already in transaction");
        } else {
            ensureConnection();
            m_c.setAutoCommit(false);
            m_bInTransaction = true;
        }
    }

    /**
     *
     * @throws SQLException
     */
    public void commit() throws SQLException {
        if (!m_bInTransaction) {
            throw new SQLException("Transaction not started");
        }

        try {
            m_c.commit();
            m_c.setAutoCommit(true);
            m_bInTransaction = false;
            markConnectionValidation();
        } catch (SQLException ex) {
            discardConnectionAfterTransactionFailure();
            throw ex;
        }
    }

    /**
     *
     * @throws SQLException
     */
    public void rollback() throws SQLException {
        if (!m_bInTransaction) {
            throw new SQLException("Transaction not started");
        }

        try {
            m_c.rollback();
            m_c.setAutoCommit(true);
            m_bInTransaction = false;
            markConnectionValidation();
        } catch (SQLException ex) {
            discardConnectionAfterTransactionFailure();
            throw ex;
        }
    }

    /**
     *
     * @return
     */
    public boolean isTransaction() {
        return m_bInTransaction;
    }

    private void discardConnectionAfterTransactionFailure() {
        Connection failedConnection = m_c;
        m_c = null;
        m_bInTransaction = false;
        m_lastConnectionValidationNanos = 0L;

        if (failedConnection != null) {
            try {
                failedConnection.close();
            } catch (SQLException ignored) {
                // The connection is already unusable and must not be reused.
            }
        }
    }
    
    private void ensureConnection() throws SQLException {
        // isClosed() no detecta necesariamente un socket que el servidor cerro
        // por wait_timeout. Validar con isValid() en CADA sentencia, sin embargo,
        // agrega un ping JDBC al camino caliente. La validacion se limita a una
        // frecuencia maxima configurable (60 s por defecto), independientemente
        // de cuantas sentencias se ejecuten entre validaciones.
        boolean reconnect = m_c == null;

        if (!reconnect) {
            try {
                reconnect = m_c.isClosed();
                if (!reconnect && shouldValidateConnection()) {
                    try {
                        reconnect = !m_c.isValid(CONNECTION_VALIDATION_TIMEOUT_SECONDS);
                        if (!reconnect) {
                            markConnectionValidation();
                        }
                    } catch (SQLFeatureNotSupportedException | AbstractMethodError e) {
                        // Drivers JDBC antiguos pueden no implementar isValid().
                        // En ese caso conservamos el comportamiento historico y
                        // evitamos reintentar la comprobacion en cada sentencia.
                        markConnectionValidation();
                        reconnect = false;
                    }
                }
            } catch (SQLException e) {
                // Una excepcion durante la validacion tambien significa que la
                // conexion ya no es reutilizable de forma segura.
                reconnect = true;
            }
        }

        if (reconnect) {
            connect();
        }
    }

    private boolean shouldValidateConnection() {
        if (CONNECTION_VALIDATION_INTERVAL_NANOS <= 0L || m_lastConnectionValidationNanos == 0L) {
            return true;
        }
        long elapsed = System.nanoTime() - m_lastConnectionValidationNanos;
        return elapsed < 0L || elapsed >= CONNECTION_VALIDATION_INTERVAL_NANOS;
    }

    private void markConnectionValidation() {
        m_lastConnectionValidationNanos = System.nanoTime();
    }

    private static long validationIntervalMillis() {
        String configured = System.getProperty("kylgis.jdbc.validationIntervalMillis");
        if (configured == null || configured.trim().isEmpty()) {
            return DEFAULT_CONNECTION_VALIDATION_INTERVAL_MILLIS;
        }
        try {
            return Math.max(0L, Long.parseLong(configured.trim()));
        } catch (NumberFormatException ex) {
            return DEFAULT_CONNECTION_VALIDATION_INTERVAL_MILLIS;
        }
    }

    /**
     *
     * @return
     * @throws SQLException
     */
    public String getURL() throws SQLException {
        return getConnection().getMetaData().getURL();
    }

    private SessionDB getDiff() throws SQLException {

        String sdbmanager = getConnection().getMetaData().getDatabaseProductName();
        switch (sdbmanager) {
            case "HSQL Database Engine":
                return new SessionDBHSQLDB();
            case "MySQL":
                return new SessionDBMySQL();
            case "PostgreSQL":
                return new SessionDBPostgreSQL();
            case "Oracle":
                return new SessionDBOracle();
            case "Apache Derby":
                return new SessionDBDerby();
            default:
                return new SessionDBGeneric(sdbmanager);
        }
    }
}
