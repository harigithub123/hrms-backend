package com.hrms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Layout for overlaying merged offer HTML onto a fixed letterhead PDF.
 * Tune {@code body-start-y} if text overlaps your logo/header.
 */
@ConfigurationProperties(prefix = "hrms.offer")
public class OfferPdfTemplateProperties {

    /**
     * Classpath template (bundled in jar), e.g. {@code classpath:/templates/offer-letter-template.pdf}
     */
    private String pdfTemplate = "classpath:/templates/offer-letter-template.pdf";

    /**
     * Optional absolute file path override (takes precedence when non-blank), e.g.
     * {@code C:/path/to/HariNale_Offer_Letter_20220115_133603.pdf}
     */
    private String pdfTemplateFile = "";

    /** Y position (PDF points, bottom-left origin) where body text starts. A4 height ≈ 842. */
    private float bodyStartY = 620f;

    private float bodyMarginX = 50f;

    /** Stop wrapping to next page when below this Y. */
    private float bodyMinY = 72f;

    private float bodyLineHeight = 12f;

    private int wrapChars = 85;

    public String getPdfTemplate() {
        return pdfTemplate;
    }

    public void setPdfTemplate(String pdfTemplate) {
        this.pdfTemplate = pdfTemplate;
    }

    public String getPdfTemplateFile() {
        return pdfTemplateFile;
    }

    public void setPdfTemplateFile(String pdfTemplateFile) {
        this.pdfTemplateFile = pdfTemplateFile;
    }

    public float getBodyStartY() {
        return bodyStartY;
    }

    public void setBodyStartY(float bodyStartY) {
        this.bodyStartY = bodyStartY;
    }

    public float getBodyMarginX() {
        return bodyMarginX;
    }

    public void setBodyMarginX(float bodyMarginX) {
        this.bodyMarginX = bodyMarginX;
    }

    public float getBodyMinY() {
        return bodyMinY;
    }

    public void setBodyMinY(float bodyMinY) {
        this.bodyMinY = bodyMinY;
    }

    public float getBodyLineHeight() {
        return bodyLineHeight;
    }

    public void setBodyLineHeight(float bodyLineHeight) {
        this.bodyLineHeight = bodyLineHeight;
    }

    public int getWrapChars() {
        return wrapChars;
    }

    public void setWrapChars(int wrapChars) {
        this.wrapChars = wrapChars;
    }
}
