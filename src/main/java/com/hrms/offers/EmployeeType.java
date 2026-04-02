package com.hrms.offers;
import java.util.Arrays;
import java.util.Optional;

public enum EmployeeType {

    PERMANENT_FULL_TIME("Permanent - Full Time"),
    PERMANENT_PART_TIME("Permanent - Part Time"),
    CONTRACT("Contract");

    private final String displayName;

    EmployeeType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Convert string to enum safely (case insensitive)
     */
    public static Optional<EmployeeType> fromString(String value) {
        return Arrays.stream(EmployeeType.values())
                .filter(type -> type.name().equalsIgnoreCase(value)
                        || type.displayName.equalsIgnoreCase(value))
                .findFirst();
    }

    /**
     * Strict conversion (throws exception if not found)
     */
    public static EmployeeType fromStringOrThrow(String value) {
        return fromString(value)
                .orElseThrow(() -> new IllegalArgumentException("Invalid EmployeeType: " + value));
    }

    /**
     * Check if employee type is permanent
     */
    public boolean isPermanent() {
        return this == PERMANENT_FULL_TIME || this == PERMANENT_PART_TIME;
    }

    /**
     * Check if employee type is contract
     */
    public boolean isContract() {
        return this == CONTRACT;
    }
}