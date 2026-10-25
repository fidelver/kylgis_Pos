package com.mx.kylgis.pos.data.gui;

import com.mx.kylgis.pos.basic.BasicException;
import com.mx.kylgis.pos.data.loader.Vectorer;
import com.mx.kylgis.pos.forms.AppLocal;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Standard one-line search for navigators backed by tabular data.
 *
 * The query is matched against every visible value supplied by the
 * navigator's Vectorer. Results are explicit choices; no first row is
 * selected implicitly.
 */
public final class StandardSearchDialog extends JDialog {

    private Integer result;
    private final List<SearchEntry> source;
    private final JTextField search = new JTextField(32);
    private final DefaultListModel<SearchEntry> model = new DefaultListModel<>();
    private final JList<SearchEntry> matches = new JList<>(model);
    private final JButton accept = new JButton();
    private final JButton cancel = new JButton();

    private StandardSearchDialog(Window owner, List<SearchEntry> source) {
        super(owner, AppLocal.getIntString("recipe.catalog.choose"), ModalityType.APPLICATION_MODAL);
        this.source = source == null ? Collections.emptyList() : source;
        buildUi();
        refreshResults();
    }

    /**
     * Opens the standard search and returns the original list index selected,
     * or null when the user cancels/closes the dialog.
     */
    public static Integer showMessage(Component parent, ListModel<?> data, Vectorer vectorer) {
        if (data == null || vectorer == null) {
            return null;
        }

        List<SearchEntry> entries = new ArrayList<>();
        try {
            for (int index = 0; index < data.getSize(); index++) {
                Object value = data.getElementAt(index);
                String[] values = vectorer.getValues(value);
                entries.add(new SearchEntry(index, values));
            }
        } catch (BasicException error) {
            new MessageInf(error).show(parent);
            return null;
        }

        Window owner = windowFor(parent);
        StandardSearchDialog dialog = new StandardSearchDialog(owner, entries);
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
        getContentPane().setLayout(new BorderLayout(8, 8));
        ((JPanel) getContentPane()).setBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel header = new JPanel(new BorderLayout(6, 0));
        header.add(new JLabel(AppLocal.getIntString("recipe.catalog.search")),
                BorderLayout.WEST);
        header.add(search, BorderLayout.CENTER);
        getContentPane().add(header, BorderLayout.NORTH);

        matches.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        matches.setToolTipText(AppLocal.getIntString("tooltip.search.results"));
        getContentPane().add(new JScrollPane(matches), BorderLayout.CENTER);

        accept.setText(AppLocal.getIntString("recipe.catalog.accept"));
        cancel.setText(AppLocal.getIntString("recipe.catalog.cancel"));
        accept.setEnabled(false);
        accept.setToolTipText(AppLocal.getIntString("recipe.catalog.accept.help"));
        cancel.setToolTipText(AppLocal.getIntString("recipe.catalog.cancel.help"));

        JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        actions.add(cancel);
        actions.add(accept);
        getContentPane().add(actions, BorderLayout.SOUTH);

        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { refreshResults(); }
            @Override public void removeUpdate(DocumentEvent event) { refreshResults(); }
            @Override public void changedUpdate(DocumentEvent event) { refreshResults(); }
        });
        matches.addListSelectionListener(event ->
                accept.setEnabled(!matches.isSelectionEmpty()));
        matches.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() >= 2
                        && SwingUtilities.isLeftMouseButton(event)) {
                    acceptSelected();
                }
            }
        });
        accept.addActionListener(event -> acceptSelected());
        cancel.addActionListener(event -> closeWithoutSelection());
        getRootPane().setDefaultButton(accept);
        getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ESCAPE"), "cancelStandardSearch");
        getRootPane().getActionMap().put("cancelStandardSearch",
                new javax.swing.AbstractAction() {
                    @Override public void actionPerformed(java.awt.event.ActionEvent event) {
                        closeWithoutSelection();
                    }
                });
        matches.getInputMap(javax.swing.JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke("ENTER"), "acceptStandardSearch");
        matches.getActionMap().put("acceptStandardSearch",
                new javax.swing.AbstractAction() {
                    @Override public void actionPerformed(java.awt.event.ActionEvent event) {
                        acceptSelected();
                    }
                });
    }

    private void refreshResults() {
        String query = search.getText();
        model.clear();
        for (SearchEntry entry : source) {
            if (entry.matches(query)) {
                model.addElement(entry);
            }
        }
        matches.clearSelection();
        accept.setEnabled(false);
    }

    private void acceptSelected() {
        SearchEntry selected = matches.getSelectedValue();
        if (selected != null) {
            result = selected.index;
            dispose();
        }
    }

    private void closeWithoutSelection() {
        result = null;
        dispose();
    }

    private static final class SearchEntry {
        private final int index;
        private final String[] values;
        private final String searchable;
        private final String label;

        SearchEntry(int index, String[] values) {
            this.index = index;
            this.values = values == null ? new String[0] : values.clone();
            this.searchable = join(this.values).toLowerCase(Locale.ROOT);
            this.label = joinForDisplay(this.values);
        }

        boolean matches(String query) {
            if (query == null || query.trim().isEmpty()) {
                return true;
            }
            for (String token : query.trim().toLowerCase(Locale.ROOT).split("\\s+")) {
                if (!searchable.contains(token)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            return label;
        }

        private static String join(String[] values) {
            StringBuilder text = new StringBuilder();
            if (values != null) {
                for (String value : values) {
                    if (value != null && !value.trim().isEmpty()) {
                        if (text.length() > 0) {
                            text.append(' ');
                        }
                        text.append(value.trim());
                    }
                }
            }
            return text.toString();
        }

        private static String joinForDisplay(String[] values) {
            StringBuilder text = new StringBuilder();
            if (values != null) {
                for (String value : values) {
                    if (value != null && !value.trim().isEmpty()) {
                        if (text.length() > 0) {
                            text.append("  |  ");
                        }
                        text.append(value.trim());
                    }
                }
            }
            return text.length() == 0 ? "—" : text.toString();
        }
    }
}
