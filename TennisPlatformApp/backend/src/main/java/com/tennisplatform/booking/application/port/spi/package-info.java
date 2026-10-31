/**
 * Interfaces this module needs and another module implements.
 *
 * <p>{@code membership} (Fase 28) depends on this module to book the seats of a fixed group, so
 * this module cannot depend on {@code membership} to know whether a seat is paid by a monthly
 * fee. It declares the question here and {@code membership} answers it.
 */
package com.tennisplatform.booking.application.port.spi;
