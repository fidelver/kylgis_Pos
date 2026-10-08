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
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Storage facade used by the node administration UI.
 *
 * It composes ordered node modules and edits only the final node overlay.
 * MASTER ownership, shared modules and secrets stay separate.
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
        Set<String> ids = new LinkedHashSet<>();
        for (String key : master.stringPropertyNames()) {
            String suffix = null;
            if (key.startsWith(NodeProvisioner.NODE_MODULE_PREFIX)
                    && key.endsWith(NodeProvisioner.NODE_MODULES_SUFFIX)) {
                suffix = NodeProvisioner.NODE_MODULES_SUFFIX;
            } else if (key.startsWith(NodeProvisioner.NODE_MODULE_PREFIX)
                    && key.endsWith(NodeProvisioner.NODE_MODULE_SUFFIX)) {
                suffix = NodeProvisioner.NODE_MODULE_SUFFIX;
            }
            if (suffix != null) {
                String id = key.substring(NodeProvisioner.NODE_MODULE_PREFIX.length(),
                        key.length() - suffix.length());
                if (!id.trim().isEmpty()) {
                    ids.add(id);
                }
            }
        }
        List<String> result = new ArrayList<>(ids);
        Collections.sort(result);
        return result;
    }

    public NodeDefinition loadNode(String nodeId) throws IOException {
        String id = normalizeNodeId(nodeId);
        Properties master = loadRequired(masterFile, "MASTER");
        List<String> moduleReferences = NodeProvisioner.nodeModuleReferences(master, id);
        List<File> moduleFiles = resolveModuleFiles(master, id);
        List<Properties> modules = loadModules(moduleFiles);
        Properties effective = new Properties();
        effective.putAll(master);
        for (Properties module : modules) {
            effective.putAll(module);
        }
        Properties writableModule = modules.get(modules.size() - 1);

        return new NodeDefinition(id, moduleFiles,
                moduleReferences.subList(0, moduleReferences.size() - 1),
                value(effective, "node.roles"),
                value(writableModule, "node.roles"),
                value(effective, "node.profile"),
                value(writableModule, "database.server"),
                value(writableModule, "database.port"),
                value(writableModule, "database.name"),
                value(effective, "database.server"),
                value(effective, "database.port"),
                value(effective, "database.name"));
    }

    /** Returns reusable MASTER modules available for assignment to nodes. */
    public List<String> listAvailableModules() throws IOException {
        File moduleRoot = new File(masterFile.getParentFile(), "modules").getCanonicalFile();
        if (!moduleRoot.isDirectory()) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        collectModuleReferences(moduleRoot, result);
        Collections.sort(result);
        return Collections.unmodifiableList(result);
    }

    /**
     * Replaces only the reusable part of one node stack. The mandatory
     * nodes/<nodeId>.properties overlay is preserved as the final layer.
     */
    public void saveModuleStack(String nodeId, List<String> reusableModules)
            throws IOException {
        String id = normalizeNodeId(nodeId);
        Properties master = loadRequired(masterFile, "MASTER");
        List<String> current = NodeProvisioner.nodeModuleReferences(master, id);
        if (current.isEmpty()) throw new IOException("MASTER does not define modules for node " + id);
        // Validate current stack and, critically, its final writable overlay.
        resolveModuleFiles(master, id);
        String overlayReference = current.get(current.size() - 1);

        Set<String> available = new LinkedHashSet<>(listAvailableModules());
        List<String> normalized = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        if (reusableModules != null) {
            for (String reference : reusableModules) {
                String value = normalizeModuleReference(reference);
                if (!available.contains(value)) {
                    throw new IOException("Unknown reusable node module: " + value);
                }
                if (!seen.add(value)) {
                    throw new IOException("Duplicate reusable node module: " + value);
                }
                normalized.add(value);
            }
        }
        normalized.add(overlayReference);
        atomicUpdateMasterModuleStack(masterFile,
                NodeProvisioner.nodeModulesKey(id), join(normalized),
                NodeProvisioner.nodeModuleKey(id));
    }

    public void saveNode(String nodeId, String roles, String profile,
            String databaseServerOverride, String databasePortOverride,
            String databaseNameOverride) throws IOException {
        String id = normalizeNodeId(nodeId);
        Properties master = loadRequired(masterFile, "MASTER");
        List<File> moduleFiles = resolveModuleFiles(master, id);
        File moduleFile = moduleFiles.get(moduleFiles.size() - 1);
        Properties module = loadRequired(moduleFile, "writable node module");

        putOrRemove(module, "node.roles", roles);
        putOrRemove(module, "node.profile", profile);
        putOrRemove(module, "database.server", databaseServerOverride);
        putOrRemove(module, "database.port", databasePortOverride);
        putOrRemove(module, "database.name", databaseNameOverride);
        atomicStore(moduleFile, module, "KylGis POS node overlay: " + id);
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

    private List<File> resolveModuleFiles(Properties master, String nodeId) throws IOException {
        List<String> references = NodeProvisioner.nodeModuleReferences(master, nodeId);
        if (references.isEmpty()) {
            throw new IOException("MASTER does not define modules for node " + nodeId);
        }
        List<File> files = new ArrayList<>();
        for (String reference : references) {
            File candidate = new File(reference);
            File resolved = candidate.isAbsolute() ? candidate
                    : new File(masterFile.getParentFile(), reference);
            files.add(resolved.getCanonicalFile());
        }
        File expectedOverlay = new File(new File(masterFile.getParentFile(), "nodes"),
                nodeId + ".properties").getCanonicalFile();
        File actualOverlay = files.get(files.size() - 1).getCanonicalFile();
        if (!actualOverlay.equals(expectedOverlay)) {
            throw new IOException("Node " + nodeId
                    + " module stack must end with its private overlay: "
                    + expectedOverlay.getAbsolutePath());
        }
        return files;
    }

    private void collectModuleReferences(File directory,
            List<String> result) throws IOException {
        File[] children = directory.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (Files.isSymbolicLink(child.toPath())) continue;
            if (child.isDirectory()) {
                collectModuleReferences(child, result);
            } else if (child.isFile() && child.getName().endsWith(".properties")) {
                String relative = masterFile.getParentFile().getCanonicalFile().toPath()
                        .relativize(child.getCanonicalFile().toPath()).toString()
                        .replace(File.separatorChar, '/');
                result.add(normalizeModuleReference(relative));
            }
        }
    }

    private static String normalizeModuleReference(String reference) {
        String value = trimToNull(reference);
        if (value == null) throw new IllegalArgumentException("module reference is required");
        value = value.replace('\\', '/');
        if (!value.startsWith("modules/") || value.startsWith("/")
                || value.contains("../") || value.equals("modules/..")) {
            throw new IllegalArgumentException("Invalid reusable module reference: " + value);
        }
        return value;
    }

    private static String join(List<String> values) {
        StringBuilder result = new StringBuilder();
        for (String value : values) {
            if (result.length() > 0) result.append(',');
            result.append(value);
        }
        return result.toString();
    }

    private static List<Properties> loadModules(List<File> files) throws IOException {
        List<Properties> modules = new ArrayList<>();
        for (File file : files) {
            modules.add(loadRequired(file, "node module"));
        }
        return modules;
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

    /** Updates one MASTER property without rewriting comments or unrelated lines. */
    private static void atomicUpdateMasterModuleStack(File file, String key,
            String value, String legacyKey) throws IOException {
        byte[] original = Files.readAllBytes(file.toPath());
        String text = new String(original, StandardCharsets.ISO_8859_1);
        String newline = text.contains("\r\n") ? "\r\n" : "\n";
        boolean finalNewline = text.endsWith("\n") || text.endsWith("\r");
        String[] lines = text.split("\r?\n", -1);
        List<String> output = new ArrayList<>();
        boolean written = false;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (i == lines.length - 1 && line.isEmpty() && finalNewline) continue;
            if (isPropertyLine(line, legacyKey)) continue;
            if (isPropertyLine(line, key)) {
                if (!written) {
                    output.add(key + "=" + value);
                    written = true;
                }
                continue;
            }
            output.add(line);
        }
        if (!written) output.add(key + "=" + value);

        StringBuilder updated = new StringBuilder(text.length() + 128);
        for (int i = 0; i < output.size(); i++) {
            if (i > 0) updated.append(newline);
            updated.append(output.get(i));
        }
        if (finalNewline) updated.append(newline);
        atomicStoreBytes(file, updated.toString().getBytes(StandardCharsets.ISO_8859_1));
    }

    private static boolean isPropertyLine(String line, String key) {
        if (line == null || key == null) return false;
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) return false;
        if (!trimmed.startsWith(key)) return false;
        if (trimmed.length() == key.length()) return true;
        char separator = trimmed.charAt(key.length());
        return separator == '=' || separator == ':' || Character.isWhitespace(separator);
    }

    private static void atomicStoreBytes(File file, byte[] content) throws IOException {
        File parent = file.getAbsoluteFile().getParentFile();
        File temp = File.createTempFile(file.getName() + ".", ".tmp", parent);
        boolean moved = false;
        try {
            try (OutputStream out = new FileOutputStream(temp)) {
                out.write(content);
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
            if (!moved) temp.delete();
        }
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
        private final List<File> moduleFiles;
        private final List<String> reusableModuleReferences;
        private final String roles;
        private final String rolesOverride;
        private final String profile;
        private final String databaseServerOverride;
        private final String databasePortOverride;
        private final String databaseNameOverride;
        private final String effectiveDatabaseServer;
        private final String effectiveDatabasePort;
        private final String effectiveDatabaseName;

        private NodeDefinition(String nodeId, List<File> moduleFiles,
                List<String> reusableModuleReferences, String roles,
                String rolesOverride, String profile, String databaseServerOverride,
                String databasePortOverride, String databaseNameOverride,
                String effectiveDatabaseServer, String effectiveDatabasePort,
                String effectiveDatabaseName) {
            this.nodeId = nodeId;
            this.moduleFiles = Collections.unmodifiableList(new ArrayList<>(moduleFiles));
            this.reusableModuleReferences = Collections.unmodifiableList(
                    new ArrayList<>(reusableModuleReferences));
            this.roles = roles;
            this.rolesOverride = rolesOverride;
            this.profile = profile;
            this.databaseServerOverride = databaseServerOverride;
            this.databasePortOverride = databasePortOverride;
            this.databaseNameOverride = databaseNameOverride;
            this.effectiveDatabaseServer = effectiveDatabaseServer;
            this.effectiveDatabasePort = effectiveDatabasePort;
            this.effectiveDatabaseName = effectiveDatabaseName;
        }

        public String getNodeId() { return nodeId; }
        public File getModuleFile() {
            return moduleFiles.get(moduleFiles.size() - 1);
        }
        public List<File> getModuleFiles() { return moduleFiles; }
        public List<String> getReusableModuleReferences() { return reusableModuleReferences; }
        public String getRoles() { return roles; }
        public String getRolesOverride() { return rolesOverride; }
        public String getProfile() { return profile; }
        public String getDatabaseServerOverride() { return databaseServerOverride; }
        public String getDatabasePortOverride() { return databasePortOverride; }
        public String getDatabaseNameOverride() { return databaseNameOverride; }
        public String getEffectiveDatabaseServer() { return effectiveDatabaseServer; }
        public String getEffectiveDatabasePort() { return effectiveDatabasePort; }
        public String getEffectiveDatabaseName() { return effectiveDatabaseName; }
    }
}
