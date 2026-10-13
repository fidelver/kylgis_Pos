//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.voucher;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Generates non-sequential voucher codes and centralizes the normalization
 * rules shared by automatic and manually edited codes.
 */
public final class VoucherCodeGenerator {

    public static final int MAX_LENGTH = 100;
    private static final String PREFIX = "VO";
    private static final int GROUPS = 3;
    private static final int GROUP_LENGTH = 4;
    private static final char[] ALPHABET =
            "23456789ABCDEFGHJKMNPQRSTUVWXYZ".toCharArray();
    private static final Pattern ACCEPTED = Pattern.compile(
            "[A-Z0-9](?:[A-Z0-9-]{0,98}[A-Z0-9])?");
    private static final SecureRandom RANDOM = new SecureRandom();

    private VoucherCodeGenerator() {
    }

    /**
     * Creates a human-readable random proposal such as VO-7KQW-M8XT-3N5P.
     * The database uniqueness constraint remains the authoritative collision
     * barrier; randomness only makes collisions very unlikely and codes
     * non-predictable.
     */
    public static String generate() {
        StringBuilder code = new StringBuilder(PREFIX);
        for (int group = 0; group < GROUPS; group++) {
            code.append('-');
            for (int index = 0; index < GROUP_LENGTH; index++) {
                code.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
            }
        }
        return code.toString();
    }

    /**
     * Normalizes user-entered voucher codes without rewriting historical
     * formats: trim surrounding whitespace and use locale-independent upper
     * case. Internal characters are otherwise preserved for validation.
     */
    public static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Accepts historical/manual alphanumeric-hyphen codes while rejecting
     * whitespace, control characters, leading/trailing hyphens and values that
     * cannot fit the current VARCHAR(100) schema.
     */
    public static boolean isValid(String value) {
        String normalized = normalize(value);
        return !normalized.isEmpty()
                && normalized.length() <= MAX_LENGTH
                && ACCEPTED.matcher(normalized).matches();
    }
}
