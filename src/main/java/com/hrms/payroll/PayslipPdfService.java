package com.hrms.payroll;

import com.hrms.payroll.entity.PayRun;
import com.hrms.payroll.entity.Payslip;
import com.hrms.payroll.entity.PayslipLine;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class PayslipPdfService {

    private static final Font TITLE = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
    private static final Font HEADER = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);
    private static final Font NORMAL = new Font(Font.FontFamily.HELVETICA, 10);
    private static final Font NORMAL_BOLD = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);


        public byte[] generateSalarySlip() throws Exception {

            Document document = new Document(PageSize.A4, 20, 20, 20, 20);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);;
            document.open();

            Font titleFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 10);
            Font boldFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);

            // ================= HEADER =================
            Paragraph company = new Paragraph("Lenvica Computer Solutions Pvt Ltd", titleFont);
            company.setAlignment(Element.ALIGN_CENTER);
            document.add(company);

            Paragraph address = new Paragraph("Novel Business Center, #10, BTM 1st Stage", normalFont);
            address.setAlignment(Element.ALIGN_CENTER);
            document.add(address);

            Paragraph payslipTitle = new Paragraph("Payslip for the period of May 2018", boldFont);
            payslipTitle.setAlignment(Element.ALIGN_CENTER);
            payslipTitle.setSpacingAfter(10);
            document.add(payslipTitle);

            // ================= EMPLOYEE DETAILS =================
            PdfPTable empTable = new PdfPTable(4);
            empTable.setWidthPercentage(100);
            empTable.setSpacingAfter(10);
            empTable.setWidths(new float[]{2, 3, 2, 3});

            addCell(empTable, "Employee Id", normalFont);
            addCell(empTable, "11001", normalFont);
            addCell(empTable, "Name", normalFont);
            addCell(empTable, "Alex Jacob", normalFont);

            addCell(empTable, "Department", normalFont);
            addCell(empTable, "Information Technology", normalFont);
            addCell(empTable, "Designation", normalFont);
            addCell(empTable, "Sr. Software Engineer", normalFont);

            addCell(empTable, "Days Worked", normalFont);
            addCell(empTable, "22.0", normalFont);
            addCell(empTable, "Bank Name, Branch", normalFont);
            addCell(empTable, "HSBC, Lagos", normalFont);

            addCell(empTable, "Bank Acct", normalFont);
            addCell(empTable, "18005110026", normalFont);
            addCell(empTable, "Overtime Hours", normalFont);
            addCell(empTable, "54.00", normalFont);

            document.add(empTable);

            // ================= EARNINGS & DEDUCTIONS =================
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3, 2, 3, 2});

            addHeader(table, "Earnings", boldFont);
            addHeader(table, "Amount", boldFont);
            addHeader(table, "Deductions", boldFont);
            addHeader(table, "Amount", boldFont);

            addRow(table, "Basic Pay", "1,368.00", "National Insurance", "85.00");
            addRow(table, "Medical Allowance", "273.60", "Loss of Pay", "0.00");
            addRow(table, "Housing Allowance", "136.80", "Loan Repayment", "230.00");
            addRow(table, "Conveyance Allowance", "136.80", "Advance Repayment", "360.00");
            addRow(table, "Food Allowance", "68.40", "", "");
            addRow(table, "Overtime Allowance", "540.00", "", "");

            // Totals
            PdfPCell totalEarnLabel = new PdfPCell(new Phrase("Total Earnings (Rounded)", boldFont));
            totalEarnLabel.setColspan(1);
            totalEarnLabel.setBorder(Rectangle.TOP);
            table.addCell(totalEarnLabel);

            PdfPCell totalEarnValue = new PdfPCell(new Phrase("2,524.00", boldFont));
            totalEarnValue.setBorder(Rectangle.TOP);
            totalEarnValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(totalEarnValue);

            PdfPCell totalDedLabel = new PdfPCell(new Phrase("Total Deductions (Rounded)", boldFont));
            totalDedLabel.setBorder(Rectangle.TOP);
            table.addCell(totalDedLabel);

            PdfPCell totalDedValue = new PdfPCell(new Phrase("675.00", boldFont));
            totalDedValue.setBorder(Rectangle.TOP);
            totalDedValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(totalDedValue);

            // Net Pay
            PdfPCell netLabel = new PdfPCell(new Phrase("Net Pay (Rounded)", boldFont));
            netLabel.setColspan(3);
            netLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(netLabel);

            PdfPCell netValue = new PdfPCell(new Phrase("1,849.00", boldFont));
            netValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(netValue);

            document.add(table);

            // ================= SIGNATURE =================
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
            System.out.println("Payslip generated!");
            return baos.toByteArray();
        }

        // ================= HELPERS =================

        private void addCell(PdfPTable table, String text, Font font) {
            PdfPCell cell = new PdfPCell(new Phrase(text, font));
            cell.setPadding(5);
            cell.setBorder(Rectangle.NO_BORDER);
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
    public byte[] build(Payslip payslip, PayRun run) throws Exception {
            if(Objects.nonNull(payslip)) {
                return generateSalarySlip();
            }
            try {
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);
            document.open();

            String empName = (payslip.getEmployee().getFirstName() + " " + payslip.getEmployee().getLastName()).trim();
            document.add(new Paragraph("Payslip", TITLE));
            document.add(new Paragraph(" ", NORMAL));
            document.add(new Paragraph(
                    "Employee: " + empName + "  |  Code: " + nullSafe(payslip.getEmployee().getEmployeeCode()),
                    NORMAL
            ));
            document.add(new Paragraph(
                    "Period: " + run.getPeriodStart() + " to " + run.getPeriodEnd(),
                    NORMAL
            ));
            document.add(new Paragraph(" ", NORMAL));

            List<PayslipLine> lines = payslip.getLines().stream()
                    .sorted(Comparator
                            .comparing(PayslipLine::getKind)
                            .thenComparing(PayslipLine::getComponentCode))
                    .toList();

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3f, 1f});

            PdfPCell h1 = new PdfPCell(new Phrase("Component", HEADER));
            h1.setBorder(0);
            h1.setPaddingBottom(6);
            PdfPCell h2 = new PdfPCell(new Phrase("Amount", HEADER));
            h2.setHorizontalAlignment(Element.ALIGN_RIGHT);
            h2.setBorder(0);
            h2.setPaddingBottom(6);
            table.addCell(h1);
            table.addCell(h2);

            int row = 0;
            for (PayslipLine pl : lines) {
                if (row++ >= 40) {
                    break;
                }
                String label = sanitize(pl.getComponentName() + " (" + pl.getKind() + ")");
                PdfPCell c1 = new PdfPCell(new Phrase(label, NORMAL));
                c1.setBorder(0);
                c1.setPaddingTop(2);
                c1.setPaddingBottom(2);
                PdfPCell c2 = new PdfPCell(new Phrase(sanitize(pl.getAmount().toPlainString()), NORMAL));
                c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
                c2.setBorder(0);
                c2.setPaddingTop(2);
                c2.setPaddingBottom(2);
                table.addCell(c1);
                table.addCell(c2);
            }
            document.add(table);

            document.add(new Paragraph(" ", NORMAL));
            document.add(new Paragraph("Gross: " + sanitize(payslip.getGrossAmount().toPlainString()), NORMAL_BOLD));
            document.add(new Paragraph("Deductions: " + sanitize(payslip.getDeductionAmount().toPlainString()), NORMAL_BOLD));
            document.add(new Paragraph("Net pay: " + sanitize(payslip.getNetAmount().toPlainString()),
                    new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD)));

            document.close();
            return baos.toByteArray();
        } catch (DocumentException e) {
            throw new IOException("Failed to build payslip PDF: " + e.getMessage(), e);
        }
    }

    private static String nullSafe(String s) {
        return s != null ? s : "—";
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
