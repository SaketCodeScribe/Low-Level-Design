package Problems.FinancialAndPaymentSystems.AuditLoggingSystem;

import java.util.HashMap;
import java.util.Map;

public enum IssueType {
    Payment_Related("Payment Related"),
    Gold_Related( "Gold Related"),
    MutualFund_Related("Mutual Fund Related"),
    Insurance_Related("Insurance Related");

    private final String value;
    private static final Map<String, IssueType> map = new HashMap<>();

    static {
        for (IssueType type : values()) {
            map.put(type.value, type);
        }
    }

    IssueType(String value) {
        this.value = value;
    }

    public static IssueType getType(String value) {
        IssueType type = map.get(value);
        if (type == null) {
            throw new IllegalArgumentException("Unknown issue type: " + value);
        }
        return type;
    }

    public String getValue() {
        return value;
    }
}