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
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Posts stock production only from an ACTIVE immutable recipe version. */
public final class RecipeProductionPanel extends JPanel implements JPanelView {

    private final AppView app;
    private final RecipeUiCatalogService catalogService;
    private final RecipeTransformationQueryService queryService;
    private final InventoryTransformationService transformationService;
    private final String currentUser;

    private final JComboBox<RecipeUiCatalogService.ProductChoice> product = new JComboBox<>();
    private final JComboBox<RecipeUiCatalogService.LocationChoice> location = new JComboBox<>();
    private final JTextField batches = new JTextField("1", 10);
    private final JTextField actualOutput = new JTextField("", 10);
    private final JTextArea notes = new JTextArea(3, 28);
    private final JLabel activeRecipe = new JLabel(" ");
    private final JLabel expectedOutput = new JLabel(" ");
    private final JLabel lastResult = new JLabel(" ");
    private final JButton reload = new JButton();
    private final JButton useExpected = new JButton();
    private final JButton post = new JButton();

    private volatile boolean busy;
    private RecipeTransformationQueryService.RecipeVersionSummary activeVersion;
    private String pendingFingerprint;
    private String pendingIdempotencyKey;

    public RecipeProductionPanel(AppView app) {
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
        return AppLocal.getIntString("Menu.RecipeProduction");
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
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JLabel title = new JLabel(AppLocal.getIntString("Menu.RecipeProduction"));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder(
                AppLocal.getIntString("recipe.production.title")));
        GridBagConstraints c = constraints();
        addRow(form, c, 0, AppLocal.getIntString("recipe.production.product"), product);
        addRow(form, c, 1, AppLocal.getIntString("recipe.production.location"), location);
        addRow(form, c, 2, AppLocal.getIntString("recipe.production.active"), activeRecipe);
        addRow(form, c, 3, AppLocal.getIntString("recipe.production.batches"), batches);
        addRow(form, c, 4, AppLocal.getIntString("recipe.production.expected"), expectedOutput);

        JPanel actualPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        actualPanel.add(actualOutput);
        useExpected.setText(AppLocal.getIntString("recipe.production.use.expected"));
        actualPanel.add(useExpected);
        addRow(form, c, 5, AppLocal.getIntString("recipe.production.actual"), actualPanel);

        c.gridx = 0;
        c.gridy = 6;
        c.gridwidth = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        form.add(new JLabel(AppLocal.getIntString("recipe.notes")), c);
        c.gridx = 1;
        c.gridwidth = 2;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        form.add(new javax.swing.JScrollPane(notes), c);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        reload.setText(AppLocal.getIntString("recipe.reload"));
        post.setText(AppLocal.getIntString("recipe.production.post"));
        actions.add(reload);
        actions.add(post);
        c.gridx = 1;
        c.gridy = 7;
        c.gridwidth = 2;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(actions, c);

        c.gridx = 0;
        c.gridy = 8;
        c.gridwidth = 3;
        c.weightx = 1;
        lastResult.setBorder(BorderFactory.createEmptyBorder(8, 3, 3, 3));
        form.add(lastResult, c);

