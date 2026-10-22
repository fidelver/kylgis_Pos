package com.mx.kylgis.pos.forms;

import com.mx.kylgis.pos.basic.BasicException;
import com.mx.kylgis.pos.data.loader.Session;
import com.mx.kylgis.pos.data.loader.TransactionOutcomeUnknownException;
import com.mx.kylgis.pos.ticket.TicketInfo;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * ISOLATED LAB ONLY. Not a production payment ledger.
 * Explicit opt-in AND exact synthetic catalog gate. Never auto-migrates schema.
 */
final class SaleOperationLedgerQA {
    static final String ENABLE_PROPERTY = "kylgis.qa.durableLedger.enabled";
    static final String OPERATION_PROPERTY = "kylgis.qa.durableLedger.operationUuid";

    private SaleOperationLedgerQA() {}

    static boolean enabled() {
        return Boolean.getBoolean(ENABLE_PROPERTY);
    }

    private static Connection connection(Session session) throws SQLException {
        // The loopback-only override lets the QA COMMIT-ACK proxy carry the sale
        // while ledger writes go to the independently reachable synthetic DB.
        String qaDirect = System.getProperty("kylgis.qa.durableLedger.directJdbcUrl");
        Connection independent;
        if (qaDirect != null) {
            if (!qaDirect.startsWith("jdbc:mysql://127.0.0.1:3306/pos_qa_status?")
                    || !qaDirect.contains("useSSL=false")) {
                throw new SQLException("QA_LEDGER_REFUSE_DIRECT_URL");
            }
            independent = java.sql.DriverManager.getConnection(qaDirect, "root", "");
        } else {
            independent = session.openIndependentConnectionForQA();
        }
        try {
            if (!"pos_qa_status".equals(independent.getCatalog())) {
                throw new SQLException("QA_LEDGER_REFUSE_NON_SYNTHETIC_DATABASE");
            }
            if (!independent.getAutoCommit()) {
                throw new SQLException("QA_LEDGER_REQUIRES_INDEPENDENT_AUTOCOMMIT");
            }
            return independent;
        } catch (SQLException invalid) {
            independent.close();
            throw invalid;
        }
    }

    static void prepareAndClaim(Session session, TicketInfo ticket) throws BasicException {
        if (ticket == null || ticket.getId() == null || ticket.getId().isBlank()) {
            throw new BasicException("QA ledger requires a stable receipt UUID");
        }
        String id = ticket.getProperty(OPERATION_PROPERTY);
        if (id == null) {
            id = UUID.randomUUID().toString();
            ticket.setProperty(OPERATION_PROPERTY, id);
        }
        try {
            UUID.fromString(id);
        } catch (IllegalArgumentException invalid) {
            throw new BasicException("QA ledger operation UUID is invalid", invalid);
        }
        try (Connection c = connection(session)) {
            // Do not silently accept a receipt that belongs to another operation.
            try (PreparedStatement p = c.prepareStatement(
                    "INSERT INTO qa_sale_operation_intents"
                    + "(operation_uuid,receipt_id,terminal_id,state) VALUES (?,?,?,'PREPARED')")) {
                p.setString(1,id);
                p.setString(2,ticket.getId());
                p.setString(3,ticket.getActiveCash() == null ? "QA-UNKNOWN" : ticket.getActiveCash());
                p.executeUpdate();
            }
            int changed = transition(c, id, "PREPARED", "IN_FLIGHT", 0);
            if (changed != 1) {
                throw new SQLException("QA_LEDGER_PRECOMMIT_CLAIM_REJECTED");
            }
        } catch (SQLException failure) {
            // A lost PREPARE acknowledgement must never authorize the sale.
            throw new BasicException("QA durable operation preparation is not confirmed; no sale permitted", failure);
        }
    }

    private static int transition(Connection c, String id, String expected,
            String next, int version) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(
                "UPDATE qa_sale_operation_intents SET state=?,revision=revision+1"
                + " WHERE operation_uuid=? AND state=? AND revision=?")) {
            p.setString(1,next);
            p.setString(2,id);
            p.setString(3,expected);
            p.setInt(4,version);
            return p.executeUpdate();
        }
    }

    static void commitConfirmed(Session session, TicketInfo ticket) throws BasicException {
        String id = ticket.getProperty(OPERATION_PROPERTY);
        try (Connection c = connection(session)) {
            // A freshly opened connection verifies the durable receipt.
            try (PreparedStatement p = c.prepareStatement(
                    "SELECT COUNT(*) FROM receipts r INNER JOIN tickets t ON r.ID=t.ID WHERE r.ID=?")) {
                p.setString(1,ticket.getId());
                try (ResultSet results = p.executeQuery()) {
                    if (!results.next() || results.getInt(1) != 1) {
                        throw new SQLException("QA_LEDGER_COMMIT_CANNOT_VERIFY_RECEIPT");
                    }
                }
            }
            if (transition(c,id,"IN_FLIGHT","DB_CONFIRMED",1) != 1) {
                throw new SQLException("QA_LEDGER_COMMIT_STATE_NOT_RECONCILABLE");
            }
        } catch (SQLException failure) {
            // May already have committed. Deliberately require human reconciliation.
            throw new TransactionOutcomeUnknownException(
                    TransactionOutcomeUnknownException.Phase.COMMIT, failure);
        }
    }

    static void holdUnknown(Session session, TicketInfo ticket) {
        String id = ticket.getProperty(OPERATION_PROPERTY);
        if (id == null) return;
        try (Connection c = connection(session)) {
            transition(c,id,"IN_FLIGHT","COMMIT_UNKNOWN",1);
        } catch (SQLException failure) {
            // The precommitted IN_FLIGHT record is already a durable fail-closed barrier.
            java.util.logging.Logger.getLogger(SaleOperationLedgerQA.class.getName())
                    .warning("QA ledger could not mark UNKNOWN; operation remains IN_FLIGHT");
        }
    }
}
