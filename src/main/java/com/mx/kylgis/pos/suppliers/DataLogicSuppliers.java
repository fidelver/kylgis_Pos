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
package com.mx.kylgis.pos.suppliers;

import com.mx.kylgis.pos.basic.BasicException;
import com.mx.kylgis.pos.data.loader.*;
import com.mx.kylgis.pos.format.Formats;
import com.mx.kylgis.pos.forms.AppLocal;
import com.mx.kylgis.pos.forms.BeanFactoryDataSingle;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author JG uniCenta
 */
public class DataLogicSuppliers extends BeanFactoryDataSingle {
    
    /**
     * Main Method for supplier object
     */
    protected Session s;
    private TableDefinition tsuppliers;
    private static final Datas[] supplierdatas = new Datas[] {
        Datas.STRING, 
        Datas.STRING, 
        Datas.STRING,
        Datas.STRING, 
        Datas.STRING, 
        Datas.STRING,         
        Datas.STRING};
    
    /**
     *
     * @param s
     */
    @Override
    public void init(Session s){
        this.s = s;
        tsuppliers = new TableDefinition(s
            , "suppliers"
            , new String[] { 
                "ID", 
                "SEARCHKEY", 
                "TAXID", 
                "NAME", 
                "MAXDEBT", 
                "ADDRESS",
                "ADDRESS2",
                "POSTAL",
                "CITY",
                "REGION",
                "COUNTRY",
                "FIRSTNAME",
                "LASTNAME",
                "EMAIL",
                "PHONE",
                "PHONE2",
                "FAX",
                "NOTES", 
                "VISIBLE", 
                "CURDATE", 
                "CURDEBT",
                "VATID"  }
            , new String[] { 
                "ID", 
                AppLocal.getIntString("label.searchkey"),
                AppLocal.getIntString("label.suppliertaxid"),
                AppLocal.getIntString("label.name"),

                AppLocal.getIntString("label.maxdebt"),
                AppLocal.getIntString("label.address"),
                AppLocal.getIntString("label.address2"),
                AppLocal.getIntString("label.postal"),
                
                AppLocal.getIntString("label.city"),
                AppLocal.getIntString("label.region"),
                AppLocal.getIntString("label.country"), 
                AppLocal.getIntString("label.firstname"),

                AppLocal.getIntString("label.lastname"),
                AppLocal.getIntString("label.email"),
                AppLocal.getIntString("label.phone"),
                AppLocal.getIntString("label.phone2"),

                AppLocal.getIntString("label.fax"),
                AppLocal.getIntString("label.notes"),
                "VISIBLE",
                AppLocal.getIntString("label.curdate"),

                AppLocal.getIntString("label.curdebt"),
                AppLocal.getIntString("label.suppliervatid") }
            , new Datas[] { 
                Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, 
                Datas.DOUBLE, Datas.STRING, Datas.STRING, Datas.STRING, 
                Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING,
                Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING,
                Datas.STRING, Datas.STRING, Datas.BOOLEAN, Datas.TIMESTAMP,                 
                Datas.DOUBLE, Datas.STRING }
            , new Formats[] {
                Formats.STRING, Formats.STRING, Formats.STRING, Formats.STRING,
                Formats.CURRENCY, Formats.STRING, Formats.STRING, Formats.STRING,
                Formats.STRING, Formats.STRING, Formats.STRING, Formats.STRING,                
                Formats.STRING, Formats.STRING, Formats.STRING, Formats.STRING,
                Formats.STRING, Formats.STRING, Formats.BOOLEAN, Formats.TIMESTAMP,
                Formats.CURRENCY, Formats.STRING }
            , new int[] {0}
        );   
    }

