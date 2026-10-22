import com.mx.kylgis.pos.forms.DataLogicSales;
import com.mx.kylgis.pos.data.loader.Session;
import com.mx.kylgis.pos.ticket.*;
import com.mx.kylgis.pos.payment.PaymentInfoCash;
import com.mx.kylgis.pos.voucher.VoucherDiscountInfo;
import com.mx.kylgis.pos.basic.BasicException;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Two independent cashier JVMs call real saveTicket against one operation. */
public final class IndependentCashboxesProbe {
  static final Path DIR=Path.of("/tmp/qa-home/cashboxes");
  static final String URL=RealSaveTicketAckLossProbe.DIRECT;
  static void ck(boolean ok,String label){if(!ok)throw new AssertionError(label);System.out.println("PASS "+label);}
  static TicketInfo load()throws Exception{
    try(ObjectInputStream in=new ObjectInputStream(Files.newInputStream(DIR.resolve("ticket.bin")))){
      TicketInfo t=(TicketInfo)in.readObject();
      // Explicit synthetic reconstruction. Does not certify Swing suspend/restore.
      t.setActiveCash("QA-CASH");
      t.setUser(new UserInfo("0","Administrator"));
      t.setDate(new java.util.Date());
      t.setPayments(List.of(new PaymentInfoCash(10,10)));
      t.setVoucherDiscounts(List.of(new VoucherDiscountInfo("QA-JVM-ID","QA-JVM-NUM",null,30,20)));
      return t;
    }
  }
  static void worker(String box)throws Exception{
    TicketInfo t=load();
    Files.writeString(DIR.resolve(box+".ready"),Long.toString(ProcessHandle.current().pid()),StandardOpenOption.CREATE_NEW);
    long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(15);
    while(!(Files.exists(DIR.resolve("a.ready"))&&Files.exists(DIR.resolve("b.ready")))){
      if(System.nanoTime()>deadline)throw new AssertionError("CASHBOX_BARRIER_TIMEOUT");
      Thread.sleep(20);
    }
    System.setProperty("kylgis.qa.durableLedger.enabled","true");
    Session s=new Session(URL,"root","");
    String result;
    try {
      DataLogicSales logic=new DataLogicSales();logic.init(s);
      try {logic.saveTicket(t,"0");result="COMMITTED";}
      catch(BasicException blocked){
        if(blocked.getMessage()==null||!blocked.getMessage().contains("preparation is not confirmed"))throw blocked;
        result="LEDGER_BLOCKED";
      }
    } finally{s.close();}
    Files.writeString(DIR.resolve(box+".result"),result,StandardOpenOption.CREATE_NEW);
    System.out.println("CASHBOX_"+box+"_"+result);
  }
  public static void main(String[] args)throws Exception{
    if(!"ONLY_SYNTHETIC_POS_STATUS".equals(System.getenv("KYLGIS_QA_SCOPE")))throw new SecurityException("QA_SCOPE");
    if(args.length==1 && (args[0].equals("a")||args[0].equals("b"))){worker(args[0]);return;}
    ck(args.length==0,"CASHBOX_PARENT_ARGUMENTS");
    Class.forName("com.mysql.jdbc.Driver");
    Files.createDirectory(DIR);
    TicketInfo t=new TicketInfo();
    t.setProperty("kylgis.qa.durableLedger.operationUuid",UUID.randomUUID().toString());
    t.addLine(new TicketLineInfo("xxx999_999xxx_x9x9x9",1,30,new TaxInfo("002","IVA 0%","001",null,null,0,false,0)));
    try(ObjectOutputStream out=new ObjectOutputStream(Files.newOutputStream(DIR.resolve("ticket.bin")))){out.writeObject(t);}
    try(Connection c=DriverManager.getConnection(URL,"root","");Statement st=c.createStatement()){
      ck("pos_qa_status".equals(c.getCatalog()),"CASHBOX_QA_CATALOG");
      st.executeUpdate("INSERT INTO vouchers(id,voucher_number,amount,status) VALUES('QA-JVM-ID','QA-JVM-NUM',30,'A')");
    }
    Process a=null,b=null;
    try {
      a=new ProcessBuilder(System.getProperty("java.home")+"/bin/java","-Xmx128m","-Duser.home=/tmp/qa-home","-cp",System.getProperty("java.class.path"),"IndependentCashboxesProbe","a").inheritIO().start();
      b=new ProcessBuilder(System.getProperty("java.home")+"/bin/java","-Xmx128m","-Duser.home=/tmp/qa-home","-cp",System.getProperty("java.class.path"),"IndependentCashboxesProbe","b").inheritIO().start();
      ck(a.pid()!=b.pid()&&a.pid()!=ProcessHandle.current().pid(),"CASHBOX_DISTINCT_JVM_PIDS");
      ck(a.waitFor(30,TimeUnit.SECONDS)&&b.waitFor(30,TimeUnit.SECONDS),"CASHBOX_BOUNDED_COMPLETION");
      ck(a.exitValue()==0&&b.exitValue()==0,"CASHBOX_PROCESSES_RC_ZERO");
    } finally {
      for(Process child:new Process[]{a,b})if(child!=null&&child.isAlive()){
        child.destroyForcibly();child.waitFor(5,TimeUnit.SECONDS);
      }
    }
    List<String> results=List.of(Files.readString(DIR.resolve("a.result")),Files.readString(DIR.resolve("b.result")));
    ck(Collections.frequency(results,"COMMITTED")==1&&Collections.frequency(results,"LEDGER_BLOCKED")==1,"CASHBOX_EXACTLY_ONE_REAL_SAVE");
    try(Connection c=DriverManager.getConnection(URL,"root","")){
      for(String sql:new String[]{
        "SELECT COUNT(*) FROM receipts WHERE id=?",
        "SELECT COUNT(*) FROM tickets WHERE id=?",
        "SELECT COUNT(*) FROM ticketlines WHERE ticket=?",
        "SELECT COUNT(*) FROM payments WHERE receipt=?",
        "SELECT COUNT(*) FROM voucher_redemptions WHERE receipt_id=?"
      })ck(RealSaveTicketAckLossProbe.count(c,sql,t.getId())==1,"CASHBOX_ONE_"+sql.split("FROM ")[1].split(" ")[0]);
      ck(RealSaveTicketAckLossProbe.count(c,"SELECT COUNT(*) FROM qa_sale_operation_intents WHERE operation_uuid=? AND state='DB_CONFIRMED' AND revision=2",t.getProperty("kylgis.qa.durableLedger.operationUuid"))==1,"CASHBOX_LEDGER_CONFIRMED");
      ck(RealSaveTicketAckLossProbe.count(c,"SELECT COUNT(*) FROM qa_sale_operation_intents WHERE receipt_id=?",t.getId())==1,"CASHBOX_ONE_DURABLE_OPERATION");
      ck(RealSaveTicketAckLossProbe.count(c,"SELECT COUNT(*) FROM vouchers WHERE id=? AND status='D'","QA-JVM-ID")==1,"CASHBOX_VOUCHER_ONCE");
    }
    System.out.println("TWO_INDEPENDENT_CASHBOX_JVMS_REAL_SAVE_PASS");
  }
}
