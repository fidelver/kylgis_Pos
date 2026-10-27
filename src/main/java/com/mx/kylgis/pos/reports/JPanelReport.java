//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
//    Portions Copyright (c) 2015-2021 John Lewis (Chromis POS / ChromisKitchenScreen)
//    Portions Copyright (c) 2010-2021 Hugh Clayson / uniCenta (https://unicenta.com)
//    Portions Copyright (c) 2006-2010 Adrián Romero / Openbravo S.L.
//
//    This file is part of KylGis POS
//
//    KylGis POS is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    KylGis POS is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with KylGis POS.  If not, see <http://www.gnu.org/licenses/>.
package com.mx.kylgis.pos.reports;

import com.mx.kylgis.pos.basic.BasicException;
import com.mx.kylgis.pos.data.gui.MessageInf;
import com.mx.kylgis.pos.data.loader.BaseSentence;
import com.mx.kylgis.pos.data.loader.SentenceList;
import com.mx.kylgis.pos.data.loader.Session;
import com.mx.kylgis.pos.data.user.EditorCreator;
import com.mx.kylgis.pos.forms.*;
import com.mx.kylgis.pos.sales.TaxesLogic;
import com.mx.kylgis.pos.util.JRViewer400;
import java.awt.BorderLayout;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

/**
 *
 * @author JG uniCenta
 */
public abstract class JPanelReport extends JPanel implements JPanelView, BeanFactoryApp   {

    private static final Logger REPORT_LOG = Logger.getLogger(JPanelReport.class.getName());
    
    private JRViewer400 reportviewer = null;
    // Invalidate deferred repaints when a report is closed or executed again.
    private long viewerGeneration = 0;          
    private JasperReport jr = null;
    private EditorCreator editor = null;
            
    /**
     *
     */
    protected AppView m_App;
    private Session s;
    private Connection con;

    /**
     *
     */
    protected SentenceList taxsent;
  
    /**
     *
     */
    protected TaxesLogic taxeslogic;

    /** Creates new form JPanelReport */
    public JPanelReport() {
        
        initComponents();
        com.mx.kylgis.pos.ui.help.UiHelp.set(jToggleFilter,"report.toggleFilters");
        com.mx.kylgis.pos.ui.help.UiHelp.set(jButton1,"report.execute");
    }
    
