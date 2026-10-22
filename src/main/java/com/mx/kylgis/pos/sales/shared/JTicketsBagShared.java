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
package com.mx.kylgis.pos.sales.shared;

import com.mx.kylgis.pos.basic.BasicException;
import com.mx.kylgis.pos.beans.JNumberPop;
import com.mx.kylgis.pos.data.gui.MessageInf;
import com.mx.kylgis.pos.forms.AppLocal;
import com.mx.kylgis.pos.forms.AppView;
import com.mx.kylgis.pos.forms.JRootApp;
import com.mx.kylgis.pos.forms.DataLogicSales;
import com.mx.kylgis.pos.forms.DataLogicSystem;
import com.mx.kylgis.pos.sales.DataLogicReceipts;
import com.mx.kylgis.pos.sales.JTicketsBag;
import com.mx.kylgis.pos.sales.ReprintTicketInfo;
import com.mx.kylgis.pos.sales.SharedTicketInfo;
import com.mx.kylgis.pos.sales.TicketsEditor;
import com.mx.kylgis.pos.ticket.TicketInfo;
import java.util.List;
import java.util.UUID;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 *
 * @author JG uniCenta
 */
public class JTicketsBagShared extends JTicketsBag {
    
    private String m_sCurrentTicket = null;
    private final String m_recoveryOwner = newRecoveryOwner();
    private boolean m_recoveryStored;
    private DataLogicReceipts dlReceipts = null;
    private DataLogicSales dlSales = null;  
    private final DataLogicSystem dlSystem;    
    private Boolean showList;
   
    
    private static String newRecoveryOwner() {
        // Existing sharedtickets.LOCKED schemas have VARCHAR(20); no migration is needed.
        byte[] token = new byte[10];
        new java.security.SecureRandom().nextBytes(token);
        return java.util.HexFormat.of().formatHex(token);
    }

    /** Creates new form JTicketsBagShared
     * @param app
     * @param panelticket */
    public JTicketsBagShared(AppView app, TicketsEditor panelticket) {
        
        super(app, panelticket);
        
        dlReceipts = (DataLogicReceipts) app.getBean("com.mx.kylgis.pos.sales.DataLogicReceipts");
        dlSales = (DataLogicSales) app.getBean("com.mx.kylgis.pos.forms.DataLogicSales");
        dlSystem = (DataLogicSystem) m_App.getBean("com.mx.kylgis.pos.forms.DataLogicSystem");        

        initComponents();

        m_jReprintTickets.setVisible(false);
        
    }
    
    /**
     *
     */
    @Override
    public void activate() {
        
        if (m_sCurrentTicket != null) {
            updateCount();
            return;
        }
        m_sCurrentTicket = null;
        selectValidTicket();     
        
        m_jDelTicket.setEnabled(m_App.getAppUserView().getUser().hasPermission(
                "com.mx.kylgis.pos.sales.JPanelTicketEdits"));
        m_jDelTicket.setEnabled(m_App.getAppUserView().getUser().hasPermission("sales.DeleteTicket"));       
    }
    
    /**
     *
     * @return
     */
    @Override
    public boolean deactivate() {
        
        if (!saveCurrentTicket()) {
            return false;
        }
        
        m_sCurrentTicket = null;
        m_panelticket.setActiveTicket(null, null);       
        
        return true;
    }
        
