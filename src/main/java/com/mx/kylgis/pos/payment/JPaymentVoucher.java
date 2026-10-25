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
package com.mx.kylgis.pos.payment;

import com.mx.kylgis.pos.basic.BasicException;
import com.mx.kylgis.pos.data.gui.ComboBoxValModel;
import com.mx.kylgis.pos.data.loader.SentenceList;
import com.mx.kylgis.pos.format.Formats;
import com.mx.kylgis.pos.customers.CustomerInfoExt;
import com.mx.kylgis.pos.customers.DataLogicCustomers;
import com.mx.kylgis.pos.forms.AppLocal;
import com.mx.kylgis.pos.forms.AppView;
import com.mx.kylgis.pos.forms.DataLogicSales;
import com.mx.kylgis.pos.util.RoundUtils;
import com.mx.kylgis.pos.voucher.VoucherDiscountInfo;
import com.mx.kylgis.pos.voucher.VoucherInfo;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

/**
 *
 * @author JG uniCenta
 */
public class JPaymentVoucher extends javax.swing.JPanel implements JPaymentInterface {
    
    private final JPaymentNotifier m_notifier;
    
    private DataLogicSales dlSales;
    private DataLogicCustomers dlCustomers;    
    private ComboBoxValModel m_VoucherModel;
    private SentenceList m_sentvouch;
    private VoucherInfo m_voucherInfo;    
    
    private double m_dTicket;
    private double m_dTotal;  

    private String m_sVoucher1;
    private String m_customerId;
    private javax.swing.JTextField m_directVoucherCode;
    private javax.swing.JButton m_directVoucherSearch;
   
    /** Creates new form JPaymentTicket
     * @param app
     * @param notifier
     */
    public JPaymentVoucher(AppView app, JPaymentNotifier notifier) {

        m_notifier = notifier;
        m_dTotal=0.0;
        
        init(app);

        m_jTendered.addPropertyChangeListener("Edition", new RecalculateState());
//        m_jTendered.addEditorKeys(m_jKeys);

    }
    private void init(AppView app) {

        dlSales = (DataLogicSales) app.getBean("com.mx.kylgis.pos.forms.DataLogicSales");
        dlCustomers = (DataLogicCustomers) app.getBean("com.mx.kylgis.pos.customers.DataLogicCustomers");
        m_sentvouch = dlSales.getVoucherListByCustomer();

        initComponents();
        installDirectVoucherLookup();

        m_VoucherModel = new ComboBoxValModel();
        m_jVoucher.setModel(m_VoucherModel);
        m_jVoucher.setEnabled(false);
        webLblcustomerName.setText(null);
    }

    /**
     *
     * @param customerext
     * @param dTotal
     * @param transID
     */
    @Override
    public void activate(CustomerInfoExt customerext, double dTotal,String transID) {

            m_dTotal = dTotal;
            m_customerId = customerext == null ? null : customerext.getId();

            m_jTendered.reset();
            refreshVoucherModel(customerext);

            m_jKeys.setEnabled(false);
            m_jTendered.setEnabled(false);

            printState();
        }

    private void refreshVoucherModel(CustomerInfoExt customerext) {
        m_jMoneyEuros.setText(null);
        voucherStatus.setText(null);
        m_voucherInfo = null;
        if (m_directVoucherCode != null) {
            m_directVoucherCode.setText("");
        }
        webLblcustomerName.setText(customerext == null ? null : customerext.getName());

        try {
            String sourceReceiptId = getVoucherEditSourceReceiptId();
            List vouchers = customerext == null
                    ? java.util.Collections.emptyList()
                    : sourceReceiptId == null || sourceReceiptId.isBlank()
                            ? m_sentvouch.list(customerext.getId())
                            : dlSales.getVoucherListByCustomerForEdit(customerext.getId(), sourceReceiptId);
            m_VoucherModel = new ComboBoxValModel(vouchers);
            m_jVoucher.setModel(m_VoucherModel);
            m_jVoucher.setEnabled(customerext != null && !vouchers.isEmpty());
        } catch (BasicException ex) {
            m_VoucherModel = new ComboBoxValModel();
            m_jVoucher.setModel(m_VoucherModel);
            m_jVoucher.setEnabled(false);
        }
    }

