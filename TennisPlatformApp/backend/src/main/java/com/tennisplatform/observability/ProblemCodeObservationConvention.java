package com.tennisplatform.observability;

import com.tennisplatform.error.ProblemCode;
import io.micrometer.common.KeyValues;
import org.springframework.http.server.observation.DefaultServerRequestObservationConvention;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.stereotype.Component;

/**
 * Adds the error code to {@code http.server.requests}. With the route and the status it is what
 * tells a lost last seat ({@code LESSON_FULL}) from a clash with another booking
 * ({@code STUDENT_SCHEDULE_OVERLAP}), without a counter inside any service. The codes are a
 * fixed list of about fifty, so the tag does not blow up the number of series.
 */
@Component
class ProblemCodeObservationConvention extends DefaultServerRequestObservationConvention {

    @Override
    public KeyValues getLowCardinalityKeyValues(ServerRequestObservationContext context) {
        return super.getLowCardinalityKeyValues(context).and("code", ProblemCode.of(context.getCarrier()));
    }
}