        JPanel holder = new JPanel(new BorderLayout());
        holder.add(form, BorderLayout.NORTH);
        add(holder, BorderLayout.CENTER);
    }

    private void bindActions() {
        product.addActionListener(e -> loadActiveRecipeForSelection());
        batches.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updateExpectedLabel(); }
            @Override public void removeUpdate(DocumentEvent e) { updateExpectedLabel(); }
            @Override public void changedUpdate(DocumentEvent e) { updateExpectedLabel(); }
        });
        reload.addActionListener(e -> loadCatalogsAsync());
        useExpected.addActionListener(e -> {
            BigDecimal expected = calculateExpectedOutput();
            if (expected != null) {
                actualOutput.setText(expected.stripTrailingZeros().toPlainString());
            }
        });
        post.addActionListener(e -> postProductionAsync());
    }

    private void loadCatalogsAsync() {
        if (busy) {
            return;
        }
        setBusy(true);
        new SwingWorker<CatalogSnapshot, Void>() {
            @Override
            protected CatalogSnapshot doInBackground() throws Exception {
                return new CatalogSnapshot(catalogService.listProducts(), catalogService.listLocations());
            }

            @Override
            protected void done() {
                try {
                    CatalogSnapshot snapshot = get();
                    product.setModel(new DefaultComboBoxModel<>(snapshot.products.toArray(
                            new RecipeUiCatalogService.ProductChoice[0])));
                    location.setModel(new DefaultComboBoxModel<>(snapshot.locations.toArray(
                            new RecipeUiCatalogService.LocationChoice[0])));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    showError(AppLocal.getIntString("recipe.production.error.catalog"), ex);
                } catch (ExecutionException ex) {
                    showError(AppLocal.getIntString("recipe.production.error.catalog"), rootCause(ex));
                } finally {
                    setBusy(false);
                    loadActiveRecipeForSelection();
                }
            }
        }.execute();
    }

    private void loadActiveRecipeForSelection() {
        if (busy) {
            return;
        }
        RecipeUiCatalogService.ProductChoice selected = selectedProduct();
        activeVersion = null;
        activeRecipe.setText(AppLocal.getIntString("recipe.production.none"));
        expectedOutput.setText(" ");
        if (selected == null) {
            return;
        }
        setBusy(true);
        new SwingWorker<RecipeTransformationQueryService.RecipeVersionSummary, Void>() {
            @Override
            protected RecipeTransformationQueryService.RecipeVersionSummary doInBackground()
                    throws Exception {
                List<RecipeTransformationQueryService.RecipeVersionSummary> versions =
                        queryService.listRecipeVersions(selected.id);
                RecipeTransformationQueryService.RecipeVersionSummary found = null;
                for (RecipeTransformationQueryService.RecipeVersionSummary candidate : versions) {
                    if ("ACTIVE".equalsIgnoreCase(candidate.status)) {
                        if (found != null) {
                            throw new BasicException("Product has more than one ACTIVE recipe.");
                        }
                        found = candidate;
                    }
                }
                return found;
            }

            @Override
            protected void done() {
                try {
                    activeVersion = get();
                    if (activeVersion == null) {
                        activeRecipe.setText(AppLocal.getIntString("recipe.production.none"));
                    } else {
                        activeRecipe.setText("v" + activeVersion.versionNo + " — "
                                + activeVersion.yieldQuantity.stripTrailingZeros().toPlainString()
                                + " " + activeVersion.yieldUomId);
                    }
                    updateExpectedLabel();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    showError(AppLocal.getIntString("recipe.production.error.recipe"), ex);
                } catch (ExecutionException ex) {
                    showError(AppLocal.getIntString("recipe.production.error.recipe"), rootCause(ex));
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void updateExpectedLabel() {
        BigDecimal value = calculateExpectedOutput();
        if (value == null || activeVersion == null) {
            expectedOutput.setText(" ");
            return;
        }
        expectedOutput.setText(value.stripTrailingZeros().toPlainString()
                + " " + activeVersion.yieldUomId);
    }

    private BigDecimal calculateExpectedOutput() {
        if (activeVersion == null) {
            return null;
        }
        try {
            BigDecimal batchCount = new BigDecimal(batches.getText().trim());
            if (batchCount.signum() <= 0) {
                return null;
            }
            return activeVersion.yieldQuantity.multiply(batchCount);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private void postProductionAsync() {
        if (busy) {
            return;
        }
        if (activeVersion == null) {
            showWarning(AppLocal.getIntString("recipe.production.error.noactive"));
            return;
        }
        RecipeUiCatalogService.LocationChoice selectedLocation = selectedLocation();
        if (selectedLocation == null) {
            showWarning(AppLocal.getIntString("recipe.production.error.location"));
            return;
        }
        final BigDecimal batchCount;
        final BigDecimal actual;
        try {
            batchCount = positiveDecimal(batches.getText(),
                    AppLocal.getIntString("recipe.production.batches"));
            actual = positiveDecimal(actualOutput.getText(),
                    AppLocal.getIntString("recipe.production.actual"));
        } catch (BasicException ex) {
            showWarning(ex.getMessage());
            return;
        }
        String noteText = trimToNull(notes.getText());
        String fingerprint = activeVersion.id + "\n" + selectedLocation.id + "\n"
                + canonical(batchCount) + "\n" + canonical(actual) + "\n"
                + (noteText == null ? "" : noteText);
        if (!fingerprint.equals(pendingFingerprint)) {
            pendingFingerprint = fingerprint;
            pendingIdempotencyKey = UUID.randomUUID().toString();
        }
        final String idempotencyKey = pendingIdempotencyKey;
        InventoryTransformationService.ProductionRequest request =
                new InventoryTransformationService.ProductionRequest(idempotencyKey,
                        activeVersion.id, selectedLocation.id, batchCount, actual,
                        currentUser, noteText);
        setBusy(true);
        new SwingWorker<InventoryTransformationService.ProductionResult, Void>() {
            @Override
            protected InventoryTransformationService.ProductionResult doInBackground()
                    throws Exception {
                return transformationService.postProduction(request);
            }

            @Override
            protected void done() {
                try {
                    InventoryTransformationService.ProductionResult result = get();
                    lastResult.setText(AppLocal.getIntString("recipe.production.success") + " "
                            + result.transformationId + " — "
                            + AppLocal.getIntString("recipe.production.cost") + ": "
                            + result.outputUnitCost.stripTrailingZeros().toPlainString());
                    pendingFingerprint = null;
                    pendingIdempotencyKey = null;
                    notes.setText("");
                    JOptionPane.showMessageDialog(RecipeProductionPanel.this,
                            AppLocal.getIntString("recipe.production.success"), getTitle(),
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    showError(AppLocal.getIntString("recipe.production.error.post"), ex);
                } catch (ExecutionException ex) {
                    // Keep the idempotency key so an unchanged retry cannot duplicate stock.
                    showError(AppLocal.getIntString("recipe.production.error.post"), rootCause(ex));
                } finally {
                    setBusy(false);
                    updateExpectedLabel();
                }
            }
        }.execute();
    }

    private void setBusy(boolean value) {
        busy = value;
        setCursor(value ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
        product.setEnabled(!value);
        location.setEnabled(!value);
        batches.setEnabled(!value);
        actualOutput.setEnabled(!value);
        notes.setEnabled(!value);
        reload.setEnabled(!value);
        useExpected.setEnabled(!value && activeVersion != null);
        post.setEnabled(!value && activeVersion != null);
    }

    private RecipeUiCatalogService.ProductChoice selectedProduct() {
        Object value = product.getSelectedItem();
        return value instanceof RecipeUiCatalogService.ProductChoice
                ? (RecipeUiCatalogService.ProductChoice) value : null;
    }

    private RecipeUiCatalogService.LocationChoice selectedLocation() {
        Object value = location.getSelectedItem();
        return value instanceof RecipeUiCatalogService.LocationChoice
                ? (RecipeUiCatalogService.LocationChoice) value : null;
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

    private static String canonical(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
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
        String detail = error == null || isBlank(error.getMessage()) ? "" : "\n" + error.getMessage();
        JOptionPane.showMessageDialog(this, message + detail, getTitle(), JOptionPane.ERROR_MESSAGE);
    }

    private static Throwable rootCause(ExecutionException error) {
        return error.getCause() == null ? error : error.getCause();
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
        final List<RecipeUiCatalogService.LocationChoice> locations;

        CatalogSnapshot(List<RecipeUiCatalogService.ProductChoice> products,
                List<RecipeUiCatalogService.LocationChoice> locations) {
            this.products = products == null ? Collections.emptyList() : products;
            this.locations = locations == null ? Collections.emptyList() : locations;
        }
    }
}
