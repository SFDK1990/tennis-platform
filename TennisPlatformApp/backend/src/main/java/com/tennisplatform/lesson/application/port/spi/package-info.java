/**
 * Interfaces this module needs and another module implements.
 *
 * <p>{@code booking} depends on {@code lesson}, never the other way round, and yet cancelling a
 * lesson has to cancel its bookings and reading one has to count them. Declaring the need here
 * and letting {@code booking} fulfil it keeps the compile-time edge where the graph allows it,
 * while the call runs the other way. Other modules may implement what is in this package and
 * nothing else of it; {@code ModuleBoundariesTest} holds them to that.
 */
package com.tennisplatform.lesson.application.port.spi;
