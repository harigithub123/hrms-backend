package com.hrms.onboarding.entity;

import com.hrms.onboarding.OnboardingBankAccountType;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "onboarding_bank_details")
public class OnboardingBankDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false, unique = true)
    private OnboardingCase onboardingCase;

    @Column(name = "account_holder_name", nullable = false, length = 200)
    private String accountHolderName;

    @Column(name = "bank_name", nullable = false, length = 200)
    private String bankName;

    @Column(length = 200)
    private String branch;

    @Column(name = "account_number", nullable = false, length = 80)
    private String accountNumber;

    @Column(name = "ifsc_code", nullable = false, length = 20)
    private String ifscCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private OnboardingBankAccountType accountType = OnboardingBankAccountType.SAVINGS;

    @Column(length = 500)
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
    public OnboardingCase getOnboardingCase() { return onboardingCase; }
    public void setOnboardingCase(OnboardingCase onboardingCase) { this.onboardingCase = onboardingCase; }
    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }
    public OnboardingBankAccountType getAccountType() { return accountType; }
    public void setAccountType(OnboardingBankAccountType accountType) { this.accountType = accountType; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
