//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.voucher;

import com.mx.kylgis.pos.basic.BasicException;
import com.mx.kylgis.pos.ticket.TaxInfo;
import com.mx.kylgis.pos.ticket.TicketInfo;
import com.mx.kylgis.pos.ticket.TicketLineInfo;
import com.mx.kylgis.pos.util.RoundUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Applies voucher discounts as non-stock ticket lines distributed by tax group. */
public final class VoucherDiscountAllocator {

    public static final String PROP_DISCOUNT = "voucher.discount";
    public static final String PROP_VOUCHER_ID = "voucher.id";
    public static final String PROP_VOUCHER_CODE = "voucher.code";
    public static final String PROP_FACE_VALUE = "voucher.faceValue";
    public static final String PROP_APPLIED = "voucher.appliedAmount";

    private VoucherDiscountAllocator() {
    }

    public static boolean removeExistingAdjustments(TicketInfo ticket) {
        if (ticket == null) {
            return false;
        }
        boolean removed = ticket.getLines().removeIf(line ->
                "true".equalsIgnoreCase(line.getProperty(PROP_DISCOUNT)));
        if (removed) {
            ticket.refreshLines();
            ticket.resetTaxes();
        }
        ticket.resetVoucherDiscounts();
        return removed;
    }

    public static double apply(TicketInfo ticket, List<VoucherDiscountInfo> discounts)
            throws BasicException {
        if (ticket == null || discounts == null || discounts.isEmpty()) {
            return 0.0;
        }

        List<TaxBucket> buckets = buildBuckets(ticket);
        double remainingGross = buckets.stream()
                .filter(bucket -> RoundUtils.compare(bucket.gross, 0.0) > 0)
                .mapToDouble(bucket -> bucket.gross)
                .sum();
        if (RoundUtils.compare(remainingGross, 0.0) <= 0) {
            throw new BasicException("Voucher discount requires a positive sale total.");
        }

        double totalApplied = 0.0;
        for (VoucherDiscountInfo discount : discounts) {
            if (discount == null || discount.getTotal() <= 0.0) {
                continue;
            }
            double requested = Math.min(discount.getTotal(), remainingGross);
            if (RoundUtils.compare(requested, 0.0) <= 0) {
                break;
            }
            double applied = applyOne(ticket, buckets, discount, requested, remainingGross);
            remainingGross -= applied;
            totalApplied += applied;
        }
        return totalApplied;
    }

    private static List<TaxBucket> buildBuckets(TicketInfo ticket) {
        Map<String, TaxBucket> grouped = new LinkedHashMap<>();
        for (TicketLineInfo line : ticket.getLines()) {
            if ("true".equalsIgnoreCase(line.getProperty(PROP_DISCOUNT))) {
                continue;
            }
            double gross = line.getValue();
            TaxInfo tax = line.getTaxInfo();
            String taxId = tax == null ? "" : tax.getId();
            String taxCategory = line.getProductTaxCategoryID();
            String key = taxId + "\u0000" + (taxCategory == null ? "" : taxCategory);
            TaxBucket bucket = grouped.computeIfAbsent(key,
                    ignored -> new TaxBucket(tax, taxCategory));
            bucket.gross += gross;
        }
        return new ArrayList<>(grouped.values());
    }

    private static double applyOne(TicketInfo ticket, List<TaxBucket> buckets,
            VoucherDiscountInfo discount, double requested, double totalGross)
            throws BasicException {
        double remaining = requested;
        int eligibleBuckets = 0;
        for (TaxBucket bucket : buckets) {
            if (RoundUtils.compare(bucket.gross, 0.0) > 0) {
                eligibleBuckets++;
            }
        }

        for (TaxBucket bucket : buckets) {
            if (RoundUtils.compare(bucket.gross, 0.0) <= 0) {
                continue;
            }
            eligibleBuckets--;
            double allocation = eligibleBuckets == 0
                    ? remaining
                    : requested * (bucket.gross / totalGross);
            allocation = Math.min(allocation, bucket.gross);
            allocation = Math.min(allocation, remaining);
            if (RoundUtils.compare(allocation, 0.0) <= 0) {
                continue;
            }

            double taxRate = bucket.tax == null ? 0.0 : bucket.tax.getRate();
            double netDiscount = allocation / (1.0 + taxRate);
            TicketLineInfo adjustment = new TicketLineInfo(
                    "Descuento cupón " + discount.getVoucher(),
                    bucket.taxCategoryId,
                    "1",
                    1.0,
                    -netDiscount,
                    bucket.tax);
            adjustment.setProperty(PROP_DISCOUNT, "true");
            adjustment.setProperty(PROP_VOUCHER_ID, safe(discount.getVoucherId()));
            adjustment.setProperty(PROP_VOUCHER_CODE, safe(discount.getVoucher()));
            adjustment.setProperty(PROP_FACE_VALUE, Double.toString(discount.getFaceValue()));
            adjustment.setProperty(PROP_APPLIED, Double.toString(allocation));
            ticket.addLine(adjustment);

            bucket.gross -= allocation;
            remaining -= allocation;
        }

        double applied = requested - remaining;
        if (RoundUtils.compare(applied, requested) != 0) {
            throw new BasicException("Voucher discount could not be allocated completely across ticket taxes.");
        }
        return applied;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static final class TaxBucket {
        private final TaxInfo tax;
        private final String taxCategoryId;
        private double gross;

        private TaxBucket(TaxInfo tax, String taxCategoryId) {
            this.tax = tax;
            this.taxCategoryId = taxCategoryId;
        }
    }
}
