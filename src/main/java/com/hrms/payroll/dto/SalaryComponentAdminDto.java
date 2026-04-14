package com.hrms.payroll.dto;

import com.hrms.payroll.SalaryComponentKind;
import com.hrms.payroll.entity.SalaryComponent;

import java.math.BigDecimal;

/**
 * Admin/master-data view: salary component definition + optional org-wide fixed monthly amount.
 */
public record SalaryComponentAdminDto(
        Long id,
        String name,
        SalaryComponentKind kind,
        int sortOrder,
        boolean active,
        BigDecimal fixedMonthlyAmount
) {
    public static SalaryComponentAdminDto from(SalaryComponent c, BigDecimal fixedMonthlyAmount) {
        return new SalaryComponentAdminDto(
                c.getId(),
                c.getName(),
                c.getKind(),
                c.getSortOrder(),
                c.isActive(),
                fixedMonthlyAmount
        );
    }
}

