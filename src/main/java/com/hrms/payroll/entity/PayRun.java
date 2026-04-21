package com.hrms.payroll.entity;

import com.hrms.payroll.PayRunStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;

@Entity
@Table(name = "pay_runs")
public class PayRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pay_year", nullable = false)
    private int payYear;

    @Column(name = "pay_month", nullable = false)
    private int payMonth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PayRunStatus status = PayRunStatus.DRAFT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    /** First day of the pay run calendar month (not persisted). */
    public LocalDate resolvePeriodStart() {
        return YearMonth.of(payYear, payMonth).atDay(1);
    }

    /** Last day of the pay run calendar month (not persisted). */
    public LocalDate resolvePeriodEnd() {
        return YearMonth.of(payYear, payMonth).atEndOfMonth();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getPayYear() { return payYear; }
    public void setPayYear(int payYear) { this.payYear = payYear; }
    public int getPayMonth() { return payMonth; }
    public void setPayMonth(int payMonth) { this.payMonth = payMonth; }
    public PayRunStatus getStatus() { return status; }
    public void setStatus(PayRunStatus status) { this.status = status; }
}
