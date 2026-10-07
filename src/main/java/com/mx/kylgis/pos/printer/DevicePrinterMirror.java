//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
//    KylGis POS implementation introduced in 2026 by Fidel Arcos.
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
 *
 * Las salidas espejo terminan primero. La impresora fisica se reproduce al final
 * desde una cola en memoria; de esta forma, si el dispositivo fisico se bloquea,
 * la salida digital y la vista en pantalla ya quedaron completadas.
 * La apertura del cajon se envia unicamente al dispositivo principal.
 */
public class DevicePrinterMirror implements DevicePrinter {

    private interface PrimaryAction {
        void run();
    }

    private final DevicePrinter primary;
    private final List<DevicePrinter> mirrors = new ArrayList<>();
    private final List<PrimaryAction> primaryQueue = new ArrayList<>();
    private final DevicePrinter componentProvider;
    private boolean receiptOpen;

    public DevicePrinterMirror(DevicePrinter primary, DevicePrinter screenMirror, DevicePrinter digitalMirror) {
        this.primary = primary == null ? new DevicePrinterNull() : primary;

        // Orden intencional: digital -> pantalla -> fisica.
        if (digitalMirror != null) {
            mirrors.add(digitalMirror);
        }
        if (screenMirror != null) {
            mirrors.add(screenMirror);
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
    public void setDocumentContext(final TicketInfo ticketInfo) {
        for (DevicePrinter mirror : mirrors) {
            mirror.setDocumentContext(ticketInfo);
        }
        primary.setDocumentContext(ticketInfo);
    }

    @Override
    public void reset() {
        primaryQueue.clear();
        receiptOpen = false;
        for (DevicePrinter mirror : mirrors) {
            mirror.reset();
        }
        primary.reset();
    }

    @Override
    public void beginReceipt() {
        primaryQueue.clear();
        receiptOpen = true;
        for (DevicePrinter mirror : mirrors) {
            mirror.beginReceipt();
        }
        primaryQueue.add(new PrimaryAction() {
            @Override
            public void run() {
                primary.beginReceipt();
            }
        });
    }

    @Override
    public void printImage(final BufferedImage image) {
        for (DevicePrinter mirror : mirrors) {
            mirror.printImage(image);
        }
        enqueueOrRun(new PrimaryAction() {
            @Override
            public void run() {
                primary.printImage(image);
            }
        });
    }

    @Override
    public void printLogo() {
        for (DevicePrinter mirror : mirrors) {
            mirror.printLogo();
        }
        enqueueOrRun(new PrimaryAction() {
            @Override
            public void run() {
                primary.printLogo();
            }
        });
    }

    @Override
    public void printBarCode(final String type, final String position, final String code) {
        for (DevicePrinter mirror : mirrors) {
            mirror.printBarCode(type, position, code);
        }
        enqueueOrRun(new PrimaryAction() {
            @Override
            public void run() {
                primary.printBarCode(type, position, code);
            }
        });
    }

    @Override
    public void beginLine(final int iTextSize) {
        for (DevicePrinter mirror : mirrors) {
            mirror.beginLine(iTextSize);
        }
        enqueueOrRun(new PrimaryAction() {
            @Override
            public void run() {
                primary.beginLine(iTextSize);
            }
        });
    }

    @Override
    public void printText(final int iStyle, final String sText) {
        for (DevicePrinter mirror : mirrors) {
            mirror.printText(iStyle, sText);
        }
        enqueueOrRun(new PrimaryAction() {
            @Override
            public void run() {
                primary.printText(iStyle, sText);
            }
        });
    }

    @Override
    public void endLine() {
        for (DevicePrinter mirror : mirrors) {
            mirror.endLine();
        }
        enqueueOrRun(new PrimaryAction() {
            @Override
            public void run() {
                primary.endLine();
            }
        });
    }

    @Override
    public void endReceipt() {
        if (!receiptOpen) {
            primary.endReceipt();
            return;
        }

        // Digital y pantalla deben quedar completamente terminadas antes de
        // comenzar cualquier E/S de la impresora fisica.
        for (DevicePrinter mirror : mirrors) {
            mirror.endReceipt();
        }
        primaryQueue.add(new PrimaryAction() {
            @Override
            public void run() {
                primary.endReceipt();
            }
        });

        try {
            for (PrimaryAction action : primaryQueue) {
                action.run();
            }
        } finally {
            primaryQueue.clear();
            receiptOpen = false;
        }
    }

    private void enqueueOrRun(PrimaryAction action) {
        if (receiptOpen) {
            primaryQueue.add(action);
        } else {
            action.run();
        }
    }

    @Override
    public void openDrawer() {
        // El cajon pertenece a la impresora fisica principal; no debe duplicarse.
        primary.openDrawer();
    }
}
