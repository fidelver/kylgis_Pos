//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.voucher;

import com.mx.kylgis.pos.basic.BasicException;
import com.mx.kylgis.pos.forms.AppView;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

/** Modal UI for batch voucher campaign creation and assignment report. */
public final class VoucherCampaignDialog extends JDialog {

    private final AppView app;
    private final VoucherCampaignService service;
    private final JTextField campaignName = new JTextField(22);
    private final JTextField prefix = new JTextField(18);
    private final JTextField amount = new JTextField(10);
    private final JTextField expiry = new JTextField(10);
    private final JRadioButton byCustomers = new JRadioButton("Clientes seleccionados", true);
    private final JRadioButton byQuantity = new JRadioButton("Cantidad libre", false);
    private final JSpinner quantity = new JSpinner(new SpinnerNumberModel(10, 1, 10000, 1));
    private final CustomerModel customerModel = new CustomerModel();
    private final ReportModel reportModel = new ReportModel();
    private final JTable customerTable = new JTable(customerModel);
    private final JTable reportTable = new JTable(reportModel);
    private final JButton createButton = new JButton("Crear campaña");
    private final JButton copyReportButton = new JButton("Copiar reporte");

    private VoucherCampaignDialog(AppView app, Component owner) throws BasicException {
        super(SwingUtilities.getWindowAncestor(owner), "Campañas de cupones", ModalityType.APPLICATION_MODAL);
        this.app = app;
        this.service = new VoucherCampaignService(app.getSession());
        buildUi();
        loadCustomers();
        pack();
        setMinimumSize(new Dimension(980, 680));
        setLocationRelativeTo(owner);
    }

    public static void showDialog(AppView app, Component owner) {
        try {
            new VoucherCampaignDialog(app, owner).setVisible(true);
        } catch (BasicException ex) {
            JOptionPane.showMessageDialog(owner, ex.getMessage(), "Campañas de cupones",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void buildUi() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setContentPane(root);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 4, 3, 4);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;
        int row = 0;
        addField(form, c, row++, "Nombre de campaña:", campaignName);
        addField(form, c, row++, "Prefijo de cupón:", prefix);
        addField(form, c, row++, "Importe de cada cupón:", amount);
        addField(form, c, row++, "Vigencia hasta (AAAA-MM-DD):", expiry);

        JPanel mode = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        ButtonGroup group = new ButtonGroup();
        group.add(byCustomers);
        group.add(byQuantity);
        mode.add(byCustomers);
        mode.add(byQuantity);
        mode.add(new JLabel("Cantidad:"));
        mode.add(quantity);
        c.gridx = 0; c.gridy = row; c.gridwidth = 2; c.weightx = 1;
        form.add(mode, c);
        root.add(form, BorderLayout.NORTH);

        customerTable.setAutoCreateRowSorter(true);
        customerTable.setRowHeight(24);
        customerTable.getColumnModel().getColumn(0).setMaxWidth(70);
        reportTable.setAutoCreateRowSorter(true);
        reportTable.setRowHeight(24);

        JPanel customersPanel = new JPanel(new BorderLayout());
        customersPanel.setBorder(BorderFactory.createTitledBorder("Clientes de la campaña"));
        customersPanel.add(new JScrollPane(customerTable), BorderLayout.CENTER);

        JPanel reportPanel = new JPanel(new BorderLayout());
        reportPanel.setBorder(BorderFactory.createTitledBorder("Reporte generado"));
        reportPanel.add(new JScrollPane(reportTable), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, customersPanel, reportPanel);
        split.setResizeWeight(0.55);
        split.setPreferredSize(new Dimension(940, 500));
        root.add(split, BorderLayout.CENTER);

        JButton close = new JButton("Cerrar");
        copyReportButton.setEnabled(false);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(copyReportButton);
        buttons.add(createButton);
        buttons.add(close);
        root.add(buttons, BorderLayout.SOUTH);

        byCustomers.addActionListener(e -> updateMode());
        byQuantity.addActionListener(e -> updateMode());
        createButton.addActionListener(e -> createCampaign());
        copyReportButton.addActionListener(e -> copyReport());
        close.addActionListener(e -> dispose());
        updateMode();
    }

    private static void addField(JPanel panel, GridBagConstraints c, int row,
            String label, Component field) {
        c.gridy = row; c.gridx = 0; c.gridwidth = 1; c.weightx = 0;
        panel.add(new JLabel(label), c);
        c.gridx = 1; c.weightx = 1;
        panel.add(field, c);
    }

    private void loadCustomers() throws BasicException {
        customerModel.setRows(service.listVisibleCustomers());
    }

    private void updateMode() {
        boolean customers = byCustomers.isSelected();
        customerTable.setEnabled(customers);
        quantity.setEnabled(!customers);
    }

    private void createCampaign() {
        createButton.setEnabled(false);
        try {
            double campaignAmount = Double.parseDouble(amount.getText().trim().replace(',', '.'));
            Date expiresAt = parseExpiry(expiry.getText());
            String operatorId = app.getAppUserView().getUser().getId();
            VoucherCampaignService.CampaignResult result;
            if (byCustomers.isSelected()) {
                List<String> selected = customerModel.selectedIds();
                result = service.createForCustomers(campaignName.getText(), prefix.getText(),
                        campaignAmount, expiresAt, selected, operatorId);
            } else {
                result = service.createByQuantity(campaignName.getText(), prefix.getText(),
                        campaignAmount, expiresAt, (Integer) quantity.getValue(), operatorId);
            }
            reportModel.setRows(result.rows);
            copyReportButton.setEnabled(!result.rows.isEmpty());
            JOptionPane.showMessageDialog(this,
                    "Campaña creada: " + result.campaignName + "\nCupones: " + result.rows.size(),
                    "Campañas de cupones", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "El importe debe ser numérico y mayor que cero.",
                    "Campañas de cupones", JOptionPane.WARNING_MESSAGE);
        } catch (BasicException | IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Campañas de cupones",
                    JOptionPane.ERROR_MESSAGE);
        } finally {
            createButton.setEnabled(true);
        }
    }

