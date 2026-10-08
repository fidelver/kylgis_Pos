//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.runtime;

import com.mx.kylgis.pos.node.NodeRole;

/** One executable KylGis node capability. */
public interface RuntimeCapability {
    NodeRole getRole();
    CapabilityType getType();
    void validate(RuntimeLaunchContext context) throws Exception;
    RuntimeHandle start(RuntimeLaunchContext context, boolean daemon) throws Exception;
}
