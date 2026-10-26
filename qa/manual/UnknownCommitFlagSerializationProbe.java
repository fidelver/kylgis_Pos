import com.mx.kylgis.pos.ticket.TicketInfo;
import java.io.*;

/** Verifies the local UI's fail-closed flag survives TicketInfo copying/serialization. */
public final class UnknownCommitFlagSerializationProbe {
    private static void ck(boolean yes,String label){
        if(!yes)throw new AssertionError(label);
        System.out.println("PASS "+label);
    }
    public static void main(String[] args)throws Exception{
        final String key="kylgis.commit.outcome.unknown";
        TicketInfo original=new TicketInfo();
        original.setTicketId(42);
        original.setTicketStatus(7);
        original.setProperty(key,"true");
        ck("true".equals(original.getProperty(key)),"UNKNOWN_FLAG_MARKED");
        TicketInfo copied=original.copyTicket();
        ck("true".equals(copied.getProperty(key)),"UNKNOWN_FLAG_SURVIVES_COPY");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(ObjectOutputStream out=new ObjectOutputStream(bytes)){
            out.writeObject(original);
        }
        TicketInfo restored;
        try(ObjectInputStream in=new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()))){
            restored=(TicketInfo)in.readObject();
        }
        ck("true".equals(restored.getProperty(key)),"UNKNOWN_FLAG_SURVIVES_EXTERNALIZABLE");
        ck(restored.getTicketId()==42 && restored.getTicketStatus()==7,
                "SERIALIZED_FOLIO_STATUS_DISTINCT");
        System.out.println("UNKNOWN_COMMIT_FLAG_SERIALIZATION_PASS");
    }
}
