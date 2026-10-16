//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.voucher;

/** Generates deterministic human-readable voucher codes for one campaign. */
public final class VoucherCampaignCodeGenerator {

    private VoucherCampaignCodeGenerator() {
    }

    /**
     * Generates PREFIXA, PREFIXB ... PREFIXZ, PREFIXAA ... using a zero-based
     * sequence. Prefix normalization and final syntax follow VoucherCodeGenerator.
     */
    public static String generate(String prefix, int sequence) {
        if (sequence < 0) {
            throw new IllegalArgumentException("Voucher campaign sequence cannot be negative.");
        }
        String normalizedPrefix = VoucherCodeGenerator.normalize(prefix);
        if (normalizedPrefix.isEmpty()) {
            throw new IllegalArgumentException("Voucher campaign prefix is required.");
        }
        String code = normalizedPrefix + suffix(sequence);
        if (!VoucherCodeGenerator.isValid(code)) {
            throw new IllegalArgumentException("Generated voucher code is invalid or exceeds 100 characters: " + code);
        }
        return code;
    }

    static String suffix(int sequence) {
        StringBuilder suffix = new StringBuilder();
        int value = sequence;
        do {
            int remainder = value % 26;
            suffix.append((char) ('A' + remainder));
            value = (value / 26) - 1;
        } while (value >= 0);
        return suffix.reverse().toString();
    }
}
