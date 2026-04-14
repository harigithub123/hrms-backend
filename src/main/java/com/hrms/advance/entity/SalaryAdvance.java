package com.hrms.advance.entity;

import com.hrms.advance.AdvanceStatus;
import com.hrms.auth.entity.User;
import com.hrms.org.entity.Employee;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "salary_advances")
public class SalaryAdvance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(length = 2000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdvanceStatus status = AdvanceStatus.PENDING;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_user_id")
    private User approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rejected_reason", length = 1000)
    private String rejectedReason;

    @Column(name = "payout_date")
    private LocalDate payoutDate;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "recovery_months", nullable = false)
    private int recoveryMonths = 1;

    @Column(name = "recovery_amount_per_month", precision = 14, scale = 2)
    private BigDecimal recoveryAmountPerMonth;

    @Column(name = "outstanding_balance", precision = 14, scale = 2)
    private BigDecimal outstandingBalance;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant n = Instant.now();
        requestedAt = n;
        createdAt = n;
        updatedAt = n;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public AdvanceStatus getStatus() { return status; }
    public void setStatus(AdvanceStatus status) { this.status = status; }
    public Instant getRequestedAt() { return requestedAt; }
    public User getApprovedBy() { return approvedBy; }
    public void setApprovedBy(User approvedBy) { this.approvedBy = approvedBy; }
    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
    public String getRejectedReason() { return rejectedReason; }
    public void setRejectedReason(String rejectedReason) { this.rejectedReason = rejectedReason; }
    public LocalDate getPayoutDate() { return payoutDate; }
    public void setPayoutDate(LocalDate payoutDate) { this.payoutDate = payoutDate; }
    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
    public int getRecoveryMonths() { return recoveryMonths; }
    public void setRecoveryMonths(int recoveryMonths) { this.recoveryMonths = recoveryMonths; }
    public BigDecimal getRecoveryAmountPerMonth() { return recoveryAmountPerMonth; }
    public void setRecoveryAmountPerMonth(BigDecimal recoveryAmountPerMonth) { this.recoveryAmountPerMonth = recoveryAmountPerMonth; }
    public BigDecimal getOutstandingBalance() { return outstandingBalance; }
    public void setOutstandingBalance(BigDecimal outstandingBalance) { this.outstandingBalance = outstandingBalance; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
