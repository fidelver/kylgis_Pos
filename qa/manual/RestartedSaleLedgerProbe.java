import java.sql.*;
/** Independent JVM after COMMIT ACK loss. QA synthetic catalog only. */
public final class RestartedSaleLedgerProbe {
  static void check(boolean value,String label){if(!value)throw new AssertionError(label);System.out.println("PASS "+label);}
  public static void main(String[] args)throws Exception{
    if(args.length!=2 || !"isolated-restart-qa".equals(args[0]))throw new SecurityException("QA_SCOPE");
    String id=args[1];
    java.util.UUID.fromString(id);
    String url="jdbc:mysql://127.0.0.1:3306/pos_qa_status?useSSL=false&connectTimeout=2500";
    Class.forName("com.mysql.jdbc.Driver");
    try(Connection c=DriverManager.getConnection(url,"root","")){
      check("pos_qa_status".equals(c.getCatalog()),"RESTART_QA_CATALOG");
      String receipt;
      try(PreparedStatement p=c.prepareStatement("SELECT receipt_id,state,revision FROM qa_sale_operation_intents WHERE operation_uuid=?")){
        p.setString(1,id);
        try(ResultSet r=p.executeQuery()){
          check(r.next(),"RESTART_OPERATION_FOUND");
          receipt=r.getString(1);
          check("COMMIT_UNKNOWN".equals(r.getString(2)) && r.getInt(3)==2,"RESTART_STILL_UNKNOWN");
          check(!r.next(),"RESTART_SINGLE_OPERATION");
        }
      }
      try(PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM receipts WHERE id=?")){
        p.setString(1,receipt);try(ResultSet r=p.executeQuery()){r.next();check(r.getInt(1)==1,"RESTART_RECEIPT_DURABLE");}
      }
      try(PreparedStatement p=c.prepareStatement("UPDATE qa_sale_operation_intents SET state='IN_FLIGHT', revision=revision+1 WHERE operation_uuid=? AND state='PREPARED' AND revision=0")){
        p.setString(1,id);
        check(p.executeUpdate()==0,"RESTART_NEW_JVM_CANNOT_RECLAIM");
      }
      try(PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM payments WHERE receipt=?")){
        p.setString(1,receipt);try(ResultSet r=p.executeQuery()){r.next();check(r.getInt(1)==1,"RESTART_ONE_PAYMENT");}
      }
    }
    System.out.println("RESTART_SEPARATE_JVM_FAIL_CLOSED_PASS");
  }
}
