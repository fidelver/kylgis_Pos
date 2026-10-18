import java.nio.file.*;
import java.sql.Timestamp;
import java.util.*;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

/** Generates temporary synthetic PDFs and checks selected business-context fields. */
public final class ReportFinanceContentSmoke {
    private static final Timestamp START = Timestamp.valueOf("2026-10-01 00:00:00");
    private static final Timestamp END = Timestamp.valueOf("2026-10-18 00:00:00");

    private static Map<String, Object> data(Object... values) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            out.put((String) values[i], values[i + 1]);
        }
        return out;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("reports dir, temporary PDF dir");
        Path root = Path.of(args[0]);
        Path outdir = Path.of(args[1]);
        Locale locale = Locale.forLanguageTag("es-MX");
        Object[] dateFilter = {null, START, null, END};
        Map<String, Object> params = new HashMap<>();
        params.put(JRParameter.REPORT_LOCALE, locale);
        params.put("ARG", new Object[]{dateFilter});
        params.put("KYLGIS_REPORT_PERIOD", "01/10/2026 00:00 - 18/10/2026 00:00 (fin exclusivo)");
        fill(root, outdir, "sales_extproducts", "sales_messages", params, List.of(
            data("CUSTname","QA CLIENTE UNO", "CATname","QA CATEGORIA", "PRODref","QA-REF-1",
                 "PRODname","QA PRODUCTO PRIMERO", "TOTALunits",2.0, "TOTALnet",150.25,
                 "TOTALtax",24.04, "TOTALgross",174.29),
            data("CUSTname","QA CLIENTE UNO", "CATname","QA CATEGORIA", "PRODref","QA-REF-2",
                 "PRODname","QA PRODUCTO SEGUNDO", "TOTALunits",1.0, "TOTALnet",40.00,
                 "TOTALtax",6.40, "TOTALgross",46.40)));
        fill(root, outdir, "sales_closedpos_1", "sales_messages", params, List.of(
            data("TOTAL",200.0, "HOST","QA-CAJA", "HOSTSEQUENCE",42,
                 "MONEY","QA-CIERRE", "DATESTART",START, "DATEEND",END,
                 "PAYMENT","cash")));
        fill(root, outdir, "sales_productsalesprofit", "sales_messages", params, List.of(
            data("REFERENCE","QA-REF-PROFIT", "NAME","QA PRODUCTO UTILIDAD",
                 "PRICEBUY",15.0, "PRICESELL",20.0, "SOLD_UNITS",2.0,
                 "COST_VALUE",30.0, "EXPECTED_SALES_VALUE",40.0,
                 "ACTUAL_SALES_VALUE",42.0, "EXPECTED_PROFIT",10.0,
                 "ACTUAL_PROFIT",12.0)));
        fill(root, outdir, "vouchers", "vouchers_messages", params, List.of(
            data("NAME","QA BONO", "STARTDATE",START, "ENDDATE",END,
                 "TOTAL",32.50, "TICKETID","900", "RECEIPT","QA-RECIBO")));
    }

    private static void fill(Path root, Path output, String name, String bundle,
                             Map<String, Object> shared, List<Map<String, Object>> rows) throws Exception {
        Map<String, Object> params = new HashMap<>(shared);
        params.put(JRParameter.REPORT_RESOURCE_BUNDLE, ResourceBundle.getBundle(
            "com.mx.kylgis.pos.reports." + bundle,
            (Locale) shared.get(JRParameter.REPORT_LOCALE)));
        JasperReport compiled = JasperCompileManager.compileReport(
            Files.newInputStream(root.resolve(name + ".jrxml")));
        JasperPrint result = JasperFillManager.fillReport(
            compiled, params, new JRMapCollectionDataSource(new ArrayList<Map<String,?>>(rows)));
        if (result.getPages().isEmpty()) {
            throw new IllegalStateException("No pages with nonempty data: " + name);
        }
        byte[] pdf = JasperExportManager.exportReportToPdf(result);
        if (pdf.length < 5 || pdf[0] != '%' || pdf[1] != 'P' ||
                pdf[2] != 'D' || pdf[3] != 'F') {
            throw new IllegalStateException("Invalid PDF: " + name);
        }
        Files.write(output.resolve(name + ".pdf"), pdf);
        System.out.println("KYLGIS_FINANCE_RENDER " + name + " pages=" +
            result.getPages().size() + " bytes=" + pdf.length);
    }
}
