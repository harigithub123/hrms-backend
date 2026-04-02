# PayrollService Refactoring: Remove SalaryStructure Dependency

## Date: April 2, 2026

## Overview
Refactored `PayrollService` to remove dependency on `EmployeeSalaryStructure` and use `EmployeeCompensation` instead for pay run generation. This consolidates compensation data management into a single source of truth.

## Changes Made

### 1. PayrollService.java
**Location:** `src/main/java/com/hrms/payroll/PayrollService.java`

#### Added Imports:
- `com.hrms.compensation.CompensationFrequency`
- `com.hrms.compensation.entity.EmployeeCompensation`
- `com.hrms.compensation.entity.EmployeeCompensationLine`
- `com.hrms.compensation.repository.EmployeeCompensationRepository`

#### Removed Imports:
- References to `EmployeeSalaryStructure` and related classes

#### Constructor Changes:
- **Removed:** `EmployeeSalaryStructureRepository structureRepository`
- **Added:** `EmployeeCompensationRepository compensationRepository`

#### Removed Methods:
- `saveStructure(SalaryStructureRequest req)` - Creating salary structures is deprecated
- `getLatestStructureForEmployee(Long employeeId, LocalDate asOf)` - No longer needed

#### Modified Methods:
- `createPayRun(PayRunCreateRequest req)`:
  - Now fetches active compensation using `compensationRepository.findActiveAsOf()`
  - Iterates through `EmployeeCompensationLine` instead of `EmployeeSalaryStructureLine`
  - Added frequency conversion logic via new helper method `convertToMonthlyAmount()`
  - Error message updated: "No employees with compensation found" instead of "No employees with salary structure found"

#### New Methods:
- `convertToMonthlyAmount(EmployeeCompensationLine line)`:
  - Converts compensation amounts based on frequency (MONTHLY, YEARLY, ONE_TIME)
  - MONTHLY: returns amount as-is
  - YEARLY: divides by 12
  - ONE_TIME: returns zero (not included in regular payroll)

### 2. CompensationService.java
**Location:** `src/main/java/com/hrms/compensation/CompensationService.java`

#### Removed Imports:
- `com.hrms.payroll.PayrollService`
- `com.hrms.payroll.dto.SalaryStructureLineRequest`
- `com.hrms.payroll.dto.SalaryStructureRequest`

#### Constructor Changes:
- **Removed:** `PayrollService payrollService` dependency

#### Removed Methods:
- `syncToSalaryStructure(Long compensationId)` - No longer syncing to deprecated salary structure table

### 3. CompensationController.java
**Location:** `src/main/java/com/hrms/compensation/CompensationController.java`

#### Removed Imports:
- `com.hrms.payroll.dto.SalaryStructureDto`

#### Removed Endpoints:
- `POST /api/compensation/{id}/sync-structure` - Removed sync endpoint

### 4. OfferService.java
**Location:** `src/main/java/com/hrms/offers/OfferService.java`

#### Removed Imports:
- `com.hrms.compensation.CompensationService`

#### Constructor Changes:
- **Removed:** `CompensationService compensationService` dependency

#### Modified Methods:
- `markJoined(Long id, MarkJoinedRequest body)`:
  - Removed call to `compensationService.syncToSalaryStructure()` after creating employee compensation
  - Employee compensation is now the sole source; no syncing needed

### 5. PayrollStructureController.java (DELETED)
**Location:** `src/main/java/com/hrms/payroll/PayrollStructureController.java`

#### Reason for Deletion:
- Controller provided REST endpoints for managing salary structures
- Both endpoints (`POST /api/payroll/structures` and `GET /api/payroll/structures/employee/{employeeId}`) called removed methods
- Salary structure management is now deprecated in favor of compensation management

### 6. OfferServiceTest.java
**Location:** `src/test/java/com/hrms/offers/OfferServiceTest.java`

#### Removed Mocks:
- `@Mock CompensationService compensationService`

#### Test Updates:
- Removed `compensationService` from `OfferService` constructor in `setUp()`
- Removed verification: `verify(compensationService, times(1)).syncToSalaryStructure(any())`
- Removed unnecessary `jobOfferRepository.save(any())` stubs from PDF generation tests

## Benefits

### 1. Single Source of Truth
- Compensation data is now managed exclusively through `EmployeeCompensation`
- Eliminates duplicate/inconsistent data between salary structures and compensation

### 2. Frequency Support
- Payroll now properly handles MONTHLY, YEARLY, and ONE_TIME compensation frequencies
- Automatically converts yearly amounts to monthly for payslips

### 3. Simplified Architecture
- Removed circular dependency between `CompensationService` and `PayrollService`
- Cleaner separation of concerns

### 4. Reduced Maintenance
- No need to sync between two systems
- Fewer entities and endpoints to maintain

## Migration Notes

### For API Consumers:
1. **Removed Endpoints:**
   - `POST /api/payroll/structures` - Use `POST /api/compensation` instead
   - `GET /api/payroll/structures/employee/{employeeId}` - Use `GET /api/compensation/employee/{employeeId}` instead
   - `POST /api/compensation/{id}/sync-structure` - No longer needed

### For Database:
- `employee_salary_structure` and `employee_salary_structure_lines` tables are now unused by payroll
- Consider marking as deprecated or planning migration to remove these tables entirely
- All payroll runs will now source data from `employee_compensation` and `employee_compensation_lines`

### For Future Development:
- Any new payroll features should use `EmployeeCompensation` API
- `EmployeeSalaryStructure` entities remain in codebase but are no longer actively used
- Consider complete removal in future major version

## Testing
- All 44 tests pass successfully
- `OfferServiceTest` (43 tests) validates offer and compensation creation flow
- `BcryptHashVerifyTest` (1 test) validates password hashing

## Backward Compatibility
- **Breaking Change:** REST API endpoints for salary structure management removed
- **Data Compatibility:** Existing salary structure data remains in database but is not used by payroll
- **Migration Path:** Organizations must create `EmployeeCompensation` records for all employees before running payroll

## Next Steps (Recommended)
1. Update API documentation to reflect removed endpoints
2. Create data migration script to convert existing `EmployeeSalaryStructure` to `EmployeeCompensation`
3. Add deprecation warnings to `EmployeeSalaryStructure` entity
4. Plan removal of salary structure tables in next major version
5. Update user documentation and training materials

