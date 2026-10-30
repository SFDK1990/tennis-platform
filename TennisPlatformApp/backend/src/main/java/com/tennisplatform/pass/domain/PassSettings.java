package com.tennisplatform.pass.domain;

/**
 * The teacher's two switches, both off until they turn them on: no booking without a pass
 * (Fase 27, decision 2) and selling passes in the app (Fase 27.5, decision 2).
 */
public record PassSettings(boolean required, boolean onlinePayment) {

    public static final PassSettings DEFAULTS = new PassSettings(false, false);

    /** A null keeps the current value. */
    public PassSettings changed(Boolean newRequired, Boolean newOnlinePayment) {
        return new PassSettings(newRequired == null ? required : newRequired,
                newOnlinePayment == null ? onlinePayment : newOnlinePayment);
    }
}
