//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.display.service;

import com.mx.kylgis.pos.printer.DeviceDisplay;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import javax.swing.JComponent;

public final class DeviceDisplayRemote implements DeviceDisplay {
    private final DisplayServiceConfig.Endpoint endpoint;
    public DeviceDisplayRemote(DisplayServiceConfig.Endpoint e){if(e==null)throw new IllegalArgumentException("endpoint is required");endpoint=e;}
    @Override public String getDisplayName(){return "KylGis Display Service "+endpoint.getServiceId();}
    @Override public String getDisplayDescription(){return endpoint.getServiceId()+"@"+endpoint.getHost()+":"+endpoint.getPort();}
    @Override public JComponent getDisplayComponent(){return null;}
    @Override public void writeVisor(int animation,String l1,String l2){send(DisplayProtocol.Operation.WRITE,animation,l1,l2);}
    @Override public void writeVisor(String l1,String l2){send(DisplayProtocol.Operation.WRITE,0,l1,l2);}
    @Override public void clearVisor(){send(DisplayProtocol.Operation.CLEAR,0,"","");}
    private void send(DisplayProtocol.Operation op,int animation,String l1,String l2){try(Socket socket=new Socket()){socket.connect(new InetSocketAddress(endpoint.getHost(),endpoint.getPort()),endpoint.getConnectTimeoutMs());socket.setSoTimeout(endpoint.getReadTimeoutMs());DataOutputStream out=new DataOutputStream(socket.getOutputStream());DataInputStream in=new DataInputStream(socket.getInputStream());DisplayProtocol.writeRequest(out,endpoint.getServiceId(),endpoint.getToken(),op,animation,l1,l2);DisplayProtocol.Response r=DisplayProtocol.readResponse(in);if(!r.ok)throw new IOException(r.message);}catch(IOException e){throw new IllegalStateException("KylGis display service "+endpoint.getServiceId()+" failed: "+e.getMessage(),e);}}
}
