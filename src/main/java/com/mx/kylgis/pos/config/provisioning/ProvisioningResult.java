//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
//
//    This file is part of KylGis POS.
package com.mx.kylgis.pos.config.provisioning;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Immutable snapshot of the configuration layers used to provision one node.
 */
public final class ProvisioningResult {

    private final boolean provisioned;
    private final File localFile;
    private final File masterFile;
    private final List<File> nodeModuleFiles;
    private final File secretsFile;
    private final Properties localProperties;
    private final Properties masterProperties;
    private final List<Properties> nodeModuleProperties;
    private final Properties rawEffectiveProperties;
    private final Properties effectiveProperties;

    ProvisioningResult(boolean provisioned, File localFile, File masterFile,
            List<File> nodeModuleFiles, File secretsFile, Properties localProperties,
            Properties masterProperties, List<Properties> nodeModuleProperties,
            Properties rawEffectiveProperties, Properties effectiveProperties) {
        this.provisioned = provisioned;
        this.localFile = localFile;
        this.masterFile = masterFile;
        this.nodeModuleFiles = copyFiles(nodeModuleFiles);
        this.secretsFile = secretsFile;
        this.localProperties = copy(localProperties);
        this.masterProperties = copy(masterProperties);
        this.nodeModuleProperties = copyPropertiesList(nodeModuleProperties);
        this.rawEffectiveProperties = copy(rawEffectiveProperties);
        this.effectiveProperties = copy(effectiveProperties);
    }

    public boolean isProvisioned() {
        return provisioned;
    }

    public File getLocalFile() {
        return localFile;
    }

    public File getMasterFile() {
        return masterFile;
    }

    /**
     * Returns the writable node overlay. With a module stack this is the last
     * module, preserving the legacy single-module contract.
     */
    public File getNodeModuleFile() {
        return nodeModuleFiles.isEmpty() ? null
                : nodeModuleFiles.get(nodeModuleFiles.size() - 1);
    }

    public List<File> getNodeModuleFiles() {
        return Collections.unmodifiableList(new ArrayList<>(nodeModuleFiles));
    }

    public File getSecretsFile() {
        return secretsFile;
    }

    public Properties getLocalProperties() {
        return copy(localProperties);
    }

    public Properties getMasterProperties() {
        return copy(masterProperties);
    }

    /**
     * Returns the writable node overlay properties (last module in the stack).
     */
    public Properties getNodeProperties() {
        return nodeModuleProperties.isEmpty() ? new Properties()
                : copy(nodeModuleProperties.get(nodeModuleProperties.size() - 1));
    }

    public List<Properties> getNodeModuleProperties() {
        return copyPropertiesList(nodeModuleProperties);
    }

    public Properties getMergedNodeProperties() {
        Properties merged = new Properties();
        for (Properties module : nodeModuleProperties) {
            merged.putAll(module);
        }
        return merged;
    }

    public Properties getRawEffectiveProperties() {
        return copy(rawEffectiveProperties);
    }

    public Properties getEffectiveProperties() {
        return copy(effectiveProperties);
    }

    private static List<File> copyFiles(List<File> source) {
        List<File> copy = new ArrayList<>();
        if (source != null) {
            copy.addAll(source);
        }
        return copy;
    }

    private static List<Properties> copyPropertiesList(List<Properties> source) {
        List<Properties> copy = new ArrayList<>();
        if (source != null) {
            for (Properties item : source) {
                copy.add(copy(item));
            }
        }
        return copy;
    }

    private static Properties copy(Properties source) {
        Properties copy = new Properties();
        if (source != null) {
            copy.putAll(source);
        }
        return copy;
    }
}
