package com.hrms.offers.entity;

import com.hrms.payroll.entity.SalaryComponent;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "job_offer_compensation_lines")
public class OfferCompensationLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offer_compensation_id", nullable = false)
    private OfferCompensation compensation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "component_id", nullable = false)
    private SalaryComponent component;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public OfferCompensation getCompensation() { return compensation; }
    public void setCompensation(OfferCompensation compensation) { this.compensation = compensation; }
    public SalaryComponent getComponent() { return component; }
    public void setComponent(SalaryComponent component) { this.component = component; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}

