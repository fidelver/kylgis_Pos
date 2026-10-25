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

    /**
     * Persists the operation identity before any payment component can reach a
     * gateway. This method deliberately leaves the operation in PREPARED; the
     * final CAS claim remains in prepareAndClaim after all payments are bound.
     */
    static void prepareOperation(Session session, TicketInfo ticket) throws BasicException {
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
        final String requestHash;
        try {
            requestHash = SaleOperationPayloadQA.operationFingerprint(ticket);
        } catch (java.io.IOException invalid) {
            throw new BasicException("QA request preparation is not confirmed", invalid);
        }
        try (Connection c = connection(session)) {
            try (PreparedStatement p = c.prepareStatement(
                    "INSERT INTO qa_sale_operation_intents"
                    + "(operation_uuid,receipt_id,terminal_id,state,request_sha256) VALUES (?,?,?,'PREPARED',?)")) {
                p.setString(1, id);
                p.setString(2, ticket.getId());
                p.setString(3, ticket.getActiveCash() == null ? "QA-UNKNOWN" : ticket.getActiveCash());
                p.setString(4, requestHash);
                try {
                    p.executeUpdate();
                } catch (SQLException duplicate) {
                    if (duplicate.getErrorCode() != 1062
                            || !"23000".equals(duplicate.getSQLState())) {
                        throw duplicate;
                    }
                }
            }
            try (PreparedStatement p = c.prepareStatement(
                    "SELECT receipt_id,terminal_id,state,revision,request_sha256"
                    + " FROM qa_sale_operation_intents WHERE operation_uuid=?")) {
                p.setString(1, id);
                try (ResultSet r = p.executeQuery()) {
                    String terminal = ticket.getActiveCash() == null ? "QA-UNKNOWN" : ticket.getActiveCash();
                    if (!r.next() || !ticket.getId().equals(r.getString(1))
                            || !terminal.equals(r.getString(2))
                            || !"PREPARED".equals(r.getString(3)) || r.getLong(4) != 0
                            || !requestHash.equals(r.getString(5))) {
                        throw new SQLException("QA_LEDGER_RESERVED_IDENTITY_MISMATCH_OR_NOT_PREPARED");
                    }
                }
            }
        } catch (SQLException failure) {
            throw new BasicException("QA durable operation preparation is not confirmed; no sale permitted", failure);
        }
    }

    /**
     * Binds a deterministic payment component and its caller-supplied provider
     * reference before the gateway is invoked.
     */
    static void prepareExternalIntent(Session session, TicketInfo ticket, int component,
            com.mx.kylgis.pos.payment.PaymentInfoMagcard payment) throws BasicException {
        if (ticket == null || payment == null || component < 0) {
            throw new BasicException("QA payment intent requires operation, component and payment");
        }
        String operation = ticket.getProperty(OPERATION_PROPERTY);
        String providerReference = payment.getTransactionID();
        if (operation == null || providerReference == null || providerReference.isBlank()
                || providerReference.length() > 128) {
            throw new BasicException("QA payment intent requires a stable provider reference");
        }
        final String paymentUuid = UUID.nameUUIDFromBytes(
                (operation + ":payment:" + component).getBytes(java.nio.charset.StandardCharsets.UTF_8))
                .toString();
        try (Connection c = connection(session)) {
            try (PreparedStatement p = c.prepareStatement(
                    "INSERT INTO qa_external_payment_intents"
                    + "(payment_uuid,operation_uuid,provider,provider_reference,component_index,amount,currency,state)"
                    + " VALUES (?,?,?, ?,?,?, 'MXN','PREPARED')")) {
                p.setString(1, paymentUuid);
                p.setString(2, operation);
                p.setString(3, "card-gateway");
                p.setString(4, providerReference);
                p.setInt(5, component);
                p.setBigDecimal(6, java.math.BigDecimal.valueOf(payment.getTotal()));
                try {
                    p.executeUpdate();
                } catch (SQLException duplicate) {
                    if (duplicate.getErrorCode() != 1062
                            || !"23000".equals(duplicate.getSQLState())) {
                        throw duplicate;
                    }
                }
            }
            try (PreparedStatement p = c.prepareStatement(
                    "SELECT operation_uuid,provider,provider_reference,component_index,amount,currency,state"
                    + " FROM qa_external_payment_intents WHERE payment_uuid=?")) {
                p.setString(1, paymentUuid);
                try (ResultSet r = p.executeQuery()) {
                    if (!r.next()
                            || !operation.equals(r.getString(1))
                            || !"card-gateway".equals(r.getString(2))
                            || !providerReference.equals(r.getString(3))
                            || r.getInt(4) != component
                            || r.getBigDecimal(5).compareTo(java.math.BigDecimal.valueOf(payment.getTotal())) != 0
                            || !"MXN".equals(r.getString(6))
                            || !"PREPARED".equals(r.getString(7))) {
                        throw new SQLException("QA_LEDGER_PAYMENT_INTENT_NOT_PREPARED");
                    }
                }
            }
        } catch (SQLException failure) {
            throw new BasicException("QA durable payment preparation is not confirmed; no authorization permitted", failure);
        }
    }

    static void finishExternalIntent(Session session, TicketInfo ticket, int component,
            com.mx.kylgis.pos.payment.PaymentInfoMagcard payment) throws BasicException {
        String operation = ticket == null ? null : ticket.getProperty(OPERATION_PROPERTY);
        String providerReference = payment == null ? null : payment.getTransactionID();
        if (operation == null || providerReference == null || providerReference.isBlank()) {
            throw new BasicException("QA payment result has no durable identity");
        }
        String paymentUuid = UUID.nameUUIDFromBytes(
                (operation + ":payment:" + component).getBytes(java.nio.charset.StandardCharsets.UTF_8))
                .toString();
        String next = payment.isPaymentOK() ? "APPROVED" : "DECLINED";
        try (Connection c = connection(session);
             PreparedStatement p = c.prepareStatement(
                     "UPDATE qa_external_payment_intents SET state=?"
                     + " WHERE payment_uuid=? AND operation_uuid=? AND provider_reference=? AND state='PREPARED'")) {
            p.setString(1, next);
            p.setString(2, paymentUuid);
            p.setString(3, operation);
            p.setString(4, providerReference);
            if (p.executeUpdate() != 1) {
                throw new SQLException("QA_LEDGER_PAYMENT_RESULT_NOT_RECORDED");
            }
        } catch (SQLException failure) {
            throw new BasicException("QA durable payment result is not confirmed", failure);
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
        final String requestHash;
        try { requestHash = SaleOperationPayloadQA.operationFingerprint(ticket); }
        catch (java.io.IOException invalid) { throw new BasicException("QA request preparation is not confirmed", invalid); }
        try (Connection c = connection(session)) {
            // Do not silently accept a receipt that belongs to another operation.
            try (PreparedStatement p = c.prepareStatement(
                    "INSERT INTO qa_sale_operation_intents"
                    + "(operation_uuid,receipt_id,terminal_id,state,request_sha256) VALUES (?,?,?,'PREPARED',?)")) {
                p.setString(1,id);
                p.setString(2,ticket.getId());
                p.setString(3,ticket.getActiveCash() == null ? "QA-UNKNOWN" : ticket.getActiveCash());
                p.setString(4,requestHash);
                try {
                    p.executeUpdate();
                } catch (SQLException duplicate) {
                    if (duplicate.getErrorCode() != 1062
                            || !"23000".equals(duplicate.getSQLState())) throw duplicate;
                    // A pre-payment QA coordinator may have durably reserved the
                    // exact identity. A duplicate alone never authorizes a claim.
                }
            }
            try (PreparedStatement p = c.prepareStatement(
                    "SELECT receipt_id,terminal_id,state,revision,request_sha256 FROM qa_sale_operation_intents WHERE operation_uuid=?")) {
                p.setString(1,id);
                try (ResultSet r = p.executeQuery()) {
                    String terminal = ticket.getActiveCash() == null ? "QA-UNKNOWN" : ticket.getActiveCash();
                    if (!r.next() || !ticket.getId().equals(r.getString(1))
                            || !terminal.equals(r.getString(2))
                            || !"PREPARED".equals(r.getString(3)) || r.getLong(4) != 0
                            || !requestHash.equals(r.getString(5))) {
                        throw new SQLException("QA_LEDGER_RESERVED_IDENTITY_MISMATCH_OR_NOT_PREPARED");
                    }
                }
            }
            java.util.Set<Integer> external = new java.util.HashSet<>();
            try (PreparedStatement p = c.prepareStatement(
                    "SELECT state,currency,amount,component_index,provider_reference FROM qa_external_payment_intents WHERE operation_uuid=?")) {
                p.setString(1,id);
                try (ResultSet r = p.executeQuery()) {
                    while (r.next()) {
                        int index = r.getInt(4);
                        if (r.wasNull() || index < 0 || index >= ticket.getPayments().size()
                                || !external.add(index)
                                || !("APPROVED".equals(r.getString(1)) || "RECONCILED".equals(r.getString(1)))
                                || !"MXN".equals(r.getString(2)) || r.getString(5) == null || r.getString(5).isBlank())
                            throw new SQLException("QA_LEDGER_BANK_RECONCILIATION_REQUIRED");
                        com.mx.kylgis.pos.payment.PaymentInfo payment = ticket.getPayments().get(index);
                        if (!("magcard".equals(payment.getName()) || "ccard".equals(payment.getName()))
                                || r.getBigDecimal(3).compareTo(java.math.BigDecimal.valueOf(payment.getTotal())) != 0)
                            throw new SQLException("QA_LEDGER_BANK_COMPONENT_AMOUNT_MISMATCH");
                    }
                }
            }
            for (int i=0;i<ticket.getPayments().size();i++) {
                String name = ticket.getPayments().get(i).getName();
                if (("magcard".equals(name) || "ccard".equals(name)) && !external.contains(i))
                    throw new SQLException("QA_LEDGER_EXTERNAL_INTENT_MISSING");
                if ("voucher".equals(name))
                    throw new SQLException("QA_LEDGER_LEGACY_VOUCHER_TENDER_REQUIRES_REVIEW");
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

    static String paymentRowId(Session session, TicketInfo ticket, int component) throws BasicException {
        String operation = ticket.getProperty(OPERATION_PROPERTY);
        try (Connection c = connection(session); PreparedStatement p = c.prepareStatement(
                "SELECT payment_uuid FROM qa_external_payment_intents WHERE operation_uuid=? AND component_index=?")) {
            p.setString(1, operation);
            p.setInt(2, component);
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) {
                    String payment = r.getString(1);
                    try { UUID.fromString(payment); }
                    catch (IllegalArgumentException invalid) { throw new SQLException("QA_PAYMENT_IDENTITY_INVALID", invalid); }
                    return payment;
                }
            }
            return UUID.nameUUIDFromBytes((operation + ":payment:" + component)
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
        } catch (SQLException failure) {
            throw new BasicException("QA payment identity preparation is not confirmed", failure);
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
