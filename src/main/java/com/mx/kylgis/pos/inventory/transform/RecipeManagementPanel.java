//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.inventory.transform;

import com.mx.kylgis.pos.basic.BasicException;
import com.mx.kylgis.pos.forms.AppLocal;
import com.mx.kylgis.pos.forms.AppView;
import com.mx.kylgis.pos.forms.JPanelView;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.AbstractTableModel;

/**
 * Versioned recipe administration. ACTIVE recipes are immutable: this panel
 * always creates a new DRAFT and uses explicit activate/retire operations.
 */
public final class RecipeManagementPanel extends JPanel implements JPanelView {

    private final AppView app;
    private final RecipeDefinitionService definitionService;
    private final RecipeTransformationQueryService queryService;
    private final RecipeUiCatalogService catalogService;
    private final String currentUser;

    private final JComboBox<RecipeUiCatalogService.ProductChoice> resultProduct = new JComboBox<>();
    private final JComboBox<RecipeUiCatalogService.UomChoice> yieldUom = new JComboBox<>();
    private final JTextField yieldQuantity = new JTextField("1", 10);
    private final JTextArea notes = new JTextArea(3, 28);

    private final JComboBox<RecipeUiCatalogService.ProductChoice> componentProduct = new JComboBox<>();
    private final JComboBox<RecipeUiCatalogService.UomChoice> componentUom = new JComboBox<>();
    private final JTextField componentQuantity = new JTextField("1", 8);
    private final JTextField componentWaste = new JTextField("", 6);
    private final ComponentTableModel componentModel = new ComponentTableModel();
    private final JTable componentTable = new JTable(componentModel);

    private final VersionTableModel versionModel = new VersionTableModel();
    private final JTable versionTable = new JTable(versionModel);
    private final ComponentPreviewTableModel previewModel = new ComponentPreviewTableModel();
    private final JTable previewTable = new JTable(previewModel);
    private final JLabel selectedVersion = new JLabel(" ");

    private final JButton addComponent = new JButton();
    private final JButton removeComponent = new JButton();
    private final JButton clearDraft = new JButton();
    private final JButton saveDraft = new JButton();
    private final JButton refresh = new JButton();
    private final JButton activateVersion = new JButton();
    private final JButton retireVersion = new JButton();

    private volatile boolean busy;
    private List<RecipeUiCatalogService.ProductChoice> products = Collections.emptyList();
    private List<RecipeUiCatalogService.UomChoice> uoms = Collections.emptyList();

    public RecipeManagementPanel(AppView app) {
        if (app == null) {
            throw new IllegalArgumentException("app is required");
        }
        this.app = app;
        this.definitionService = new RecipeDefinitionService(app.getSession());
        this.queryService = new RecipeTransformationQueryService(app.getSession());
        this.catalogService = new RecipeUiCatalogService(app.getSession());
        this.currentUser = app.getAppUserView().getUser().getName();
        buildUi();
        bindActions();
        setBusy(false);
    }

    @Override
    public String getTitle() {
        return AppLocal.getIntString("Menu.Recipes");
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

        JLabel title = new JLabel(AppLocal.getIntString("Menu.Recipes"));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        add(title, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildDraftPanel(), buildVersionsPanel());
        split.setResizeWeight(0.56d);
        split.setContinuousLayout(true);
        add(split, BorderLayout.CENTER);
    }

