package com.hrms.onboarding.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "onboarding_task_audits")
public class OnboardingTaskAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private OnboardingTask task;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(length = 2000)
    private String detail;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "created_by_username", length = 100)
    private String createdByUsername;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public OnboardingTask getTask() { return task; }
    public void setTask(OnboardingTask task) { this.task = task; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public Long getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(Long createdByUserId) { this.createdByUserId = createdByUserId; }
    public String getCreatedByUsername() { return createdByUsername; }
    public void setCreatedByUsername(String createdByUsername) { this.createdByUsername = createdByUsername; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
