//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
//    Portions Copyright (c) 2015-2021 John Lewis (Chromis POS / ChromisKitchenScreen)
//    Portions Copyright (c) 2010-2021 Hugh Clayson / uniCenta (https://unicenta.com)
//    Portions Copyright (c) 2006-2010 Adrián Romero / Openbravo S.L.
//
//    This file is part of KylGis POS
package com.mx.kylgis.pos.printer;

import com.mx.kylgis.pos.ticket.TicketInfo;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;

/**
 * Multiplexa la cola de la impresora principal de recibos hacia salidas espejo.
 * La apertura del cajón se envía únicamente al dispositivo principal.
 */
public class DevicePrinterMirror implements DevicePrinter {

    private final DevicePrinter primary;
    private final List<DevicePrinter> mirrors = new ArrayList<>();
    private final DevicePrinter componentProvider;

    public DevicePrinterMirror(DevicePrinter primary, DevicePrinter screenMirror, DevicePrinter digitalMirror) {
        this.primary = primary == null ? new DevicePrinterNull() : primary;
        if (screenMirror != null) {
            mirrors.add(screenMirror);
        }
        if (digitalMirror != null) {
            mirrors.add(digitalMirror);
        }
        this.componentProvider = screenMirror != null ? screenMirror : this.primary;
    }

    @Override
    public String getPrinterName() {
        return componentProvider.getPrinterName();
    }

    @Override
    public String getPrinterDescription() {
        return primary.getPrinterDescription();
    }

    @Override
    public JComponent getPrinterComponent() {
        return componentProvider.getPrinterComponent();
    }

    @Override
    public void setDocumentContext(TicketInfo ticketInfo) {
        primary.setDocumentContext(ticketInfo);
        for (DevicePrinter mirror : mirrors) {
            mirror.setDocumentContext(ticketInfo);
        }
    }

    @Override
    public void reset() {
        primary.reset();
        for (DevicePrinter mirror : mirrors) {
            mirror.reset();
        }
    }

    @Override
    public void beginReceipt() {
        primary.beginReceipt();
        for (DevicePrinter mirror : mirrors) {
            mirror.beginReceipt();
        }
    }

    @Override
    public void printImage(BufferedImage image) {
        primary.printImage(image);
        for (DevicePrinter mirror : mirrors) {
            mirror.printImage(image);
        }
    }

    @Override
    public void printLogo() {
        primary.printLogo();
        for (DevicePrinter mirror : mirrors) {
            mirror.printLogo();
        }
    }

    @Override
    public void printBarCode(String type, String position, String code) {
        primary.printBarCode(type, position, code);
        for (DevicePrinter mirror : mirrors) {
            mirror.printBarCode(type, position, code);
        }
    }

    @Override
    public void beginLine(int iTextSize) {
        primary.beginLine(iTextSize);
        for (DevicePrinter mirror : mirrors) {
            mirror.beginLine(iTextSize);
        }
    }

    @Override
    public void printText(int iStyle, String sText) {
        primary.printText(iStyle, sText);
        for (DevicePrinter mirror : mirrors) {
            mirror.printText(iStyle, sText);
        }
    }

    @Override
    public void endLine() {
        primary.endLine();
        for (DevicePrinter mirror : mirrors) {
            mirror.endLine();
        }
    }

    @Override
    public void endReceipt() {
        primary.endReceipt();
        for (DevicePrinter mirror : mirrors) {
            mirror.endReceipt();
        }
    }

    @Override
    public void openDrawer() {
        // El cajón pertenece a la impresora física principal; no debe duplicarse.
        primary.openDrawer();
    }
}
