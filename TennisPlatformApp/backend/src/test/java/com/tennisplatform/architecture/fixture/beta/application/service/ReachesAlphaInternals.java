package com.tennisplatform.architecture.fixture.beta.application.service;

import com.tennisplatform.architecture.fixture.alpha.domain.AlphaInternal;

public class ReachesAlphaInternals {

    public int use(AlphaInternal internal) {
        return internal.value();
    }
}
