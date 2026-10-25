package com.mx.kylgis.pos.fiscal;

import com.mx.kylgis.pos.data.loader.Session;
import com.mx.kylgis.pos.forms.AppLocal;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.math.BigDecimal;
import java.sql.SQLException;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** Product-to-SAT classification editor, independent from the reserved ProductsEditor. */
public final class SatProductMetadataDialog extends JDialog {
    private final SatTaxRepository repository;
    private final JTextField search = new JTextField(18);
    private final JComboBox<SatTaxRepository.ProductChoice> results = new JComboBox<>();
    private final JTextField objeto = new JTextField(12);
    private final JTextField claveProd = new JTextField(12);
    private final JTextField claveUnidad = new JTextField(12);
    private final JTextField cuotaFactor = new JTextField(12);
    private final JTextField cuotaUnidad = new JTextField(12);
    private final JButton save = new JButton(AppLocal.getIntString("button.save"));
    private boolean schemaReady = false;

    public static void showEditor(Component parent, Session session) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        SatProductMetadataDialog dialog = new SatProductMetadataDialog(owner, session);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    private SatProductMetadataDialog(Window owner, Session session) {
        super(owner, AppLocal.getIntString("sat.product.title"), ModalityType.APPLICATION_MODAL);
        repository = new SatTaxRepository(session);
        JPanel content = new JPanel(new BorderLayout(8,8));
        JPanel fields = new JPanel(new GridBagLayout());
        int row=0;
        JPanel finder = new JPanel(new BorderLayout(4,0));
        JButton find = new JButton(AppLocal.getIntString("sat.product.search"));
        finder.add(search,BorderLayout.CENTER);
        finder.add(find,BorderLayout.EAST);
        addField(fields,row++,"sat.product.query",finder);
        addField(fields,row++,"sat.product.result",results);
        addField(fields,row++,"sat.product.objeto",objeto);
        addField(fields,row++,"sat.product.prods",claveProd);
        addField(fields,row++,"sat.product.unidad",claveUnidad);
        addField(fields,row++,"sat.product.cuotafactor",cuotaFactor);
        addField(fields,row++,"sat.product.cuotaunidad",cuotaUnidad);
        content.add(fields,BorderLayout.CENTER);
        JLabel warn=new JLabel(AppLocal.getIntString("sat.editor.warning"));
        content.add(warn,BorderLayout.NORTH);
        JPanel buttons=new JPanel();
        JButton cancel=new JButton(AppLocal.getIntString("button.Cancel"));
        buttons.add(save);buttons.add(cancel);
        content.add(buttons,BorderLayout.SOUTH);
        setContentPane(content);
        find.addActionListener(e->search());
        search.addActionListener(e->search());
        results.addActionListener(e->loadSelected());
        save.addActionListener(e->save());
        cancel.addActionListener(e->dispose());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(650,430);
        setMinimumSize(getSize());
        try {
            schemaReady=repository.schemaAvailable();
            if (!schemaReady) JOptionPane.showMessageDialog(this,
                    AppLocal.getIntString("sat.schema.required"),getTitle(),JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,e.getMessage(),getTitle(),JOptionPane.ERROR_MESSAGE);
        }
        save.setEnabled(false);
    }

    private static void addField(JPanel parent,int y,String label,Component input) {
        GridBagConstraints k=new GridBagConstraints();
        k.gridx=0;k.gridy=y;k.insets=new Insets(4,8,4,8);k.anchor=GridBagConstraints.WEST;
        parent.add(new JLabel(AppLocal.getIntString(label)),k);
        k=new GridBagConstraints();
        k.gridx=1;k.gridy=y;k.weightx=1;k.fill=GridBagConstraints.HORIZONTAL;
        k.insets=new Insets(4,8,4,8);
        parent.add(input,k);
    }

    private void search() {
        results.removeAllItems();
        save.setEnabled(false);
        if (!schemaReady) return;
        try {
            for(SatTaxRepository.ProductChoice p:repository.searchProducts(search.getText())) {
                results.addItem(p);
            }
            if(results.getItemCount()==0) {
                JOptionPane.showMessageDialog(this,AppLocal.getIntString("sat.product.none"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,e.getMessage(),getTitle(),JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadSelected() {
        SatTaxRepository.ProductChoice p=(SatTaxRepository.ProductChoice)results.getSelectedItem();
        clearFields();
        save.setEnabled(schemaReady && p!=null);
        if (p==null) return;
        try {
            SatConceptMetadata m=repository.findProduct(p.id);
            if(m==null) return;
            objeto.setText(m.getObjetoImp());
            claveProd.setText(m.getClaveProdServ());
            claveUnidad.setText(m.getClaveUnidad());
            if(m.getCuotaCantidadPorUnidad()!=null) cuotaFactor.setText(m.getCuotaCantidadPorUnidad().toPlainString());
            cuotaUnidad.setText(m.getCuotaUnidad());
        } catch(SQLException | RuntimeException ex) {
            save.setEnabled(false);
            JOptionPane.showMessageDialog(this,ex.getMessage(),getTitle(),JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearFields() {
        objeto.setText("");claveProd.setText("");claveUnidad.setText("");
        cuotaFactor.setText("");cuotaUnidad.setText("");
    }
    private static String nullable(String v) {
        return v==null || v.trim().isEmpty()?null:v.trim();
    }
    private void save() {
        SatTaxRepository.ProductChoice p=(SatTaxRepository.ProductChoice)results.getSelectedItem();
        if(!schemaReady || p==null) return;
        try {
            SatConceptMetadata m=new SatConceptMetadata(p.id,nullable(objeto.getText()),
                    nullable(claveProd.getText()),nullable(claveUnidad.getText()),
                    nullable(cuotaFactor.getText())==null?null:new BigDecimal(cuotaFactor.getText().trim()),
                    nullable(cuotaUnidad.getText()));
            repository.saveProduct(m);
            JOptionPane.showMessageDialog(this,AppLocal.getIntString("sat.product.saved"));
        } catch(SQLException | RuntimeException ex) {
            JOptionPane.showMessageDialog(this,ex.getMessage(),getTitle(),JOptionPane.ERROR_MESSAGE);
        }
    }
}