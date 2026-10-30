package com.tennisplatform.pass.application.service;

import com.tennisplatform.pass.application.port.in.ChargePasses;
import com.tennisplatform.pass.application.port.in.ManagePasses;
import com.tennisplatform.pass.application.port.in.PassView;
import com.tennisplatform.pass.application.port.in.QueryPasses;
import com.tennisplatform.pass.application.port.out.PassRepository;
import com.tennisplatform.pass.application.port.out.PassSettingsRepository;
import com.tennisplatform.pass.application.port.out.PassTypeRepository;
import com.tennisplatform.pass.application.port.spi.PassBookings;
import com.tennisplatform.pass.domain.Pass;
import com.tennisplatform.pass.domain.PassLessonType;
import com.tennisplatform.pass.domain.PassNotFoundException;
import com.tennisplatform.pass.domain.PassType;
import com.tennisplatform.pass.domain.PassTypeNotFoundException;
import com.tennisplatform.pass.domain.PaymentMethod;
import com.tennisplatform.shared.domain.ForbiddenOperationException;
import com.tennisplatform.student.application.port.in.QueryManagedStudent;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import com.tennisplatform.teacher.application.port.in.TeacherProfileView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The passes of the students and what booking asks about them (41-fase27-analisis-bonos.md).
 *
 * <p>Days are the teacher's: a pass "until 15 January" ends at midnight on the courts, wherever
 * the server runs.
 */
public class PassesService implements ManagePasses, QueryPasses, ChargePasses {

    private static final Logger log = LoggerFactory.getLogger(PassesService.class);

    private final PassRepository passes;
    private final PassTypeRepository types;
    private final PassSettingsRepository settings;
    private final PassBookings bookings;
    private final QueryManagedStudent managedStudents;
    private final GetTeacherProfile teacherProfile;
    private final Clock clock;

    public PassesService(PassRepository passes, PassTypeRepository types, PassSettingsRepository settings,
                         PassBookings bookings, QueryManagedStudent managedStudents,
                         GetTeacherProfile teacherProfile, Clock clock) {
        this.passes = passes;
        this.types = types;
        this.settings = settings;
        this.bookings = bookings;
        this.managedStudents = managedStudents;
        this.teacherProfile = teacherProfile;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PassView record(UUID teacherUserId, UUID studentUserId, UUID passTypeId, LocalDate paidOn,
                           String paymentMethod) {
        if (!managedStudents.isManagedBy(teacherUserId, studentUserId)) {
            throw new ForbiddenOperationException("STUDENT_NOT_MANAGED", "This student is not managed by you.");
        }
        PassType type = types.findById(passTypeId)
                .filter(found -> found.teacherUserId().equals(teacherUserId))
                .orElseThrow(() -> new PassTypeNotFoundException("No pass type with id " + passTypeId));
        Pass saved = passes.save(Pass.sell(type, studentUserId, paidOn, PaymentMethod.parse(paymentMethod),
                clock.instant()));
        log.info("Pass {} recorded for student {}", saved.id(), studentUserId);
        return PassView.from(saved, 0, today(teacherUserId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PassView> ofTeacher(UUID teacherUserId, UUID studentUserId) {
        return views(passes.findByTeacher(teacherUserId, studentUserId), today(teacherUserId));
    }

    @Override
    @Transactional
    public PassView voidPass(UUID teacherUserId, UUID passId) {
        Pass pass = passes.lockById(passId)
                .filter(found -> found.teacherUserId().equals(teacherUserId))
                .orElseThrow(() -> new PassNotFoundException("No pass with id " + passId));
        int used = usedOf(pass);
        Pass voided = passes.save(pass.voided(used, clock.instant()));
        log.info("Pass {} voided", passId);
        return PassView.from(voided, used, today(teacherUserId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PassView> ofStudent(UUID studentUserId) {
        List<Pass> found = passes.findByStudent(studentUserId);
        if (found.isEmpty()) {
            return List.of();
        }
        return views(found, today(found.getFirst().teacherUserId()));
    }

    @Override
    @Transactional(readOnly = true)
    public PassSales soldBetween(UUID teacherUserId, LocalDate from, LocalDate until) {
        List<Pass> sold = passes.findSold(teacherUserId, from, until);
        return new PassSales(sold.size(), sold.stream().mapToLong(Pass::priceCents).sum());
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public PassCharge charge(UUID teacherUserId, UUID studentUserId, String lessonType, Instant lessonStartsAt) {
        if (!settings.of(teacherUserId).required()) {
            return new PassCharge(false, null);
        }
        List<Pass> locked = passes.lockUsable(teacherUserId, studentUserId, PassLessonType.parse(lessonType));
        return new PassCharge(true, payer(locked, teacherUserId, lessonStartsAt).map(Pass::id).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canPay(UUID teacherUserId, UUID studentUserId, String lessonType, Instant lessonStartsAt) {
        if (!settings.of(teacherUserId).required()) {
            return true;
        }
        List<Pass> usable = passes.findUsable(teacherUserId, studentUserId, PassLessonType.parse(lessonType));
        return payer(usable, teacherUserId, lessonStartsAt).isPresent();
    }

    /** The pass that expires first among those valid on the lesson's day with a lesson left (decision 4). */
    private Optional<Pass> payer(List<Pass> candidates, UUID teacherUserId, Instant lessonStartsAt) {
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        LocalDate lessonDay = lessonStartsAt.atZone(zoneOf(teacherUserId)).toLocalDate();
        Map<UUID, Integer> used = bookings.countConfirmed(candidates.stream().map(Pass::id).toList());
        return candidates.stream()
                .filter(pass -> pass.coversDay(lessonDay))
                .filter(pass -> pass.remaining(used.getOrDefault(pass.id(), 0)) > 0)
                .min(Pass.SPENDING_ORDER);
    }

    private List<PassView> views(List<Pass> found, LocalDate today) {
        if (found.isEmpty()) {
            return List.of();
        }
        Map<UUID, Integer> used = bookings.countConfirmed(found.stream().map(Pass::id).toList());
        return found.stream()
                .map(pass -> PassView.from(pass, used.getOrDefault(pass.id(), 0), today))
                .toList();
    }

    private int usedOf(Pass pass) {
        return bookings.countConfirmed(List.of(pass.id())).getOrDefault(pass.id(), 0);
    }

    private LocalDate today(UUID teacherUserId) {
        return LocalDate.now(clock.withZone(zoneOf(teacherUserId)));
    }

    private ZoneId zoneOf(UUID teacherUserId) {
        return ZoneId.of(teacherProfile.byUserId(teacherUserId).map(TeacherProfileView::timezone).orElseThrow());
    }
}
