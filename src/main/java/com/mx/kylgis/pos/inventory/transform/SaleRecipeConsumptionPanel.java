// KylGis POS Punto de Venta Táctil - GPLv3+
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;

/**
 * Monitoring panel for recipes consumed when a sale is finalized.
 *
 * Recipe activation remains managed by RecipeManagementPanel. This panel
 * exposes the active sale recipes and pending manager alerts without mixing
 * commercial bundle configuration with production recipes.
 */
public final class SaleRecipeConsumptionPanel extends JPanel implements JPanelView {

    private final AppView app;
    private final RecipeTableModel recipeModel = new RecipeTableModel();
    private final AlertTableModel alertModel = new AlertTableModel();
    private final JTable recipes = new JTable(recipeModel);
    private final JTable alerts = new JTable(alertModel);
    private final JButton refresh = new JButton();
    private final JLabel summary = new JLabel(" ");
    private volatile boolean busy;

    public SaleRecipeConsumptionPanel(AppView app) {
        if (app == null) {
            throw new IllegalArgumentException("app is required");
        }
        this.app = app;
        buildUi();
        refresh.addActionListener(e -> refreshAsync());
        setBusy(false);
    }

    @Override
    public String getTitle() {
        return AppLocal.getIntString("Menu.SaleRecipeConsumption");
    }

