package com.mx.kylgis.pos.fiscal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * Writes immutable SAT calculation snapshots for new ticket lines. Must be
 * called INSIDE DataLogicSales.saveTicket transaction after ticketlines insert
 * and before its commit. Never backfill/overwrite historical data.
 */
public final class SatTaxSnapshotWriter {
    private SatTaxSnapshotWriter() {}

    public static void insert(Connection c, String ticketId, int lineNo,
            SatConceptMetadata concept, List<SatTaxCalculator.Line> lines)
            throws SQLException {
        if (c == null || ticketId == null || ticketId.isBlank() || lineNo < 0
                || concept == null || lines == null) {
            throw new IllegalArgumentException("Identidad o resultado fiscal incompleto");
        }
        if (c.getAutoCommit()) {
            throw new SQLException("Snapshot SAT exige la transacción activa de la venta");
        }
        try (PreparedStatement s = c.prepareStatement(
                "INSERT INTO ticketlines_sat (ticket_id,line_no,objeto_imp,clave_prodserv,clave_unidad)"
                + " VALUES (?,?,?,?,?)")) {
            s.setString(1, ticketId);
            s.setInt(2, lineNo);
            s.setString(3, concept.getObjetoImp());
            s.setString(4, concept.getClaveProdServ());
            s.setString(5, concept.getClaveUnidad());
            s.executeUpdate();
        }
        try (PreparedStatement s = c.prepareStatement(
                "INSERT INTO ticketlines_sat_taxes (ticket_id,line_no,tax_id,impuesto,tipo_factor,"
                + "naturaleza,tasa_cuota,cuota_unidad,base,cantidad_fiscal,importe)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?,?)")) {
            for (SatTaxCalculator.Line line : lines) {
                SatTaxProfile p = line.profile();
                if (!p.isEnabled()) {
                    throw new IllegalStateException("Impuesto SAT no habilitado: " + p.getTaxId());
                }
                s.setString(1, ticketId);
                s.setInt(2, lineNo);
                s.setString(3, p.getTaxId());
                s.setString(4, p.getImpuesto());
                s.setString(5, p.getFactor().name());
                s.setString(6, p.getNature().name());
                s.setBigDecimal(7, p.getTasaCuota());
                s.setString(8, p.getCuotaUnidad());
                s.setBigDecimal(9, line.base());
                s.setBigDecimal(10, line.cantidadFiscal());
                s.setBigDecimal(11, line.importe());
                s.addBatch();
            }
            s.executeBatch();
        }
    }
}