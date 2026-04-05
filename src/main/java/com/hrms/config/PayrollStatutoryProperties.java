package com.hrms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "hrms.payroll.statutory")
public class PayrollStatutoryProperties {

    /**
     * Employee PF (or similar) deducted every month when greater than zero. Set to 0 to disable.
     */
    private BigDecimal providentFundMonthly = new BigDecimal("1800");

    /**
     * Professional tax deducted every month when &gt; 0. Set to 0 to disable.
     */
    private BigDecimal professionalTaxMonthly = new BigDecimal("200");

    public BigDecimal getProvidentFundMonthly() {
        return providentFundMonthly;
    }

    public void setProvidentFundMonthly(BigDecimal providentFundMonthly) {
        this.providentFundMonthly = providentFundMonthly;
    }

    public BigDecimal getProfessionalTaxMonthly() {
        return professionalTaxMonthly;
    }

    public void setProfessionalTaxMonthly(BigDecimal professionalTaxMonthly) {
        this.professionalTaxMonthly = professionalTaxMonthly;
    }
}
