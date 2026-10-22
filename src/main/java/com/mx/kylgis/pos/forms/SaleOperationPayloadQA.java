package com.mx.kylgis.pos.forms;

import com.mx.kylgis.pos.ticket.*;
import com.mx.kylgis.pos.payment.PaymentInfo;
import com.mx.kylgis.pos.voucher.VoucherDiscountInfo;
import java.io.*;
import java.security.*;
import java.util.HexFormat;

/** Deterministic QA payment request identity; no SQL, bank call or card credentials. */
public final class SaleOperationPayloadQA {
    private SaleOperationPayloadQA() {}
    public static String fingerprint(TicketInfo ticket) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(1); // immutable request contract version
            text(out, ticket.getId());
            text(out, ticket.getActiveCash() == null ? "QA-UNKNOWN" : ticket.getActiveCash());
            text(out, ticket.getCustomerId());
            out.writeInt(ticket.getTicketType());
            text(out, "MXN");
            boundedCount(out, ticket.getLines().size());
            for (TicketLineInfo line : ticket.getLines()) {
                text(out, line.getProductID());
                number(out, line.getMultiply());
                number(out, line.getPrice());
                text(out, line.getTaxInfo() == null ? null : line.getTaxInfo().getId());
            }
            boundedCount(out, ticket.getPayments().size());
            for (PaymentInfo payment : ticket.getPayments()) {
                text(out, payment.getName());
                number(out, payment.getTotal());
                number(out, payment.getPaid());
                number(out, payment.getTendered());
                text(out, payment.getVoucher());
                // Provider reference arrives after dispatch; it is bound separately.
            }
            boundedCount(out, ticket.getVoucherDiscounts().size());
            for (VoucherDiscountInfo discount : ticket.getVoucherDiscounts()) {
                text(out, discount.getVoucherId());
                text(out, discount.getVoucher());
                text(out, discount.getCustomerId());
                number(out, discount.getFaceValue());
                number(out, discount.getTotal());
            }
        }
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray())); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    private static void boundedCount(DataOutputStream out,int n)throws IOException {
        if(n<0||n>10000)throw new IOException("QA request cardinality");
        out.writeInt(n);
    }
    private static void text(DataOutputStream out,String text)throws IOException {
        if(text!=null&&text.length()>4096)throw new IOException("QA request field bound");
        out.writeBoolean(text!=null);
        if(text!=null)out.writeUTF(text);
    }
    private static void number(DataOutputStream out,double value)throws IOException {
        if(!Double.isFinite(value))throw new IOException("QA request amount is not finite");
        out.writeDouble(value);
    }
}
