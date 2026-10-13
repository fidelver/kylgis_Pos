//    KylGis POS Punto de Venta Táctil
package com.mx.kylgis.pos.voucher;

import com.mx.kylgis.pos.basic.BasicException;
import com.mx.kylgis.pos.forms.AppLocal;
import com.mx.kylgis.pos.forms.AppUser;
import com.mx.kylgis.pos.forms.DataLogicSystem;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

/** Secondary authorization used only when editing a sale that consumed vouchers. */
public final class VoucherEditAuthorization {

    private VoucherEditAuthorization() {
    }

    public static AppUser request(Component parent, DataLogicSystem dlSystem) throws BasicException {
        List<AppUser> privileged = new ArrayList<>();
        for (Object item : dlSystem.listPeopleVisible()) {
            if (item instanceof AppUser user && VoucherEditContext.isPrivilegedRole(user.getRole())) {
                privileged.add(user);
            }
        }

        if (privileged.isEmpty()) {
            JOptionPane.showMessageDialog(parent,
                    AppLocal.getIntString("message.voucheredit.noauthorizers"),
                    AppLocal.getIntString("title.voucheredit.authorization"),
                    JOptionPane.WARNING_MESSAGE);
            return null;
        }

        JComboBox<AppUser> users = new JComboBox<>(privileged.toArray(AppUser[]::new));
        users.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setText(value instanceof AppUser user ? user.getName() : "");
                return this;
            }
        });
        JPasswordField password = new JPasswordField(16);
        JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
        panel.add(new JLabel(AppLocal.getIntString("message.voucheredit.authrequired")));
        panel.add(new JLabel(AppLocal.getIntString("label.voucheredit.authorizer")));
        panel.add(users);
        panel.add(new JLabel(AppLocal.getIntString("label.Password")));
        panel.add(password);

        int option = JOptionPane.showConfirmDialog(parent, panel,
                AppLocal.getIntString("title.voucheredit.authorization"),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (option != JOptionPane.OK_OPTION) {
            clear(password.getPassword());
            return null;
        }

        AppUser selected = (AppUser) users.getSelectedItem();
        char[] chars = password.getPassword();
        try {
            String supplied = new String(chars);
            boolean valid = selected != null
                    && (selected.authenticate() ? supplied.isEmpty() : selected.authenticate(supplied));
            if (!valid) {
                JOptionPane.showMessageDialog(parent,
                        AppLocal.getIntString("message.voucheredit.authfailed"),
                        AppLocal.getIntString("title.voucheredit.authorization"),
                        JOptionPane.ERROR_MESSAGE);
                return null;
            }
            return selected;
        } finally {
            clear(chars);
            password.setText("");
        }
    }

    private static void clear(char[] value) {
        if (value != null) {
            Arrays.fill(value, '\0');
        }
    }
}
