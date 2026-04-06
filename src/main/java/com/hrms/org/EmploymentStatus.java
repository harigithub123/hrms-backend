package com.hrms.org;

/**
 * Lifecycle / separation status for an employee record.
 */
public enum EmploymentStatus {
    JOINED,
    TERMINATED,
    RETIRED,
    ABSCONDED,
    EXITED,
    FULL_AND_FINAL_PENDING,
    RESIGNED;

    public static boolean isSeparation(EmploymentStatus s) {
        return s != null && s != JOINED;
    }
}
