package com.hrms.offers;

import com.hrms.offers.entity.JobOffer;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.util.Objects.nonNull;

/**
 * Renders offer text onto a fixed letterhead PDF (your company template). If no template is found,
 * falls back to a simple generated page. Uses iText 5.
 */
@Service
public class OfferPdfService {

    private static final int FOOTER_HEIGHT = 50;

    public byte[] build(JobOffer offer) throws IOException {
        OfferLetterPdfModel model = new OfferLetterPdfModel(
                offer != null ? offer.getEmployeeType() : null,
                offer != null ? offer.getCandidateName() : null,
                offer != null ? offer.getCandidateEmail() : null,
                offer != null ? offer.getCandidateMobile() : null,
                offer != null ? offer.getJoiningDate() : null,
                offer != null ? offer.getOfferReleaseDate() : null,
                offer != null && offer.getProbationPeriodMonths() != null ? offer.getProbationPeriodMonths() : 0,
                offer != null && offer.getJoiningBonus() != null ? offer.getJoiningBonus(): null,
                offer != null && offer.getYearlyBonus() != null ? offer.getYearlyBonus() : null,
                offer != null && offer.getDesignation() != null ? offer.getDesignation().getName() : "—",
                offer != null && offer.getDepartment() != null ? offer.getDepartment().getName() : "—",
                offer != null && offer.getAnnualCtc() != null ? offer.getAnnualCtc(): BigDecimal.ZERO,
                List.of()
        );
        return generateOfferLetter(model);
    }

    public record OfferLetterPdfModel(
            String employeeType,
            String employeeName,
            String personalEmail,
            String mobile,
            LocalDate joiningDate,
            LocalDate offerReleaseDate,
            int probationMonths,
            BigDecimal joiningBonus,
            BigDecimal yearlyBonus,
            String designation,
            String department,
            BigDecimal annualCtc,
            List<OfferCompLine> compensationLines
    ) {}

    public record OfferCompLine(String componentLabel, BigDecimal amount) {}

    public byte[] generateOfferLetter(OfferLetterPdfModel data) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            Document document = new Document(PageSize.A4, 40, 40, 40, 40 + FOOTER_HEIGHT);
            PdfWriter writer = PdfWriter.getInstance(document, baos);

            writer.setPageEvent(new FooterPageEvent());

            document.open();

            // Fonts
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 11);
            Font boldFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);

            // Logo
            addLogo(document);

            // Title
            Paragraph title = new Paragraph("OFFER LETTER", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Date
            Paragraph date = new Paragraph(
                    "Date: " + formatDate(data.offerReleaseDate()), normalFont);
            date.setAlignment(Element.ALIGN_RIGHT);
            date.setSpacingAfter(20);
            document.add(date);

            // Candidate Name
            document.add(new Paragraph(data.employeeName(), boldFont));
            //document.add(new Paragraph(data.(), normalFont));
            document.add(Chunk.NEWLINE);

            // Subject
            Paragraph subject = new Paragraph(
                    "Subject: Offer for the position of " + data.designation(),
                    boldFont);
            subject.setSpacingAfter(10);
            document.add(subject);

            // Body
            document.add(new Paragraph(
                    "Dear " + data.employeeName() + ",",
                    normalFont));
            document.add(Chunk.NEWLINE);

            document.add(new Paragraph(
                    "We are pleased to extend to you an offer for the position of "
                            + data.designation()
                            + " with Kambson Private Limited, based in "
                            + " Bangalore.",
                    normalFont));

            document.add(Chunk.NEWLINE);

            document.add(new Paragraph(
                    "You will be placed on probation for a period of "+ data.probationMonths()  +" months from your date of joining."
                    + " This offer is conditional upon successful completion of background verification and submission of all required documentation."
                    + " Any discrepancy or misrepresentation may result in withdrawal of this offer or termination of employment without notice. ",
                    normalFont));

            document.add(Chunk.NEWLINE);

            String txt = "We are sure your valuable experience and passion to excel will be of great value to Kambson Private Limited and will help" +
                    " Kambson Private Limited move faster towards its global vision.\n";

            document.add(new Paragraph(txt, normalFont));
            document.add(Chunk.NEWLINE);
            // Salary Table
            document.add(new Paragraph("Compensation Details:", headerFont));
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3, 2});

            addCell(table, "Component", boldFont);
            addCell(table, "Amount", boldFont);

            List<OfferCompLine> compLines = data.compensationLines() != null ? data.compensationLines() : List.of();
            for (OfferCompLine l : compLines) {
                addCell(table, l.componentLabel() != null ? l.componentLabel() : "Component", normalFont);
                addCell(table, l.amount() != null ? l.amount().toString() : "", normalFont);
            }

            addCell(table, "Total", boldFont);
            addCell(table, data.annualCtc()+"", boldFont);

            table.setSpacingAfter(20);
            document.add(table);

            if (BigDecimal.ZERO.compareTo(data.joiningBonus()) > 0) {
                document.add(new Paragraph(
                        "As discussed, you will also receive a joining bonus of " + data.joiningBonus()  + " in first month salary.\n",
                        normalFont));
                document.add(Chunk.NEWLINE);
            }

            document.add(new Paragraph(
                    "This letter constitutes an offer of employment and does not create an employer-employee relationship"
                    + " until a formal employment agreement is executed.\n",
                    normalFont));
            document.add(Chunk.NEWLINE);

            document.add(new Paragraph(
                    "We are confident that your skills and experience will be a valuable addition to our organization,"
                    +" and we look forward to your contribution.",
                    normalFont));
            document.add(Chunk.NEWLINE);

            // Joining Date
            document.add(new Paragraph(
                    "Your expected joining date is: " + formatDate(data.joiningDate())+"\n",
                    normalFont));
            document.add(Chunk.NEWLINE);

            // Closing
            document.add(new Paragraph(
                    "We look forward to welcoming you to the team.\n",
                    normalFont));
            document.add(Chunk.NEWLINE);
            document.add(Chunk.NEWLINE);

            document.add(new Paragraph("Sincerely,", normalFont));
            document.add(new Paragraph("Kambson Private Limited", boldFont));

            document.close();

        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }

        return baos.toByteArray();
    }

    private static void addLogo(Document document) {
        try {
            InputStream is = OfferPdfService.class
                    .getClassLoader()
                    .getResourceAsStream("images/logo.png");

            if (is != null) {
                Image logo = Image.getInstance(is.readAllBytes());
                logo.scaleToFit(120, 60);
                logo.setAlignment(Element.ALIGN_LEFT);
                document.add(logo);
            }
        } catch (Exception ignored) {
        }
    }

    private static void addCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(8);
        table.addCell(cell);
    }

    private static String formatDate(LocalDate date) {
        return date != null
                ? date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                : "";
    }

}
