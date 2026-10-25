package com.mx.kylgis.pos.inventory.transform;

import com.mx.kylgis.pos.forms.AppLocal;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Shared searchable selector for product choices used by inventory-transform panels. */
final class ProductChoiceSearchDialog extends JDialog {
    private RecipeUiCatalogService.ProductChoice result;
    private final List<RecipeUiCatalogService.ProductChoice> source;
    private final JTextField search = new JTextField(32);
    private final DefaultListModel<RecipeUiCatalogService.ProductChoice> model = new DefaultListModel<>();
    private final JList<RecipeUiCatalogService.ProductChoice> matches = new JList<>(model);
    private final JButton accept = new JButton();
    private final JButton cancel = new JButton();

    private ProductChoiceSearchDialog(Window owner,
            List<RecipeUiCatalogService.ProductChoice> source) {
        super(owner, AppLocal.getIntString("recipe.catalog.choose"), ModalityType.APPLICATION_MODAL);
        this.source = source == null ? Collections.emptyList() : source;
        buildUi();
        refreshResults();
    }

    static RecipeUiCatalogService.ProductChoice showMessage(Component parent,
            List<RecipeUiCatalogService.ProductChoice> products) {
        Window owner = parent instanceof Window ? (Window) parent
                : parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        ProductChoiceSearchDialog dialog = new ProductChoiceSearchDialog(owner, products);
        dialog.setSize(780, 560);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
        return dialog.result;
    }

    private void buildUi() {
        setLayout(new BorderLayout(8, 8));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel header = new JPanel(new BorderLayout(6, 0));
        header.add(new JLabel(AppLocal.getIntString("recipe.catalog.search")), BorderLayout.WEST);
        header.add(search, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        matches.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        matches.setToolTipText(AppLocal.getIntString("tooltip.search.results"));
        add(new JScrollPane(matches), BorderLayout.CENTER);

        accept.setText(AppLocal.getIntString("recipe.catalog.accept"));
        cancel.setText(AppLocal.getIntString("recipe.catalog.cancel"));
        accept.setEnabled(false);
        accept.setToolTipText(AppLocal.getIntString("recipe.catalog.accept.help"));
        cancel.setToolTipText(AppLocal.getIntString("recipe.catalog.cancel.help"));
        JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        actions.add(cancel);
        actions.add(accept);
        add(actions, BorderLayout.SOUTH);

        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refreshResults(); }
            @Override public void removeUpdate(DocumentEvent e) { refreshResults(); }
            @Override public void changedUpdate(DocumentEvent e) { refreshResults(); }
        });
        matches.addListSelectionListener(e -> accept.setEnabled(!matches.isSelectionEmpty()));
        matches.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2 && SwingUtilities.isLeftMouseButton(e)) {
                    acceptSelected();
                }
            }
        });
        accept.addActionListener(e -> acceptSelected());
        cancel.addActionListener(e -> { result = null; dispose(); });
        getRootPane().setDefaultButton(accept);
        getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ESCAPE"), "cancelProductChoice");
        getRootPane().getActionMap().put("cancelProductChoice",
                new javax.swing.AbstractAction() {
                    @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                        result = null;
                        dispose();
                    }
                });
        matches.getInputMap(javax.swing.JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke("ENTER"), "acceptProductChoice");
        matches.getActionMap().put("acceptProductChoice",
                new javax.swing.AbstractAction() {
                    @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                        acceptSelected();
                    }
                });
    }

    private void refreshResults() {
        model.clear();
        String query = search.getText();
        for (RecipeUiCatalogService.ProductChoice product : source) {
            if (product != null && product.matchesSearch(query)) {
                model.addElement(product);
            }
        }
        matches.clearSelection();
        accept.setEnabled(false);
    }

    private void acceptSelected() {
        RecipeUiCatalogService.ProductChoice selected = matches.getSelectedValue();
        if (selected != null) {
            result = selected;
            dispose();
        }
    }
}
