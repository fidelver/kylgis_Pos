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
package com.mx.kylgis.pos.voucher;

import com.mx.kylgis.pos.basic.BasicException;
import com.mx.kylgis.pos.beans.JCalendarDialog;
import com.mx.kylgis.pos.data.gui.ComboBoxValModel;
import com.mx.kylgis.pos.data.user.DirtyManager;
import com.mx.kylgis.pos.data.user.EditorRecord;
import com.mx.kylgis.pos.format.Formats;
import com.mx.kylgis.pos.customers.CustomerInfo;
import com.mx.kylgis.pos.customers.DataLogicCustomers;
import com.mx.kylgis.pos.customers.JCustomerFinder;
import com.mx.kylgis.pos.customers.JDialogNewCustomer;
import com.mx.kylgis.pos.forms.AppLocal;
import com.mx.kylgis.pos.forms.AppView;
import com.mx.kylgis.pos.forms.DataLogicSystem;
import com.mx.kylgis.pos.util.ValidateBuilder;
import java.awt.Component;
import java.awt.image.BufferedImage;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

public final class VoucherEditor extends javax.swing.JPanel implements EditorRecord {

    private Object id;
    private final DataLogicCustomers dlCustomers;
    private final  DataLogicSystem dlSystem;
    private CustomerInfo customerInfo;
    private Date expiresAt;
    private static final long MIN_NEW_VOUCHER_LIFETIME_MILLIS = 24L * 60L * 60L * 1000L;
    private boolean insertingVoucher;
    private Date minimumNewExpiry;
    private final AppView m_app;
    
    private final ComboBoxValModel m_ReasonModel;    

     public VoucherEditor(DirtyManager dirty,AppView app) {
        m_app = app;

        initComponents();
  
        dlCustomers = (DataLogicCustomers) 
                app.getBean("com.mx.kylgis.pos.customers.DataLogicCustomers");
        dlSystem= (DataLogicSystem) 
                app.getBean("com.mx.kylgis.pos.forms.DataLogicSystem");
        m_jNumber.getDocument().addDocumentListener(dirty);
        m_jCustomer.getDocument().addDocumentListener(dirty);
        m_jAmount.getDocument().addDocumentListener(dirty);
        m_jStatus.getDocument().addDocumentListener(dirty);
        
        jButtonPrint.setVisible(false);
        jChkNoExpiry.addActionListener(dirty);
        m_jBtnExpiresAt.addActionListener(dirty);       
        
        m_ReasonModel = new ComboBoxValModel();
        m_ReasonModel.add(AppLocal.getIntString("cboption.find"));
        m_ReasonModel.add(AppLocal.getIntString("cboption.create"));              
        jCBCustomer.setModel(m_ReasonModel);   
        jLblStatus.setIcon(null);
                
        writeValueEOF();
    }
     
    @Override
    public void writeValueEOF() {
        id = null;
        customerInfo = null;
        insertingVoucher = false;
        minimumNewExpiry = null;
        expiresAt = null;
        updateExpiryControls(false);
        m_jNumber.setText(null);
        m_jNumber.setEnabled(false);
        m_jCustomer.setText(null);
        m_jCustomer.setEnabled(false);
        m_jAmount.setText(null);
        m_jAmount.setEnabled(false);
        m_jStatus.setText(null);
        m_jStatus.setEnabled(false);        
        
        jButtonPrint.setEnabled(false);
    }
    
    @Override
    public void writeValueInsert() {
        id = UUID.randomUUID().toString();
        customerInfo = null;
        insertingVoucher = true;
        minimumNewExpiry = new Date(System.currentTimeMillis() + MIN_NEW_VOUCHER_LIFETIME_MILLIS);
        expiresAt = null;
        updateExpiryControls(true);
        m_jNumber.setText(VoucherCodeGenerator.generate());
        m_jNumber.setEnabled(true);
        m_jCustomer.setText(null);
        m_jCustomer.setEnabled(true);
        m_jAmount.setText(null);
        m_jAmount.setEnabled(true);
        m_jStatus.setText(null);
        m_jStatus.setText("A");
        
        jButtonPrint.setEnabled(false);
        jButtonPrint.setEnabled(true);        
    }
    
