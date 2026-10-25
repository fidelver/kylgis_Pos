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
import java.math.BigDecimal;
import java.sql.Timestamp;
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
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.AbstractTableModel;

/** Read-only transformation traceability plus guarded reversal action. */
public final class TransformationHistoryPanel extends JPanel implements JPanelView {

    private final AppView app;
    private final RecipeUiCatalogService catalogService;
    private final RecipeTransformationQueryService queryService;
    private final InventoryTransformationService transformationService;
    private final String currentUser;

    private final JComboBox<LocationFilter> location = new JComboBox<>();
    private final JTextField limit = new JTextField("100", 5);
    private final JButton reload = new JButton();
    private final JButton reverse = new JButton();
    private final JLabel detailHeader = new JLabel(" ");
    private final TransformationTableModel transformationModel = new TransformationTableModel();
    private final JTable transformationTable = new JTable(transformationModel);
    private final LineTableModel lineModel = new LineTableModel();
    private final JTable lineTable = new JTable(lineModel);

    private volatile boolean busy;
    private String pendingReversalFingerprint;
    private String pendingReversalKey;

    public TransformationHistoryPanel(AppView app) {
        if (app == null) {
            throw new IllegalArgumentException("app is required");
        }
        this.app = app;
        this.catalogService = new RecipeUiCatalogService(app.getSession());
        this.queryService = new RecipeTransformationQueryService(app.getSession());
        this.transformationService = new InventoryTransformationService(app.getSession());
        this.currentUser = app.getAppUserView().getUser().getName();
        buildUi();
        bindActions();
        setBusy(false);
    }

    @Override
    public String getTitle() {
        return AppLocal.getIntString("Menu.TransformationHistory");
    }

