//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.display.service;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

final class DisplayProtocol {
    static final int MAGIC=0x4B474453; // KGDS
    static final int VERSION=1,MAX_STRING_BYTES=65535;
    enum Operation{WRITE,CLEAR}
    private DisplayProtocol(){}
    static void writeRequest(DataOutputStream out,String service,String token,Operation op,int animation,String line1,String line2)throws IOException{out.writeInt(MAGIC);out.writeInt(VERSION);write(out,service);write(out,token);out.writeByte(op.ordinal());out.writeInt(animation);write(out,line1);write(out,line2);out.flush();}
    static Request readRequest(DataInputStream in)throws IOException{check(in);String s=read(in),t=read(in);int o=in.readUnsignedByte();Operation[] ops=Operation.values();if(o>=ops.length)throw new IOException("Invalid display operation");int a=in.readInt();return new Request(s,t,ops[o],a,read(in),read(in));}
    static void writeResponse(DataOutputStream out,boolean ok,String msg)throws IOException{out.writeInt(MAGIC);out.writeInt(VERSION);out.writeBoolean(ok);write(out,msg==null?"":msg);out.flush();}
    static Response readResponse(DataInputStream in)throws IOException{check(in);return new Response(in.readBoolean(),read(in));}
    private static void check(DataInputStream in)throws IOException{if(in.readInt()!=MAGIC)throw new IOException("Invalid display protocol magic");int v=in.readInt();if(v!=VERSION)throw new IOException("Unsupported display protocol version: "+v);}
    private static void write(DataOutputStream out,String v)throws IOException{byte[] b=(v==null?"":v).getBytes(StandardCharsets.UTF_8);if(b.length>MAX_STRING_BYTES)throw new IOException("Display field too long");out.writeInt(b.length);out.write(b);}
    private static String read(DataInputStream in)throws IOException{int n=in.readInt();if(n<0||n>MAX_STRING_BYTES)throw new IOException("Invalid display field length");byte[] b=new byte[n];in.readFully(b);return new String(b,StandardCharsets.UTF_8);}
    static final class Request{final String serviceId,token,line1,line2;final Operation op;final int animation;Request(String s,String t,Operation op,int a,String l1,String l2){serviceId=s;token=t;this.op=op;animation=a;line1=l1;line2=l2;}}
    static final class Response{final boolean ok;final String message;Response(boolean ok,String msg){this.ok=ok;message=msg;}}
}
