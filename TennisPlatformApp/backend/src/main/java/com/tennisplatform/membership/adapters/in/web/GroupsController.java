package com.tennisplatform.membership.adapters.in.web;

import com.tennisplatform.identity.application.port.in.AuthenticatedUser;
import com.tennisplatform.membership.application.port.in.GroupView;
import com.tennisplatform.membership.application.port.in.GroupView.MemberView;
import com.tennisplatform.membership.application.port.in.JoinGroups;
import com.tennisplatform.membership.application.port.in.ManageGroups;
import com.tennisplatform.membership.application.port.in.MembershipView;
import com.tennisplatform.membership.application.port.in.QueryGroups;
import com.tennisplatform.shared.domain.ForbiddenOperationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * The fixed groups (Fase 28): the teacher makes them out of weekly series, and students join and
 * leave them, by themselves or through the teacher.
 */
@RestController
@RequestMapping("/api/v1")
class GroupsController {

    private final ManageGroups manage;
    private final QueryGroups query;
    private final JoinGroups join;

    GroupsController(ManageGroups manage, QueryGroups query, JoinGroups join) {
        this.manage = manage;
        this.query = query;
        this.join = join;
    }

    /** For anybody signed in: what a student needs to choose a group. Who is in each is left out. */
    @GetMapping("/groups")
    GroupsResponse all() {
        return new GroupsResponse(query.all());
    }

    @GetMapping("/teacher/groups")
    GroupsResponse ofTeacher(@AuthenticationPrincipal AuthenticatedUser caller) {
        requireTeacher(caller);
        return new GroupsResponse(manage.ofTeacher(caller.id()));
    }

    @PostMapping("/teacher/groups")
    @ResponseStatus(HttpStatus.CREATED)
    GroupView create(@AuthenticationPrincipal AuthenticatedUser caller, @Valid @RequestBody CreateGroupRequest request) {
        requireTeacher(caller);
        return manage.create(caller.id(), request.name(), request.monthlyFeeCents());
    }

    @PatchMapping("/teacher/groups/{id}")
    GroupView update(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID id,
                     @RequestBody UpdateGroupRequest request) {
        requireTeacher(caller);
        return manage.update(caller.id(), id, request.name(), request.monthlyFeeCents());
    }

    @DeleteMapping("/teacher/groups/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID id) {
        requireTeacher(caller);
        manage.delete(caller.id(), id);
    }

    @PostMapping("/teacher/groups/{id}/series")
    GroupView attachSeries(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID id,
                           @Valid @RequestBody AttachSeriesRequest request) {
        requireTeacher(caller);
        return manage.attachSeries(caller.id(), id, request.seriesId());
    }

    @PostMapping("/teacher/groups/{id}/members")
    @ResponseStatus(HttpStatus.CREATED)
    MemberView addMember(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID id,
                         @Valid @RequestBody AddMemberRequest request) {
        requireTeacher(caller);
        return join.add(caller.id(), id, request.studentUserId());
    }

    @DeleteMapping("/teacher/groups/{id}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removeMember(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID id,
                      @PathVariable UUID userId) {
        requireTeacher(caller);
        join.remove(caller.id(), id, userId);
    }

    @PostMapping("/me/groups/{id}/membership")
    @ResponseStatus(HttpStatus.CREATED)
    MembershipView joinGroup(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID id) {
        requireStudent(caller);
        return join.join(caller.id(), caller.emailVerified(), id);
    }

    @DeleteMapping("/me/groups/{id}/membership")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void leaveGroup(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID id) {
        requireStudent(caller);
        join.leave(caller.id(), id);
    }

    @GetMapping("/me/memberships")
    MembershipsResponse mine(@AuthenticationPrincipal AuthenticatedUser caller) {
        requireStudent(caller);
        return new MembershipsResponse(join.ofStudent(caller.id()));
    }

    private static void requireTeacher(AuthenticatedUser caller) {
        if (caller == null || !caller.isTeacher()) {
            throw ForbiddenOperationException.teacherOnly("Only the teacher manages the groups");
        }
    }

    private static void requireStudent(AuthenticatedUser caller) {
        if (caller == null || !caller.isStudent()) {
            throw ForbiddenOperationException.roleNotAllowed("Only a student joins a group");
        }
    }

    record GroupsResponse(List<GroupView> items) {
    }

    record MembershipsResponse(List<MembershipView> items) {
    }

    record CreateGroupRequest(@NotNull String name, @NotNull Integer monthlyFeeCents) {
    }

    record UpdateGroupRequest(String name, Integer monthlyFeeCents) {
    }

    record AttachSeriesRequest(@NotNull UUID seriesId) {
    }

    record AddMemberRequest(@NotNull UUID studentUserId) {
    }
}
