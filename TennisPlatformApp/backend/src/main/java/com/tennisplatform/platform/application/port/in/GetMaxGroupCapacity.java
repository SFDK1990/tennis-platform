package com.tennisplatform.platform.application.port.in;

/**
 * The largest a group lesson may be.
 *
 * <p>Read by {@code lesson} before creating one. Until Fase 8 the only rule was "more than
 * zero", so a typo could create a lesson with ten thousand seats on a court that fits a handful.
 *
 * <p>Returns an {@code int} and not the domain object, for the same reason as
 * {@link GetStudentLimit}: a caller in another module that received {@code PlatformSettings}
 * would depend on this module's domain, which the boundary rules reject.
 */
public interface GetMaxGroupCapacity {

    int maxGroupCapacity();
}
