package com.tennisplatform.notification.configuration;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "tennis.notification")
public class NotificationProperties {

    /** Signs every email and names the sender. */
    @NotBlank
    private String schoolName = "Escuela de Tenis Oasis Papagayo";

    @NotBlank
    private String mailFrom = "no-reply@tennis-platform.local";

    /** For the links in the emails; the pages live in the frontend. */
    @NotBlank
    private String frontendBaseUrl = "http://localhost:3000";

    /** Off in the test profile, so a test can read what was sent as soon as its request returns. */
    private boolean sendInBackground = true;

    public String getSchoolName() {
        return schoolName;
    }

    public void setSchoolName(String schoolName) {
        this.schoolName = schoolName;
    }

    public String getMailFrom() {
        return mailFrom;
    }

    public void setMailFrom(String mailFrom) {
        this.mailFrom = mailFrom;
    }

    public String getFrontendBaseUrl() {
        return frontendBaseUrl;
    }

    public void setFrontendBaseUrl(String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public boolean isSendInBackground() {
        return sendInBackground;
    }

    public void setSendInBackground(boolean sendInBackground) {
        this.sendInBackground = sendInBackground;
    }
}
