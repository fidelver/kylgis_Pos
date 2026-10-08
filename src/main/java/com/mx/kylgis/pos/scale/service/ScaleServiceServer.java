//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.scale.service;

import com.mx.kylgis.pos.forms.AppProperties;
import com.mx.kylgis.pos.scale.DeviceScale;
import com.mx.kylgis.pos.scale.ScaleException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Network owner of one physical scale. */
public final class ScaleServiceServer implements AutoCloseable {
    private static final Logger LOG = Logger.getLogger(ScaleServiceServer.class.getName());
    private final String serviceId, token;
    private final DeviceScale scale;
    private final ServerSocket serverSocket;
    private final ExecutorService clients = Executors.newFixedThreadPool(4, new ThreadFactory() {
        private int sequence;
        @Override public synchronized Thread newThread(Runnable task) {
            Thread t = new Thread(task, "kylgis-scale-client-" + (++sequence));
            t.setDaemon(true); return t;
        }
    });
    private volatile boolean running;

    public ScaleServiceServer(AppProperties properties) throws IOException {
        if (properties == null) throw new IllegalArgumentException("properties are required");
        serviceId = required(properties.getProperty("service.id"), "service.id");
        ScaleServiceConfig.Endpoint endpoint = ScaleServiceConfig.endpoint(properties, serviceId);
        token = endpoint.getToken();
        String bind = ScaleServiceConfig.bindAddress(properties, serviceId);
        scale = new DeviceScale(null, new LocalHardwareProperties(properties));
        if (!scale.existsScale()) throw new IllegalArgumentException("Scale service has no local machine.scale configured");
        serverSocket = new ServerSocket(); serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress(InetAddress.getByName(bind), endpoint.getPort()), 16);
    }

    public int getLocalPort() { return serverSocket.getLocalPort(); }

    public void serve() throws IOException {
        running = true;
        LOG.log(Level.INFO, "KylGis scale service {0} listening on {1}",
                new Object[]{serviceId, serverSocket.getLocalSocketAddress()});
        while (running) {
            try {
                final Socket socket = serverSocket.accept();
                clients.submit(new Runnable() { @Override public void run() { handle(socket); } });
            } catch (IOException ex) { if (running) throw ex; }
        }
    }

    private void handle(Socket socket) {
        try (Socket client = socket;
                DataInputStream in = new DataInputStream(client.getInputStream());
                DataOutputStream out = new DataOutputStream(client.getOutputStream())) {
            client.setSoTimeout(10000);
            try {
                ScaleProtocol.Request request = ScaleProtocol.readRequest(in);
                if (!serviceId.equals(request.serviceId) || !secureEquals(token, request.token)) {
                    throw new IOException("Scale service authentication failed");
                }
                Double weight;
                synchronized (scale) { weight = scale.readWeight(); }
                ScaleProtocol.writeResponse(out, true, weight, "OK");
            } catch (ScaleException | IOException ex) {
                LOG.log(Level.WARNING, "Rejected/failed scale read from {0}: {1}",
                        new Object[]{client.getRemoteSocketAddress(), safeMessage(ex)});
                try { ScaleProtocol.writeResponse(out, false, null, safeMessage(ex)); }
                catch (IOException ignored) { }
            }
        } catch (IOException ex) {
            LOG.log(Level.FINE, "Scale service client connection ended", ex);
        }
    }

    @Override public void close() throws IOException {
        running = false; serverSocket.close(); clients.shutdownNow();
    }

    private static final class LocalHardwareProperties implements AppProperties {
        private final AppProperties delegate;
        LocalHardwareProperties(AppProperties delegate) { this.delegate = delegate; }
        @Override public java.io.File getConfigFile() { return delegate.getConfigFile(); }
        @Override public String getHost() { return delegate.getHost(); }
        @Override public String getProperty(String key) {
            if (ScaleServiceConfig.DEVICE_SERVICE_KEY.equals(key)) return null;
            return delegate.getProperty(key);
        }
    }

    private static boolean secureEquals(String left, String right) {
        byte[] a = (left == null ? "" : left).getBytes(StandardCharsets.UTF_8);
        byte[] b = (right == null ? "" : right).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }
    private static String required(String value, String key) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("Missing " + key);
        return value.trim();
    }
    private static String safeMessage(Exception ex) {
        String m = ex.getMessage(); return m == null || m.trim().isEmpty() ? ex.getClass().getSimpleName() : m;
    }
}
