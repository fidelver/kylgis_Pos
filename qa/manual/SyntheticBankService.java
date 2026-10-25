import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Private synthetic bank service. Independent process; loopback in network-none QA. */
public final class SyntheticBankService {
  static final Path DIR=Path.of("/tmp/qa-home/bank-service");
  static void persist(Path p,String value)throws IOException{
    try(FileChannel ch=FileChannel.open(p,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)){
      ByteBuffer b=StandardCharsets.UTF_8.encode(value);
      while(b.hasRemaining())ch.write(b);
      ch.force(true);
    }
    try(FileChannel ch=FileChannel.open(DIR,StandardOpenOption.READ)){ch.force(true);}
  }
  public static void main(String[] args)throws Exception{
    if(!"ONLY_SYNTHETIC_POS_STATUS".equals(System.getenv("KYLGIS_QA_SCOPE")))throw new SecurityException("QA_SCOPE");
    String token=Files.readString(DIR.resolve("token"));
    try(ServerSocket server=new ServerSocket(0,8,InetAddress.getByName("127.0.0.1"))){
      Files.writeString(DIR.resolve("port"),Integer.toString(server.getLocalPort()));
      System.out.println("SYNTHETIC_BANK_SERVICE_READY pid="+ProcessHandle.current().pid());
      while(true){
        try(Socket s=server.accept()){
          s.setSoTimeout(2500);
          BufferedReader in=new BufferedReader(new InputStreamReader(s.getInputStream(),StandardCharsets.UTF_8));
          PrintWriter out=new PrintWriter(new OutputStreamWriter(s.getOutputStream(),StandardCharsets.UTF_8),true);
          if(!token.equals(in.readLine())){out.println("UNAUTHORIZED");continue;}
          String line=in.readLine();
          if(line==null||line.length()>256){out.println("INVALID");continue;}
          String[] a=line.split(" ");
          if(a.length<2){out.println("INVALID");continue;}
          UUID.fromString(a[1]);
          Path record=DIR.resolve(a[1]+".record");
          if(a[0].equals("QUERY")&&a.length==2){
            out.println(Files.exists(record)?Files.readString(record):"NOT_FOUND");continue;
          }
          if(!a[0].equals("CHARGE")||a.length!=5||!a[2].matches("[1-9][0-9]{0,8}")||!a[3].equals("MXN")||!Set.of("APPROVED","DECLINED","UNKNOWN","LOST_ACK").contains(a[4])){
            out.println("INVALID");continue;
          }
          if(Files.exists(record)){
            String prior=Files.readString(record);
            if(!prior.startsWith(a[1]+"|"+a[2]+"|"+a[3]+"|"))out.println("CONFLICT");
            else out.println(prior);
            continue;
          }
          String state=a[4].equals("LOST_ACK")?"APPROVED":a[4];
          String result=a[1]+"|"+a[2]+"|"+a[3]+"|"+state+"|"+(state.equals("APPROVED")?1:0);
          persist(record,result);
          if(a[4].equals("LOST_ACK")){
            Thread.sleep(1000); // authorization durable, client times out before any response
          } else out.println(result);
        } catch(IOException|IllegalArgumentException ignored) {
          // No payload, token or exception details enter service logs.
        }
      }
    }
  }
}
