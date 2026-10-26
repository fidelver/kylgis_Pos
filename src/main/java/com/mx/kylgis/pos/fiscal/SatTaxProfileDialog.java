package com.mx.kylgis.pos.fiscal;

import com.mx.kylgis.pos.data.loader.Session;
import com.mx.kylgis.pos.forms.AppLocal;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Window;

/** Fiscal metadata editor. Saving never rewrites a historical tax RATE. */
public final class SatTaxProfileDialog extends JDialog {
    private final SatTaxRepository repository;
    private final String taxId;
    private final JCheckBox enabled = new JCheckBox();
    private final JComboBox<String> impuesto = new JComboBox<>(new String[]{
        "", "001", "002", "003"
    });
    private final JComboBox<String> factor = new JComboBox<>(new String[]{
        "", "Tasa", "Cuota", "Exento"
    });
    private final JComboBox<String> nature = new JComboBox<>(new String[]{
        "", "Traslado", "Retencion"
    });
    private final JTextField rate = new JTextField(12);
    private final JTextField unit = new JTextField(12);
    private final JTextField from = new JTextField(12);
    private final JTextField until = new JTextField(12);

    public static void showEditor(Component parent, Session session, String taxId) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        SatTaxProfileDialog d = new SatTaxProfileDialog(owner, session, taxId);
        d.setLocationRelativeTo(parent);
        d.setVisible(true);
    }

    private SatTaxProfileDialog(Window owner, Session session, String taxId) {
        super(owner, AppLocal.getIntString("sat.editor.title"), ModalityType.APPLICATION_MODAL);
        this.repository = new SatTaxRepository(session);
        this.taxId = taxId;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JPanel panel = new JPanel(new GridBagLayout());
        int row = 0;
        addField(panel, row++, "sat.editor.enabled", enabled);
        addField(panel, row++, "sat.editor.impuesto", impuesto);
        addField(panel, row++, "sat.editor.factor", factor);
        addField(panel, row++, "sat.editor.nature", nature);
        addField(panel, row++, "sat.editor.rate", rate);
        addField(panel, row++, "sat.editor.unit", unit);
        addField(panel, row++, "sat.editor.from", from);
        addField(panel, row++, "sat.editor.until", until);
        JLabel warning = new JLabel(AppLocal.getIntString("sat.editor.warning"));
        GridBagConstraints w = new GridBagConstraints();
        w.gridx=0; w.gridy=row++; w.gridwidth=2; w.insets=new Insets(6,8,6,8);
        panel.add(warning, w);
        JButton save = new JButton(AppLocal.getIntString("button.save"));
        JButton cancel = new JButton(AppLocal.getIntString("button.Cancel"));
        JPanel controls = new JPanel();
        controls.add(save); controls.add(cancel);
        GridBagConstraints b = new GridBagConstraints();
        b.gridx=0; b.gridy=row; b.gridwidth=2; b.insets=new Insets(4,8,8,8);
        panel.add(controls,b);
        setContentPane(panel);
        factor.addActionListener(e -> updateFields());
        save.addActionListener(e -> save());
        cancel.addActionListener(e -> dispose());
        rate.setToolTipText(AppLocal.getIntString("sat.editor.rate.help"));
        unit.setToolTipText(AppLocal.getIntString("sat.editor.unit.help"));
        from.setToolTipText("AAAA-MM-DD");
        until.setToolTipText("AAAA-MM-DD");
        try {
            if (!repository.schemaAvailable()) {
                save.setEnabled(false);
                JOptionPane.showMessageDialog(this, AppLocal.getIntString("sat.schema.required"),
                        getTitle(), JOptionPane.WARNING_MESSAGE);
            } else {
                SatTaxProfile p = repository.findTax(taxId);
                if (p != null) apply(p);
            }
        } catch (SQLException | RuntimeException ex) {
            save.setEnabled(false);
            JOptionPane.showMessageDialog(this, ex.getMessage(), getTitle(), JOptionPane.ERROR_MESSAGE);
        }
        updateFields();
        pack();
        setMinimumSize(getSize());
    }

    private static void addField(JPanel panel,int y,String title,Component control) {
        GridBagConstraints l = new GridBagConstraints();
        l.gridx=0; l.gridy=y; l.anchor=GridBagConstraints.WEST; l.insets=new Insets(3,10,3,8);
        panel.add(new JLabel(AppLocal.getIntString(title)),l);
        GridBagConstraints f = new GridBagConstraints();
        f.gridx=1; f.gridy=y; f.weightx=1; f.fill=GridBagConstraints.HORIZONTAL;
        f.insets=new Insets(3,8,3,10);
        panel.add(control,f);
    }

    private void updateFields() {
        boolean quota = "Cuota".equals(factor.getSelectedItem());
        boolean exempt = "Exento".equals(factor.getSelectedItem());
        rate.setEnabled(!exempt);
        unit.setEnabled(quota);
        if (exempt) { rate.setText(""); unit.setText(""); }
        if (!quota) unit.setText("");
    }

    private void apply(SatTaxProfile p) {
        enabled.setSelected(p.isEnabled());
        impuesto.setSelectedItem(p.getImpuesto() == null ? "" : p.getImpuesto().trim());
        factor.setSelectedItem(p.getFactor() == null ? "" : p.getFactor().name());
        nature.setSelectedItem(p.getNature() == null ? "" : p.getNature().name());
        rate.setText(p.getTasaCuota() == null ? "" : p.getTasaCuota().toPlainString());
        unit.setText(p.getCuotaUnidad() == null ? "" : p.getCuotaUnidad());
        from.setText(p.getDesde() == null ? "" : p.getDesde().toString());
        until.setText(p.getHasta() == null ? "" : p.getHasta().toString());
    }

    private void save() {
        try {
            String f=(String)factor.getSelectedItem();
            String n=(String)nature.getSelectedItem();
            String tx=(String)impuesto.getSelectedItem();
            String rateString=rate.getText().trim();
            SatTaxProfile p = new SatTaxProfile(taxId,
                tx == null || tx.isBlank() ? null : tx,
                f == null || f.isBlank() ? null : SatTaxProfile.Factor.valueOf(f),
                n == null || n.isBlank() ? null : SatTaxProfile.Nature.valueOf(n),
                rateString.isEmpty() ? null : new BigDecimal(rateString),
                unit.getText(), parseDate(from.getText()), parseDate(until.getText()),
                enabled.isSelected());
            repository.saveTax(p);
            dispose();
        } catch (SQLException | RuntimeException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), getTitle(), JOptionPane.ERROR_MESSAGE);
        }
    }

    private static LocalDate parseDate(String date) {
        return date == null || date.isBlank() ? null : LocalDate.parse(date.trim());
    }
}