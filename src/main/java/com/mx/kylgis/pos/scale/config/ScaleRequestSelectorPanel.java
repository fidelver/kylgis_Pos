//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.scale.config;

import com.mx.kylgis.pos.forms.AppLocal;
import com.mx.kylgis.pos.scale.ScaleRequestCatalog;
import com.mx.kylgis.pos.scale.ScaleRequestCommand;
import com.mx.kylgis.pos.ui.help.UiHelp;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentListener;

/** Selector editable y localizado para el comando de solicitud de báscula. */
public final class ScaleRequestSelectorPanel extends JPanel {

    private final JComboBox<String> command = new JComboBox<>();
    private final JLabel nativeRequest = new JLabel();

    public ScaleRequestSelectorPanel() {
        super(new FlowLayout(FlowLayout.LEFT, 4, 0));
        setOpaque(false);

        add(new JLabel(AppLocal.getIntString("label.scalerequestcommand")));

        command.setEditable(true);
        command.setFont(new java.awt.Font("Arial", 0, 12));
        command.setPreferredSize(new Dimension(145, 28));
        for (String preset : ScaleRequestCatalog.presets()) {
            command.addItem(preset);
        }
        UiHelp.set(command, "scale.request");
        add(command);

        nativeRequest.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 11));
        add(nativeRequest);
        setPreferredSize(new Dimension(400, 30));
        setScaleType(null);
    }

    public String getConfiguredCommand() {
        Object value = command.getEditor().getItem();
        return ScaleRequestCommand.normalize(value == null ? "" : value.toString());
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

    /**
     * Actualiza la capacidad y la referencia nativa según el driver elegido.
     * El valor guardado se conserva aunque el driver actual no lo aplique.
     */
    public void setScaleType(String scaleType) {
        boolean supported = ScaleRequestCatalog.supportsCustomRequest(scaleType);
        command.setEnabled(supported);
        nativeRequest.setEnabled(supported);
        nativeRequest.setText(AppLocal.getIntString("label.scalerequestnative")
                + " " + ScaleRequestCatalog.nativeRequest(scaleType));
        String tooltipKey = supported
                ? "tooltip.scalerequestcommand"
                : "tooltip.scalerequestunsupported";
        String tooltip = AppLocal.getIntString(tooltipKey);
        command.setToolTipText(tooltip);
        nativeRequest.setToolTipText(tooltip);
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
