package com.hrms.payroll;

import com.hrms.org.entity.Department;
import com.hrms.org.entity.Designation;
import com.hrms.org.entity.Employee;
import com.hrms.payroll.entity.EmployeePayrollBank;
import com.hrms.payroll.entity.PayRun;
import com.hrms.payroll.entity.Payslip;
import com.hrms.payroll.entity.PayslipLine;
import com.hrms.payroll.repository.EmployeePayrollBankRepository;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class PayslipPdfService {

    private final EmployeePayrollBankRepository employeePayrollBankRepository;

    public PayslipPdfService(EmployeePayrollBankRepository employeePayrollBankRepository) {
        this.employeePayrollBankRepository = employeePayrollBankRepository;
    }

    private static final Font TITLE = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);

    /** Shown when a labeled field has no data on {@link Payslip} (or nested employee) / pay run. */
    private static final String PLACEHOLDER = "--";

    private static final DateTimeFormatter PERIOD_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    public byte[] build(Payslip payslip, PayRun run) throws IOException {
        Objects.requireNonNull(payslip, "payslip");
        Objects.requireNonNull(run, "run");
        try {
            Document document = new Document(PageSize.A4, 20, 20, 20, 20);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);
            document.open();

            Font normalFont = new Font(Font.FontFamily.HELVETICA, 10);
            Font boldFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);

            // Not on Payslip — placeholder per requirement
            Paragraph company = new Paragraph(sanitize(PLACEHOLDER), TITLE);
            company.setAlignment(Element.ALIGN_CENTER);
            document.add(company);

            Paragraph address = new Paragraph(sanitize(PLACEHOLDER), normalFont);
            address.setAlignment(Element.ALIGN_CENTER);
            document.add(address);

            String periodText = "Payslip for the period of "
                    + PERIOD_FMT.format(run.getPeriodStart())
                    + " to "
                    + PERIOD_FMT.format(run.getPeriodEnd());
            Paragraph payslipTitle = new Paragraph(sanitize(periodText), boldFont);
            payslipTitle.setAlignment(Element.ALIGN_CENTER);
            payslipTitle.setSpacingAfter(10);
            document.add(payslipTitle);

            Employee emp = payslip.getEmployee();
            String empIdDisplay = resolveEmployeeIdDisplay(emp);
            String empName = sanitize(textOrPlaceholder((emp.getFirstName() + " " + emp.getLastName()).trim()));
            String dept = textOrPlaceholder(departmentName(emp));
            String desig = textOrPlaceholder(designationName(emp));
            if (!PLACEHOLDER.equals(dept)) {
                dept = sanitize(dept);
            }
            if (!PLACEHOLDER.equals(desig)) {
                desig = sanitize(desig);
            }

            PdfPTable empTable = new PdfPTable(4);
            empTable.setWidthPercentage(100);
            empTable.setSpacingAfter(10);
            empTable.setWidths(new float[]{2, 3, 2, 3});

            addEmployeeDetailCell(empTable, "Employee Id", normalFont);
            addEmployeeDetailCell(empTable, sanitize(empIdDisplay), normalFont);
            addEmployeeDetailCell(empTable, "Name", normalFont);
            addEmployeeDetailCell(empTable, empName, normalFont);

            addEmployeeDetailCell(empTable, "Department", normalFont);
            addEmployeeDetailCell(empTable, dept, normalFont);
            addEmployeeDetailCell(empTable, "Designation", normalFont);
            addEmployeeDetailCell(empTable, desig, normalFont);

            addEmployeeDetailCell(empTable, "Days Worked", normalFont);
            addEmployeeDetailCell(empTable, PLACEHOLDER, normalFont);
            addEmployeeDetailCell(empTable, "Bank Name, Branch", normalFont);
            addEmployeeDetailCell(empTable, sanitize(textOrPlaceholder(resolveBankBranchDisplay(emp, run))), normalFont);

            addEmployeeDetailCell(empTable, "Bank Acct", normalFont);
            addEmployeeDetailCell(empTable, sanitize(resolveBankAccountDisplay(emp, run)), normalFont);
            addEmployeeDetailCell(empTable, "Overtime Hours", normalFont);
            addEmployeeDetailCell(empTable, PLACEHOLDER, normalFont);

            document.add(empTable);

            List<PayslipLine> lines = new ArrayList<>(payslip.getLines());
            lines.sort(Comparator
                    .comparing(PayslipLine::getKind)
                    .thenComparing(PayslipLine::getComponentCode, Comparator.nullsLast(String::compareTo)));

            List<PayslipLine> earnings = lines.stream()
                    .filter(l -> l.getKind() == SalaryComponentKind.EARNING)
                    .toList();
            List<PayslipLine> deductions = lines.stream()
                    .filter(l -> l.getKind() == SalaryComponentKind.DEDUCTION)
                    .toList();

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3, 2, 3, 2});

            addHeader(table, "Earnings", boldFont);
            addHeader(table, "Amount", boldFont);
            addHeader(table, "Deductions", boldFont);
            addHeader(table, "Amount", boldFont);

            int maxRows = Math.max(earnings.size(), deductions.size());
            for (int i = 0; i < maxRows; i++) {
                String eName = "";
                String eAmt = "";
                if (i < earnings.size()) {
                    PayslipLine el = earnings.get(i);
                    eName = sanitize(el.getComponentName());
                    eAmt = sanitize(formatAmount(el.getAmount()));
                }
                String dName = "";
                String dAmt = "";
                if (i < deductions.size()) {
                    PayslipLine dl = deductions.get(i);
                    dName = sanitize(dl.getComponentName());
                    dAmt = sanitize(formatAmount(dl.getAmount()));
                }
                addRow(table, eName, eAmt, dName, dAmt);
            }

            PdfPCell totalEarnLabel = new PdfPCell(new Phrase("Total Earnings (Rounded)", boldFont));
            totalEarnLabel.setColspan(1);
            totalEarnLabel.setBorder(Rectangle.TOP);
            table.addCell(totalEarnLabel);

            PdfPCell totalEarnValue = new PdfPCell(new Phrase(sanitize(formatAmount(payslip.getGrossAmount())), boldFont));
            totalEarnValue.setBorder(Rectangle.TOP);
            totalEarnValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(totalEarnValue);

            PdfPCell totalDedLabel = new PdfPCell(new Phrase("Total Deductions (Rounded)", boldFont));
            totalDedLabel.setBorder(Rectangle.TOP);
            table.addCell(totalDedLabel);

            PdfPCell totalDedValue = new PdfPCell(new Phrase(sanitize(formatAmount(payslip.getDeductionAmount())), boldFont));
            totalDedValue.setBorder(Rectangle.TOP);
            totalDedValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(totalDedValue);

            PdfPCell netLabel = new PdfPCell(new Phrase("Net Pay (Rounded)", boldFont));
            netLabel.setColspan(3);
            netLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(netLabel);

            PdfPCell netValue = new PdfPCell(new Phrase(sanitize(formatAmount(payslip.getNetAmount())), boldFont));
            netValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(netValue);

            document.add(table);

            PdfPTable signTable = new PdfPTable(2);
            signTable.setWidthPercentage(100);
            signTable.setSpacingBefore(30);

            PdfPCell empSign = new PdfPCell(new Phrase("Employer's Signature", normalFont));
            empSign.setBorder(Rectangle.TOP);
            empSign.setHorizontalAlignment(Element.ALIGN_LEFT);
            empSign.setPaddingTop(10);

            PdfPCell employeeSign = new PdfPCell(new Phrase("Employee's Signature", normalFont));
            employeeSign.setBorder(Rectangle.TOP);
            employeeSign.setHorizontalAlignment(Element.ALIGN_RIGHT);
            employeeSign.setPaddingTop(10);

            signTable.addCell(empSign);
            signTable.addCell(employeeSign);

            document.add(signTable);

            document.close();
            return baos.toByteArray();
        } catch (DocumentException e) {
            throw new IOException("Failed to build payslip PDF: " + e.getMessage(), e);
        }
    }

    private String resolveBankBranchDisplay(Employee emp, PayRun run) {
        Optional<EmployeePayrollBank> opt = employeePayrollBankRepository.findByEmployee_Id(emp.getId());
        if (opt.isEmpty()) {
            return null;
        }
        EmployeePayrollBank b = opt.get();
        if (b.getEffectiveFrom() != null && run.getPeriodEnd().isBefore(b.getEffectiveFrom())) {
            return null;
        }
        String bn = b.getBankName();
        if (b.getBranch() != null && !b.getBranch().isBlank()) {
            return bn + ", " + b.getBranch();
        }
        return bn;
    }

    private String resolveBankAccountDisplay(Employee emp, PayRun run) {
        Optional<EmployeePayrollBank> opt = employeePayrollBankRepository.findByEmployee_Id(emp.getId());
        if (opt.isEmpty()) {
            return PLACEHOLDER;
        }
        EmployeePayrollBank b = opt.get();
        if (b.getEffectiveFrom() != null && run.getPeriodEnd().isBefore(b.getEffectiveFrom())) {
            return PLACEHOLDER;
        }
        return maskAccountLast4(b.getAccountNumber());
    }

    private static String maskAccountLast4(String raw) {
        if (raw == null || raw.isBlank()) {
            return PLACEHOLDER;
        }
        String d = raw.replaceAll("\\s", "");
        if (d.length() <= 4) {
            return "****";
        }
        return "****" + d.substring(d.length() - 4);
    }

    private static String resolveEmployeeIdDisplay(Employee emp) {
        if (emp.getEmployeeCode() != null && !emp.getEmployeeCode().isBlank()) {
            return emp.getEmployeeCode();
        }
        return String.valueOf(emp.getId());
    }

    private static String departmentName(Employee emp) {
        Department d = emp.getDepartment();
        if (d == null || d.getName() == null || d.getName().isBlank()) {
            return null;
        }
        return d.getName();
    }

    private static String designationName(Employee emp) {
        Designation d = emp.getDesignation();
        if (d == null || d.getName() == null || d.getName().isBlank()) {
            return null;
        }
        return d.getName();
    }

    private static String textOrPlaceholder(String s) {
        if (s == null || s.isBlank()) {
            return PLACEHOLDER;
        }
        return s;
    }

    private static String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return PLACEHOLDER;
        }
        return amount.toPlainString();
    }

    /** Bordered cells for the employee / payroll-info block above earnings & deductions. */
    private void addEmployeeDetailCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderWidth(0.5f);
        table.addCell(cell);
    }

    private void addHeader(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        table.addCell(cell);
    }

    private void addRow(PdfPTable table, String e1, String e2, String d1, String d2) {
        table.addCell(new PdfPCell(new Phrase(e1)));
        PdfPCell amt1 = new PdfPCell(new Phrase(e2));
        amt1.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(amt1);

        table.addCell(new PdfPCell(new Phrase(d1)));
        PdfPCell amt2 = new PdfPCell(new Phrase(d2));
        amt2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(amt2);
    }

    private static String sanitize(String s) {
        if (s == null) {
            return "";
        }
        return s.replace('\u2013', '-').chars()
                .filter(c -> c >= 32 && c < 127)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }
}
