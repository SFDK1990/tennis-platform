/**
 * Interfaces this module needs and another module implements.
 *
 * <p>{@code booking} depends on {@code student}, never the other way round, and yet letting a
 * student go has to cancel their upcoming bookings. Declaring the need here and letting {@code
 * booking} fulfil it keeps the compile-time edge where the graph allows it. Other modules may
 * implement what is in this package and nothing else of it; {@code ModuleBoundariesTest} holds
 * them to that.
 */
package com.tennisplatform.student.application.port.spi;
