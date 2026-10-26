// KylGis POS - standard product search dialog
package com.mx.kylgis.pos.panels;

import com.mx.kylgis.pos.basic.BasicException;
import com.mx.kylgis.pos.ticket.CategoryInfo;
import com.mx.kylgis.pos.forms.AppLocal;
import com.mx.kylgis.pos.forms.DataLogicSales;
import com.mx.kylgis.pos.ticket.ProductInfoExt;
import com.mx.kylgis.pos.data.loader.QBFCompareEnum;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

/**
 * One-line product search used by all inventory product finders.
 * Results are explicit choices; the first match is never selected implicitly.
 */
public final class ProductSearchDialog extends JDialog {

    private ProductInfoExt result;
    private final List<ProductOption> source;
    private final JTextField search = new JTextField(32);
    private final DefaultListModel<ProductOption> model = new DefaultListModel<>();
    private final JList<ProductOption> matches = new JList<>(model);
    private final JButton accept = new JButton();
    private final JButton cancel = new JButton();

    private ProductSearchDialog(Window owner, List<ProductOption> source, String title) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        this.source = source == null ? Collections.emptyList() : source;
        buildUi();
        refreshResults();
    }

    public static ProductInfoExt showMessage(Component parent, DataLogicSales sales,
            int productsType) {
        if (sales == null) {
            return null;
        }
        List<ProductOption> options;
        DataLogicSales.ProductSearchContext context = null;
        try {
            context = sales.openProductSearchContext();
            DataLogicSales searchSales = context.getSales();
            Object[] emptyFilter = {
                QBFCompareEnum.COMP_NONE, null,
                QBFCompareEnum.COMP_NONE, null,
                QBFCompareEnum.COMP_NONE, null,
                QBFCompareEnum.COMP_NONE, null,
                QBFCompareEnum.COMP_NONE, null
            };
            List<ProductInfoExt> products;
            switch (productsType) {
                case 1:
                    products = searchSales.getProductListNormal().list(emptyFilter);
                    break;
                case 2:
                    products = searchSales.getProductListAuxiliar().list(emptyFilter);
                    break;
                default:
                    products = searchSales.getProductList().list(emptyFilter);
                    break;
            }
            Map<String, String> categories = new HashMap<>();
            for (Object value : searchSales.getCategoriesList().list()) {
                if (value instanceof CategoryInfo) {
                    CategoryInfo category = (CategoryInfo) value;
                    categories.put(category.getID(), category.getName());
                }
            }
            options = new ArrayList<>();
            for (ProductInfoExt product : products) {
                if (product != null) {
                    options.add(new ProductOption(product,
                            categories.get(product.getCategoryID())));
                }
            }
        } catch (BasicException error) {
            new com.mx.kylgis.pos.data.gui.MessageInf(error).show(parent);
            return null;
        } finally {
            if (context != null) {
                context.close();
            }
        }

        Window owner = windowFor(parent);
        ProductSearchDialog dialog = new ProductSearchDialog(owner, options,
                AppLocal.getIntString("recipe.catalog.choose"));
        dialog.setSize(780, 560);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
        return dialog.result;
    }

    private static Window windowFor(Component parent) {
        if (parent instanceof Window) {
            return (Window) parent;
        }
        return parent == null ? null : windowFor(parent.getParent());
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
                .put(KeyStroke.getKeyStroke("ESCAPE"), "cancelProductSearch");
        getRootPane().getActionMap().put("cancelProductSearch",
                new javax.swing.AbstractAction() {
                    @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                        result = null;
                        dispose();
                    }
                });
        matches.getInputMap(javax.swing.JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke("ENTER"), "acceptProductSearch");
        matches.getActionMap().put("acceptProductSearch",
                new javax.swing.AbstractAction() {
                    @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                        acceptSelected();
                    }
                });
    }

    private void refreshResults() {
        String query = search.getText();
        model.clear();
        for (ProductOption option : source) {
            if (option.matches(query)) {
                model.addElement(option);
            }
        }
        matches.clearSelection();
        accept.setEnabled(false);
    }

    private void acceptSelected() {
        ProductOption selected = matches.getSelectedValue();
        if (selected != null) {
            result = selected.product;
            dispose();
        }
    }

    private static final class ProductOption {
        final ProductInfoExt product;
        final String category;

        ProductOption(ProductInfoExt product, String category) {
            this.product = product;
            this.category = category;
        }

        boolean matches(String query) {
            if (query == null || query.trim().isEmpty()) {
                return true;
            }
            String searchable = join(product.getName(), product.getReference(),
                    product.getCode(), category).toLowerCase(Locale.ROOT);
            for (String token : query.trim().toLowerCase(Locale.ROOT).split("\\s+")) {
                if (!searchable.contains(token)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            String name = safe(product.getName());
            String reference = safe(product.getReference());
            String code = safe(product.getCode());
            String identifier = reference.isEmpty() ? code
                    : code.isEmpty() ? reference : reference + " / " + code;
            String label = identifier.isEmpty() ? name : name + " [" + identifier + "]";
            return category == null || category.trim().isEmpty()
                    ? label : label + " — " + category.trim();
        }

        private static String join(String... values) {
            StringBuilder result = new StringBuilder();
            for (String value : values) {
                if (value != null && !value.trim().isEmpty()) {
                    if (result.length() > 0) result.append(' ');
                    result.append(value.trim());
                }
            }
            return result.toString();
        }

        private static String safe(String value) {
            return value == null ? "" : value.trim();
        }
    }
}
