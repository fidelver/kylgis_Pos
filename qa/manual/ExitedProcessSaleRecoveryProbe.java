import com.mx.kylgis.pos.forms.DataLogicSales;
import com.mx.kylgis.pos.data.loader.Session;
import com.mx.kylgis.pos.ticket.TicketInfo;
import com.mx.kylgis.pos.basic.BasicException;
import java.io.*;
import java.sql.*;
/** Recovery probe executed after the original QA JVM has fully exited. */
public final class ExitedProcessSaleRecoveryProbe {
  static void ck(boolean v,String s){if(!v)throw new AssertionError(s);System.out.println("PASS "+s);}
  public static void main(String[] args)throws Exception {
    if(!"ONLY_SYNTHETIC_POS_STATUS".equals(System.getenv("KYLGIS_QA_SCOPE")))
      throw new SecurityException("QA_SCOPE");
    TicketInfo ticket;
    try(ObjectInputStream in=new ObjectInputStream(new FileInputStream("/tmp/qa-home/ack-ticket.bin"))){
      ticket=(TicketInfo)in.readObject();
    }
    String op=ticket.getProperty("kylgis.qa.durableLedger.operationUuid");
    java.util.UUID.fromString(op);
    RestartedSaleLedgerProbe.main(new String[]{"isolated-restart-qa",op});
    System.setProperty("kylgis.qa.durableLedger.enabled","true");
    Session s=new Session(RealSaveTicketAckLossProbe.DIRECT,"root","");
    try {
      DataLogicSales logic=new DataLogicSales();logic.init(s);
      try {logic.saveTicket(ticket,"0");throw new AssertionError("RESTART_REAL_SAVE_ALLOWED");}
      catch(BasicException expected){
        ck(expected.getMessage().contains("preparation is not confirmed"),"EXITED_JVM_REAL_SAVE_BLOCKED");
      }
    } finally{s.close();}
    try(Connection c=DriverManager.getConnection(RealSaveTicketAckLossProbe.DIRECT,"root","")){
      ck("pos_qa_status".equals(c.getCatalog()),"EXITED_JVM_CATALOG");
      for(String sql:new String[]{
        "SELECT COUNT(*) FROM receipts WHERE id=?",
        "SELECT COUNT(*) FROM tickets WHERE id=?",
        "SELECT COUNT(*) FROM ticketlines WHERE ticket=?",
        "SELECT COUNT(*) FROM payments WHERE receipt=?",
        "SELECT COUNT(*) FROM voucher_redemptions WHERE receipt_id=?"
      }) ck(RealSaveTicketAckLossProbe.count(c,sql,ticket.getId())==1,"EXITED_JVM_EXACTLY_ONE_"+sql.split("FROM ")[1].split(" ")[0]);
      ck(RealSaveTicketAckLossProbe.count(c,"SELECT COUNT(*) FROM qa_sale_operation_intents WHERE operation_uuid=? AND receipt_id='"+ticket.getId()+"' AND state='COMMIT_UNKNOWN' AND revision=2",op)==1,"EXITED_JVM_IDENTITY_AND_REVISION");
      ck(RealSaveTicketAckLossProbe.count(c,"SELECT COUNT(*) FROM vouchers WHERE id=? AND status='D'","QA-ACK-ID")==1,"EXITED_JVM_VOUCHER_ONCE");
    }
    System.out.println("EXITED_FIRST_JVM_REAL_SAVE_RECOVERY_PASS");
  }
}
