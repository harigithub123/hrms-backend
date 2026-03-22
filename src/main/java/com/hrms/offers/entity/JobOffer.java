package com.hrms.offers.entity;

import com.hrms.offers.OfferStatus;
import com.hrms.org.entity.Department;
import com.hrms.org.entity.Designation;
import com.hrms.org.entity.Employee;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "job_offers")
public class JobOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private OfferTemplate template;

    @Column(name = "candidate_name", nullable = false, length = 200)
    private String candidateName;

    @Column(name = "candidate_email", length = 255)
    private String candidateEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OfferStatus status = OfferStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "designation_id")
    private Designation designation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Employee manager;

    @Column(name = "join_date")
    private LocalDate joinDate;

    @Column(name = "annual_ctc", precision = 14, scale = 2)
    private BigDecimal annualCtc;

    @Column(nullable = false, length = 10)
    private String currency = "INR";

    @Column(name = "body_html", columnDefinition = "TEXT")
    private String bodyHtml;

    @Column(name = "pdf_generated_at")
    private Instant pdfGeneratedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant n = Instant.now();
        createdAt = n;
        updatedAt = n;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public OfferTemplate getTemplate() { return template; }
    public void setTemplate(OfferTemplate template) { this.template = template; }
    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }
    public String getCandidateEmail() { return candidateEmail; }
    public void setCandidateEmail(String candidateEmail) { this.candidateEmail = candidateEmail; }
    public OfferStatus getStatus() { return status; }
    public void setStatus(OfferStatus status) { this.status = status; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public Designation getDesignation() { return designation; }
    public void setDesignation(Designation designation) { this.designation = designation; }
    public Employee getManager() { return manager; }
    public void setManager(Employee manager) { this.manager = manager; }
    public LocalDate getJoinDate() { return joinDate; }
    public void setJoinDate(LocalDate joinDate) { this.joinDate = joinDate; }
    public BigDecimal getAnnualCtc() { return annualCtc; }
    public void setAnnualCtc(BigDecimal annualCtc) { this.annualCtc = annualCtc; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getBodyHtml() { return bodyHtml; }
    public void setBodyHtml(String bodyHtml) { this.bodyHtml = bodyHtml; }
    public Instant getPdfGeneratedAt() { return pdfGeneratedAt; }
    public void setPdfGeneratedAt(Instant pdfGeneratedAt) { this.pdfGeneratedAt = pdfGeneratedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
