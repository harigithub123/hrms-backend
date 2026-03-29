package com.hrms.leave;

public enum LeaveBalanceAdjustmentKind {
    /** Changes annual allocation (grant / correction). */
    ALLOCATION,
    /** Changes carried-forward balance from prior periods. */
    CARRY_FORWARD,
    /** Reduces pool when leave lapses (taken from allocation first, then carry-forward). */
    LAPSE
}
