//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
//
//    This file is part of KylGis POS.
package com.mx.kylgis.pos.config.provisioning;

import java.io.File;
import java.util.Properties;

/**
 * Immutable snapshot of the configuration layers used to provision one node.
 */
public final class ProvisioningResult {

    private final boolean provisioned;
    private final File localFile;
    private final File masterFile;
    private final File nodeModuleFile;
    private final Properties localProperties;
    private final Properties masterProperties;
    private final Properties nodeProperties;
    private final Properties effectiveProperties;

    ProvisioningResult(boolean provisioned, File localFile, File masterFile,
            File nodeModuleFile, Properties localProperties,
            Properties masterProperties, Properties nodeProperties,
            Properties effectiveProperties) {
        this.provisioned = provisioned;
        this.localFile = localFile;
        this.masterFile = masterFile;
        this.nodeModuleFile = nodeModuleFile;
        this.localProperties = copy(localProperties);
        this.masterProperties = copy(masterProperties);
        this.nodeProperties = copy(nodeProperties);
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

    public File getNodeModuleFile() {
        return nodeModuleFile;
    }

    public Properties getLocalProperties() {
        return copy(localProperties);
    }

    public Properties getMasterProperties() {
        return copy(masterProperties);
    }

    public Properties getNodeProperties() {
        return copy(nodeProperties);
    }

    public Properties getEffectiveProperties() {
        return copy(effectiveProperties);
    }

    private static Properties copy(Properties source) {
        Properties copy = new Properties();
        if (source != null) {
            copy.putAll(source);
        }
        return copy;
    }
}
