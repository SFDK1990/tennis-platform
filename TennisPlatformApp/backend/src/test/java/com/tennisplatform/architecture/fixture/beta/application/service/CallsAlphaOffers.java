package com.tennisplatform.architecture.fixture.beta.application.service;

import com.tennisplatform.architecture.fixture.alpha.application.port.in.AlphaOffers;

public class CallsAlphaOffers {

    public int use(AlphaOffers offers) {
        return offers.offer();
    }
}
