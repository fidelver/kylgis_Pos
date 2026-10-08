//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
//
//    This file is part of KylGis POS.
package com.mx.kylgis.pos.node;

/**
 * Capabilities that a KylGis node may expose. Roles are metadata only at this
 * stage; declaring a role does not start or stop services.
 */
public enum NodeRole {

    MASTER("master"),
    SERVER("server"),
    POS("pos"),
    KITCHEN("kitchen"),
    PRINTER_SERVICE("printer_service"),
    REMOTE_SESSIONS("remote_sessions");

    private final String propertyValue;

    NodeRole(String propertyValue) {
        this.propertyValue = propertyValue;
    }

    public String getPropertyValue() {
        return propertyValue;
    }

    public static NodeRole fromPropertyValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        for (NodeRole role : values()) {
            if (role.propertyValue.equalsIgnoreCase(normalized)) {
                return role;
            }
        }
        return null;
    }
}
