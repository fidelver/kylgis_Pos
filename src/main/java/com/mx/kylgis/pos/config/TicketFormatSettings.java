//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
//    KylGis POS implementation introduced in 2026 by Fidel Arcos.
//
//    This file is part of KylGis POS
package com.mx.kylgis.pos.config;

/**
 * Estado compartido del formato lógico/físico del ticket dentro del panel de
 * configuración. Permite editar el mismo ajuste desde Ticket Setup o
 * Peripherals sin que un panel sobrescriba al otro al guardar.
 */
final class TicketFormatSettings {

    private static String paperSize = "58";
    private static int width = 32;

    private TicketFormatSettings() {
    }

    static synchronized void set(String paper, int chars) {
        if (!"80".equals(paper) && !"120".equals(paper)) {
            paper = "58";
        }
        paperSize = paper;
        width = Math.max(31, Math.min(chars, 120));
    }

    static synchronized String getPaperSize() {
        return paperSize;
    }

    static synchronized int getWidth() {
        return width;
    }

    static int presetWidth(String paper) {
        if ("80".equals(paper)) return 42;
        if ("120".equals(paper)) return 64;
        return 32;
    }
}
