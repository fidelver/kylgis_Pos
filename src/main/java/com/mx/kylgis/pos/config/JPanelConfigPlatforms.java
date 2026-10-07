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

package com.mx.kylgis.pos.config;

import com.mx.kylgis.pos.data.user.DirtyManager;
import com.mx.kylgis.pos.forms.AppConfig;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

/**
 * Configuration of additional sales platforms.
 * Built-in platforms are always supplied by the application.
 */
public class JPanelConfigPlatforms extends JPanel implements PanelConfig {

    private static final String[] BUILT_INS = {"UBER EATS", "DIDI FOOD", "RAPPI"};

    private final DirtyManager dirty = new DirtyManager();
    private final DefaultListModel<String> model = new DefaultListModel<>();
    private final JList<String> customPlatforms = new JList<>(model);
    private final JTextField newPlatform = new JTextField(20);

    public JPanelConfigPlatforms() {
        setOpaque(true);
        setBackground(javax.swing.UIManager.getColor("Panel.background"));
        setLayout(new BorderLayout(8, 8));

        JPanel included = new JPanel(new GridLayout(0, 1, 2, 2));
        included.setOpaque(false);
        included.setBorder(javax.swing.BorderFactory.createTitledBorder("Plataformas incluidas (fijas)"));
        for (String builtIn : BUILT_INS) {
            included.add(new JLabel("\u2713  " + builtIn));
        }

        JPanel top = new JPanel(new BorderLayout(4, 4));
        top.setOpaque(false);
        top.add(included, BorderLayout.CENTER);
        top.add(new JLabel("Las plataformas incluidas siempre estarán disponibles y no se pueden eliminar."),
                BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        JScrollPane customScroll = new JScrollPane(customPlatforms);
        customScroll.setBorder(javax.swing.BorderFactory.createTitledBorder("Plataformas adicionales"));
        add(customScroll, BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.setOpaque(false);
        JButton add = new JButton("Agregar");
        JButton remove = new JButton("Eliminar seleccionada");
        controls.add(new JLabel("Nueva plataforma:"));
        controls.add(newPlatform);
        controls.add(add);
        controls.add(remove);
        add(controls, BorderLayout.SOUTH);

        add.addActionListener(e -> addPlatform());
        remove.addActionListener(e -> removePlatform());
    }

    private void addPlatform() {
        String name = normalize(newPlatform.getText());
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El nombre de la plataforma es obligatorio.",
                    "Plataformas", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (name.indexOf('|') >= 0) {
            JOptionPane.showMessageDialog(this, "El nombre de la plataforma no puede contener el carácter |.",
                    "Plataformas", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (isBuiltIn(name) || contains(name)) {
            JOptionPane.showMessageDialog(this, "La plataforma ya existe.",
                    "Plataformas", JOptionPane.WARNING_MESSAGE);
            return;
        }
        model.addElement(name);
        newPlatform.setText("");
        dirty.setDirty(true);
    }

    private void removePlatform() {
        int index = customPlatforms.getSelectedIndex();
        if (index >= 0) {
            model.remove(index);
            dirty.setDirty(true);
        }
    }

    @Override
    public boolean hasChanged() {
        return dirty.isDirty();
    }

    @Override
    public Component getConfigComponent() {
        return this;
    }

    @Override
    public void loadProperties(AppConfig config) {
        model.clear();
        String value = config.getProperty("sales.platforms.custom");
        if (value != null && !value.trim().isEmpty()) {
            Set<String> unique = new LinkedHashSet<>();
            for (String item : value.split("\\|")) {
                String name = normalize(item);
                if (!name.isEmpty() && !isBuiltIn(name)) {
                    unique.add(name);
                }
            }
            for (String name : unique) {
                model.addElement(name);
            }
        }
        dirty.setDirty(false);
    }

    @Override
    public void saveProperties(AppConfig config) {
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < model.size(); i++) {
            if (value.length() > 0) {
                value.append('|');
            }
            value.append(model.get(i));
        }
        config.setProperty("sales.platforms.custom", value.toString());
        dirty.setDirty(false);
    }

    private boolean contains(String name) {
        for (int i = 0; i < model.size(); i++) {
            if (model.get(i).equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBuiltIn(String name) {
        for (String builtIn : BUILT_INS) {
            if (builtIn.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}
