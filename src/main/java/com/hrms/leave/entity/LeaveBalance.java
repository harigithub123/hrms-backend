package com.hrms.leave.entity;

import com.hrms.org.entity.Employee;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "leave_balances",
        uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "leave_type_id", "year"})
)
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id")
    private LeaveType leaveType;

    @Column(nullable = false)
    private int year;

    @Column(name = "allocated_days", nullable = false)
    private BigDecimal allocatedDays = BigDecimal.ZERO;

    @Column(name = "used_days", nullable = false)
    private BigDecimal usedDays = BigDecimal.ZERO;

    @Column(name = "carry_forwarded_days", nullable = false)
    private BigDecimal carryForwardedDays = BigDecimal.ZERO;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public LeaveType getLeaveType() { return leaveType; }
    public void setLeaveType(LeaveType leaveType) { this.leaveType = leaveType; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public BigDecimal getAllocatedDays() { return allocatedDays; }
    public void setAllocatedDays(BigDecimal allocatedDays) { this.allocatedDays = allocatedDays; }
    public BigDecimal getUsedDays() { return usedDays; }
    public void setUsedDays(BigDecimal usedDays) { this.usedDays = usedDays; }
    public BigDecimal getCarryForwardedDays() { return carryForwardedDays; }
    public void setCarryForwardedDays(BigDecimal carryForwardedDays) { this.carryForwardedDays = carryForwardedDays; }
}
