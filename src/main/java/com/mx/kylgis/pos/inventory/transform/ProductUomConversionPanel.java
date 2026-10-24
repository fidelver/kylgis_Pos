//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.inventory.transform;

import com.mx.kylgis.pos.basic.BasicException;
import com.mx.kylgis.pos.forms.AppLocal;
import com.mx.kylgis.pos.forms.AppView;
import com.mx.kylgis.pos.forms.JPanelView;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.AbstractTableModel;

/** Product-specific alternate-UOM conversion registry. */
public final class ProductUomConversionPanel extends JPanel implements JPanelView {

    private final ProductUomConversionService conversionService;
    private final RecipeUiCatalogService catalogService;

    private final JComboBox<RecipeUiCatalogService.ProductChoice> product = new JComboBox<>();
    private final JComboBox<RecipeUiCatalogService.UomChoice> fromUom = new JComboBox<>();
    private final JLabel inventoryUom = new JLabel(" ");
    private final JTextField factor = new JTextField("1", 12);
    private final JCheckBox active = new JCheckBox();
    private final JLabel explanation = new JLabel(" ");
    private final JButton save = new JButton();
    private final JButton reload = new JButton();
    private final JButton toggle = new JButton();
    private final ConversionTableModel model = new ConversionTableModel();
    private final JTable table = new JTable(model);

    private volatile boolean busy;
    private List<RecipeUiCatalogService.UomChoice> uoms = Collections.emptyList();

    public ProductUomConversionPanel(AppView app) {
        if (app == null) {
            throw new IllegalArgumentException("app is required");
        }
        this.conversionService = new ProductUomConversionService(app.getSession());
        this.catalogService = new RecipeUiCatalogService(app.getSession());
        buildUi();
        bindActions();
        setBusy(false);
    }

    @Override
    public String getTitle() {
        return AppLocal.getIntString("Menu.ProductUomConversions");
    }

    @Override
    public void activate() throws BasicException {
        loadCatalogsAsync();
    }

    @Override
    public boolean deactivate() {
        return !busy;
    }

    @Override
    public JComponent getComponent() {
        return this;
    }

