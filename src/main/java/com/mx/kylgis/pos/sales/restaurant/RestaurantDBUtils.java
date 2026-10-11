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
import java.util.Objects;

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
        final String sql = "SELECT P.ID, P.CUSTOMER, P.WAITER, P.GUESTS, P.OCCUPIED, "
                + "EXISTS (SELECT 1 FROM sharedtickets S WHERE S.ID = P.ID) AS HAS_TICKET "
                + "FROM places P";
        try (Statement stateStmt = s.getConnection().createStatement();
             ResultSet stateRs = stateStmt.executeQuery(sql)) {
            while (stateRs.next()) {
                snapshot.put(stateRs.getString("ID"), new TableState(
                        stateRs.getString("CUSTOMER"),
                        stateRs.getString("WAITER"),
                        stateRs.getInt("GUESTS"),
                        stateRs.getTimestamp("OCCUPIED"),
                        stateRs.getBoolean("HAS_TICKET")));
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
        private final boolean hasTicket;

        private TableState(String customer, String waiter, int guests, Timestamp occupied,
                boolean hasTicket) {
            this.customer = customer;
            this.waiter = waiter;
            this.guests = guests;
            this.occupied = occupied;
            this.hasTicket = hasTicket;
        }

        public String getCustomer() { return customer; }
        public String getWaiter() { return waiter; }
        public int getGuests() { return guests; }
        public Timestamp getOccupied() { return occupied; }
        public boolean hasTicket() { return hasTicket; }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof TableState)) {
                return false;
            }
            TableState other = (TableState) obj;
            return guests == other.guests
                    && hasTicket == other.hasTicket
                    && Objects.equals(customer, other.customer)
                    && Objects.equals(waiter, other.waiter)
                    && Objects.equals(occupied, other.occupied);
        }

        @Override
        public int hashCode() {
            return Objects.hash(customer, waiter, guests, occupied, hasTicket);
        }
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
     * Transfers restaurant metadata after a ticket was successfully moved to
     * an empty table. Nothing is cleared from the source until the destination
     * update succeeds.
     *
     * @param sourceTableID source place ID
     * @param targetTableID destination place ID
     * @return true when both rows were updated atomically
     */
    public boolean moveTableState(String sourceTableID, String targetTableID) {
        if (sourceTableID == null || targetTableID == null) {
            return false;
        }
        if (sourceTableID.equals(targetTableID)) {
            return true;
        }

        TransferState source = getTransferState(sourceTableID);
        if (source == null) {
            return false;
        }
        return transferTableState(sourceTableID, targetTableID, source);
    }

    /**
     * Merges metadata after two restaurant tickets were merged. The target
     * identity/customer/waiter/occupied time wins when already present, while
     * guests are added. Source metadata is cleared only after the target update.
     *
     * @param sourceTableID source place ID
     * @param targetTableID destination place ID
     * @return true when both rows were updated atomically
     */
    public boolean mergeTableState(String sourceTableID, String targetTableID) {
        if (sourceTableID == null || targetTableID == null) {
            return false;
        }
        if (sourceTableID.equals(targetTableID)) {
            return true;
        }

        TransferState source = getTransferState(sourceTableID);
        TransferState target = getTransferState(targetTableID);
        if (source == null || target == null) {
            return false;
        }

        TransferState merged = new TransferState(
                firstNonBlank(target.customer, source.customer),
                firstNonBlank(target.waiter, source.waiter),
                target.ticketID,
                Math.max(0, target.guests) + Math.max(0, source.guests),
                target.occupied != null ? target.occupied : source.occupied);
        return transferTableState(sourceTableID, targetTableID, merged);
    }

    private boolean transferTableState(String sourceTableID, String targetTableID,
            TransferState targetState) {
        boolean ownTransaction = !s.isTransaction();
        try {
            if (ownTransaction) {
                s.begin();
            }

            int targetCount;
            try (PreparedStatement statement = s.getConnection().prepareStatement(
                    "UPDATE places SET CUSTOMER=?, WAITER=?, TICKETID=?, TABLEMOVED=FALSE, "
                    + "GUESTS=?, OCCUPIED=? WHERE ID=?")) {
                statement.setString(1, targetState.customer);
                statement.setString(2, targetState.waiter);
                statement.setString(3, targetState.ticketID);
                statement.setInt(4, targetState.guests);
                statement.setTimestamp(5, targetState.occupied);
                statement.setString(6, targetTableID);
                targetCount = statement.executeUpdate();
            }
            if (targetCount != 1) {
                throw new SQLException("Destination restaurant table not found: " + targetTableID);
            }

            int sourceCount;
            try (PreparedStatement statement = s.getConnection().prepareStatement(
                    "UPDATE places SET CUSTOMER=NULL, WAITER=NULL, TICKETID=NULL, "
                    + "TABLEMOVED=FALSE, GUESTS=0, OCCUPIED=NULL WHERE ID=?")) {
                statement.setString(1, sourceTableID);
                sourceCount = statement.executeUpdate();
            }
            if (sourceCount != 1) {
                throw new SQLException("Source restaurant table not found: " + sourceTableID);
            }

            if (ownTransaction) {
                s.commit();
            }
            return true;
        } catch (SQLException ex) {
            if (ownTransaction && s.isTransaction()) {
                try {
                    s.rollback();
                } catch (SQLException ignored) {
                    // Preserve the original failure as the transfer result.
                }
            }
            return false;
        }
    }

    private TransferState getTransferState(String tableID) {
        return queryOne(
                "SELECT CUSTOMER, WAITER, TICKETID, GUESTS, OCCUPIED FROM places WHERE ID=?",
                statement -> statement.setString(1, tableID),
                resultSet -> new TransferState(
                        resultSet.getString("CUSTOMER"),
                        resultSet.getString("WAITER"),
                        resultSet.getString("TICKETID"),
                        resultSet.getInt("GUESTS"),
                        resultSet.getTimestamp("OCCUPIED")),
                null);
    }

    private static String firstNonBlank(String preferred, String fallback) {
        return preferred == null || preferred.trim().isEmpty() ? fallback : preferred;
    }

    private static final class TransferState {
        private final String customer;
        private final String waiter;
        private final String ticketID;
        private final int guests;
        private final Timestamp occupied;

        private TransferState(String customer, String waiter, String ticketID,
                int guests, Timestamp occupied) {
            this.customer = customer;
            this.waiter = waiter;
            this.ticketID = ticketID;
            this.guests = guests;
            this.occupied = occupied;
        }
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

    /**
     * Assigns a ticket to a table while preserving the original occupied time
     * when the same ticket is reopened. A new/different ticket always gets a
     * fresh timestamp, which also repairs stale OCCUPIED values left on free
     * tables by historical versions.
     */
    public void assignTicketToTable(String ticketID, String tableName) {
        executeUpdate("UPDATE places SET "
                + "OCCUPIED=CASE WHEN TICKETID IS NULL OR TICKETID<>? OR OCCUPIED IS NULL "
                + "THEN CURRENT_TIMESTAMP ELSE OCCUPIED END, "
                + "TICKETID=?, TABLEMOVED=FALSE WHERE NAME=?", statement -> {
            statement.setString(1, ticketID);
            statement.setString(2, ticketID);
            statement.setString(3, tableName);
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
        executeUpdate("UPDATE places SET OCCUPIED=CURRENT_TIMESTAMP "
                + "WHERE TICKETID=? AND OCCUPIED IS NULL",
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
