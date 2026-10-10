/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mx.kylgis.pos.sales.restaurant;

import com.mx.kylgis.pos.data.loader.Session;
import com.mx.kylgis.pos.forms.AppView;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

/**
 * Database helpers used by the restaurant table workflow.
 *
 * <p>Every operation requests the current connection from {@link Session} so
 * it benefits from the common reconnect policy. Statements and result sets are
 * method-local and closed deterministically; this avoids retaining JDBC
 * resources for the lifetime of the restaurant screen.</p>
 *
 * @author JDL
 */
public class RestaurantDBUtils {

    private final Session s;

    private interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    private interface RowReader<T> {
        T read(ResultSet resultSet) throws SQLException;
    }

    /**
     * Loads volatile restaurant table state in one query. The connection is
     * requested from Session for every refresh so the common JDBC reconnection
     * policy can replace sockets expired by wait_timeout.
     *
     * @return snapshot keyed by place ID, or null when the query fails
     */
    public Map<String, TableState> getPlacesStateSnapshot() {
        Map<String, TableState> snapshot = new HashMap<>();
        final String sql = "SELECT ID, CUSTOMER, WAITER, GUESTS, OCCUPIED FROM places";
        try (Statement stateStmt = s.getConnection().createStatement();
             ResultSet stateRs = stateStmt.executeQuery(sql)) {
            while (stateRs.next()) {
                snapshot.put(stateRs.getString("ID"), new TableState(
                        stateRs.getString("CUSTOMER"),
                        stateRs.getString("WAITER"),
                        stateRs.getInt("GUESTS"),
                        stateRs.getTimestamp("OCCUPIED")));
            }
        } catch (SQLException ex) {
            return null;
        }
        return snapshot;
    }

    /** Immutable state used by the restaurant map refresh. */
    public static final class TableState {
        private final String customer;
        private final String waiter;
        private final int guests;
        private final Timestamp occupied;

        private TableState(String customer, String waiter, int guests, Timestamp occupied) {
            this.customer = customer;
            this.waiter = waiter;
            this.guests = guests;
            this.occupied = occupied;
        }

        public String getCustomer() { return customer; }
        public String getWaiter() { return waiter; }
        public int getGuests() { return guests; }
        public Timestamp getOccupied() { return occupied; }
    }

    /** Package-private constructor used by isolated JDBC regression smokes. */
    RestaurantDBUtils(Session session) {
        this.s = session;
    }

    /**
     * @param oApp current POS application
     */
    public RestaurantDBUtils(AppView oApp) {
        this.s = oApp.getSession();
    }

    /**
     * @param newTable destination table
     * @param ticketID ticket being moved
     */
    public void moveCustomer(String newTable, String ticketID) {
        String oldTable = getTableDetails(ticketID);

        if (countTicketIdInTable(ticketID) > 1) {
            setCustomerNameInTable(getCustomerNameInTable(oldTable), newTable);
            setWaiterNameInTable(getWaiterNameInTable(oldTable), newTable);
            setTicketIdInTable(ticketID, newTable);
            setGuestsInTable(getGuestsInTable(oldTable), newTable);

            oldTable = getTableMovedName(ticketID);
            if ((oldTable != null) && (oldTable != newTable)) {
                clearCustomerNameInTable(oldTable);
                clearWaiterNameInTable(oldTable);
                clearTicketIdInTable(oldTable);
                clearTableMovedFlag(oldTable);
            } else {
                oldTable = getTableMovedName(ticketID);
                clearTableMovedFlag(oldTable);
            }
        }
    }

    public void setCustomerNameInTable(String custName, String tableName) {
        executeUpdate("UPDATE places SET CUSTOMER=? WHERE NAME=?", statement -> {
            statement.setString(1, custName);
            statement.setString(2, tableName);
        });
    }

    public void setCustomerNameInTableById(String custName, String tableID) {
        executeUpdate("UPDATE places SET CUSTOMER=? WHERE ID=?", statement -> {
            statement.setString(1, custName);
            statement.setString(2, tableID);
        });
    }

    public void setCustomerNameInTableByTicketId(String custName, String ticketID) {
        executeUpdate("UPDATE places SET CUSTOMER=? WHERE TICKETID=?", statement -> {
            statement.setString(1, custName);
            statement.setString(2, ticketID);
        });
    }

    public String getCustomerNameInTable(String tableName) {
        return queryOne("SELECT CUSTOMER FROM places WHERE NAME=?",
                statement -> statement.setString(1, tableName),
                resultSet -> resultSet.getString("CUSTOMER"), "");
    }

    public String getCustomerNameInTableById(String tableId) {
        return queryOne("SELECT CUSTOMER FROM places WHERE ID=?",
                statement -> statement.setString(1, tableId),
                resultSet -> resultSet.getString("CUSTOMER"), "");
    }

    public void clearCustomerNameInTable(String tableName) {
        executeUpdate("UPDATE places SET CUSTOMER=null WHERE NAME=?",
                statement -> statement.setString(1, tableName));
    }

    public void clearCustomerNameInTableById(String tableID) {
        executeUpdate("UPDATE places SET CUSTOMER=null WHERE ID=?",
                statement -> statement.setString(1, tableID));
    }

    public void setWaiterNameInTable(String waiterName, String tableName) {
        executeUpdate("UPDATE places SET WAITER=? WHERE NAME=?", statement -> {
            statement.setString(1, waiterName);
            statement.setString(2, tableName);
        });
    }

    public void setWaiterNameInTableById(String waiterName, String tableID) {
        executeUpdate("UPDATE places SET WAITER=? WHERE ID=?", statement -> {
            statement.setString(1, waiterName);
            statement.setString(2, tableID);
        });
    }

