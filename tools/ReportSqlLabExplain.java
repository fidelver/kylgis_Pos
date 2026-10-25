import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.Base64;
import com.mx.kylgis.pos.util.AltEncrypter;

/** Read-only EXPLAIN for 69 extracted KylGis POS report SELECTs on .18 LAB.
 * No statement with a DML/DDL verb is permitted. Never prints credentials.
 */
public final class ReportSqlLabExplain {
  public static void main(String[] args) throws Exception {
    if(args.length!=2) throw new IllegalArgumentException("Usage: <test-properties> <base64-queries-tsv>");
    Properties props=new Properties();
    try(InputStream in=Files.newInputStream(Path.of(args[0]))) {props.load(in);}
    String url=props.getProperty("db.URL","");
    if(!url.startsWith("jdbc:mysql://192.168.58.18:3306/") &&
       !url.startsWith("jdbc:mariadb://192.168.58.18:3306/"))
      throw new IllegalStateException("SAFETY_ABORT: URL is not explicitly .18 LAB");
    String username=props.getProperty("db.user");
    String password=props.getProperty("db.password");
    if(username==null||password==null) throw new IllegalStateException("LAB credentials unavailable");
    if(password.startsWith("crypt:"))
      password=new AltEncrypter("cypherkey"+username).decrypt(password.substring(6));
    if(password==null) throw new IllegalStateException("LAB password decode failed");
    List<String> rows=Files.readAllLines(Path.of(args[1]),StandardCharsets.UTF_8);
    if(rows.size()!=69) throw new IllegalStateException("Expected 69 SQL SELECTs, got "+rows.size());
    int passed=0,failed=0;
    try(Connection connection=DriverManager.getConnection(url,username,password)) {
      connection.setReadOnly(true);
      for(String line:rows) {
        String[] parts=line.split("\t",2);
        if(parts.length!=2) throw new IllegalArgumentException("Invalid SQL entry");
        String name=parts[0];
        String sql=new String(Base64.getDecoder().decode(parts[1]),StandardCharsets.UTF_8);
        sql=sql.replace("?(QBF_FILTER)","1=1").trim();
        // The static extractor only concatenates Java string literals. The
        // 12 legacy scripts append DB.TRUE() at runtime; recover precisely
        // that known expression, without modifying the report sources.
        sql=sql.replaceAll("(?i)(\\bVISIBLE\\s*=\\s*)(?=AND\\b|$)", "$1 TRUE ");
        if(!sql.toUpperCase(Locale.ROOT).startsWith("SELECT "))
          throw new IllegalStateException("Non-SELECT SQL rejected: "+name);
        if(sql.contains("?")) throw new IllegalStateException("Unbound query placeholder: "+name);
        try(Statement statement=connection.createStatement()) {
          statement.setQueryTimeout(25);
          try(ResultSet rs=statement.executeQuery("EXPLAIN "+sql)) {
            int planRows=0;while(rs.next())planRows++;
            System.out.println("REPORT_SQL_LAB_EXPLAIN_PASS "+name+" plan_rows="+planRows);
            passed++;
          }
        } catch(SQLException e) {
          System.out.println("REPORT_SQL_LAB_EXPLAIN_FAIL "+name+" sqlstate="+e.getSQLState()+
                              " code="+e.getErrorCode()+" msg="+e.getMessage().replace('\n',' '));
          failed++;
        }
      }
    }
    System.out.println("REPORT_SQL_LAB_SUMMARY total="+rows.size()+" pass="+passed+" fail="+failed+" readonly=true host=192.168.58.18");
    if(failed>0) System.exit(2);
  }
}
