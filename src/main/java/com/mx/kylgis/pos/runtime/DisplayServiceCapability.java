//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.runtime;

import com.mx.kylgis.pos.display.service.DisplayServiceConfig;
import com.mx.kylgis.pos.display.service.DisplayServiceServer;
import com.mx.kylgis.pos.forms.AppConfig;
import com.mx.kylgis.pos.hardware.NativeHardwareSupport;
import com.mx.kylgis.pos.node.NodeRole;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DisplayServiceCapability implements RuntimeCapability {
    private static final Logger LOG=Logger.getLogger(DisplayServiceCapability.class.getName());
    @Override public NodeRole getRole(){return NodeRole.DISPLAY_SERVICE;}
    @Override public CapabilityType getType(){return CapabilityType.SERVICE;}
    @Override public void validate(RuntimeLaunchContext context){AppConfig c=context.getConfig();String id=required(c.getProperty("service.id"),"service.id");DisplayServiceConfig.endpoint(c,id);DisplayServiceConfig.bindAddress(c,id);NativeHardwareSupport.validateDisplayService(c,id);if(context.getNodeContext().hasRole(NodeRole.POS)&&!id.equals(DisplayServiceConfig.getServiceId(c)))throw new IllegalStateException("Combined pos,display_service node must route display through device.display.service="+id);}
    @Override public RuntimeHandle start(RuntimeLaunchContext context,boolean daemon)throws IOException{final DisplayServiceServer server=new DisplayServiceServer(context.getConfig());Thread t=new Thread(new Runnable(){@Override public void run(){try{server.serve();}catch(IOException e){if(!Thread.currentThread().isInterrupted())LOG.log(Level.SEVERE,"KylGis display service stopped unexpectedly",e);}}},"kylgis-runtime-display-service");t.setDaemon(daemon);t.start();return new RuntimeHandle(){@Override public void close()throws IOException{server.close();}};}
    private static String required(String v,String k){if(v==null||v.trim().isEmpty())throw new IllegalArgumentException("Missing "+k);return v.trim();}
}
