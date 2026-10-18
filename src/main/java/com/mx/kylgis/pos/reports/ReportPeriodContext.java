package com.mx.kylgis.pos.reports;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Builds a compact, human-readable date range from the JParamsDatesInterval
 * component within a composed QBF editor. No SQL or database access.
 */
public final class ReportPeriodContext {
    private ReportPeriodContext() { }

    public static String describe(Object filterParams) {
        if (!(filterParams instanceof Object[])) {
            return "Sin fechas seleccionadas";
        }
        for (Object filter : (Object[]) filterParams) {
            if (!(filter instanceof Object[])) {
                continue;
            }
            Object[] values = (Object[]) filter;
            if (values.length < 4) {
                continue;
            }
            Date start = values[1] instanceof Date ? (Date) values[1] : null;
            Date end = values[3] instanceof Date ? (Date) values[3] : null;
            if (start == null && end == null) {
                continue;
            }
            SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            if (start != null && end != null) {
                return format.format(start) + " - " + format.format(end) + " (fin exclusivo)";
            }
            if (start != null) {
                return "Desde " + format.format(start);
            }
            return "Antes de " + format.format(end);
        }
        return "Sin fechas seleccionadas";
    }
}
