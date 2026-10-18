import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
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

/** Datos ficticios sin conexión ni credenciales; prueba contenido, no cifras de negocio. */
public final class ReportContentSmoke {
    private ReportContentSmoke() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Expected report template and output PDF");
        }
        Path template = Path.of(args[0]);
        Path destination = Path.of(args[1]);
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("CATEGORY", "KYLQA-CATEGORIA-SINTETICA");
        record.put("CATEGORYNAME", "KYLQA-CATEGORIA-SINTETICA");
        record.put("REFERENCE", "KYLQA-REFERENCIA-001");
        record.put("CODE", "KYLQA-CODIGO-001");
        record.put("NAME", "KYLQA-PRODUCTO-CONTENIDO-001");
        record.put("UNITS", Double.valueOf(2.5));
        record.put("PRICEBUY", Double.valueOf(12.25));
        record.put("PRICESELL", Double.valueOf(27.5));
        record.put("TAX", "IVA");
        record.put("TAXRATE", Double.valueOf(1.16));
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(JRParameter.REPORT_LOCALE, Locale.forLanguageTag("es-MX"));
        ResourceBundle bundle = ResourceBundle.getBundle(
            "com.mx.kylgis.pos.reports.products_messages",
            Locale.forLanguageTag("es-MX")
        );
        parameters.put(JRParameter.REPORT_RESOURCE_BUNDLE, bundle);
        JasperReport report;
        try (var stream = Files.newInputStream(template)) {
            report = JasperCompileManager.compileReport(stream);
        }
        JasperPrint printable = JasperFillManager.fillReport(
            report, parameters, new JRMapCollectionDataSource(List.of(record)));
        if (printable.getPages().size() < 1) {
            throw new IllegalStateException("El reporte no contiene páginas con el registro sintético");
        }
        byte[] pdf = JasperExportManager.exportReportToPdf(printable);
        if (pdf.length < 5 || pdf[0] != '%' || pdf[1] != 'P' ||
                pdf[2] != 'D' || pdf[3] != 'F') {
            throw new IllegalStateException("Firma PDF inválida");
        }
        Files.write(destination, pdf);
        System.out.println("KYLGIS_REPORT_CONTENT_RENDER pages=" +
            printable.getPages().size() + " bytes=" + pdf.length);
    }
}
