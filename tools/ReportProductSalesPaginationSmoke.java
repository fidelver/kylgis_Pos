import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import net.sf.jasperreports.engine.JRPrintElement;
import net.sf.jasperreports.engine.JRPrintImage;
import net.sf.jasperreports.engine.JRPrintPage;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

/**
 * Exact boundary test for the 15-product report split.
 * Synthetic only; no database, printer, or POS session.
 */
public final class ReportProductSalesPaginationSmoke {
    private static final int PRODUCTS_PER_PAGE = 15;

    private static List<Map<String, ?>> rows(int count) {
        List<Map<String, ?>> result = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("REFERENCE", String.format("R%04d", i));
            row.put("NAME", String.format("Producto de prueba número %03d", i));
            row.put("UNITS", (double) ((i % 9) + 1));
            row.put("TOTAL", (double) i * 10);
            result.add(row);
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Use: ReportProductSalesPaginationSmoke <jrxml>");
        JasperReport template;
        try (InputStream in = Files.newInputStream(Path.of(args[0]))) {
            template = JasperCompileManager.compileReport(in);
        }
        ResourceBundle resources = ResourceBundle.getBundle(
                "com.mx.kylgis.pos.reports.sales_messages", Locale.forLanguageTag("es-MX"));
        int[] cases = {0, 1, 14, 15, 16, 29, 30, 31, 37, 45, 46, 120};
        int tests = 0;
        for (int size : cases) {
            Map<String, Object> params = new HashMap<>();
            params.put(JRParameter.REPORT_RESOURCE_BUNDLE, resources);
            params.put(JRParameter.REPORT_LOCALE, Locale.forLanguageTag("es-MX"));
            params.put("ARG", new Object[]{
                new Object[]{null, java.sql.Timestamp.valueOf("2026-03-18 00:00:00"),
                             null, java.sql.Timestamp.valueOf("2026-03-31 23:59:59")}
            });
            JasperPrint print = JasperFillManager.fillReport(
                    template, params, new JRMapCollectionDataSource(rows(size)));
            int expected = (size + PRODUCTS_PER_PAGE - 1) / PRODUCTS_PER_PAGE;
            if (print.getPages().size() != expected) {
                throw new AssertionError("count=" + size + " expected pages=" + expected
                    + " actual=" + print.getPages().size());
            }
            for (int i = 0; i < expected; i++) {
                JRPrintPage page = print.getPages().get(i);
                int charts = 0;
                for (JRPrintElement element : page.getElements()) {
                    if (element instanceof JRPrintImage) charts++;
                }
                if (charts != 1) {
                    throw new AssertionError("count=" + size + " page=" + (i + 1)
                        + " expected one chart actual=" + charts);
                }
            }
            System.out.println("PRODUCT_SALES_PAGINATION_PASS rows=" + size
                + " pages=" + print.getPages().size());
            tests++;
        }
        System.out.println("PRODUCT_SALES_PAGINATION_QA_PASS cases=" + tests
            + " perPage=" + PRODUCTS_PER_PAGE);
    }
}
