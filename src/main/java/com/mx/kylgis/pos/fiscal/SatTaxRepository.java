package com.mx.kylgis.pos.fiscal;

import com.mx.kylgis.pos.data.loader.Session;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Prepared JDBC access to additive SAT tables. Deliberately does not alter
 * app transactions, auto-commit, or open a second uncoordinated Session.
 * Calling code must use the normal EDT for short editor actions; never run
 * this against the sales Session from a SwingWorker.
 */
public final class SatTaxRepository {
    private final Session session;

    public SatTaxRepository(Session session) {
        if (session == null) throw new IllegalArgumentException("Session required");
        this.session = session;
    }

    public boolean schemaAvailable() throws SQLException {
        Connection c = session.getConnection();
        DatabaseMetaData meta = c.getMetaData();
        for (String name : new String[]{"taxes_sat", "TAXES_SAT"}) {
            try (ResultSet rs = meta.getTables(c.getCatalog(), null, name, new String[]{"TABLE"})) {
                if (rs.next()) return true;
            }
        }
        return false;
    }

    public SatTaxProfile findTax(String taxId) throws SQLException {
        final String sql = "SELECT impuesto,tipo_factor,naturaleza,tasa_cuota,cuota_unidad,"
                + "vigencia_desde,vigencia_hasta,sat_enabled FROM taxes_sat WHERE tax_id=?";
        try (PreparedStatement st = session.getConnection().prepareStatement(sql)) {
            st.setString(1, taxId);
            try (ResultSet rs = st.executeQuery()) {
                if (!rs.next()) return null;
                String f = rs.getString(2);
                String n = rs.getString(3);
                Date from = rs.getDate(6);
                Date to = rs.getDate(7);
                return new SatTaxProfile(taxId, rs.getString(1),
                        f == null ? null : SatTaxProfile.Factor.valueOf(f),
                        n == null ? null : SatTaxProfile.Nature.valueOf(n),
                        rs.getBigDecimal(4), rs.getString(5),
                        from == null ? null : from.toLocalDate(),
                        to == null ? null : to.toLocalDate(),
                        rs.getBoolean(8));
            }
        }
    }