    /**
     *
    */
    @Override
    public void deleteTicket() {
        TicketInfo active = m_panelticket.getActiveTicket();
        if (m_recoveryStored || (active != null && active.requiresPaymentReconciliation())) {
            JOptionPane.showMessageDialog(this,
                    "La venta requiere conciliación y no se puede descartar.\n"
                    + "Identificador de recibo: " + active.getId(),
                    "Venta pendiente de conciliación", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        dlSystem.execTicketRemoved(
            new Object[] {
                m_App.getAppUserView().getUser().getName(),
                "Void",   
                "Ticket Deleted",
                0.0
            });

        m_sCurrentTicket = null;
        selectValidTicket();      
    }

    public void updateCount() {
        try {
            List<SharedTicketInfo> l = dlReceipts.getSharedTicketList();
            int count = l.size();
            
            if (count > 0) {
                m_jListTickets.setText(Integer.toString(count));
            } else {
                m_jListTickets.setText("");
            }
        } catch (BasicException ex) {
            new MessageInf(ex).show(this);
            m_jListTickets.setText("");
        }
    }
   
    
    /**
     *
     * @return
     */
    @Override
    protected JComponent getBagComponent() {
        return this;
    }
    
    /**
     *
     * @return
     */
    @Override
    protected JComponent getNullComponent() {
        return new JPanel();
    }
   
    private boolean saveCurrentTicket() {
        if (m_sCurrentTicket == null) {
            return true;
        }
        TicketInfo ticket = m_panelticket.getActiveTicket();
        try {
            if (ticket == null) {
                throw new BasicException("No active ticket; recovery identity retained.");
            }
            if (m_recoveryStored) {
                if (!dlReceipts.suspendSharedTicketIfOwned(
                        m_sCurrentTicket, ticket, ticket.getPickupId(), m_recoveryOwner)) {
                    throw new BasicException("Recovery ownership is not confirmed; reconcile before leaving.");
                }
            } else if (ticket.getLinesCount() > 0 || ticket.requiresPaymentReconciliation()
                    || !ticket.getPayments().isEmpty() || !ticket.getVoucherDiscounts().isEmpty()
                    || ticket.getProperty("kylgis.qa.durableLedger.operationUuid") != null) {
                dlReceipts.insertSharedTicket(m_sCurrentTicket, ticket, ticket.getPickupId());
            }
            // Clear the working identity only after a confirmed durable save.
            m_sCurrentTicket = null;
            m_recoveryStored = false;
            return true;
        } catch (BasicException | RuntimeException e) {
            if (ticket != null && (!ticket.getPayments().isEmpty()
                    || !ticket.getVoucherDiscounts().isEmpty()
                    || ticket.getProperty("kylgis.qa.durableLedger.operationUuid") != null)) {
                ticket.setProperty(TicketInfo.PAYMENT_RECONCILIATION_PROPERTY, "true");
            }
            JOptionPane.showMessageDialog(this,
                    "No se pudo confirmar la suspensión. La venta permanece abierta.\n"
                    + "No cierre el programa ni vuelva a cobrar; conserve el identificador para revisión.\n"
                    + "Identificador de recibo: " + (ticket == null ? m_sCurrentTicket : ticket.getId()),
                    "No se pudo suspender la venta", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private void setActiveTicket(String id) throws BasicException {
        // Claim first, then read. Competing selectors cannot both remove or open the same row.
        if (!dlReceipts.tryLockSharedTicket(id, m_recoveryOwner)) {
            throw new BasicException("La venta está abierta en otra caja o requiere revisión de recuperación.");
        }
        boolean assigned = false;
        try {
            TicketInfo ticket = dlReceipts.getSharedTicket(id);
            if (ticket == null) {
                throw new BasicException(AppLocal.getIntString("message.noticket"));
            }
            Integer pickUp = dlReceipts.getPickupId(id);
            boolean recovery = ticket.requiresPaymentReconciliation();
            if (!recovery && !dlReceipts.deleteSharedTicketIfOwned(id, m_recoveryOwner)) {
                throw new BasicException("La propiedad del ticket cambió; no se puede recuperar.");
            }
            m_sCurrentTicket = id;
            m_recoveryStored = recovery;
            assigned = true;
            ticket.setPickupId(pickUp);
            m_panelticket.setActiveTicket(ticket, null);
        } finally {
            if (!assigned) {
                dlReceipts.unlockSharedTicketIfOwned(id, m_recoveryOwner);
            }
        }
        updateCount();
    }

    private void setActiveReprintTicket(String id) throws BasicException{
          

        TicketInfo ticket = dlSales.getReprintTicket(id);
            m_sCurrentTicket = id;
    }
    
    private void selectValidTicket() {

        newTicket();
        updateCount();
        
        try {
            List<SharedTicketInfo> l = dlReceipts.getSharedTicketList();
            if (l.isEmpty()) {
                m_jListTickets.setText("");                
                newTicket();
            } else {
                showList = m_App.getAppUserView().getUser().hasPermission("sales.ShowList");
                if (showList) {
                    m_jListTickets.doClick();                   
                }
            }
        } catch (BasicException e) {
            new MessageInf(e).show(this);
            newTicket();
        }    
    }    
    
    private void newTicket() {      
        
        if (!saveCurrentTicket()) {
            return;
        }

        TicketInfo ticket = new TicketInfo();    
        m_sCurrentTicket = UUID.randomUUID().toString();
        m_panelticket.setActiveTicket(ticket, null);      

        updateCount();        
 
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        m_jNewTicket = new javax.swing.JButton();
        m_jListTickets = new javax.swing.JButton();
        m_jDelTicket = new javax.swing.JButton();
        m_jReprintTickets = new javax.swing.JButton();
        m_jHold = new javax.swing.JButton();

        setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        setLayout(new java.awt.BorderLayout());

        m_jNewTicket.setFont(new java.awt.Font("Arial", 0, 11)); // NOI18N
        m_jNewTicket.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/mx/kylgis/pos/images/sale_new.png"))); // NOI18N
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("pos_messages"); // NOI18N
        m_jNewTicket.setToolTipText(bundle.getString("tooltip.addnewticket")); // NOI18N
        m_jNewTicket.setFocusPainted(false);
        m_jNewTicket.setFocusable(false);
        m_jNewTicket.setMargin(new java.awt.Insets(0, 4, 0, 4));
        m_jNewTicket.setMaximumSize(new java.awt.Dimension(50, 40));
        m_jNewTicket.setMinimumSize(new java.awt.Dimension(50, 40));
        m_jNewTicket.setPreferredSize(new java.awt.Dimension(80, 45));
        m_jNewTicket.setRequestFocusEnabled(false);
        m_jNewTicket.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jNewTicketActionPerformed(evt);
            }
        });
        jPanel1.add(m_jNewTicket);

        m_jListTickets.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        m_jListTickets.setForeground(new java.awt.Color(255, 0, 153));
        m_jListTickets.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/mx/kylgis/pos/images/sale_pending.png"))); // NOI18N
        m_jListTickets.setToolTipText(bundle.getString("tooltip.layaway")); // NOI18N
        m_jListTickets.setFocusPainted(false);
        m_jListTickets.setFocusable(false);
        m_jListTickets.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        m_jListTickets.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        m_jListTickets.setIconTextGap(1);
        m_jListTickets.setMargin(new java.awt.Insets(0, 2, 0, 2));
        m_jListTickets.setMaximumSize(new java.awt.Dimension(50, 40));
        m_jListTickets.setMinimumSize(new java.awt.Dimension(50, 40));
        m_jListTickets.setPreferredSize(new java.awt.Dimension(80, 45));
        m_jListTickets.setRequestFocusEnabled(false);
        m_jListTickets.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
        m_jListTickets.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jListTicketsActionPerformed(evt);
            }
        });
        jPanel1.add(m_jListTickets);

