package com.hrms.payroll;

import com.hrms.payroll.entity.PayRun;
import com.hrms.payroll.entity.Payslip;
import com.hrms.payroll.entity.PayslipLine;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;

@Service
public class PayslipPdfService {

    public byte[] build(Payslip payslip, PayRun run) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            float margin = 50;
            float y = page.getMediaBox().getHeight() - margin;

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                String empName = (payslip.getEmployee().getFirstName() + " " + payslip.getEmployee().getLastName()).trim();
                y = drawLine(cs, PDType1Font.HELVETICA_BOLD, 16, margin, y, "Payslip");
                y -= 8;
                y = drawLine(cs, PDType1Font.HELVETICA, 11, margin, y,
                        "Employee: " + empName + "  |  Code: " + nullSafe(payslip.getEmployee().getEmployeeCode()));
                y = drawLine(cs, PDType1Font.HELVETICA, 11, margin, y,
                        "Period: " + run.getPeriodStart() + " to " + run.getPeriodEnd());
                y -= 12;

                List<PayslipLine> lines = payslip.getLines().stream()
                        .sorted(Comparator
                                .comparing(PayslipLine::getKind)
                                .thenComparing(PayslipLine::getComponentCode))
                        .toList();

                y = drawLine(cs, PDType1Font.HELVETICA_BOLD, 11, margin, y, "Component");
                drawLineRight(cs, PDType1Font.HELVETICA_BOLD, 11, page.getMediaBox().getWidth() - margin, y, "Amount");
                y -= 14;

                for (PayslipLine pl : lines) {
                    String label = pl.getComponentName() + " (" + pl.getKind() + ")";
                    y = drawLine(cs, PDType1Font.HELVETICA, 10, margin, y, label);
                    drawLineRight(cs, PDType1Font.HELVETICA, 10, page.getMediaBox().getWidth() - margin, y, pl.getAmount().toPlainString());
                    y -= 14;
                    if (y < 80) {
                        break;
                    }
                }

                y -= 10;
                y = drawLine(cs, PDType1Font.HELVETICA_BOLD, 11, margin, y,
                        "Gross: " + payslip.getGrossAmount().toPlainString());
                y = drawLine(cs, PDType1Font.HELVETICA_BOLD, 11, margin, y,
                        "Deductions: " + payslip.getDeductionAmount().toPlainString());
                y = drawLine(cs, PDType1Font.HELVETICA_BOLD, 12, margin, y,
                        "Net pay: " + payslip.getNetAmount().toPlainString());
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    private static String nullSafe(String s) {
        return s != null ? s : "—";
    }

    private float drawLine(PDPageContentStream cs, PDType1Font font, float size, float x, float y, String text)
            throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(sanitize(text));
        cs.endText();
        return y - size - 4;
    }

    private void drawLineRight(PDPageContentStream cs, PDType1Font font, float size, float rightX, float y, String text)
            throws IOException {
        String t = sanitize(text);
        float width = font.getStringWidth(t) / 1000 * size;
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(rightX - width, y);
        cs.showText(t);
        cs.endText();
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