    /**
     * One fiscal profile is saved independently from legacy taxes. It is
     * intended only for already-persisted TAX_ID. No automatic seed/mapping.
     */
    public void saveTax(SatTaxProfile tax) throws SQLException {
        String update = "UPDATE taxes_sat SET impuesto=?,tipo_factor=?,naturaleza=?,"
                + "tasa_cuota=?,cuota_unidad=?,vigencia_desde=?,vigencia_hasta=?,sat_enabled=?"
                + " WHERE tax_id=?";
        Connection c = session.getConnection();
        String insert = "INSERT INTO taxes_sat (impuesto,tipo_factor,naturaleza,tasa_cuota,"
                + "cuota_unidad,vigencia_desde,vigencia_hasta,sat_enabled,tax_id)"
                + " VALUES (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement st = c.prepareStatement(insert)) {
            bindTax(st, tax);
            st.executeUpdate();
        } catch (SQLException race) {
            // Existing key: update its metadata; unrelated SQL errors still fail.
            if (!isDuplicateKey(race)) throw race;
            try (PreparedStatement st = c.prepareStatement(update)) {
                bindTax(st, tax);
                st.executeUpdate();
            }
        }
    }

    private static void bindTax(PreparedStatement st, SatTaxProfile tax) throws SQLException {
        st.setString(1, tax.getImpuesto());
        st.setString(2, tax.getFactor() == null ? null : tax.getFactor().name());
        st.setString(3, tax.getNature() == null ? null : tax.getNature().name());
        st.setBigDecimal(4, tax.getTasaCuota());
        st.setString(5, tax.getCuotaUnidad());
        st.setDate(6, tax.getDesde() == null ? null : Date.valueOf(tax.getDesde()));
        st.setDate(7, tax.getHasta() == null ? null : Date.valueOf(tax.getHasta()));
        st.setBoolean(8, tax.isEnabled());
        st.setString(9, tax.getTaxId());
    }

    public static final class ChildTax {
        public final String taxId;
        public final boolean cascade;
        public ChildTax(String taxId, boolean cascade) {
            this.taxId=taxId; this.cascade=cascade;
        }
    }

    public java.util.List<ChildTax> listChildren(String parentId) throws SQLException {
        java.util.List<ChildTax> out=new java.util.ArrayList<>();
        try (PreparedStatement st = session.getConnection().prepareStatement(
                "SELECT id,ratecascade FROM taxes WHERE parentid=?"
                + " ORDER BY CASE WHEN rateorder IS NULL THEN 1 ELSE 0 END,rateorder,id")) {
            st.setString(1,parentId);
            try (ResultSet rs=st.executeQuery()) {
                while(rs.next()) out.add(new ChildTax(rs.getString(1),rs.getBoolean(2)));
            }
        }
        return out;
    }

    public static final class ProductChoice {
        public final String id;
        public final String name;
        public final String code;
        public ProductChoice(String id, String name, String code) {
            this.id = id; this.name = name; this.code = code;
        }
        @Override
        public String toString() { return name + " (" + code + ")"; }
    }

    /** Bounded metadata-only search, never loading product image blobs. */
    public java.util.List<ProductChoice> searchProducts(String value) throws SQLException {
        if (value == null || value.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }
        String term = value.trim();
        java.util.List<ProductChoice> out = new java.util.ArrayList<>();
        try (PreparedStatement st = session.getConnection().prepareStatement(
                "SELECT id,name,code FROM products WHERE code=? OR reference=?"
                + " OR name LIKE ? ORDER BY name LIMIT 50")) {
            st.setString(1, term);
            st.setString(2, term);
            st.setString(3, "%" + term.replace("%", "").replace("_", "") + "%");
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    out.add(new ProductChoice(rs.getString(1),rs.getString(2),rs.getString(3)));
                }
            }
        }
        return out;
    }

    public SatConceptMetadata findProduct(String productId) throws SQLException {
        try (PreparedStatement st = session.getConnection().prepareStatement(
                "SELECT objeto_imp,clave_prodserv,clave_unidad,cuota_cantidad_unidad,cuota_unidad FROM products_sat WHERE product_id=?")) {
            st.setString(1, productId);
            try (ResultSet rs = st.executeQuery()) {
                return rs.next() ? new SatConceptMetadata(productId,
                        rs.getString(1), rs.getString(2), rs.getString(3), rs.getBigDecimal(4), rs.getString(5)) : null;
            }
        }
    }

    public void saveProduct(SatConceptMetadata data) throws SQLException {
        Connection c = session.getConnection();
        final String update = "UPDATE products_sat SET objeto_imp=?,clave_prodserv=?,clave_unidad=?,cuota_cantidad_unidad=?,cuota_unidad=? WHERE product_id=?";
        // INSERT first; UPDATE's count can be zero when existing values match.
        try (PreparedStatement st = c.prepareStatement(
                "INSERT INTO products_sat (objeto_imp,clave_prodserv,clave_unidad,cuota_cantidad_unidad,cuota_unidad,product_id) VALUES (?,?,?,?,?,?)")) {
            bindProduct(st, data);
            st.executeUpdate();
        } catch (SQLException race) {
            if (!isDuplicateKey(race)) throw race;
            try (PreparedStatement st = c.prepareStatement(update)) {
                bindProduct(st, data);
                st.executeUpdate();
            }
        }
    }

    private static boolean isDuplicateKey(SQLException error) {
        return "23505".equals(error.getSQLState())
                || "23000".equals(error.getSQLState())
                || error.getErrorCode() == 1062;
    }

    private static void bindProduct(PreparedStatement st, SatConceptMetadata p) throws SQLException {
        st.setString(1, p.getObjetoImp());
        st.setString(2, p.getClaveProdServ());
        st.setString(3, p.getClaveUnidad());
        st.setBigDecimal(4, p.getCuotaCantidadPorUnidad());
        st.setString(5, p.getCuotaUnidad());
        st.setString(6, p.getProductId());
    }
}