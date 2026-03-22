package com.hrms.offers;

import com.hrms.config.OfferPdfTemplateProperties;
import com.hrms.offers.entity.JobOffer;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Renders offer text onto a fixed letterhead PDF (your company template). If no template is found,
 * falls back to a simple generated page. Uses iText 5.
 */
@Service
public class OfferPdfService {

    private static final Logger log = LoggerFactory.getLogger(OfferPdfService.class);

    private static final float BLANK_MARGIN = 50;
    private static final float TITLE_GAP = 16;
    private static final float BOTTOM_GUARD = 50;

    private final OfferPdfTemplateProperties props;
    private final ResourceLoader resourceLoader;

    public OfferPdfService(OfferPdfTemplateProperties props, ResourceLoader resourceLoader) {
        this.props = props;
        this.resourceLoader = resourceLoader;
    }

    public byte[] build(JobOffer offer) throws IOException {
        String html = offer.getBodyHtml() != null ? offer.getBodyHtml() : "";
        return buildFromHtml(html);
    }

    public byte[] buildFromHtml(String bodyHtml) throws IOException {
        String text = bodyHtml != null
                ? bodyHtml.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim()
                : "";
        text = toHelveticaSafe(text);
        List<String> lines = wrap(text, props.getWrapChars());

        Optional<Resource> template = resolveTemplateResource();
        if (template.isPresent()) {
            try (InputStream is = template.get().getInputStream()) {
                return buildOnTemplate(is, lines);
            }
        }
        log.info(
                "Offer PDF template not found (classpath:/templates/offer-letter-template.pdf or hrms.offer.pdf-template-file). Using generated layout."
        );
        return buildBlankPdf(lines);
    }

    private Optional<Resource> resolveTemplateResource() {
        String filePath = props.getPdfTemplateFile();
        if (filePath != null && !filePath.isBlank()) {
            Path p = Paths.get(filePath.trim());
            if (Files.isRegularFile(p)) {
                return Optional.of(new FileSystemResource(p.toFile()));
            }
            log.warn("hrms.offer.pdf-template-file does not exist: {}", p.toAbsolutePath());
        }
        Resource cp = resourceLoader.getResource(props.getPdfTemplate());
        try {
            if (cp.exists() && cp.isReadable()) {
                return Optional.of(cp);
            }
        } catch (Exception e) {
            log.debug("Classpath offer template not available: {}", e.getMessage());
        }
        return Optional.empty();
    }

    private byte[] buildOnTemplate(InputStream templatePdf, List<String> lines) throws IOException {
        try {
            PdfReader reader = new PdfReader(templatePdf);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfStamper stamper = new PdfStamper(reader, baos);
            try {
                Font font = new Font(Font.FontFamily.HELVETICA, 10);
                float margin = props.getBodyMarginX();
                float lineHeight = props.getBodyLineHeight();
                float minY = props.getBodyMinY();

                int totalPages = reader.getNumberOfPages();
                int pageNum = 1;
                float y = props.getBodyStartY();

                for (int i = 0; i < lines.size(); i++) {
                    if (y < minY) {
                        pageNum++;
                        if (pageNum > totalPages) {
                            stamper.insertPage(pageNum, PageSize.A4);
                            totalPages++;
                        }
                        y = PageSize.A4.getHeight() - BLANK_MARGIN;
                    }
                    PdfContentByte cb = stamper.getOverContent(pageNum);
                    String line = safeLine(lines.get(i));
                    ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, new Phrase(line, font), margin, y, 0);
                    y -= lineHeight;
                }
            } finally {
                stamper.close();
                reader.close();
            }
            return baos.toByteArray();
        } catch (DocumentException e) {
            throw new IOException("Failed to stamp offer PDF: " + e.getMessage(), e);
        }
    }

    private byte[] buildBlankPdf(List<String> lines) throws IOException {
        try {
            Document document = new Document(PageSize.A4, BLANK_MARGIN, BLANK_MARGIN, BLANK_MARGIN, BLANK_MARGIN);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
            Font bodyFont = new Font(Font.FontFamily.HELVETICA, 10);
            document.add(new Paragraph("Offer letter", titleFont));
            document.add(new Paragraph(" ", bodyFont));

            for (String line : lines) {
                document.add(new Paragraph(safeLine(line), bodyFont));
            }

            document.close();
            return baos.toByteArray();
        } catch (DocumentException e) {
            throw new IOException("Failed to build offer PDF: " + e.getMessage(), e);
        }
    }

    private static String safeLine(String line) {
        String safe = line == null || line.isBlank() ? " " : line;
        if (safe.length() > 500) {
            safe = safe.substring(0, 500) + "...";
        }
        return safe;
    }

    private static String toHelveticaSafe(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            if (c >= 32 && c <= 126) {
                sb.append(c);
            } else if (Character.isWhitespace(c)) {
                sb.append(' ');
            } else {
                sb.append(' ');
            }
        }
        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    private static List<String> wrap(String text, int maxChars) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isBlank()) {
            out.add("(No content)");
            return out;
        }
        String[] words = text.split(" ");
        StringBuilder cur = new StringBuilder();
        for (String w : words) {
            if (cur.length() + w.length() + 1 > maxChars) {
                out.add(cur.toString());
                cur = new StringBuilder(w);
            } else {
                if (!cur.isEmpty()) {
                    cur.append(' ');
                }
                cur.append(w);
            }
        }
        if (!cur.isEmpty()) {
            out.add(cur.toString());
        }
        return out;
    }
}
