//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.printer.service;

import java.util.Arrays;

/** One already-rendered DevicePrinter operation. */
public final class PrintCommand {

    public enum Type {
        RESET,
        BEGIN_RECEIPT,
        PRINT_IMAGE,
        PRINT_LOGO,
        PRINT_BARCODE,
        BEGIN_LINE,
        PRINT_TEXT,
        END_LINE,
        END_RECEIPT,
        OPEN_DRAWER
    }

    private final Type type;
    private final int intValue;
    private final String value1;
    private final String value2;
    private final String value3;
    private final byte[] binary;

    private PrintCommand(Type type, int intValue, String value1,
            String value2, String value3, byte[] binary) {
        this.type = type;
        this.intValue = intValue;
        this.value1 = value1;
        this.value2 = value2;
        this.value3 = value3;
        this.binary = binary == null ? null : Arrays.copyOf(binary, binary.length);
    }

    public static PrintCommand simple(Type type) {
        return new PrintCommand(type, 0, null, null, null, null);
    }

    public static PrintCommand integer(Type type, int value) {
        return new PrintCommand(type, value, null, null, null, null);
    }

    public static PrintCommand text(int style, String text) {
        return new PrintCommand(Type.PRINT_TEXT, style, text, null, null, null);
    }

    public static PrintCommand barcode(String type, String position, String code) {
        return new PrintCommand(Type.PRINT_BARCODE, 0, type, position, code, null);
    }

    public static PrintCommand image(byte[] png) {
        return new PrintCommand(Type.PRINT_IMAGE, 0, null, null, null, png);
    }

    static PrintCommand raw(Type type, int intValue, String value1,
            String value2, String value3, byte[] binary) {
        return new PrintCommand(type, intValue, value1, value2, value3, binary);
    }

    public Type getType() { return type; }
    public int getIntValue() { return intValue; }
    public String getValue1() { return value1; }
    public String getValue2() { return value2; }
    public String getValue3() { return value3; }
    public byte[] getBinary() {
        return binary == null ? null : Arrays.copyOf(binary, binary.length);
    }
}
