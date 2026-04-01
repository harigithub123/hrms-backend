package com.hrms.offers.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "job_offer_compensation")
public class OfferCompensation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offer_id", nullable = false, unique = true)
    private JobOffer offer;

    @Column(nullable = false, length = 10)
    private String currency = "INR";

    @Column(name = "annual_ctc", precision = 14, scale = 2)
    private BigDecimal annualCtc;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "compensation", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<OfferCompensationLine> offerCompensationLine = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public JobOffer getOffer() { return offer; }
    public void setOffer(JobOffer offer) { this.offer = offer; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getAnnualCtc() { return annualCtc; }
    public void setAnnualCtc(BigDecimal annualCtc) { this.annualCtc = annualCtc; }
    public Instant getCreatedAt() { return createdAt; }
    public List<OfferCompensationLine> getOfferCompensationLine() { return offerCompensationLine; }
}