    /**
     *
     * @param app
     * @throws BeanFactoryException
     */
    @Override
    public void init(AppView app) throws BeanFactoryException {   
        
        m_App = app;
        
        DataLogicSales dlSales = (DataLogicSales) app.getBean("com.mx.kylgis.pos.forms.DataLogicSales");
        taxsent = dlSales.getTaxList();
        
        editor = getEditorCreator();
        if (editor instanceof ReportEditorCreator) {
            jPanelFilter.add(((ReportEditorCreator) editor).getComponent(), BorderLayout.CENTER);
            jPanelFilter.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                    javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, 0),
                    "executeReportFromFilter");
            jPanelFilter.getActionMap().put("executeReportFromFilter",
                    new javax.swing.AbstractAction() {
                        @Override
                        public void actionPerformed(java.awt.event.ActionEvent event) {
                            jButton1.doClick();
                        }
                    });
        }
                  
        reportviewer = new JRViewer400(null);                                                       
        
        add(reportviewer, BorderLayout.CENTER);
        
        try {     
            
            InputStream in = getClass().getResourceAsStream(getReport() + ".ser");
            if (in == null) {      
                // read and compile the report
                JasperDesign jd = JRXmlLoader.load(getClass().getResourceAsStream(getReport() + ".jrxml"));            
                jr = JasperCompileManager.compileReport(jd);    
            } else {
// JG 16 May 12 use try-with-resources
                try (ObjectInputStream oin = new ObjectInputStream(in)) {
                    jr = (JasperReport) oin.readObject();
                }
            }
// JG 16 May 12 use multicatch
        } catch (JRException | IOException | ClassNotFoundException | RuntimeException e) {
            MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotloadreport"), e);
            msg.show(this);
            jr = null;
        }  
    }
    
    /**
     *
     * @return
     */
    @Override
    public Object getBean() {
        return this;
    }
    
    /**
     *
     * @return
     */
    protected abstract String getReport();

    /**
     *
     * @return
     */
    protected abstract String getResourceBundle();

    /**
     *
     * @return
     */
    protected abstract BaseSentence getSentence();

    /**
     *
     * @return
     */
    protected abstract ReportFields getReportFields();

    /**
     *
     * @return
     */
    protected EditorCreator getEditorCreator() {
        return null;
    }
    
    /**
     *
     * @return
     */
    @Override
    public JComponent getComponent() {
        return this;
    }
    
    /**
     *
     * @throws BasicException
     */
    @Override
    public void activate() throws BasicException {

        setVisibleFilter(true);
        taxeslogic = new TaxesLogic(taxsent.list()); 
    }

    /**
     *
     * @return
     */
    @Override
    public boolean deactivate() {
        ++viewerGeneration;    
        
        reportviewer.loadJasperPrint(null);
        return true;
    }
    
    /**
     *
     * @param value
     */
    protected void setVisibleButtonFilter(boolean value) {
        jToggleFilter.setVisible(value);
    }
    
    /**
     *
     * @param value
     */
    protected void setVisibleFilter(boolean value) {
        jToggleFilter.setSelected(value);
        jToggleFilterActionPerformed(null);
    }
    
    /**
     * Refreshes the report viewer after the filter panel changes visibility.
     *
     * <p>The report action can finish while the navigator is still completing
     * its layout transition. In that window {@code isShowing()} may be false,
     * so the refresh must not be discarded. The immediate pass handles the
     * current event and the deferred pass lets Swing lay out the new hierarchy
     * before repainting it.</p>
     */
    private void refreshReportViewer(final long generation) {
        Runnable refresh = () -> {
            if (generation != viewerGeneration || reportviewer == null) {
                return;
            }

            revalidate();
            doLayout();
            reportviewer.setVisible(true);
            reportviewer.revalidate();
            reportviewer.repaint();

            java.awt.Container parent = getParent();
            if (parent != null) {
                parent.revalidate();
                parent.repaint();
            }
            repaint();
        };

        refresh.run();
        SwingUtilities.invokeLater(refresh);
    }

    private void launchreport() {
        final long generation = ++viewerGeneration;     
        
        m_App.waitCursorBegin();
        try {
        
        if (jr != null) {
            try {     
                
                // Archivo de recursos
                String res = getResourceBundle();  
                
                // Parametros y los datos
                Object params = (editor == null) ? null : editor.createValue();                
                JRDataSourceBasic data = new JRDataSourceBasic(getSentence(), getReportFields(), params);
                
                // Construyo el mapa de los parametros.
                Map reportparams = new HashMap();
                reportparams.put("ARG", params);
                reportparams.put("KYLGIS_REPORT_PERIOD", ReportPeriodContext.describe(params));
                // El título debe pertenecer al reporte abierto, no a una clave
                // genérica duplicada entre distintos recursos heredados.
                if (this instanceof PanelReportBean) {
                    reportparams.put("KYLGIS_REPORT_TITLE", ((PanelReportBean) this).getTitle());
                }
                if (res != null) {
                      reportparams.put("REPORT_RESOURCE_BUNDLE", ResourceBundle.getBundle(res));
                }                
                reportparams.put("TAXESLOGIC", taxeslogic); 
                
                long fillStarted = System.nanoTime();
                REPORT_LOG.info(() -> "KYL-JASPER fill_started report=" + getReport());
                JasperPrint jp = JasperFillManager.fillReport(jr, reportparams, data);
                final int rowsRead = data.getRowsRead();
                final int pages = jp.getPages().size();
                final long fillMs = (System.nanoTime() - fillStarted) / 1_000_000L;
                REPORT_LOG.info(() -> "KYL-JASPER fill_completed report=" + getReport()
                        + " rows=" + rowsRead + " pages=" + pages + " elapsed_ms=" + fillMs);    
            
                // Jasper may return zero pages, or an empty title page, when the
                // BeanShell query yielded no records. Do not present it as a report.
                // Retain filters so operators can extend dates or adjust criteria.
                if (rowsRead == 0 || pages == 0) {
                    reportviewer.loadJasperPrint(null);
                    setVisibleFilter(true);
                    new MessageInf(MessageInf.SGN_NOTICE,
                            AppLocal.getIntString("message.reportnodata")).show(this);
                } else {
                    // Relayout first, then load/refresh the JasperPrint at its final size.
                    // Loading before hiding the filter left stale viewer geometry.
                    setVisibleFilter(false);
                    revalidate();
                    doLayout();
                    reportviewer.loadJasperPrint(jp);
                    REPORT_LOG.info(() -> "KYL-JASPER viewer_loaded report=" + getReport()
                            + " pages=" + pages + " showing=" + reportviewer.isShowing()
                            + " size=" + reportviewer.getWidth() + "x" + reportviewer.getHeight());
                    refreshReportViewer(generation);
                }
                
            } catch (MissingResourceException e) {
                REPORT_LOG.warning("KYL-JASPER resource_bundle_missing report=" + getReport() + " cause=" + e.getClass().getSimpleName());
                setVisibleFilter(true);
                MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotloadresourcedata"), e);
                msg.show(this);
            } catch (JRException | RuntimeException e) {
                REPORT_LOG.warning("KYL-JASPER fill_failed report=" + getReport() + " cause=" + e.getClass().getSimpleName());
                setVisibleFilter(true);
                MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotfillreport"), e);
                msg.show(this);
            } catch (BasicException e) {
                REPORT_LOG.warning("KYL-JASPER data_failed report=" + getReport() + " cause=" + e.getClass().getSimpleName());
                setVisibleFilter(true);
                MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotloadreportdata"), e);
                msg.show(this);
            }
        } else {
            setVisibleFilter(true);
            REPORT_LOG.warning("KYL-JASPER template_unavailable report=" + getReport());
            new MessageInf(MessageInf.SGN_WARNING,
                    AppLocal.getIntString("message.cannotloadreport"),
                    new IllegalStateException("Jasper template unavailable: " + getReport())).show(this);
        }

        } finally {
            m_App.waitCursorEnd();
        }
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanelHeader = new javax.swing.JPanel();
        jPanelFilter = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jToggleFilter = new javax.swing.JToggleButton();
        jButton1 = new javax.swing.JButton();

        setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setFont(new java.awt.Font("SansSerif", 0, 12)); // NOI18N
        setLayout(new java.awt.BorderLayout());

        jPanelHeader.setLayout(new java.awt.BorderLayout());

        jPanelFilter.setLayout(new java.awt.BorderLayout());
        jPanelHeader.add(jPanelFilter, java.awt.BorderLayout.CENTER);

        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        jToggleFilter.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/mx/kylgis/pos/images/1downarrow.png"))); // NOI18N
        jToggleFilter.setSelected(true);
        jToggleFilter.setToolTipText("Hide/Show Filter");
        jToggleFilter.setPreferredSize(new java.awt.Dimension(80, 45));
        jToggleFilter.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/com/mx/kylgis/pos/images/1uparrow.png"))); // NOI18N
        jToggleFilter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleFilterActionPerformed(evt);
            }
        });
        jPanel1.add(jToggleFilter);

        jButton1.setFont(new java.awt.Font("SansSerif", 0, 12)); // NOI18N
        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/mx/kylgis/pos/images/ok.png"))); // NOI18N
        jButton1.setText(AppLocal.getIntString("button.executereport")); // NOI18N
        jButton1.setToolTipText("Execute Report");
        jButton1.setPreferredSize(new java.awt.Dimension(150, 45));
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jPanel1.add(jButton1);

        jPanelHeader.add(jPanel1, java.awt.BorderLayout.SOUTH);

        add(jPanelHeader, java.awt.BorderLayout.NORTH);
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed

        launchreport();
        
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jToggleFilterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleFilterActionPerformed

        jPanelFilter.setVisible(jToggleFilter.isSelected());
    
    }//GEN-LAST:event_jToggleFilterActionPerformed

    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanelFilter;
    private javax.swing.JPanel jPanelHeader;
    private javax.swing.JToggleButton jToggleFilter;
    // End of variables declaration//GEN-END:variables
    
}