    @Override
    public void writeValueDelete(Object value) {
        insertingVoucher = false;
        minimumNewExpiry = null;
    if ("A".equals(m_jStatus.getText())) {
        try {
            Object[] attr = (Object[]) value;
            id = attr[0];
            m_jNumber.setText(Formats.STRING.formatValue(attr[1]));
            m_jNumber.setEnabled(false);
            customerInfo = dlCustomers.getCustomerInfo(attr[2].toString());
            m_jCustomer.setText(customerInfo.getName());
            m_jCustomer.setEnabled(false);
            m_jAmount.setText(Formats.DOUBLE.formatValue(attr[3]));
            m_jAmount.setEnabled(false);
            m_jStatus.setText(Formats.STRING.formatValue(attr[4]));
            expiresAt = (Date) attr[5];
            updateExpiryControls(false);
            m_jStatus.setEnabled(false);
            
                jButtonPrint.setEnabled(false);
        } catch (BasicException ex) {
            Logger.getLogger(VoucherEditor.class.getName()).log(Level.SEVERE, null, ex);
        }
    } else {
        JOptionPane.showMessageDialog(this, 
            AppLocal.getIntString("message.voucherdelete"),
            AppLocal.getIntString("Check"),
            JOptionPane.WARNING_MESSAGE);
    }
    }    
    
