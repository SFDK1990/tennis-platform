package com.tennisplatform.platform.application.service;

import com.tennisplatform.platform.application.port.in.GetMaxGroupCapacity;
import com.tennisplatform.platform.application.port.in.GetStudentLimit;
import com.tennisplatform.platform.application.port.out.PlatformSettingsRepository;
import com.tennisplatform.platform.domain.PlatformSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.function.ToIntFunction;

/**
 * Serves every limit the platform configuration holds.
 *
 * <p>One service for two ports rather than one each: what a caller needs differs, but the way
 * the value is obtained - read the singleton row, and if somebody deleted it say so loudly and
 * carry on - is identical, and it was going to be copied the moment a second limit appeared.
 * The ports stay separate so that {@code student} still depends only on the limit it reads.
 */
public class PlatformLimitsService implements GetStudentLimit, GetMaxGroupCapacity {

    private static final Logger log = LoggerFactory.getLogger(PlatformLimitsService.class);

    /**
     * Only reached when the seeded row is missing, which means somebody deleted it. Refusing to
     * answer would take the platform down over a configuration value; answering with the same
     * numbers the changesets seed keeps it working and says so loudly.
     */
    private static final int FALLBACK_STUDENT_LIMIT = 50;
    private static final int FALLBACK_MAX_GROUP_CAPACITY = 8;

    private final PlatformSettingsRepository settings;

    public PlatformLimitsService(PlatformSettingsRepository settings) {
        this.settings = settings;
    }

    @Override
    @Transactional(readOnly = true)
    public int studentLimit() {
        return read(PlatformSettings::studentLimit, FALLBACK_STUDENT_LIMIT, "student limit");
    }

    @Override
    @Transactional(readOnly = true)
    public int maxGroupCapacity() {
        return read(PlatformSettings::maxGroupCapacity, FALLBACK_MAX_GROUP_CAPACITY, "group capacity limit");
    }

    private int read(ToIntFunction<PlatformSettings> value, int fallback, String what) {
        Optional<PlatformSettings> configuration = settings.find();
        if (configuration.isEmpty()) {
            log.warn("No platform configuration row found; falling back to a {} of {}. The "
                    + "v4-platform changeset seeds this row, so it has been deleted by hand.",
                    what, fallback);
            return fallback;
        }
        return value.applyAsInt(configuration.get());
    }
}
