package com.tennisplatform.architecture.fixture.beta.application.service;

import com.tennisplatform.architecture.fixture.alpha.application.port.spi.AlphaNeeds;

public class CallsAlphaNeeds {

    public int use(AlphaNeeds needs) {
        return needs.count();
    }
}
