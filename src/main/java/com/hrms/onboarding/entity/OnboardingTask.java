package com.hrms.onboarding.entity;

import com.hrms.onboarding.OnboardingTaskStatus;
import jakarta.persistence.*;

@Entity
@Table(name = "onboarding_tasks")
public class OnboardingTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false)
    private OnboardingCase onboardingCase;

    @Column(nullable = false, length = 300)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OnboardingTaskStatus status = OnboardingTaskStatus.PENDING;

    @Column(nullable = false)
    private boolean done;

    @Column(name = "comment_text", length = 2000)
    private String commentText;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public OnboardingCase getOnboardingCase() { return onboardingCase; }
    public void setOnboardingCase(OnboardingCase onboardingCase) { this.onboardingCase = onboardingCase; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public OnboardingTaskStatus getStatus() { return status; }
    public void setStatus(OnboardingTaskStatus status) { this.status = status; }
    public boolean isDone() { return done; }
    public void setDone(boolean done) { this.done = done; }
    public String getCommentText() { return commentText; }
    public void setCommentText(String commentText) { this.commentText = commentText; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
