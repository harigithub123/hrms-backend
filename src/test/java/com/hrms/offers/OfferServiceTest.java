package com.hrms.offers;

import com.hrms.auth.entity.User;
import com.hrms.compensation.CompensationFrequency;
import com.hrms.compensation.CompensationService;
import com.hrms.compensation.entity.EmployeeCompensation;
import com.hrms.compensation.repository.EmployeeCompensationRepository;
import com.hrms.offers.dto.*;
import com.hrms.offers.entity.JobOffer;
import com.hrms.offers.entity.JobOfferEvent;
import com.hrms.offers.entity.OfferCompensation;
import com.hrms.offers.entity.OfferCompensationLine;
import com.hrms.offers.repository.JobOfferEventRepository;
import com.hrms.offers.repository.OfferCompensationRepository;
import com.hrms.offers.repository.JobOfferRepository;
import com.hrms.org.EmployeeRequest;
import com.hrms.org.EmployeeService;
import com.hrms.org.EmployeeDto;
import com.hrms.org.entity.Employee;
import com.hrms.org.repository.DepartmentRepository;
import com.hrms.org.repository.DesignationRepository;
import com.hrms.org.repository.EmployeeRepository;
import com.hrms.payroll.entity.SalaryComponent;
import com.hrms.payroll.repository.SalaryComponentRepository;
import com.hrms.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfferServiceTest {

    @Mock
    private JobOfferRepository jobOfferRepository;
    @Mock
    private JobOfferEventRepository jobOfferEventRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private DesignationRepository designationRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private OfferPdfService offerPdfService;
    @Mock
    private OfferCompensationRepository offerCompensationRepository;
    @Mock
    private SalaryComponentRepository salaryComponentRepository;
    @Mock
    private OfferEmailService offerEmailService;
    @Mock
    private EmployeeService employeeService;
    @Mock
    private EmployeeCompensationRepository employeeCompensationRepository;
    @Mock
    private CompensationService compensationService;

    private OfferService offerService;

    @BeforeEach
    void setUp() {
        offerService = new OfferService(
                offerCompensationRepository,
                jobOfferRepository,
                jobOfferEventRepository,
                departmentRepository,
                designationRepository,
                employeeRepository,
                currentUserService,
                offerPdfService,
                salaryComponentRepository,
                offerEmailService,
                employeeService,
                employeeCompensationRepository,
                compensationService
        );
    }

    // ============ listOffers Tests ============

    @Test
    void should_listOffers_whenUserIsHrAdmin() {
        User hrUser = createHrAdminUser();
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser);
        JobOffer offer1 = createJobOffer(1L, "Candidate1", OfferStatus.DRAFT);
        JobOffer offer2 = createJobOffer(2L, "Candidate2", OfferStatus.SENT);
        when(jobOfferRepository.findAllByOrderByIdDesc()).thenReturn(Arrays.asList(offer2, offer1));

        List<OfferDto> result = offerService.listOffers();

        assertEquals(2, result.size());
        verify(jobOfferRepository, times(1)).findAllByOrderByIdDesc();
    }

    @Test
    void should_throwForbiddenException_whenListOffersAndUserIsNotHrAdmin() {
        User regularUser = createRegularUser();
        when(currentUserService.requireCurrentUser()).thenReturn(regularUser);

        assertThrows(ResponseStatusException.class, () -> offerService.listOffers());
    }

    // ============ listOffersPaged Tests ============

    @Test
    void should_listOffersPaged_whenValidParameters() {
        User hrUser = createHrAdminUser();
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser);
        Pageable pageable = PageRequest.of(0, 10);
        JobOffer offer = createJobOffer(1L, "Candidate1", OfferStatus.DRAFT);
        Page<JobOffer> page = new PageImpl<>(Collections.singletonList(offer), pageable, 1);

        when(jobOfferRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<OfferDto> result = offerService.listOffersPaged("DRAFT", "PERMANENT", null, 1L, 1L, pageable);

        assertEquals(1, result.getContent().size());
        verify(jobOfferRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void should_throwForbiddenException_whenListOffersPagedAndUserIsNotHrAdmin() {
        User regularUser = createRegularUser();
        when(currentUserService.requireCurrentUser()).thenReturn(regularUser);
        Pageable pageable = PageRequest.of(0, 10);

        assertThrows(ResponseStatusException.class,
                () -> offerService.listOffersPaged("DRAFT", null, null, null, null, pageable));
    }

    // ============ getOffer Tests ============

    @Test
    void should_getOffer_whenOfferExists() {
        User hrUser = createHrAdminUser();
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser);
        JobOffer offer = createJobOffer(1L, "Candidate1", OfferStatus.DRAFT);
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(offer));

        OfferDto result = offerService.getOffer(1L);

        assertNotNull(result);
        verify(jobOfferRepository, times(1)).findById(1L);
    }

    @Test
    void should_throwException_whenGetOfferNotFound() {
        User hrUser = createHrAdminUser();
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser);
        when(jobOfferRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> offerService.getOffer(999L));
    }

    // ============ createOffer Tests ============

    @Test
    void should_createOffer_whenNoValidationErrors() {
        User hrUser = createHrAdminUser();
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser);

        JobOffer savedOffer = createJobOffer(1L, "John Doe", OfferStatus.DRAFT);
        when(jobOfferRepository.save(any(JobOffer.class))).thenReturn(savedOffer);

        OfferCreateRequest request = new OfferCreateRequest(
                "John Doe", "john@example.com", "9999999999", "PERMANENT", 1L, 1L,
                LocalDate.now().plusMonths(1), 3, null, List.of()
        );

        OfferDto result = offerService.createOffer(request);

        assertNotNull(result);
        verify(jobOfferRepository, times(1)).save(any(JobOffer.class));
    }

    @Test
    void should_createOffer_withCompensationLines_whenValid() {
        User hrUser = createHrAdminUser();
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser);

        JobOffer savedOffer = createJobOffer(1L, "John Doe", OfferStatus.DRAFT);
        savedOffer.setCurrency("INR");
        when(jobOfferRepository.save(any(JobOffer.class))).thenReturn(savedOffer);

        SalaryComponent component = new SalaryComponent();
        component.setId(1L);
        component.setName("Basic Salary");
        when(salaryComponentRepository.findById(1L)).thenReturn(Optional.of(component));

        OfferCompensation compensation = new OfferCompensation();
        compensation.setCurrency("INR");
        when(offerCompensationRepository.save(any(OfferCompensation.class))).thenReturn(compensation);

        List<OfferCompensationLineRequest> lines = List.of(
                new OfferCompensationLineRequest(1L, new BigDecimal("50000"), CompensationFrequency.MONTHLY)
        );

        OfferCreateRequest request = new OfferCreateRequest(
                "John Doe", "john@example.com", "9999999999", "PERMANENT", 1L, 1L,
                LocalDate.now().plusMonths(1), 3, null, lines
        );

        OfferDto result = offerService.createOffer(request);

        assertNotNull(result);
        verify(jobOfferRepository, times(1)).save(any(JobOffer.class));
        verify(offerCompensationRepository, times(1)).save(any(OfferCompensation.class));
    }

    @Test
    void should_throwException_whenCreateOfferWithFrequencyMismatch() {
        User hrUser = createHrAdminUser();
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser);

        JobOffer savedOffer = createJobOffer(1L, "John Doe", OfferStatus.DRAFT);
        savedOffer.setCurrency("INR");
        when(jobOfferRepository.save(any(JobOffer.class))).thenReturn(savedOffer);

        List<OfferCompensationLineRequest> lines = List.of(
                new OfferCompensationLineRequest(1L, new BigDecimal("50000"), CompensationFrequency.MONTHLY),
                new OfferCompensationLineRequest(1L, new BigDecimal("10000"), CompensationFrequency.YEARLY)
        );

        OfferCreateRequest request = new OfferCreateRequest(
                "John Doe", "john@example.com", "9999999999", "PERMANENT", 1L, 1L,
                LocalDate.now().plusMonths(1), 3, null, lines
        );

        assertThrows(IllegalArgumentException.class, () -> offerService.createOffer(request));
    }

    @Test
    void should_throwException_whenCreateOfferWithInvalidSalaryComponent() {
        User hrUser = createHrAdminUser();
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser);

        JobOffer savedOffer = createJobOffer(1L, "John Doe", OfferStatus.DRAFT);
        savedOffer.setCurrency("INR");
        when(jobOfferRepository.save(any(JobOffer.class))).thenReturn(savedOffer);
        when(salaryComponentRepository.findById(999L)).thenReturn(Optional.empty());

        List<OfferCompensationLineRequest> lines = List.of(
                new OfferCompensationLineRequest(999L, new BigDecimal("50000"), CompensationFrequency.MONTHLY)
        );

        OfferCreateRequest request = new OfferCreateRequest(
                "John Doe", "john@example.com", "9999999999", "PERMANENT", 1L, 1L,
                LocalDate.now().plusMonths(1), 3, null, lines
        );

        assertThrows(IllegalArgumentException.class, () -> offerService.createOffer(request));
    }

    @Test
    void should_throwForbiddenException_whenCreateOfferAndUserIsNotHrAdmin() {
        User regularUser = createRegularUser();
        when(currentUserService.requireCurrentUser()).thenReturn(regularUser);

        OfferCreateRequest request = new OfferCreateRequest(
                "John Doe", "john@example.com", "9999999999", "PERMANENT", 1L, 1L,
                LocalDate.now().plusMonths(1), 3, null, List.of()
        );

        assertThrows(ResponseStatusException.class, () -> offerService.createOffer(request));
    }

    // ============ refreshBody Tests ============

    @Test
    void should_refreshBody_whenOfferExists() {
        User hrUser = createHrAdminUser();
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser);
        JobOffer offer = createJobOffer(1L, "John Doe", OfferStatus.DRAFT);
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(jobOfferRepository.save(offer)).thenReturn(offer);

        OfferDto result = offerService.refreshBody(1L);

        assertNotNull(result);
        verify(jobOfferRepository, times(1)).findById(1L);
        verify(jobOfferRepository, times(1)).save(offer);
    }

    @Test
    void should_throwException_whenRefreshBodyOfferNotFound() {
        User hrUser = createHrAdminUser();
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser);
        when(jobOfferRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> offerService.refreshBody(999L));
    }

    // ============ action Tests ============

    @Test
    void should_throwException_whenActionRequestIsNull() {
        User hrUser = createHrAdminUser();
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser);

        assertThrows(IllegalArgumentException.class, () -> offerService.action(1L, null));
    }

    @Test
    void should_throwException_whenActionIsNull() {
        User hrUser = createHrAdminUser();
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser);

        OfferActionRequest request = new OfferActionRequest(null, null);

        assertThrows(IllegalArgumentException.class, () -> offerService.action(1L, request));
    }

    @Test
    void should_executeAction_whenActionIsSend() {
        User hrUser = createHrAdminUser();
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser);

        JobOffer offer = createJobOffer(1L, "John Doe", OfferStatus.DRAFT);
        offer.setCandidateEmail("john@example.com");
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(jobOfferRepository.findWithDepartmentAndDesignationById(1L)).thenReturn(Optional.of(offer));
        when(offerPdfService.generateOfferLetter(any())).thenReturn(new byte[]{});
        when(jobOfferRepository.save(any())).thenReturn(offer);
        when(offerCompensationRepository.findByOfferId(1L)).thenReturn(Optional.empty());

        OfferActionRequest request = new OfferActionRequest(OfferAction.SEND, null);

        OfferDto result = offerService.action(1L, request);

        assertNotNull(result);
        assertEquals(OfferStatus.SENT, offer.getStatus());
    }

    @Test
    void should_executeAction_whenActionIsResend() {
        User hrUser = createHrAdminUser();
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser);

        JobOffer offer = createJobOffer(1L, "John Doe", OfferStatus.SENT);
        offer.setCandidateEmail("john@example.com");
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(jobOfferRepository.findWithDepartmentAndDesignationById(1L)).thenReturn(Optional.of(offer));
        when(offerPdfService.generateOfferLetter(any())).thenReturn(new byte[]{});
        when(jobOfferRepository.save(any())).thenReturn(offer);
        when(offerCompensationRepository.findByOfferId(1L)).thenReturn(Optional.empty());

        OfferActionRequest request = new OfferActionRequest(OfferAction.RESEND, null);

        OfferDto result = offerService.action(1L, request);

        assertNotNull(result);
    }

    @Test
    void should_executeAction_whenActionIsAccept() {
        User hrUser = createHrAdminUser();
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser);

        JobOffer offer = createJobOffer(1L, "John Doe", OfferStatus.SENT);
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(jobOfferRepository.save(any())).thenReturn(offer);

        OfferActionRequest request = new OfferActionRequest(OfferAction.ACCEPT, null);

        OfferDto result = offerService.action(1L, request);

        assertNotNull(result);
        assertEquals(OfferStatus.ACCEPTED, offer.getStatus());
    }

    @Test
    void should_executeAction_whenActionIsReject() {
        User hrUser = createHrAdminUser();
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser);

        JobOffer offer = createJobOffer(1L, "John Doe", OfferStatus.SENT);
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(jobOfferRepository.save(any())).thenReturn(offer);

        OfferActionRequest request = new OfferActionRequest(OfferAction.REJECT, null);

        OfferDto result = offerService.action(1L, request);

        assertNotNull(result);
        assertEquals(OfferStatus.REJECTED, offer.getStatus());
    }

    // ============ releaseOffer Tests ============

    @Test
    void should_releaseOffer_whenStatusIsDraft() {
        User hrUser = createHrAdminUser();
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser);

        JobOffer offer = createJobOffer(1L, "John Doe", OfferStatus.DRAFT);
        offer.setCandidateEmail("john@example.com");
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(jobOfferRepository.findWithDepartmentAndDesignationById(1L)).thenReturn(Optional.of(offer));
        when(offerPdfService.generateOfferLetter(any())).thenReturn(new byte[]{});
        when(jobOfferRepository.save(any())).thenReturn(offer);
        when(offerCompensationRepository.findByOfferId(1L)).thenReturn(Optional.empty());

        OfferDto result = offerService.releaseOffer(1L, false);

        assertNotNull(result);
        assertEquals(OfferStatus.SENT, offer.getStatus());
        verify(jobOfferEventRepository, atLeastOnce()).save(any(JobOfferEvent.class));
    }

    @Test
    void should_releaseOffer_resend_whenStatusIsSent() {
        User hrUser = createHrAdminUser();
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser);

        JobOffer offer = createJobOffer(1L, "John Doe", OfferStatus.SENT);
        offer.setCandidateEmail("john@example.com");
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(jobOfferRepository.findWithDepartmentAndDesignationById(1L)).thenReturn(Optional.of(offer));
        when(offerPdfService.generateOfferLetter(any())).thenReturn(new byte[]{});
        when(jobOfferRepository.save(any())).thenReturn(offer);
        when(offerCompensationRepository.findByOfferId(1L)).thenReturn(Optional.empty());

        OfferDto result = offerService.releaseOffer(1L, true);

        assertNotNull(result);
    }

    @Test
    void should_throwException_whenReleaseOfferWithoutCandidateEmail() {
        User hrUser = createHrAdminUser();
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser);

        JobOffer offer = createJobOffer(1L, "John Doe", OfferStatus.DRAFT);
        offer.setCandidateEmail(null);
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(offer));

        assertThrows(IllegalArgumentException.class, () -> offerService.releaseOffer(1L, false));
    }

    @Test
    void should_throwException_whenReleaseOfferWithInvalidStatus() {
        User hrUser = createHrAdminUser();
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser);

        JobOffer offer = createJobOffer(1L, "John Doe", OfferStatus.ACCEPTED);
        offer.setCandidateEmail("john@example.com");
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(offer));

        assertThrows(IllegalArgumentException.class, () -> offerService.releaseOffer(1L, false));
    }

    @Test
    void should_recordEmailFailureEvent_whenEmailSendingFails() throws Exception {
        User hrUser = createHrAdminUser();
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser);

        JobOffer offer = createJobOffer(1L, "John Doe", OfferStatus.DRAFT);
        offer.setCandidateEmail("john@example.com");
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(jobOfferRepository.findWithDepartmentAndDesignationById(1L)).thenReturn(Optional.of(offer));
        when(offerPdfService.generateOfferLetter(any())).thenReturn(new byte[]{});
        when(jobOfferRepository.save(any())).thenReturn(offer);
        when(offerCompensationRepository.findByOfferId(1L)).thenReturn(Optional.empty());
        doThrow(new RuntimeException("Email service down")).when(offerEmailService).sendOffer(anyString(), anyString(), any(), anyString());

        OfferDto result = offerService.releaseOffer(1L, false);

        assertNotNull(result);
        verify(jobOfferEventRepository, atLeast(2)).save(any(JobOfferEvent.class));
    }

    // ============ generatePdfDownload Tests ============

    @Test
    void should_generatePdfDownload_whenOfferExists() {
        User hrUser = createHrAdminUser();
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser);

        JobOffer offer = createJobOffer(1L, "John Doe", OfferStatus.DRAFT);
        when(jobOfferRepository.findWithDepartmentAndDesignationById(1L)).thenReturn(Optional.of(offer));
        when(jobOfferRepository.save(any())).thenReturn(offer);
        when(offerPdfService.generateOfferLetter(any())).thenReturn(new byte[]{1, 2, 3});
        when(offerCompensationRepository.findByOfferId(1L)).thenReturn(Optional.empty());

        OfferService.OfferPdfDownload result = offerService.generatePdfDownload(1L);

        assertNotNull(result);
        assertNotNull(result.bytes());
        assertNotNull(result.filename());
        assertTrue(result.filename().endsWith(".pdf"));
        assertTrue(result.filename().contains("Offer_Letter"));
    }

    @Test
    void should_generatePdfDownloadWithCompensationLines_whenCompensationExists() {
        User hrUser = createHrAdminUser();
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser);

        JobOffer offer = createJobOffer(1L, "John Doe", OfferStatus.DRAFT);
        offer.setCurrency("INR");

        OfferCompensation compensation = new OfferCompensation();
        compensation.setCurrency("INR");

        SalaryComponent component = new SalaryComponent();
        component.setId(1L);
        component.setName("Basic Salary");

        OfferCompensationLine line = new OfferCompensationLine();
        line.setComponent(component);
        line.setAmount(new BigDecimal("50000"));
        line.setFrequency(CompensationFrequency.MONTHLY);

        compensation.getOfferCompensationLine().add(line);

        when(jobOfferRepository.findWithDepartmentAndDesignationById(1L)).thenReturn(Optional.of(offer));
        when(jobOfferRepository.save(any())).thenReturn(offer);
        when(offerCompensationRepository.findByOfferId(1L)).thenReturn(Optional.of(compensation));
        when(offerPdfService.generateOfferLetter(any())).thenReturn(new byte[]{1, 2, 3});

        OfferService.OfferPdfDownload result = offerService.generatePdfDownload(1L);

        assertNotNull(result);
        assertNotNull(result.bytes());
    }

    @Test
    void should_throwException_whenGeneratePdfDownloadOfferNotFound() {
        User hrUser = createHrAdminUser();
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser);
        when(jobOfferRepository.findWithDepartmentAndDesignationById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> offerService.generatePdfDownload(999L));
    }

    // ============ acceptOffer Tests ============

    @Test
    void should_acceptOffer_whenStatusIsSent() {
        User hrUser = createHrAdminUser();
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser);

        JobOffer offer = createJobOffer(1L, "John Doe", OfferStatus.SENT);
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(jobOfferRepository.save(any())).thenReturn(offer);

        OfferDto result = offerService.acceptOffer(1L);

        assertNotNull(result);
        assertEquals(OfferStatus.ACCEPTED, offer.getStatus());
        verify(jobOfferEventRepository, times(1)).save(any(JobOfferEvent.class));
    }

    @Test
    void should_throwException_whenAcceptOfferAlreadyJoined() {
        User hrUser = createHrAdminUser();
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser);

        JobOffer offer = createJobOffer(1L, "John Doe", OfferStatus.JOINED);
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(offer));

        assertThrows(IllegalArgumentException.class, () -> offerService.acceptOffer(1L));
    }

    // ============ rejectOffer Tests ============

    @Test
    void should_rejectOffer_whenStatusIsSent() {
        User hrUser = createHrAdminUser();
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser);

        JobOffer offer = createJobOffer(1L, "John Doe", OfferStatus.SENT);
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(jobOfferRepository.save(any())).thenReturn(offer);

        OfferDto result = offerService.rejectOffer(1L);

        assertNotNull(result);
        assertEquals(OfferStatus.REJECTED, offer.getStatus());
        verify(jobOfferEventRepository, times(1)).save(any(JobOfferEvent.class));
    }

    @Test
    void should_throwException_whenRejectOfferAlreadyJoined() {
        User hrUser = createHrAdminUser();
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser);

        JobOffer offer = createJobOffer(1L, "John Doe", OfferStatus.JOINED);
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(offer));

        assertThrows(IllegalArgumentException.class, () -> offerService.rejectOffer(1L));
    }

    // ============ markJoined Tests ============

    @Test
    void should_markJoined_whenActionIsJoined() {
        User hrUser = createHrAdminUser();
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser);

        JobOffer offer = createJobOffer(1L, "John Doe", OfferStatus.ACCEPTED);
        offer.setCandidateEmail("john@example.com");
        offer.setCandidateMobile("9999999999");

        Employee mockEmployee = new Employee();
        mockEmployee.setId(1L);

        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(employeeRepository.getReferenceById(any())).thenReturn(mockEmployee);
        EmployeeDto empDto = new EmployeeDto(1L, "EMP001", "John", "Doe", "john@example.com", "9999999999", 1L, "IT", 1L, "Developer", null, null, LocalDate.now());
        when(employeeService.create(any(EmployeeRequest.class))).thenReturn(empDto);

        OfferCompensation compensation = new OfferCompensation();
        compensation.setCurrency("INR");
        SalaryComponent component = new SalaryComponent();
        component.setId(1L);
        component.setName("Basic Salary");
        OfferCompensationLine line = new OfferCompensationLine();
        line.setComponent(component);
        line.setAmount(new BigDecimal("50000"));
        line.setFrequency(CompensationFrequency.MONTHLY);
        compensation.getOfferCompensationLine().add(line);

        when(offerCompensationRepository.findByOfferId(1L)).thenReturn(Optional.of(compensation));

        ArgumentCaptor<EmployeeCompensation> captor = ArgumentCaptor.forClass(EmployeeCompensation.class);
        when(employeeCompensationRepository.save(captor.capture())).thenAnswer(i -> i.getArgument(0));
        when(jobOfferRepository.save(any())).thenReturn(offer);

        MarkJoinedRequest request = new MarkJoinedRequest(null, LocalDate.now(), true);

        OfferDto result = offerService.markJoined(1L, request);

        assertNotNull(result);
        assertEquals(OfferStatus.JOINED, offer.getStatus());
        verify(jobOfferEventRepository, times(1)).save(any(JobOfferEvent.class));
        verify(employeeService, times(1)).create(any(EmployeeRequest.class));
        verify(compensationService, times(1)).syncToSalaryStructure(any());

        // Verify annualCtc calculation
        EmployeeCompensation savedCompensation = captor.getValue();
        assertNotNull(savedCompensation.getAnnualCtc());
        assertEquals(new BigDecimal("600000.00"), savedCompensation.getAnnualCtc()); // 50000 * 12 months
    }

    @Test
    void should_throwException_whenMarkJoinedWithoutActualJoiningDate() {
        User hrUser = createHrAdminUser();
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser);

        JobOffer offer = createJobOffer(1L, "John Doe", OfferStatus.ACCEPTED);
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(offer));

        MarkJoinedRequest request = new MarkJoinedRequest(null, null, true);

        assertThrows(IllegalArgumentException.class, () -> offerService.markJoined(1L, request));
    }

    @Test
    void should_throwException_whenMarkJoinedWithoutAcceptedStatus() {
        User hrUser = createHrAdminUser();
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser);

        JobOffer offer = createJobOffer(1L, "John Doe", OfferStatus.SENT);
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(offer));

        MarkJoinedRequest request = new MarkJoinedRequest(null, LocalDate.now(), true);

        assertThrows(IllegalArgumentException.class, () -> offerService.markJoined(1L, request));
    }

    @Test
    void should_throwException_whenMarkJoinedEmployeeAlreadyCreated() {
        User hrUser = createHrAdminUser();
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser);

        JobOffer offer = createJobOffer(1L, "John Doe", OfferStatus.ACCEPTED);
        Employee mockEmployee = new Employee();
        mockEmployee.setId(1L);
        offer.setEmployee(mockEmployee);
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(offer));

        MarkJoinedRequest request = new MarkJoinedRequest(null, LocalDate.now(), true);

        assertThrows(IllegalArgumentException.class, () -> offerService.markJoined(1L, request));
    }

    @Test
    void should_throwException_whenMarkJoinedWithoutCandidateEmail() {
        User hrUser = createHrAdminUser();
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser);

        JobOffer offer = createJobOffer(1L, "John Doe", OfferStatus.ACCEPTED);
        offer.setCandidateEmail(null);
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(offer));

        MarkJoinedRequest request = new MarkJoinedRequest(null, LocalDate.now(), true);

        assertThrows(IllegalArgumentException.class, () -> offerService.markJoined(1L, request));
    }

    @Test
    void should_throwException_whenMarkJoinedWithoutCandidateMobile() {
        User hrUser = createHrAdminUser();
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser);

        JobOffer offer = createJobOffer(1L, "John Doe", OfferStatus.ACCEPTED);
        offer.setCandidateEmail("john@example.com");
        offer.setCandidateMobile(null);
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(offer));

        MarkJoinedRequest request = new MarkJoinedRequest(null, LocalDate.now(), true);

        assertThrows(IllegalArgumentException.class, () -> offerService.markJoined(1L, request));
    }

    @Test
    void should_throwException_whenMarkJoinedWithoutOfferCompensation() {
        User hrUser = createHrAdminUser();
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser);

        JobOffer offer = createJobOffer(1L, "John Doe", OfferStatus.ACCEPTED);
        offer.setCandidateEmail("john@example.com");
        offer.setCandidateMobile("9999999999");

        Employee mockEmployee = new Employee();
        mockEmployee.setId(1L);

        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(employeeRepository.getReferenceById(any())).thenReturn(mockEmployee);
        EmployeeDto empDto2 = new EmployeeDto(1L, "EMP001", "John", "Doe", "john@example.com", "9999999999", 1L, "IT", 1L, "Developer", null, null, LocalDate.now());
        when(employeeService.create(any(EmployeeRequest.class))).thenReturn(empDto2);
        when(offerCompensationRepository.findByOfferId(1L)).thenReturn(Optional.empty());

        MarkJoinedRequest request = new MarkJoinedRequest(null, LocalDate.now(), true);

        assertThrows(IllegalArgumentException.class, () -> offerService.markJoined(1L, request));
    }

    @Test
    void should_throwException_whenMarkJoinedWithoutCompensationLines() {
        User hrUser = createHrAdminUser();
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser);

        JobOffer offer = createJobOffer(1L, "John Doe", OfferStatus.ACCEPTED);
        offer.setCandidateEmail("john@example.com");
        offer.setCandidateMobile("9999999999");

        Employee mockEmployee = new Employee();
        mockEmployee.setId(1L);

        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(employeeRepository.getReferenceById(any())).thenReturn(mockEmployee);
        EmployeeDto empDto3 = new EmployeeDto(1L, "EMP001", "John", "Doe", "john@example.com", "9999999999", 1L, "IT", 1L, "Developer", null, null, LocalDate.now());
        when(employeeService.create(any(EmployeeRequest.class))).thenReturn(empDto3);

        OfferCompensation compensation = new OfferCompensation();
        compensation.setCurrency("INR");

        when(offerCompensationRepository.findByOfferId(1L)).thenReturn(Optional.of(compensation));

        MarkJoinedRequest request = new MarkJoinedRequest(null, LocalDate.now(), true);

        assertThrows(IllegalArgumentException.class, () -> offerService.markJoined(1L, request));
    }

    @Test
    void should_setCompensationEffectiveFrom_whenProvidedInRequest() {
        User hrUser = createHrAdminUser();
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser);

        JobOffer offer = createJobOffer(1L, "John Doe", OfferStatus.ACCEPTED);
        offer.setCandidateEmail("john@example.com");
        offer.setCandidateMobile("9999999999");

        Employee mockEmployee = new Employee();
        mockEmployee.setId(1L);

        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(employeeRepository.getReferenceById(any())).thenReturn(mockEmployee);
        EmployeeDto empDto4 = new EmployeeDto(1L, "EMP001", "John", "Doe", "john@example.com", "9999999999", 1L, "IT", 1L, "Developer", null, null, LocalDate.now());
        when(employeeService.create(any(EmployeeRequest.class))).thenReturn(empDto4);

        OfferCompensation compensation = new OfferCompensation();
        compensation.setCurrency("INR");
        SalaryComponent component = new SalaryComponent();
        component.setId(1L);
        component.setName("Basic Salary");
        OfferCompensationLine line = new OfferCompensationLine();
        line.setComponent(component);
        line.setAmount(new BigDecimal("50000"));
        line.setFrequency(CompensationFrequency.MONTHLY);
        compensation.getOfferCompensationLine().add(line);

        when(offerCompensationRepository.findByOfferId(1L)).thenReturn(Optional.of(compensation));

        ArgumentCaptor<EmployeeCompensation> captor = ArgumentCaptor.forClass(EmployeeCompensation.class);
        when(employeeCompensationRepository.save(captor.capture())).thenAnswer(i -> i.getArgument(0));
        when(jobOfferRepository.save(any())).thenReturn(offer);

        LocalDate effectiveFrom = LocalDate.now().plusDays(5);
        MarkJoinedRequest request = new MarkJoinedRequest(effectiveFrom, LocalDate.now(), true);

        OfferDto result = offerService.markJoined(1L, request);

        assertNotNull(result);
        EmployeeCompensation savedCompensation = captor.getValue();
        assertEquals(effectiveFrom, savedCompensation.getEffectiveFrom());
    }

    // ============ exportOffersCsv Tests ============

    @Test
    void should_exportOffersCsv_whenOffersExist() {
        User hrUser = createHrAdminUser();
        when(currentUserService.requireCurrentUser()).thenReturn(hrUser);

        JobOffer offer = createJobOffer(1L, "John Doe", OfferStatus.DRAFT);
        when(jobOfferRepository.findAll(any(Specification.class))).thenReturn(Collections.singletonList(offer));

        OfferService.CsvExport result = offerService.exportOffersCsv("DRAFT", null, null, null, null);

        assertNotNull(result);
        assertNotNull(result.filename());
        assertNotNull(result.csv());
        assertTrue(result.filename().endsWith(".csv"));
        assertTrue(result.filename().startsWith("offers_"));
    }

    // ============ buildOfferPdfFilename Tests ============

    @Test
    void should_buildOfferPdfFilename_withValidCandidateName() {
        String filename = OfferService.buildOfferPdfFilename("John Doe");

        assertNotNull(filename);
        assertTrue(filename.contains("JohnDoe"));
        assertTrue(filename.contains("Offer_Letter"));
        assertTrue(filename.endsWith(".pdf"));
    }

    @Test
    void should_buildOfferPdfFilename_withSpecialCharacters() {
        String filename = OfferService.buildOfferPdfFilename("John@Doe!");

        assertNotNull(filename);
        assertTrue(filename.contains("JohnDoe"));
        assertTrue(filename.endsWith(".pdf"));
    }

    @Test
    void should_buildOfferPdfFilename_withNullCandidateName() {
        String filename = OfferService.buildOfferPdfFilename(null);

        assertNotNull(filename);
        assertTrue(filename.contains("Candidate"));
        assertTrue(filename.endsWith(".pdf"));
    }

    @Test
    void should_buildOfferPdfFilename_withLongCandidateName() {
        String longName = "A".repeat(60);

        String filename = OfferService.buildOfferPdfFilename(longName);

        assertNotNull(filename);
        assertTrue(filename.endsWith(".pdf"));
    }

    // ============ Helper Methods ============

    private User createHrAdminUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");

        com.hrms.auth.entity.Role hrRole = new com.hrms.auth.entity.Role();
        hrRole.setId(1L);
        hrRole.setName("ROLE_HR");

        user.setRoles(Collections.singleton(hrRole));
        return user;
    }

    private User createRegularUser() {
        User user = new User();
        user.setId(2L);
        user.setUsername("employee");

        com.hrms.auth.entity.Role employeeRole = new com.hrms.auth.entity.Role();
        employeeRole.setId(3L);
        employeeRole.setName("ROLE_EMPLOYEE");

        user.setRoles(Collections.singleton(employeeRole));
        return user;
    }

    private JobOffer createJobOffer(Long id, String candidateName, OfferStatus status) {
        JobOffer offer = new JobOffer();
        offer.setId(id);
        offer.setCandidateName(candidateName);
        offer.setCandidateEmail("candidate@example.com");
        offer.setCandidateMobile("9999999999");
        offer.setStatus(status);
        offer.setEmployeeType("PERMANENT");
        offer.setJoiningDate(LocalDate.now().plusMonths(1));
        offer.setProbationPeriodMonths(3);
        return offer;
    }
}

