//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
//    Compatibility adapter over JasperReports 6.21.5 Swing viewer.
package com.mx.kylgis.pos.util;

import java.io.InputStream;
import java.util.Locale;
import java.util.ResourceBundle;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReportsContext;
import net.sf.jasperreports.swing.JRViewer;

/**
 * Backwards-compatible KylGis viewer facade.
 *
 * <p>The historical JRViewer400 class was a full copy of an old JasperReports
 * viewer. Keeping this small adapter preserves the API used by KylGis while
 * delegating navigation, rendering, printing and resource handling to the
 * JasperReports version actually bundled with the application.</p>
 */
public final class JRViewer400 extends JRViewer {

    public JRViewer400(String fileName, boolean isXML) throws JRException {
        super(fileName, isXML);
    }

    public JRViewer400(InputStream is, boolean isXML) throws JRException {
        super(is, isXML);
    }

    public JRViewer400(JasperPrint jrPrint) {
        super(jrPrint);
    }

    public JRViewer400(String fileName, boolean isXML, Locale locale) throws JRException {
        super(fileName, isXML, locale);
    }

    public JRViewer400(InputStream is, boolean isXML, Locale locale) throws JRException {
        super(is, isXML, locale);
    }

    public JRViewer400(JasperPrint jrPrint, Locale locale) {
        super(jrPrint, locale);
    }

    public JRViewer400(String fileName, boolean isXML, Locale locale, ResourceBundle resBundle)
            throws JRException {
        super(fileName, isXML, locale, resBundle);
    }

    public JRViewer400(InputStream is, boolean isXML, Locale locale, ResourceBundle resBundle)
            throws JRException {
        super(is, isXML, locale, resBundle);
    }

    public JRViewer400(JasperPrint jrPrint, Locale locale, ResourceBundle resBundle) {
        super(jrPrint, locale, resBundle);
    }

    public JRViewer400(JasperReportsContext jasperReportsContext, String fileName,
            boolean isXML, Locale locale, ResourceBundle resBundle) throws JRException {
        super(jasperReportsContext, fileName, isXML, locale, resBundle);
    }

    public JRViewer400(JasperReportsContext jasperReportsContext, InputStream is,
            boolean isXML, Locale locale, ResourceBundle resBundle) throws JRException {
        super(jasperReportsContext, is, isXML, locale, resBundle);
    }

    public JRViewer400(JasperReportsContext jasperReportsContext, JasperPrint jrPrint,
            Locale locale, ResourceBundle resBundle) {
        super(jasperReportsContext, jrPrint, locale, resBundle);
    }

    /**
     * Preserves the refresh API used by the KylGis report panels.
     * A null report means clear the current preview.
     */
    public void loadJasperPrint(JasperPrint jrPrint) {
        // JRViewer.clear() recursively removes every Swing child, including its
        // toolbar and page canvas. A later report then has data but no UI to draw it.
        // Always use the controller: loading null resets the report while preserving
        // the viewer structure for the next execution.
        viewerContext.loadReport(jrPrint);
        if (jrPrint == null) {
            revalidate();
            repaint();
            return;
        }
        setZoomRatio(1.0f);
        viewerContext.refreshPage();
        revalidate();
        repaint();
    }
}
