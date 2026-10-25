import java.sql.Timestamp;
import com.mx.kylgis.pos.reports.ReportPeriodContext;

public final class ReportPeriodContextSmoke {
    public static void main(String[] args) {
        Timestamp first = Timestamp.valueOf("2026-10-01 00:00:00");
        Timestamp last = Timestamp.valueOf("2026-10-18 00:00:00");
        Object[] filter = {null, first, null, last};
        String value = ReportPeriodContext.describe(new Object[]{"qa customer", filter});
        if (!value.contains("01/10/2026 00:00") ||
                !value.contains("18/10/2026 00:00") ||
                !value.contains("fin exclusivo")) {
            throw new AssertionError("Wrong rendered period: " + value);
        }
        if (!"Sin fechas seleccionadas".equals(ReportPeriodContext.describe(new Object[]{new Object[]{"customer"}}))) {
            throw new AssertionError("Incorrect fallback for filter without dates");
        }
        String open = ReportPeriodContext.describe(new Object[]{new Object[]{null, first, null, null}});
        if (!open.startsWith("Desde 01/10/2026")) {
            throw new AssertionError("Start-only date omitted");
        }
        System.out.println("KYLGIS_PERIOD_CONTEXT_OK cases=3");
    }
}