    private JPanel buildDraftPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder(AppLocal.getIntString("recipe.draft.title")));

        JPanel header = new JPanel(new GridBagLayout());
        GridBagConstraints c = constraints();
        addRow(header, c, 0, AppLocal.getIntString("recipe.result.product"), resultProduct);
        addRow(header, c, 1, AppLocal.getIntString("recipe.yield.quantity"), yieldQuantity);
        addRow(header, c, 2, AppLocal.getIntString("recipe.yield.uom"), yieldUom);
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        header.add(new JLabel(AppLocal.getIntString("recipe.notes")), c);
        c.gridx = 1;
        c.gridwidth = 3;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        JScrollPane notesScroll = new JScrollPane(notes);
        notesScroll.setPreferredSize(new Dimension(300, 70));
        header.add(notesScroll, c);
        panel.add(header, BorderLayout.NORTH);

        componentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        componentTable.setFillsViewportHeight(true);
        JScrollPane componentScroll = new JScrollPane(componentTable);
        componentScroll.setBorder(BorderFactory.createTitledBorder(
                AppLocal.getIntString("recipe.components")));
        panel.add(componentScroll, BorderLayout.CENTER);

        JPanel componentEditor = new JPanel(new GridBagLayout());
        componentEditor.setBorder(BorderFactory.createTitledBorder(
                AppLocal.getIntString("recipe.component.add")));
        GridBagConstraints e = constraints();
        addRow(componentEditor, e, 0, AppLocal.getIntString("recipe.component.product"), componentProduct);
        addRow(componentEditor, e, 1, AppLocal.getIntString("recipe.component.quantity"), componentQuantity);
        addRow(componentEditor, e, 2, AppLocal.getIntString("recipe.component.uom"), componentUom);
        addRow(componentEditor, e, 3, AppLocal.getIntString("recipe.component.waste"), componentWaste);

        JPanel componentButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        addComponent.setText(AppLocal.getIntString("recipe.component.add.button"));
        removeComponent.setText(AppLocal.getIntString("recipe.component.remove.button"));
        componentButtons.add(addComponent);
        componentButtons.add(removeComponent);
        e.gridx = 1;
        e.gridy = 4;
        e.gridwidth = 3;
        e.weightx = 1;
        e.fill = GridBagConstraints.HORIZONTAL;
        componentEditor.add(componentButtons, e);

        JPanel draftActions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        clearDraft.setText(AppLocal.getIntString("button.clean"));
        saveDraft.setText(AppLocal.getIntString("recipe.create.draft"));
        draftActions.add(clearDraft);
        draftActions.add(saveDraft);

        JPanel south = new JPanel(new BorderLayout());
        south.add(componentEditor, BorderLayout.CENTER);
        south.add(draftActions, BorderLayout.SOUTH);
        panel.add(south, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildVersionsPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder(AppLocal.getIntString("recipe.versions")));

        versionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        versionTable.setFillsViewportHeight(true);
        JScrollPane versions = new JScrollPane(versionTable);
        versions.setPreferredSize(new Dimension(440, 240));
        panel.add(versions, BorderLayout.NORTH);

        JPanel detail = new JPanel(new BorderLayout(4, 4));
        selectedVersion.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        detail.add(selectedVersion, BorderLayout.NORTH);
        previewTable.setFillsViewportHeight(true);
        detail.add(new JScrollPane(previewTable), BorderLayout.CENTER);
        panel.add(detail, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        refresh.setText(AppLocal.getIntString("recipe.reload"));
        activateVersion.setText(AppLocal.getIntString("recipe.activate"));
        retireVersion.setText(AppLocal.getIntString("recipe.retire"));
        actions.add(refresh);
        actions.add(activateVersion);
        actions.add(retireVersion);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private static GridBagConstraints constraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 3, 3, 3);
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
        c.gridwidth = 3;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, c);
    }

    private void bindActions() {
        resultProduct.addActionListener(e -> {
            RecipeUiCatalogService.ProductChoice product = selectedProduct(resultProduct);
            if (product != null) {
                selectUom(yieldUom, product.uomId);
                loadVersionsAsync(product.id);
            } else {
                versionModel.setRows(Collections.emptyList());
                previewModel.setRows(Collections.emptyList());
            }
        });
        componentProduct.addActionListener(e -> {
            RecipeUiCatalogService.ProductChoice product = selectedProduct(componentProduct);
            if (product != null) {
                selectUom(componentUom, product.uomId);
            }
        });
        addComponent.addActionListener(e -> addDraftComponent());
        removeComponent.addActionListener(e -> removeSelectedDraftComponent());
        clearDraft.addActionListener(e -> resetDraft());
        saveDraft.addActionListener(e -> createDraftAsync());
        refresh.addActionListener(e -> reloadVersionsForSelectedProduct());
        activateVersion.addActionListener(e -> mutateSelectedVersion(true));
        retireVersion.addActionListener(e -> mutateSelectedVersion(false));
        versionTable.getSelectionModel().addListSelectionListener(this::versionSelectionChanged);
    }

    private void loadCatalogsAsync() {
        if (busy) {
            return;
        }
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
                    products = snapshot.products;
                    uoms = snapshot.uoms;
                    resultProduct.setModel(new DefaultComboBoxModel<>(
                            products.toArray(new RecipeUiCatalogService.ProductChoice[0])));
                    componentProduct.setModel(new DefaultComboBoxModel<>(
                            products.toArray(new RecipeUiCatalogService.ProductChoice[0])));
                    yieldUom.setModel(new DefaultComboBoxModel<>(
                            uoms.toArray(new RecipeUiCatalogService.UomChoice[0])));
                    componentUom.setModel(new DefaultComboBoxModel<>(
                            uoms.toArray(new RecipeUiCatalogService.UomChoice[0])));
                    RecipeUiCatalogService.ProductChoice selected = selectedProduct(resultProduct);
                    if (selected != null) {
                        selectUom(yieldUom, selected.uomId);
                    }
                    RecipeUiCatalogService.ProductChoice component = selectedProduct(componentProduct);
                    if (component != null) {
                        selectUom(componentUom, component.uomId);
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    showError(AppLocal.getIntString("recipe.error.catalog"), ex);
                } catch (ExecutionException ex) {
                    showError(AppLocal.getIntString("recipe.error.catalog"), rootCause(ex));
                } finally {
                    setBusy(false);
                    reloadVersionsForSelectedProduct();
                }
            }
        }.execute();
    }

    private void loadVersionsAsync(String productId) {
        if (busy || isBlank(productId)) {
            return;
        }
        setBusy(true);
        new SwingWorker<List<RecipeTransformationQueryService.RecipeVersionSummary>, Void>() {
            @Override
            protected List<RecipeTransformationQueryService.RecipeVersionSummary> doInBackground()
                    throws Exception {
                return queryService.listRecipeVersions(productId);
            }

            @Override
            protected void done() {
                try {
                    versionModel.setRows(get());
                    previewModel.setRows(Collections.emptyList());
                    selectedVersion.setText(" ");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    showError(AppLocal.getIntString("recipe.error.versions"), ex);
                } catch (ExecutionException ex) {
                    showError(AppLocal.getIntString("recipe.error.versions"), rootCause(ex));
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void loadVersionDetailAsync(RecipeTransformationQueryService.RecipeVersionSummary version) {
        if (busy || version == null) {
            return;
        }
        setBusy(true);
        new SwingWorker<RecipeTransformationQueryService.RecipeVersionDetail, Void>() {
            @Override
            protected RecipeTransformationQueryService.RecipeVersionDetail doInBackground()
                    throws Exception {
                return queryService.getRecipeVersion(version.id);
            }

            @Override
            protected void done() {
                try {
                    RecipeTransformationQueryService.RecipeVersionDetail detail = get();
                    previewModel.setRows(detail.components);
                    selectedVersion.setText(AppLocal.getIntString("recipe.selected.version")
                            + " " + detail.summary.versionNo + " — " + detail.summary.statusLabel());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    showError(AppLocal.getIntString("recipe.error.detail"), ex);
                } catch (ExecutionException ex) {
                    showError(AppLocal.getIntString("recipe.error.detail"), rootCause(ex));
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void createDraftAsync() {
        if (busy) {
            return;
        }
        RecipeUiCatalogService.ProductChoice output = selectedProduct(resultProduct);
        RecipeUiCatalogService.UomChoice outputUom = selectedUom(yieldUom);
        if (output == null || outputUom == null) {
            showWarning(AppLocal.getIntString("recipe.error.result"));
            return;
        }
        final BigDecimal yield;
        try {
            yield = positiveDecimal(yieldQuantity.getText(), AppLocal.getIntString("recipe.yield.quantity"));
        } catch (BasicException ex) {
            showWarning(ex.getMessage());
            return;
        }
        List<RecipeDefinitionService.ComponentDraft> components = componentModel.toDrafts();
        if (components.isEmpty()) {
            showWarning(AppLocal.getIntString("recipe.error.components"));
            return;
        }
        String id = UUID.randomUUID().toString();
        String noteText = trimToNull(notes.getText());
        setBusy(true);
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return definitionService.createNextDraft(id, output.id, yield,
                        outputUom.id, currentUser, noteText, components);
            }

            @Override
            protected void done() {
                try {
                    get();
                    componentModel.clear();
                    notes.setText("");
                    JOptionPane.showMessageDialog(RecipeManagementPanel.this,
                            AppLocal.getIntString("recipe.draft.created"), getTitle(),
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    showError(AppLocal.getIntString("recipe.error.create"), ex);
                } catch (ExecutionException ex) {
                    showError(AppLocal.getIntString("recipe.error.create"), rootCause(ex));
                } finally {
                    setBusy(false);
                    loadVersionsAsync(output.id);
                }
            }
        }.execute();
    }

    private void mutateSelectedVersion(boolean activate) {
        if (busy) {
            return;
        }
        RecipeTransformationQueryService.RecipeVersionSummary version = selectedVersionRow();
        if (version == null) {
            showWarning(AppLocal.getIntString("recipe.error.select.version"));
            return;
        }
        if (activate && !"DRAFT".equalsIgnoreCase(version.status)) {
            showWarning(AppLocal.getIntString("recipe.error.activate.draft"));
            return;
        }
        if (!activate && "RETIRED".equalsIgnoreCase(version.status)) {
            return;
        }
        int confirmation = JOptionPane.showConfirmDialog(this,
                AppLocal.getIntString(activate ? "recipe.confirm.activate" : "recipe.confirm.retire"),
                getTitle(), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirmation != JOptionPane.YES_OPTION) {
            return;
        }
        RecipeUiCatalogService.ProductChoice product = selectedProduct(resultProduct);
        setBusy(true);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                if (activate) {
                    definitionService.activate(version.id);
                } else {
                    definitionService.retire(version.id);
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    showError(AppLocal.getIntString("recipe.error.update"), ex);
                } catch (ExecutionException ex) {
                    showError(AppLocal.getIntString("recipe.error.update"), rootCause(ex));
                } finally {
                    setBusy(false);
                    if (product != null) {
                        loadVersionsAsync(product.id);
                    }
                }
            }
        }.execute();
    }

    private void addDraftComponent() {
        RecipeUiCatalogService.ProductChoice output = selectedProduct(resultProduct);
        RecipeUiCatalogService.ProductChoice component = selectedProduct(componentProduct);
        RecipeUiCatalogService.UomChoice uom = selectedUom(componentUom);
        if (component == null || uom == null) {
            showWarning(AppLocal.getIntString("recipe.error.component"));
            return;
        }
        if (output != null && output.id.equals(component.id)) {
            showWarning(AppLocal.getIntString("recipe.error.self.component"));
            return;
        }
        final BigDecimal quantity;
        final BigDecimal waste;
        try {
            quantity = positiveDecimal(componentQuantity.getText(),
                    AppLocal.getIntString("recipe.component.quantity"));
            waste = optionalRatio(componentWaste.getText());
        } catch (BasicException ex) {
            showWarning(ex.getMessage());
            return;
        }
        if (componentModel.containsProduct(component.id)) {
            showWarning(AppLocal.getIntString("recipe.error.duplicate.component"));
            return;
        }
        componentModel.add(new DraftComponentRow(component, quantity, uom, waste));
    }

    private void removeSelectedDraftComponent() {
        int row = componentTable.getSelectedRow();
        if (row >= 0) {
            componentModel.remove(componentTable.convertRowIndexToModel(row));
        }
    }

    private void resetDraft() {
        componentModel.clear();
        notes.setText("");
        yieldQuantity.setText("1");
        componentQuantity.setText("1");
        componentWaste.setText("");
    }

    private void reloadVersionsForSelectedProduct() {
        RecipeUiCatalogService.ProductChoice selected = selectedProduct(resultProduct);
        if (selected != null) {
            loadVersionsAsync(selected.id);
        }
    }

    private void versionSelectionChanged(ListSelectionEvent event) {
        if (!event.getValueIsAdjusting()) {
            RecipeTransformationQueryService.RecipeVersionSummary selected = selectedVersionRow();
            if (selected != null) {
                SwingUtilities.invokeLater(() -> loadVersionDetailAsync(selected));
            }
        }
    }

    private RecipeTransformationQueryService.RecipeVersionSummary selectedVersionRow() {
        int row = versionTable.getSelectedRow();
        return row < 0 ? null : versionModel.get(versionTable.convertRowIndexToModel(row));
    }

    private void setBusy(boolean value) {
        busy = value;
        Cursor cursor = value ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
                : Cursor.getDefaultCursor();
        setCursor(cursor);
        addComponent.setEnabled(!value);
        removeComponent.setEnabled(!value);
        clearDraft.setEnabled(!value);
        saveDraft.setEnabled(!value);
        refresh.setEnabled(!value);
        activateVersion.setEnabled(!value);
        retireVersion.setEnabled(!value);
        resultProduct.setEnabled(!value);
        componentProduct.setEnabled(!value);
        yieldUom.setEnabled(!value);
        componentUom.setEnabled(!value);
    }

    private static RecipeUiCatalogService.ProductChoice selectedProduct(
            JComboBox<RecipeUiCatalogService.ProductChoice> combo) {
        Object selected = combo.getSelectedItem();
        return selected instanceof RecipeUiCatalogService.ProductChoice
                ? (RecipeUiCatalogService.ProductChoice) selected : null;
    }

    private static RecipeUiCatalogService.UomChoice selectedUom(
            JComboBox<RecipeUiCatalogService.UomChoice> combo) {
        Object selected = combo.getSelectedItem();
        return selected instanceof RecipeUiCatalogService.UomChoice
                ? (RecipeUiCatalogService.UomChoice) selected : null;
    }

    private static void selectUom(JComboBox<RecipeUiCatalogService.UomChoice> combo, String uomId) {
        if (isBlank(uomId)) {
            return;
        }
        for (int i = 0; i < combo.getItemCount(); i++) {
            RecipeUiCatalogService.UomChoice item = combo.getItemAt(i);
            if (item != null && uomId.equals(item.id)) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    private static BigDecimal positiveDecimal(String text, String label) throws BasicException {
        try {
            BigDecimal value = new BigDecimal(text == null ? "" : text.trim());
            if (value.signum() <= 0) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new BasicException(label + ": " + AppLocal.getIntString("recipe.error.positive"));
        }
    }

    private static BigDecimal optionalRatio(String text) throws BasicException {
        if (isBlank(text)) {
            return null;
        }
        try {
            BigDecimal value = new BigDecimal(text.trim());
            if (value.signum() < 0 || value.compareTo(BigDecimal.ONE) >= 0) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new BasicException(AppLocal.getIntString("recipe.error.waste.ratio"));
        }
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, getTitle(), JOptionPane.WARNING_MESSAGE);
    }

    private void showError(String message, Throwable error) {
        String detail = error == null || isBlank(error.getMessage()) ? "" : "\n" + error.getMessage();
        JOptionPane.showMessageDialog(this, message + detail, getTitle(), JOptionPane.ERROR_MESSAGE);
    }

    private static Throwable rootCause(ExecutionException error) {
        Throwable cause = error.getCause();
        return cause == null ? error : cause;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static final class CatalogSnapshot {
        final List<RecipeUiCatalogService.ProductChoice> products;
        final List<RecipeUiCatalogService.UomChoice> uoms;

        CatalogSnapshot(List<RecipeUiCatalogService.ProductChoice> products,
                List<RecipeUiCatalogService.UomChoice> uoms) {
            this.products = products;
            this.uoms = uoms;
        }
    }

    private static final class DraftComponentRow {
        final RecipeUiCatalogService.ProductChoice product;
        final BigDecimal quantity;
        final RecipeUiCatalogService.UomChoice uom;
        final BigDecimal wasteAllowance;

        DraftComponentRow(RecipeUiCatalogService.ProductChoice product, BigDecimal quantity,
                RecipeUiCatalogService.UomChoice uom, BigDecimal wasteAllowance) {
            this.product = product;
            this.quantity = quantity;
            this.uom = uom;
            this.wasteAllowance = wasteAllowance;
        }
    }

    private static final class ComponentTableModel extends AbstractTableModel {
        private final List<DraftComponentRow> rows = new ArrayList<>();
        private final String[] columns = {
            AppLocal.getIntString("recipe.component.product"),
            AppLocal.getIntString("recipe.component.quantity"),
            AppLocal.getIntString("recipe.component.uom"),
            AppLocal.getIntString("recipe.component.waste")
        };

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            DraftComponentRow row = rows.get(rowIndex);
            switch (columnIndex) {
                case 0: return row.product;
                case 1: return row.quantity;
                case 2: return row.uom;
                case 3: return row.wasteAllowance;
                default: return null;
            }
        }

        void add(DraftComponentRow row) {
            rows.add(row);
            fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
        }

        void remove(int index) {
            rows.remove(index);
            fireTableRowsDeleted(index, index);
        }

        void clear() {
            int size = rows.size();
            rows.clear();
            if (size > 0) {
                fireTableRowsDeleted(0, size - 1);
            }
        }

        boolean containsProduct(String productId) {
            for (DraftComponentRow row : rows) {
                if (row.product.id.equals(productId)) {
                    return true;
                }
            }
            return false;
        }

        List<RecipeDefinitionService.ComponentDraft> toDrafts() {
            List<RecipeDefinitionService.ComponentDraft> result = new ArrayList<>();
            for (DraftComponentRow row : rows) {
                result.add(new RecipeDefinitionService.ComponentDraft(row.product.id,
                        row.quantity, row.uom.id, row.wasteAllowance));
            }
            return result;
        }
    }

    private static final class VersionTableModel extends AbstractTableModel {
        private final String[] columns = {
            AppLocal.getIntString("recipe.version"),
            AppLocal.getIntString("recipe.status"),
            AppLocal.getIntString("recipe.yield.quantity"),
            AppLocal.getIntString("recipe.yield.uom"),
            AppLocal.getIntString("recipe.created.by")
        };
        private List<RecipeTransformationQueryService.RecipeVersionSummary> rows = Collections.emptyList();

        void setRows(List<RecipeTransformationQueryService.RecipeVersionSummary> values) {
            rows = values == null ? Collections.emptyList() : new ArrayList<>(values);
            fireTableDataChanged();
        }

        RecipeTransformationQueryService.RecipeVersionSummary get(int index) {
            return rows.get(index);
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int column) { return columns[column]; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            RecipeTransformationQueryService.RecipeVersionSummary row = rows.get(rowIndex);
            switch (columnIndex) {
                case 0: return row.versionNo;
                case 1: return row.statusLabel();
                case 2: return row.yieldQuantity;
                case 3: return row.yieldUomLabel();
                case 4: return row.createdBy;
                default: return null;
            }
        }
    }

    private static final class ComponentPreviewTableModel extends AbstractTableModel {
        private final String[] columns = {
            AppLocal.getIntString("recipe.line"),
            AppLocal.getIntString("recipe.component.product"),
            AppLocal.getIntString("recipe.component.quantity"),
            AppLocal.getIntString("recipe.component.uom"),
            AppLocal.getIntString("recipe.component.waste")
        };
        private List<RecipeTransformationQueryService.RecipeComponent> rows = Collections.emptyList();

        void setRows(List<RecipeTransformationQueryService.RecipeComponent> values) {
            rows = values == null ? Collections.emptyList() : new ArrayList<>(values);
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int column) { return columns[column]; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            RecipeTransformationQueryService.RecipeComponent row = rows.get(rowIndex);
            switch (columnIndex) {
                case 0: return row.lineNo;
                case 1: return row.productLabel();
                case 2: return row.quantity;
                case 3: return row.uomLabel();
                case 4: return row.wasteAllowance;
                default: return null;
            }
        }
    }
}