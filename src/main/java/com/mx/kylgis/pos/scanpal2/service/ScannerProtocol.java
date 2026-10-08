//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.scanpal2.service;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

final class ScannerProtocol {
    static final int MAGIC=0x4B47534E; // KGSN
    static final int VERSION=1, MAX_STRING_BYTES=65535;
    enum Operation { CONNECT, DISCONNECT, START_DOWNLOAD, RECEIVE_PRODUCT, START_UPLOAD, SEND_PRODUCT, STOP_UPLOAD }
    private ScannerProtocol() { }

    static void writeRequest(DataOutputStream out,String service,String token,String session,
            Operation op,String name,String code,Double price)throws IOException{
        out.writeInt(MAGIC);out.writeInt(VERSION);write(out,service);write(out,token);write(out,session);
        out.writeByte(op.ordinal());write(out,name==null?"":name);write(out,code==null?"":code);
        out.writeBoolean(price!=null);if(price!=null)out.writeDouble(price);out.flush();
    }
    static Request readRequest(DataInputStream in)throws IOException{
        check(in);String s=read(in),t=read(in),session=read(in);int o=in.readUnsignedByte();
        Operation[] ops=Operation.values();if(o>=ops.length)throw new IOException("Invalid scanner operation");
        String name=read(in),code=read(in);Double price=in.readBoolean()?in.readDouble():null;
        return new Request(s,t,session,ops[o],name,code,price);
    }
    static void writeResponse(DataOutputStream out,boolean ok,String code,Double qty,String msg)throws IOException{
        out.writeInt(MAGIC);out.writeInt(VERSION);out.writeBoolean(ok);out.writeBoolean(code!=null);
        if(code!=null){write(out,code);out.writeDouble(qty==null?0.0:qty);}write(out,msg==null?"":msg);out.flush();
    }
    static Response readResponse(DataInputStream in)throws IOException{
        check(in);boolean ok=in.readBoolean(),has=in.readBoolean();String code=null;Double qty=null;
        if(has){code=read(in);qty=in.readDouble();}return new Response(ok,code,qty,read(in));
    }
    private static void check(DataInputStream in)throws IOException{if(in.readInt()!=MAGIC)throw new IOException("Invalid scanner protocol magic");int v=in.readInt();if(v!=VERSION)throw new IOException("Unsupported scanner protocol version: "+v);}
    private static void write(DataOutputStream out,String v)throws IOException{byte[] b=(v==null?"":v).getBytes(StandardCharsets.UTF_8);if(b.length>MAX_STRING_BYTES)throw new IOException("Scanner field too long");out.writeInt(b.length);out.write(b);}
    private static String read(DataInputStream in)throws IOException{int n=in.readInt();if(n<0||n>MAX_STRING_BYTES)throw new IOException("Invalid scanner field length");byte[] b=new byte[n];in.readFully(b);return new String(b,StandardCharsets.UTF_8);}
    static final class Request {final String serviceId,token,sessionId,name,code;final Operation op;final Double price;Request(String s,String t,String session,Operation op,String name,String code,Double price){serviceId=s;token=t;sessionId=session;this.op=op;this.name=name;this.code=code;this.price=price;}}
    static final class Response {final boolean ok;final String code,message;final Double quantity;Response(boolean ok,String code,Double q,String m){this.ok=ok;this.code=code;quantity=q;message=m;}}
}
