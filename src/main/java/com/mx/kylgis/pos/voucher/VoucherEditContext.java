//    KylGis POS Punto de Venta Táctil
package com.mx.kylgis.pos.voucher;

import com.mx.kylgis.pos.forms.AppUser;
import com.mx.kylgis.pos.payment.PaymentInfo;
import com.mx.kylgis.pos.ticket.TicketInfo;
import java.time.Instant;
import java.util.Set;
import java.util.TreeSet;

/**
 * Context carried by an edited sale that originally consumed one or more vouchers.
 * The values live in ticket properties so the authorizer/source information is
 * persisted with the replacement receipt for audit purposes.
 */
public final class VoucherEditContext {

    public static final String SOURCE_RECEIPT_ID = "kylgis.edit.voucher.sourceReceiptId";
    public static final String AUTH_USER_ID = "kylgis.edit.voucher.authorizedById";
    public static final String AUTH_USER_NAME = "kylgis.edit.voucher.authorizedByName";
    public static final String AUTH_USER_ROLE = "kylgis.edit.voucher.authorizedByRole";
    public static final String AUTH_AT = "kylgis.edit.voucher.authorizedAt";

    private VoucherEditContext() {
    }

    public static Set<String> voucherNumbers(TicketInfo ticket) {
        Set<String> result = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        if (ticket == null || ticket.getPayments() == null) {
            return result;
        }
        for (PaymentInfo payment : ticket.getPayments()) {
            if (payment != null && "voucherin".equals(payment.getName())) {
                String voucher = payment.getVoucher();
                if (voucher != null && !voucher.isBlank()) {
                    result.add(voucher.trim());
                }
            }
        }
        return result;
    }

    public static boolean requiresAuthorization(TicketInfo sourceTicket) {
        return !voucherNumbers(sourceTicket).isEmpty();
    }

    public static boolean isPrivilegedRole(String roleId) {
        return "0".equals(roleId) || "1".equals(roleId);
    }

    public static void stamp(TicketInfo editedTicket, TicketInfo sourceTicket, AppUser authorizer) {
        editedTicket.setProperty(SOURCE_RECEIPT_ID, sourceTicket.getId());
        editedTicket.setProperty(AUTH_USER_ID, authorizer.getId());
        editedTicket.setProperty(AUTH_USER_NAME, authorizer.getName());
        editedTicket.setProperty(AUTH_USER_ROLE, authorizer.getRole());
        editedTicket.setProperty(AUTH_AT, Instant.now().toString());
    }
}
