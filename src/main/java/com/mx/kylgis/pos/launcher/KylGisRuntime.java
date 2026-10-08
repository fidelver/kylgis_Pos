//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.launcher;

import com.mx.kylgis.pos.forms.AppConfig;
import com.mx.kylgis.pos.forms.StartPOS;
import com.mx.kylgis.pos.node.NodeContext;
import com.mx.kylgis.pos.node.NodeRole;
import com.mx.kylgis.pos.printer.service.PrintServiceServer;
import com.mx.kylgis.pos.printer.service.PrintServiceConfig;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Canonical role-aware entry point for the KylGis POS JAR.
 *
 * Legacy properties without node.roles still resolve to POS through NodeContext,
 * so changing the JAR Main-Class does not require immediate migration.
 */
public final class KylGisRuntime {

    private static final Logger LOG = Logger.getLogger(KylGisRuntime.class.getName());

    private KylGisRuntime() { }

    public static void main(String[] args) {
        AppConfig config = new AppConfig(args);
        config.load();
        AppConfig.setActiveInstance(config);

        NodeContext context = NodeContext.from(config);
        LOG.log(Level.INFO, "Starting KylGis runtime for {0}", context);

        boolean startsPos = context.hasRole(NodeRole.POS);
        boolean startsPrinterService = context.hasRole(NodeRole.PRINTER_SERVICE);

        if (startsPrinterService) {
            try {
                if (startsPos) {
                    validateCombinedPrinterIsolation(config);
                }
                final PrintServiceServer printServer = new PrintServiceServer(config);
                Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                    @Override public void run() {
                        try { printServer.close(); } catch (IOException ignored) { }
                    }
                }, "kylgis-runtime-print-shutdown"));

                if (startsPos) {
                    Thread serviceThread = new Thread(new Runnable() {
                        @Override public void run() {
                            try {
                                printServer.serve();
                            } catch (IOException ex) {
                                LOG.log(Level.SEVERE, "KylGis print service stopped unexpectedly", ex);
                            }
                        }
                    }, "kylgis-runtime-print-service");
                    serviceThread.setDaemon(true);
                    serviceThread.start();
                } else {
                    // A service-only node deliberately owns the main thread.
                    printServer.serve();
                    return;
                }
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "Cannot start printer_service role", ex);
                System.exit(1);
                return;
            }
        }

        if (startsPos) {
            StartPOS.start(config);
            return;
        }

        if (context.hasRole(NodeRole.KITCHEN)) {
            LOG.severe("Role kitchen is defined in the topology but is still provided by the KylGis Kitchen Screen artifact; this POS JAR cannot start it yet.");
        }
        if (context.hasRole(NodeRole.SERVER)) {
            LOG.warning("Role server is metadata-only in this build; no server runtime has been implemented yet.");
        }
        if (context.hasRole(NodeRole.REMOTE_SESSIONS)) {
            LOG.warning("Role remote_sessions is metadata-only in this build; no remote-session runtime has been implemented yet.");
        }
        if (context.isMasterNode()) {
            LOG.warning("Role master is an administrative capability and does not start a standalone process by itself.");
        }

        LOG.severe("Node has no runnable role in this KylGis POS artifact: " + context.getRoles());
        System.exit(2);
    }
    private static void validateCombinedPrinterIsolation(AppConfig config) {
        String serviceId = config.getProperty("service.id");
        if (serviceId == null || serviceId.trim().isEmpty()) {
            throw new IllegalStateException("Combined pos,printer_service node requires service.id");
        }
        String allowed = PrintServiceConfig.allowedPrinters(config, serviceId.trim());
        for (int i = 1; i <= 6; i++) {
            String index = Integer.toString(i);
            if (!PrintServiceConfig.isPrinterAllowed(allowed, index)) {
                continue;
            }
            String routedService = PrintServiceConfig.getServiceId(config, index);
            if (!serviceId.trim().equals(routedService)) {
                throw new IllegalStateException("Combined pos,printer_service node must route Printer "
                        + index + " through its own service " + serviceId
                        + " using device.printer." + index + ".service");
            }
        }
    }

}
