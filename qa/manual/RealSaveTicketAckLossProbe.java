import com.mx.kylgis.pos.forms.DataLogicSales;
import com.mx.kylgis.pos.data.loader.Session;
import com.mx.kylgis.pos.data.loader.TransactionOutcomeUnknownException;
import com.mx.kylgis.pos.basic.BasicException;
import com.mx.kylgis.pos.ticket.*;
import com.mx.kylgis.pos.payment.PaymentInfoCash;
import com.mx.kylgis.pos.voucher.VoucherDiscountInfo;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Drop MariaDB COMMIT OK at TCP layer for REAL DataLogicSales.saveTicket.
 * QA synthetic only: no external bank, SAT/PAC, shared LAB, or production.
 */
public final class RealSaveTicketAckLossProbe {
    static final String DB = "pos_qa_status";
    static final String DIRECT = "jdbc:mysql://127.0.0.1:3306/"+DB
            +"?useSSL=false&connectTimeout=3000&socketTimeout=5000";
    static int checks;
    static void ck(boolean ok,String marker) {
        if (!ok) throw new AssertionError(marker);
        System.out.println("PASS "+marker);checks++;
    }
    static int count(Connection c,String sql,String value)throws SQLException {
        try(PreparedStatement ps=c.prepareStatement(sql)){
            ps.setString(1,value);
            try(ResultSet r=ps.executeQuery()){r.next();return r.getInt(1);}
        }
    }
    static byte[] packet(InputStream in)throws IOException {
        byte[] header=in.readNBytes(4);
        if(header.length==0)return null;
        if(header.length!=4)throw new EOFException("packet header");
        int len=(header[0]&255)|((header[1]&255)<<8)|((header[2]&255)<<16);
        if(len<0 || len>1048576)throw new IOException("packet bound");
        byte[] payload=in.readNBytes(len);
        if(payload.length!=len)throw new EOFException("packet payload");
        byte[] all=new byte[4+len];
        System.arraycopy(header,0,all,0,4);
        System.arraycopy(payload,0,all,4,len);
        return all;
    }
    static void send(OutputStream out,byte[] p)throws IOException {
        out.write(p);out.flush();
    }
    static boolean isCommit(byte[] p){
        return p.length>=11 && (p[4]&255)==3
                && new String(p,5,p.length-5,StandardCharsets.UTF_8)
                .trim().equalsIgnoreCase("COMMIT");
    }
    static final class AckDropProxy implements AutoCloseable {
        final ServerSocket listener;
        final AtomicBoolean seen=new AtomicBoolean(),dispatched=new AtomicBoolean(),
                serverOk=new AtomicBoolean(),cut=new AtomicBoolean();
        final Thread service;
        volatile Throwable failure;
        AckDropProxy() throws IOException {
            listener=new ServerSocket(0,1,InetAddress.getByName("127.0.0.1"));
            service=new Thread(this::serve,"kylgis-ack-qa-proxy");
            service.setDaemon(true);service.start();
        }
        int port(){return listener.getLocalPort();}
        void terminate(Socket client,Socket server)throws IOException {
            cut.set(true);
            client.setSoLinger(true,0);
            client.close();
            server.close();
        }
        void serve() {
            try(Socket client=listener.accept();
                    Socket server=new Socket("127.0.0.1",3306)){
                client.setTcpNoDelay(true);
                server.setTcpNoDelay(true);
                Thread request=new Thread(()->{
                    try{
                        byte[] p;
                        while((p=packet(client.getInputStream()))!=null) {
                            if(isCommit(p))seen.set(true);
                            send(server.getOutputStream(),p);
                            if(isCommit(p))dispatched.set(true);
                        }
                    }catch(IOException ignored){}
                },"qa-client-db");
                request.setDaemon(true);request.start();
                byte[] reply;
                while((reply=packet(server.getInputStream()))!=null){
                    if(seen.get() && reply.length>=5 && (reply[4]&255)==0){
                        serverOk.set(true);
                        terminate(client,server);
                        return;
                    }
                    send(client.getOutputStream(),reply);
                }
            }catch(IOException e){
                if(!cut.get())failure=e;
            }catch(Throwable unexpected){failure=unexpected;}
        }
        public void close() throws Exception {
            listener.close();service.join(5000);
            if(failure!=null)throw new AssertionError("PROXY_FAILURE",failure);
        }
    }
    static TicketInfo ticket(){
        TicketInfo t=new TicketInfo();
        t.setTicketType(TicketInfo.RECEIPT_NORMAL);
        t.setDate(new java.util.Date());
        t.setActiveCash("QA-CASH");
        t.setUser(new UserInfo("0","Administrator"));
        t.setVoucherDiscounts(List.of(
                new VoucherDiscountInfo("QA-ACK-ID","QA-ACK-NUM",null,30,20)));
        t.addLine(new TicketLineInfo("xxx999_999xxx_x9x9x9",1,30,
                new TaxInfo("002","IVA 0%","001",null,null,0.0,false,0)));
        t.setPayments(List.of(new PaymentInfoCash(10,10)));
        return t;
    }
    public static void main(String[] args)throws Exception{
        if(!"ONLY_SYNTHETIC_POS_STATUS".equals(System.getenv("KYLGIS_QA_SCOPE")))
            throw new SecurityException("REFUSE_NON_SYNTHETIC_SCOPE");
        Class.forName("com.mysql.jdbc.Driver");
        try(Connection c=DriverManager.getConnection(DIRECT,"root","")){
            ck(DB.equals(c.getCatalog()),"ACK_QA_CATALOG");
            try(Statement st=c.createStatement()){
                st.executeUpdate("INSERT INTO vouchers(id,voucher_number,amount,status) "
                        +"VALUES('QA-ACK-ID','QA-ACK-NUM',30,'A')");
            }
        }
        boolean ledgerEnabled = "1".equals(System.getenv("KYLGIS_QA_LEDGER_ACK_OPTIN"));
        TicketInfo sale=ticket();
        boolean clientReportedFailure=false;
        try(AckDropProxy proxy=new AckDropProxy()){
            String url="jdbc:mysql://127.0.0.1:"+proxy.port()+"/"+DB
                    +"?useSSL=false&connectTimeout=3000&socketTimeout=5000";
            if (ledgerEnabled) {
                System.setProperty("kylgis.qa.durableLedger.enabled", "true");
                System.setProperty("kylgis.qa.durableLedger.directJdbcUrl", DIRECT);
            }
            Session session=new Session(url,"root","");
            try{
                DataLogicSales logic=new DataLogicSales();
                logic.init(session);
                try{
                    logic.saveTicket(sale,"0");
                    throw new AssertionError("COMMIT_ACK_WAS_NOT_LOST");
                }catch(BasicException expected){
                    ck(expected instanceof TransactionOutcomeUnknownException,
                            "COMMIT_FAILURE_TYPED_UNKNOWN");
                    TransactionOutcomeUnknownException unknown =
                            (TransactionOutcomeUnknownException) expected;
                    ck(unknown.getPhase()==TransactionOutcomeUnknownException.Phase.COMMIT,
                            "COMMIT_FAILURE_PHASE_RECORDED");
                    clientReportedFailure=true;
                    System.out.println("EXPECTED_COMMIT_COMMUNICATION_FAILURE="
                            +expected.getClass().getSimpleName());
                }
            }finally{
                try{session.close();}catch(Exception ignored){}
            }
            ck(clientReportedFailure,"CLIENT_COMMIT_RESULT_UNKNOWN");
            ck(proxy.seen.get() && proxy.dispatched.get() && proxy.serverOk.get()
                    && proxy.cut.get(),"SERVER_COMMIT_OK_WITHHELD_FROM_CLIENT");
        }
        // A new independent JDBC connection is authoritative for durable DB state.
        try(Connection fresh=DriverManager.getConnection(DIRECT,"root","")){
            ck(DB.equals(fresh.getCatalog()),"ACK_FRESH_DB_CATALOG");
            ck(count(fresh,"SELECT COUNT(*) FROM receipts WHERE id=?",sale.getId())==1,
                    "ACK_DURABLE_RECEIPT");
            ck(count(fresh,"SELECT COUNT(*) FROM tickets WHERE id=?",sale.getId())==1,
                    "ACK_DURABLE_TICKET");
            ck(count(fresh,"SELECT COUNT(*) FROM ticketlines WHERE ticket=?",sale.getId())==1,
                    "ACK_DURABLE_LINE");
            ck(count(fresh,"SELECT COUNT(*) FROM payments WHERE receipt=?",sale.getId())==1,
                    "ACK_DURABLE_PAYMENT");
            ck(count(fresh,"SELECT COUNT(*) FROM voucher_redemptions WHERE receipt_id=?",
                    sale.getId())==1,"ACK_DURABLE_REDEMPTION");
            ck(count(fresh,"SELECT COUNT(*) FROM vouchers WHERE id=? AND status='D'",
                    "QA-ACK-ID")==1,"ACK_DURABLE_VOUCHER_CONSUMPTION");
            if (ledgerEnabled) {
                String operationId=sale.getProperty("kylgis.qa.durableLedger.operationUuid");
                ck(operationId!=null && !operationId.isBlank(),"ACK_LEDGER_OPERATION_PERSISTED");
                ck(count(fresh,"SELECT COUNT(*) FROM qa_sale_operation_intents "
                        +"WHERE operation_uuid=? AND state='COMMIT_UNKNOWN' AND revision=2",
                        operationId)==1,"ACK_LEDGER_UNKNOWN_DURABLE");
                Session retry=new Session(DIRECT,"root","");
                try {
                    DataLogicSales sameSale=new DataLogicSales();
                    sameSale.init(retry);
                    try{
                        sameSale.saveTicket(sale,"0");
                        throw new AssertionError("ACK_DUPLICATE_SALE_ALLOWED");
                    }catch(BasicException blocked){
                        ck(blocked.getMessage()!=null
                            && blocked.getMessage().contains("preparation is not confirmed"),
                            "ACK_RETRY_SAME_RECEIPT_BLOCKED");
                    }
                } finally { retry.close(); }
                ck(count(fresh,"SELECT COUNT(*) FROM payments WHERE receipt=?",sale.getId())==1,
                        "ACK_NO_DUPLICATE_PAYMENT_AFTER_RETRY");
                String restartFile = System.getenv("KYLGIS_QA_RESTART_TICKET");
                if (restartFile != null) {
                    if (!restartFile.equals("/tmp/qa-home/ack-ticket.bin"))
                        throw new SecurityException("REFUSE_RESTART_PATH");
                    try (ObjectOutputStream out = new ObjectOutputStream(
                            new FileOutputStream(restartFile))) {
                        out.writeObject(sale);
                    }
                    System.out.println("ACK_HANDOFF_DURABLE_FIRST_JVM_EXIT_PENDING");
                } else {
                Process restarted = new ProcessBuilder(
                        System.getProperty("java.home") + "/bin/java",
                        "-cp",System.getProperty("java.class.path"),
                        "RestartedSaleLedgerProbe","isolated-restart-qa",operationId)
                        .inheritIO().start();
                ck(restarted.waitFor()==0,"ACK_RESTART_SEPARATE_JVM_PASS");
                }
            }
        }
        System.out.println("CORE_SAVE_TICKET_TCP_ACK_LOSS_CONFIRMED checks="+checks);
        System.out.println("NO_AUTOMATIC_RETRY_OR_REFUND_TESTED_OR_PERMITTED");
    }
}
