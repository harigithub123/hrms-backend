package com.hrms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hrms.email")
public class HrmsEmailProperties {

    private String from = "no-reply@example.com";
    private String replyTo = "";
    private String offerSubject = "Your Offer Letter";

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }
    public String getReplyTo() { return replyTo; }
    public void setReplyTo(String replyTo) { this.replyTo = replyTo; }
    public String getOfferSubject() { return offerSubject; }
    public void setOfferSubject(String offerSubject) { this.offerSubject = offerSubject; }
}

