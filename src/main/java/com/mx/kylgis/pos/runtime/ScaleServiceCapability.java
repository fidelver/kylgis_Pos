//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.runtime;

import com.mx.kylgis.pos.forms.AppConfig;
import com.mx.kylgis.pos.hardware.NativeHardwareSupport;
import com.mx.kylgis.pos.node.NodeRole;
import com.mx.kylgis.pos.scale.service.ScaleServiceConfig;
import com.mx.kylgis.pos.scale.service.ScaleServiceServer;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Network scale-service capability. */
public final class ScaleServiceCapability implements RuntimeCapability {
    private static final Logger LOG = Logger.getLogger(ScaleServiceCapability.class.getName());

    @Override public NodeRole getRole() { return NodeRole.SCALE_SERVICE; }
    @Override public CapabilityType getType() { return CapabilityType.SERVICE; }

    @Override
    public void validate(RuntimeLaunchContext context) {
        AppConfig config = context.getConfig();
        String serviceId = required(config.getProperty("service.id"), "service.id");
        ScaleServiceConfig.endpoint(config, serviceId);
        ScaleServiceConfig.bindAddress(config, serviceId);
        NativeHardwareSupport.validateScaleService(config, serviceId);
        if (context.getNodeContext().hasRole(NodeRole.POS)) {
            String routed = ScaleServiceConfig.getServiceId(config);
            if (!serviceId.equals(routed)) {
                throw new IllegalStateException("Combined pos,scale_service node must route its scale through "
                        + "device.scale.service=" + serviceId);
            }
        }
    }

    @Override
    public RuntimeHandle start(RuntimeLaunchContext context, boolean daemon) throws IOException {
        final ScaleServiceServer server = new ScaleServiceServer(context.getConfig());
        Thread thread = new Thread(new Runnable() {
            @Override public void run() {
                try { server.serve(); }
                catch (IOException ex) {
                    if (!Thread.currentThread().isInterrupted()) {
                        LOG.log(Level.SEVERE, "KylGis scale service stopped unexpectedly", ex);
                    }
                }
            }
        }, "kylgis-runtime-scale-service");
        thread.setDaemon(daemon); thread.start();
        return new RuntimeHandle() { @Override public void close() throws IOException { server.close(); } };
    }

    private static String required(String value, String key) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("Missing " + key);
        return value.trim();
    }
}