    private static Date parseExpiry(String value) throws BasicException {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            LocalDate day = LocalDate.parse(value.trim());
            return Date.from(day.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant());
        } catch (RuntimeException ex) {
            throw new BasicException("La vigencia debe usar el formato AAAA-MM-DD.", ex);
        }
    }

    private void copyReport() {
        StringBuilder out = new StringBuilder("Cliente\tTeléfono\tCupón\tImporte\tEstado\n");
        for (VoucherCampaignService.CampaignVoucherRow row : reportModel.rows) {
            out.append(row.customerName).append('\t')
                    .append(row.phone).append('\t')
                    .append(row.voucherCode).append('\t')
                    .append(row.amount).append('\t')
                    .append(row.status).append('\n');
        }
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(out.toString()), null);
        JOptionPane.showMessageDialog(this, "Reporte copiado al portapapeles.",
                "Campañas de cupones", JOptionPane.INFORMATION_MESSAGE);
    }

    private static final class CustomerModel extends AbstractTableModel {
        private final List<VoucherCampaignService.CustomerTarget> rows = new ArrayList<>();
        private final List<Boolean> selected = new ArrayList<>();
        private final String[] columns = {"Elegir", "Cliente", "Teléfono", "Email"};

        void setRows(List<VoucherCampaignService.CustomerTarget> values) {
            rows.clear(); selected.clear();
            rows.addAll(values);
            for (int i = 0; i < rows.size(); i++) selected.add(Boolean.FALSE);
            fireTableDataChanged();
        }

        List<String> selectedIds() {
            List<String> ids = new ArrayList<>();
            for (int i = 0; i < rows.size(); i++) {
                if (selected.get(i)) ids.add(rows.get(i).id);
            }
            return ids;
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int column) { return columns[column]; }
        @Override public Class<?> getColumnClass(int column) { return column == 0 ? Boolean.class : String.class; }
        @Override public boolean isCellEditable(int row, int column) { return column == 0; }
        @Override public Object getValueAt(int row, int column) {
            VoucherCampaignService.CustomerTarget value = rows.get(row);
            return switch (column) {
                case 0 -> selected.get(row);
                case 1 -> value.name;
                case 2 -> value.preferredPhone();
                case 3 -> value.email;
                default -> "";
            };
        }
        @Override public void setValueAt(Object value, int row, int column) {
            if (column == 0) {
                selected.set(row, Boolean.TRUE.equals(value));
                fireTableCellUpdated(row, column);
            }
        }
    }

    private static final class ReportModel extends AbstractTableModel {
        private final List<VoucherCampaignService.CampaignVoucherRow> rows = new ArrayList<>();
        private final String[] columns = {"Cliente", "Teléfono", "Cupón", "Importe", "Estado", "Vigencia"};

        void setRows(List<VoucherCampaignService.CampaignVoucherRow> values) {
            rows.clear(); rows.addAll(values); fireTableDataChanged();
        }
        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int column) { return columns[column]; }
        @Override public Object getValueAt(int row, int column) {
            VoucherCampaignService.CampaignVoucherRow value = rows.get(row);
            return switch (column) {
                case 0 -> value.customerName;
                case 1 -> value.phone;
                case 2 -> value.voucherCode;
                case 3 -> value.amount;
                case 4 -> value.status;
                case 5 -> value.expiresAt == null ? "Sin caducidad" : value.expiresAt;
                default -> "";
            };
        }
    }
}
