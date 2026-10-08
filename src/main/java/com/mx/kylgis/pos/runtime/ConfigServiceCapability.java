//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.runtime;

import com.mx.kylgis.pos.config.remote.ConfigServiceConfig;
import com.mx.kylgis.pos.config.remote.RemoteConfigServer;
import com.mx.kylgis.pos.forms.AppConfig;
import com.mx.kylgis.pos.node.NodeRole;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Encrypted MASTER configuration service exposed by role server. */
public final class ConfigServiceCapability implements RuntimeCapability {
    private static final Logger LOG = Logger.getLogger(
            ConfigServiceCapability.class.getName());

    @Override public NodeRole getRole() { return NodeRole.SERVER; }
    @Override public CapabilityType getType() { return CapabilityType.SERVICE; }

    @Override
    public void validate(RuntimeLaunchContext context) {
        if (!context.getNodeContext().isMasterNode()) {
            throw new IllegalStateException(
                    "server role requires master role on the same node");
        }
        AppConfig config = context.getConfig();
        if (!config.isProvisioned() || config.getMasterConfigFile() == null) {
            throw new IllegalStateException(
                    "server role requires a local MASTER configuration");
        }
        String serviceId = ConfigServiceConfig.serviceId(config);
        ConfigServiceConfig.bind(config, serviceId);
        ConfigServiceConfig.port(config, serviceId);
    }

    @Override
    public RuntimeHandle start(RuntimeLaunchContext context, boolean daemon)
            throws IOException {
        final RemoteConfigServer server = new RemoteConfigServer(
                context.getConfig());
        Thread thread = new Thread(new Runnable() {
            @Override public void run() {
                try {
                    server.serve();
                } catch (IOException ex) {
                    if (!Thread.currentThread().isInterrupted()) {
                        LOG.log(Level.SEVERE,
                                "KylGis config service stopped unexpectedly",
                                ex);
                    }
                }
            }
        }, "kylgis-runtime-config-service");
        thread.setDaemon(daemon);
        thread.start();
        return new RuntimeHandle() {
            @Override public void close() throws IOException {
                server.close();
            }
        };
    }
}
