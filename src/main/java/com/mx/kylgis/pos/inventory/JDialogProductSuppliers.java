//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.inventory;

import com.mx.kylgis.pos.basic.BasicException;
import com.mx.kylgis.pos.forms.AppLocal;
import com.mx.kylgis.pos.suppliers.DataLogicSuppliers;
import com.mx.kylgis.pos.suppliers.ProductSupplierInfo;
import com.mx.kylgis.pos.suppliers.SupplierInfo;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

/**
 * Edits the N:M supplier associations of one persisted product.
 *
 * The legacy products.supplier field is synchronized by DataLogicSuppliers with
 * the preferred active association, so old reports/import paths remain
 * compatible while the product can keep any number of suppliers.
 */
public final class JDialogProductSuppliers extends JDialog {

    private final DataLogicSuppliers logic;
    private final String productId;
    private final SupplierTableModel model;
    private boolean saved;

    private JDialogProductSuppliers(Window owner, DataLogicSuppliers logic,
            String productId, List<SupplierInfo> supplierChoices,
            List<ProductSupplierInfo> current) {
        super(owner, AppLocal.getIntString("dialog.productsuppliers.title"),
                ModalityType.APPLICATION_MODAL);
        this.logic = logic;
        this.productId = productId;
        this.model = new SupplierTableModel(productId, supplierChoices, current);
        initUi();
    }

    public static boolean showDialog(Component parent, DataLogicSuppliers logic,
            String productId, List<SupplierInfo> supplierChoices) throws BasicException {
        List<ProductSupplierInfo> current = logic.getProductSuppliers(productId);
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        JDialogProductSuppliers dialog = new JDialogProductSuppliers(
                owner, logic, productId, supplierChoices, current);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
        return dialog.saved;
    }

    private void initUi() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        JLabel help = new JLabel(AppLocal.getIntString("message.productsuppliers.help"));
        help.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 0, 8));
        add(help, BorderLayout.NORTH);

        JTable table = new JTable(model);
        table.setRowHeight(26);
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(65);
        table.getColumnModel().getColumn(1).setPreferredWidth(70);
        table.getColumnModel().getColumn(2).setPreferredWidth(55);
        table.getColumnModel().getColumn(3).setPreferredWidth(250);
        table.getColumnModel().getColumn(4).setPreferredWidth(110);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(620, 340));
        add(scroll, BorderLayout.CENTER);

        JButton save = new JButton(AppLocal.getIntString("button.save"));
        save.addActionListener(e -> saveChanges());
        JButton cancel = new JButton(AppLocal.getIntString("button.cancel"));
        cancel.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancel);
        buttons.add(save);
        add(buttons, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(save);
        pack();
    }

    private void saveChanges() {
        try {
            logic.replaceProductSuppliers(productId, model.toAssociations());
            saved = true;
            dispose();
        } catch (BasicException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    AppLocal.getIntString("dialog.productsuppliers.title"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static final class SupplierRow {
        private final SupplierInfo supplier;
        private final ProductSupplierInfo existing;
        private boolean associated;
        private boolean preferred;
        private boolean active;

        private SupplierRow(SupplierInfo supplier, ProductSupplierInfo existing) {
            this.supplier = supplier;
            this.existing = existing;
            this.associated = existing != null;
            this.preferred = existing != null && existing.isPreferred();
            this.active = existing == null || existing.isActive();
        }
    }

    private static final class SupplierTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {
            AppLocal.getIntString("label.productsupplier.associated"),
            AppLocal.getIntString("label.productsupplier.preferred"),
            AppLocal.getIntString("label.productsupplier.active"),
            AppLocal.getIntString("label.supplier"),
            AppLocal.getIntString("label.searchkey")
        };

        private final String productId;
        private final List<SupplierRow> rows = new ArrayList<>();

        private SupplierTableModel(String productId, List<SupplierInfo> choices,
                List<ProductSupplierInfo> current) {
            this.productId = productId;
            Map<String, ProductSupplierInfo> associations = new HashMap<>();
            if (current != null) {
                for (ProductSupplierInfo association : current) {
                    associations.put(association.getSupplierId(), association);
                }
            }

            Map<String, SupplierInfo> merged = new LinkedHashMap<>();
            if (choices != null) {
                for (SupplierInfo supplier : choices) {
                    if (supplier != null && supplier.getID() != null) {
                        merged.put(supplier.getID(), supplier);
                    }
                }
            }
            // Preserve existing associations even when a supplier is currently
            // hidden from the normal visible-supplier combo.
            if (current != null) {
                for (ProductSupplierInfo association : current) {
                    if (!merged.containsKey(association.getSupplierId())) {
                        merged.put(association.getSupplierId(), new SupplierInfo(
                                association.getSupplierId(),
                                association.getSupplierSearchKey(),
                                association.getSupplierName()));
                    }
                }
            }

            for (SupplierInfo supplier : merged.values()) {
                rows.add(new SupplierRow(supplier, associations.get(supplier.getID())));
            }
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex < 3 ? Boolean.class : String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex < 3;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            SupplierRow row = rows.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return row.associated;
                case 1:
                    return row.preferred;
                case 2:
                    return row.active;
                case 3:
                    return row.supplier.getName();
                case 4:
                    return row.supplier.getSearchkey();
                default:
                    return null;
            }
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            SupplierRow row = rows.get(rowIndex);
            boolean selected = Boolean.TRUE.equals(value);
            if (columnIndex == 0) {
                row.associated = selected;
                if (!selected) {
                    row.preferred = false;
                    row.active = false;
                } else if (!row.active) {
                    row.active = true;
                }
            } else if (columnIndex == 1) {
                if (selected) {
                    for (SupplierRow other : rows) {
                        other.preferred = false;
                    }
                    row.associated = true;
                    row.active = true;
                    row.preferred = true;
                    fireTableRowsUpdated(0, rows.size() - 1);
                    return;
                }
                row.preferred = false;
            } else if (columnIndex == 2) {
                row.active = selected;
                if (selected) {
                    row.associated = true;
                } else {
                    row.preferred = false;
                }
            }
            fireTableRowsUpdated(rowIndex, rowIndex);
        }

        private List<ProductSupplierInfo> toAssociations() {
            boolean hasPreferred = false;
            SupplierRow firstActive = null;
            for (SupplierRow row : rows) {
                if (row.associated && row.active && firstActive == null) {
                    firstActive = row;
                }
                if (row.associated && row.active && row.preferred) {
                    hasPreferred = true;
                }
            }
            if (!hasPreferred && firstActive != null) {
                firstActive.preferred = true;
            }

            List<ProductSupplierInfo> result = new ArrayList<>();
            for (SupplierRow row : rows) {
                if (!row.associated) {
                    continue;
                }
                ProductSupplierInfo old = row.existing;
                result.add(new ProductSupplierInfo(
                        productId,
                        row.supplier.getID(),
                        row.supplier.getName(),
                        row.supplier.getSearchkey(),
                        row.preferred,
                        row.active,
                        old == null ? null : old.getSupplierSku(),
                        old == null ? null : old.getPurchaseUom(),
                        old == null ? null : old.getLastCost(),
                        old == null ? null : old.getPriority(),
                        old == null ? null : old.getLastPurchase()));
            }
            return result;
        }

    }
}
