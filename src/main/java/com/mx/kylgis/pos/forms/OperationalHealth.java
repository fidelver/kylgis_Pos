//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.forms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runtime health registry. It separates visual severity from functional
 * capability blocking so recoverable faults can be repaired from the POS.
 */
public final class OperationalHealth {

    public enum Category {
        DATABASE("BBDD"),
        PERIPHERALS("Periféricos"),
        RESOURCES("Recursos");

        private final String label;

        Category(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum Severity {
        GREEN(0), AMBER(1), RED(2);

        private final int rank;

        Severity(int rank) {
            this.rank = rank;
        }

        public Severity max(Severity other) {
            return other != null && other.rank > rank ? other : this;
        }
    }

    public static final String CAP_STARTUP = "startup";
    public static final String CAP_PRINT_RECEIPT = "print.receipt";
    public static final String CAP_PRINT_RECEIPT_ALT = "print.receipt.alt";
    public static final String CAP_PRINT_TOTAL = "print.total";
    public static final String CAP_PRINT_LINE = "print.line";
    public static final String CAP_PRINT_REPRINT = "print.reprint";
    public static final String CAP_PRINT_INVENTORY = "print.inventory";
    public static final String CAP_PRINT_PREVIEW = "print.preview";
    public static final String CAP_PRINT_CASH = "print.cash";
    public static final String CAP_PRINT_CUSTOMER = "print.customer";
    public static final String CAP_SCALE = "scale";
    public static final String CAP_SCANNER = "scanner";
    public static final String CAP_DISPLAY = "display";
    public static final String CAP_IBUTTON = "ibutton";

    public static final class Issue {
        private final String code;
        private final Severity severity;
        private final String message;
        private final String blockedCapability;

        private Issue(String code, Severity severity, String message, String blockedCapability) {
            this.code = code;
            this.severity = severity;
            this.message = message;
            this.blockedCapability = blockedCapability;
        }

        public String getCode() { return code; }
        public Severity getSeverity() { return severity; }
        public String getMessage() { return message; }
        public String getBlockedCapability() { return blockedCapability; }
    }

    private final Map<Category, LinkedHashMap<String, Issue>> issues =
            new EnumMap<>(Category.class);

    public OperationalHealth() {
        for (Category category : Category.values()) {
            issues.put(category, new LinkedHashMap<>());
        }
    }

    public synchronized void clear(Category category) {
        issues.get(category).clear();
    }

    public synchronized void clearIssue(Category category, String code) {
        if (code != null) {
            issues.get(category).remove(code);
        }
    }

    public synchronized void report(Category category, String code,
            Severity severity, String message, String blockedCapability) {
        if (category == null || code == null || code.trim().isEmpty() || severity == null) {
            return;
        }
        issues.get(category).put(code,
                new Issue(code, severity, message == null ? "" : message, blockedCapability));
    }

    public synchronized Severity getSeverity(Category category) {
        Severity result = Severity.GREEN;
        for (Issue issue : issues.get(category).values()) {
            result = result.max(issue.severity);
        }
        return result;
    }

    public synchronized List<Issue> getIssues(Category category) {
        return Collections.unmodifiableList(new ArrayList<>(issues.get(category).values()));
    }

    public synchronized boolean isCapabilityAvailable(String capability) {
        if (capability == null || capability.trim().isEmpty()) {
            return true;
        }
        for (Map<String, Issue> categoryIssues : issues.values()) {
            for (Issue issue : categoryIssues.values()) {
                if (capability.equals(issue.blockedCapability)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static String capabilityForPrinterResource(String resourceName) {
        if (resourceName == null) {
            return CAP_PRINT_RECEIPT;
        }
        switch (resourceName) {
            case "Printer.Ticket":
                return CAP_PRINT_RECEIPT;
            case "Printer.Ticket2":
                return CAP_PRINT_RECEIPT_ALT;
            case "Printer.TicketTotal":
                return CAP_PRINT_TOTAL;
            case "Printer.TicketLine":
                return CAP_PRINT_LINE;
            case "Printer.ReprintTicket":
            case "Printer.ReprintLastTicket":
                return CAP_PRINT_REPRINT;
            case "Printer.TicketPreview":
            case "Printer.TicketPreview2":
            case "Printer.CloseCash.Preview":
                return CAP_PRINT_PREVIEW;
            case "Printer.Inventory":
                return CAP_PRINT_INVENTORY;
            case "Printer.CloseCash":
            case "Printer.PartialCash":
                return CAP_PRINT_CASH;
            case "Printer.CustomerPaid":
            case "Printer.CustomerPaid2":
                return CAP_PRINT_CUSTOMER;
            default:
                return CAP_PRINT_RECEIPT;
        }
    }

    public synchronized String getCapabilityBlockReason(String capability) {
        if (capability == null) {
            return null;
        }
        for (Map<String, Issue> categoryIssues : issues.values()) {
            for (Issue issue : categoryIssues.values()) {
                if (capability.equals(issue.blockedCapability)) {
                    return issue.message;
                }
            }
        }
        return null;
    }
}
