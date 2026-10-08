//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.display.service;

import com.mx.kylgis.pos.forms.AppProperties;
import com.mx.kylgis.pos.printer.DeviceDisplay;
import com.mx.kylgis.pos.printer.DeviceTicket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DisplayServiceServer implements AutoCloseable {
    private static final Logger LOG=Logger.getLogger(DisplayServiceServer.class.getName());
    private final String serviceId,token;private final DeviceDisplay display;private final ServerSocket server;
    private final ExecutorService clients=Executors.newFixedThreadPool(4,new ThreadFactory(){private int n;@Override public synchronized Thread newThread(Runnable r){Thread t=new Thread(r,"kylgis-display-client-"+(++n));t.setDaemon(true);return t;}});private volatile boolean running;
    public DisplayServiceServer(AppProperties p)throws IOException{if(p==null)throw new IllegalArgumentException("properties are required");serviceId=required(p.getProperty("service.id"),"service.id");DisplayServiceConfig.Endpoint e=DisplayServiceConfig.endpoint(p,serviceId);token=e.getToken();DeviceTicket ticket=new DeviceTicket(null,new LocalHardwareProperties(p));display=ticket.getDeviceDisplay();server=new ServerSocket();server.setReuseAddress(true);server.bind(new InetSocketAddress(InetAddress.getByName(DisplayServiceConfig.bindAddress(p,serviceId)),e.getPort()),16);}
    public void serve()throws IOException{running=true;LOG.log(Level.INFO,"KylGis display service {0} listening on {1}",new Object[]{serviceId,server.getLocalSocketAddress()});while(running){try{final Socket s=server.accept();clients.submit(new Runnable(){@Override public void run(){handle(s);}});}catch(IOException e){if(running)throw e;}}}
    private void handle(Socket socket){try(Socket c=socket;DataInputStream in=new DataInputStream(c.getInputStream());DataOutputStream out=new DataOutputStream(c.getOutputStream())){c.setSoTimeout(10000);try{DisplayProtocol.Request r=DisplayProtocol.readRequest(in);if(!serviceId.equals(r.serviceId)||!same(token,r.token))throw new IOException("Display service authentication failed");synchronized(display){if(r.op==DisplayProtocol.Operation.CLEAR)display.clearVisor();else display.writeVisor(r.animation,r.line1,r.line2);}DisplayProtocol.writeResponse(out,true,"OK");}catch(Exception e){LOG.log(Level.WARNING,"Rejected/failed display operation from {0}: {1}",new Object[]{c.getRemoteSocketAddress(),safe(e)});try{DisplayProtocol.writeResponse(out,false,safe(e));}catch(IOException ignored){}}}catch(IOException e){LOG.log(Level.FINE,"Display client connection ended",e);}}
    @Override public void close()throws IOException{running=false;server.close();clients.shutdownNow();}
    private static final class LocalHardwareProperties implements AppProperties{private final AppProperties d;LocalHardwareProperties(AppProperties d){this.d=d;}@Override public java.io.File getConfigFile(){return d.getConfigFile();}@Override public String getHost(){return d.getHost();}@Override public String getProperty(String k){if(DisplayServiceConfig.DEVICE_SERVICE_KEY.equals(k))return null;if("machine.fiscalprinter".equals(k)||"machine.printer".equals(k)||k.matches("machine\\.printer\\.[2-6]"))return "Not defined";if("screen.ticket.enabled".equals(k)||"digital.ticket.enabled".equals(k))return "false";return d.getProperty(k);}}
    private static boolean same(String a,String b){return MessageDigest.isEqual((a==null?"":a).getBytes(StandardCharsets.UTF_8),(b==null?"":b).getBytes(StandardCharsets.UTF_8));}
    private static String required(String v,String k){if(v==null||v.trim().isEmpty())throw new IllegalArgumentException("Missing "+k);return v.trim();}
    private static String safe(Exception e){String m=e.getMessage();return m==null||m.trim().isEmpty()?e.getClass().getSimpleName():m;}
}
