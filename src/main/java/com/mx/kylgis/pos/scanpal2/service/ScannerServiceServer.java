//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.scanpal2.service;

import com.mx.kylgis.pos.forms.AppProperties;
import com.mx.kylgis.pos.scanpal2.DeviceScanner;
import com.mx.kylgis.pos.scanpal2.DeviceScannerException;
import com.mx.kylgis.pos.scanpal2.DeviceScannerFactory;
import com.mx.kylgis.pos.scanpal2.ProductDownloaded;
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

public final class ScannerServiceServer implements AutoCloseable {
    private static final Logger LOG=Logger.getLogger(ScannerServiceServer.class.getName());
    private static final long SESSION_TTL_MS=60000L;
    private final String serviceId,token;private final DeviceScanner scanner;private final ServerSocket server;
    private final ExecutorService clients=Executors.newFixedThreadPool(4,new ThreadFactory(){private int n;@Override public synchronized Thread newThread(Runnable r){Thread t=new Thread(r,"kylgis-scanner-client-"+(++n));t.setDaemon(true);return t;}});
    private volatile boolean running;private String activeSession;private long lastActivity;
    public ScannerServiceServer(AppProperties p)throws IOException{
        if(p==null)throw new IllegalArgumentException("properties are required");serviceId=required(p.getProperty("service.id"),"service.id");
        ScannerServiceConfig.Endpoint e=ScannerServiceConfig.endpoint(p,serviceId);token=e.getToken();scanner=DeviceScannerFactory.createInstance(new LocalHardwareProperties(p));
        if(scanner==null)throw new IllegalArgumentException("Scanner service has no local machine.scanner configured");
        server=new ServerSocket();server.setReuseAddress(true);server.bind(new InetSocketAddress(InetAddress.getByName(ScannerServiceConfig.bindAddress(p,serviceId)),e.getPort()),16);
    }
    public void serve()throws IOException{running=true;LOG.log(Level.INFO,"KylGis scanner service {0} listening on {1}",new Object[]{serviceId,server.getLocalSocketAddress()});while(running){try{final Socket s=server.accept();clients.submit(new Runnable(){@Override public void run(){handle(s);}});}catch(IOException e){if(running)throw e;}}}
    private void handle(Socket socket){try(Socket c=socket;DataInputStream in=new DataInputStream(c.getInputStream());DataOutputStream out=new DataOutputStream(c.getOutputStream())){c.setSoTimeout(15000);try{ScannerProtocol.Request r=ScannerProtocol.readRequest(in);if(!serviceId.equals(r.serviceId)||!same(token,r.token))throw new IOException("Scanner service authentication failed");ProductDownloaded product; synchronized(scanner){product=execute(r);}ScannerProtocol.writeResponse(out,true,product==null?null:product.getCode(),product==null?null:product.getQuantity(),"OK");}catch(Exception e){LOG.log(Level.WARNING,"Rejected/failed scanner operation from {0}: {1}",new Object[]{c.getRemoteSocketAddress(),safe(e)});try{ScannerProtocol.writeResponse(out,false,null,null,safe(e));}catch(IOException ignored){}}}catch(IOException e){LOG.log(Level.FINE,"Scanner client connection ended",e);}}
    private ProductDownloaded execute(ScannerProtocol.Request r)throws DeviceScannerException{
        long now=System.currentTimeMillis();
        if(r.op==ScannerProtocol.Operation.CONNECT){if(activeSession!=null&&!activeSession.equals(r.sessionId)){if(now-lastActivity<=SESSION_TTL_MS)throw new DeviceScannerException("Scanner is busy");try{scanner.disconnectDevice();}catch(Exception ignored){}}scanner.connectDevice();activeSession=r.sessionId;lastActivity=now;return null;}
        if(r.op==ScannerProtocol.Operation.DISCONNECT){if(activeSession==null||activeSession.equals(r.sessionId)){scanner.disconnectDevice();activeSession=null;}return null;}
        if(activeSession==null||!activeSession.equals(r.sessionId))throw new DeviceScannerException("Scanner session is not connected");lastActivity=now;
        switch(r.op){case START_DOWNLOAD:scanner.startDownloadProduct();return null;case RECEIVE_PRODUCT:return scanner.recieveProduct();case START_UPLOAD:scanner.startUploadProduct();return null;case SEND_PRODUCT:scanner.sendProduct(r.name,r.code,r.price);return null;case STOP_UPLOAD:scanner.stopUploadProduct();return null;default:throw new DeviceScannerException("Unsupported scanner operation");}
    }
    @Override public void close()throws IOException{running=false;try{scanner.disconnectDevice();}catch(Exception ignored){}server.close();clients.shutdownNow();}
    private static final class LocalHardwareProperties implements AppProperties{private final AppProperties d;LocalHardwareProperties(AppProperties d){this.d=d;}@Override public java.io.File getConfigFile(){return d.getConfigFile();}@Override public String getHost(){return d.getHost();}@Override public String getProperty(String k){return ScannerServiceConfig.DEVICE_SERVICE_KEY.equals(k)?null:d.getProperty(k);}}
    private static boolean same(String a,String b){return MessageDigest.isEqual((a==null?"":a).getBytes(StandardCharsets.UTF_8),(b==null?"":b).getBytes(StandardCharsets.UTF_8));}
    private static String required(String v,String k){if(v==null||v.trim().isEmpty())throw new IllegalArgumentException("Missing "+k);return v.trim();}
    private static String safe(Exception e){String m=e.getMessage();return m==null||m.trim().isEmpty()?e.getClass().getSimpleName():m;}
}
