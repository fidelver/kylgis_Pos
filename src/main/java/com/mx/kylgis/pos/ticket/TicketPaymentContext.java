package com.mx.kylgis.pos.ticket;

import com.mx.kylgis.pos.payment.PaymentInfo;
import com.mx.kylgis.pos.payment.PaymentInfoMagcard;
import com.mx.kylgis.pos.voucher.VoucherDiscountInfo;
import java.io.*;
import java.util.*;

/**
 * Versioned financial facts for suspended tickets. No PAN, tracks, gateway
 * object, authorization credentials or automatic charge capability is restored.
 */
final class TicketPaymentContext {
    private static final int MAGIC = 0x4b504331; // KPC1
    private static final int VERSION = 1;
    private static final int MAX_COMPONENTS = 256;
    private static final int MAX_BYTES = 256 * 1024;

    static void write(ObjectOutput out, TicketInfo ticket, boolean processed) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream data = new DataOutputStream(bytes)) {
            data.writeInt(VERSION);
            string(data, ticket.getActiveCash());
            data.writeBoolean(processed);
            count(data, ticket.getPayments().size());
            for (PaymentInfo payment : ticket.getPayments()) {
                if (payment == null) throw new IOException("Null payment component");
                string(data, payment.getName());
                number(data, payment.getTotal());
                number(data, payment.getPaid());
                number(data, payment.getTendered());
                number(data, payment.getChange());
                string(data, payment.getTransactionID());
                string(data, payment.getVoucher());
                // Legacy getCardName mutates the card object; never read card data.
                string(data, payment instanceof PaymentInfoMagcard ? null : payment.getCardName());
            }
            count(data, ticket.getVoucherDiscounts().size());
            for (VoucherDiscountInfo discount : ticket.getVoucherDiscounts()) {
                if (discount == null) throw new IOException("Null voucher component");
                string(data, discount.getVoucherId());
                string(data, discount.getVoucher());
                string(data, discount.getCustomerId());
                number(data, discount.getFaceValue());
                number(data, discount.getTotal());
            }
        }
        if (bytes.size() > MAX_BYTES) throw new IOException("Payment context too large");
        out.writeInt(MAGIC);
        out.writeInt(bytes.size());
        out.write(bytes.toByteArray());
    }

    static boolean read(ObjectInput in, TicketInfo ticket) throws IOException {
        int magic;
        int first;
        try {
            first = in.read();
        } catch (OptionalDataException legacy) {
            if (legacy.eof) return false;
            throw legacy;
        }
        if (first == -1) return false;
        // Once the first byte exists, a truncated extension is corruption.
        magic = (first << 24) | (in.readUnsignedByte() << 16)
                | (in.readUnsignedByte() << 8) | in.readUnsignedByte();
        if (magic != MAGIC) throw new IOException("Invalid payment context marker");
        int size = in.readInt();
        if (size < 4 || size > MAX_BYTES) throw new IOException("Invalid payment context size");
        byte[] bytes = new byte[size];
        in.readFully(bytes);
        List<PaymentInfo> payments = new ArrayList<>();
        List<VoucherDiscountInfo> discounts = new ArrayList<>();
        String cash;
        boolean processed;
        try (DataInputStream data = new DataInputStream(new ByteArrayInputStream(bytes))) {
            if (data.readInt() != VERSION) throw new IOException("Unsupported payment context version");
            cash = string(data);
            processed = data.readBoolean();
            int count = count(data);
            for (int i = 0; i < count; i++) {
                String name = string(data);
                if (name == null || name.isBlank()) throw new IOException("Missing payment type");
                payments.add(new RestoredPayment(name, number(data), number(data),
                        number(data), number(data), string(data), string(data), string(data)));
            }
            count = count(data);
            for (int i = 0; i < count; i++) {
                String id = string(data), code = string(data), customer = string(data);
                double face = number(data), applied = number(data);
                if (id == null || code == null || face < 0 || applied < 0 || applied > face)
                    throw new IOException("Invalid voucher context");
                discounts.add(new VoucherDiscountInfo(id, code, customer, face, applied));
            }
            if (data.read() != -1) throw new IOException("Trailing payment context bytes");
        }
        // Apply only after the entire bounded extension has been validated.
        ticket.setPayments(payments);
        ticket.setVoucherDiscounts(discounts);
        ticket.setActiveCash(cash);
        ticket.setIsProcessed(processed);
        if (!payments.isEmpty() || !discounts.isEmpty() || processed)
            ticket.setProperty(TicketInfo.PAYMENT_RECONCILIATION_PROPERTY, "true");
        return true;
    }

    private static void count(DataOutputStream out, int count) throws IOException {
        if (count < 0 || count > MAX_COMPONENTS) throw new IOException("Too many payment components");
        out.writeInt(count);
    }

    private static int count(DataInputStream in) throws IOException {
        int count = in.readInt();
        if (count < 0 || count > MAX_COMPONENTS) throw new IOException("Invalid payment component count");
        return count;
    }

    private static void string(DataOutputStream out, String value) throws IOException {
        if (value != null && value.length() > 4096) throw new IOException("Payment field too long");
        out.writeBoolean(value != null);
        if (value != null) out.writeUTF(value);
    }

    private static String string(DataInputStream in) throws IOException {
        String value = in.readBoolean() ? in.readUTF() : null;
        if (value != null && value.length() > 4096) throw new IOException("Payment field too long");
        return value;
    }

    private static void number(DataOutputStream out, double value) throws IOException {
        if (!Double.isFinite(value)) throw new IOException("Non-finite payment amount");
        out.writeDouble(value);
    }

    private static double number(DataInputStream in) throws IOException {
        double value = in.readDouble();
        if (!Double.isFinite(value)) throw new IOException("Non-finite payment amount");
        return value;
    }

    /** Inert financial record; cannot be handed to a bank gateway as a card. */
    private static final class RestoredPayment extends PaymentInfo {
        private final String name, transaction, voucher, cardName;
        private final double total, paid, tendered, change;
        private RestoredPayment(String name, double total, double paid, double tendered,
                double change, String transaction, String voucher, String cardName) {
            this.name = name; this.total = total; this.paid = paid;
            this.tendered = tendered; this.change = change;
            this.transaction = transaction; this.voucher = voucher; this.cardName = cardName;
        }
        public String getName() { return name; }
        public double getTotal() { return total; }
        public double getPaid() { return paid; }
        public double getTendered() { return tendered; }
        public double getChange() { return change; }
        public String getTransactionID() { return transaction; }
        public String getVoucher() { return voucher; }
        public String getCardName() { return cardName; }
        public PaymentInfo copyPayment() { return this; } // immutable
    }
}