    /**
     *
     * @return supplier data
     */
        public SentenceList getSupplierList() {
        return new StaticSentence(s
            , new QBFBuilder("SELECT "
                    + "ID, SEARCHKEY, TAXID, NAME, "
                    + "POSTAL, PHONE, EMAIL "
                    + "FROM suppliers "
                    + "WHERE VISIBLE = " + s.DB.TRUE() + " AND ?(QBF_FILTER) ORDER BY NAME"
                , new String[] {"SEARCHKEY", "TAXID", "NAME", "POSTAL", "PHONE", "EMAIL"})
            , new SerializerWriteBasic(new Datas[] {
                Datas.OBJECT, Datas.STRING, 
                Datas.OBJECT, Datas.STRING, 
                Datas.OBJECT, Datas.STRING, 
                Datas.OBJECT, Datas.STRING, 
                Datas.OBJECT, Datas.STRING, 
                Datas.OBJECT, Datas.STRING})
            , (DataRead dr) -> {
                SupplierInfo s1 = new SupplierInfo(dr.getString(1));
            s1.setSearchkey(dr.getString(2));
            s1.setTaxid(dr.getString(3));
            s1.setName(dr.getString(4));
            s1.setPostal(dr.getString(5));
            s1.setPhone(dr.getString(6));
            s1.setEmail(dr.getString(7));
            return s1;
        });
    }
        
    /**
     *
     * @return supplier data
     */
        public SentenceList getSuppList() {
        return new StaticSentence(s
            , new QBFBuilder("SELECT "
                    + "ID, SEARCHKEY, TAXID, NAME, "
                    + "POSTAL, PHONE, EMAIL "
                    + "FROM suppliers "
                    + "WHERE VISIBLE = " + s.DB.TRUE() + " ORDER BY NAME"
                , new String[] {"SEARCHKEY", "TAXID", "NAME", "POSTAL", "PHONE", "EMAIL"})
            , new SerializerWriteBasic(new Datas[] {
                Datas.OBJECT, Datas.STRING, 
                Datas.OBJECT, Datas.STRING, 
                Datas.OBJECT, Datas.STRING, 
                Datas.OBJECT, Datas.STRING, 
                Datas.OBJECT, Datas.STRING, 
                Datas.OBJECT, Datas.STRING})
            , (DataRead dr) -> {
                SupplierInfo s1 = new SupplierInfo(dr.getString(1));
            s1.setSearchkey(dr.getString(2));
            s1.setTaxid(dr.getString(3));
            s1.setName(dr.getString(4));
            s1.setPostal(dr.getString(5));
            s1.setPhone(dr.getString(6));
            s1.setEmail(dr.getString(7));
            return s1;
        });
    }        
        
