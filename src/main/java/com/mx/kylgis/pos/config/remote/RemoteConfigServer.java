//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.config.remote;

import com.mx.kylgis.pos.config.provisioning.NodeProvisioner;
import com.mx.kylgis.pos.forms.AppConfig;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

/** MASTER-side encrypted provisioning service. */
public final class RemoteConfigServer implements AutoCloseable {
    private static final Logger LOG = Logger.getLogger(
            RemoteConfigServer.class.getName());
    private static final long NONCE_TTL_MS = 5L * 60L * 1000L;
    private static final int MAX_NONCES = 4096;

    private final AppConfig config;
    private final File masterFile;
    private final String serviceId;
    private final ServerSocket serverSocket;
    private final ExecutorService clients = Executors.newFixedThreadPool(8,
            new ThreadFactory() {
        private int sequence;
        @Override public synchronized Thread newThread(Runnable task) {
            Thread thread = new Thread(task,
                    "kylgis-config-client-" + (++sequence));
            thread.setDaemon(true);
            return thread;
        }
    });
    private final Map<String, Long> usedNonces =
            new LinkedHashMap<String, Long>(128, 0.75f, true);
    private volatile boolean running;

    public RemoteConfigServer(AppConfig config) throws IOException {
        if (config == null) {
            throw new IllegalArgumentException("config is required");
        }
        if (!config.isProvisioned() || config.getMasterConfigFile() == null) {
            throw new IllegalArgumentException(
                    "server role requires a locally provisioned MASTER");
        }
        this.config = config;
        this.masterFile = config.getMasterConfigFile().getCanonicalFile();
        this.serviceId = ConfigServiceConfig.serviceId(config);
        String bind = ConfigServiceConfig.bind(config, serviceId);
        int port = ConfigServiceConfig.port(config, serviceId);

        // Validate MASTER readability before binding a socket.
        NodeProvisioner.resolveEffectiveNode(masterFile,
                config.getProperty(NodeProvisioner.NODE_ID_KEY));

        this.serverSocket = new ServerSocket();
        this.serverSocket.setReuseAddress(true);
        this.serverSocket.bind(new InetSocketAddress(
                InetAddress.getByName(bind), port), 32);
    }

    public int getLocalPort() {
        return serverSocket.getLocalPort();
    }

    public void serve() throws IOException {
        running = true;
        LOG.log(Level.INFO, "KylGis config service {0} listening on {1}",
                new Object[]{serviceId,
                    serverSocket.getLocalSocketAddress()});
        while (running) {
            try {
                final Socket socket = serverSocket.accept();
                clients.submit(new Runnable() {
                    @Override public void run() { handle(socket); }
                });
            } catch (IOException ex) {
                if (running) throw ex;
            }
        }
    }

    private void handle(Socket socket) {
        try (Socket client = socket;
                DataInputStream in = new DataInputStream(
                        client.getInputStream());
                DataOutputStream out = new DataOutputStream(
                        client.getOutputStream())) {
            client.setSoTimeout(15000);
            RemoteConfigProtocol.Request request;
            try {
                request = RemoteConfigProtocol.readRequest(in);
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "Rejected malformed provisioning request from {0}",
                        client.getRemoteSocketAddress());
                RemoteConfigProtocol.writeError(out, "Unauthorized");
                return;
            }

            String token;
            try {
                if (!serviceId.equals(request.getServiceId())) {
                    throw new IllegalArgumentException("wrong service");
                }
                token = ConfigServiceConfig.tokenForNode(config,
                        serviceId, request.getNodeId());
                if (!RemoteConfigProtocol.authenticate(request, token)) {
                    throw new IllegalArgumentException("bad authentication");
                }
                long now = System.currentTimeMillis();
                if (request.getTimestamp() < now - RemoteConfigProtocol.MAX_CLOCK_SKEW_MS
                        || request.getTimestamp() > now + RemoteConfigProtocol.MAX_CLOCK_SKEW_MS) {
                    throw new IllegalArgumentException("stale request");
                }
                rejectReplay(request);
            } catch (Exception ex) {
                LOG.log(Level.WARNING,
                        "Rejected unauthenticated provisioning request from {0}",
                        client.getRemoteSocketAddress());
                RemoteConfigProtocol.writeError(out, "Unauthorized");
                return;
            }

            try {
                Properties effective = NodeProvisioner.resolveEffectiveNode(
                        masterFile, request.getNodeId());
                RemoteConfigProtocol.writeSuccess(out, request, token,
                        effective);
                LOG.log(Level.INFO, "Provisioned node {0} to {1}",
                        new Object[]{request.getNodeId(),
                            client.getRemoteSocketAddress()});
            } catch (Exception ex) {
                LOG.log(Level.WARNING,
                        "Authenticated provisioning failed for node "
                        + request.getNodeId(), ex);
                RemoteConfigProtocol.writeError(out,
                        "Provisioning unavailable");
            }
        } catch (IOException ex) {
            LOG.log(Level.FINE,
                    "Provisioning client connection ended", ex);
        }
    }

    private synchronized void rejectReplay(RemoteConfigProtocol.Request request)
            throws RemoteConfigException {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Long>> iterator =
                usedNonces.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (now - entry.getValue() > NONCE_TTL_MS) iterator.remove();
        }
        String key = request.getNodeId() + ":"
                + Base64.getEncoder().encodeToString(request.getNonce());
        if (usedNonces.containsKey(key)) {
            throw new RemoteConfigException("Repeated provisioning nonce");
        }
        usedNonces.put(key, now);
        while (usedNonces.size() > MAX_NONCES) {
            Iterator<String> keys = usedNonces.keySet().iterator();
            if (keys.hasNext()) {
                keys.next();
                keys.remove();
            } else {
                break;
            }
        }
    }

    @Override public void close() throws IOException {
        running = false;
        serverSocket.close();
        clients.shutdownNow();
    }
}
