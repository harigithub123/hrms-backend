package com.hrms.payroll;

import com.hrms.advance.AdvanceStatus;
import com.hrms.advance.entity.SalaryAdvance;
import com.hrms.advance.repository.PayslipAdvanceDeductionRepository;
import com.hrms.advance.repository.SalaryAdvanceRepository;
import com.hrms.auth.entity.Role;
import com.hrms.auth.entity.User;
import com.hrms.compensation.CompensationFrequency;
import com.hrms.compensation.entity.EmployeeCompensation;
import com.hrms.compensation.entity.EmployeeCompensationLine;
import com.hrms.compensation.repository.EmployeeCompensationRepository;
import com.hrms.org.entity.Employee;
import com.hrms.org.repository.EmployeeRepository;
import com.hrms.payroll.dto.PayRunCreateRequest;
import com.hrms.payroll.dto.PayRunDto;
import com.hrms.payroll.dto.PayslipDto;
import com.hrms.payroll.dto.SalaryComponentRequest;
import com.hrms.payroll.entity.PayRun;
import com.hrms.payroll.entity.Payslip;
import com.hrms.payroll.entity.SalaryComponent;
import com.hrms.payroll.repository.PayRunRepository;
import com.hrms.payroll.repository.PayslipRepository;
import com.hrms.payroll.repository.SalaryComponentRepository;
import com.hrms.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {

    @Mock
    private SalaryComponentRepository salaryComponentRepository;
    @Mock
    private EmployeeCompensationRepository compensationRepository;
    @Mock
    private PayRunRepository payRunRepository;
    @Mock
    private PayslipRepository payslipRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private PayslipPdfService payslipPdfService;
    @Mock
    private SalaryAdvanceRepository salaryAdvanceRepository;
    @Mock
    private PayslipAdvanceDeductionRepository payslipAdvanceDeductionRepository;

    private PayrollService payrollService;

    @BeforeEach
    void setUp() {
        payrollService = new PayrollService(
                salaryComponentRepository,
                compensationRepository,
                payRunRepository,
                payslipRepository,
                employeeRepository,
                currentUserService,
                payslipPdfService,
                salaryAdvanceRepository,
                payslipAdvanceDeductionRepository
        );
    }

    @Test
    void listComponents_returnsActiveOrdered() {
        SalaryComponent c = new SalaryComponent();
        c.setId(1L);
        c.setCode("BASIC");
        c.setName("Basic");
        c.setKind(SalaryComponentKind.EARNING);
        c.setSortOrder(1);
        c.setActive(true);
        when(salaryComponentRepository.findByActiveTrueOrderBySortOrderAsc()).thenReturn(List.of(c));

        var result = payrollService.listComponents();

        assertEquals(1, result.size());
        assertEquals("BASIC", result.get(0).code());
        verify(salaryComponentRepository).findByActiveTrueOrderBySortOrderAsc();
        verifyNoInteractions(currentUserService);
    }

    @Test
    void listAllComponentsAdmin_whenHr_succeeds() {
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser());
        SalaryComponent c = new SalaryComponent();
        c.setId(2L);
        c.setCode("Z");
        c.setName("Zed");
        c.setKind(SalaryComponentKind.DEDUCTION);
        c.setSortOrder(5);
        c.setActive(false);
        when(salaryComponentRepository.findAll()).thenReturn(List.of(c));

        var result = payrollService.listAllComponentsAdmin();

        assertEquals(1, result.size());
        assertEquals("Z", result.get(0).code());
    }

    @Test
    void listAllComponentsAdmin_whenNotHr_throwsForbidden() {
        when(currentUserService.requireCurrentUser()).thenReturn(employeeUser());

        assertThrows(ResponseStatusException.class, () -> payrollService.listAllComponentsAdmin());
    }

    @Test
    void createComponent_whenHr_saves() {
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser());
        when(salaryComponentRepository.existsByCodeIgnoreCase("new")).thenReturn(false);
        SalaryComponent saved = new SalaryComponent();
        saved.setId(10L);
        saved.setCode("NEW");
        saved.setName("New Comp");
        saved.setKind(SalaryComponentKind.EARNING);
        saved.setSortOrder(1);
        saved.setActive(true);
        when(salaryComponentRepository.save(any(SalaryComponent.class))).thenReturn(saved);

        var req = new SalaryComponentRequest("new", "  New Comp  ", SalaryComponentKind.EARNING, 1, true);
        var dto = payrollService.createComponent(req);

        assertEquals("NEW", dto.code());
        assertEquals("New Comp", dto.name());
        verify(salaryComponentRepository).save(argThat(sc -> "NEW".equals(sc.getCode())));
    }

    @Test
    void createComponent_whenDuplicateCode_throws() {
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser());
        when(salaryComponentRepository.existsByCodeIgnoreCase("dup")).thenReturn(true);

        var req = new SalaryComponentRequest("dup", "Dup", SalaryComponentKind.EARNING, 1, true);

        assertThrows(IllegalArgumentException.class, () -> payrollService.createComponent(req));
        verify(salaryComponentRepository, never()).save(any());
    }

    @Test
    void updateComponent_whenNotFound_throws() {
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser());
        when(salaryComponentRepository.findById(99L)).thenReturn(Optional.empty());

        var req = new SalaryComponentRequest("X", "X", SalaryComponentKind.EARNING, 1, true);

        assertThrows(IllegalArgumentException.class, () -> payrollService.updateComponent(99L, req));
    }

    @Test
    void updateComponent_whenCodeBelongsToOther_throws() {
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser());
        SalaryComponent existing = new SalaryComponent();
        existing.setId(1L);
        when(salaryComponentRepository.findById(1L)).thenReturn(Optional.of(existing));
        SalaryComponent other = new SalaryComponent();
        other.setId(2L);
        when(salaryComponentRepository.findByCodeIgnoreCase("other")).thenReturn(Optional.of(other));

        var req = new SalaryComponentRequest("other", "Name", SalaryComponentKind.EARNING, 1, true);

        assertThrows(IllegalArgumentException.class, () -> payrollService.updateComponent(1L, req));
    }

    @Test
    void updateComponent_whenHr_updatesFields() {
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser());
        SalaryComponent existing = new SalaryComponent();
        existing.setId(3L);
        existing.setCode("OLD");
        when(salaryComponentRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(salaryComponentRepository.findByCodeIgnoreCase("old")).thenReturn(Optional.of(existing));
        when(salaryComponentRepository.save(existing)).thenReturn(existing);

        var req = new SalaryComponentRequest("old", "Renamed", SalaryComponentKind.DEDUCTION, 2, false);
        payrollService.updateComponent(3L, req);

        assertEquals("OLD", existing.getCode());
        assertEquals("Renamed", existing.getName());
        assertEquals(SalaryComponentKind.DEDUCTION, existing.getKind());
        assertEquals(2, existing.getSortOrder());
        assertFalse(existing.isActive());
    }

    @Test
    void createPayRun_whenPeriodEndBeforeStart_throws() {
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser());
        LocalDate start = LocalDate.of(2026, 4, 30);
        LocalDate end = LocalDate.of(2026, 4, 1);
        var req = new PayRunCreateRequest(start, end);

        assertThrows(IllegalArgumentException.class, () -> payrollService.createPayRun(req));
        verify(payRunRepository, never()).save(any());
    }

    @Test
    void createPayRun_whenPeriodExists_throws() {
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser());
        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate end = LocalDate.of(2026, 4, 30);
        when(payRunRepository.existsByPeriodStartAndPeriodEnd(start, end)).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> payrollService.createPayRun(new PayRunCreateRequest(start, end)));
        verify(payRunRepository, never()).save(any());
    }

    @Test
    void createPayRun_whenNoCompensatedEmployees_deletesDraftAndThrows() {
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser());
        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate end = LocalDate.of(2026, 4, 30);
        when(payRunRepository.existsByPeriodStartAndPeriodEnd(start, end)).thenReturn(false);
        PayRun draft = new PayRun();
        draft.setPeriodStart(start);
        draft.setPeriodEnd(end);
        when(payRunRepository.save(any(PayRun.class))).thenAnswer(inv -> {
            PayRun r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(50L);
            }
            return r;
        });
        when(employeeRepository.findAll()).thenReturn(List.of(employee(1L)));
        when(compensationRepository.findActiveAsOf(1L, end)).thenReturn(Collections.emptyList());
        when(salaryComponentRepository.findByCodeIgnoreCase("ADVANCE_RECOVERY")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> payrollService.createPayRun(new PayRunCreateRequest(start, end)));

        ArgumentCaptor<PayRun> deleteCaptor = ArgumentCaptor.forClass(PayRun.class);
        verify(payRunRepository).delete(deleteCaptor.capture());
        assertEquals(50L, deleteCaptor.getValue().getId());
        verify(payslipRepository, never()).save(any());
    }

    @Test
    void createPayRun_finalizesWhenPayslipCreated() {
        when(currentUserService.requireCurrentUser()).thenReturn(adminUser());
        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate end = LocalDate.of(2026, 4, 30);
        when(payRunRepository.existsByPeriodStartAndPeriodEnd(start, end)).thenReturn(false);
        when(payRunRepository.save(any(PayRun.class))).thenAnswer(inv -> {
            PayRun r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(77L);
            }
            return r;
        });
        Employee emp = employee(9L);
        when(employeeRepository.findAll()).thenReturn(List.of(emp));

        SalaryComponent earning = new SalaryComponent();
        earning.setId(1L);
        earning.setCode("BASIC");
        earning.setName("Basic");
        earning.setKind(SalaryComponentKind.EARNING);
        earning.setActive(true);

        EmployeeCompensationLine line = new EmployeeCompensationLine();
        line.setComponent(earning);
        line.setAmount(new BigDecimal("30000"));
        line.setFrequency(CompensationFrequency.MONTHLY);
        EmployeeCompensation comp = new EmployeeCompensation();
        comp.getLines().add(line);

        when(compensationRepository.findActiveAsOf(9L, end)).thenReturn(List.of(comp));
        when(salaryComponentRepository.findByCodeIgnoreCase("ADVANCE_RECOVERY")).thenReturn(Optional.empty());
        when(payslipRepository.save(any(Payslip.class))).thenAnswer(inv -> {
            Payslip p = inv.getArgument(0);
            p.setId(1001L);
            return p;
        });

        PayRunDto dto = payrollService.createPayRun(new PayRunCreateRequest(start, end));

        assertEquals(77L, dto.id());
        assertEquals(PayRunStatus.FINALIZED, dto.status());
        ArgumentCaptor<Payslip> slipCaptor = ArgumentCaptor.forClass(Payslip.class);
        verify(payslipRepository).save(slipCaptor.capture());
        Payslip saved = slipCaptor.getValue();
        assertEquals(new BigDecimal("30000"), saved.getGrossAmount());
        assertEquals(new BigDecimal("30000"), saved.getNetAmount());
        assertEquals(0, saved.getDeductionAmount().compareTo(BigDecimal.ZERO));
    }

    @Test
    void createPayRun_appliesAdvanceRecoveryWhenComponentExists() {
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser());
        LocalDate start = LocalDate.of(2026, 5, 1);
        LocalDate end = LocalDate.of(2026, 5, 31);
        when(payRunRepository.existsByPeriodStartAndPeriodEnd(start, end)).thenReturn(false);
        when(payRunRepository.save(any(PayRun.class))).thenAnswer(inv -> {
            PayRun r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(88L);
            }
            return r;
        });

        Employee emp = employee(2L);
        when(employeeRepository.findAll()).thenReturn(List.of(emp));

        SalaryComponent earning = new SalaryComponent();
        earning.setId(1L);
        earning.setCode("BASIC");
        earning.setName("Basic");
        earning.setKind(SalaryComponentKind.EARNING);
        earning.setActive(true);

        EmployeeCompensationLine cl = new EmployeeCompensationLine();
        cl.setComponent(earning);
        cl.setAmount(new BigDecimal("50000"));
        cl.setFrequency(CompensationFrequency.MONTHLY);
        EmployeeCompensation comp = new EmployeeCompensation();
        comp.getLines().add(cl);
        when(compensationRepository.findActiveAsOf(2L, end)).thenReturn(List.of(comp));

        SalaryComponent advComp = new SalaryComponent();
        advComp.setId(99L);
        advComp.setCode("ADVANCE_RECOVERY");
        advComp.setName("Advance recovery");
        advComp.setKind(SalaryComponentKind.DEDUCTION);
        when(salaryComponentRepository.findByCodeIgnoreCase("ADVANCE_RECOVERY")).thenReturn(Optional.of(advComp));

        SalaryAdvance advance = new SalaryAdvance();
        advance.setId(5L);
        advance.setOutstandingBalance(new BigDecimal("1000"));
        advance.setRecoveryAmountPerMonth(new BigDecimal("200"));
        advance.setStatus(AdvanceStatus.PAID);
        when(salaryAdvanceRepository.findByEmployeeIdAndStatusAndOutstandingBalanceGreaterThan(
                2L, AdvanceStatus.PAID, BigDecimal.ZERO)).thenReturn(List.of(advance));

        when(payslipRepository.save(any(Payslip.class))).thenAnswer(inv -> {
            Payslip p = inv.getArgument(0);
            p.setId(2002L);
            return p;
        });
        when(salaryAdvanceRepository.save(any(SalaryAdvance.class))).thenAnswer(inv -> inv.getArgument(0));

        payrollService.createPayRun(new PayRunCreateRequest(start, end));

        verify(payslipAdvanceDeductionRepository).save(any());
        verify(salaryAdvanceRepository).save(argThat(a ->
                new BigDecimal("800").compareTo(a.getOutstandingBalance()) == 0));
    }

    @Test
    void listPayRuns_whenNotHr_throws() {
        when(currentUserService.requireCurrentUser()).thenReturn(employeeUser());

        assertThrows(ResponseStatusException.class, () -> payrollService.listPayRuns());
    }

    @Test
    void listPayslipsForRun_whenRunMissing_throws() {
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser());
        when(payRunRepository.existsById(1L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> payrollService.listPayslipsForRun(1L));
    }

    @Test
    void listMyPayslips_whenUserHasNoEmployee_throws() {
        User u = employeeUser();
        u.setEmployee(null);
        when(currentUserService.requireCurrentUser()).thenReturn(u);

        assertThrows(IllegalArgumentException.class, () -> payrollService.listMyPayslips(null));
    }

    @Test
    void getPayslip_whenEmployeeViewsOther_throwsForbidden() {
        User u = employeeUser();
        Employee mine = employee(1L);
        u.setEmployee(mine);
        when(currentUserService.requireCurrentUser()).thenReturn(u);

        Employee other = employee(2L);
        Payslip p = payslipWithEmployee(other);
        when(payslipRepository.findByIdWithLines(10L)).thenReturn(Optional.of(p));

        assertThrows(ResponseStatusException.class, () -> payrollService.getPayslip(10L));
    }

    @Test
    void getPayslip_whenHr_views_succeeds() {
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser());
        Payslip p = payslipWithEmployee(employee(3L));
        p.setId(11L);
        when(payslipRepository.findByIdWithLines(11L)).thenReturn(Optional.of(p));

        PayslipDto dto = payrollService.getPayslip(11L);

        assertEquals(11L, dto.id());
        assertEquals(3L, dto.employeeId());
    }

    @Test
    void payslipPdf_whenIoError_throwsIllegalState() throws Exception {
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser());
        Payslip p = payslipWithEmployee(employee(4L));
        PayRun run = new PayRun();
        run.setId(20L);
        p.setPayRun(run);
        when(payslipRepository.findByIdWithLines(12L)).thenReturn(Optional.of(p));
        when(payslipPdfService.build(eq(p), eq(run))).thenThrow(new IOException("disk full"));

        assertThrows(IllegalStateException.class, () -> payrollService.payslipPdf(12L));
        verify(payslipRepository, never()).save(p);
    }

    @Test
    void payslipPdf_whenOk_updatesPdfTimestamp() throws Exception {
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser());
        Payslip p = payslipWithEmployee(employee(4L));
        PayRun run = new PayRun();
        run.setId(21L);
        p.setPayRun(run);
        when(payslipRepository.findByIdWithLines(13L)).thenReturn(Optional.of(p));
        when(payslipPdfService.build(p, run)).thenReturn(new byte[]{1, 2, 3});

        byte[] out = payrollService.payslipPdf(13L);

        assertArrayEquals(new byte[]{1, 2, 3}, out);
        verify(payslipRepository).save(p);
        assertNotNull(p.getPdfGeneratedAt());
    }

    private static User hrUser() {
        User u = new User();
        u.setId(1L);
        Role r = new Role();
        r.setName("ROLE_HR");
        u.setRoles(Set.of(r));
        return u;
    }

    private static User adminUser() {
        User u = new User();
        u.setId(2L);
        Role r = new Role();
        r.setName("ROLE_ADMIN");
        u.setRoles(Set.of(r));
        return u;
    }

    private static User employeeUser() {
        User u = new User();
        u.setId(3L);
        Role r = new Role();
        r.setName("ROLE_EMPLOYEE");
        u.setRoles(Set.of(r));
        return u;
    }

    private static Employee employee(long id) {
        Employee e = new Employee();
        e.setId(id);
        e.setFirstName("Test");
        e.setLastName("User");
        return e;
    }

    private static Payslip payslipWithEmployee(Employee e) {
        Payslip p = new Payslip();
        p.setId(1L);
        p.setEmployee(e);
        PayRun run = new PayRun();
        run.setId(5L);
        p.setPayRun(run);
        p.setGrossAmount(BigDecimal.TEN);
        p.setDeductionAmount(BigDecimal.ZERO);
        p.setNetAmount(BigDecimal.TEN);
        p.setLines(new ArrayList<>());
        return p;
    }
}
