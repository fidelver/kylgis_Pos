//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.printer.service;

/** Runtime bridge because the inherited DevicePrinter API has no checked exceptions. */
public final class PrintServiceException extends RuntimeException {
    public PrintServiceException(String message) { super(message); }
    public PrintServiceException(String message, Throwable cause) { super(message, cause); }
}
