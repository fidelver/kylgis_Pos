import com.mx.kylgis.pos.ticket.TicketInfo;

/** Regression gate for TicketInfo status assignment, no DB or production. */
public final class TicketStatusSetterProbe {
    public static void main(String[] args) {
        TicketInfo fresh = new TicketInfo();
        fresh.setTicketStatus(7);
        if (fresh.getTicketStatus() != 7) {
            throw new AssertionError("FRESH_STATUS_EXPECTED_7 got " + fresh.getTicketStatus());
        }
        System.out.println("PASS FRESH_STATUS");
        TicketInfo existing = new TicketInfo();
        existing.setTicketId(42);
        existing.setTicketStatus(7);
        if (existing.getTicketStatus() != 7) {
            throw new AssertionError("EXISTING_STATUS_CORRUPTED expected=7 actual="
                    + existing.getTicketStatus() + " ticketId=" + existing.getTicketId());
        }
        if (existing.getTicketId() != 42) {
            throw new AssertionError("TICKET_ID_CHANGED");
        }
        System.out.println("PASS EXISTING_STATUS_INDEPENDENT_OF_ID");
    }
}
