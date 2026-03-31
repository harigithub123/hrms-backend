package com.hrms.leave;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Pro-rata leave entitlement for the join calendar year:
 * annual days × (inclusive days from join through 31 Dec) / length of that year in days.
 */
public final class LeaveProRataCalculator {

    private LeaveProRataCalculator() {}

    public static BigDecimal proRataAllocatedDaysForJoinYear(BigDecimal daysPerYear, LocalDate joinDate) {
        if (daysPerYear == null || daysPerYear.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        LocalDate join = joinDate != null ? joinDate : LocalDate.now();
        int year = join.getYear();
        LocalDate yearEnd = LocalDate.of(year, 12, 31);
        if (join.isAfter(yearEnd)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        long remainingInclusive = ChronoUnit.DAYS.between(join, yearEnd) + 1;
        int daysInYear = join.lengthOfYear();
        if (daysInYear <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal ratio = BigDecimal.valueOf(remainingInclusive)
                .divide(BigDecimal.valueOf(daysInYear), 10, RoundingMode.HALF_UP);
        BigDecimal allocated = daysPerYear.multiply(ratio);
        if (allocated.compareTo(daysPerYear) > 0) {
            allocated = daysPerYear;
        }
        return allocated.setScale(2, RoundingMode.HALF_UP);
    }
}
