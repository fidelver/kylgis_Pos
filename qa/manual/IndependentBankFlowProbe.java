import com.mx.kylgis.pos.forms.DataLogicSales;
import com.mx.kylgis.pos.data.loader.Session;
import com.mx.kylgis.pos.data.loader.TransactionOutcomeUnknownException;
import com.mx.kylgis.pos.ticket.*;
import com.mx.kylgis.pos.payment.*;
import com.mx.kylgis.pos.voucher.VoucherDiscountInfo;
import com.mx.kylgis.pos.basic.BasicException;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** QA coordinator, independent bank + real saveTicket. Never a commercial adapter. */
public final class IndependentBankFlowProbe {
  static final String URL=RealSaveTicketAckLossProbe.DIRECT;
  static final Path DIR=SyntheticBankService.DIR;
  static final String OP="kylgis.qa.durableLedger.operationUuid";
  static int checks;
  static void ck(boolean v,String s){if(!v)throw new AssertionError(s);checks++;System.out.println("PASS "+s);}
  static Connection db()throws Exception{
    Connection c=DriverManager.getConnection(URL,"root","");
    if(!"pos_qa_status".equals(c.getCatalog())){c.close();throw new SecurityException("QA_CATALOG");}return c;
  }
  static int update(String sql,String...args)throws Exception{
    try(Connection c=db();PreparedStatement p=c.prepareStatement(sql)){
      for(int i=0;i<args.length;i++)p.setString(i+1,args[i]);return p.executeUpdate();
    }
  }
  static String request(String query,int timeout)throws Exception{return request(query,timeout,Files.readString(DIR.resolve("token")));}
  static String request(String query,int timeout,String token)throws Exception{
    int port=Integer.parseInt(Files.readString(DIR.resolve("port")));
    try(Socket s=new Socket()){
      s.connect(new InetSocketAddress("127.0.0.1",port),1500);s.setSoTimeout(timeout);
      PrintWriter out=new PrintWriter(new OutputStreamWriter(s.getOutputStream(),StandardCharsets.UTF_8),true);
      out.println(token);out.println(query);
      return new BufferedReader(new InputStreamReader(s.getInputStream(),StandardCharsets.UTF_8)).readLine();
    }
  }
  static Process bank()throws Exception{
    Files.deleteIfExists(DIR.resolve("port"));
    Process p=new ProcessBuilder(System.getProperty("java.home")+"/bin/java","-Xmx96m","-Duser.home=/tmp/qa-home","-cp",System.getProperty("java.class.path"),"SyntheticBankService").inheritIO().start();
    long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
    while(!Files.exists(DIR.resolve("port"))){
      if(!p.isAlive()||System.nanoTime()>until){p.destroyForcibly();p.waitFor();throw new AssertionError("BANK_START_TIMEOUT");}Thread.sleep(20);
    }
    ck(p.pid()!=ProcessHandle.current().pid(),"BANK_INDEPENDENT_PROCESS");return p;
  }
  static void stop(Process p)throws Exception{
    if(p!=null&&p.isAlive()){p.destroyForcibly();ck(p.waitFor(5,TimeUnit.SECONDS),"BANK_OWNED_PROCESS_STOPPED");}
  }
  static TicketInfo sale(String label)throws Exception{
    TicketInfo t=new TicketInfo();t.setActiveCash("QA-CASH");t.setDate(new java.util.Date());t.setUser(new UserInfo("0","Administrator"));
    t.setProperty(OP,UUID.randomUUID().toString());
    t.addLine(new TicketLineInfo("xxx999_999xxx_x9x9x9",1,30,new TaxInfo("002","IVA 0%","001",null,null,0,false,0)));
    t.setVoucherDiscounts(List.of(new VoucherDiscountInfo("QA-BANK-"+label,"QA-BANK-"+label,null,30,20)));
    t.setPayments(List.of(new PaymentInfoCash(4,4),new PaymentInfoTicket(6,"magcard")));
    update("INSERT INTO vouchers(id,voucher_number,amount,status) VALUES(?,?,30,'A')","QA-BANK-"+label,"QA-BANK-"+label);
    update("INSERT INTO qa_sale_operation_intents(operation_uuid,receipt_id,terminal_id,state,request_sha256) VALUES(?,?,'QA-CASH','PREPARED',?)",t.getProperty(OP),t.getId(),com.mx.kylgis.pos.forms.SaleOperationPayloadQA.fingerprint(t));
    return t;
  }
  static String intent(TicketInfo t)throws Exception{
    String pay=UUID.randomUUID().toString();
    update("INSERT INTO qa_external_payment_intents(payment_uuid,operation_uuid,provider,amount,currency,state,component_index) VALUES(?,?,'SYNTHETIC_BANK',6,'MXN','PREPARED',1)",pay,t.getProperty(OP));
    return pay;
  }
  static boolean dispatch(String pay,String mode)throws Exception{
    // Persist UNKNOWN BEFORE dispatch. A second cashier never sends another AUTH.
    if(update("UPDATE qa_external_payment_intents SET state='UNKNOWN' WHERE payment_uuid=? AND state='PREPARED'",pay)!=1)return false;
    try{
      String response=request("CHARGE "+pay+" 600 MXN "+mode,300);
      if(response==null)throw new EOFException();
      applyResponse(pay,response);
    }catch(SocketTimeoutException|EOFException ambiguous){
      System.out.println("BANK_TIMEOUT_REMAINS_UNKNOWN");
    }
    return true;
  }
  static String applyResponse(String pay,String response)throws Exception{
    String[] a=response==null?new String[0]:response.split("\\|");
    if(a.length!=5||!a[0].equals(pay)||!a[1].equals("600")||!a[2].equals("MXN")||!Set.of("APPROVED","DECLINED","UNKNOWN").contains(a[3]))
      throw new SecurityException("BANK_EVIDENCE_IDENTITY_AMOUNT_CURRENCY_MISMATCH");
    if(!a[4].equals(a[3].equals("APPROVED")?"1":"0"))throw new SecurityException("BANK_CHARGE_COUNT_MISMATCH");
    update("UPDATE qa_external_payment_intents SET state=?,provider_reference=? WHERE payment_uuid=? AND state='UNKNOWN'",a[3],pay,pay);
    return a[3];
  }
  static void blocked(TicketInfo t)throws Exception{
    Session s=new Session(URL,"root","");
    try{
      DataLogicSales l=new DataLogicSales();l.init(s);
      try{l.saveTicket(t,"0");throw new AssertionError("BANK_UNRESOLVED_SALE_ALLOWED");}
      catch(BasicException expected){ck(expected.getMessage()!=null&&expected.getMessage().contains("preparation is not confirmed"),"BANK_PENDING_REAL_SAVE_BLOCKED");}
    }finally{s.close();}
  }
  static void immutableRejected(String sql,String...args)throws Exception{
    boolean rejected=false;
    try{update(sql,args);}catch(SQLException expected){rejected="45000".equals(expected.getSQLState());}
    ck(rejected,"BANK_IMMUTABLE_OR_RETAINED_REQUEST_REJECTED");
  }
  static void savedOnce(TicketInfo t)throws Exception{
    System.setProperty("kylgis.qa.durableLedger.directJdbcUrl",URL);
    try(RealSaveTicketAckLossProbe.AckDropProxy proxy=new RealSaveTicketAckLossProbe.AckDropProxy()){
      String proxied="jdbc:mysql://127.0.0.1:"+proxy.port()+"/pos_qa_status?useSSL=false&socketTimeout=5000";
      Session s=new Session(proxied,"root","");
      try{
        DataLogicSales l=new DataLogicSales();l.init(s);
        try{l.saveTicket(t,"0");throw new AssertionError("BANK_POS_ACK_NOT_LOST");}
        catch(TransactionOutcomeUnknownException expected){ck(proxy.serverOk.get(),"BANK_AUTH_AND_POS_COMMIT_OK_ACK_LOST");}
      }finally{try{s.close();}catch(Exception ignored){}}
    }
    blocked(t);
    try(Connection c=db()){
      for(String sql:new String[]{"SELECT COUNT(*) FROM receipts WHERE id=?","SELECT COUNT(*) FROM tickets WHERE id=?","SELECT COUNT(*) FROM voucher_redemptions WHERE receipt_id=?"})
        ck(RealSaveTicketAckLossProbe.count(c,sql,t.getId())==1,"BANK_REAL_ONE_"+sql.split("FROM ")[1].split(" ")[0]);
      ck(RealSaveTicketAckLossProbe.count(c,"SELECT COUNT(*) FROM payments WHERE receipt=?",t.getId())==2,"BANK_REAL_TWO_MIXED_COMPONENTS");
      ck(RealSaveTicketAckLossProbe.count(c,"SELECT COUNT(*) FROM payments p JOIN qa_external_payment_intents i ON p.ID=i.payment_uuid WHERE p.receipt=? AND i.component_index=1 AND i.provider_reference IS NOT NULL",t.getId())==1,"BANK_PROVIDER_COMPONENT_ID_EQUALS_REAL_PAYMENT_ID");
      try(PreparedStatement p=c.prepareStatement("SELECT SUM(total) FROM payments WHERE receipt=?")){
        p.setString(1,t.getId());try(ResultSet r=p.executeQuery()){r.next();ck(r.getBigDecimal(1).compareTo(new java.math.BigDecimal("10.00"))==0,"BANK_MIXED_AMOUNT_EXACT");}
      }
    }
    System.clearProperty("kylgis.qa.durableLedger.directJdbcUrl");
  }
  static void child(String pay,String mode,String box)throws Exception{
    Files.writeString(DIR.resolve(box+".ready"),Long.toString(ProcessHandle.current().pid()),StandardOpenOption.CREATE_NEW);
    long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
    while(!(Files.exists(DIR.resolve("a.ready"))&&Files.exists(DIR.resolve("b.ready")))){
      if(System.nanoTime()>deadline)throw new AssertionError("BANK_CASHIER_BARRIER_TIMEOUT");
      Thread.sleep(10);
    }
    boolean sent=dispatch(pay,mode);
    Files.writeString(DIR.resolve(box+".result"),sent?"SENT":"BLOCKED",StandardOpenOption.CREATE_NEW);
  }
  public static void main(String[] args)throws Exception{
    if(!"ONLY_SYNTHETIC_POS_STATUS".equals(System.getenv("KYLGIS_QA_SCOPE")))throw new SecurityException("QA_SCOPE");
    Class.forName("com.mysql.jdbc.Driver");
    if(args.length==3){child(args[0],args[1],args[2]);return;}
    System.setProperty("kylgis.qa.durableLedger.enabled","true");
    Files.createDirectory(DIR);Files.writeString(DIR.resolve("token"),UUID.randomUUID().toString(),StandardOpenOption.CREATE_NEW);
    Process service=null;
    try {
      service=bank();
      for(String mode:List.of("APPROVED","DECLINED","UNKNOWN","LOST_ACK")){
        TicketInfo t=sale(mode);String pay=intent(t);
        ck("UNAUTHORIZED".equals(request("QUERY "+pay,1000,"invalid-fixture-token")),"BANK_AUTH_REJECTS_WRONG_TOKEN");
        blocked(t);
        ck("NOT_FOUND".equals(request("QUERY "+pay,1000)),"BANK_NO_CHARGE_BEFORE_DURABLE_DISPATCH_"+mode);
        TicketInfo conflicting=t.copyTicket();
        conflicting.getProperties().remove(TicketInfo.PAYMENT_RECONCILIATION_PROPERTY); // QA: test ledger binding independently
        ck(!conflicting.getId().equals(t.getId()),"BANK_CONFLICT_FIXTURE_DIFFERENT_RECEIPT");
        blocked(conflicting);
        t.setActiveCash("QA-OTHER-TERMINAL");
        blocked(t);
        t.setActiveCash("QA-CASH");
        ck(dispatch(pay,mode),"BANK_FIRST_DISPATCH_"+mode);
        ck(!dispatch(pay,mode),"BANK_NO_SECOND_DISPATCH_"+mode);
        if(mode.equals("LOST_ACK")){
          try(Connection c=db()){ck(RealSaveTicketAckLossProbe.count(c,"SELECT COUNT(*) FROM qa_external_payment_intents WHERE payment_uuid=? AND state='UNKNOWN'",pay)==1,"BANK_TIMEOUT_DURABLE_UNKNOWN");}
          blocked(t);
        }
        String state=applyResponse(pay,request("QUERY "+pay,2500));
        ck(state.equals(mode.equals("LOST_ACK")?"APPROVED":mode),"BANK_RECONCILIATION_"+mode);
        if(state.equals("APPROVED")){
          t.setPayments(List.of(new PaymentInfoCash(4,4),new PaymentInfoTicket(7,"magcard")));
          blocked(t); // same UUID/receipt cannot authorize changed amount
          t.setPayments(List.of(new PaymentInfoCash(4,4),new PaymentInfoTicket(6,"magcard")));
          immutableRejected("UPDATE qa_external_payment_intents SET amount=7 WHERE payment_uuid=?",pay);
          immutableRejected("UPDATE qa_external_payment_intents SET currency='USD' WHERE payment_uuid=?",pay);
          immutableRejected("UPDATE qa_external_payment_intents SET state='UNKNOWN' WHERE payment_uuid=?",pay);
          immutableRejected("UPDATE qa_external_payment_intents SET component_index=0 WHERE payment_uuid=?",pay);
          immutableRejected("UPDATE qa_external_payment_intents SET provider_reference='OTHER' WHERE payment_uuid=?",pay);
          immutableRejected("UPDATE qa_sale_operation_intents SET request_sha256=REPEAT('0',64) WHERE operation_uuid=?",t.getProperty(OP));
          immutableRejected("UPDATE qa_sale_operation_intents SET receipt_id='OTHER' WHERE operation_uuid=?",t.getProperty(OP));
          immutableRejected("DELETE FROM qa_external_payment_intents WHERE payment_uuid=?",pay);
          immutableRejected("DELETE FROM qa_sale_operation_intents WHERE operation_uuid=?",t.getProperty(OP));
          savedOnce(t);
        }else blocked(t);
        String same=request("CHARGE "+pay+" 600 MXN "+mode,2500);
        ck(same.equals(request("QUERY "+pay,2500)),"BANK_SAME_KEY_NO_SECOND_CHARGE_"+mode);
        ck("CONFLICT".equals(request("CHARGE "+pay+" 601 MXN "+mode,2500)),"BANK_KEY_AMOUNT_CONFLICT_"+mode);
        if(mode.equals("LOST_ACK")){
          stop(service);service=null;
          boolean unavailable=false;
          try{request("QUERY "+pay,800);}catch(IOException expected){unavailable=true;}
          ck(unavailable,"BANK_SERVICE_OUTAGE_OBSERVED");
          try(Connection c=db()){ck(c.isValid(2),"BANK_OUTAGE_POS_DB_REMAINS_AVAILABLE");}
          blocked(t);
          ck(!dispatch(pay,mode),"BANK_OUTAGE_CANNOT_REDISPATCH");
          service=bank();
          ck(request("QUERY "+pay,2500).equals(same),"BANK_RESTART_PRESERVES_AUTHORIZATION");
          blocked(t);
        }
      }
      TicketInfo race=sale("RACE");String pay=intent(race);
      Process a=null,b=null;
      try{
        a=new ProcessBuilder(System.getProperty("java.home")+"/bin/java","-Xmx96m","-Duser.home=/tmp/qa-home","-cp",System.getProperty("java.class.path"),"IndependentBankFlowProbe",pay,"APPROVED","a").inheritIO().start();
        b=new ProcessBuilder(System.getProperty("java.home")+"/bin/java","-Xmx96m","-Duser.home=/tmp/qa-home","-cp",System.getProperty("java.class.path"),"IndependentBankFlowProbe",pay,"APPROVED","b").inheritIO().start();
        ck(a.pid()!=b.pid()&&a.pid()!=service.pid()&&b.pid()!=service.pid(),"BANK_TWO_CASHIERS_DISTINCT_JVMS");
        ck(a.waitFor(15,TimeUnit.SECONDS)&&b.waitFor(15,TimeUnit.SECONDS),"BANK_CASHIERS_BOUNDED");
        ck(a.exitValue()==0&&b.exitValue()==0,"BANK_CASHIERS_RC_ZERO");
      }finally{for(Process p:new Process[]{a,b})stop(p);}
      List<String> results=List.of(Files.readString(DIR.resolve("a.result")),Files.readString(DIR.resolve("b.result")));
      ck(Collections.frequency(results,"SENT")==1&&Collections.frequency(results,"BLOCKED")==1,"BANK_TWO_JVMS_ONE_DISPATCH");
      ck(request("QUERY "+pay,2500).endsWith("|APPROVED|1"),"BANK_TWO_JVMS_ONE_CHARGE");
      savedOnce(race);
      System.out.println("INDEPENDENT_BANK_REAL_POS_FLOW_PASS checks="+checks);
    }finally{stop(service);}
  }
}
