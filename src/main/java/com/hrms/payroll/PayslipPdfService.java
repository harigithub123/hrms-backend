package com.hrms.payroll;

import com.hrms.payroll.entity.PayRun;
import com.hrms.payroll.entity.Payslip;
import com.hrms.payroll.entity.PayslipLine;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;

@Service
public class PayslipPdfService {

    private static final Font TITLE = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
    private static final Font HEADER = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);
    private static final Font NORMAL = new Font(Font.FontFamily.HELVETICA, 10);
    private static final Font NORMAL_BOLD = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);

    public byte[] build(Payslip payslip, PayRun run) throws IOException {
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
