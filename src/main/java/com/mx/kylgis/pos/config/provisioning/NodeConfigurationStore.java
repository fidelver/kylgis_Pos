//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.config.provisioning;

import com.mx.kylgis.pos.forms.AppConfig;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Storage facade used by the node administration UI.
 *
 * It edits only node modules. MASTER ownership and secrets stay separate.
 */
public final class NodeConfigurationStore {

    private static final String DEFAULT_WORKSPACE = "00-MultiPlataforma";

    private final File masterFile;
    private final File bootstrapDirectory;

    public NodeConfigurationStore(File masterFile, File bootstrapDirectory) {
        if (masterFile == null || bootstrapDirectory == null) {
            throw new IllegalArgumentException("masterFile and bootstrapDirectory are required");
        }
        this.masterFile = masterFile.getAbsoluteFile();
        this.bootstrapDirectory = bootstrapDirectory.getAbsoluteFile();
    }

    public static NodeConfigurationStore forConfig(AppConfig config) {
        if (config != null && config.isProvisioned() && config.getMasterConfigFile() != null) {
            File master = config.getMasterConfigFile().getAbsoluteFile();
            File configDir = master.getParentFile() == null
                    ? null : master.getParentFile().getParentFile();
            File bootstrap = configDir == null
                    ? defaultBootstrapDirectory()
                    : new File(configDir, "bootstrap");
            return new NodeConfigurationStore(master, bootstrap);
        }
        return new NodeConfigurationStore(defaultMasterFile(), defaultBootstrapDirectory());
    }

    public static File defaultWorkspace() {
        return new File(System.getProperty("user.home"), DEFAULT_WORKSPACE);
    }

    public static File defaultMasterFile() {
        return new File(new File(defaultWorkspace(), "config/master"),
                "kylgis-master.properties");
    }

    public static File defaultBootstrapDirectory() {
        return new File(defaultWorkspace(), "config/bootstrap");
    }

    public List<String> listNodeIds() throws IOException {
        Properties master = loadRequired(masterFile, "MASTER");
        List<String> result = new ArrayList<>();
        for (String key : master.stringPropertyNames()) {
            if (key.startsWith(NodeProvisioner.NODE_MODULE_PREFIX)
                    && key.endsWith(NodeProvisioner.NODE_MODULE_SUFFIX)) {
                String id = key.substring(NodeProvisioner.NODE_MODULE_PREFIX.length(),
                        key.length() - NodeProvisioner.NODE_MODULE_SUFFIX.length());
                if (!id.trim().isEmpty()) {
                    result.add(id);
                }
            }
        }
        Collections.sort(result);
        return result;
    }

    public NodeDefinition loadNode(String nodeId) throws IOException {
        String id = normalizeNodeId(nodeId);
        Properties master = loadRequired(masterFile, "MASTER");
        File moduleFile = resolveModuleFile(master, id);
        Properties module = loadRequired(moduleFile, "node module");
        Properties effective = new Properties();
        effective.putAll(master);
        effective.putAll(module);

        return new NodeDefinition(id, moduleFile,
                value(module, "node.roles"),
                value(module, "node.profile"),
                value(module, "database.server"),
                value(module, "database.port"),
                value(module, "database.name"),
                value(effective, "database.server"),
                value(effective, "database.port"),
                value(effective, "database.name"));
    }

    public void saveNode(String nodeId, String roles, String profile,
            String databaseServerOverride, String databasePortOverride,
            String databaseNameOverride) throws IOException {
        String id = normalizeNodeId(nodeId);
        Properties master = loadRequired(masterFile, "MASTER");
        File moduleFile = resolveModuleFile(master, id);
        Properties module = loadRequired(moduleFile, "node module");

        putOrRemove(module, "node.roles", roles);
        putOrRemove(module, "node.profile", profile);
        putOrRemove(module, "database.server", databaseServerOverride);
        putOrRemove(module, "database.port", databasePortOverride);
        putOrRemove(module, "database.name", databaseNameOverride);
        atomicStore(moduleFile, module, "KylGis POS node module: " + id);
    }

