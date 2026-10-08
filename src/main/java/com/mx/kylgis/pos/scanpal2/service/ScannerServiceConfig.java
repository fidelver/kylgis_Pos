//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.scanpal2.service;

import com.mx.kylgis.pos.forms.AppProperties;

public final class ScannerServiceConfig {
    public static final int DEFAULT_PORT = 19103;
    public static final String DEVICE_SERVICE_KEY = "device.scanner.service";
    private ScannerServiceConfig() { }

    public static String getServiceId(AppProperties p) {
        return trim(p == null ? null : p.getProperty(DEVICE_SERVICE_KEY));
    }
    public static String prefix(String id) { return "scanner.service." + normalize(id) + "."; }
    public static Endpoint endpoint(AppProperties p, String id) {
        if (p == null) throw new IllegalArgumentException("properties are required");
        String sid = normalize(id), prefix = prefix(sid);
        String host = trim(p.getProperty(prefix+"host")); if (host == null) host="127.0.0.1";
        int port = number(p.getProperty(prefix+"port"), DEFAULT_PORT, 1, 65535);
        int connect = number(p.getProperty(prefix+"connect.timeout.ms"), 2500, 100, 120000);
        int read = number(p.getProperty(prefix+"read.timeout.ms"), 10000, 100, 120000);
        String token = trim(p.getProperty(prefix+"token"));
        if (token == null) throw new IllegalArgumentException("Missing " + prefix + "token");
        return new Endpoint(sid,host,port,token,connect,read);
    }
    public static String bindAddress(AppProperties p, String id) {
        String v=trim(p.getProperty(prefix(id)+"bind")); return v==null?"127.0.0.1":v;
    }
    private static int number(String v,int d,int min,int max) {
        String x=trim(v); if(x==null)return d;
        try { int n=Integer.parseInt(x); if(n<min||n>max)throw new NumberFormatException(); return n; }
        catch(NumberFormatException e){throw new IllegalArgumentException("Invalid scanner service number: "+v,e);}
    }
    private static String normalize(String v) {
        String x=trim(v); if(x==null||!x.matches("[A-Za-z0-9._-]+"))throw new IllegalArgumentException("Invalid scanner service id: "+v); return x;
    }
    private static String trim(String v){if(v==null)return null;v=v.trim();return v.isEmpty()?null:v;}
    public static final class Endpoint {
        private final String id,host,token; private final int port,connect,read;
        private Endpoint(String id,String host,int port,String token,int connect,int read){this.id=id;this.host=host;this.port=port;this.token=token;this.connect=connect;this.read=read;}
        public String getServiceId(){return id;} public String getHost(){return host;} public int getPort(){return port;}
        public String getToken(){return token;} public int getConnectTimeoutMs(){return connect;} public int getReadTimeoutMs(){return read;}
    }
}
