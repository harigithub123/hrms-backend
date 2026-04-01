package com.hrms.compensation.entity;

import com.hrms.compensation.CompensationFrequency;
import com.hrms.payroll.entity.SalaryComponent;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "employee_compensation_lines")
public class EmployeeCompensationLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compensation_id", nullable = false)
    private EmployeeCompensation compensation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "component_id", nullable = false)
    private SalaryComponent component;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CompensationFrequency frequency = CompensationFrequency.MONTHLY;

    @Column(name = "payable_on")
    private LocalDate payableOn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public EmployeeCompensation getCompensation() { return compensation; }
    public void setCompensation(EmployeeCompensation compensation) { this.compensation = compensation; }
    public SalaryComponent getComponent() { return component; }
    public void setComponent(SalaryComponent component) { this.component = component; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public CompensationFrequency getFrequency() { return frequency; }
    public void setFrequency(CompensationFrequency frequency) { this.frequency = frequency; }
    public LocalDate getPayableOn() { return payableOn; }
    public void setPayableOn(LocalDate payableOn) { this.payableOn = payableOn; }
}
