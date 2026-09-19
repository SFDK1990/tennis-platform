package com.tennisplatform.platform.application.service;

import com.tennisplatform.platform.application.port.in.GetStudentLimit;
import com.tennisplatform.platform.application.port.out.PlatformSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

public class GetStudentLimitService implements GetStudentLimit {

    private static final Logger log = LoggerFactory.getLogger(GetStudentLimitService.class);

    /**
     * Only reached when the seeded row is missing, which means somebody deleted it. Refusing to
     * answer would take the management of students down over a configuration value; answering
     * with the same number the changeset seeds keeps the platform working and says so loudly.
     */
    private static final int FALLBACK_STUDENT_LIMIT = 50;

    private final PlatformSettingsRepository settings;

    public GetStudentLimitService(PlatformSettingsRepository settings) {
        this.settings = settings;
    }

    @Override
    @Transactional(readOnly = true)
    public int studentLimit() {
        return settings.find()
                .map(configuration -> configuration.studentLimit())
                .orElseGet(() -> {
                    log.warn("No platform configuration row found; falling back to a student "
                            + "limit of {}. The v4-platform changeset seeds this row, so it "
                            + "has been deleted by hand.", FALLBACK_STUDENT_LIMIT);
                    return FALLBACK_STUDENT_LIMIT;
                });
    }
}
