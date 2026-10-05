//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
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

import java.util.List;

/**
 * Layout lógico de un ticket. Mantiene pequeñas las columnas numéricas y
 * entrega el ancho útil a los textos descriptivos.
 */
public final class TicketLayout {

    public static final int MIN_WIDTH = 31;
    public static final int MAX_WIDTH = 120;
    public static final int DEFAULT_WIDTH = 32;

    private final int width;
    private final int priceWidth;
    private final int quantityWidth;
    private final int amountWidth;
    private final int numericGapWidth;

    public TicketLayout(int requestedWidth) {
        width = clamp(requestedWidth, MIN_WIDTH, MAX_WIDTH);

        // Estas columnas no necesitan crecer con el papel. El espacio extra
        // queda como separación visual y la descripción usa el ancho completo.
        priceWidth = 10;
        quantityWidth = 8; // suficiente para "Cantidad" y cantidades comunes
        amountWidth = 13;
        numericGapWidth = Math.max(0, width - priceWidth - quantityWidth - amountWidth);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public int getWidth() {
        return width;
    }

    public int getDescriptionWidth() {
        return width;
    }

    public int getPriceWidth() {
        return priceWidth;
    }

    public int getQuantityWidth() {
        return quantityWidth;
    }

    public int getAmountWidth() {
        return amountWidth;
    }

    public int getNumericGapWidth() {
        return numericGapWidth;
    }

    public int getTotalLabelWidth() {
        return Math.min(11, Math.max(1, width - 8));
    }

    public int getTotalValueWidth() {
        return width - getTotalLabelWidth();
    }

    public int getArticlesLabelWidth() {
        return Math.min(20, Math.max(1, width - 4));
    }

    public int getArticlesValueWidth() {
        return width - getArticlesLabelWidth();
    }

    public int getPaymentLabelWidth() {
        return Math.min(17, Math.max(1, width - 10));
    }

    public int getPaymentValueWidth() {
        return width - getPaymentLabelWidth();
    }

    public String getSeparator() {
        StringBuilder sb = new StringBuilder(width);
        for (int i = 0; i < width; i++) {
            sb.append('-');
        }
        return sb.toString();
    }

    public String getDoubleSeparator() {
        StringBuilder sb = new StringBuilder(width);
        for (int i = 0; i < width; i++) {
            sb.append('=');
        }
        return sb.toString();
    }

    public List<String> wrap(String text) {
        return TicketTextUtils.wrapWords(text, width);
    }

    public List<String> wrapProduct(String text, boolean component) {
        String value = text == null ? "" : text;
        if (component) {
            value = "*" + value;
        }
        return wrap(value);
    }
}
