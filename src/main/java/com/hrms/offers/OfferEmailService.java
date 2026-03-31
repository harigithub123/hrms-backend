package com.hrms.offers;

import com.hrms.config.HrmsEmailProperties;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class OfferEmailService {

    private final JavaMailSender mailSender;
    private final HrmsEmailProperties emailProps;

    public OfferEmailService(JavaMailSender mailSender, HrmsEmailProperties emailProps) {
        this.mailSender = mailSender;
        this.emailProps = emailProps;
    }

    public void sendOffer(String to, String candidateName, byte[] pdfBytes, String pdfFilename) throws Exception {
        String safeTo = to != null ? to.trim() : "";
        if (safeTo.isBlank()) {
            throw new IllegalArgumentException("Candidate personal email is required to send offer");
        }

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
        helper.setTo(safeTo);
        helper.setFrom(emailProps.getFrom());
        if (emailProps.getReplyTo() != null && !emailProps.getReplyTo().isBlank()) {
            helper.setReplyTo(emailProps.getReplyTo().trim());
        }
        String subject = emailProps.getOfferSubject() != null ? emailProps.getOfferSubject() : "Your Offer Letter";
        helper.setSubject(subject);

        String name = candidateName != null && !candidateName.isBlank() ? candidateName.trim() : "Candidate";
        String body = """
                Dear %s,

                Please find attached your offer letter.

                Regards,
                HR
                """.formatted(name);
        helper.setText(body, false);
        helper.addAttachment(pdfFilename != null && !pdfFilename.isBlank() ? pdfFilename : "OfferLetter.pdf",
                new org.springframework.core.io.ByteArrayResource(pdfBytes));

        mailSender.send(message);
    }
}

