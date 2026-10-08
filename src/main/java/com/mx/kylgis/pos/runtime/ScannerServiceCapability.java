//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.runtime;

import com.mx.kylgis.pos.forms.AppConfig;
import com.mx.kylgis.pos.hardware.NativeHardwareSupport;
import com.mx.kylgis.pos.node.NodeRole;
import com.mx.kylgis.pos.scanpal2.service.ScannerServiceConfig;
import com.mx.kylgis.pos.scanpal2.service.ScannerServiceServer;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ScannerServiceCapability implements RuntimeCapability {
    private static final Logger LOG=Logger.getLogger(ScannerServiceCapability.class.getName());
    @Override public NodeRole getRole(){return NodeRole.SCANNER_SERVICE;}
    @Override public CapabilityType getType(){return CapabilityType.SERVICE;}
    @Override public void validate(RuntimeLaunchContext context){
        AppConfig c=context.getConfig();String id=required(c.getProperty("service.id"),"service.id");
        ScannerServiceConfig.endpoint(c,id);ScannerServiceConfig.bindAddress(c,id);NativeHardwareSupport.validateScannerService(c,id);
        if(context.getNodeContext().hasRole(NodeRole.POS)&&!id.equals(ScannerServiceConfig.getServiceId(c)))
            throw new IllegalStateException("Combined pos,scanner_service node must route scanner through device.scanner.service="+id);
    }
    @Override public RuntimeHandle start(RuntimeLaunchContext context,boolean daemon)throws IOException{
        final ScannerServiceServer server=new ScannerServiceServer(context.getConfig());Thread t=new Thread(new Runnable(){@Override public void run(){try{server.serve();}catch(IOException e){if(!Thread.currentThread().isInterrupted())LOG.log(Level.SEVERE,"KylGis scanner service stopped unexpectedly",e);}}},"kylgis-runtime-scanner-service");t.setDaemon(daemon);t.start();return new RuntimeHandle(){@Override public void close()throws IOException{server.close();}};
    }
    private static String required(String v,String k){if(v==null||v.trim().isEmpty())throw new IllegalArgumentException("Missing "+k);return v.trim();}
}
