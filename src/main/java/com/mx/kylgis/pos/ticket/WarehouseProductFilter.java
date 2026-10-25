package com.mx.kylgis.pos.ticket;

import com.mx.kylgis.pos.basic.BasicException;
import com.mx.kylgis.pos.data.gui.ComboBoxValModel;
import com.mx.kylgis.pos.data.loader.Datas;
import com.mx.kylgis.pos.data.loader.QBFCompareEnum;
import com.mx.kylgis.pos.data.loader.SentenceList;
import com.mx.kylgis.pos.data.loader.SerializerWrite;
import com.mx.kylgis.pos.data.loader.SerializerWriteBasic;
import com.mx.kylgis.pos.forms.AppLocal;
import com.mx.kylgis.pos.forms.AppView;
import com.mx.kylgis.pos.forms.DataLogicSales;
import com.mx.kylgis.pos.reports.ReportEditorCreator;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Search parameters for warehouse reports.
 *
 * The free-text field deliberately maps to all product identifiers that are
 * useful in the warehouse report: name, barcode, reference/key and UOM name.
 * Category remains an independent selector and the stock quantity filter is
 * intentionally not exposed here.
 */
public final class WarehouseProductFilter extends javax.swing.JPanel
        implements ReportEditorCreator {

    private SentenceList categories;
    private ComboBoxValModel categoryModel;
    private final JTextField search = new JTextField();
    private final JComboBox category = new JComboBox();
    private final JButton reset = new JButton();
    private final JLabel searchLabel = new JLabel();
    private final JLabel categoryLabel = new JLabel();

    public WarehouseProductFilter() {
        buildUi();
    }

    @Override
    public void init(AppView app) {
        DataLogicSales sales = (DataLogicSales) app.getBean(
                "com.mx.kylgis.pos.forms.DataLogicSales");
        categories = sales.getCategoriesList();
        categoryModel = new ComboBoxValModel();
        category.setModel(categoryModel);
    }

    @Override
    public void activate() throws BasicException {
        List categoryList = categories.list();
        categoryList.add(0, null);
        categoryModel = new ComboBoxValModel(categoryList);
        category.setModel(categoryModel);
        clearFilters();
    }

    @Override
    public SerializerWrite getSerializerWrite() {
        return new SerializerWriteBasic(
                Datas.OBJECT, Datas.STRING,
                Datas.OBJECT, Datas.STRING);
    }

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public Object createValue() throws BasicException {
        String value = search.getText() == null ? "" : search.getText().trim();
        Object selectedCategory = categoryModel == null
                ? null : categoryModel.getSelectedKey();
        return new Object[] {
            value.isEmpty() ? QBFCompareEnum.COMP_NONE : QBFCompareEnum.COMP_RE,
            value.isEmpty() ? null : "%" + value + "%",
            selectedCategory == null
                    ? QBFCompareEnum.COMP_NONE : QBFCompareEnum.COMP_EQUALS,
            selectedCategory
        };
    }

    private void buildUi() {
        setPreferredSize(new java.awt.Dimension(946, 58));
        setLayout(new GridBagLayout());

        searchLabel.setText(AppLocal.getIntString("label.prodsearch"));
        searchLabel.setFont(new java.awt.Font("Arial", 0, 14));
        categoryLabel.setText(AppLocal.getIntString("label.prodcategory"));
        categoryLabel.setFont(new java.awt.Font("Arial", 0, 14));

        search.setFont(new java.awt.Font("Arial", 0, 14));
        search.setPreferredSize(new java.awt.Dimension(315, 30));
        search.setToolTipText(AppLocal.getIntString("tooltip.warehouse.search"));
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { updateSearchWidth(); }
            @Override public void removeUpdate(DocumentEvent event) { updateSearchWidth(); }
            @Override public void changedUpdate(DocumentEvent event) { updateSearchWidth(); }
        });
        category.setFont(new java.awt.Font("Arial", 0, 14));
        category.setPreferredSize(new java.awt.Dimension(220, 30));

        reset.setIcon(new javax.swing.ImageIcon(getClass().getResource(
                "/com/mx/kylgis/pos/images/reload.png")));
        reset.setToolTipText(AppLocal.getIntString("tooltip.search.reset"));
        reset.getAccessibleContext().setAccessibleName(
                AppLocal.getIntString("tooltip.search.reset"));
        reset.getAccessibleContext().setAccessibleDescription(
                AppLocal.getIntString("tooltip.search.reset"));
        reset.addActionListener(event -> clearFilters());

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 8, 4, 8);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy = 0;

        c.gridx = 0;
        c.weightx = 0;
        add(searchLabel, c);
        c.gridx = 1;
        c.weightx = 0;
        add(search, c);
        c.gridx = 2;
        c.weightx = 0;
        add(categoryLabel, c);
        c.gridx = 3;
        c.weightx = 1;
        add(category, c);
        c.gridx = 4;
        c.weightx = 0;
        add(reset, c);
    }

    private void clearFilters() {
        search.setText("");
        if (category.getItemCount() > 0) {
            category.setSelectedIndex(0);
        }
        updateSearchWidth();
    }

    /** Keeps the search row readable without allowing it to displace category and reset. */
    private void updateSearchWidth() {
        String value = search.getText() == null ? "" : search.getText();
        int textWidth = search.getFontMetrics(search.getFont()).stringWidth(value);
        int width = Math.max(315, Math.min(510, textWidth + 36));
        search.setPreferredSize(new java.awt.Dimension(width, 30));
        search.setMinimumSize(new java.awt.Dimension(315, 30));
        search.setMaximumSize(new java.awt.Dimension(510, 30));
        search.revalidate();
        revalidate();
        repaint();
    }
}
