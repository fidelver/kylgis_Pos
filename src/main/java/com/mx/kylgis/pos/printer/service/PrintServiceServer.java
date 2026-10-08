//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.printer.service;

import com.mx.kylgis.pos.forms.AppProperties;
import com.mx.kylgis.pos.printer.DevicePrinter;
import com.mx.kylgis.pos.printer.DeviceTicket;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/** Headless network service that is the sole opener of its configured printers. */
public final class PrintServiceServer implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(PrintServiceServer.class.getName());
    private final String serviceId;
    private final String token;
    private final String allowedPrinters;
    private final DeviceTicket deviceTicket;
    private final ServerSocket serverSocket;
    private final ExecutorService clients = Executors.newFixedThreadPool(8, new ThreadFactory() {
        private int sequence;
        @Override public synchronized Thread newThread(Runnable task) {
            Thread thread = new Thread(task, "kylgis-print-client-" + (++sequence));
            thread.setDaemon(true);
            return thread;
        }
    });
    private volatile boolean running;

    public PrintServiceServer(AppProperties properties) throws IOException {
        this(prepare(properties));
    }

    private PrintServiceServer(ServerBootstrap bootstrap) throws IOException {
        this.serviceId = bootstrap.serviceId;
        this.token = bootstrap.endpoint.getToken();
        this.allowedPrinters = bootstrap.allowedPrinters;
        // Hardware is opened only after service identity, endpoint and token
        // have been validated successfully by prepare().
        this.deviceTicket = new DeviceTicket(null,
                new LocalHardwareProperties(bootstrap.properties));
        this.serverSocket = new ServerSocket();
        this.serverSocket.setReuseAddress(true);
        this.serverSocket.bind(new InetSocketAddress(
                InetAddress.getByName(bootstrap.bindAddress), bootstrap.endpoint.getPort()), 32);
    }

    private static ServerBootstrap prepare(AppProperties properties) {
        if (properties == null) throw new IllegalArgumentException("properties are required");
        String serviceId = required(properties.getProperty("service.id"), "service.id");
        PrintServiceConfig.Endpoint endpoint = PrintServiceConfig.endpoint(properties, serviceId);
        String bind = PrintServiceConfig.bindAddress(properties, serviceId);
        String allowed = PrintServiceConfig.allowedPrinters(properties, serviceId);
        return new ServerBootstrap(properties, serviceId, endpoint, bind, allowed);
    }

    public int getLocalPort() { return serverSocket.getLocalPort(); }

    public void serve() throws IOException {
        running = true;
        LOG.log(Level.INFO, "KylGis print service {0} listening on {1}",
                new Object[]{serviceId, serverSocket.getLocalSocketAddress()});
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
                DataInputStream in = new DataInputStream(client.getInputStream());
                DataOutputStream out = new DataOutputStream(client.getOutputStream())) {
            client.setSoTimeout(15000);
            try {
                PrintProtocol.Request request = PrintProtocol.readRequest(in);
                validate(request);
                execute(request.printerIndex, request.commands);
                PrintProtocol.writeResponse(out, true, "OK");
            } catch (Exception ex) {
                if (ex instanceof IOException || ex instanceof IllegalArgumentException) {
                    LOG.log(Level.WARNING, "Rejected print job from {0}: {1}",
                            new Object[]{client.getRemoteSocketAddress(), safeMessage(ex)});
                } else {
                    LOG.log(Level.WARNING, "Failed print job from " + client.getRemoteSocketAddress(), ex);
                }
                try { PrintProtocol.writeResponse(out, false, safeMessage(ex)); } catch (IOException ignored) { }
            }
        } catch (IOException ex) {
            LOG.log(Level.FINE, "Print service client connection ended", ex);
        }
    }

    private void validate(PrintProtocol.Request request) throws IOException {
        if (!serviceId.equals(request.serviceId)) throw new IOException("Wrong print service id");
        if (!secureEquals(token, request.token)) throw new IOException("Print service authentication failed");
        if (request.printerIndex == null || !request.printerIndex.matches("[1-6]")) {
            throw new IOException("Invalid target printer");
        }
        if (!PrintServiceConfig.isPrinterAllowed(allowedPrinters, request.printerIndex)) {
            throw new IOException("Printer " + request.printerIndex + " is not exposed by service " + serviceId);
        }
    }

    private void execute(String printerIndex, List<PrintCommand> commands) throws IOException {
        DevicePrinter printer = deviceTicket.getDevicePrinter(printerIndex);
        synchronized (printer) {
            for (PrintCommand command : commands) execute(printer, command);
        }
    }

    private static void execute(DevicePrinter printer, PrintCommand command) throws IOException {
        switch (command.getType()) {
            case RESET: printer.reset(); break;
            case BEGIN_RECEIPT: printer.beginReceipt(); break;
            case PRINT_IMAGE:
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(command.getBinary()));
                if (image == null) throw new IOException("Invalid image payload");
                printer.printImage(image); break;
            case PRINT_LOGO: printer.printLogo(); break;
            case PRINT_BARCODE: printer.printBarCode(command.getValue1(), command.getValue2(), command.getValue3()); break;
            case BEGIN_LINE: printer.beginLine(command.getIntValue()); break;
            case PRINT_TEXT: printer.printText(command.getIntValue(), command.getValue1()); break;
            case END_LINE: printer.endLine(); break;
            case END_RECEIPT: printer.endReceipt(); break;
            case OPEN_DRAWER: printer.openDrawer(); break;
            default: throw new IOException("Unsupported print command " + command.getType());
        }
    }

    @Override public void close() throws IOException {
        running = false;
        serverSocket.close();
        clients.shutdownNow();
    }

    /**
     * The service process must see the physical printer configuration even when
     * the same node also runs POS and routes that logical printer back through
     * its own service. Only client-side device.printer.N.service keys are hidden.
     */
    private static final class LocalHardwareProperties implements AppProperties {
        private final AppProperties delegate;
        LocalHardwareProperties(AppProperties delegate) { this.delegate = delegate; }
        @Override public java.io.File getConfigFile() { return delegate.getConfigFile(); }
        @Override public String getHost() { return delegate.getHost(); }
        @Override public String getProperty(String key) {
            if (key != null && key.matches("device\\.printer\\.[1-6]\\.service(?:\\.target)?")) {
                return null;
            }
            return delegate.getProperty(key);
        }
    }

    private static final class ServerBootstrap {
        final AppProperties properties;
        final String serviceId;
        final PrintServiceConfig.Endpoint endpoint;
        final String bindAddress;
        final String allowedPrinters;
        ServerBootstrap(AppProperties properties, String serviceId,
                PrintServiceConfig.Endpoint endpoint, String bindAddress,
                String allowedPrinters) {
            this.properties = properties;
            this.serviceId = serviceId;
            this.endpoint = endpoint;
            this.bindAddress = bindAddress;
            this.allowedPrinters = allowedPrinters;
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
        String message = ex.getMessage();
        return message == null || message.trim().isEmpty() ? ex.getClass().getSimpleName() : message;
    }
}
