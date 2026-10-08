//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.runtime;

import com.mx.kylgis.pos.forms.AppConfig;
import com.mx.kylgis.pos.node.NodeRole;
import com.mx.kylgis.pos.printer.service.PrintServiceConfig;
import com.mx.kylgis.pos.printer.service.PrintServiceServer;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Network print-service capability. */
public final class PrintServiceCapability implements RuntimeCapability {
    private static final Logger LOG = Logger.getLogger(PrintServiceCapability.class.getName());

    @Override public NodeRole getRole() { return NodeRole.PRINTER_SERVICE; }
    @Override public CapabilityType getType() { return CapabilityType.SERVICE; }

    @Override
    public void validate(RuntimeLaunchContext context) {
        AppConfig config = context.getConfig();
        String serviceId = required(config.getProperty("service.id"), "service.id");
        // Resolve all service-level settings before hardware is opened.
        PrintServiceConfig.endpoint(config, serviceId);
        PrintServiceConfig.bindAddress(config, serviceId);
        String allowed = PrintServiceConfig.allowedPrinters(config, serviceId);

        if (context.getNodeContext().hasRole(NodeRole.POS)) {
            for (int i = 1; i <= 6; i++) {
                String index = Integer.toString(i);
                if (!PrintServiceConfig.isPrinterAllowed(allowed, index)) continue;
                String routedService = PrintServiceConfig.getServiceId(config, index);
                if (!serviceId.equals(routedService)) {
                    throw new IllegalStateException("Combined pos,printer_service node must route Printer "
                            + index + " through its own service " + serviceId
                            + " using device.printer." + index + ".service");
                }
            }
        }
    }

    @Override
    public RuntimeHandle start(RuntimeLaunchContext context, boolean daemon) throws IOException {
        final PrintServiceServer server = new PrintServiceServer(context.getConfig());
        Thread thread = new Thread(new Runnable() {
            @Override public void run() {
                try {
                    server.serve();
                } catch (IOException ex) {
                    if (!Thread.currentThread().isInterrupted()) {
                        LOG.log(Level.SEVERE, "KylGis print service stopped unexpectedly", ex);
                    }
                }
            }
        }, "kylgis-runtime-print-service");
        thread.setDaemon(daemon);
        thread.start();
        return new RuntimeHandle() {
            @Override public void close() throws IOException { server.close(); }
        };
    }

    private static String required(String value, String key) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing " + key);
        }
        return value.trim();
    }
}
