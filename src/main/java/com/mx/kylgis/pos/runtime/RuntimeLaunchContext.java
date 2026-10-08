//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.runtime;

import com.mx.kylgis.pos.forms.AppConfig;
import com.mx.kylgis.pos.node.NodeContext;

/** Immutable launch context shared by all capabilities of one node. */
public final class RuntimeLaunchContext {
    private final AppConfig config;
    private final NodeContext nodeContext;

    public RuntimeLaunchContext(AppConfig config, NodeContext nodeContext) {
        if (config == null || nodeContext == null) {
            throw new IllegalArgumentException("config and nodeContext are required");
        }
        this.config = config;
        this.nodeContext = nodeContext;
    }

    public AppConfig getConfig() { return config; }
    public NodeContext getNodeContext() { return nodeContext; }
}
