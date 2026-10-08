//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.runtime;

import com.mx.kylgis.pos.forms.StartPOS;
import com.mx.kylgis.pos.hardware.NativeHardwareSupport;
import com.mx.kylgis.pos.node.NodeRole;
import com.mx.kylgis.pos.scale.service.ScaleServiceConfig;

/** Interactive POS capability. */
public final class PosCapability implements RuntimeCapability {
    private static final RuntimeHandle NOOP = new RuntimeHandle() {
        @Override public void close() { }
    };

    @Override public NodeRole getRole() { return NodeRole.POS; }
    @Override public CapabilityType getType() { return CapabilityType.UI; }
    @Override public void validate(RuntimeLaunchContext context) {
        String scaleService = ScaleServiceConfig.getServiceId(context.getConfig());
        if (scaleService != null) {
            ScaleServiceConfig.endpoint(context.getConfig(), scaleService);
        }
        NativeHardwareSupport.validatePos(context.getConfig());
    }
    @Override public RuntimeHandle start(RuntimeLaunchContext context, boolean daemon) {
        StartPOS.start(context.getConfig());
        return NOOP;
    }
}
