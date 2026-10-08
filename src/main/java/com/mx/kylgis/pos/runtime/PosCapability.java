//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.runtime;

import com.mx.kylgis.pos.forms.StartPOS;
import com.mx.kylgis.pos.display.service.DisplayServiceConfig;
import com.mx.kylgis.pos.hardware.NativeHardwareSupport;
import com.mx.kylgis.pos.node.NodeRole;
import com.mx.kylgis.pos.scale.service.ScaleServiceConfig;
import com.mx.kylgis.pos.scanpal2.service.ScannerServiceConfig;

/** Interactive POS capability. */
public final class PosCapability implements RuntimeCapability {
    private static final RuntimeHandle NOOP = new RuntimeHandle() {
        @Override public void close() { }
    };

    @Override public NodeRole getRole() { return NodeRole.POS; }
    @Override public CapabilityType getType() { return CapabilityType.UI; }
    @Override public void validate(RuntimeLaunchContext context) {
        String displayService = DisplayServiceConfig.getServiceId(context.getConfig());
        if (displayService != null) {
            DisplayServiceConfig.endpoint(context.getConfig(), displayService);
        }
        String scaleService = ScaleServiceConfig.getServiceId(context.getConfig());
        if (scaleService != null) {
            ScaleServiceConfig.endpoint(context.getConfig(), scaleService);
        }
        String scannerService = ScannerServiceConfig.getServiceId(context.getConfig());
        if (scannerService != null) {
            ScannerServiceConfig.endpoint(context.getConfig(), scannerService);
        }
        NativeHardwareSupport.validatePos(context.getConfig());
    }
    @Override public RuntimeHandle start(RuntimeLaunchContext context, boolean daemon) {
        StartPOS.start(context.getConfig());
        return NOOP;
    }
}
