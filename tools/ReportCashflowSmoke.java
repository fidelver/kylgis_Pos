import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;

import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

/** Prueba sintética de fechas, tipo de pago y suma; sin base de datos. */
public final class ReportCashflowSmoke {
    private ReportCashflowSmoke() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Expected JRXML and temporary PDF");
        }
        Map<String, Object> record = new HashMap<>();
        record.put("PAYMENT", "cash");
        record.put("PAYMENT_TOTAL", Double.valueOf(1234.50));
        Map<String, Object> params = new HashMap<>();
        params.put(JRParameter.REPORT_LOCALE, Locale.forLanguageTag("es-MX"));
        params.put(JRParameter.REPORT_RESOURCE_BUNDLE, ResourceBundle.getBundle(
            "com.mx.kylgis.pos.reports.sales_messages", Locale.forLanguageTag("es-MX")));
        // PanelReportBean passes qbffilter.createValue() as ARG; it contains
        // a nested date comparison array: [kind, start, kind, end].
        Object[] interval = {
            null, Timestamp.valueOf("2026-10-01 00:00:00"),
            null, Timestamp.valueOf("2026-10-18 00:00:00")
        };
        params.put("ARG", new Object[]{interval});
        JasperReport compiled;
        try (var source = Files.newInputStream(Path.of(args[0]))) {
            compiled = JasperCompileManager.compileReport(source);
        }
        JasperPrint rendered = JasperFillManager.fillReport(
            compiled, params, new JRMapCollectionDataSource(List.of(record)));
        if (rendered.getPages().isEmpty()) {
            throw new IllegalStateException("cashflow returned empty pages");
        }
        byte[] pdf = JasperExportManager.exportReportToPdf(rendered);
        if (pdf.length < 5 || pdf[0] != '%' || pdf[1] != 'P') {
            throw new IllegalStateException("PDF signature invalid");
        }
        Files.write(Path.of(args[1]), pdf);
        System.out.println("KYLGIS_CASHFLOW_RENDER pages=" + rendered.getPages().size()
            + " pdf_bytes=" + pdf.length);
    }
}
