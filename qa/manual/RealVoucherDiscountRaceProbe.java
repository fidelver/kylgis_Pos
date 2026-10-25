import com.mx.kylgis.pos.forms.DataLogicSales;
import com.mx.kylgis.pos.data.loader.Session;
import com.mx.kylgis.pos.basic.BasicException;
import com.mx.kylgis.pos.ticket.*;
import com.mx.kylgis.pos.payment.PaymentInfoCash;
import com.mx.kylgis.pos.voucher.VoucherDiscountInfo;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Two independent real DataLogicSales.saveTicket transactions attempt the SAME
 * discount voucher at once in a synthetic database. This is not a payment
 * processor / bank certification.
 */
public final class RealVoucherDiscountRaceProbe {
    private static final String URL="jdbc:mysql://127.0.0.1:3306/pos_qa_status"
            +"?useSSL=false&connectTimeout=2500&socketTimeout=10000";
    private static int checks;
    private static void ck(boolean value,String id){
        if(!value)throw new AssertionError(id);
        System.out.println("PASS "+id);checks++;
    }
    private static int integer(Connection c,String sql)throws SQLException{
        try(Statement st=c.createStatement();ResultSet rs=st.executeQuery(sql)){
            if(!rs.next())throw new AssertionError("NO_SQL_ROW");
            return rs.getInt(1);
        }
    }
    private static TicketInfo ticket(){
        TicketInfo t=new TicketInfo();
        t.setTicketType(TicketInfo.RECEIPT_NORMAL);
        t.setDate(new java.util.Date());
        t.setActiveCash("QA-CASH");
        t.setUser(new UserInfo("0","Administrator"));
        t.setVoucherDiscounts(List.of(
                new VoucherDiscountInfo("QA-RACE-ID","QA-RACE-NUM",null,30.0,20.0)));
        t.addLine(new TicketLineInfo("xxx999_999xxx_x9x9x9",1,30,
                new TaxInfo("002","IVA 0%","001",null,null,0.0,false,0)));
        t.setPayments(List.of(new PaymentInfoCash(10,10)));
        return t;
    }
    private record Attempt(boolean saved,String reason){}
    private static Attempt attempt(CountDownLatch ready,CountDownLatch go)throws Exception{
        TicketInfo sale=ticket();
        ready.countDown();
        if(!go.await(10,TimeUnit.SECONDS))
            throw new AssertionError("RACE_START_TIMEOUT");
        Session session=new Session(URL,"root","");
        try{
            DataLogicSales logic=new DataLogicSales();
            logic.init(session);
            try{
                logic.saveTicket(sale,"0");
                return new Attempt(true,"COMMITTED");
            }catch(BasicException rejected){
                return new Attempt(false,String.valueOf(rejected.getMessage()));
            }
        }finally{session.close();}
    }
    public static void main(String[] args)throws Exception {
        if(!"ONLY_SYNTHETIC_POS_STATUS".equals(System.getenv("KYLGIS_QA_SCOPE")))
            throw new SecurityException("REFUSE_EXTERNAL_DB");
        Class.forName("com.mysql.jdbc.Driver");
        try(Connection c=DriverManager.getConnection(URL,"root","")){
            ck("pos_qa_status".equals(c.getCatalog()),"RACE_CATALOG");
            try(Statement st=c.createStatement()){
                st.executeUpdate("INSERT INTO vouchers(id,voucher_number,amount,status) "
                        +"VALUES('QA-RACE-ID','QA-RACE-NUM',30,'A')");
            }
            ck(integer(c,"SELECT COUNT(*) FROM vouchers WHERE id='QA-RACE-ID' AND status='A'")==1,
                    "ONE_AVAILABLE_VOUCHER");
            ck(integer(c,"SELECT COUNT(*) FROM ticketlines")==1,"ONE_PRIOR_LINE");
        }
        ExecutorService pool=Executors.newFixedThreadPool(2);
        CountDownLatch ready=new CountDownLatch(2),go=new CountDownLatch(1);
        Attempt a,b;
        try{
            Future<Attempt> f1=pool.submit(()->attempt(ready,go));
            Future<Attempt> f2=pool.submit(()->attempt(ready,go));
            ck(ready.await(8,TimeUnit.SECONDS),"TWO_SESSIONS_READY");
            go.countDown();
            a=f1.get(25,TimeUnit.SECONDS);
            b=f2.get(25,TimeUnit.SECONDS);
        }finally{
            pool.shutdownNow();
            ck(pool.awaitTermination(8,TimeUnit.SECONDS),"RACE_THREADS_CLEANED");
        }
        System.out.println("RACE_RESULT_A="+a+"; RACE_RESULT_B="+b);
        ck(a.saved()!=b.saved(),"EXACTLY_ONE_SAVE_SUCCEEDS");
        String reason=a.saved()?b.reason():a.reason();
        ck(reason.contains("Voucher is invalid") || reason.contains("already redeemed"),
                "SECOND_REJECTED_FOR_VOUCHER_STATE");
        try(Connection fresh=DriverManager.getConnection(URL,"root","")){
            ck(integer(fresh,"SELECT COUNT(*) FROM vouchers WHERE id='QA-RACE-ID' AND status='D'")==1,
                    "VOUCHER_CONSUMED_ONCE");
            ck(integer(fresh,"SELECT COUNT(*) FROM voucher_redemptions WHERE voucher_id='QA-RACE-ID' AND status='ACTIVE'")==1,
                    "ONE_ACTIVE_REDEMPTION");
            ck(integer(fresh,"SELECT COUNT(*) FROM tickets")==3,
                    "ONLY_ONE_NEW_TICKET");
            ck(integer(fresh,"SELECT COUNT(*) FROM receipts")==3,
                    "ONLY_ONE_NEW_RECEIPT");
            ck(integer(fresh,"SELECT COUNT(*) FROM payments")==2,
                    "ONLY_ONE_NEW_PAYMENT");
            ck(integer(fresh,"SELECT COUNT(*) FROM ticketlines")==2,
                    "ONLY_ONE_NEW_LINE");
        }
        System.out.println("REAL_VOUCHER_DISCOUNT_RACE_PASS checks="+checks);
    }
}