    public File provisionBootstrap(String nodeId, boolean overwrite) throws IOException {
        String id = normalizeNodeId(nodeId);
        if (!bootstrapDirectory.isDirectory() && !bootstrapDirectory.mkdirs()) {
            throw new IOException("Cannot create bootstrap directory: " + bootstrapDirectory);
        }
        File bootstrap = new File(bootstrapDirectory, id + ".properties");
        NodeProvisioner.provisionBootstrap(bootstrap, masterFile, id, overwrite);
        return bootstrap;
    }

    public File getMasterFile() {
        return masterFile;
    }

    public File getBootstrapDirectory() {
        return bootstrapDirectory;
    }

    public File getBootstrapFile(String nodeId) {
        return new File(bootstrapDirectory, normalizeNodeId(nodeId) + ".properties");
    }

    private File resolveModuleFile(Properties master, String nodeId) throws IOException {
        String key = NodeProvisioner.nodeModuleKey(nodeId);
        String reference = trimToNull(master.getProperty(key));
        if (reference == null) {
            throw new IOException("MASTER does not define " + key);
        }
        File candidate = new File(reference);
        return candidate.isAbsolute() ? candidate
                : new File(masterFile.getParentFile(), reference).getAbsoluteFile();
    }

    private static Properties loadRequired(File file, String label) throws IOException {
        if (file == null || !file.isFile()) {
            throw new IOException("KylGis " + label + " does not exist: "
                    + (file == null ? "null" : file.getAbsolutePath()));
        }
        Properties result = new Properties();
        try (InputStream in = new FileInputStream(file)) {
            result.load(in);
        }
        return result;
    }

    private static void atomicStore(File file, Properties properties, String comment)
            throws IOException {
        File parent = file.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Cannot create node module directory: " + parent);
        }
        File temp = File.createTempFile(file.getName() + ".", ".tmp", parent);
        boolean moved = false;
        try {
            try (OutputStream out = new FileOutputStream(temp)) {
                properties.store(out, comment);
            }
            try {
                Files.move(temp.toPath(), file.toPath(),
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temp.toPath(), file.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
            moved = true;
        } finally {
            if (!moved) {
                temp.delete();
            }
        }
    }

    private static void putOrRemove(Properties properties, String key, String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            properties.remove(key);
        } else {
            properties.setProperty(key, normalized);
        }
    }

    private static String value(Properties properties, String key) {
        String value = properties.getProperty(key);
        return value == null ? "" : value.trim();
    }

    private static String normalizeNodeId(String nodeId) {
        String value = trimToNull(nodeId);
        if (value == null) {
            throw new IllegalArgumentException("nodeId is required");
        }
        return value;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static final class NodeDefinition {
        private final String nodeId;
        private final File moduleFile;
        private final String roles;
        private final String profile;
        private final String databaseServerOverride;
        private final String databasePortOverride;
        private final String databaseNameOverride;
        private final String effectiveDatabaseServer;
        private final String effectiveDatabasePort;
        private final String effectiveDatabaseName;

        private NodeDefinition(String nodeId, File moduleFile, String roles,
                String profile, String databaseServerOverride,
                String databasePortOverride, String databaseNameOverride,
                String effectiveDatabaseServer, String effectiveDatabasePort,
                String effectiveDatabaseName) {
            this.nodeId = nodeId;
            this.moduleFile = moduleFile;
            this.roles = roles;
            this.profile = profile;
            this.databaseServerOverride = databaseServerOverride;
            this.databasePortOverride = databasePortOverride;
            this.databaseNameOverride = databaseNameOverride;
            this.effectiveDatabaseServer = effectiveDatabaseServer;
            this.effectiveDatabasePort = effectiveDatabasePort;
            this.effectiveDatabaseName = effectiveDatabaseName;
        }

        public String getNodeId() { return nodeId; }
        public File getModuleFile() { return moduleFile; }
        public String getRoles() { return roles; }
        public String getProfile() { return profile; }
        public String getDatabaseServerOverride() { return databaseServerOverride; }
        public String getDatabasePortOverride() { return databasePortOverride; }
        public String getDatabaseNameOverride() { return databaseNameOverride; }
        public String getEffectiveDatabaseServer() { return effectiveDatabaseServer; }
        public String getEffectiveDatabasePort() { return effectiveDatabasePort; }
        public String getEffectiveDatabaseName() { return effectiveDatabaseName; }
    }
}
