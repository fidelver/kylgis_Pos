//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.printer.service;

import com.mx.kylgis.pos.forms.AppConfig;
import com.mx.kylgis.pos.node.NodeContext;
import com.mx.kylgis.pos.node.NodeRole;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Dedicated headless launcher for a node with role printer_service. */
public final class PrintServiceMain {

    private static final Logger LOG = Logger.getLogger(PrintServiceMain.class.getName());

    private PrintServiceMain() { }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: PrintServiceMain <node-bootstrap.properties>");
            System.exit(2);
        }
        try {
            AppConfig config = new AppConfig(new File(args[0]));
            AppConfig.setActiveInstance(config);
            NodeContext context = NodeContext.from(config);
            if (!context.hasRole(NodeRole.PRINTER_SERVICE)) {
                throw new IllegalStateException("Node " + context.getNodeId()
                        + " does not declare role printer_service");
            }
            String serviceId = config.getProperty("service.id");
            if (serviceId == null || serviceId.trim().isEmpty()) {
                throw new IllegalStateException("printer_service node requires service.id");
            }
            final PrintServiceServer server = new PrintServiceServer(config);
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override public void run() {
                    try { server.close(); } catch (Exception ignored) { }
                }
            }, "kylgis-print-service-shutdown"));
            server.serve();
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Cannot start KylGis print service", ex);
            System.exit(1);
        }
    }
}