    @Override
    public void activate() throws BasicException {
        refreshAsync();
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

        JPanel recipePanel = new JPanel(new BorderLayout(4, 4));
        recipePanel.setBorder(BorderFactory.createTitledBorder(
                AppLocal.getIntString("recipe.sale.active.title")));
        recipes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        recipes.setFillsViewportHeight(true);
        recipePanel.add(new JScrollPane(recipes), BorderLayout.CENTER);

        JPanel alertPanel = new JPanel(new BorderLayout(4, 4));
        alertPanel.setBorder(BorderFactory.createTitledBorder(
                AppLocal.getIntString("recipe.sale.alerts.title")));
        alerts.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        alerts.setFillsViewportHeight(true);
        alertPanel.add(new JScrollPane(alerts), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                recipePanel, alertPanel);
        split.setResizeWeight(0.58d);
        split.setContinuousLayout(true);
        add(split, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout());
        south.add(summary, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        refresh.setText(AppLocal.getIntString("recipe.sale.refresh"));
        actions.add(refresh);
        south.add(actions, BorderLayout.EAST);
        add(south, BorderLayout.SOUTH);
    }

    private void refreshAsync() {
        if (busy) {
            return;
        }
        setBusy(true);
        new SwingWorker<Snapshot, Void>() {
            @Override
            protected Snapshot doInBackground() throws Exception {
                return loadSnapshot();
            }

            @Override
            protected void done() {
                try {
                    Snapshot snapshot = get();
                    recipeModel.setRows(snapshot.recipes);
                    alertModel.setRows(snapshot.alerts);
                    summary.setText(AppLocal.getIntString(
                            "recipe.sale.summary",
                            snapshot.recipes.size(), snapshot.alerts.size()));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    showError(ex);
                } catch (ExecutionException ex) {
                    showError(rootCause(ex));
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private Snapshot loadSnapshot() throws SQLException {
        List<RecipeRow> active = new ArrayList<>();
        List<AlertRow> pending = new ArrayList<>();
        Connection connection = app.getSession().getConnection();
        SaleRecipeConsumptionService.resolveSatisfiedAlerts(connection);

        String recipesSql = "SELECT rv.id, p.reference, p.name, rv.version_no, "
                + "rv.yield_quantity, yu.name, COUNT(rc.line_no) "
                + "FROM recipe_versions rv "
                + "JOIN products p ON p.id = rv.product_id "
                + "LEFT JOIN uom yu ON yu.id = rv.yield_uom_id "
                + "LEFT JOIN recipe_components rc ON rc.recipe_version_id = rv.id "
                + "WHERE UPPER(rv.status) = 'ACTIVE' "
                + "AND UPPER(rv.recipe_mode) = 'SALE_CONSUMPTION' "
                + "GROUP BY rv.id, p.reference, p.name, rv.version_no, "
                + "rv.yield_quantity, yu.name ORDER BY p.name, rv.version_no";
        try (PreparedStatement statement = connection.prepareStatement(recipesSql);
                ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                active.add(new RecipeRow(rows.getString(1), label(rows.getString(2), rows.getString(3)),
                        rows.getInt(4), rows.getBigDecimal(5), rows.getString(6),
                        rows.getInt(7)));
            }
        }

        String alertsSql = "SELECT a.kind, p.reference, p.name, a.required_quantity, "
                + "a.available_quantity, a.created_at "
                + "FROM sale_recipe_consumption_alerts a "
                + "LEFT JOIN products p ON p.id = a.product_id "
                + "WHERE a.status = 'PENDING' ORDER BY a.created_at DESC";
        try (PreparedStatement statement = connection.prepareStatement(alertsSql);
                ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                pending.add(new AlertRow(rows.getString(1),
                        label(rows.getString(2), rows.getString(3)),
                        rows.getBigDecimal(4), rows.getBigDecimal(5),
                        rows.getTimestamp(6)));
            }
        }
        return new Snapshot(active, pending);
    }

    private void setBusy(boolean value) {
        busy = value;
        refresh.setEnabled(!value);
        setCursor(value ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
                : Cursor.getDefaultCursor());
    }

    private void showError(Throwable error) {
        String message = error == null || error.getMessage() == null
                ? AppLocal.getIntString("recipe.sale.error")
                : error.getMessage();
        JOptionPane.showMessageDialog(this, message, getTitle(),
                JOptionPane.ERROR_MESSAGE);
    }

    private static Throwable rootCause(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private static String label(String reference, String name) {
        String ref = reference == null ? "" : reference.trim();
        String product = name == null ? "" : name.trim();
        if (ref.isEmpty()) {
            return product;
        }
        return product.isEmpty() ? ref : ref + " - " + product;
    }

    private static final class Snapshot {
        final List<RecipeRow> recipes;
        final List<AlertRow> alerts;
        Snapshot(List<RecipeRow> recipes, List<AlertRow> alerts) {
            this.recipes = recipes;
            this.alerts = alerts;
        }
    }

    private static final class RecipeRow {
        final String id;
        final String product;
        final int version;
        final BigDecimal yield;
        final String uom;
        final int components;
        RecipeRow(String id, String product, int version, BigDecimal yield,
                String uom, int components) {
            this.id = id;
            this.product = product;
            this.version = version;
            this.yield = yield;
            this.uom = uom;
            this.components = components;
        }
    }

    private static final class AlertRow {
        final String kind;
        final String product;
        final BigDecimal required;
        final BigDecimal available;
        final Timestamp created;
        AlertRow(String kind, String product, BigDecimal required,
                BigDecimal available, Timestamp created) {
            this.kind = kind;
            this.product = product;
            this.required = required;
            this.available = available;
            this.created = created;
        }
    }

    private static final class RecipeTableModel extends AbstractTableModel {
        private final String[] columns = {
            AppLocal.getIntString("recipe.sale.column.product"),
            AppLocal.getIntString("recipe.sale.column.version"),
            AppLocal.getIntString("recipe.sale.column.yield"),
            AppLocal.getIntString("recipe.sale.column.components"),
            AppLocal.getIntString("recipe.sale.column.recipe")
        };
        private List<RecipeRow> rows = Collections.emptyList();

        void setRows(List<RecipeRow> rows) {
            this.rows = rows == null ? Collections.emptyList() : new ArrayList<>(rows);
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int column) { return columns[column]; }
        @Override public Object getValueAt(int row, int column) {
            RecipeRow value = rows.get(row);
            switch (column) {
                case 0: return value.product;
                case 1: return value.version;
                case 2: return value.yield + " " + (value.uom == null ? "" : value.uom);
                case 3: return value.components;
                default: return value.id;
            }
        }
    }

    private static final class AlertTableModel extends AbstractTableModel {
        private final String[] columns = {
            AppLocal.getIntString("recipe.sale.column.type"),
            AppLocal.getIntString("recipe.sale.column.product"),
            AppLocal.getIntString("recipe.sale.column.required"),
            AppLocal.getIntString("recipe.sale.column.available"),
            AppLocal.getIntString("recipe.sale.column.date")
        };
        private List<AlertRow> rows = Collections.emptyList();

        void setRows(List<AlertRow> rows) {
            this.rows = rows == null ? Collections.emptyList() : new ArrayList<>(rows);
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int column) { return columns[column]; }
        @Override public Object getValueAt(int row, int column) {
            AlertRow value = rows.get(row);
            switch (column) {
                case 0: return value.kind;
                case 1: return value.product;
                case 2: return value.required;
                case 3: return value.available;
                default: return value.created;
            }
        }
    }
}
