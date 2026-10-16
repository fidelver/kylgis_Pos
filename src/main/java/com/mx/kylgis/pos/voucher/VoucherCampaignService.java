//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.voucher;

import com.mx.kylgis.pos.basic.BasicException;
import com.mx.kylgis.pos.data.loader.Session;
import com.mx.kylgis.pos.data.loader.Transaction;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Transactional creation and reporting of voucher campaigns. */
public final class VoucherCampaignService {

    public static final String MODE_CUSTOMERS = "CUSTOMERS";
    public static final String MODE_QUANTITY = "QUANTITY";

    private final Session session;

    public VoucherCampaignService(Session session) {
        this.session = session;
    }

    public List<CustomerTarget> listVisibleCustomers() throws BasicException {
        List<CustomerTarget> result = new ArrayList<>();
        try (PreparedStatement ps = session.getConnection().prepareStatement(
                "SELECT ID, NAME, PHONE, PHONE2, EMAIL FROM customers "
                + "WHERE VISIBLE = TRUE ORDER BY NAME, ID");
                ResultSet rows = ps.executeQuery()) {
            while (rows.next()) {
                result.add(new CustomerTarget(rows.getString(1), rows.getString(2),
                        rows.getString(3), rows.getString(4), rows.getString(5)));
            }
            return result;
        } catch (SQLException ex) {
            throw new BasicException("Unable to list customers for voucher campaign.", ex);
        }
    }

    public CampaignResult createForCustomers(String name, String codePrefix, double amount,
            Date expiresAt, List<String> customerIds, String operatorId) throws BasicException {
        if (customerIds == null || customerIds.isEmpty()) {
            throw new BasicException("Select at least one customer for the voucher campaign.");
        }
        return createCampaign(name, codePrefix, amount, expiresAt,
                MODE_CUSTOMERS, customerIds.size(), new ArrayList<>(customerIds), operatorId);
    }

    public CampaignResult createByQuantity(String name, String codePrefix, double amount,
            Date expiresAt, int quantity, String operatorId) throws BasicException {
        return createCampaign(name, codePrefix, amount, expiresAt,
                MODE_QUANTITY, quantity, Collections.emptyList(), operatorId);
    }

    private CampaignResult createCampaign(final String name, final String codePrefix,
            final double amount, final Date expiresAt, final String mode, final int quantity,
            final List<String> customerIds, final String operatorId) throws BasicException {
        final String campaignName = name == null ? "" : name.trim();
        final String prefix = VoucherCodeGenerator.normalize(codePrefix);
        if (campaignName.isEmpty()) {
            throw new BasicException("Campaign name is required.");
        }
        if (campaignName.length() > 255) {
            throw new BasicException("Campaign name cannot exceed 255 characters.");
        }
        if (prefix.isEmpty()) {
            throw new BasicException("Campaign voucher prefix is required.");
        }
        if (!Double.isFinite(amount) || amount <= 0.0) {
            throw new BasicException("Campaign voucher amount must be greater than zero.");
        }
        if (expiresAt != null && expiresAt.getTime() <= System.currentTimeMillis()) {
            throw new BasicException("Campaign expiry must be in the future.");
        }
        if (quantity < 1 || quantity > 10000) {
            throw new BasicException("Campaign quantity must be between 1 and 10000 vouchers.");
        }
        if (operatorId == null || operatorId.isBlank()) {
            throw new BasicException("Campaign creation requires a valid operator.");
        }
        VoucherCampaignCodeGenerator.generate(prefix, quantity - 1);

        final String campaignId = UUID.randomUUID().toString();
        final Date createdAt = new Date();
        Transaction transaction = new Transaction(session) {
            @Override
            public Object transact() throws BasicException {
                try {
                    Connection connection = session.getConnection();
                    List<CustomerTarget> targets = MODE_CUSTOMERS.equals(mode)
                            ? resolveCustomers(connection, customerIds)
                            : Collections.emptyList();
                    if (MODE_CUSTOMERS.equals(mode) && targets.size() != quantity) {
                        throw new BasicException("One or more selected customers are no longer available.");
                    }

                    insertCampaign(connection, campaignId, campaignName, prefix, mode,
                            amount, quantity, expiresAt, createdAt, operatorId);

                    List<CampaignVoucherRow> rows = new ArrayList<>(quantity);
                    try (PreparedStatement insertVoucher = connection.prepareStatement(
                            "INSERT INTO vouchers "
                            + "(ID, VOUCHER_NUMBER, CUSTOMER, AMOUNT, STATUS, EXPIRES_AT, CAMPAIGN_ID) "
                            + "VALUES (?, ?, ?, ?, 'A', ?, ?)")) {
                        for (int i = 0; i < quantity; i++) {
                            CustomerTarget target = MODE_CUSTOMERS.equals(mode) ? targets.get(i) : null;
                            String code = VoucherCampaignCodeGenerator.generate(prefix, i);
                            insertVoucher.setString(1, UUID.randomUUID().toString());
                            insertVoucher.setString(2, code);
                            insertVoucher.setString(3, target == null ? null : target.id);
                            insertVoucher.setDouble(4, amount);
                            if (expiresAt == null) {
                                insertVoucher.setTimestamp(5, null);
                            } else {
                                insertVoucher.setTimestamp(5, new Timestamp(expiresAt.getTime()));
                            }
                            insertVoucher.setString(6, campaignId);
                            insertVoucher.executeUpdate();
                            rows.add(new CampaignVoucherRow(campaignId, campaignName,
                                    target == null ? null : target.id,
                                    target == null ? "" : target.name,
                                    target == null ? "" : target.preferredPhone(),
                                    target == null ? "" : target.email,
                                    code, amount, "A", expiresAt));
                        }
                    }
                    return new CampaignResult(campaignId, campaignName, mode, rows);
                } catch (SQLException ex) {
                    throw new BasicException("Voucher campaign could not be created. No vouchers were saved.", ex);
                }
            }
        };
        return (CampaignResult) transaction.execute();
    }

