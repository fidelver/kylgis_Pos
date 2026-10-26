import com.mx.kylgis.pos.util.JRViewer400;
import javax.swing.SwingUtilities;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.base.JRBasePrintPage;

/**
 * Regression guard: the Jasper Swing viewer must retain its toolbar/canvas
 * through load -> clear -> load, as occurs when navigating between reports.
 * No database, printer or user session is accessed.
 */
public final class ReportViewerLifecycleSmoke {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JRViewer400 viewer = new JRViewer400((JasperPrint) null);
            int initial = viewer.getComponentCount();
            if (initial < 2) {
                throw new AssertionError("Viewer missing toolbar or canvas at initialization: " + initial);
            }

            JasperPrint report = new JasperPrint();
            report.setPageWidth(400);
            report.setPageHeight(600);
            report.addPage(new JRBasePrintPage());

            viewer.loadJasperPrint(report);
            assertChildren(viewer, initial, "first report");

            viewer.loadJasperPrint(null);
            assertChildren(viewer, initial, "clear");

            viewer.loadJasperPrint(report);
            assertChildren(viewer, initial, "second report");

            viewer.loadJasperPrint(null);
            assertChildren(viewer, initial, "second clear");
            System.out.println("KYLGIS_JASPER_VIEWER_LIFECYCLE_PASS initial=" + initial
                    + " after=" + viewer.getComponentCount());
        });
    }

    private static void assertChildren(JRViewer400 viewer, int expected, String stage) {
        int current = viewer.getComponentCount();
        if (current != expected) {
            throw new AssertionError("Viewer components destroyed after " + stage
                    + ": initial=" + expected + " current=" + current);
        }
    }
}
