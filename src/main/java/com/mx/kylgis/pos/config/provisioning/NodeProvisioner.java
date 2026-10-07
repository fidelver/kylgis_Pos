//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
//
//    This file is part of KylGis POS.
package com.mx.kylgis.pos.config.provisioning;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Resolves KylGis configuration layers for one node.
 *
 * Legacy mode (no config.master): DEFAULT + LOCAL.
 * Provisioned mode: DEFAULT + MASTER + NODE MODULE + LOCAL BOOTSTRAP.
 *
 * The local file remains the bootstrap identity of the node. The MASTER owns
 * the topology and points to the module associated with each node.
 */
public final class NodeProvisioner {

    public static final String MASTER_KEY = "config.master";
    public static final String NODE_ID_KEY = "node.id";
    public static final String NODE_MODULE_PREFIX = "node.";
    public static final String NODE_MODULE_SUFFIX = ".module";
    public static final String SECRETS_KEY = "config.secrets";

    private NodeProvisioner() {
    }

    public static ProvisioningResult resolve(File localFile, Properties defaults)
            throws IOException {
        if (localFile == null) {
            throw new IllegalArgumentException("localFile is required");
        }

        Properties local = loadIfPresent(localFile);
        String masterReference = trimToNull(local.getProperty(MASTER_KEY));

        if (masterReference == null) {
            Properties effective = merge(defaults, local);
            return new ProvisioningResult(false, localFile, null, null, null,
                    local, new Properties(), new Properties(), effective, effective);
        }

        if (!localFile.isFile()) {
            throw new IOException("Provisioned node bootstrap does not exist: "
                    + localFile.getAbsolutePath());
        }

        String nodeId = trimToNull(local.getProperty(NODE_ID_KEY));
        if (nodeId == null) {
            throw new IOException("Provisioned node requires explicit " + NODE_ID_KEY
                    + " in " + localFile.getAbsolutePath());
        }

        File masterFile = resolveRelative(localFile.getParentFile(), masterReference);
        Properties master = loadRequired(masterFile, "MASTER");

        String moduleKey = nodeModuleKey(nodeId);
        String moduleReference = trimToNull(master.getProperty(moduleKey));
        if (moduleReference == null) {
            throw new IOException("MASTER does not define " + moduleKey
                    + " for node " + nodeId);
        }

        File nodeModuleFile = resolveRelative(masterFile.getParentFile(), moduleReference);
        Properties nodeModule = loadRequired(nodeModuleFile, "node module");

        Properties rawEffective = merge(defaults, master, nodeModule, local);
        File secretsFile = null;
        Properties effective = rawEffective;
        if (SecretResolver.containsSecretReferences(rawEffective)) {
            String secretsReference = trimToNull(master.getProperty(SECRETS_KEY));
            if (secretsReference == null) {
                throw new IOException("MASTER must define " + SECRETS_KEY
                        + " because the effective configuration contains !secret references");
            }
            secretsFile = resolveRelative(masterFile.getParentFile(), secretsReference);
            Properties secrets = loadRequired(secretsFile, "secrets");
            effective = SecretResolver.resolve(rawEffective, secrets);
        }

        return new ProvisioningResult(true, localFile, masterFile, nodeModuleFile,
                secretsFile, local, master, nodeModule, rawEffective, effective);
    }

    public static void provisionBootstrap(File localFile, File masterFile,
            String nodeId, boolean overwrite) throws IOException {
        if (localFile == null || masterFile == null) {
            throw new IllegalArgumentException("localFile and masterFile are required");
        }
        String normalizedNodeId = trimToNull(nodeId);
        if (normalizedNodeId == null) {
            throw new IllegalArgumentException("nodeId is required");
        }
        if (localFile.exists() && !overwrite) {
            throw new IOException("Bootstrap already exists: " + localFile.getAbsolutePath());
        }

        Properties master = loadRequired(masterFile, "MASTER");
        String moduleReference = trimToNull(master.getProperty(nodeModuleKey(normalizedNodeId)));
        if (moduleReference == null) {
            throw new IOException("MASTER does not define node module for " + normalizedNodeId);
        }
        File nodeModule = resolveRelative(masterFile.getParentFile(), moduleReference);
        loadRequired(nodeModule, "node module");

        File parent = localFile.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Cannot create bootstrap directory: " + parent);
        }

        Properties bootstrap = new Properties();
        bootstrap.setProperty(NODE_ID_KEY, normalizedNodeId);
        bootstrap.setProperty(MASTER_KEY, relativizeIfPossible(parent, masterFile));

        try (OutputStream out = new FileOutputStream(localFile)) {
            bootstrap.store(out, "KylGis POS node bootstrap. Managed by provisioning.");
        }
    }

    public static String nodeModuleKey(String nodeId) {
        return NODE_MODULE_PREFIX + nodeId + NODE_MODULE_SUFFIX;
    }

    private static Properties loadIfPresent(File file) throws IOException {
        if (!file.isFile()) {
            return new Properties();
        }
        return load(file);
    }

    private static Properties loadRequired(File file, String layerName) throws IOException {
        if (!file.isFile()) {
            throw new IOException("KylGis " + layerName + " file does not exist: "
                    + file.getAbsolutePath());
        }
        return load(file);
    }

    private static Properties load(File file) throws IOException {
        Properties properties = new Properties();
        try (InputStream in = new FileInputStream(file)) {
            properties.load(in);
        }
        return properties;
    }

    private static Properties merge(Properties... layers) {
        Properties merged = new Properties();
        if (layers != null) {
            for (Properties layer : layers) {
                if (layer != null) {
                    merged.putAll(layer);
                }
            }
        }
        return merged;
    }

    private static File resolveRelative(File baseDirectory, String reference) {
        File candidate = new File(reference);
        if (candidate.isAbsolute()) {
            return candidate.getAbsoluteFile();
        }
        File base = baseDirectory == null ? new File(".") : baseDirectory;
        return new File(base, reference).getAbsoluteFile();
    }

    private static String relativizeIfPossible(File baseDirectory, File target) {
        if (baseDirectory == null) {
            return target.getAbsolutePath();
        }
        try {
            Path base = baseDirectory.getCanonicalFile().toPath();
            Path destination = target.getCanonicalFile().toPath();
            return base.relativize(destination).toString();
        } catch (IOException | IllegalArgumentException ex) {
            return target.getAbsolutePath();
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
