import com.mx.kylgis.pos.util.AltEncrypter;
import java.io.InputStream;
import java.nio.file.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

/** Renders the *actual* BeanShell profit SELECT from LAB .18 with discounts. */
public final class ReportLabProfitRealSmoke {
    static final String START = "2026-10-01", END = "2026-10-19";
    public static void main(String[] args) throws Exception {
        if (args.length!=4) throw new IllegalArgumentException("LAB config, SQL file, JRXML, output");
        Properties cfg=new Properties();
        try(InputStream is=Files.newInputStream(Path.of(args[0]))){cfg.load(is);}
        String url=cfg.getProperty("db.URL","");
        if(!url.startsWith("jdbc:mysql://192.168.58.18:3306/kylgis") ||
            url.contains("192.168.58.17"))throw new SecurityException("LAB_ONLY");
        String user=cfg.getProperty("db.user"),pass=cfg.getProperty("db.password");
        if(pass.startsWith("crypt:"))
            pass=new AltEncrypter("cypherkey"+user).decrypt(pass.substring(6));
        if(pass==null)throw new SecurityException("BAD_CREDENTIAL_FORMAT");
        String sql=Files.readString(Path.of(args[1]));
        if(!sql.stripLeading().toUpperCase(Locale.ROOT).startsWith("SELECT") ||
             sql.contains("?(QBF_FILTER)") || sql.contains("192.168.58.17"))
            throw new SecurityException("SELECT_ONLY");
        JasperReport template=JasperCompileManager.compileReport(args[2]);
        List<Map<String,?>> data=new ArrayList<>();
        BigDecimal adjustment=BigDecimal.ZERO,profits=BigDecimal.ZERO;
        int adjustmentRows=0;
        Class.forName("com.mysql.jdbc.Driver");
        try(Connection c=DriverManager.getConnection(url,user,pass)){
            c.setReadOnly(true);
            try(PreparedStatement st=c.prepareStatement(sql)){
                st.setString(1,START);st.setString(2,END);st.setQueryTimeout(12);
                try(ResultSet rs=st.executeQuery()){
                    while(rs.next()){
                        Map<String,Object> row=new HashMap<>();
                        for(JRField f:template.getFields()) {
                            String field=f.getName();
                            if(f.getValueClass()==Double.class)
                                row.put(field,rs.getObject(field)==null?null:rs.getDouble(field));
                            else if(f.getValueClass()==Integer.class)
                                row.put(field,rs.getInt(field));
                            else row.put(field,rs.getString(field));
                        }
                        String ref=rs.getString("REFERENCE");
                        BigDecimal actual=rs.getBigDecimal("ACTUAL_PROFIT");
                        if(actual!=null)profits=profits.add(actual);
                        if("AJUSTES".equals(ref)){
                            adjustmentRows++;
                            adjustment=adjustment.add(actual==null?BigDecimal.ZERO:actual);
                        }
                        data.add(row);
                    }
                }
            }
        }
        if(data.size()<2||adjustmentRows!=1||adjustment.compareTo(new BigDecimal("-43.11"))<0
          ||adjustment.compareTo(new BigDecimal("-43.10"))>0)
            throw new AssertionError("ADJUSTMENT_MISSING rows="+adjustmentRows+" amount="+adjustment);
        ResourceBundle bundle=ResourceBundle.getBundle(
            "com.mx.kylgis.pos.reports.sales_messages",Locale.forLanguageTag("es-MX"));
        Map<String,Object> p=new HashMap<>();
        p.put(JRParameter.REPORT_RESOURCE_BUNDLE,bundle);
        p.put(JRParameter.REPORT_LOCALE,Locale.forLanguageTag("es-MX"));
        p.put("ARG",new Object[]{new Object[]{null,Timestamp.valueOf("2026-10-01 00:00:00"),
             null,Timestamp.valueOf("2026-10-18 23:59:59")}});
        p.put("KYLGIS_REPORT_PERIOD","1 a 18 oct 2026");
        JasperPrint printed=JasperFillManager.fillReport(template,p,
            new JRMapCollectionDataSource(data));
        if(printed.getPages().isEmpty())throw new AssertionError("JASPER_NO_PAGES");
        Path out=Path.of(args[3]);JasperExportManager.exportReportToPdfFile(printed,out.toString());
        System.out.println("LAB_REAL_PROFIT_ADJUSTMENTS_PASS rows="+data.size()+
            " discounts="+adjustmentRows+" net_adjustment="+adjustment+
            " profit_estimated="+profits+" pages="+printed.getPages().size()+
            " pdf_bytes="+Files.size(out));
    }
}
