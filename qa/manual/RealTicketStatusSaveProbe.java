import com.mx.kylgis.pos.forms.DataLogicSales;
import com.mx.kylgis.pos.data.loader.Session;
import com.mx.kylgis.pos.payment.PaymentInfoCash;
import com.mx.kylgis.pos.ticket.*;
import java.sql.*;
import java.util.Date;
import java.util.List;

/** Uses REAL DataLogicSales.saveTicket, not a reimplementation of its SQL. */
public final class RealTicketStatusSaveProbe {
    private static final String URL="jdbc:mysql://127.0.0.1:3306/pos_qa_status"
            + "?useSSL=false&connectTimeout=2500&socketTimeout=5000";
    private static int checks;
    private static void ck(boolean result, String code) {
        if (!result) throw new AssertionError(code);
        checks++;
        System.out.println("PASS " + code);
    }
    private static int oneInt(Connection c,String sql)throws SQLException {
        try(Statement st=c.createStatement();ResultSet rs=st.executeQuery(sql)){
            if(!rs.next())throw new AssertionError("EMPTY_SQL_RESULT");
            return rs.getInt(1);
        }
    }
    public static void main(String[] args)throws Exception {
        if (!"ONLY_SYNTHETIC_POS_STATUS".equals(System.getenv("KYLGIS_QA_SCOPE")))
            throw new SecurityException("REFUSE_NON_SYNTHETIC_SCOPE");
        Class.forName("com.mysql.jdbc.Driver");
        try(Connection c=DriverManager.getConnection(URL,"root","")){
            ck("pos_qa_status".equals(c.getCatalog()),"QA_CATALOG");
            try (Statement check = c.createStatement();
                    ResultSet rs = check.executeQuery("SELECT @@version, @@port")) {
                ck(rs.next() && rs.getString(1).contains("MariaDB")
                        && rs.getInt(2) == 3306, "QA_MARIADB_ENGINE");
            }
            try(Statement st=c.createStatement()){
                st.executeUpdate("INSERT INTO closedcash(money,host,hostsequence,datestart) "
                        + "VALUES('QA-CASH','QA-TICKETSTATUS',1,CURRENT_TIMESTAMP)");
                st.executeUpdate("INSERT INTO receipts(id,money,datenew) "
                        + "VALUES('QA_UNRELATED_RECEIPT','QA-CASH',CURRENT_TIMESTAMP)");
                st.executeUpdate("INSERT INTO tickets(id,tickettype,ticketid,person,status) "
                        + "VALUES('QA_UNRELATED_RECEIPT',0,7,'0',8)");
            }
        }
        TicketInfo ticket = new TicketInfo();
        ticket.setTicketType(TicketInfo.RECEIPT_NORMAL);
        ticket.setTicketId(42);
        ticket.setTicketStatus(7);
        ck(ticket.getTicketId()==42,"TARGET_FOLIO_42");
        ck(ticket.getTicketStatus()==7,"TARGET_STATUS_7");
        ticket.setDate(new Date());
        ticket.setActiveCash("QA-CASH");
        ticket.setUser(new UserInfo("0","Administrator"));
        ticket.addLine(new TicketLineInfo("xxx999_999xxx_x9x9x9",1,30,
                new TaxInfo("002","IVA 0%","001",null,null,0.0,false,0)));
        ticket.setPayments(List.of(new PaymentInfoCash(30,30)));
        Session session=new Session(URL,"root","");
        try {
            DataLogicSales sales=new DataLogicSales();
            sales.init(session);
            sales.saveTicket(ticket,"0");
            System.out.println("SAVE_TICKET_REAL_COMMIT_RETURNED");
        } finally {
            session.close();
        }
        try(Connection fresh=DriverManager.getConnection(URL,"root","")){
            ck(oneInt(fresh,"SELECT COUNT(*) FROM tickets WHERE ticketid=42 AND tickettype=0")==1,
                    "ONE_TARGET_TICKET");
            ck(oneInt(fresh,"SELECT status FROM tickets WHERE ticketid=42 AND tickettype=0")==7,
                    "TARGET_STATUS_PERSISTED");
            ck(oneInt(fresh,"SELECT status FROM tickets WHERE id='QA_UNRELATED_RECEIPT'")==8,
                    "UNRELATED_TICKET_UNCHANGED");
            ck(oneInt(fresh,"SELECT COUNT(*) FROM ticketlines")==1,"ONE_TICKETLINE");
            ck(oneInt(fresh,"SELECT COUNT(*) FROM payments")==1,"ONE_PAYMENT");
            ck(oneInt(fresh,"SELECT COUNT(*) FROM receipts")==2,"TWO_RECEIPTS_EXPECTED");
            if (Boolean.getBoolean("kylgis.qa.durableLedger.enabled")) {
                String op = ticket.getProperty("kylgis.qa.durableLedger.operationUuid");
                ck(op != null && !op.isBlank(), "DURABLE_OPERATION_UUID_SET");
                try (PreparedStatement p = fresh.prepareStatement(
                        "SELECT state,revision FROM qa_sale_operation_intents "
                        + "WHERE operation_uuid=? AND receipt_id=?")) {
                    p.setString(1,op);
                    p.setString(2,ticket.getId());
                    try (ResultSet rs=p.executeQuery()) {
                        ck(rs.next() && "DB_CONFIRMED".equals(rs.getString(1))
                            && rs.getInt(2)==2, "LEDGER_DB_CONFIRMED_AND_VERSION_2");
                        ck(!rs.next(), "LEDGER_EXACTLY_ONE_OPERATION");
                    }
                }
            }
        }
        System.out.println("REAL_SAVE_TICKET_STATUS_PASS checks="+checks);
    }
}