        m_jDelTicket.setFont(new java.awt.Font("Arial", 0, 11)); // NOI18N
        m_jDelTicket.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/mx/kylgis/pos/images/sale_delete.png"))); // NOI18N
        m_jDelTicket.setToolTipText(bundle.getString("tooltip.delete")); // NOI18N
        m_jDelTicket.setFocusPainted(false);
        m_jDelTicket.setFocusable(false);
        m_jDelTicket.setMargin(new java.awt.Insets(0, 4, 0, 4));
        m_jDelTicket.setMaximumSize(new java.awt.Dimension(50, 40));
        m_jDelTicket.setMinimumSize(new java.awt.Dimension(50, 40));
        m_jDelTicket.setPreferredSize(new java.awt.Dimension(80, 45));
        m_jDelTicket.setRequestFocusEnabled(false);
        m_jDelTicket.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jDelTicketActionPerformed(evt);
            }
        });
        jPanel1.add(m_jDelTicket);

        m_jReprintTickets.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        m_jReprintTickets.setForeground(new java.awt.Color(255, 0, 153));
        m_jReprintTickets.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/mx/kylgis/pos/images/reprint24.png"))); // NOI18N
        m_jReprintTickets.setToolTipText(bundle.getString("tooltip.reprint")); // NOI18N
        m_jReprintTickets.setFocusPainted(false);
        m_jReprintTickets.setFocusable(false);
        m_jReprintTickets.setIconTextGap(1);
        m_jReprintTickets.setMargin(new java.awt.Insets(0, 2, 0, 2));
        m_jReprintTickets.setMaximumSize(new java.awt.Dimension(50, 40));
        m_jReprintTickets.setMinimumSize(new java.awt.Dimension(50, 40));
        m_jReprintTickets.setPreferredSize(new java.awt.Dimension(80, 45));
        m_jReprintTickets.setRequestFocusEnabled(false);
        m_jReprintTickets.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
        m_jReprintTickets.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jReprintTicketsActionPerformed(evt);
            }
        });
        jPanel1.add(m_jReprintTickets);

        m_jHold.setFont(new java.awt.Font("Arial", 0, 11)); // NOI18N
        m_jHold.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/mx/kylgis/pos/images/logout.png"))); // NOI18N
        m_jHold.setToolTipText(bundle.getString("tooltip.quicklogoff")); // NOI18N
        m_jHold.setFocusPainted(false);
        m_jHold.setFocusable(false);
        m_jHold.setMargin(new java.awt.Insets(0, 4, 0, 4));
        m_jHold.setMaximumSize(new java.awt.Dimension(50, 40));
        m_jHold.setMinimumSize(new java.awt.Dimension(50, 40));
        m_jHold.setPreferredSize(new java.awt.Dimension(80, 45));
        m_jHold.setRequestFocusEnabled(false);
        m_jHold.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jHoldActionPerformed(evt);
            }
        });
        jPanel1.add(m_jHold);

        add(jPanel1, java.awt.BorderLayout.WEST);
    }// </editor-fold>//GEN-END:initComponents

    private void m_jListTicketsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jListTicketsActionPerformed

        SwingUtilities.invokeLater(() -> {
            try {
                if (!m_App.getAppUserView().getUser().hasPermission("sales.ViewSharedTicket")){
                    JOptionPane.showMessageDialog(null,
                            AppLocal.getIntString("message.sharedticket"),
                            AppLocal.getIntString("message.sharedtickettitle"),
                            JOptionPane.INFORMATION_MESSAGE);                
                }else{

                    if("0".equals(m_App.getAppUserView().getUser().getRole())
                            || "1".equals(m_App.getAppUserView().getUser().getRole())
                            || m_App.getAppUserView().getUser().hasPermission("sales.ViewSharedTicket")
                            || m_App.getAppUserView().getUser().hasPermission("sales.Override"))
                    {
                        List<SharedTicketInfo> l = dlReceipts.getSharedTicketList();
                        JTicketsBagSharedList listDialog = JTicketsBagSharedList.newJDialog(JTicketsBagShared.this);
                        String id = listDialog.showTicketsList(l, dlReceipts);

                        if (id != null) {
                            if (!saveCurrentTicket()) { return; }
                            setActiveTicket(id); 
                        }
                    }else{

                        String appuser = m_App.getAppUserView().getUser().getId();
                        List<SharedTicketInfo> l = dlReceipts.getUserSharedTicketList(
                                appuser);
                        
                        JTicketsBagSharedList listDialog = JTicketsBagSharedList.newJDialog(JTicketsBagShared.this);
                        
                        String id = listDialog.showTicketsList(l, dlReceipts);
                        
                        if (id != null) {
                            if (!saveCurrentTicket()) { return; }
                            setActiveTicket(id); 
                        }
                    }
                }
            }catch (BasicException e) {
                new MessageInf(e).show(JTicketsBagShared.this);
                newTicket();                    
            }
        });
        
    }//GEN-LAST:event_m_jListTicketsActionPerformed

    private void m_jNewTicketActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jNewTicketActionPerformed

        newTicket();
        
    }//GEN-LAST:event_m_jNewTicketActionPerformed

    private void m_jHoldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jHoldActionPerformed
        if (!deactivate()) {
            return;
        }
        ((JRootApp)m_App).closeAppView();
    }//GEN-LAST:event_m_jHoldActionPerformed

    private void m_jReprintTicketsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jReprintTicketsActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {

                try {
                    if (!m_App.getAppUserView().getUser().hasPermission("sales.ReprintTicket")){
                        JOptionPane.showMessageDialog(null,
                            AppLocal.getIntString("message.reprintticket"),
                            AppLocal.getIntString("message.reprinttickettitle"),
                            JOptionPane.INFORMATION_MESSAGE);
                    }else{

                        if("0".equals(m_App.getAppUserView().getUser().getRole())
                            || "1".equals(m_App.getAppUserView().getUser().getRole())
                            || m_App.getAppUserView().getUser().hasPermission("sales.ViewSharedTicket")
                            || m_App.getAppUserView().getUser().hasPermission("sales.Override"))
                        {
                            List<ReprintTicketInfo> l = dlSales.getReprintTicketList();
                            JTicketsReprintList listDialog = JTicketsReprintList.newJDialog(JTicketsBagShared.this);
                            String id = listDialog.showTicketsList(l, dlSales);

                        }else{

                            String appuser = m_App.getAppUserView().getUser().getId();
                            List<ReprintTicketInfo> l = dlSales.getReprintTicketList();

                            JTicketsReprintList listDialog = JTicketsReprintList.newJDialog(JTicketsBagShared.this);

                            String id = listDialog.showTicketsList(l, dlSales);

                            if (id != null) {
                                if (!saveCurrentTicket()) { return; }
                                setActiveReprintTicket(id);
                            }
                        }
                    }
                }catch (BasicException e) {
                    new MessageInf(e).show(JTicketsBagShared.this);
                    newTicket();
                }
            }
        });
    }//GEN-LAST:event_m_jReprintTicketsActionPerformed

    private void m_jDelTicketActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jDelTicketActionPerformed
