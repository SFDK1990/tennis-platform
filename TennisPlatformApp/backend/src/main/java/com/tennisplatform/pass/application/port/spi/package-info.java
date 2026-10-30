/**
 * Interfaces this module needs and another module implements.
 *
 * <p>{@code booking} depends on this module to charge a lesson to a pass, so this module cannot
 * depend on {@code booking} to know how many lessons a pass has spent. It declares the question
 * here and {@code booking} answers it, as it does for {@code lesson} and {@code student}.
 */
package com.tennisplatform.pass.application.port.spi;
