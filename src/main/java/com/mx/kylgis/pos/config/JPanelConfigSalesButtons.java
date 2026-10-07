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
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Per-installation visibility of optional sales toolbar buttons.
 * Permissions remain independent: hiding a button is not authorization.
 */
public class JPanelConfigSalesButtons extends JPanel implements PanelConfig {

    private final DirtyManager dirty = new DirtyManager();
    private final JCheckBox platformOrder = new JCheckBox("Código de orden / plataformas");
    private final JCheckBox scale = new JCheckBox("Báscula");
    private final JCheckBox remotePrint = new JCheckBox("Impresión remota / Kitchen Screen");
    private final JCheckBox split = new JCheckBox("Dividir cuenta");
    private final JCheckBox reprint = new JCheckBox("Imprimir último ticket");
    private final JCheckBox ticketPreview = new JCheckBox("Imprimir / previsualizar ticket actual");

    public JPanelConfigSalesButtons() {
        setOpaque(true);
        setBackground(javax.swing.UIManager.getColor("Panel.background"));
        setLayout(new BorderLayout(10, 10));

        JLabel note = new JLabel(
                "<html>Seleccione los botones opcionales que deben mostrarse en la pantalla de venta.<br>"
                + "Los permisos de usuario continúan controlándose mediante roles.</html>");
        add(note, BorderLayout.NORTH);

        JPanel options = new JPanel(new GridLayout(0, 1, 2, 2));
        options.setOpaque(false);
        options.setBorder(BorderFactory.createTitledBorder("Botones opcionales"));
        options.add(platformOrder);
        options.add(scale);
        options.add(remotePrint);
        options.add(split);
        options.add(reprint);
        options.add(ticketPreview);

        JPanel optionsWrapper = new JPanel(new BorderLayout());
        optionsWrapper.setOpaque(false);
        optionsWrapper.add(options, BorderLayout.NORTH);
        add(optionsWrapper, BorderLayout.CENTER);

        platformOrder.addActionListener(dirty);
        scale.addActionListener(dirty);
        remotePrint.addActionListener(dirty);
        split.addActionListener(dirty);
        reprint.addActionListener(dirty);
        ticketPreview.addActionListener(dirty);
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
        platformOrder.setSelected(readBoolean(config, "sales.button.platformorder", false));
        scale.setSelected(readBoolean(config, "sales.button.scale", true));
        remotePrint.setSelected(readBoolean(config, "sales.button.remoteprint", true));
        split.setSelected(readBoolean(config, "sales.button.split", true));
        reprint.setSelected(readBoolean(config, "sales.button.reprint", true));
        ticketPreview.setSelected(readBoolean(config, "sales.button.ticketpreview", true));
        dirty.setDirty(false);
    }

    @Override
    public void saveProperties(AppConfig config) {
        config.setProperty("sales.button.platformorder", Boolean.toString(platformOrder.isSelected()));
        config.setProperty("sales.button.scale", Boolean.toString(scale.isSelected()));
        config.setProperty("sales.button.remoteprint", Boolean.toString(remotePrint.isSelected()));
        config.setProperty("sales.button.split", Boolean.toString(split.isSelected()));
        config.setProperty("sales.button.reprint", Boolean.toString(reprint.isSelected()));
        config.setProperty("sales.button.ticketpreview", Boolean.toString(ticketPreview.isSelected()));
        dirty.setDirty(false);
    }

    private boolean readBoolean(AppConfig config, String key, boolean defaultValue) {
        String value = config.getProperty(key);
        return value == null ? defaultValue : Boolean.parseBoolean(value);
    }
}
