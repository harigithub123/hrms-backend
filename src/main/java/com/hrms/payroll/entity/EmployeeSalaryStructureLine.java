package com.hrms.payroll.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "employee_salary_structure_lines", uniqueConstraints = @UniqueConstraint(columnNames = {"structure_id", "component_id"}))
public class EmployeeSalaryStructureLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "structure_id")
    private EmployeeSalaryStructure structure;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "component_id")
    private SalaryComponent component;

    @Column(nullable = false)
    private BigDecimal amount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public EmployeeSalaryStructure getStructure() { return structure; }
    public void setStructure(EmployeeSalaryStructure structure) { this.structure = structure; }
    public SalaryComponent getComponent() { return component; }
    public void setComponent(SalaryComponent component) { this.component = component; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
