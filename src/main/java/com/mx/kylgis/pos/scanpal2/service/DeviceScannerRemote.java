//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.scanpal2.service;

import com.mx.kylgis.pos.scanpal2.DeviceScanner;
import com.mx.kylgis.pos.scanpal2.DeviceScannerException;
import com.mx.kylgis.pos.scanpal2.ProductDownloaded;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.UUID;

public final class DeviceScannerRemote implements DeviceScanner {
    private final ScannerServiceConfig.Endpoint endpoint;
    private final String sessionId=UUID.randomUUID().toString();
    public DeviceScannerRemote(ScannerServiceConfig.Endpoint e){if(e==null)throw new IllegalArgumentException("endpoint is required");endpoint=e;}
    @Override public void connectDevice()throws DeviceScannerException{call(ScannerProtocol.Operation.CONNECT,null,null,null);}
    @Override public void disconnectDevice(){try{call(ScannerProtocol.Operation.DISCONNECT,null,null,null);}catch(DeviceScannerException ignored){}}
    @Override public void startDownloadProduct()throws DeviceScannerException{call(ScannerProtocol.Operation.START_DOWNLOAD,null,null,null);}
    @Override public ProductDownloaded recieveProduct()throws DeviceScannerException{
        ScannerProtocol.Response r=call(ScannerProtocol.Operation.RECEIVE_PRODUCT,null,null,null);if(r.code==null)return null;
        ProductDownloaded p=new ProductDownloaded();p.setCode(r.code);p.setQuantity(r.quantity==null?0:r.quantity);return p;
    }
    @Override public void startUploadProduct()throws DeviceScannerException{call(ScannerProtocol.Operation.START_UPLOAD,null,null,null);}
    @Override public void sendProduct(String name,String code,Double price)throws DeviceScannerException{call(ScannerProtocol.Operation.SEND_PRODUCT,name,code,price);}
    @Override public void stopUploadProduct()throws DeviceScannerException{call(ScannerProtocol.Operation.STOP_UPLOAD,null,null,null);}
    private ScannerProtocol.Response call(ScannerProtocol.Operation op,String name,String code,Double price)throws DeviceScannerException{
        try(Socket socket=new Socket()){
            socket.connect(new InetSocketAddress(endpoint.getHost(),endpoint.getPort()),endpoint.getConnectTimeoutMs());socket.setSoTimeout(endpoint.getReadTimeoutMs());
            DataOutputStream out=new DataOutputStream(socket.getOutputStream());DataInputStream in=new DataInputStream(socket.getInputStream());
            ScannerProtocol.writeRequest(out,endpoint.getServiceId(),endpoint.getToken(),sessionId,op,name,code,price);
            ScannerProtocol.Response r=ScannerProtocol.readResponse(in);if(!r.ok)throw new IOException(r.message);return r;
        }catch(IOException e){throw new DeviceScannerException("KylGis scanner service "+endpoint.getServiceId()+" failed: "+e.getMessage(),e);}
    }
}