    @Override
    public void writeValueEdit(Object value) {
        insertingVoucher = false;
        minimumNewExpiry = null;

        try {
            Object[] attr = (Object[]) value;
            id = attr[0];
            m_jNumber.setText(Formats.STRING.formatValue(attr[1]));
            m_jNumber.setEnabled(true);
            customerInfo = dlCustomers.getCustomerInfo(attr[2].toString());
            m_jCustomer.setText(customerInfo.getName());
            m_jCustomer.setEnabled(true);
            m_jAmount.setText(Formats.DOUBLE.formatValue(attr[3]));
            m_jAmount.setEnabled(true);
            m_jStatus.setText(Formats.STRING.formatValue(attr[4]));
            expiresAt = (Date) attr[5];
            updateExpiryControls(false);
            
            jButtonPrint.setEnabled(true);
            
            if (null == m_jStatus.getText()) {
                jLblStatus.setIcon(null);
            } else switch (m_jStatus.getText()) {
                case "A":
                    jLblStatus.setIcon(new javax.swing.ImageIcon(getClass()
                            .getResource("/com/mx/kylgis/pos/images/ok.png")));
                    m_jNumber.setEnabled(true);
                    m_jAmount.setEnabled(true); 
                    jCBCustomer.setEnabled(true);
                    m_jStatus.setText("A");
                    updateExpiryControls(true);
                    break;
                case "D":
                    jLblStatus.setIcon(new javax.swing.ImageIcon(getClass()
                            .getResource("/com/mx/kylgis/pos/images/refundit.png")));
                    m_jNumber.setEnabled(false);
                    m_jAmount.setEnabled(false);
                    jCBCustomer.setEnabled(false);                    
                    m_jStatus.setText("D");
                    updateExpiryControls(false);
                    
                    break;
                default:
                    jLblStatus.setIcon(null);
                    break;
            }             
            
        } catch (BasicException ex) {
            Logger.getLogger(VoucherEditor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public Object createValue() throws BasicException {
        if (customerInfo == null) {
            throw new BasicException("Voucher requires a selected customer.");
        }

        Object[] attr = new Object[6];

        attr[0] = id;
        attr[1] = VoucherCodeGenerator.normalize(m_jNumber.getText());
        attr[2] = customerInfo.getId();
        attr[3] = Formats.DOUBLE.parseValue(m_jAmount.getText());
        attr[4] = m_jStatus.getText();
        attr[5] = expiresAt;
        
        return attr;
    }    
     
    @Override
    public Component getComponent() {
        return this;
    }
    
    @Override
    public void refresh() {
    }

    private void updateExpiryControls(boolean editable) {
        boolean noExpiry = expiresAt == null;
        jChkNoExpiry.setSelected(noExpiry);
        m_jExpiresAt.setText(noExpiry ? "" : Formats.TIMESTAMP.formatValue(expiresAt));
        m_jExpiresAt.setEnabled(editable && !noExpiry);
        m_jBtnExpiresAt.setEnabled(editable && !noExpiry);
        jChkNoExpiry.setEnabled(editable);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel2 = new javax.swing.JLabel();
        m_jNumber = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        m_jCustomer = new javax.swing.JTextField();
        m_jAmount = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jButtonPrint = new javax.swing.JButton();
        jLblStatus = new javax.swing.JLabel();
        m_jStatus = new javax.swing.JTextField();
        jCBCustomer = new javax.swing.JComboBox<>();
        jLblExpiry = new javax.swing.JLabel();
        m_jExpiresAt = new javax.swing.JTextField();
        m_jBtnExpiresAt = new javax.swing.JButton();
        jChkNoExpiry = new javax.swing.JCheckBox();

        setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N

        jLabel2.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel2.setText(AppLocal.getIntString("label.Number")); // NOI18N
        jLabel2.setPreferredSize(new java.awt.Dimension(100, 30));

        m_jNumber.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        m_jNumber.setPreferredSize(new java.awt.Dimension(240, 30));

        jLabel3.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel3.setText(AppLocal.getIntString("label.customer")); // NOI18N
        jLabel3.setPreferredSize(new java.awt.Dimension(100, 30));

        m_jCustomer.setEditable(false);
        m_jCustomer.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        m_jCustomer.setPreferredSize(new java.awt.Dimension(240, 30));

        m_jAmount.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        m_jAmount.setPreferredSize(new java.awt.Dimension(240, 30));

        jLabel5.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel5.setText(AppLocal.getIntString("label.paymenttotal")); // NOI18N
        jLabel5.setPreferredSize(new java.awt.Dimension(100, 30));

        jButtonPrint.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jButtonPrint.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/mx/kylgis/pos/images/printer24.png"))); // NOI18N
        jButtonPrint.setToolTipText(AppLocal.getIntString("button.print")); // NOI18N
        jButtonPrint.setFocusPainted(false);
        jButtonPrint.setFocusable(false);
        jButtonPrint.setMargin(new java.awt.Insets(8, 14, 8, 14));
        jButtonPrint.setPreferredSize(new java.awt.Dimension(80, 45));
        jButtonPrint.setRequestFocusEnabled(false);
        jButtonPrint.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonPrintActionPerformed(evt);
            }
        });

        jLblStatus.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLblStatus.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/mx/kylgis/pos/images/ok.png"))); // NOI18N
        jLblStatus.setText(AppLocal.getIntString("label.Status")); // NOI18N
        jLblStatus.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        jLblStatus.setPreferredSize(new java.awt.Dimension(100, 30));

        m_jStatus.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        m_jStatus.setPreferredSize(new java.awt.Dimension(240, 30));

        jCBCustomer.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jCBCustomer.setMaximumRowCount(2);
        jCBCustomer.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Find", "Create" }));
        jCBCustomer.setToolTipText(AppLocal.getIntString("label.voucherCustomer")); // NOI18N
        jCBCustomer.setPreferredSize(new java.awt.Dimension(110, 30));
        jCBCustomer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCBCustomerActionPerformed(evt);
            }
        });

        jLblExpiry.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLblExpiry.setText(AppLocal.getIntString("label.voucherexpiry")); // NOI18N
        jLblExpiry.setPreferredSize(new java.awt.Dimension(100, 30));

        m_jExpiresAt.setEditable(false);
        m_jExpiresAt.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        m_jExpiresAt.setPreferredSize(new java.awt.Dimension(200, 30));

        m_jBtnExpiresAt.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/mx/kylgis/pos/images/date.png"))); // NOI18N
        m_jBtnExpiresAt.setToolTipText(AppLocal.getIntString("tooltip.voucherexpiry")); // NOI18N
        m_jBtnExpiresAt.setPreferredSize(new java.awt.Dimension(40, 30));
        m_jBtnExpiresAt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jBtnExpiresAtActionPerformed(evt);
            }
        });

        jChkNoExpiry.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jChkNoExpiry.setSelected(true);
        jChkNoExpiry.setText(AppLocal.getIntString("label.vouchernoexpiry")); // NOI18N
        jChkNoExpiry.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jChkNoExpiryActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(m_jCustomer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jCBCustomer, 0, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(m_jNumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(m_jAmount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLblExpiry, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(m_jExpiresAt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(m_jBtnExpiresAt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jChkNoExpiry)))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLblStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(m_jStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 52, Short.MAX_VALUE)
                        .addComponent(jButtonPrint, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(28, 28, 28))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(11, 11, 11)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(m_jNumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(m_jCustomer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jCBCustomer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(m_jAmount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLblExpiry, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(m_jExpiresAt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(m_jBtnExpiresAt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jChkNoExpiry))
                .addGap(15, 15, 15)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButtonPrint, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLblStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(m_jStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

private void jButtonPrintActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonPrintActionPerformed

    try {
        VoucherInfo  voucherInfo = dlCustomers.getVoucherInfoAll(id.toString());
        BufferedImage image = dlSystem.getResourceAsImage("Window.Logo");
        if (voucherInfo!=null){
            JDialogReportPanel dialog = JDialogReportPanel
                    .getDialog(this,m_app,voucherInfo,image);
            dialog.setVisible(true);
        }
                
    } catch (BasicException ex) {

    }    
}//GEN-LAST:event_jButtonPrintActionPerformed

    private void jCBCustomerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCBCustomerActionPerformed
        if(jCBCustomer.getSelectedIndex() == 0){

            JCustomerFinder finder = JCustomerFinder.getCustomerFinder(this, dlCustomers);
            finder.setVisible(true);
            customerInfo = finder.getSelectedCustomer() ;

            if (finder.getSelectedCustomer()!=null){
                m_jCustomer.setText(customerInfo.getName()); 
            }
        } else {

         JDialogNewCustomer dialog = JDialogNewCustomer.getDialog(this,m_app);
         dialog.setVisible(true);
       
           customerInfo=dialog.getSelectedCustomer();
            if (dialog.getSelectedCustomer()!=null){
                 m_jCustomer.setText(customerInfo.getName());  
            }
        }

    }//GEN-LAST:event_jCBCustomerActionPerformed

    private void m_jBtnExpiresAtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jBtnExpiresAtActionPerformed
        Date selected = expiresAt == null
                ? (insertingVoucher && minimumNewExpiry != null
                        ? minimumNewExpiry
                        : new Date(System.currentTimeMillis() + 5 * 60 * 1000L))
                : expiresAt;
        selected = JCalendarDialog.showCalendarTime(this, selected);
        if (selected != null) {
            if (insertingVoucher && minimumNewExpiry != null
                    && selected.before(minimumNewExpiry)) {
                JOptionPane.showMessageDialog(this,
                        AppLocal.getIntString("message.voucherminimumlifetime"),
                        AppLocal.getIntString("Check"),
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!selected.after(new Date())) {
                JOptionPane.showMessageDialog(this,
                        AppLocal.getIntString("message.voucherexpiryfuture"),
                        AppLocal.getIntString("Check"),
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            expiresAt = selected;
            jChkNoExpiry.setSelected(false);
            updateExpiryControls(true);
        }
    }//GEN-LAST:event_m_jBtnExpiresAtActionPerformed

    private void jChkNoExpiryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jChkNoExpiryActionPerformed
        if (jChkNoExpiry.isSelected()) {
            expiresAt = null;
            updateExpiryControls(true);
        } else {
            m_jExpiresAt.setEnabled(true);
            m_jBtnExpiresAt.setEnabled(true);
            m_jBtnExpiresAt.requestFocusInWindow();
        }
    }//GEN-LAST:event_jChkNoExpiryActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonPrint;
    private javax.swing.JComboBox<String> jCBCustomer;
    private javax.swing.JCheckBox jChkNoExpiry;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLblExpiry;
    private javax.swing.JLabel jLblStatus;
    private javax.swing.JTextField m_jAmount;
    private javax.swing.JButton m_jBtnExpiresAt;
    private javax.swing.JTextField m_jCustomer;
    private javax.swing.JTextField m_jExpiresAt;
    private javax.swing.JTextField m_jNumber;
    private javax.swing.JTextField m_jStatus;
    // End of variables declaration//GEN-END:variables

    public boolean isDataValid() {
        String voucherNumber = VoucherCodeGenerator.normalize(m_jNumber.getText());
        m_jNumber.setText(voucherNumber);

        if (!VoucherCodeGenerator.isValid(voucherNumber)) {
            JOptionPane.showMessageDialog(this,
                    AppLocal.getIntString("message.voucherinvalidnumber"),
                    AppLocal.getIntString("Check"),
                    JOptionPane.WARNING_MESSAGE);
            return false;
        }

        try {
            if (dlCustomers.countOtherVouchersWithNumber(voucherNumber, id.toString()) > 0) {
                JOptionPane.showMessageDialog(this,
                        AppLocal.getIntString("message.voucherduplicatenumber"),
                        AppLocal.getIntString("Check"),
                        JOptionPane.WARNING_MESSAGE);
                return false;
            }
        } catch (BasicException ex) {
            Logger.getLogger(VoucherEditor.class.getName()).log(Level.WARNING,
                    "Unable to verify voucher-number uniqueness", ex);
            JOptionPane.showMessageDialog(this,
                    AppLocal.getIntString("message.vouchercheckfailed"),
                    AppLocal.getIntString("Check"),
                    JOptionPane.WARNING_MESSAGE);
            return false;
        }

        if (customerInfo == null) {
            JOptionPane.showMessageDialog(this,
                    AppLocal.getIntString("message.emptycustomer"),
                    AppLocal.getIntString("Check"),
                    JOptionPane.WARNING_MESSAGE);
            return false;
        }

        if (!jChkNoExpiry.isSelected()) {
            if (expiresAt == null) {
                JOptionPane.showMessageDialog(this,
                        AppLocal.getIntString("message.voucherexpiryrequired"),
                        AppLocal.getIntString("Check"),
                        JOptionPane.WARNING_MESSAGE);
                return false;
            }
            if (insertingVoucher && minimumNewExpiry != null
                    && expiresAt.before(minimumNewExpiry)) {
                JOptionPane.showMessageDialog(this,
                        AppLocal.getIntString("message.voucherminimumlifetime"),
                        AppLocal.getIntString("Check"),
                        JOptionPane.WARNING_MESSAGE);
                return false;
            }
            if ("A".equals(m_jStatus.getText()) && !expiresAt.after(new Date())) {
                JOptionPane.showMessageDialog(this,
                        AppLocal.getIntString("message.voucherexpiryfuture"),
                        AppLocal.getIntString("Check"),
                        JOptionPane.WARNING_MESSAGE);
                return false;
            }
        }

        ValidateBuilder validate = new ValidateBuilder(this);
        validate.setValidate(voucherNumber,ValidateBuilder.IS_NOT_EMPTY,
                AppLocal.getIntString("message.message.emptynumber"));
        validate.setValidate(m_jCustomer.getText(),ValidateBuilder.IS_NOT_EMPTY,
                AppLocal.getIntString("message.emptycustomer"));
        validate.setValidate(m_jAmount.getText(),ValidateBuilder.IS_DOUBLE,
                AppLocal.getIntString("message.numericamount"));
        validate.setValidate(m_jStatus.getText(),ValidateBuilder.IS_NOT_EMPTY,
                AppLocal.getIntString("message.emptystatus"));
        return validate.getValid();
    }
    
    

}
