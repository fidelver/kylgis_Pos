//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.voucher;

import com.mx.kylgis.pos.basic.BasicException;
import com.mx.kylgis.pos.sales.TaxesException;
import com.mx.kylgis.pos.sales.TaxesLogic;
import com.mx.kylgis.pos.ticket.TaxInfo;
import com.mx.kylgis.pos.ticket.TicketInfo;
import com.mx.kylgis.pos.ticket.TicketLineInfo;
import com.mx.kylgis.pos.ticket.TicketTaxInfo;
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

    public static double apply(TicketInfo ticket, List<VoucherDiscountInfo> discounts,
            TaxesLogic taxesLogic) throws BasicException {
        if (ticket == null || discounts == null || discounts.isEmpty()) {
            return 0.0;
        }

        if (taxesLogic == null) {
            throw new BasicException("Voucher discount requires the active tax engine.");
        }

        List<TaxBucket> buckets = buildBuckets(ticket, taxesLogic);
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

    private static List<TaxBucket> buildBuckets(TicketInfo ticket, TaxesLogic taxesLogic)
            throws BasicException {
        Map<String, TaxBucket> grouped = new LinkedHashMap<>();
        for (TicketLineInfo line : ticket.getLines()) {
            if ("true".equalsIgnoreCase(line.getProperty(PROP_DISCOUNT))) {
                continue;
            }
            double gross = calculateGross(line, taxesLogic);
            TaxInfo tax = line.getTaxInfo();
            String taxId = tax == null ? "" : tax.getId();
            String taxCategory = line.getProductTaxCategoryID();
            String key = taxId + "\u0000" + (taxCategory == null ? "" : taxCategory);
            TaxBucket bucket = grouped.get(key);
            if (bucket == null) {
                bucket = new TaxBucket(tax, taxCategory,
                        calculateGrossFactor(tax, taxCategory, taxesLogic));
                grouped.put(key, bucket);
            }
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

            double netDiscount = allocation / bucket.grossFactor;
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

    private static double calculateGross(TicketLineInfo line, TaxesLogic taxesLogic)
            throws BasicException {
        try {
            double gross = line.getSubValue();
            for (TicketTaxInfo tax : taxesLogic.calculateTaxes(line)) {
                gross += tax.getTax();
            }
            return gross;
        } catch (TaxesException ex) {
            throw new BasicException("Voucher discount could not evaluate the ticket tax tree.", ex);
        }
    }

    private static double calculateGrossFactor(TaxInfo tax, String taxCategoryId,
            TaxesLogic taxesLogic) throws BasicException {
        if (tax == null) {
            throw new BasicException("Voucher discount found a line without an applicable tax rule.");
        }
        TicketLineInfo unit = new TicketLineInfo(
                "Voucher tax factor", taxCategoryId, "1", 1.0, 1.0, tax);
        double factor = calculateGross(unit, taxesLogic);
        if (!Double.isFinite(factor) || RoundUtils.compare(factor, 0.0) <= 0) {
            throw new BasicException("Voucher discount found an invalid effective tax factor.");
        }
        return factor;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static final class TaxBucket {
        private final TaxInfo tax;
        private final String taxCategoryId;
        private final double grossFactor;
        private double gross;

        private TaxBucket(TaxInfo tax, String taxCategoryId, double grossFactor) {
            this.tax = tax;
            this.taxCategoryId = taxCategoryId;
            this.grossFactor = grossFactor;
        }
    }
}
