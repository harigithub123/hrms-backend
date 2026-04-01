package com.hrms.offers.entity;

import com.hrms.offers.OfferEventAction;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "job_offer_events")
public class JobOfferEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offer_id", nullable = false)
    private JobOffer offer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private OfferEventAction action;

    @Column(name = "action_at", nullable = false)
    private Instant actionAt;

    @Column(name = "action_by_user_id")
    private Long actionByUserId;

    @Column(length = 2000)
    private String remark;

    @PrePersist
    protected void onCreate() {
        if (actionAt == null) actionAt = Instant.now();
    }

    public Long getId() { return id; }

    public JobOffer getOffer() { return offer; }
    public void setOffer(JobOffer offer) { this.offer = offer; }

    public OfferEventAction getAction() { return action; }
    public void setAction(OfferEventAction action) { this.action = action; }

    public Instant getActionAt() { return actionAt; }
    public void setActionAt(Instant actionAt) { this.actionAt = actionAt; }

    public Long getActionByUserId() { return actionByUserId; }
    public void setActionByUserId(Long actionByUserId) { this.actionByUserId = actionByUserId; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}