    public String getWaiterNameInTable(String tableName) {
        return queryOne("SELECT WAITER FROM places WHERE NAME=?",
                statement -> statement.setString(1, tableName),
                resultSet -> resultSet.getString("WAITER"), "");
    }

    public String getWaiterNameInTableById(String tableID) {
        return queryOne("SELECT WAITER FROM places WHERE ID=?",
                statement -> statement.setString(1, tableID),
                resultSet -> resultSet.getString("WAITER"), "");
    }

    public void clearWaiterNameInTable(String tableName) {
        executeUpdate("UPDATE places SET WAITER=null WHERE NAME=?",
                statement -> statement.setString(1, tableName));
    }

    public void clearWaiterNameInTableById(String tableID) {
        executeUpdate("UPDATE places SET WAITER=null WHERE ID=?",
                statement -> statement.setString(1, tableID));
    }

    public String getTicketIdInTable(String ID) {
        return queryOne("SELECT TICKETID FROM places WHERE ID=?",
                statement -> statement.setString(1, ID),
                resultSet -> resultSet.getString("TICKETID"), "");
    }

    public void setTicketIdInTable(String ticketID, String tableName) {
        executeUpdate("UPDATE places SET TICKETID=? WHERE NAME=?", statement -> {
            statement.setString(1, ticketID);
            statement.setString(2, tableName);
        });
    }

    public void clearTicketIdInTable(String tableName) {
        executeUpdate("UPDATE places SET TICKETID=null WHERE NAME=?",
                statement -> statement.setString(1, tableName));
    }

    public void clearTicketIdInTableById(String tableID) {
        executeUpdate("UPDATE places SET TICKETID=null WHERE ID=?",
                statement -> statement.setString(1, tableID));
    }

    public Integer getGuestsInTable(String tableID) {
        return queryOne("SELECT GUESTS FROM places WHERE ID=?",
                statement -> statement.setString(1, tableID),
                resultSet -> resultSet.getInt("GUESTS"), 0);
    }

    public void setGuestsInTable(Integer guests, String tableID) {
        executeUpdate("UPDATE places SET GUESTS=? WHERE ID=?", statement -> {
            statement.setInt(1, guests);
            statement.setString(2, tableID);
        });
    }

    public Integer updateGuestsInTable(String tableID) {
        return queryOne("SELECT GUESTS FROM places WHERE ID=?",
                statement -> statement.setString(1, tableID),
                resultSet -> resultSet.getInt("GUESTS"), 0);
    }

    public void clearGuestsInTable(String tableID) {
        executeUpdate("UPDATE places SET GUESTS=0 WHERE ID=?",
                statement -> statement.setString(1, tableID));
    }

    public void clearGuestsTable(String table) {
        executeUpdate("UPDATE places SET GUESTS=0 WHERE NAME=?",
                statement -> statement.setString(1, table));
    }

    public void clearOccupied(String tableID) {
        executeUpdate("UPDATE places SET OCCUPIED=null WHERE ID=?",
                statement -> statement.setString(1, tableID));
    }

    public void clearOccupiedTable(String table) {
        executeUpdate("UPDATE places SET OCCUPIED=null WHERE NAME=?",
                statement -> statement.setString(1, table));
    }

    public Timestamp getOccupied(String tableID) {
        return queryOne("SELECT OCCUPIED FROM places WHERE ID=?",
                statement -> statement.setString(1, tableID),
                resultSet -> resultSet.getTimestamp("OCCUPIED"), null);
    }

    public void setOccupied(String ticketID) {
        executeUpdate("UPDATE places SET OCCUPIED=NOW() WHERE TICKETID=?",
                statement -> statement.setString(1, ticketID));
    }

    public Integer countTicketIdInTable(String ticketID) {
        return queryOne("SELECT COUNT(*) AS RECORDCOUNT FROM places WHERE TICKETID=?",
                statement -> statement.setString(1, ticketID),
                resultSet -> resultSet.getInt("RECORDCOUNT"), 0);
    }

    public String getTableDetails(String ticketID) {
        return queryOne("SELECT NAME FROM places WHERE TICKETID=?",
                statement -> statement.setString(1, ticketID),
                resultSet -> resultSet.getString("NAME"), "");
    }

    public void setTableMovedFlag(String tableID) {
        executeUpdate("UPDATE places SET TABLEMOVED='true' WHERE ID=?",
                statement -> statement.setString(1, tableID));
    }

    public String getTableMovedName(String ticketID) {
        return queryOne("SELECT NAME FROM places WHERE TICKETID=? AND TABLEMOVED='true'",
                statement -> statement.setString(1, ticketID),
                resultSet -> resultSet.getString("NAME"), null);
    }

    public Boolean getTableMovedFlag(String ticketID) {
        return queryOne("SELECT TABLEMOVED FROM places WHERE TICKETID=?",
                statement -> statement.setString(1, ticketID),
                resultSet -> resultSet.getBoolean("TABLEMOVED"), false);
    }

    public void clearTableMovedFlag(String tableID) {
        executeUpdate("UPDATE places SET TABLEMOVED='false' WHERE NAME=?",
                statement -> statement.setString(1, tableID));
    }

    private void executeUpdate(String sql, StatementBinder binder) {
        try (PreparedStatement statement = s.getConnection().prepareStatement(sql)) {
            binder.bind(statement);
            statement.executeUpdate();
        } catch (SQLException ex) {
            // Preserve historical behavior: restaurant metadata failures do not
            // abort the sale flow. Callers continue with their existing fallback.
        }
    }

    private <T> T queryOne(String sql, StatementBinder binder,
            RowReader<T> reader, T fallback) {
        try (PreparedStatement statement = s.getConnection().prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? reader.read(resultSet) : fallback;
            }
        } catch (SQLException ex) {
            return fallback;
        }
    }
}
