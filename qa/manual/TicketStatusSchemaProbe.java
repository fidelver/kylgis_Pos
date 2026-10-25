import com.mx.kylgis.pos.data.loader.*;

/** Executes the original parameterized POS schema loader in the sealed QA container. */
public final class TicketStatusSchemaProbe {
    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.jdbc.Driver");
        Session s = new Session("jdbc:mysql://127.0.0.1:3306/pos_qa_status"
                + "?useSSL=false&connectTimeout=2500&socketTimeout=5000", "root", "");
        try {
            BatchSentenceResource batch = new BatchSentenceResource(
                    s, "/com/mx/kylgis/pos/scripts/MySQL-create.sql");
            batch.putParameter("APP_ID", "kylgis-ticketstatus-sandbox");
            batch.putParameter("APP_NAME", "KylGis Ticket Status QA");
            batch.putParameter("APP_VERSION", "1.3");
            DataResultSet result = batch.openExec(null);
            int errors = 0;
            while (result.next()) {
                System.out.println("SCHEMA_PROBLEM=" + result.getCurrent());
                if (++errors >= 5) break;
            }
            if (errors != 0) throw new AssertionError("SCHEMA_ERRORS=" + errors);
            System.out.println("STATUS_SCHEMA_LOADER_PASS");
        } finally {
            s.close();
        }
    }
}
