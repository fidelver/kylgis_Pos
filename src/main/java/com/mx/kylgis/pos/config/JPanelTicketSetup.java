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
package com.mx.kylgis.pos.config;

import com.mx.kylgis.pos.data.user.DirtyManager;
import java.awt.Component;
import javax.swing.SpinnerNumberModel;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import com.mx.kylgis.pos.forms.AppConfig;
import com.mx.kylgis.pos.forms.AppLocal;
import com.mx.kylgis.pos.util.AltEncrypter;
import javax.swing.JOptionPane;

/**
 *
 * @author JG uniCenta
 */
public class JPanelTicketSetup extends javax.swing.JPanel implements PanelConfig {
    
    private final DirtyManager dirty = new DirtyManager();
    private String receipt="1";
    private Integer x = 0;
    private String receiptSize;
    private String pickupSize;
    private final Integer ps = 0;

    private Connection conn;
    private String sdbmanager;
    private String SQL;
    private Statement stmt;  
    private boolean loadingTicketFormat;
    private final String defaultDigitalPath = new java.io.File(System.getProperty("user.home"), "KylGisPOS").getPath();
    private javax.swing.JPanel ticketSetupContainer;
    private javax.swing.JCheckBox screenMirrorCheck;
    private javax.swing.JCheckBox digitalMirrorCheck;
    private javax.swing.JTextField digitalMirrorPath;
    private javax.swing.JButton digitalMirrorBrowse;
    private javax.swing.JComboBox<String> receiptPrinterRoute;
    
    /**
     *
     */
    public JPanelTicketSetup() {
        
        initComponents();
        
        jReceiptSize.addChangeListener(dirty);
        jPickupSize.addChangeListener(dirty);
        jTextReceiptPrefix.getDocument().addDocumentListener(dirty);
        m_jReceiptPrintOff.addActionListener(dirty);
        jcboTicketPaper.addActionListener(dirty);
        jTicketWidth.addChangeListener(dirty);

        initTicketOutputControls();
        jbtnReset.setVisible(true);
    }
    
        
    /**
     *
     * @return
     */
    @Override
    public boolean hasChanged() {
        return dirty.isDirty();
    }
    
    /**
     *
     * @return
     */
    @Override
    public Component getConfigComponent() {
        return ticketSetupContainer;
    }
   
