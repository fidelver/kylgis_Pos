//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.config;

import com.mx.kylgis.pos.config.provisioning.NodeConfigurationStore;
import com.mx.kylgis.pos.config.provisioning.NodeConfigurationStore.NodeDefinition;
import com.mx.kylgis.pos.forms.AppConfig;
import com.mx.kylgis.pos.node.NodeContext;
import com.mx.kylgis.pos.node.NodeRole;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Administration panel for KylGis MASTER node modules.
 * Secrets are intentionally excluded from this UI.
 */
public final class JPanelConfigNodes extends JPanel implements PanelConfig {

    private final JComboBox<String> nodeSelector = new JComboBox<>();
    private final JPanel rolesPanel = new JPanel(new GridLayout(0, 3, 8, 4));
    private final Map<NodeRole, JCheckBox> roleChecks = new EnumMap<>(NodeRole.class);
    private final JTextField profile = new JTextField();
    private final JTextField dbServer = new JTextField();
    private final JTextField dbPort = new JTextField();
    private final JTextField dbName = new JTextField();
    private final JLabel effectiveServer = new JLabel("-");
    private final JLabel effectivePort = new JLabel("-");
    private final JLabel effectiveDatabase = new JLabel("-");
    private final JLabel masterPath = new JLabel("-");
    private final JLabel modulePath = new JLabel("-");
    private final JLabel bootstrapPath = new JLabel("-");
    private final JLabel status = new JLabel(" ");
    private final JButton reloadButton = new JButton("Recargar");
    private final JButton saveButton = new JButton("Guardar nodo");
    private final JButton provisionButton = new JButton("Aprovisionar bootstrap");

    private NodeConfigurationStore store;
    private String currentNodeId;
    private boolean loading;
    private boolean dirty;
    private boolean administrationEnabled;
    private String runtimeNodeId;

