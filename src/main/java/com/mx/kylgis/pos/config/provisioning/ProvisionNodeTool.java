//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
//
//    This file is part of KylGis POS.
package com.mx.kylgis.pos.config.provisioning;

import java.io.File;

/** Command-line helper to create the minimal bootstrap of one KylGis node. */
public final class ProvisionNodeTool {

    private ProvisionNodeTool() {
    }

    public static void main(String[] args) {
        if (args.length < 3 || args.length > 4) {
            printUsage();
            System.exit(2);
            return;
        }

        File master = new File(args[0]);
        String nodeId = args[1];
        File output = new File(args[2]);
        boolean overwrite = args.length == 4 && "--force".equals(args[3]);
        if (args.length == 4 && !overwrite) {
            printUsage();
            System.exit(2);
            return;
        }

        try {
            NodeProvisioner.provisionBootstrap(output, master, nodeId, overwrite);
            System.out.println("KylGis node provisioned: " + nodeId);
            System.out.println("Bootstrap: " + output.getAbsolutePath());
            System.out.println("MASTER: " + master.getAbsolutePath());
        } catch (Exception ex) {
            System.err.println("Cannot provision KylGis node: " + ex.getMessage());
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.err.println("Usage: ProvisionNodeTool <master.properties> <node.id> <output.properties> [--force]");
    }
}
