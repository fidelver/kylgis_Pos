import java.io.*;import java.nio.file.*;import java.sql.*;import java.util.*;
import com.mx.kylgis.pos.util.AltEncrypter;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
public class ReportLabChartRealSmoke{
 public static void main(String[] argv)throws Exception{
  Properties p=new Properties();try(InputStream in=Files.newInputStream(Path.of(argv[0]))){p.load(in);}
  String url=p.getProperty("db.URL","");
  if(!url.startsWith("jdbc:mysql://192.168.58.18:3306/kylgis")||url.contains("192.168.58.17"))throw new SecurityException("LAB_ONLY");
  String user=p.getProperty("db.user");String pass=p.getProperty("db.password");
  if(pass.startsWith("crypt:"))pass=new AltEncrypter("cypherkey"+user).decrypt(pass.substring(6));
  String sql="SELECT p.REFERENCE, p.NAME, SUM(tl.UNITS) UNITS, SUM(tl.UNITS*tl.PRICE) TOTAL FROM receipts r JOIN tickets t ON r.ID=t.ID JOIN ticketlines tl ON tl.TICKET=t.ID JOIN products p ON p.ID=tl.PRODUCT WHERE r.DATENEW>=? AND r.DATENEW<? GROUP BY p.REFERENCE,p.NAME ORDER BY p.NAME";
  ArrayList<Map<String,?>> rows=new ArrayList<>();double total=0;
  try(Connection db=DriverManager.getConnection(url,user,pass)){db.setReadOnly(true);
   try(PreparedStatement st=db.prepareStatement(sql)){st.setString(1,"2026-03-18");st.setString(2,"2026-04-01");
    try(ResultSet r=st.executeQuery()){while(r.next()){Map<String,Object> line=new HashMap<>();
     line.put("REFERENCE",r.getString(1));line.put("NAME",r.getString(2));line.put("UNITS",r.getDouble(3));line.put("TOTAL",r.getDouble(4));
     rows.add(line);total+=r.getDouble(4);
    }}
   }
  }
  if(rows.size()<3)throw new IllegalStateException("INSUFFICIENT_LAB_ROWS");
  JasperReport report=JasperCompileManager.compileReport(argv[1]);
  ResourceBundle labels=ResourceBundle.getBundle("com.mx.kylgis.pos.reports.sales_messages",Locale.forLanguageTag("es-MX"));
  Map<String,Object> params=new HashMap<>();
  params.put(JRParameter.REPORT_LOCALE,Locale.forLanguageTag("es-MX"));
  params.put(JRParameter.REPORT_RESOURCE_BUNDLE,labels);
  params.put("ARG",new Object[]{new Object[]{null,Timestamp.valueOf("2026-03-18 00:00:00"),null,Timestamp.valueOf("2026-03-31 23:59:59")}});
  params.put("KYLGIS_REPORT_PERIOD","18/03/2026 a 31/03/2026");
  JasperPrint pdf=JasperFillManager.fillReport(report,params,new JRMapCollectionDataSource(rows));
  if(pdf.getPages().isEmpty())throw new AssertionError("NO_PAGES");
  int expectedPages=(rows.size()+14)/15;
  if(pdf.getPages().size()!=expectedPages)
   throw new AssertionError("PAGINATION_EXPECTED="+expectedPages+" actual="+pdf.getPages().size());
  Path output=Path.of(argv[2]);
  JasperExportManager.exportReportToPdfFile(pdf,output.toString());
  System.out.println("LAB_REAL_CHART_PDF_OK rows="+rows.size()+" pages="+pdf.getPages().size()+" total="+String.format(Locale.ROOT,"%.2f",total)+" bytes="+Files.size(output)+" path="+output);
 }
}