    private void buildUi() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel title = new JLabel(getTitle());
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder(
                AppLocal.getIntString("uomconv.editor")));
        GridBagConstraints c = constraints();
        addRow(form, c, 0, AppLocal.getIntString("uomconv.product"), product);
        addRow(form, c, 1, AppLocal.getIntString("uomconv.inventory.uom"), inventoryUom);
        addRow(form, c, 2, AppLocal.getIntString("uomconv.from.uom"), fromUom);
        addRow(form, c, 3, AppLocal.getIntString("uomconv.factor"), factor);
        active.setText(AppLocal.getIntString("uomconv.active"));
        c.gridx = 1;
        c.gridy = 4;
        c.gridwidth = 2;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(active, c);
        c.gridy = 5;
        form.add(explanation, c);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        reload.setText(AppLocal.getIntString("recipe.reload"));
        save.setText(AppLocal.getIntString("uomconv.save"));
        actions.add(reload);
        actions.add(save);
        c.gridy = 6;
        form.add(actions, c);
        add(form, BorderLayout.WEST);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        JPanel list = new JPanel(new BorderLayout(4, 4));
        list.setBorder(BorderFactory.createTitledBorder(
                AppLocal.getIntString("uomconv.list")));
        list.add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel tableActions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        toggle.setText(AppLocal.getIntString("uomconv.toggle"));
        tableActions.add(toggle);
        list.add(tableActions, BorderLayout.SOUTH);
        add(list, BorderLayout.CENTER);
    }

    private void bindActions() {
        product.addActionListener(e -> {
            updateInventoryUomLabel();
            loadConversionsForSelection();
        });
        fromUom.addActionListener(e -> updateExplanation());
        factor.getDocument().addDocumentListener(new SimpleDocumentListener(this::updateExplanation));
        reload.addActionListener(e -> loadCatalogsAsync());
        save.addActionListener(e -> saveAsync());
        toggle.addActionListener(e -> toggleSelectedAsync());
        table.getSelectionModel().addListSelectionListener(this::selectionChanged);
    }

    private void loadCatalogsAsync() {
        if (busy) return;
        setBusy(true);
        new SwingWorker<CatalogSnapshot, Void>() {
            @Override
            protected CatalogSnapshot doInBackground() throws Exception {
                return new CatalogSnapshot(catalogService.listProducts(), catalogService.listUoms());
            }

            @Override
            protected void done() {
                try {
                    CatalogSnapshot snapshot = get();
                    uoms = snapshot.uoms;
                    product.setModel(new DefaultComboBoxModel<>(snapshot.products.toArray(
                            new RecipeUiCatalogService.ProductChoice[0])));
                    fromUom.setModel(new DefaultComboBoxModel<>(snapshot.uoms.toArray(
                            new RecipeUiCatalogService.UomChoice[0])));
                    updateInventoryUomLabel();
                    updateExplanation();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    showError(AppLocal.getIntString("uomconv.error.catalog"), ex);
                } catch (ExecutionException ex) {
                    showError(AppLocal.getIntString("uomconv.error.catalog"), rootCause(ex));
                } finally {
                    setBusy(false);
                    loadConversionsForSelection();
                }
            }
        }.execute();
    }

    private void loadConversionsForSelection() {
        if (busy) return;
        RecipeUiCatalogService.ProductChoice selected = selectedProduct();
        if (selected == null) {
            model.setRows(Collections.emptyList());
            return;
        }
        setBusy(true);
        new SwingWorker<List<ProductUomConversionService.ConversionInfo>, Void>() {
            @Override
            protected List<ProductUomConversionService.ConversionInfo> doInBackground()
                    throws Exception {
                return conversionService.listForProduct(selected.id);
            }

            @Override
            protected void done() {
                try {
                    model.setRows(get());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    showError(AppLocal.getIntString("uomconv.error.load"), ex);
                } catch (ExecutionException ex) {
                    showError(AppLocal.getIntString("uomconv.error.load"), rootCause(ex));
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void saveAsync() {
        if (busy) return;
        RecipeUiCatalogService.ProductChoice selectedProduct = selectedProduct();
        RecipeUiCatalogService.UomChoice selectedUom = selectedUom();
        if (selectedProduct == null || selectedUom == null) {
            showWarning(AppLocal.getIntString("uomconv.error.selection"));
            return;
        }
        final BigDecimal conversionFactor;
        try {
            conversionFactor = new BigDecimal(factor.getText().trim());
            if (conversionFactor.signum() <= 0) throw new NumberFormatException();
        } catch (RuntimeException ex) {
            showWarning(AppLocal.getIntString("uomconv.error.factor"));
            return;
        }
        setBusy(true);
        new SwingWorker<ProductUomConversionService.ConversionInfo, Void>() {
            @Override
            protected ProductUomConversionService.ConversionInfo doInBackground() throws Exception {
                return conversionService.saveToInventory(selectedProduct.id, selectedUom.id,
                        conversionFactor, active.isSelected());
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(ProductUomConversionPanel.this,
                            AppLocal.getIntString("uomconv.saved"), getTitle(),
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    showError(AppLocal.getIntString("uomconv.error.save"), ex);
                } catch (ExecutionException ex) {
                    showError(AppLocal.getIntString("uomconv.error.save"), rootCause(ex));
                } finally {
                    setBusy(false);
                    loadConversionsForSelection();
                }
            }
        }.execute();
    }

    private void toggleSelectedAsync() {
        if (busy) return;
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            showWarning(AppLocal.getIntString("uomconv.error.row"));
            return;
        }
        ProductUomConversionService.ConversionInfo selected =
                model.get(table.convertRowIndexToModel(viewRow));
        boolean next = !selected.active;
        setBusy(true);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                conversionService.setActive(selected.productId, selected.fromUomId, next);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    showError(AppLocal.getIntString("uomconv.error.toggle"), ex);
                } catch (ExecutionException ex) {
                    showError(AppLocal.getIntString("uomconv.error.toggle"), rootCause(ex));
                } finally {
                    setBusy(false);
                    loadConversionsForSelection();
                }
            }
        }.execute();
    }

    private void selectionChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) return;
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return;
        ProductUomConversionService.ConversionInfo selected =
                model.get(table.convertRowIndexToModel(viewRow));
        selectUom(selected.fromUomId);
        factor.setText(selected.factor.stripTrailingZeros().toPlainString());
        active.setSelected(selected.active);
        updateExplanation();
    }

    private void updateInventoryUomLabel() {
        RecipeUiCatalogService.ProductChoice selected = selectedProduct();
        inventoryUom.setText(selected == null ? " " : uomLabel(selected.uomId));
        updateExplanation();
    }

    private void updateExplanation() {
        RecipeUiCatalogService.ProductChoice selectedProduct = selectedProduct();
        RecipeUiCatalogService.UomChoice selectedUom = selectedUom();
        if (selectedProduct == null || selectedUom == null) {
            explanation.setText(" ");
            return;
        }
        String text = factor.getText() == null ? "" : factor.getText().trim();
        explanation.setText("1 " + selectedUom + " = " + text + " "
                + uomLabel(selectedProduct.uomId));
    }

    private String uomLabel(String id) {
        for (RecipeUiCatalogService.UomChoice uom : uoms) {
            if (uom != null && id != null && id.equals(uom.id)) {
                return uom.toString();
            }
        }
        return id == null ? "" : id;
    }

    private void selectUom(String id) {
        for (int i = 0; i < fromUom.getItemCount(); i++) {
            RecipeUiCatalogService.UomChoice item = fromUom.getItemAt(i);
            if (item != null && id.equals(item.id)) {
                fromUom.setSelectedIndex(i);
                return;
            }
        }
    }

    private RecipeUiCatalogService.ProductChoice selectedProduct() {
        Object value = product.getSelectedItem();
        return value instanceof RecipeUiCatalogService.ProductChoice
                ? (RecipeUiCatalogService.ProductChoice) value : null;
    }

    private RecipeUiCatalogService.UomChoice selectedUom() {
        Object value = fromUom.getSelectedItem();
        return value instanceof RecipeUiCatalogService.UomChoice
                ? (RecipeUiCatalogService.UomChoice) value : null;
    }

    private void setBusy(boolean value) {
        busy = value;
        setCursor(value ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
        product.setEnabled(!value);
        fromUom.setEnabled(!value);
        factor.setEnabled(!value);
        active.setEnabled(!value);
        save.setEnabled(!value);
        reload.setEnabled(!value);
        toggle.setEnabled(!value);
    }

    private static GridBagConstraints constraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.WEST;
        return c;
    }

    private static void addRow(JPanel panel, GridBagConstraints c, int row,
            String label, JComponent field) {
        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        panel.add(new JLabel(label), c);
        c.gridx = 1;
        c.gridwidth = 2;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, c);
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, getTitle(), JOptionPane.WARNING_MESSAGE);
    }

    private void showError(String message, Throwable error) {
        String detail = error == null || error.getMessage() == null || error.getMessage().isBlank()
                ? "" : "\n" + error.getMessage();
        JOptionPane.showMessageDialog(this, message + detail, getTitle(), JOptionPane.ERROR_MESSAGE);
    }

    private static Throwable rootCause(ExecutionException error) {
        return error.getCause() == null ? error : error.getCause();
    }

    private static final class CatalogSnapshot {
        final List<RecipeUiCatalogService.ProductChoice> products;
        final List<RecipeUiCatalogService.UomChoice> uoms;

        CatalogSnapshot(List<RecipeUiCatalogService.ProductChoice> products,
                List<RecipeUiCatalogService.UomChoice> uoms) {
            this.products = products == null ? Collections.emptyList() : products;
            this.uoms = uoms == null ? Collections.emptyList() : uoms;
        }
    }

    private static final class ConversionTableModel extends AbstractTableModel {
        private final String[] columns = {
            AppLocal.getIntString("uomconv.from.uom"),
            AppLocal.getIntString("uomconv.inventory.uom"),
            AppLocal.getIntString("uomconv.factor"),
            AppLocal.getIntString("uomconv.active")
        };
        private List<ProductUomConversionService.ConversionInfo> rows = Collections.emptyList();

        void setRows(List<ProductUomConversionService.ConversionInfo> values) {
            rows = values == null ? Collections.emptyList() : new ArrayList<>(values);
            fireTableDataChanged();
        }

        ProductUomConversionService.ConversionInfo get(int index) {
            return rows.get(index);
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int column) { return columns[column]; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            ProductUomConversionService.ConversionInfo row = rows.get(rowIndex);
            switch (columnIndex) {
                case 0: return row.fromUomId;
                case 1: return row.toUomId;
                case 2: return row.factor;
                case 3: return row.active;
                default: return null;
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 3 ? Boolean.class : Object.class;
        }
    }

    private static final class SimpleDocumentListener implements javax.swing.event.DocumentListener {
        private final Runnable action;
        SimpleDocumentListener(Runnable action) { this.action = action; }
        @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { action.run(); }
        @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { action.run(); }
        @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { action.run(); }
    }
}