    private void initTicketOutputControls() {
        javax.swing.JPanel outputs = new javax.swing.JPanel(new java.awt.BorderLayout(6, 5));
        outputs.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createTitledBorder("Salidas del ticket"),
                javax.swing.BorderFactory.createEmptyBorder(4, 8, 6, 8)));

        javax.swing.JLabel note = new javax.swing.JLabel(
                "<html><b>Nota:</b> Pantalla y Ticket digital son copias de la impresora principal. "
                + "Se recomienda usar Printer 1 para los recibos.</html>");
        outputs.add(note, java.awt.BorderLayout.NORTH);

        javax.swing.JPanel checks = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        receiptPrinterRoute = new javax.swing.JComboBox<>(new String[]{
                "Printer 1", "Printer 2", "Printer 3", "Printer 4", "Printer 5", "Printer 6"});
        checks.add(new javax.swing.JLabel("Impresora lógica del recibo:"));
        checks.add(receiptPrinterRoute);
        screenMirrorCheck = new javax.swing.JCheckBox("Mostrar copia del recibo en pantalla");
        digitalMirrorCheck = new javax.swing.JCheckBox("Generar ticket digital");
        checks.add(screenMirrorCheck);
        checks.add(digitalMirrorCheck);
        outputs.add(checks, java.awt.BorderLayout.CENTER);

        javax.swing.JPanel path = new javax.swing.JPanel(new java.awt.BorderLayout(6, 0));
        path.add(new javax.swing.JLabel("Carpeta base:"), java.awt.BorderLayout.WEST);
        digitalMirrorPath = new javax.swing.JTextField(defaultDigitalPath);
        digitalMirrorBrowse = new javax.swing.JButton("Examinar...");
        path.add(digitalMirrorPath, java.awt.BorderLayout.CENTER);
        path.add(digitalMirrorBrowse, java.awt.BorderLayout.EAST);
        outputs.add(path, java.awt.BorderLayout.SOUTH);

        receiptPrinterRoute.addActionListener(dirty);
        screenMirrorCheck.addActionListener(dirty);
        digitalMirrorCheck.addActionListener(dirty);
        digitalMirrorCheck.addActionListener(e -> updateDigitalControls());
        digitalMirrorPath.getDocument().addDocumentListener(dirty);
        digitalMirrorBrowse.addActionListener(e -> chooseDigitalPath());

        setPreferredSize(new java.awt.Dimension(700, 330));
        ticketSetupContainer = new javax.swing.JPanel(new java.awt.BorderLayout(0, 5));
        ticketSetupContainer.setOpaque(false);
        ticketSetupContainer.add(this, java.awt.BorderLayout.CENTER);
        ticketSetupContainer.add(outputs, java.awt.BorderLayout.SOUTH);
        ticketSetupContainer.setPreferredSize(new java.awt.Dimension(700, 500));
        updateDigitalControls();
    }

    private static String normalizeReceiptPrinter(String value) {
        if (value != null && value.trim().matches("[1-6]")) {
            return value.trim();
        }
        return "1";
    }

    private String selectedReceiptPrinter() {
        Object selected = receiptPrinterRoute == null ? null : receiptPrinterRoute.getSelectedItem();
        if (selected != null) {
            String value = selected.toString().replace("Printer", "").trim();
            return normalizeReceiptPrinter(value);
        }
        return "1";
    }

    private void updateDigitalControls() {
        boolean enabled = digitalMirrorCheck != null && digitalMirrorCheck.isSelected();
        if (digitalMirrorPath != null) digitalMirrorPath.setEnabled(enabled);
        if (digitalMirrorBrowse != null) digitalMirrorBrowse.setEnabled(enabled);
    }

    private void chooseDigitalPath() {
        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser(digitalMirrorPath.getText());
        chooser.setDialogTitle("Seleccionar carpeta base para tickets digitales");
        chooser.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showOpenDialog(ticketSetupContainer) == javax.swing.JFileChooser.APPROVE_OPTION) {
            digitalMirrorPath.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    /**
     *
     * @param config
     */
    @Override
    public void loadProperties(AppConfig config) {

        receiptSize =(config.getProperty("till.receiptsize"));
        if (receiptSize == null || "".equals(receiptSize)){
            jReceiptSize.setModel(new SpinnerNumberModel(1,1,20,1));
        } else {            
            jReceiptSize.setModel(new SpinnerNumberModel(Integer.parseInt(receiptSize),1,20,1));
        }                

        pickupSize =(config.getProperty("till.pickupsize"));
        if (pickupSize == null || "".equals(pickupSize)){
            jPickupSize.setModel(new SpinnerNumberModel(1,1,20,1));
        } else {            
            jPickupSize.setModel(new SpinnerNumberModel(Integer.parseInt(pickupSize),1,20,1));
        }        
        
        jTextReceiptPrefix.setText(config.getProperty("till.receiptprefix"));        
// build the example receipt using the loaded details        
        receipt="";
        x=1;
        while (x < (Integer)jReceiptSize.getValue()){
            receipt += "0";
        x++; 
    }
         
        receipt += "1";
         jTicketExample.setText(jTextReceiptPrefix.getText()+receipt);  
         m_jReceiptPrintOff.setSelected(Boolean.parseBoolean(config.getProperty("till.receiptprintoff")));

         String paperSize = config.getProperty("ticket.paper.size");
         String ticketWidth = config.getProperty("ticket.width");
         int width = 32;
         try {
             width = Integer.parseInt(ticketWidth);
         } catch (Exception ex) {
             width = 32;
         }
         width = Math.max(31, Math.min(120, width));
         if (!"80".equals(paperSize) && !"120".equals(paperSize)) {
             paperSize = "58";
         }
         TicketFormatSettings.set(paperSize, width);
         loadingTicketFormat = true;
         try {
             jcboTicketPaper.setSelectedItem(paperSize + " mm");
             jTicketWidth.setValue(width);
         } finally {
             loadingTicketFormat = false;
         }

        String receiptPrinter = normalizeReceiptPrinter(config.getProperty("ticket.printer"));
        receiptPrinterRoute.setSelectedItem("Printer " + receiptPrinter);
        screenMirrorCheck.setSelected(Boolean.parseBoolean(config.getProperty("screen.ticket.enabled")));
        digitalMirrorCheck.setSelected(Boolean.parseBoolean(config.getProperty("digital.ticket.enabled")));
        String mirrorPath = config.getProperty("digital.ticket.path");
        digitalMirrorPath.setText(mirrorPath == null || mirrorPath.trim().isEmpty() ? defaultDigitalPath : mirrorPath.trim());
        updateDigitalControls();

        dirty.setDirty(false);

        
    }
    
    /*
     * JG Oct 2017 
     * This block to be used for internal SETS/RESETS and external ORDERS sync's  
    */    
    public void loadUp() throws ClassNotFoundException, SQLException {    

/* Add external received order reset block here - 
 * Get connex to secondary or external system's DB + [params]
 * Pref' use is JSON/REST rather than PreparedStatement
*/        
    }
    
    /**
     *
     * @param config
     */
    @Override
    public void saveProperties(AppConfig config) {
        
        config.setProperty("till.receiptprefix", jTextReceiptPrefix.getText());
        config.setProperty("till.receiptsize", jReceiptSize.getValue().toString());
        config.setProperty("till.pickupsize", jPickupSize.getValue().toString());        
        config.setProperty("till.receiptprintoff",Boolean.toString(m_jReceiptPrintOff.isSelected()));
        config.setProperty("ticket.paper.size", TicketFormatSettings.getPaperSize());
        config.setProperty("ticket.width", Integer.toString(TicketFormatSettings.getWidth()));
        config.setProperty("ticket.printer", selectedReceiptPrinter());
        config.setProperty("screen.ticket.enabled", Boolean.toString(screenMirrorCheck.isSelected()));
        config.setProperty("digital.ticket.enabled", Boolean.toString(digitalMirrorCheck.isSelected()));
        String mirrorPath = digitalMirrorPath.getText() == null ? "" : digitalMirrorPath.getText().trim();
        config.setProperty("digital.ticket.path", mirrorPath.isEmpty() ? defaultDigitalPath : mirrorPath);

        dirty.setDirty(false);
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTextField2 = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jReceiptSize = new javax.swing.JSpinner();
        jLabel3 = new javax.swing.JLabel();
        jTextReceiptPrefix = new javax.swing.JTextField();
        jTicketExample = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jPickupSize = new javax.swing.JSpinner();
        m_jReceiptPrintOff = new javax.swing.JCheckBox();
        jbtnReset = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        jcboTicketPaper = new javax.swing.JComboBox<>();
        jLabel5 = new javax.swing.JLabel();
        jTicketWidth = new javax.swing.JSpinner();

        jTextField2.setText("jTextField2");

        setBackground(new java.awt.Color(255, 255, 255));
        setOpaque(false);
        setPreferredSize(new java.awt.Dimension(700, 500));

        jLabel1.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("pos_messages"); // NOI18N
        jLabel1.setText(bundle.getString("label.ticketsetupnumber")); // NOI18N
        jLabel1.setPreferredSize(new java.awt.Dimension(190, 30));

        jReceiptSize.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N
        jReceiptSize.setModel(new javax.swing.SpinnerNumberModel(0, 0, null, 1));
        jReceiptSize.setPreferredSize(new java.awt.Dimension(50, 30));
        jReceiptSize.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jReceiptSizeStateChanged(evt);
            }
        });

        jLabel3.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel3.setText(bundle.getString("label.ticketsetupprefix")); // NOI18N

        jTextReceiptPrefix.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N
        jTextReceiptPrefix.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        jTextReceiptPrefix.setPreferredSize(new java.awt.Dimension(100, 30));
        jTextReceiptPrefix.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                jTextReceiptPrefixKeyReleased(evt);
            }
        });

        jTicketExample.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N
        jTicketExample.setText("1");
        jTicketExample.setDisabledTextColor(new java.awt.Color(0, 0, 0));
        jTicketExample.setEnabled(false);
        jTicketExample.setPreferredSize(new java.awt.Dimension(100, 30));

        jLabel2.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel2.setText(bundle.getString("label.pickupcodesize")); // NOI18N
        jLabel2.setPreferredSize(new java.awt.Dimension(190, 30));

        jPickupSize.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N
        jPickupSize.setModel(new javax.swing.SpinnerNumberModel(0, 0, null, 1));
        jPickupSize.setToolTipText("");
        jPickupSize.setPreferredSize(new java.awt.Dimension(50, 30));
        jPickupSize.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jPickupSizeStateChanged(evt);
            }
        });

        m_jReceiptPrintOff.setBackground(new java.awt.Color(255, 255, 255));
        m_jReceiptPrintOff.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        m_jReceiptPrintOff.setText(bundle.getString("label.receiptprint")); // NOI18N
        m_jReceiptPrintOff.setOpaque(false);
        m_jReceiptPrintOff.setPreferredSize(new java.awt.Dimension(180, 30));
        m_jReceiptPrintOff.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jReceiptPrintOffActionPerformed(evt);
            }
        });

        jbtnReset.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jbtnReset.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/mx/kylgis/pos/images/reload.png"))); // NOI18N
        jbtnReset.setText(AppLocal.getIntString("label.resetpickup")); // NOI18N
        jbtnReset.setMaximumSize(new java.awt.Dimension(70, 33));
        jbtnReset.setMinimumSize(new java.awt.Dimension(70, 33));
        jbtnReset.setPreferredSize(new java.awt.Dimension(100, 45));
        jbtnReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbtnResetActionPerformed(evt);
            }
        });

        jLabel4.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel4.setText(bundle.getString("label.ticketpapersize")); // NOI18N
        jLabel4.setPreferredSize(new java.awt.Dimension(190, 30));

        jcboTicketPaper.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jcboTicketPaper.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "58 mm", "80 mm", "120 mm" }));
        jcboTicketPaper.setPreferredSize(new java.awt.Dimension(130, 30));
        jcboTicketPaper.setToolTipText(bundle.getString("tooltip.ticketpapersize"));
        jcboTicketPaper.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jcboTicketPaperActionPerformed(evt);
            }
        });

        jLabel5.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel5.setText(bundle.getString("label.ticketwidth")); // NOI18N

        jTicketWidth.setFont(new java.awt.Font("Arial", 0, 16)); // NOI18N
        jTicketWidth.setModel(new javax.swing.SpinnerNumberModel(32, 31, 120, 1));
        jTicketWidth.setPreferredSize(new java.awt.Dimension(70, 30));
        jTicketWidth.setToolTipText(bundle.getString("tooltip.ticketwidth"));
        jTicketWidth.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jTicketWidthStateChanged(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jcboTicketPaper, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jTicketWidth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(m_jReceiptPrintOff, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                            .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(18, 18, 18)
                            .addComponent(jPickupSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(18, 18, 18)
                            .addComponent(jbtnReset, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGap(18, 18, 18)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jReceiptSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGroup(layout.createSequentialGroup()
                                    .addComponent(jTextReceiptPrefix, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                    .addComponent(jTicketExample, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jReceiptSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextReceiptPrefix, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTicketExample, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(m_jReceiptPrintOff, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jcboTicketPaper, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5)
                    .addComponent(jTicketWidth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jPickupSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jbtnReset, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(241, 241, 241))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jTextReceiptPrefixKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextReceiptPrefixKeyReleased

        jTicketExample.setText(jTextReceiptPrefix.getText()+ receipt);
    }//GEN-LAST:event_jTextReceiptPrefixKeyReleased

    private void jReceiptSizeStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jReceiptSizeStateChanged

        receipt="";
        x=1;
        while (x < (Integer)jReceiptSize.getValue()){
            receipt += "0";
        x++; 
    }
        receipt += "1";
         jTicketExample.setText(jTextReceiptPrefix.getText()+receipt);
         
    }//GEN-LAST:event_jReceiptSizeStateChanged

    private void jPickupSizeStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jPickupSizeStateChanged

    }//GEN-LAST:event_jPickupSizeStateChanged

    private void m_jReceiptPrintOffActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jReceiptPrintOffActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_m_jReceiptPrintOffActionPerformed

    private void jcboTicketPaperActionPerformed(java.awt.event.ActionEvent evt) {
        if (loadingTicketFormat) return;
        String selected = String.valueOf(jcboTicketPaper.getSelectedItem());
        String paper = selected.startsWith("80") ? "80" : selected.startsWith("120") ? "120" : "58";
        int preset = TicketFormatSettings.presetWidth(paper);
        loadingTicketFormat = true;
        try {
            jTicketWidth.setValue(preset);
        } finally {
            loadingTicketFormat = false;
        }
        TicketFormatSettings.set(paper, preset);
    }

    private void jTicketWidthStateChanged(javax.swing.event.ChangeEvent evt) {
        if (loadingTicketFormat) return;
        String selected = String.valueOf(jcboTicketPaper.getSelectedItem());
        String paper = selected.startsWith("80") ? "80" : selected.startsWith("120") ? "120" : "58";
        TicketFormatSettings.set(paper, ((Number) jTicketWidth.getValue()).intValue());
    }

    private void jbtnResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbtnResetActionPerformed
        int response = JOptionPane.showOptionDialog(null,
            AppLocal.getIntString("message.resetpickup"),
                    "Reset",
                    JOptionPane.YES_NO_OPTION, 
                    JOptionPane.QUESTION_MESSAGE,
                    null, null, null);
        if (response == JOptionPane.YES_OPTION) {
            try {

                String db_url = (AppConfig.getInstance().getProperty("db.URL"));
                String db_schema = (AppConfig.getInstance().getProperty("db.schema"));            
                String db_user =(AppConfig.getInstance().getProperty("db.user"));            
                String db_password = (AppConfig.getInstance().getProperty("db.password"));
            
                if (db_user != null && db_password != null && db_password.startsWith("crypt:")) {
                        AltEncrypter cypher = new AltEncrypter("cypherkey" + db_user);
                    db_password = cypher.decrypt(db_password.substring(6));
                }

                String url = db_url + db_schema;            
            
                conn = DriverManager.getConnection(url,db_user,db_password);
                sdbmanager = conn.getMetaData().getDatabaseProductName();
                stmt = (Statement) conn.createStatement();
            
                if ("MySQL".equals(sdbmanager)) {
                    SQL = "UPDATE pickup_number SET id = 0";
                    try {
                        stmt.executeUpdate(SQL);
                    } catch (SQLException e){
                        System.out.println(e.getMessage()); 
                    }
                } else if ("PostgreSQL".equals(sdbmanager)) {
                    SQL = "ALTER SEQUENCE pickup_number RESTART WITH 1";
                    try {
                        stmt.executeUpdate(SQL);
                    } catch (SQLException e) {
                        System.out.println(e.getMessage());
                    }
                }
            } catch (SQLException ex) {
                Logger.getLogger(JPanelTicketSetup.class.getName()).log(Level.SEVERE, null, ex);
            }
        }        
    }//GEN-LAST:event_jbtnResetActionPerformed
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JComboBox<String> jcboTicketPaper;
    private javax.swing.JSpinner jTicketWidth;
    private javax.swing.JSpinner jPickupSize;
    private javax.swing.JSpinner jReceiptSize;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextReceiptPrefix;
    private javax.swing.JTextField jTicketExample;
    private javax.swing.JButton jbtnReset;
    private javax.swing.JCheckBox m_jReceiptPrintOff;
    // End of variables declaration//GEN-END:variables
    
}
