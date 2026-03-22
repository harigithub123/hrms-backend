package com.hrms.advance.entity;

import com.hrms.payroll.entity.Payslip;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "payslip_advance_deductions")
public class PayslipAdvanceDeduction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payslip_id", nullable = false)
    private Payslip payslip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "advance_id", nullable = false)
    private SalaryAdvance advance;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Payslip getPayslip() { return payslip; }
    public void setPayslip(Payslip payslip) { this.payslip = payslip; }
    public SalaryAdvance getAdvance() { return advance; }
    public void setAdvance(SalaryAdvance advance) { this.advance = advance; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
