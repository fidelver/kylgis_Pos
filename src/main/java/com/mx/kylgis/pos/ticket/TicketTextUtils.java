//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
//    KylGis POS implementation introduced in 2026 by Fidel Arcos.
//    Portions Copyright (c) 2015-2021 John Lewis (Chromis POS / ChromisKitchenScreen)
//    Portions Copyright (c) 2010-2021 Hugh Clayson / uniCenta (https://unicenta.com)
//    Portions Copyright (c) 2006-2010 Adrián Romero / Openbravo S.L.
//
//    This file is part of KylGis POS
//
//    KylGis POS is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
package com.mx.kylgis.pos.ticket;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilidades para envolver texto de tickets respetando palabras completas.
 */
public final class TicketTextUtils {

    private TicketTextUtils() {
    }

    /**
     * Divide texto en líneas de como máximo {@code width} caracteres sin
     * cortar palabras. Los saltos de línea existentes se respetan. Solamente
     * se divide un token cuando él solo excede todo el ancho disponible.
     *
     * @param text texto a envolver
     * @param width ancho lógico del ticket en caracteres
     * @return líneas listas para imprimir
     */
    public static List<String> wrapWords(String text, int width) {
        int safeWidth = Math.max(1, width);
        List<String> result = new ArrayList<>();

        if (text == null || text.trim().isEmpty()) {
            return result;
        }

        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        String[] paragraphs = normalized.split("\n", -1);

        for (String paragraph : paragraphs) {
            String clean = paragraph.trim().replaceAll("[\\t ]+", " ");
            if (clean.isEmpty()) {
                result.add("");
                continue;
            }

            StringBuilder line = new StringBuilder();
            for (String token : clean.split(" ")) {
                if (token.length() > safeWidth) {
                    if (line.length() > 0) {
                        result.add(line.toString());
                        line.setLength(0);
                    }
                    int pos = 0;
                    while (token.length() - pos > safeWidth) {
                        result.add(token.substring(pos, pos + safeWidth));
                        pos += safeWidth;
                    }
                    if (pos < token.length()) {
                        line.append(token.substring(pos));
                    }
                } else if (line.length() == 0) {
                    line.append(token);
                } else if (line.length() + 1 + token.length() <= safeWidth) {
                    line.append(' ').append(token);
                } else {
                    result.add(line.toString());
                    line.setLength(0);
                    line.append(token);
                }
            }
            if (line.length() > 0) {
                result.add(line.toString());
            }
        }
        return result;
    }
}
