package com.hrms.leave.entity;

import com.hrms.auth.entity.User;
import com.hrms.leave.LeaveBalanceAdjustmentKind;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "leave_balance_adjustments")
public class LeaveBalanceAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_balance_id")
    private LeaveBalance leaveBalance;

    @Enumerated(EnumType.STRING)
    @Column(name = "adjustment_kind", nullable = false, length = 30)
    private LeaveBalanceAdjustmentKind kind;

    @Column(name = "delta_days", nullable = false)
    private BigDecimal deltaDays;

    @Column(name = "comment_text", nullable = false, length = 2000)
    private String commentText;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LeaveBalance getLeaveBalance() { return leaveBalance; }
    public void setLeaveBalance(LeaveBalance leaveBalance) { this.leaveBalance = leaveBalance; }
    public LeaveBalanceAdjustmentKind getKind() { return kind; }
    public void setKind(LeaveBalanceAdjustmentKind kind) { this.kind = kind; }
    public BigDecimal getDeltaDays() { return deltaDays; }
    public void setDeltaDays(BigDecimal deltaDays) { this.deltaDays = deltaDays; }
    public String getCommentText() { return commentText; }
    public void setCommentText(String commentText) { this.commentText = commentText; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
}
