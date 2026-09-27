package com.tennisplatform.platform.application.service;

import com.tennisplatform.platform.application.port.in.ManagePlatformSettings;
import com.tennisplatform.platform.application.port.in.PlatformSettingsView;
import com.tennisplatform.platform.application.port.out.PlatformSettingsRepository;
import com.tennisplatform.platform.domain.PlatformSettings;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * Unlike {@link PlatformLimitsService}, there is no fallback here: a console that showed or
 * changed made-up numbers would hide that the seeded row is gone, which is the thing to fix.
 */
public class PlatformSettingsService implements ManagePlatformSettings {

    private final PlatformSettingsRepository settings;
    private final Clock clock;

    public PlatformSettingsService(PlatformSettingsRepository settings, Clock clock) {
        this.settings = settings;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformSettingsView current() {
        return view(stored());
    }

    @Override
    @Transactional
    public PlatformSettingsView change(UUID administrator, Integer studentLimit, Integer maxGroupCapacity) {
        PlatformSettings changed = stored().changedBy(administrator, clock.instant(), studentLimit, maxGroupCapacity);
        return view(settings.save(changed));
    }

    private PlatformSettings stored() {
        return settings.find().orElseThrow(() -> new IllegalStateException(
                "The platform configuration row is missing; the v4-platform changeset seeds it"));
    }

    private static PlatformSettingsView view(PlatformSettings settings) {
        return new PlatformSettingsView(settings.studentLimit(), settings.maxGroupCapacity(),
                settings.updatedAt(), settings.updatedBy());
    }
}
