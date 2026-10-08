//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.config.provisioning;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Command-line helper to create local or remote KylGis node bootstraps. */
public final class ProvisionNodeTool {

    private ProvisionNodeTool() { }

    public static void main(String[] args) {
        if (args.length > 0 && "remote".equalsIgnoreCase(args[0])) {
            provisionRemote(args);
        } else {
            provisionLocal(args);
        }
    }

    private static void provisionLocal(String[] args) {
        if (args.length < 3 || args.length > 4) {
            printUsage(); System.exit(2); return;
        }
        File master = new File(args[0]);
        String nodeId = args[1];
        File output = new File(args[2]);
        boolean overwrite = args.length == 4 && "--force".equals(args[3]);
        if (args.length == 4 && !overwrite) { printUsage(); System.exit(2); return; }
        try {
            NodeProvisioner.provisionBootstrap(output, master, nodeId, overwrite);
            System.out.println("KylGis local node provisioned: " + nodeId);
            System.out.println("Bootstrap: " + output.getAbsolutePath());
            System.out.println("MASTER: " + master.getAbsolutePath());
        } catch (Exception ex) {
            System.err.println("Cannot provision KylGis node: " + ex.getMessage());
            System.exit(1);
        }
    }

    private static void provisionRemote(String[] args) {
        if (args.length < 7 || args.length > 8) {
            printUsage(); System.exit(2); return;
        }
        String nodeId = args[1];
        File output = new File(args[2]);
        String serviceId = args[3];
        String host = args[4];
        int port;
        try { port = Integer.parseInt(args[5]); }
        catch (NumberFormatException ex) { System.err.println("Invalid remote port"); System.exit(2); return; }
        File tokenFile = new File(args[6]);
        boolean overwrite = args.length == 8 && "--force".equals(args[7]);
        if (args.length == 8 && !overwrite) { printUsage(); System.exit(2); return; }
        try {
            String token = new String(Files.readAllBytes(tokenFile.toPath()), StandardCharsets.UTF_8).trim();
            NodeProvisioner.provisionRemoteBootstrap(output, nodeId, serviceId,
                    host, port, token, overwrite);
            System.out.println("KylGis remote node provisioned: " + nodeId);
            System.out.println("Bootstrap: " + output.getAbsolutePath());
            System.out.println("Config service: " + serviceId + " @ " + host + ":" + port);
        } catch (Exception ex) {
            System.err.println("Cannot provision remote KylGis node: " + ex.getMessage());
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.err.println("Local:  ProvisionNodeTool <master.properties> <node.id> <output.properties> [--force]");
        System.err.println("Remote: ProvisionNodeTool remote <node.id> <output.properties> <service.id> <host> <port> <token-file> [--force]");
    }
}
