//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.scale.config;

import com.mx.kylgis.pos.scale.ScaleRequestCommand;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentListener;

/** Compact editable selector for the scale weight-request command. */
public final class ScaleRequestSelectorPanel extends JPanel {

    private final JComboBox<String> command = new JComboBox<>();

    public ScaleRequestSelectorPanel() {
        super(new FlowLayout(FlowLayout.LEFT, 4, 0));
        setOpaque(false);
        add(new JLabel("Solicitud:"));

        command.setEditable(true);
        command.setFont(new java.awt.Font("Arial", 0, 12));
        command.setPreferredSize(new Dimension(145, 28));
        command.addItem(ScaleRequestCommand.AUTO);
        command.addItem("ASCII:P");
        command.addItem("ASCII:W");
        command.addItem("HEX:57 0D");
        command.addItem("ASCII:$");
        command.addItem("HEX:05");
        command.setToolTipText("AUTO o comando editable: ASCII:P / HEX:50 0D (1 a 8 bytes)");
        add(command);
        setPreferredSize(new Dimension(220, 30));
    }

    public String getConfiguredCommand() {
        return ScaleRequestCommand.normalize(String.valueOf(command.getEditor().getItem()));
    }

    public void setConfiguredCommand(String value) {
        String normalized;
        try {
            normalized = ScaleRequestCommand.normalize(value);
        } catch (IllegalArgumentException ex) {
            // Preserve an unknown historic value visibly so the user can correct it.
            normalized = value == null || value.trim().isEmpty()
                    ? ScaleRequestCommand.AUTO : value.trim();
        }
        command.setSelectedItem(normalized);
    }

    public void addActionListener(ActionListener listener) {
        command.addActionListener(listener);
    }

    public void addDocumentListener(DocumentListener listener) {
        Object editor = command.getEditor().getEditorComponent();
        if (editor instanceof JTextField) {
            ((JTextField) editor).getDocument().addDocumentListener(listener);
        }
    }
}
