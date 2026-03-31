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

    @Column(name = "candidate_mobile", length = 30)
    private String candidateMobile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OfferStatus status = OfferStatus.DRAFT;

    @Column(name = "employee_type", length = 30)
    private String employeeType;

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

    @Column(name = "offer_release_date")
    private LocalDate offerReleaseDate;

    @Column(name = "probation_period_months")
    private Integer probationPeriodMonths;

    @Column(name = "joining_bonus", precision = 14, scale = 2)
    private BigDecimal joiningBonus;

    @Column(name = "yearly_bonus", precision = 14, scale = 2)
    private BigDecimal yearlyBonus;

    @Column(name = "annual_ctc", precision = 14, scale = 2)
    private BigDecimal annualCtc;

    @Column(nullable = false, length = 10)
    private String currency = "INR";

    @Column(name = "body_html", columnDefinition = "TEXT")
    private String bodyHtml;

    @Column(name = "pdf_generated_at")
    private Instant pdfGeneratedAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "sent_by_user_id")
    private Long sentByUserId;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "accepted_by_user_id")
    private Long acceptedByUserId;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "rejected_by_user_id")
    private Long rejectedByUserId;

    @Column(name = "joined_at")
    private Instant joinedAt;

    @Column(name = "joined_by_user_id")
    private Long joinedByUserId;

    @Column(name = "last_email_status", length = 20)
    private String lastEmailStatus;

    @Column(name = "last_email_error", length = 2000)
    private String lastEmailError;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

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
    public String getCandidateMobile() { return candidateMobile; }
    public void setCandidateMobile(String candidateMobile) { this.candidateMobile = candidateMobile; }
    public OfferStatus getStatus() { return status; }
    public void setStatus(OfferStatus status) { this.status = status; }
    public String getEmployeeType() { return employeeType; }
    public void setEmployeeType(String employeeType) { this.employeeType = employeeType; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public Designation getDesignation() { return designation; }
    public void setDesignation(Designation designation) { this.designation = designation; }
    public Employee getManager() { return manager; }
    public void setManager(Employee manager) { this.manager = manager; }
    public LocalDate getJoinDate() { return joinDate; }
    public void setJoinDate(LocalDate joinDate) { this.joinDate = joinDate; }
    public LocalDate getOfferReleaseDate() { return offerReleaseDate; }
    public void setOfferReleaseDate(LocalDate offerReleaseDate) { this.offerReleaseDate = offerReleaseDate; }
    public Integer getProbationPeriodMonths() { return probationPeriodMonths; }
    public void setProbationPeriodMonths(Integer probationPeriodMonths) { this.probationPeriodMonths = probationPeriodMonths; }
    public BigDecimal getJoiningBonus() { return joiningBonus; }
    public void setJoiningBonus(BigDecimal joiningBonus) { this.joiningBonus = joiningBonus; }
    public BigDecimal getYearlyBonus() { return yearlyBonus; }
    public void setYearlyBonus(BigDecimal yearlyBonus) { this.yearlyBonus = yearlyBonus; }
    public BigDecimal getAnnualCtc() { return annualCtc; }
    public void setAnnualCtc(BigDecimal annualCtc) { this.annualCtc = annualCtc; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getBodyHtml() { return bodyHtml; }
    public void setBodyHtml(String bodyHtml) { this.bodyHtml = bodyHtml; }
    public Instant getPdfGeneratedAt() { return pdfGeneratedAt; }
    public void setPdfGeneratedAt(Instant pdfGeneratedAt) { this.pdfGeneratedAt = pdfGeneratedAt; }
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
    public Long getSentByUserId() { return sentByUserId; }
    public void setSentByUserId(Long sentByUserId) { this.sentByUserId = sentByUserId; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(Instant acceptedAt) { this.acceptedAt = acceptedAt; }
    public Long getAcceptedByUserId() { return acceptedByUserId; }
    public void setAcceptedByUserId(Long acceptedByUserId) { this.acceptedByUserId = acceptedByUserId; }
    public Instant getRejectedAt() { return rejectedAt; }
    public void setRejectedAt(Instant rejectedAt) { this.rejectedAt = rejectedAt; }
    public Long getRejectedByUserId() { return rejectedByUserId; }
    public void setRejectedByUserId(Long rejectedByUserId) { this.rejectedByUserId = rejectedByUserId; }
    public Instant getJoinedAt() { return joinedAt; }
    public void setJoinedAt(Instant joinedAt) { this.joinedAt = joinedAt; }
    public Long getJoinedByUserId() { return joinedByUserId; }
    public void setJoinedByUserId(Long joinedByUserId) { this.joinedByUserId = joinedByUserId; }
    public String getLastEmailStatus() { return lastEmailStatus; }
    public void setLastEmailStatus(String lastEmailStatus) { this.lastEmailStatus = lastEmailStatus; }
    public String getLastEmailError() { return lastEmailError; }
    public void setLastEmailError(String lastEmailError) { this.lastEmailError = lastEmailError; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