    private void installDirectVoucherLookup() {
        javax.swing.JPanel direct = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 5));
        javax.swing.JLabel label = new javax.swing.JLabel("Código de cupón:");
        label.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 16));
        m_directVoucherCode = new javax.swing.JTextField(22);
        m_directVoucherCode.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 16));
        m_directVoucherSearch = new javax.swing.JButton("Buscar");
        m_directVoucherSearch.addActionListener(e -> lookupDirectVoucher());
        m_directVoucherCode.addActionListener(e -> lookupDirectVoucher());
        direct.add(label);
        direct.add(m_directVoucherCode);
        direct.add(m_directVoucherSearch);
        add(direct, java.awt.BorderLayout.SOUTH);
    }

    private void lookupDirectVoucher() {
        String code = com.mx.kylgis.pos.voucher.VoucherCodeGenerator.normalize(
                m_directVoucherCode == null ? null : m_directVoucherCode.getText());
        m_voucherInfo = null;
        m_jVoucher.setSelectedItem(null);
        if (code.isEmpty()) {
            voucherStatus.setText("Capture un código de cupón.");
            return;
        }
        try {
            VoucherInfo voucher = dlCustomers.getVoucherInfoByNumber(code);
            if (voucher == null) {
                voucherStatus.setText("Cupón no encontrado.");
                return;
            }
            String sourceReceiptId = getVoucherEditSourceReceiptId();
            boolean active = "A".equals(voucher.getStatus());
            boolean authorizedSourceVoucher = "D".equals(voucher.getStatus())
                    && sourceReceiptId != null && !sourceReceiptId.isBlank()
                    && dlSales.isReusableVoucherFromSourceReceipt(sourceReceiptId, voucher.getVoucherNumber());
            if (!active && !authorizedSourceVoucher) {
                voucherStatus.setText("Cupón ya redimido o inactivo.");
                return;
            }
            if (voucher.isExpired(new java.util.Date())) {
                voucherStatus.setText("Cupón vencido.");
                return;
            }
            if (voucher.getCustomerId() != null
                    && (m_customerId == null || !m_customerId.equals(voucher.getCustomerId()))) {
                voucherStatus.setText("Cupón asignado a otro cliente.");
                return;
            }
            m_voucherInfo = voucher;
            m_jVoucher.setSelectedItem(null);
            m_jTendered.setDoubleValue(voucher.getAmount());
            m_jMoneyEuros.setText(Formats.CURRENCY.formatValue(voucher.getAmount()));
            webLblcustomerName.setText(voucher.getCustomerId() == null
                    ? "Cupón de campaña abierto" : voucher.getCustomerName());
            voucherStatus.setText(active ? "Disponible" : "Redimido en venta origen (edición autorizada)");
            printState();
        } catch (BasicException ex) {
            voucherStatus.setText("No se pudo consultar el cupón.");
        }
    }

    /**
     *
     * @return
     */
    @Override
    public Component getComponent() {
        return this;
    }
    
    /**
     *
     * @return
     */
    @Override
    public PaymentInfo executePayment() {
        VoucherInfo voucherInfo = m_voucherInfo;
        if (voucherInfo == null) {
            Object selected = m_VoucherModel.getSelectedItem();
            if (selected instanceof VoucherInfo selectedVoucher) {
                voucherInfo = selectedVoucher;
            }
        }
        if (voucherInfo == null || voucherInfo.isExpired(new java.util.Date())) {
            return null;
        }
        if (voucherInfo.getCustomerId() != null
                && (m_customerId == null || !m_customerId.equals(voucherInfo.getCustomerId()))) {
            return null;
        }

        String sourceReceiptId = getVoucherEditSourceReceiptId();
        boolean active = "A".equals(voucherInfo.getStatus());
        boolean authorizedSourceVoucher = false;
        if ("D".equals(voucherInfo.getStatus())
                && sourceReceiptId != null && !sourceReceiptId.isBlank()) {
            try {
                authorizedSourceVoucher = dlSales.isReusableVoucherFromSourceReceipt(
                        sourceReceiptId, voucherInfo.getVoucherNumber());
            } catch (BasicException ex) {
                return null;
            }
        }
        if (!active && !authorizedSourceVoucher) {
            return null;
        }
        m_sVoucher1 = voucherInfo.getVoucherNumber();
        double discountCapacity = m_notifier instanceof JPaymentSelect paymentSelect
                ? paymentSelect.getVoucherDiscountCapacity()
                : Math.max(0.0, m_dTotal);
        double appliedAmount = Math.min(voucherInfo.getAmount(), discountCapacity);
        if (appliedAmount <= 0.0) {
            return null;
        }
        return new VoucherDiscountInfo(voucherInfo.getId(), m_sVoucher1,
                voucherInfo.getCustomerId(), voucherInfo.getAmount(), appliedAmount);
    }

    private String getVoucherEditSourceReceiptId() {
        return m_notifier instanceof JPaymentSelect paymentSelect
                ? paymentSelect.getVoucherEditSourceReceiptId()
                : null;
    }

    private void printState() {

        Double value = m_jTendered.getDoubleValue();
        if (value == null) {
            m_dTicket = 0.0;
        } else {
            m_dTicket = value;
        } 
        
        m_jMoneyEuros.setText(Formats.CURRENCY.formatValue(m_dTicket));
        
        int iCompare = RoundUtils.compare(m_dTicket, m_dTotal);
        
        // it is allowed to pay more
        m_notifier.setStatus(m_dTicket > 0.0, iCompare >= 0);
    }

    private class RecalculateState implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            printState();
        }
    }    
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel4 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        m_jVoucher = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        m_jMoneyEuros = new javax.swing.JLabel();
        webLblCustomer = new com.alee.laf.label.WebLabel();
        webLblcustomerName = new com.alee.laf.label.WebLabel();
        voucherStatus = new com.alee.laf.label.WebLabel();
        jLabel6 = new javax.swing.JLabel();
        jPanel11 = new javax.swing.JPanel();
        jPanel12 = new javax.swing.JPanel();
        m_jKeys = new com.mx.kylgis.pos.editor.JEditorKeys();
        jPanel1 = new javax.swing.JPanel();
        m_jTendered = new com.mx.kylgis.pos.editor.JEditorCurrencyPositive();

        setLayout(new java.awt.BorderLayout());

        jLabel5.setFont(new java.awt.Font("Arial", 1, 18)); // NOI18N
        jLabel5.setLabelFor(m_jVoucher);
        jLabel5.setText(AppLocal.getIntString("label.voucher")); // NOI18N
        jLabel5.setPreferredSize(new java.awt.Dimension(100, 30));

        m_jVoucher.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        m_jVoucher.setPreferredSize(new java.awt.Dimension(180, 30));
        m_jVoucher.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jVoucherActionPerformed(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Arial", 1, 18)); // NOI18N
        jLabel1.setText(AppLocal.getIntString("label.voucherValue")); // NOI18N
        jLabel1.setPreferredSize(new java.awt.Dimension(100, 30));

        m_jMoneyEuros.setBackground(new java.awt.Color(204, 255, 51));
        m_jMoneyEuros.setFont(new java.awt.Font("Arial", 1, 18)); // NOI18N
        m_jMoneyEuros.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        m_jMoneyEuros.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createLineBorder(javax.swing.UIManager.getDefaults().getColor("Button.darkShadow")), javax.swing.BorderFactory.createEmptyBorder(1, 4, 1, 4)));
        m_jMoneyEuros.setOpaque(true);
        m_jMoneyEuros.setPreferredSize(new java.awt.Dimension(180, 30));

        webLblCustomer.setText(AppLocal.getIntString("label.customer")); // NOI18N
        webLblCustomer.setToolTipText("");
        webLblCustomer.setFont(new java.awt.Font("Arial", 1, 18)); // NOI18N
        webLblCustomer.setPreferredSize(new java.awt.Dimension(100, 30));

        webLblcustomerName.setText(AppLocal.getIntString("label.customer")); // NOI18N
        webLblcustomerName.setToolTipText("");
        webLblcustomerName.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        webLblcustomerName.setPreferredSize(new java.awt.Dimension(100, 30));

        voucherStatus.setText(AppLocal.getIntString("label.voucherStatus")); // NOI18N
        voucherStatus.setToolTipText("");
        voucherStatus.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        voucherStatus.setPreferredSize(new java.awt.Dimension(100, 30));

        jLabel6.setFont(new java.awt.Font("Arial", 1, 18)); // NOI18N
        jLabel6.setLabelFor(m_jVoucher);
        jLabel6.setText(AppLocal.getIntString("label.voucherStatus")); // NOI18N
        jLabel6.setPreferredSize(new java.awt.Dimension(100, 30));

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(webLblCustomer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(webLblcustomerName, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(m_jMoneyEuros, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(m_jVoucher, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(voucherStatus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(m_jVoucher, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(voucherStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(m_jMoneyEuros, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(webLblCustomer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(webLblcustomerName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(245, 245, 245))
        );

        add(jPanel4, java.awt.BorderLayout.CENTER);

        jPanel11.setLayout(new java.awt.BorderLayout());

        jPanel12.setLayout(new javax.swing.BoxLayout(jPanel12, javax.swing.BoxLayout.Y_AXIS));
        jPanel12.add(m_jKeys);

        jPanel1.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        jPanel1.setLayout(new java.awt.BorderLayout());

        m_jTendered.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        m_jTendered.setPreferredSize(new java.awt.Dimension(130, 30));
        jPanel1.add(m_jTendered, java.awt.BorderLayout.CENTER);

        jPanel12.add(jPanel1);

        jPanel11.add(jPanel12, java.awt.BorderLayout.NORTH);

        add(jPanel11, java.awt.BorderLayout.EAST);
    }// </editor-fold>//GEN-END:initComponents

    private void m_jVoucherActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jVoucherActionPerformed
    
        m_jMoneyEuros.setText(null);

        if (m_VoucherModel.getSelectedKey()!=null){
            try {

                String id = m_VoucherModel.getSelectedKey().toString();
                m_voucherInfo = dlCustomers.getVoucherInfo(id);

                if (m_voucherInfo != null){
                    m_jTendered.setDoubleValue(m_voucherInfo.getAmount());
                    m_jMoneyEuros.setText(Formats.CURRENCY.formatValue(m_voucherInfo.getAmount()));
                    webLblcustomerName.setText(m_voucherInfo.getCustomerName());
                    
                    if ("A".equals(m_voucherInfo.getStatus())) {
                        voucherStatus.setText("Available");    
                    } else if ("D".equals(m_voucherInfo.getStatus())) {
                        voucherStatus.setText("Redeemed");                        
                    }

                    printState();
                }
            } catch (BasicException ex) {
//                ex.printStackTrace();
            }

        }
    }//GEN-LAST:event_m_jVoucherActionPerformed
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel4;
    private com.mx.kylgis.pos.editor.JEditorKeys m_jKeys;
    private javax.swing.JLabel m_jMoneyEuros;
    private com.mx.kylgis.pos.editor.JEditorCurrencyPositive m_jTendered;
    private javax.swing.JComboBox m_jVoucher;
    private com.alee.laf.label.WebLabel voucherStatus;
    private com.alee.laf.label.WebLabel webLblCustomer;
    private com.alee.laf.label.WebLabel webLblcustomerName;
    // End of variables declaration//GEN-END:variables
    
}
