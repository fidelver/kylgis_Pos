import java.sql.*;
import java.util.UUID;

/** QA ONLY: a separate durable connection and explicit fail-closed claims. */
public final class IsolatedSaleOperationLedgerProbe {
    private static void require(boolean ok,String name) { if(!ok)throw new AssertionError(name);System.out.println("PASS "+name); }
    private static int update(Connection c,String sql,String...args)throws SQLException{
        try(PreparedStatement p=c.prepareStatement(sql)){
            for(int i=0;i<args.length;i++)p.setString(i+1,args[i]);
            return p.executeUpdate();
        }
    }
    private static String state(Connection c,String receipt)throws SQLException{
        try(PreparedStatement p=c.prepareStatement("SELECT state FROM qa_sale_operation_intents WHERE receipt_id=?")){
            p.setString(1,receipt);
            try(ResultSet r=p.executeQuery()){return r.next()?r.getString(1):null;}
        }
    }
    public static void main(String[] args)throws Exception{
        if(args.length!=1||!"isolated-ledger-qa".equals(args[0]))throw new IllegalArgumentException("scope");
        String url="jdbc:mariadb://127.0.0.1:3306/isolated_ledger_qa?sslMode=disable";
        String receipt="REAL-PROBE-R1";String id=UUID.randomUUID().toString();
        try(Connection prepare=DriverManager.getConnection(url,"root","")){
            require("isolated_ledger_qa".equals(prepare.getCatalog()),"ISOLATED_CATALOG");
            require(update(prepare,"INSERT INTO qa_sale_operation_intents(operation_uuid,receipt_id,terminal_id) VALUES (?,?,?)",id,receipt,"C1")==1,"PREPARE_COMMITTED");
        }
        try(Connection another=DriverManager.getConnection(url,"root","")){
            require("PREPARED".equals(state(another,receipt)),"PREPARE_SURVIVES_CONNECTION_CLOSE");
            require(update(another,"UPDATE qa_sale_operation_intents SET state='IN_FLIGHT',revision=revision+1 WHERE operation_uuid=? AND state='PREPARED' AND revision=0",id)==1,"FIRST_CLAIM_WINS");
        }
        try(Connection otherTill=DriverManager.getConnection(url,"root","")){
            require(update(otherTill,"UPDATE qa_sale_operation_intents SET state='IN_FLIGHT',revision=revision+1 WHERE operation_uuid=? AND state='PREPARED' AND revision=0",id)==0,"SECOND_TILL_BLOCKED");
            // Simulates an ACK loss: no verified committed sale result; hold the operation.
            require(update(otherTill,"UPDATE qa_sale_operation_intents SET state='COMMIT_UNKNOWN',revision=revision+1 WHERE operation_uuid=? AND state='IN_FLIGHT' AND revision=1",id)==1,"UNKNOWN_RECORDED");
        }
        try(Connection restarted=DriverManager.getConnection(url,"root","")){
            require("COMMIT_UNKNOWN".equals(state(restarted,receipt)),"UNKNOWN_SURVIVES_NEW_PROCESS_CONNECTION");
            require(update(restarted,"UPDATE qa_sale_operation_intents SET state='IN_FLIGHT',revision=revision+1 WHERE operation_uuid=? AND state='PREPARED'",id)==0,"RETRY_FAIL_CLOSED");
        }
        System.out.println("ISOLATED_LEDGER_JDBC_PROBE_PASS");
    }
}
