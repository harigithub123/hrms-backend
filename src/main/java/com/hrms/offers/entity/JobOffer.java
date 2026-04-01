package com.hrms.offers.entity;

import com.hrms.offers.OfferStatus;
import com.hrms.org.entity.Department;
import com.hrms.org.entity.Designation;
import com.hrms.org.entity.Employee;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "job_offers")
public class JobOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(name = "joining_date")
    private LocalDate joiningDate;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private java.time.Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        java.time.Instant n = java.time.Instant.now();
        createdAt = n;
        updatedAt = n;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public LocalDate getJoiningDate() { return joiningDate; }
    public void setJoiningDate(LocalDate joiningDate) { this.joiningDate = joiningDate; }
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
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public java.time.Instant getCreatedAt() { return createdAt; }
    public java.time.Instant getUpdatedAt() { return updatedAt; }
}
