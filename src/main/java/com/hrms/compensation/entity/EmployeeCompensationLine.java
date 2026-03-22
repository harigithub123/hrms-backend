package com.hrms.compensation.entity;

import com.hrms.payroll.entity.SalaryComponent;
import jakarta.persistence.*;

import java.math.BigDecimal;

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public EmployeeCompensation getCompensation() { return compensation; }
    public void setCompensation(EmployeeCompensation compensation) { this.compensation = compensation; }
    public SalaryComponent getComponent() { return component; }
    public void setComponent(SalaryComponent component) { this.component = component; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
