//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.runtime;

/** Lifecycle handle returned by a started capability. */
public interface RuntimeHandle extends AutoCloseable {
    @Override
    void close() throws Exception;
}
