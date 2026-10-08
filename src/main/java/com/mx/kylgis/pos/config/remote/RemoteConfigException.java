//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.config.remote;

import java.io.IOException;

public final class RemoteConfigException extends IOException {
    public RemoteConfigException(String message) { super(message); }
    public RemoteConfigException(String message, Throwable cause) { super(message, cause); }
}
