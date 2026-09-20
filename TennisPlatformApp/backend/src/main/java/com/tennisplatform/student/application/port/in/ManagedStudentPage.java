package com.tennisplatform.student.application.port.in;

import java.util.List;

/**
 * A page of managed students, shaped as 11-contrato-api.md defines pagination:
 * {@code { items, page, size, totalItems }}.
 *
 * <p>Declared here rather than in {@code shared} because it is the first listing in the
 * codebase and nothing else needs it yet. When the second one arrives - bookings, or the admin
 * user list - this is the moment to extract a generic page into {@code shared} and let every
 * module depend on it, which also means widening the dependency graph to allow that. Doing it
 * now would be guessing at the shape from a single example.
 */
public record ManagedStudentPage(List<ManagedStudentView> items, int page, int size,
                                 long totalItems) {

    /**
     * Copies the list on the way in, so a page cannot be edited through the collection the
     * caller happened to pass. A record gives away its components by reference, which makes
     * "immutable" true of the reference and not of what it points at.
     */
    public ManagedStudentPage {
        items = List.copyOf(items);
    }
}
