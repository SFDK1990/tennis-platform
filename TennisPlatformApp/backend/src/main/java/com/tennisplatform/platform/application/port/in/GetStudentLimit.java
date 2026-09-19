package com.tennisplatform.platform.application.port.in;

/**
 * How many students the teacher may have under management at once.
 *
 * <p>The public read side of the platform configuration, and the whole reason this module
 * exists in Fase 6: {@code student} has to check the limit before taking a student on, and
 * {@code student} may not depend on {@code administration}, where 02-arquitectura.md had
 * filed the configuration. The console that changes the value is still Fase 7.
 *
 * <p>It returns an {@code int} and not the domain object on purpose: a caller in another
 * module that received {@code PlatformSettings} would depend on this module's domain, which
 * the module boundary rules reject - and rightly, because then the limit could not be
 * reshaped without breaking them.
 */
public interface GetStudentLimit {

    int studentLimit();
}