    /**
     * Returns every supplier associated with a product. Preferred suppliers are
     * listed first, followed by explicit priority and supplier name.
     */
    public List<ProductSupplierInfo> getProductSuppliers(String productId) throws BasicException {
        if (productId == null || productId.isBlank()) {
            throw new BasicException("Product supplier lookup requires a product id.");
        }

        List<ProductSupplierInfo> result = new ArrayList<>();
        String sql = "SELECT ps.product_id, ps.supplier_id, s.name, s.searchkey, "
                + "ps.is_preferred, ps.active, ps.supplier_sku, ps.purchase_uom, "
                + "ps.last_cost, ps.priority, ps.last_purchase "
                + "FROM product_suppliers ps "
                + "JOIN suppliers s ON s.id = ps.supplier_id "
                + "WHERE ps.product_id = ? "
                + "ORDER BY ps.is_preferred DESC, "
                + "CASE WHEN ps.priority IS NULL THEN 1 ELSE 0 END, ps.priority, s.name";
        try (PreparedStatement statement = s.getConnection().prepareStatement(sql)) {
            statement.setString(1, productId);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    Object cost = rows.getObject(9);
                    Object priority = rows.getObject(10);
                    result.add(new ProductSupplierInfo(
                            rows.getString(1),
                            rows.getString(2),
                            rows.getString(3),
                            rows.getString(4),
                            rows.getBoolean(5),
                            rows.getBoolean(6),
                            rows.getString(7),
                            rows.getString(8),
                            cost == null ? null : ((Number) cost).doubleValue(),
                            priority == null ? null : ((Number) priority).intValue(),
                            rows.getTimestamp(11)));
                }
            }
        } catch (SQLException ex) {
            throw new BasicException("Unable to load product suppliers.", ex);
        }
        return result;
    }

    /**
     * Atomically replaces a product's supplier associations. The historical
     * products.supplier field remains synchronized with the preferred active
     * supplier for compatibility with legacy product rows. stockdiary is never
     * changed here because it records the supplier actually used per movement.
     */
    public void replaceProductSuppliers(final String productId,
            List<ProductSupplierInfo> associations,
            Set<String> expectedSupplierIds) throws BasicException {
        replaceProductSuppliersInternal(productId, associations, expectedSupplierIds, null);
    }

    /**
     * Replaces supplier associations only if relationship state is unchanged
     * since the editor loaded it. Purchase metadata is deliberately excluded
     * from the optimistic snapshot so receiving stock does not create a false
     * conflict while the supplier dialog is open.
     */
    public void replaceProductSuppliers(final String productId,
            List<ProductSupplierInfo> associations,
            List<ProductSupplierInfo> expectedAssociations) throws BasicException {
        Map<String, String> expectedState = relationshipState(expectedAssociations);
        replaceProductSuppliersInternal(productId, associations,
                expectedState == null ? null : expectedState.keySet(), expectedState);
    }

    private void replaceProductSuppliersInternal(final String productId,
            List<ProductSupplierInfo> associations,
            Set<String> expectedSupplierIds,
            Map<String, String> expectedRelationshipState) throws BasicException {
        if (productId == null || productId.isBlank()) {
            throw new BasicException("Product supplier update requires a product id.");
        }

        final List<ProductSupplierInfo> requested = associations == null
                ? new ArrayList<>() : new ArrayList<>(associations);
        final Set<String> expectedIds = expectedSupplierIds == null
                ? null : new HashSet<>(expectedSupplierIds);
        final Map<String, String> expectedState = expectedRelationshipState == null
                ? null : new HashMap<>(expectedRelationshipState);
        final Set<String> uniqueIds = new HashSet<>();
        String explicitPreferred = null;
        String firstActive = null;

        for (ProductSupplierInfo association : requested) {
            if (association == null || association.getSupplierId() == null
                    || association.getSupplierId().isBlank()) {
                throw new BasicException("Product supplier update contains an empty supplier id.");
            }
            String supplierId = association.getSupplierId().trim();
            if (!uniqueIds.add(supplierId)) {
                throw new BasicException("Product supplier update contains a duplicate supplier: "
                        + supplierId);
            }
            if (association.isActive() && firstActive == null) {
                firstActive = supplierId;
            }
            if (association.isPreferred()) {
                if (!association.isActive()) {
                    throw new BasicException("The preferred supplier must be active.");
                }
                if (explicitPreferred != null) {
                    throw new BasicException("Only one preferred supplier is allowed per product.");
                }
                explicitPreferred = supplierId;
            }
        }

        final String preferredSupplier = explicitPreferred == null ? firstActive : explicitPreferred;

        new Transaction<Void>(s) {
            @Override
            protected Void transact() throws BasicException {
                try {
                    Connection connection = s.getConnection();
                    verifyProductExists(connection, productId);
                    Set<String> currentIds = loadProductSupplierIds(connection, productId);
                    if (expectedIds != null && !expectedIds.equals(currentIds)) {
                        throw new BasicException(AppLocal.getIntString(
                                "message.productsuppliers.concurrentchange"));
                    }
                    if (expectedState != null
                            && !expectedState.equals(loadProductSupplierState(connection, productId))) {
                        throw new BasicException(AppLocal.getIntString(
                                "message.productsuppliers.concurrentchange"));
                    }
                    verifySuppliersExist(connection, uniqueIds);

                    // Apply only relationship-state changes to rows that already exist.
                    // Purchase metadata such as last_cost/last_purchase is learned by
                    // inventory transactions and must never be overwritten by a dialog
                    // that may have been open while purchases were being received.
                    try (PreparedStatement deactivate = connection.prepareStatement(
                            "UPDATE product_suppliers SET is_preferred = ?, active = ? "
                            + "WHERE product_id = ? AND supplier_id = ?")) {
                        for (String currentId : currentIds) {
                            if (!uniqueIds.contains(currentId)) {
                                deactivate.setBoolean(1, false);
                                deactivate.setBoolean(2, false);
                                deactivate.setString(3, productId);
                                deactivate.setString(4, currentId);
                                deactivate.addBatch();
                            }
                        }
                        deactivate.executeBatch();
                    }

                    String updateSql = "UPDATE product_suppliers SET is_preferred = ?, active = ? "
                            + "WHERE product_id = ? AND supplier_id = ?";
                    String insertSql = "INSERT INTO product_suppliers "
                            + "(product_id, supplier_id, is_preferred, active, supplier_sku, "
                            + "purchase_uom, last_cost, priority, last_purchase) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement update = connection.prepareStatement(updateSql);
                            PreparedStatement insert = connection.prepareStatement(insertSql)) {
                        for (ProductSupplierInfo association : requested) {
                            String supplierId = association.getSupplierId().trim();
                            if (currentIds.contains(supplierId)) {
                                update.setBoolean(1, supplierId.equals(preferredSupplier));
                                update.setBoolean(2, association.isActive());
                                update.setString(3, productId);
                                update.setString(4, supplierId);
                                update.executeUpdate();
                                continue;
                            }

                            insert.setString(1, productId);
                            insert.setString(2, supplierId);
                            insert.setBoolean(3, supplierId.equals(preferredSupplier));
                            insert.setBoolean(4, association.isActive());
                            insert.setString(5, association.getSupplierSku());
                            insert.setString(6, association.getPurchaseUom());
                            if (association.getLastCost() == null) {
                                insert.setNull(7, Types.DOUBLE);
                            } else {
                                insert.setDouble(7, association.getLastCost());
                            }
                            if (association.getPriority() == null) {
                                insert.setNull(8, Types.INTEGER);
                            } else {
                                insert.setInt(8, association.getPriority());
                            }
                            if (association.getLastPurchase() == null) {
                                insert.setNull(9, Types.TIMESTAMP);
                            } else {
                                insert.setTimestamp(9, association.getLastPurchase());
                            }
                            insert.executeUpdate();
                        }
                    }

                    try (PreparedStatement legacy = connection.prepareStatement(
                            "UPDATE products SET supplier = ? WHERE id = ?")) {
                        if (preferredSupplier == null) {
                            legacy.setNull(1, Types.VARCHAR);
                        } else {
                            legacy.setString(1, preferredSupplier);
                        }
                        legacy.setString(2, productId);
                        if (legacy.executeUpdate() != 1) {
                            throw new SQLException("Product row was not updated exactly once.");
                        }
                    }
                    return null;
                } catch (SQLException ex) {
                    throw new BasicException("Unable to update product suppliers.", ex);
                }
            }
        }.execute();
    }

    private static void verifyProductExists(Connection connection, String productId)
            throws SQLException {
        // Serialize supplier-list replacement with product edits and purchase
        // learning. The product row is the common lock for every operation that
        // can change products.supplier or product_suppliers for this product.
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM products WHERE id = ? FOR UPDATE")) {
            statement.setString(1, productId);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) {
                    throw new SQLException("Unknown product: " + productId);
                }
            }
        }
    }

    private static Map<String, String> relationshipState(
            List<ProductSupplierInfo> associations) throws BasicException {
        if (associations == null) {
            return null;
        }
        Map<String, String> state = new HashMap<>();
        for (ProductSupplierInfo association : associations) {
            if (association == null || association.getSupplierId() == null
                    || association.getSupplierId().isBlank()) {
                throw new BasicException("Expected product supplier snapshot contains an empty supplier id.");
            }
            String supplierId = association.getSupplierId().trim();
            String previous = state.put(supplierId,
                    relationshipState(association.isPreferred(), association.isActive()));
            if (previous != null) {
                throw new BasicException("Expected product supplier snapshot contains a duplicate supplier: "
                        + supplierId);
            }
        }
        return state;
    }

    private static Map<String, String> loadProductSupplierState(Connection connection,
            String productId) throws SQLException {
        Map<String, String> state = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT supplier_id, is_preferred, active FROM product_suppliers WHERE product_id = ?")) {
            statement.setString(1, productId);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    state.put(rows.getString(1), relationshipState(
                            rows.getBoolean(2), rows.getBoolean(3)));
                }
            }
        }
        return state;
    }

    private static String relationshipState(boolean preferred, boolean active) {
        return (preferred ? "1" : "0") + (active ? "1" : "0");
    }

    private static Set<String> loadProductSupplierIds(Connection connection, String productId)
            throws SQLException {
        Set<String> supplierIds = new HashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT supplier_id FROM product_suppliers WHERE product_id = ?")) {
            statement.setString(1, productId);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    supplierIds.add(rows.getString(1));
                }
            }
        }
        return supplierIds;
    }

    private static void verifySuppliersExist(Connection connection, Set<String> supplierIds)
            throws SQLException {
        if (supplierIds.isEmpty()) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM suppliers WHERE id = ?")) {
            for (String supplierId : supplierIds) {
                statement.setString(1, supplierId);
                try (ResultSet rows = statement.executeQuery()) {
                    if (!rows.next()) {
                        throw new SQLException("Unknown supplier: " + supplierId);
                    }
                }
            }
        }
    }

    /**
     * Updates extended notes for a supplier.
     *
     * @param supplier supplier to update
     * @return affected rows
     * @throws BasicException on database failure
     */
    public int updateSupplierExt(final SupplierInfoExt supplier) throws BasicException {
     
        return new PreparedSentence(s
                , "UPDATE suppliers SET NOTES = ? WHERE ID = ?"
                , SerializerWriteParams.INSTANCE )
                .exec(new DataParams() {
                @Override
                    public void writeValues() throws BasicException {
                        setString(1, supplier.getNotes());
                        setString(2, supplier.getID());
                    }
                });        
    }
    public final TableDefinition getTableSuppliers() {
        return tsuppliers;
    }
    
    /**
     * JG Aug 2017 - Return Supplier Id - sId param
     * @param sId
     * @return
     * @throws BasicException
     */
        @SuppressWarnings("unchecked")
    public final List<SupplierTransaction> getSuppliersTransactionList(String sId) throws BasicException {
                    
        return new PreparedSentence(s,               
                "SELECT "
                    + "stockdiary.datenew, "
                    + "products.NAME, "
                    + "stockdiary.units, "
                    + "stockdiary.price, "
                    + "stockdiary.reason, "
                    + "suppliers.id "                        
                + "FROM (stockdiary stockdiary "
                        + "INNER JOIN suppliers suppliers "
                        + "ON (stockdiary.supplier = suppliers.id)) "
                        + "INNER JOIN products products "
                        + "ON (stockdiary.product = products.ID) "
                + "WHERE suppliers.id = ? " 
                    + "GROUP BY "
                        + "stockdiary.datenew, "
                        + "products.NAME, " 
                        + "stockdiary.reason "
                + "ORDER BY stockdiary.datenew DESC",
                    SerializerWriteString.INSTANCE,                
                        SupplierTransaction.getSerializerRead()).list(sId);
    }    
}