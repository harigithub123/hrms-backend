package com.hrms.onboarding.entity;

import com.hrms.auth.entity.User;
import com.hrms.offers.entity.JobOffer;
import com.hrms.onboarding.OnboardingStatus;
import com.hrms.org.entity.Department;
import com.hrms.org.entity.Designation;
import com.hrms.org.entity.Employee;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "onboarding_cases")
public class OnboardingCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OnboardingStatus status = OnboardingStatus.DRAFT;

    @Column(name = "candidate_first_name", nullable = false, length = 100)
    private String candidateFirstName;

    @Column(name = "candidate_last_name", nullable = false, length = 100)
    private String candidateLastName;

    @Column(name = "candidate_email", length = 255)
    private String candidateEmail;

    @Column(name = "join_date", nullable = false)
    private LocalDate joinDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "designation_id")
    private Designation designation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Employee manager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offer_id")
    private JobOffer offer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_hr_user_id")
    private User assignedHr;

    @Column(length = 2000)
    private String notes;

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
    public OnboardingStatus getStatus() { return status; }
    public void setStatus(OnboardingStatus status) { this.status = status; }
    public String getCandidateFirstName() { return candidateFirstName; }
    public void setCandidateFirstName(String candidateFirstName) { this.candidateFirstName = candidateFirstName; }
    public String getCandidateLastName() { return candidateLastName; }
    public void setCandidateLastName(String candidateLastName) { this.candidateLastName = candidateLastName; }
    public String getCandidateEmail() { return candidateEmail; }
    public void setCandidateEmail(String candidateEmail) { this.candidateEmail = candidateEmail; }
    public LocalDate getJoinDate() { return joinDate; }
    public void setJoinDate(LocalDate joinDate) { this.joinDate = joinDate; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public Designation getDesignation() { return designation; }
    public void setDesignation(Designation designation) { this.designation = designation; }
    public Employee getManager() { return manager; }
    public void setManager(Employee manager) { this.manager = manager; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public JobOffer getOffer() { return offer; }
    public void setOffer(JobOffer offer) { this.offer = offer; }
    public User getAssignedHr() { return assignedHr; }
    public void setAssignedHr(User assignedHr) { this.assignedHr = assignedHr; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
