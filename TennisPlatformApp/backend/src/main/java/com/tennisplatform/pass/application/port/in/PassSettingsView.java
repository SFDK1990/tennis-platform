package com.tennisplatform.pass.application.port.in;

/**
 * The teacher's switches, and whether online payment can be switched on at all: it needs the
 * gateway's keys in the server's environment (42-fase27.5, decision 6).
 */
public record PassSettingsView(boolean required, boolean onlinePayment, boolean onlinePaymentAvailable) {
}