    public JPanelConfigNodes() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        initializeRoleSelector();
        add(buildHeader(), BorderLayout.NORTH);
        add(new JScrollPane(buildForm()), BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        installListeners();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.X_AXIS));
        header.add(new JLabel("Nodo: "));
        nodeSelector.setPrototypeDisplayValue("caja-plataformas        ");
        header.add(nodeSelector);
        header.add(Box.createHorizontalStrut(12));
        header.add(reloadButton);
        header.add(Box.createHorizontalGlue());
        header.add(saveButton);
        header.add(Box.createHorizontalStrut(8));
        header.add(provisionButton);
        return header;
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        row = addComponent(form, c, row, "Funciones", rolesPanel);
        row = addField(form, c, row, "Perfil", profile,
                "Metadato operativo; no cambia la identidad del producto");
        row = addField(form, c, row, "Servidor BBDD (override)", dbServer,
                "Vacío = heredar del MASTER");
        row = addField(form, c, row, "Puerto BBDD (override)", dbPort,
                "Vacío = heredar del MASTER");
        row = addField(form, c, row, "Base de datos", dbName,
                "Nombre/esquema asignado a este nodo");

        row = addValue(form, c, row, "Servidor efectivo", effectiveServer);
        row = addValue(form, c, row, "Puerto efectivo", effectivePort);
        row = addValue(form, c, row, "BBDD efectiva", effectiveDatabase);
        row = addValue(form, c, row, "MASTER", masterPath);
        row = addValue(form, c, row, "Módulos (orden)", modulePath);
        row = addValue(form, c, row, "Bootstrap", bootstrapPath);
        row = addValue(form, c, row, "Credenciales",
                new JLabel("Gestionadas por MASTER / secrets.properties"));

        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 2;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        form.add(Box.createVerticalGlue(), c);
        return form;
    }

    private void initializeRoleSelector() {
        rolesPanel.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        for (NodeRole role : NodeRole.values()) {
            JCheckBox check = new JCheckBox(roleLabel(role));
            check.setToolTipText("node.roles=" + role.getPropertyValue());
            check.addActionListener(e -> markDirty());
            roleChecks.put(role, check);
            rolesPanel.add(check);
        }
    }

    private String roleLabel(NodeRole role) {
        switch (role) {
            case MASTER: return "MASTER";
            case SERVER: return "Servidor configuración";
            case POS: return "Punto de venta";
            case KITCHEN: return "Cocina";
            case PRINTER_SERVICE: return "Servicio impresión";
            case SCALE_SERVICE: return "Servicio báscula";
            case SCANNER_SERVICE: return "Servicio scanner";
            case DISPLAY_SERVICE: return "Servicio visor";
            case REMOTE_SESSIONS: return "Sesiones remotas";
            default: return role.getPropertyValue();
        }
    }

    private int addComponent(JPanel panel, GridBagConstraints c, int row,
            String label, Component component) {
        c.gridy = row;
        c.gridx = 0;
        c.gridwidth = 1;
        c.weightx = 0.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel(label + ":"), c);
        c.gridx = 1;
        c.weightx = 1.0;
        panel.add(component, c);
        return row + 1;
    }

    private int addField(JPanel panel, GridBagConstraints c, int row,
            String label, JTextField field, String tooltip) {
        c.gridy = row;
        c.gridx = 0;
        c.gridwidth = 1;
        c.weightx = 0.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel(label + ":"), c);
        c.gridx = 1;
        c.weightx = 1.0;
        field.setColumns(32);
        field.setToolTipText(tooltip);
        panel.add(field, c);
        return row + 1;
    }

    private int addValue(JPanel panel, GridBagConstraints c, int row,
            String label, JLabel value) {
        c.gridy = row;
        c.gridx = 0;
        c.gridwidth = 1;
        c.weightx = 0.0;
        panel.add(new JLabel(label + ":"), c);
        c.gridx = 1;
        c.weightx = 1.0;
        panel.add(value, c);
        return row + 1;
    }

    private void installListeners() {
        DocumentListener listener = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { markDirty(); }
            @Override public void removeUpdate(DocumentEvent e) { markDirty(); }
            @Override public void changedUpdate(DocumentEvent e) { markDirty(); }
        };
        profile.getDocument().addDocumentListener(listener);
        dbServer.getDocument().addDocumentListener(listener);
        dbPort.getDocument().addDocumentListener(listener);
        dbName.getDocument().addDocumentListener(listener);

        nodeSelector.addActionListener(e -> handleNodeSelection());
        reloadButton.addActionListener(e -> reloadAll());
        saveButton.addActionListener(e -> saveFromButton());
        provisionButton.addActionListener(e -> provisionBootstrap());
    }

    private void markDirty() {
        if (!loading) {
            dirty = true;
            status.setText("Cambios pendientes en " + currentNodeId);
        }
    }

    private void handleNodeSelection() {
        if (loading || store == null) {
            return;
        }
        String selected = (String) nodeSelector.getSelectedItem();
        if (selected == null || selected.equals(currentNodeId)) {
            return;
        }
        if (dirty && currentNodeId != null) {
            int result = JOptionPane.showConfirmDialog(this,
                    "Hay cambios sin guardar en " + currentNodeId
                    + ". ¿Guardarlos antes de cambiar de nodo?",
                    "KylGis POS - Nodos",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (result == JOptionPane.CANCEL_OPTION || result == JOptionPane.CLOSED_OPTION) {
                loading = true;
                nodeSelector.setSelectedItem(currentNodeId);
                loading = false;
                return;
            }
            if (result == JOptionPane.YES_OPTION && !saveCurrentNode(true)) {
                loading = true;
                nodeSelector.setSelectedItem(currentNodeId);
                loading = false;
                return;
            }
        }
        loadNode(selected);
    }

    private void reloadAll() {
        if (store == null) {
            return;
        }
        try {
            loadNodeList(currentNodeId);
            status.setText("Configuración recargada desde disco");
        } catch (IOException ex) {
            showError("No fue posible recargar los nodos", ex);
        }
    }

    private void saveFromButton() {
        saveCurrentNode(false);
    }

    private boolean saveCurrentNode(boolean silentSuccess) {
        if (store == null || currentNodeId == null) {
            return true;
        }
        if (!administrationEnabled) {
            status.setText("Modo consulta: este nodo no tiene rol master");
            return false;
        }
        try {
            validateFields();
            String savedNodeId = currentNodeId;
            String savedRoles = selectedRoles();
            store.saveNode(savedNodeId, savedRoles, profile.getText(),
                    dbServer.getText(), dbPort.getText(), dbName.getText());
            loadNode(savedNodeId);
            if (savedNodeId.equals(runtimeNodeId) && !containsMasterRole(savedRoles)) {
                setAdministrationEnabled(false);
            }
            if (!silentSuccess) {
                status.setText("Nodo guardado: " + savedNodeId);
            }
            return true;
        } catch (IOException | IllegalArgumentException ex) {
            showError("No fue posible guardar el nodo", ex);
            return false;
        }
    }

    private void validateFields() {
        if (selectedRoles().isEmpty()) {
            throw new IllegalArgumentException("El nodo debe tener al menos una función");
        }
        String port = dbPort.getText().trim();
        if (!port.isEmpty()) {
            int number;
            try {
                number = Integer.parseInt(port);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("El puerto de BBDD debe ser numérico");
            }
            if (number < 1 || number > 65535) {
                throw new IllegalArgumentException("Puerto BBDD fuera de rango: " + number);
            }
        }
    }

    private void provisionBootstrap() {
        if (store == null || currentNodeId == null) {
            return;
        }
        if (!administrationEnabled) {
            status.setText("Modo consulta: este nodo no tiene rol master");
            return;
        }
        try {
            File target = store.getBootstrapFile(currentNodeId);
            boolean overwrite = false;
            if (target.exists()) {
                int result = JOptionPane.showConfirmDialog(this,
                        "El bootstrap ya existe. ¿Recrearlo?\n" + target.getAbsolutePath(),
                        "KylGis POS - Aprovisionamiento",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);
                if (result != JOptionPane.YES_OPTION) {
                    return;
                }
                overwrite = true;
            }
            File generated = store.provisionBootstrap(currentNodeId, overwrite);
            bootstrapPath.setText(generated.getAbsolutePath());
            status.setText("Bootstrap listo para " + currentNodeId);
        } catch (IOException | IllegalArgumentException ex) {
            showError("No fue posible aprovisionar el nodo", ex);
        }
    }

    private void loadNodeList(String preferredNode) throws IOException {
        List<String> nodes = store.listNodeIds();
        loading = true;
        try {
            nodeSelector.removeAllItems();
            for (String node : nodes) {
                nodeSelector.addItem(node);
            }
            if (preferredNode != null && nodes.contains(preferredNode)) {
                nodeSelector.setSelectedItem(preferredNode);
            } else if (!nodes.isEmpty()) {
                nodeSelector.setSelectedIndex(0);
            }
        } finally {
            loading = false;
        }
        String selected = (String) nodeSelector.getSelectedItem();
        if (selected != null) {
            loadNode(selected);
        } else {
            clearNode();
            status.setText("El MASTER no contiene nodos registrados");
        }
    }

    private void loadNode(String nodeId) {
        try {
            NodeDefinition node = store.loadNode(nodeId);
            loading = true;
            try {
                currentNodeId = node.getNodeId();
                setSelectedRoles(node.getRoles());
                profile.setText(node.getProfile());
                dbServer.setText(node.getDatabaseServerOverride());
                dbPort.setText(node.getDatabasePortOverride());
                dbName.setText(node.getDatabaseNameOverride());
                effectiveServer.setText(valueOrInherited(node.getEffectiveDatabaseServer()));
                effectivePort.setText(valueOrInherited(node.getEffectiveDatabasePort()));
                effectiveDatabase.setText(valueOrInherited(node.getEffectiveDatabaseName()));
                masterPath.setText(store.getMasterFile().getAbsolutePath());
                modulePath.setText(formatModulePaths(node.getModuleFiles()));
                bootstrapPath.setText(store.getBootstrapFile(nodeId).getAbsolutePath());
                nodeSelector.setSelectedItem(nodeId);
                dirty = false;
                status.setText("Nodo cargado: " + nodeId);
            } finally {
                loading = false;
            }
        } catch (IOException | IllegalArgumentException ex) {
            showError("No fue posible cargar el nodo " + nodeId, ex);
        }
    }

    private void clearNode() {
        loading = true;
        try {
            currentNodeId = null;
            setSelectedRoles("");
            profile.setText("");
            dbServer.setText("");
            dbPort.setText("");
            dbName.setText("");
            effectiveServer.setText("-");
            effectivePort.setText("-");
            effectiveDatabase.setText("-");
            modulePath.setText("-");
            bootstrapPath.setText("-");
            dirty = false;
        } finally {
            loading = false;
        }
    }

    private String formatModulePaths(List<File> files) {
        if (files == null || files.isEmpty()) {
            return "-";
        }
        StringBuilder result = new StringBuilder("<html>");
        for (int i = 0; i < files.size(); i++) {
            if (i > 0) {
                result.append(" &rarr; ");
            }
            result.append(files.get(i).getAbsolutePath());
        }
        result.append("</html>");
        return result.toString();
    }

    private String valueOrInherited(String value) {
        return value == null || value.trim().isEmpty() ? "(sin definir)" : value;
    }

    private void setAdministrationEnabled(boolean enabled) {
        administrationEnabled = enabled;
        for (JCheckBox check : roleChecks.values()) check.setEnabled(enabled);
        profile.setEnabled(enabled);
        dbServer.setEnabled(enabled);
        dbPort.setEnabled(enabled);
        dbName.setEnabled(enabled);
        saveButton.setEnabled(enabled);
        provisionButton.setEnabled(enabled);
    }

    private String selectedRoles() {
        StringBuilder result = new StringBuilder();
        for (NodeRole role : NodeRole.values()) {
            JCheckBox check = roleChecks.get(role);
            if (check != null && check.isSelected()) {
                if (result.length() > 0) result.append(',');
                result.append(role.getPropertyValue());
            }
        }
        return result.toString();
    }

    private void setSelectedRoles(String roleList) {
        EnumSet<NodeRole> selected = EnumSet.noneOf(NodeRole.class);
        if (roleList != null && !roleList.trim().isEmpty()) {
            for (String token : roleList.split("[,;\\s]+")) {
                String value = token == null ? "" : token.trim();
                if (value.isEmpty()) continue;
                NodeRole role = NodeRole.fromPropertyValue(value);
                if (role == null) {
                    throw new IllegalArgumentException("Rol de nodo desconocido: " + value);
                }
                selected.add(role);
            }
        }
        for (Map.Entry<NodeRole, JCheckBox> entry : roleChecks.entrySet()) {
            entry.getValue().setSelected(selected.contains(entry.getKey()));
        }
    }

    private boolean containsMasterRole(String roleList) {
        if (roleList == null) {
            return false;
        }
        for (String token : roleList.split("[,;\\s]+")) {
            NodeRole role = NodeRole.fromPropertyValue(token);
            if (role == NodeRole.MASTER) {
                return true;
            }
        }
        return false;
    }

    private void showError(String message, Exception ex) {
        status.setText(message + ": " + ex.getMessage());
        JOptionPane.showMessageDialog(this,
                message + "\n" + ex.getMessage(),
                "KylGis POS - Nodos",
                JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void loadProperties(AppConfig config) {
        NodeContext context = NodeContext.from(config);
        runtimeNodeId = context.getNodeId();
        setAdministrationEnabled(context.isMasterNode() && !config.isRemoteProvisioned());

        if (config.isRemoteProvisioned()) {
            loadRemoteCurrentNode(config, context);
            return;
        }

        store = NodeConfigurationStore.forConfig(config);
        masterPath.setText(store.getMasterFile().getAbsolutePath());
        try {
            loadNodeList(currentNodeId);
            if (!administrationEnabled) {
                status.setText("Modo consulta: " + runtimeNodeId + " no tiene rol master");
            }
        } catch (IOException ex) {
            clearNode();
            status.setText("MASTER no disponible: " + ex.getMessage());
        }
    }

    private void loadRemoteCurrentNode(AppConfig config, NodeContext context) {
        store = null;
        loading = true;
        try {
            nodeSelector.removeAllItems();
            nodeSelector.addItem(context.getNodeId());
            nodeSelector.setSelectedItem(context.getNodeId());
            currentNodeId = context.getNodeId();
            setSelectedRoles(joinRoles(context));
            profile.setText(value(config.getProperty("node.profile")));
            dbServer.setText("");
            dbPort.setText("");
            dbName.setText("");
            effectiveServer.setText(valueOrInherited(config.getProperty("database.server")));
            effectivePort.setText(valueOrInherited(config.getProperty("database.port")));
            effectiveDatabase.setText(valueOrInherited(config.getProperty("database.name")));
            masterPath.setText(config.isRemoteCacheFallback()
                    ? "MASTER remoto (offline: cache cifrado)" : "MASTER remoto");
            modulePath.setText("Gestionados por MASTER remoto");
            bootstrapPath.setText(config.getConfigFile().getAbsolutePath());
            dirty = false;
            status.setText(config.isRemoteCacheFallback()
                    ? "Modo consulta: MASTER remoto no disponible; usando última configuración válida"
                    : "Modo consulta: configuración recibida desde MASTER remoto");
        } finally {
            loading = false;
        }
    }

    private static String joinRoles(NodeContext context) {
        StringBuilder result = new StringBuilder();
        for (NodeRole role : context.getRoles()) {
            if (result.length() > 0) result.append(',');
            result.append(role.getPropertyValue());
        }
        return result.toString();
    }

    private static String value(String text) {
        return text == null ? "" : text.trim();
    }

    @Override
    public void saveProperties(AppConfig config) {
        if (dirty && !saveCurrentNode(true)) {
            throw new IllegalStateException("No se pudo guardar el módulo del nodo " + currentNodeId);
        }
    }

    @Override
    public boolean hasChanged() {
        return dirty;
    }

    @Override
    public Component getConfigComponent() {
        return this;
    }
}
