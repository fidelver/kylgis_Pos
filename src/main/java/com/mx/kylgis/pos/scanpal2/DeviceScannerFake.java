//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.scanpal2;

/** In-memory scanner used only for diagnostics/tests. */
public final class DeviceScannerFake implements DeviceScanner {
    private boolean connected;
    private boolean emitted;
    private void require() throws DeviceScannerException { if (!connected) throw new DeviceScannerException("Fake scanner not connected"); }
    @Override public void connectDevice() { connected=true; }
    @Override public void disconnectDevice() { connected=false; }
    @Override public void startDownloadProduct() throws DeviceScannerException { require(); emitted=false; }
    @Override public ProductDownloaded recieveProduct() throws DeviceScannerException {
        require(); if (emitted) return null; emitted=true;
        ProductDownloaded p=new ProductDownloaded();p.setCode("TEST-001");p.setQuantity(1.25);return p;
    }
    @Override public void startUploadProduct() throws DeviceScannerException { require(); }
    @Override public void sendProduct(String name,String code,Double price) throws DeviceScannerException { require(); }
    @Override public void stopUploadProduct() throws DeviceScannerException { require(); }
}