    @Override
    public void activate() throws BasicException {
        loadLocationsAsync();
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

        JLabel title = new JLabel(AppLocal.getIntString("Menu.TransformationHistory"));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        add(title, BorderLayout.NORTH);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filters.setBorder(BorderFactory.createTitledBorder(
                AppLocal.getIntString("transform.history.filters")));
        filters.add(new JLabel(AppLocal.getIntString("recipe.production.location")));
        location.setPrototypeDisplayValue(new LocationFilter(null,
                AppLocal.getIntString("transform.history.all.locations")));
        filters.add(location);
        filters.add(new JLabel(AppLocal.getIntString("transform.history.limit")));
        filters.add(limit);
        reload.setText(AppLocal.getIntString("recipe.reload"));
        filters.add(reload);

        transformationTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        transformationTable.setFillsViewportHeight(true);
        lineTable.setFillsViewportHeight(true);

        JPanel master = new JPanel(new BorderLayout(4, 4));
        master.add(filters, BorderLayout.NORTH);
        master.add(new JScrollPane(transformationTable), BorderLayout.CENTER);

        JPanel detail = new JPanel(new BorderLayout(4, 4));
        detail.setBorder(BorderFactory.createTitledBorder(
                AppLocal.getIntString("transform.history.detail")));
        detailHeader.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        detail.add(detailHeader, BorderLayout.NORTH);
        detail.add(new JScrollPane(lineTable), BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        reverse.setText(AppLocal.getIntString("transform.history.reverse"));
        actions.add(reverse);
        detail.add(actions, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, master, detail);
        split.setResizeWeight(0.58d);
        split.setContinuousLayout(true);
        add(split, BorderLayout.CENTER);
    }

    private void bindActions() {
        reload.addActionListener(e -> loadHistoryAsync());
        location.addActionListener(e -> {
            if (!busy) {
                loadHistoryAsync();
            }
        });
        transformationTable.getSelectionModel().addListSelectionListener(this::selectionChanged);
        reverse.addActionListener(e -> reverseSelectedAsync());
    }

    private void loadLocationsAsync() {
        if (busy) {
            return;
        }
        setBusy(true);
        new SwingWorker<List<RecipeUiCatalogService.LocationChoice>, Void>() {
            @Override
            protected List<RecipeUiCatalogService.LocationChoice> doInBackground() throws Exception {
                return catalogService.listLocations();
            }

            @Override
            protected void done() {
                try {
                    List<RecipeUiCatalogService.LocationChoice> values = get();
                    List<LocationFilter> filters = new ArrayList<>();
                    filters.add(new LocationFilter(null,
                            AppLocal.getIntString("transform.history.all.locations")));
                    for (RecipeUiCatalogService.LocationChoice value : values) {
                        filters.add(new LocationFilter(value.id, value.toString()));
                    }
                    location.setModel(new DefaultComboBoxModel<>(
                            filters.toArray(new LocationFilter[0])));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    showError(AppLocal.getIntString("transform.history.error.locations"), ex);
                } catch (ExecutionException ex) {
                    showError(AppLocal.getIntString("transform.history.error.locations"), rootCause(ex));
                } finally {
                    setBusy(false);
                    loadHistoryAsync();
                }
            }
        }.execute();
    }

    private void loadHistoryAsync() {
        if (busy) {
            return;
        }
        final int maxRows;
        try {
            maxRows = Integer.parseInt(limit.getText().trim());
            if (maxRows < 1 || maxRows > 500) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            showWarning(AppLocal.getIntString("transform.history.error.limit"));
            return;
        }
        LocationFilter filter = (LocationFilter) location.getSelectedItem();
        String locationId = filter == null ? null : filter.id;
        setBusy(true);
        new SwingWorker<List<RecipeTransformationQueryService.TransformationSummary>, Void>() {
            @Override
            protected List<RecipeTransformationQueryService.TransformationSummary> doInBackground()
                    throws Exception {
                return queryService.listRecentTransformations(locationId, maxRows);
            }

            @Override
            protected void done() {
                try {
                    transformationModel.setRows(get());
                    lineModel.setRows(Collections.emptyList());
                    detailHeader.setText(" ");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    showError(AppLocal.getIntString("transform.history.error.load"), ex);
                } catch (ExecutionException ex) {
                    showError(AppLocal.getIntString("transform.history.error.load"), rootCause(ex));
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void selectionChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting() || busy) {
            return;
        }
        RecipeTransformationQueryService.TransformationSummary selected = selectedSummary();
        if (selected != null) {
            loadDetailAsync(selected.id);
        }
    }

    private void loadDetailAsync(String transformationId) {
        if (busy) {
            return;
        }
        setBusy(true);
        new SwingWorker<RecipeTransformationQueryService.TransformationDetail, Void>() {
            @Override
            protected RecipeTransformationQueryService.TransformationDetail doInBackground()
                    throws Exception {
                return queryService.getTransformation(transformationId);
            }

            @Override
            protected void done() {
                try {
                    RecipeTransformationQueryService.TransformationDetail detail = get();
                    lineModel.setRows(detail.lines);
                    detailHeader.setText(detail.summary.id + " — " + detail.summary.typeLabel()
                            + " — " + detail.summary.statusLabel());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    showError(AppLocal.getIntString("transform.history.error.detail"), ex);
                } catch (ExecutionException ex) {
                    showError(AppLocal.getIntString("transform.history.error.detail"), rootCause(ex));
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void reverseSelectedAsync() {
        if (busy) {
            return;
        }
        RecipeTransformationQueryService.TransformationSummary selected = selectedSummary();
        if (selected == null) {
            showWarning(AppLocal.getIntString("transform.history.error.select"));
            return;
        }
        boolean reversibleType = "PRODUCTION".equalsIgnoreCase(selected.type)
                || "CONVERSION".equalsIgnoreCase(selected.type);
        if (!reversibleType || !"POSTED".equalsIgnoreCase(selected.status)) {
            showWarning(AppLocal.getIntString("transform.history.error.reversible"));
            return;
        }
        String reason = JOptionPane.showInputDialog(this,
                AppLocal.getIntString("transform.history.reverse.reason"),
                AppLocal.getIntString("transform.history.reverse"),
                JOptionPane.QUESTION_MESSAGE);
        if (reason == null) {
            return;
        }
        reason = reason.trim();
        if (reason.isEmpty()) {
            showWarning(AppLocal.getIntString("transform.history.error.reason"));
            return;
        }
        String fingerprint = selected.id + "\n" + reason;
        if (!fingerprint.equals(pendingReversalFingerprint)) {
            pendingReversalFingerprint = fingerprint;
            pendingReversalKey = UUID.randomUUID().toString();
        }
        final String note = reason;
        InventoryTransformationService.ReversalRequest request =
                new InventoryTransformationService.ReversalRequest(pendingReversalKey,
                        selected.id, currentUser, note);
        setBusy(true);
        new SwingWorker<InventoryTransformationService.ReversalResult, Void>() {
            @Override
            protected InventoryTransformationService.ReversalResult doInBackground()
                    throws Exception {
                return transformationService.reverseTransformation(request);
            }

            @Override
            protected void done() {
                try {
                    InventoryTransformationService.ReversalResult result = get();
                    pendingReversalFingerprint = null;
                    pendingReversalKey = null;
                    JOptionPane.showMessageDialog(TransformationHistoryPanel.this,
                            AppLocal.getIntString("transform.history.reverse.success") + " "
                            + result.reversalTransformationId,
                            getTitle(), JOptionPane.INFORMATION_MESSAGE);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    showError(AppLocal.getIntString("transform.history.error.reverse"), ex);
                } catch (ExecutionException ex) {
                    // Keep the same key for an unchanged retry after an uncertain failure.
                    showError(AppLocal.getIntString("transform.history.error.reverse"), rootCause(ex));
                } finally {
                    setBusy(false);
                    loadHistoryAsync();
                }
            }
        }.execute();
    }

    private RecipeTransformationQueryService.TransformationSummary selectedSummary() {
        int selected = transformationTable.getSelectedRow();
        return selected < 0 ? null
                : transformationModel.get(transformationTable.convertRowIndexToModel(selected));
    }

    private void setBusy(boolean value) {
        busy = value;
        setCursor(value ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
        location.setEnabled(!value);
        limit.setEnabled(!value);
        reload.setEnabled(!value);
        reverse.setEnabled(!value);
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

    private static final class LocationFilter {
        final String id;
        final String label;

        LocationFilter(String id, String label) {
            this.id = id;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static final class TransformationTableModel extends AbstractTableModel {
        private final String[] columns = {
            AppLocal.getIntString("transform.history.date"),
            AppLocal.getIntString("transform.history.type"),
            AppLocal.getIntString("recipe.status"),
            AppLocal.getIntString("recipe.production.location"),
            AppLocal.getIntString("transform.history.expected"),
            AppLocal.getIntString("transform.history.actual"),
            AppLocal.getIntString("recipe.created.by")
        };
        private List<RecipeTransformationQueryService.TransformationSummary> rows = Collections.emptyList();

        void setRows(List<RecipeTransformationQueryService.TransformationSummary> values) {
            rows = values == null ? Collections.emptyList() : new ArrayList<>(values);
            fireTableDataChanged();
        }

        RecipeTransformationQueryService.TransformationSummary get(int index) {
            return rows.get(index);
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int column) { return columns[column]; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            RecipeTransformationQueryService.TransformationSummary row = rows.get(rowIndex);
            switch (columnIndex) {
                case 0: return row.createdAt;
                case 1: return row.typeLabel();
                case 2: return row.statusLabel();
                case 3: return row.locationLabel();
                case 4: return row.expectedOutput;
                case 5: return row.actualOutput;
                case 6: return row.createdBy;
                default: return null;
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0) return Timestamp.class;
            if (columnIndex == 4 || columnIndex == 5) return BigDecimal.class;
            return String.class;
        }
    }

    private static final class LineTableModel extends AbstractTableModel {
        private final String[] columns = {
            AppLocal.getIntString("recipe.line"),
            AppLocal.getIntString("transform.history.direction"),
            AppLocal.getIntString("recipe.component.product"),
            AppLocal.getIntString("recipe.component.quantity"),
            AppLocal.getIntString("recipe.component.uom"),
            AppLocal.getIntString("transform.history.base.quantity"),
            AppLocal.getIntString("recipe.production.cost"),
            AppLocal.getIntString("transform.history.total.cost"),
            AppLocal.getIntString("transform.history.stockdiary")
        };
        private List<RecipeTransformationQueryService.TransformationLine> rows = Collections.emptyList();

        void setRows(List<RecipeTransformationQueryService.TransformationLine> values) {
            rows = values == null ? Collections.emptyList() : new ArrayList<>(values);
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int column) { return columns[column]; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            RecipeTransformationQueryService.TransformationLine row = rows.get(rowIndex);
            switch (columnIndex) {
                case 0: return row.lineNo;
                case 1: return row.directionLabel();
                case 2: return row.productLabel();
                case 3: return row.quantity;
                case 4: return row.uomLabel();
                case 5: return row.baseQuantity;
                case 6: return row.unitCost;
                case 7: return row.totalCost;
                case 8: return row.stockDiaryId;
                default: return null;
            }
        }
    }
}