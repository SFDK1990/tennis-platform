package com.tennisplatform.student.application.port.in;

import java.util.UUID;

/**
 * A student deletes their own account (01-analisis-funcional.md §18,
 * 30-fase19-analisis-cierre-mvp.md). One transaction: the account loses its address and
 * password, every teacher stops managing the student, their upcoming bookings are cancelled
 * and their profile is emptied. Past bookings stay, under "Alumno eliminado".
 */
public interface DeleteMyAccount {

    /** Asks for the password: it cannot be undone, and an access token alone should not be enough. */
    void delete(UUID studentUserId, String password);
}