    public List<CampaignSummary> listCampaigns() throws BasicException {
        List<CampaignSummary> result = new ArrayList<>();
        String sql = "SELECT ID, NAME, ASSIGNMENT_MODE, QUANTITY, STATUS, CREATED_AT "
                + "FROM voucher_campaigns ORDER BY CREATED_AT DESC, NAME, ID";
        try (PreparedStatement ps = session.getConnection().prepareStatement(sql);
                ResultSet rows = ps.executeQuery()) {
            while (rows.next()) {
                Timestamp created = rows.getTimestamp(6);
                result.add(new CampaignSummary(rows.getString(1), rows.getString(2),
                        rows.getString(3), rows.getInt(4), rows.getString(5),
                        created == null ? null : new Date(created.getTime())));
            }
            return result;
        } catch (SQLException ex) {
            throw new BasicException("Unable to list voucher campaigns.", ex);
        }
    }

    public List<CampaignVoucherRow> loadCampaignReport(String campaignId) throws BasicException {
        if (campaignId == null || campaignId.isBlank()) {
            return Collections.emptyList();
        }
        String sql = "SELECT VC.ID, VC.NAME, V.CUSTOMER, COALESCE(C.NAME, ''), "
                + "COALESCE(NULLIF(C.PHONE, ''), NULLIF(C.PHONE2, ''), ''), COALESCE(C.EMAIL, ''), "
                + "V.VOUCHER_NUMBER, V.AMOUNT, V.STATUS, V.EXPIRES_AT "
                + "FROM vouchers V JOIN voucher_campaigns VC ON VC.ID = V.CAMPAIGN_ID "
                + "LEFT JOIN customers C ON C.ID = V.CUSTOMER WHERE VC.ID = ? "
                + "ORDER BY V.VOUCHER_NUMBER";
        List<CampaignVoucherRow> result = new ArrayList<>();
        try (PreparedStatement ps = session.getConnection().prepareStatement(sql)) {
            ps.setString(1, campaignId);
            try (ResultSet rows = ps.executeQuery()) {
                while (rows.next()) {
                    Timestamp expiry = rows.getTimestamp(10);
                    result.add(new CampaignVoucherRow(rows.getString(1), rows.getString(2),
                            rows.getString(3), rows.getString(4), rows.getString(5), rows.getString(6),
                            rows.getString(7), rows.getDouble(8), rows.getString(9),
                            expiry == null ? null : new Date(expiry.getTime())));
                }
            }
            return result;
        } catch (SQLException ex) {
            throw new BasicException("Unable to load voucher campaign report.", ex);
        }
    }

