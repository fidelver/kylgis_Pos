
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
package com.mx.kylgis.pos.printer.javapos;

import com.mx.kylgis.pos.printer.DeviceFiscalPrinter;
import com.mx.kylgis.pos.printer.TicketPrinterException;
import com.mx.kylgis.pos.util.RoundUtils;
import javax.swing.JComponent;
import jpos.FiscalPrinter;
import jpos.JposException;

/**
 *
 * @author JG uniCenta
 */
public class DeviceFiscalPrinterJavaPOS extends javax.swing.JPanel implements DeviceFiscalPrinter, AutoCloseable  {
    
    private String m_sName;
    
    private FiscalPrinter m_fiscal;
    private boolean closed;
    
    /** Creates new form DeviceFiscalPrinterJavaPOSPanel
     * @param sDeviceFiscalPrinterName
     * @throws com.mx.kylgis.pos.printer.TicketPrinterException */
    public DeviceFiscalPrinterJavaPOS(String sDeviceFiscalPrinterName) throws TicketPrinterException {
        m_sName = sDeviceFiscalPrinterName;
        
        
        m_fiscal = new FiscalPrinter();
        try {       
            m_fiscal.open(m_sName);
            m_fiscal.claim(10000);
            m_fiscal.setDeviceEnabled(true);
            // m_printer.setMapMode(POSPrinterConst.PTR_MM_METRIC);  // unit = 1/100 mm - i.e. 1 cm = 10 mm = 10 * 100 units

            m_fiscal.setCheckTotal(false);
            
        } catch (JposException e) {
            close();
            throw new TicketPrinterException(e.getMessage(), e);
        }
        
        initComponents();
        // The former "Receipt Title" panel consisted of inert placeholder
        // fields. Fiscal X/Z operations remain wired to their JPOS calls.
        jButton1.setText(com.mx.kylgis.pos.ui.help.UiHelp.text("fiscal.xreport.label"));
        jButton2.setText(com.mx.kylgis.pos.ui.help.UiHelp.text("fiscal.zreport.label"));
        com.mx.kylgis.pos.ui.help.UiHelp.set(jButton1,"fiscal.xreport");
        com.mx.kylgis.pos.ui.help.UiHelp.set(jButton2,"fiscal.zreport");
    }
 
    /**
     *
     * @return
     */
    @Override
    public String getFiscalName() {
        return m_sName;
    }

    /**
     *
     * @return
     */
    @Override
    public JComponent getFiscalComponent() {
        return this;
    }
    
    /**
     *
     */
    @Override
    public void beginReceipt() {
        try {
            m_fiscal.beginFiscalReceipt(true);
        } catch (JposException e) {
        }
    }

    /**
     *
     */
    @Override
    public void endReceipt() {
        try {
            m_fiscal.endFiscalReceipt(false);
        } catch (JposException e) {
        }        
    }
    
    /**
     *
     * @param sproduct
     * @param dprice
     * @param dunits
     * @param taxinfo
     */
    @Override
    public void printLine(String sproduct, double dprice, double dunits, int taxinfo) {
        try {
            m_fiscal.printRecItem(sproduct, roundFiscal(dprice * dunits), (int)(dunits * 1000), taxinfo, roundFiscal(dprice), "");
        } catch (JposException e) {
        }             
    }
    
    /**
     *
     * @param smessage
     */
    @Override
    public void printMessage(String smessage) {
        try {
            m_fiscal.printRecMessage(smessage);
        } catch (JposException e) {
        } 
    }
    
    /**
     *
     * @param sPayment
     * @param dpaid
     */
    @Override
    public void printTotal(String sPayment, double dpaid) {
        try {
            // el primer valor es el total calculado por la aplicacion.
            // al poner 0 no se debe chequear: CAPCHECKTOTAL = false.
            m_fiscal.printRecTotal(0, roundFiscal(dpaid), sPayment);
        } catch (JposException e) {
        }          
    }
    
    /**
     *
     */
    @Override
    public void printZReport() {
        try {
            m_fiscal.printZReport();
        } catch (JposException e) {
        }          
    }
    
    /**
     *
     */
    @Override
    public void printXReport() {
        try {
            m_fiscal.printXReport();
        } catch (JposException e) {
        }     
    }
    
    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (m_fiscal == null) {
            return;
        }
        try {
            m_fiscal.setDeviceEnabled(false);
        } catch (JposException ignored) {
        }
        try {
            m_fiscal.release();
        } catch (JposException ignored) {
        }
        try {
            m_fiscal.close();
        } catch (JposException ignored) {
        }
    } 
    
    private int roundFiscal(double value) {
        return (int) Math.floor(RoundUtils.round(value) * 10000.0 + 0.5);
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();

        setLayout(null);
        setPreferredSize(new java.awt.Dimension(360, 64));

        jButton1.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jButton1.setText("*X Report");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        add(jButton1);
        jButton1.setBounds(18, 12, 150, 34);

        jButton2.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jButton2.setText("*Z Report");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        add(jButton2);
        jButton2.setBounds(190, 12, 150, 34);
    }// </editor-fold>//GEN-END:initComponents

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed

        printZReport();

    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed

        printXReport();
        
    }//GEN-LAST:event_jButton1ActionPerformed
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    // End of variables declaration//GEN-END:variables
    
}