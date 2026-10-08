//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.printer.service;

import com.mx.kylgis.pos.printer.DevicePrinter;
import com.mx.kylgis.pos.ticket.TicketInfo;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JComponent;

/** DevicePrinter client that submits one rendered receipt atomically to a service. */
public final class DevicePrinterRemote implements DevicePrinter {

    private final PrintServiceConfig.Endpoint endpoint;
    private final String targetPrinter;
    private final List<PrintCommand> queue = new ArrayList<>();
    private boolean receiptOpen;

    public DevicePrinterRemote(PrintServiceConfig.Endpoint endpoint, String targetPrinter) {
        if (endpoint == null) throw new IllegalArgumentException("endpoint is required");
        this.endpoint = endpoint;
        if (targetPrinter == null || !targetPrinter.trim().matches("[1-6]")) {
            throw new IllegalArgumentException("Target printer must be between 1 and 6: " + targetPrinter);
        }
        this.targetPrinter = targetPrinter.trim();
    }

    @Override public String getPrinterName() { return "KylGis Print Service " + endpoint.getServiceId(); }
    @Override public String getPrinterDescription() {
        return endpoint.getServiceId() + "@" + endpoint.getHost() + ":" + endpoint.getPort()
                + " -> Printer " + targetPrinter;
    }
    @Override public JComponent getPrinterComponent() { return null; }
    @Override public void setDocumentContext(TicketInfo ticketInfo) { }

    @Override public synchronized void reset() {
        queue.clear();
        receiptOpen = false;
        sendSingle(PrintCommand.simple(PrintCommand.Type.RESET));
    }

    @Override public synchronized void beginReceipt() {
        queue.clear();
        receiptOpen = true;
        queue.add(PrintCommand.simple(PrintCommand.Type.BEGIN_RECEIPT));
    }

    @Override public synchronized void printImage(BufferedImage image) {
        if (image == null) return;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            if (!ImageIO.write(image, "png", out)) throw new IOException("PNG encoder unavailable");
            enqueue(PrintCommand.image(out.toByteArray()));
        } catch (IOException ex) {
            throw new PrintServiceException("Cannot encode image for remote printer", ex);
        }
    }

    @Override public synchronized void printLogo() { enqueue(PrintCommand.simple(PrintCommand.Type.PRINT_LOGO)); }
    @Override public synchronized void printBarCode(String type, String position, String code) {
        enqueue(PrintCommand.barcode(type, position, code));
    }
    @Override public synchronized void beginLine(int iTextSize) {
        enqueue(PrintCommand.integer(PrintCommand.Type.BEGIN_LINE, iTextSize));
    }
    @Override public synchronized void printText(int iStyle, String sText) {
        enqueue(PrintCommand.text(iStyle, sText));
    }
    @Override public synchronized void endLine() { enqueue(PrintCommand.simple(PrintCommand.Type.END_LINE)); }

    @Override public synchronized void endReceipt() {
        if (!receiptOpen) {
            sendSingle(PrintCommand.simple(PrintCommand.Type.END_RECEIPT));
            return;
        }
        queue.add(PrintCommand.simple(PrintCommand.Type.END_RECEIPT));
        try {
            send(new ArrayList<>(queue));
        } finally {
            queue.clear();
            receiptOpen = false;
        }
    }

    @Override public synchronized void openDrawer() {
        sendSingle(PrintCommand.simple(PrintCommand.Type.OPEN_DRAWER));
    }

    private void enqueue(PrintCommand command) {
        if (receiptOpen) queue.add(command); else sendSingle(command);
    }

    private void sendSingle(PrintCommand command) {
        List<PrintCommand> commands = new ArrayList<>(1);
        commands.add(command);
        send(commands);
    }

    private void send(List<PrintCommand> commands) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(endpoint.getHost(), endpoint.getPort()),
                    endpoint.getConnectTimeoutMs());
            socket.setSoTimeout(endpoint.getReadTimeoutMs());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());
            PrintProtocol.writeRequest(out, endpoint.getServiceId(), endpoint.getToken(),
                    targetPrinter, commands);
            PrintProtocol.Response response = PrintProtocol.readResponse(in);
            if (!response.ok) throw new IOException(response.message);
        } catch (IOException ex) {
            throw new PrintServiceException("Print service " + endpoint.getServiceId()
                    + " failed: " + ex.getMessage(), ex);
        }
    }

}