// Only called in simple & standard NOT RESTAURANT
// For Restaurant-mode see JTicketsBagRestaurant
        
        boolean pinOK = false;

        if (m_sCurrentTicket != null) {

            if (m_App.getProperties().getProperty("override.check").equals("true")) {
                Integer secret = Integer.parseInt(m_App.getProperties().getProperty("override.pin"));
                Integer iValue = JNumberPop.showEditNumber(this, AppLocal.getIntString("title.override.enterpin")); 

                if (iValue == null ? secret == null : iValue.equals(secret)) {
                    pinOK = true;
                    int res = JOptionPane.showConfirmDialog(this
                        , AppLocal.getIntString("message.wannadelete")
                        , AppLocal.getIntString("title.editor")
                        , JOptionPane.YES_NO_OPTION
                        , JOptionPane.QUESTION_MESSAGE);
        
                    if (res == JOptionPane.YES_OPTION) {
                        deleteTicket();
                    }                    
                } else {
                    pinOK = false;
                    JOptionPane.showMessageDialog(this, AppLocal.getIntString("message.override.badpin"));                                        
                }
            } else {
                int res = JOptionPane.showConfirmDialog(this
                    , AppLocal.getIntString("message.wannadelete")
                    , AppLocal.getIntString("title.editor")
                    , JOptionPane.YES_NO_OPTION
                    , JOptionPane.QUESTION_MESSAGE);
        
                if (res == JOptionPane.YES_OPTION) {
                    deleteTicket();
                }
            }
        }
    }//GEN-LAST:event_m_jDelTicketActionPerformed
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jPanel1;
    private javax.swing.JButton m_jDelTicket;
    private javax.swing.JButton m_jHold;
    private javax.swing.JButton m_jListTickets;
    private javax.swing.JButton m_jNewTicket;
    private javax.swing.JButton m_jReprintTickets;
    // End of variables declaration//GEN-END:variables
    
}