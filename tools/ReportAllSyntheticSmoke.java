import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.*;
import java.sql.Timestamp;
import java.util.*;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

/** Offline smoke: 67 report templates filled from type-correct synthetic rows.
 * Never contacts SQL, starts a POS, or sends a print job.
 */
public final class ReportAllSyntheticSmoke {
    private static final Timestamp START = Timestamp.valueOf("2026-10-01 00:00:00");
    private static final Timestamp END = Timestamp.valueOf("2026-10-18 00:00:00");

    private static final ResourceBundle LABELS = new ResourceBundle() {
        @Override protected Object handleGetObject(String key) { return key; }
        @Override public Enumeration<String> getKeys() {
            return Collections.enumeration(Collections.<String>emptySet());
        }
    };

    private static Object valueFor(JRField f, int row) {
        Class<?> c = f.getValueClass();
        String name = f.getName().replaceAll("[^A-Za-z0-9_]", "_");
        if ("com.mx.kylgis.pos.ticket.TicketInfo".equals(c.getName())) {
            com.mx.kylgis.pos.ticket.TicketInfo ticket = new com.mx.kylgis.pos.ticket.TicketInfo();
            ticket.setTicketId(801 + row);
            ticket.setDate(new java.util.Date(START.getTime()));
            return ticket;
        }
        if (String.class.equals(c)) return "QA" + (row + 1);
        if (Integer.class.equals(c) || int.class.equals(c)) return 3 + row;
        if (Long.class.equals(c) || long.class.equals(c)) return 3L + row;
        if (Short.class.equals(c)) return (short)(3 + row);
        if (Double.class.equals(c) || double.class.equals(c)) return 20.5 + row;
        if (Float.class.equals(c) || float.class.equals(c)) return 20.5f + row;
        if (BigDecimal.class.equals(c)) return BigDecimal.valueOf(20.5 + row);
        if (Boolean.class.equals(c) || boolean.class.equals(c)) return true;
        if (java.sql.Date.class.equals(c)) return java.sql.Date.valueOf("2026-10-10");
        if (java.sql.Time.class.equals(c)) return java.sql.Time.valueOf("13:30:00");
        if (Timestamp.class.equals(c)) return Timestamp.valueOf("2026-10-10 13:30:00");
        if (java.util.Date.class.isAssignableFrom(c)) return new java.util.Date(START.getTime());
        if (java.awt.Image.class.isAssignableFrom(c)) return new BufferedImage(12, 12, BufferedImage.TYPE_INT_RGB);
        if (byte[].class.equals(c)) return new byte[0];
        return null;
    }

    private static void countText(JRPrintElement element, int[] counts) {
        if (element instanceof JRPrintText) {
            String text = ((JRPrintText)element).getFullText();
            if (text != null && !text.trim().isEmpty()) {
                counts[0]++;
                if (text.contains("QA")) counts[1]++;
            }
        }
        if (element instanceof JRPrintFrame) {
            for (JRPrintElement child : ((JRPrintFrame)element).getElements()) {
                countText(child, counts);
            }
        }
    }

    public static void main(String[] argv) throws Exception {
        Path folder = Path.of(argv[0]);
        int all = 0, compiled = 0, rendered = 0, withDataText = 0, failed = 0;
        for (Path file : Files.list(folder).filter(p -> p.getFileName().toString().endsWith(".jrxml"))
                                         .sorted().toList()) {
            String name = file.getFileName().toString();
            if (argv.length > 1 && !name.equals(argv[1])) continue;
            all++;
            try (InputStream input = Files.newInputStream(file)) {
                JasperReport report = JasperCompileManager.compileReport(input);
                compiled++;
                List<Map<String, ?>> rows = new ArrayList<>();
                for (int j = 0; j < 3; j++) {
                    Map<String, Object> fields = new HashMap<>();
                    JRField[] declaredFields = report.getFields();
                    if (declaredFields != null)
                        for (JRField f : declaredFields) fields.put(f.getName(), valueFor(f, j));
                    rows.add(fields);
                }
                Map<String, Object> params = new HashMap<>();
                params.put(JRParameter.REPORT_LOCALE, Locale.forLanguageTag("es-MX"));
                params.put(JRParameter.REPORT_RESOURCE_BUNDLE, LABELS);
                params.put("ARG", new Object[]{new Object[]{null, START, null, END}});
                params.put("KYLGIS_REPORT_PERIOD", "01/10/2026 a 18/10/2026");
                params.put("CUSTOMER_NAME", "QA CLIENTE VALE");
                params.put("CODE", "QA-VALE-123");
                params.put("ISSUED", START);
                params.put("VALUE", 35.50);
                params.put("EXPIRY", "18/10/2026");
                params.put("LOGO", new BufferedImage(12, 12, BufferedImage.TYPE_INT_RGB));
                JasperPrint result = JasperFillManager.fillReport(
                    report, params, new JRMapCollectionDataSource(rows));
                if (result.getPages().isEmpty()) throw new IllegalStateException("0 pages despite 3 synthetic records");
                int[] texts = {0, 0};
                for (JRPrintPage page : result.getPages())
                    for (JRPrintElement element : page.getElements()) countText(element, texts);
                if (argv.length > 2 && "debug".equals(argv[2])) {
                    for (JRPrintPage page : result.getPages()) {
                        for (JRPrintElement element : page.getElements()) {
                            if (element instanceof JRPrintText t) {
                                String value = t.getFullText();
                                System.out.println("VISIBLE_FIELD_DEBUG x=" + t.getX() + " y=" + t.getY() +
                                    " h=" + t.getHeight() + " text=" + value);
                            }
                        }
                    }
                }
                if (texts[0] == 0) throw new IllegalStateException("0 visible text elements");
                byte[] pdf = JasperExportManager.exportReportToPdf(result);
                if (pdf.length < 5 || pdf[0] != '%' || pdf[1] != 'P')
                    throw new IllegalStateException("Invalid PDF bytes");
                rendered++;
                if (texts[1] > 0) withDataText++;
                System.out.println("REPORT_RUNTIME_PASS " + name + " pages=" + result.getPages().size()
                    + " text=" + texts[0] + " rowsVisible=" + texts[1] + " pdfBytes=" + pdf.length);
            } catch (Throwable error) {
                failed++;
                if (argv.length > 1) error.printStackTrace(System.err);
                Throwable cause = error;
                while (cause.getCause() != null && cause.getCause() != cause) cause = cause.getCause();
                String message = String.valueOf(cause.getMessage()).replaceAll("[\\r\\n]+", " ");
                if (message.length() > 210) message = message.substring(0, 210);
                System.out.println("REPORT_RUNTIME_FAIL " + name + " " +
                    cause.getClass().getSimpleName() + ": " + message);
            }
        }
        System.out.println("KYLGIS_ALL_REPORTS_SYNTHETIC_QA all=" + all + " compiled="
            + compiled + " rendered=" + rendered + " withDataText=" + withDataText + " failed=" + failed);
        // Charts and object-only templates may not expose a literal QA marker.
        // At least 60/67 templates must show identifiable synthetic content.
        if ((argv.length == 1 && (all != 67 || withDataText < 60)) || failed > 0 || rendered != all) System.exit(2);
    }
}