    private static List<CustomerTarget> resolveCustomers(Connection connection,
            List<String> customerIds) throws SQLException, BasicException {
        List<CustomerTarget> targets = new ArrayList<>(customerIds.size());
        Set<String> seen = new HashSet<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT ID, NAME, PHONE, PHONE2, EMAIL FROM customers WHERE ID = ? AND VISIBLE = TRUE")) {
            for (String customerId : customerIds) {
                if (customerId == null || customerId.isBlank()) {
                    throw new BasicException("Campaign contains an invalid customer selection.");
                }
                if (!seen.add(customerId)) {
                    throw new BasicException("Campaign contains a duplicated customer selection: " + customerId);
                }
                ps.setString(1, customerId);
                try (ResultSet rows = ps.executeQuery()) {
                    if (!rows.next()) {
                        throw new BasicException("Selected customer is no longer available: " + customerId);
                    }
                    targets.add(new CustomerTarget(rows.getString(1), rows.getString(2),
                            rows.getString(3), rows.getString(4), rows.getString(5)));
                    if (rows.next()) {
                        throw new BasicException("Customer identity is not unique: " + customerId);
                    }
                }
            }
        }
        return targets;
    }

    private static void insertCampaign(Connection connection, String campaignId, String name,
            String prefix, String mode, double amount, int quantity, Date expiresAt,
            Date createdAt, String operatorId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO voucher_campaigns "
                + "(ID, NAME, CODE_PREFIX, ASSIGNMENT_MODE, AMOUNT, QUANTITY, STATUS, "
                + "EXPIRES_AT, CREATED_AT, CREATED_BY) VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE', ?, ?, ?)")) {
            ps.setString(1, campaignId);
            ps.setString(2, name);
            ps.setString(3, prefix);
            ps.setString(4, mode);
            ps.setDouble(5, amount);
            ps.setInt(6, quantity);
            if (expiresAt == null) {
                ps.setTimestamp(7, null);
            } else {
                ps.setTimestamp(7, new Timestamp(expiresAt.getTime()));
            }
            ps.setTimestamp(8, new Timestamp(createdAt.getTime()));
            ps.setString(9, operatorId);
            ps.executeUpdate();
        }
    }

    public static final class CampaignSummary {
        public final String id;
        public final String name;
        public final String assignmentMode;
        public final int quantity;
        public final String status;
        public final Date createdAt;

        CampaignSummary(String id, String name, String assignmentMode, int quantity,
                String status, Date createdAt) {
            this.id = id;
            this.name = name == null ? "" : name;
            this.assignmentMode = assignmentMode == null ? "" : assignmentMode;
            this.quantity = quantity;
            this.status = status == null ? "" : status;
            this.createdAt = createdAt == null ? null : new Date(createdAt.getTime());
        }

        @Override
        public String toString() {
            String when = createdAt == null ? "" : createdAt.toString();
            return name + " — " + quantity + " cupones" + (when.isEmpty() ? "" : " — " + when);
        }
    }

    public static final class CustomerTarget {
        public final String id;
        public final String name;
        public final String phone;
        public final String phone2;
        public final String email;

        CustomerTarget(String id, String name, String phone, String phone2, String email) {
            this.id = id;
            this.name = name == null ? "" : name;
            this.phone = phone == null ? "" : phone;
            this.phone2 = phone2 == null ? "" : phone2;
            this.email = email == null ? "" : email;
        }

        public String preferredPhone() {
            return phone.isBlank() ? phone2 : phone;
        }
    }

    public static final class CampaignVoucherRow {
        public final String campaignId;
        public final String campaignName;
        public final String customerId;
        public final String customerName;
        public final String phone;
        public final String email;
        public final String voucherCode;
        public final double amount;
        public final String status;
        public final Date expiresAt;

        CampaignVoucherRow(String campaignId, String campaignName, String customerId,
                String customerName, String phone, String email, String voucherCode,
                double amount, String status, Date expiresAt) {
            this.campaignId = campaignId;
            this.campaignName = campaignName;
            this.customerId = customerId;
            this.customerName = customerName;
            this.phone = phone;
            this.email = email;
            this.voucherCode = voucherCode;
            this.amount = amount;
            this.status = status;
            this.expiresAt = expiresAt == null ? null : new Date(expiresAt.getTime());
        }
    }

    public static final class CampaignResult {
        public final String campaignId;
        public final String campaignName;
        public final String assignmentMode;
        public final List<CampaignVoucherRow> rows;

        CampaignResult(String campaignId, String campaignName, String assignmentMode,
                List<CampaignVoucherRow> rows) {
            this.campaignId = campaignId;
            this.campaignName = campaignName;
            this.assignmentMode = assignmentMode;
            this.rows = Collections.unmodifiableList(new ArrayList<>(rows));
        }
    }
}
