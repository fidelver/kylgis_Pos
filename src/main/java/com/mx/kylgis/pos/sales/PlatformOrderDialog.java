//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
//    KylGis POS implementation by Fidel Arcos.
//
//    This file is part of KylGis POS
//
//    KylGis POS is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    KylGis POS is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with KylGis POS. If not, see <http://www.gnu.org/licenses/>.

package com.mx.kylgis.pos.sales;

import com.mx.kylgis.pos.ticket.TicketInfo;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Native editor for marketplace/platform order metadata.
 *
 * The platform is selected explicitly. Code format is validation metadata,
 * not a mechanism for guessing the platform.
 */
public final class PlatformOrderDialog {

    private static final String[] DEFAULT_PLATFORMS = {
        "UBER EATS", "DIDI FOOD", "RAPPI"
    };

    private PlatformOrderDialog() {
    }

    public static boolean edit(Component parent, TicketInfo ticket, String customPlatforms) {
        if (ticket == null) {
            JOptionPane.showMessageDialog(parent, "No hay un ticket activo.",
                    "Código de orden", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        JComboBox<String> platform = new JComboBox<>();
        for (String name : platformNames(customPlatforms)) {
            platform.addItem(name);
        }
        JTextField code = new JTextField(18);
        JTextField customer = new JTextField(18);

        String currentPlatform = ticket.getProperty("origen_plataforma", "");
        String currentCode = ticket.getProperty("codigo_orden", "");
        String currentCustomer = ticket.getProperty("cliente_plataforma", "");

        platform.setEditable(false);
        if (!currentPlatform.isEmpty()) {
            boolean found = false;
            for (int i = 0; i < platform.getItemCount(); i++) {
                if (currentPlatform.equalsIgnoreCase(platform.getItemAt(i))) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                platform.addItem(currentPlatform);
            }
            platform.setSelectedItem(currentPlatform);
        }
        code.setText(currentCode);
        customer.setText(currentCustomer);
        platform.setPreferredSize(new java.awt.Dimension(220, platform.getPreferredSize().height));
        code.setPreferredSize(new java.awt.Dimension(220, code.getPreferredSize().height));
        customer.setPreferredSize(new java.awt.Dimension(220, customer.getPreferredSize().height));

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0;
        panel.add(new JLabel("Plataforma:"), c);
        c.gridx = 1;
        panel.add(platform, c);

        c.gridx = 0; c.gridy = 1;
        panel.add(new JLabel("Código de orden:"), c);
        c.gridx = 1;
        panel.add(code, c);

        c.gridx = 0; c.gridy = 2;
        panel.add(new JLabel("Cliente / usuario:"), c);
        c.gridx = 1;
        panel.add(customer, c);

        Object[] options = isPlatformOrder(ticket)
                ? new Object[]{"Guardar", "Quitar plataforma", "Cancelar"}
                : new Object[]{"Guardar", "Cancelar"};

        while (true) {
            int result = JOptionPane.showOptionDialog(parent, panel, "Código de orden",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                    null, options, options[0]);

            if (result == 1 && isPlatformOrder(ticket)) {
                clear(ticket);
                return true;
            }

            if (result != 0) {
                return false;
            }

            String cleanCode = code.getText() == null ? "" : code.getText().trim();
            if (cleanCode.isEmpty()) {
                JOptionPane.showMessageDialog(parent,
                        "El código de orden es obligatorio para una venta de plataforma.",
                        "Código de orden", JOptionPane.WARNING_MESSAGE);
                continue;
            }

            String selectedPlatform = String.valueOf(platform.getSelectedItem()).trim();
            ticket.setProperty("venta_plataforma", "SI");
            ticket.setProperty("origen_plataforma", selectedPlatform);
            ticket.setProperty("codigo_orden", cleanCode.toUpperCase());
            ticket.setProperty("cliente_plataforma",
                    customer.getText() == null ? "" : customer.getText().trim().toUpperCase());
            ticket.setProperty("codigo_validado", "SI");
            return true;
        }
    }

    private static List<String> platformNames(String customPlatforms) {
        List<String> names = new ArrayList<>();
        for (String name : DEFAULT_PLATFORMS) {
            names.add(name);
        }
        if (customPlatforms != null && !customPlatforms.trim().isEmpty()) {
            for (String item : customPlatforms.split("\\|")) {
                String name = item.trim().toUpperCase();
                if (!name.isEmpty() && !names.contains(name)) {
                    names.add(name);
                }
            }
        }
        return names;
    }

    public static boolean isPlatformOrder(TicketInfo ticket) {
        return ticket != null && "SI".equalsIgnoreCase(
                ticket.getProperty("venta_plataforma", "NO"));
    }

    public static boolean hasValidCode(TicketInfo ticket) {
        return isPlatformOrder(ticket)
                && "SI".equalsIgnoreCase(ticket.getProperty("codigo_validado", "NO"))
                && !ticket.getProperty("codigo_orden", "").trim().isEmpty()
                && !ticket.getProperty("origen_plataforma", "").trim().isEmpty();
    }

    public static void clear(TicketInfo ticket) {
        ticket.setProperty("venta_plataforma", "NO");
        ticket.setProperty("codigo_orden", "");
        ticket.setProperty("origen_plataforma", "");
        ticket.setProperty("cliente_plataforma", "");
        ticket.setProperty("codigo_validado", "NO");
    }
}
