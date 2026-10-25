import com.mx.kylgis.pos.util.AltEncrypter;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Read-only reconciliation of financial report SQL against independent LAB transaction rows. */
public final class ReportLabFinanceReconcile {
    static final BigDecimal Z=BigDecimal.ZERO;
    static BigDecimal val(ResultSet rs,int i) throws SQLException {
        BigDecimal v=rs.getBigDecimal(i);return v==null?Z:v;
    }
    static void eq(String name,BigDecimal a,BigDecimal b) {
        if(a.subtract(b).abs().compareTo(new BigDecimal("0.015"))>0)
            throw new AssertionError(name+" report="+a+" raw="+b);
    }
    static PreparedStatement stmt(Connection c,String sql,String from,String until)throws SQLException{
        PreparedStatement st=c.prepareStatement(sql);st.setString(1,from);st.setString(2,until);
        st.setQueryTimeout(20);return st;
    }
    static BigDecimal[] sumReport(Connection c,String sql,String from,String until,int... cols)throws Exception{
        BigDecimal[] sums=new BigDecimal[cols.length+1];
        Arrays.fill(sums,Z);
        try(PreparedStatement st=stmt(c,sql,from,until);ResultSet rs=st.executeQuery()){
            while(rs.next()){
                sums[0]=sums[0].add(BigDecimal.ONE);
                for(int i=0;i<cols.length;i++)sums[i+1]=sums[i+1].add(val(rs,cols[i]));
            }
        }
        return sums;
    }
    static Map<String,String> load(Path path)throws Exception{
        Map<String,String> result=new HashMap<>();
        for(String line:Files.readAllLines(path)){
            String[] cells=line.split("\t",2);
            if(cells.length!=2)throw new AssertionError("MANIFEST_FORMAT");
            String sql=new String(Base64.getDecoder().decode(cells[1]),java.nio.charset.StandardCharsets.UTF_8);
            if(!sql.stripLeading().toUpperCase(Locale.ROOT).startsWith("SELECT")
              ||sql.contains("?(QBF_FILTER)")||sql.contains("192.168.58.17"))
                throw new SecurityException("MANIFEST_NOT_READ_ONLY");
            result.put(cells[0],sql);
        }
        if(result.size()!=6)throw new AssertionError("SIX_REPORTS_REQUIRED");
        return result;
    }
    static void window(Connection c,Map<String,String> q,String from,String until)throws Exception{
        long receipts=0,paymentRows=0,detailRows=0,withTax=0,missingTax=0,productRows=0,taxRows=0,noProductRows=0;
        BigDecimal payment=Z,base=Z,byRate=Z,gross=Z,productNet=Z,profitCost=Z,profitExpected=Z,
            profitReal=Z,taxBase=Z,taxAmount=Z,nonProductNet=Z;
        try(PreparedStatement st=stmt(c,"SELECT COUNT(*) FROM receipts WHERE DATENEW>=? AND DATENEW<?",from,until);
            ResultSet rs=st.executeQuery()){rs.next();receipts=rs.getLong(1);}
        try(PreparedStatement st=stmt(c,"SELECT p.TOTAL FROM payments p JOIN receipts r ON p.RECEIPT=r.ID WHERE r.DATENEW>=? AND r.DATENEW<?",from,until);
            ResultSet rs=st.executeQuery()){
            while(rs.next()){paymentRows++;payment=payment.add(val(rs,1));}
        }
        String lines="SELECT tl.UNITS,tl.PRICE,tx.RATE,p.ID,p.PRICEBUY,p.PRICESELL "+
            "FROM ticketlines tl JOIN receipts r ON tl.TICKET=r.ID "+
            "LEFT JOIN taxes tx ON tl.TAXID=tx.ID LEFT JOIN products p ON tl.PRODUCT=p.ID "+
            "WHERE r.DATENEW>=? AND r.DATENEW<?";
        try(PreparedStatement st=stmt(c,lines,from,until);ResultSet rs=st.executeQuery()){
            while(rs.next()){
                detailRows++;
                BigDecimal u=val(rs,1),net=u.multiply(val(rs,2));
                if(rs.getObject(3)!=null){
                    withTax++;BigDecimal tax=net.multiply(val(rs,3));
                    base=base.add(net);byRate=byRate.add(tax);gross=gross.add(net.add(tax));
                }else missingTax++;
                if(rs.getObject(4)!=null){
                    productRows++;productNet=productNet.add(net);
                    BigDecimal cost=u.multiply(val(rs,5)),list=u.multiply(val(rs,6));
                    profitCost=profitCost.add(cost);
                    profitExpected=profitExpected.add(list.subtract(cost));
                    profitReal=profitReal.add(net.subtract(cost));
                } else {
                    noProductRows++;
                    nonProductNet=nonProductNet.add(net);
                }
            }
        }
        try(PreparedStatement st=stmt(c,"SELECT x.BASE,x.AMOUNT FROM taxlines x JOIN receipts r ON x.RECEIPT=r.ID WHERE r.DATENEW>=? AND r.DATENEW<?",from,until);
            ResultSet rs=st.executeQuery()){
            while(rs.next()){taxRows++;taxBase=taxBase.add(val(rs,1));taxAmount=taxAmount.add(val(rs,2));}
        }
        BigDecimal[] payments=sumReport(c,q.get("sales_payments"),from,until,5);
        BigDecimal[] flow=sumReport(c,q.get("sales_cashflow"),from,until,2);
        BigDecimal[] category=sumReport(c,q.get("sales_categorysales"),from,until,4,5,6);
        BigDecimal[] taxes=sumReport(c,q.get("sales_taxes"),from,until,4,3);
        BigDecimal[] products=sumReport(c,q.get("sales_chart_productsales"),from,until,4);
        BigDecimal[] profit=sumReport(c,q.get("sales_productsalesprofit"),from,until,6,9,10);
        eq("payment_detail_total",payments[1],payment);
        eq("cashflow_total",flow[1],payment);
        eq("payment_detail_rows",payments[0],BigDecimal.valueOf(paymentRows));
        eq("category_net",category[1],base);
        eq("category_tax",category[2],byRate);
        eq("category_gross",category[3],gross);
        eq("taxline_base",taxes[1],taxBase);
        eq("taxline_amount",taxes[2],taxAmount);
        eq("category_gross_matches_payment",category[3],payment);
        eq("taxline_tax_matches_category_rate",taxAmount,byRate);
        eq("taxline_base_matches_category_base",taxBase,base);
        eq("product_sales",products[1],productNet);
        eq("profit_cost_estimate",profit[1],profitCost);
        eq("profit_expected_estimate",profit[2],profitExpected);
        eq("profit_actual_includes_adjustments",profit[3],profitReal.add(nonProductNet));
        if(receipts==0||paymentRows==0||detailRows==0)
            throw new AssertionError("LAB_WINDOW_WITHOUT_TRANSACTIONS");
        System.out.println("LAB_FINANCE_PASS interval="+from+".."+until+" receipts="+receipts+
            " payments="+paymentRows+" lines="+detailRows+" products="+productRows+
            " taxlines="+taxRows+" uncategorized_tax="+missingTax+
            " money_payment="+payment+" net_category="+base+" tax_category="+byRate+
            " taxline_tax="+taxAmount+" products_net="+productNet+
            " current_cost_estimate="+profitCost+" nonproduct_rows="+noProductRows+" adjustment_net="+nonProductNet);
    }
    public static void main(String[] args)throws Exception{
        if(args.length!=2)throw new IllegalArgumentException("LAB_PROPERTIES AND SQL_MANIFEST REQUIRED");
        Properties cfg=new Properties();
        try(InputStream in=Files.newInputStream(Path.of(args[0]))){cfg.load(in);}
        String url=cfg.getProperty("db.URL","");
        if(!url.startsWith("jdbc:mysql://192.168.58.18:3306/kylgis")
          ||url.contains("192.168.58.17"))throw new SecurityException("LAB_ONLY");
        String user=cfg.getProperty("db.user","");
        String pass=cfg.getProperty("db.password","");
        if(pass.startsWith("crypt:"))pass=new AltEncrypter("cypherkey"+user).decrypt(pass.substring(6));
        if(pass==null)throw new SecurityException("INVALID_LAB_PASSWORD");
        Map<String,String> q=load(Path.of(args[1]));
        Class.forName("com.mysql.jdbc.Driver");
        try(Connection c=DriverManager.getConnection(url,user,pass)){
            c.setReadOnly(true);
            window(c,q,"2026-03-18","2026-04-01");
            window(c,q,"2026-10-01","2026-10-19");
        }
        System.out.println("KYLGIS_FINANCIAL_LAB_GATE_PASS windows=2 reports=6");
    }
}
