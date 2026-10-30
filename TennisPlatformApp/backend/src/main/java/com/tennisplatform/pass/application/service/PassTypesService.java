package com.tennisplatform.pass.application.service;

import com.tennisplatform.pass.application.port.in.ManagePassTypes;
import com.tennisplatform.pass.application.port.in.PassSettingsView;
import com.tennisplatform.pass.application.port.in.PassTypeView;
import com.tennisplatform.pass.application.port.out.PassSettingsRepository;
import com.tennisplatform.pass.application.port.out.PassTypeRepository;
import com.tennisplatform.pass.application.port.out.PaymentGateway;
import com.tennisplatform.pass.domain.OnlinePaymentUnavailableException;
import com.tennisplatform.pass.domain.PassLessonType;
import com.tennisplatform.pass.domain.PassSettings;
import com.tennisplatform.pass.domain.PassType;
import com.tennisplatform.pass.domain.PassTypeNotFoundException;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class PassTypesService implements ManagePassTypes {

    private static final Logger log = LoggerFactory.getLogger(PassTypesService.class);

    private final PassTypeRepository types;
    private final PassSettingsRepository settings;
    private final GetTeacherProfile teacherProfile;
    private final PaymentGateway gateway;

    public PassTypesService(PassTypeRepository types, PassSettingsRepository settings,
                            GetTeacherProfile teacherProfile, PaymentGateway gateway) {
        this.types = types;
        this.settings = settings;
        this.teacherProfile = teacherProfile;
        this.gateway = gateway;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PassTypeView> ofTheTeacher() {
        return types.findByTeacher(teacherProfile.get().userId()).stream()
                .sorted(Comparator.comparing(PassType::name, String.CASE_INSENSITIVE_ORDER))
                .map(PassTypeView::from)
                .toList();
    }

    @Override
    @Transactional
    public PassTypeView create(UUID teacherUserId, NewPassType type) {
        PassType saved = types.save(new PassType(null, teacherUserId, type.name(),
                PassLessonType.parse(type.lessonType()), type.lessons(), type.priceCents(), type.validityMonths(),
                false));
        log.info("Pass type {} created", saved.id());
        return PassTypeView.from(saved);
    }

    @Override
    @Transactional
    public PassTypeView update(UUID teacherUserId, UUID passTypeId, PassTypeChange change) {
        PassType changed = owned(teacherUserId, passTypeId).changed(change.name(), change.lessonType(),
                change.lessons(), change.priceCents(), change.validityMonths(), change.neverExpires(),
                change.retired());
        return PassTypeView.from(types.save(changed));
    }

    @Override
    @Transactional
    public void delete(UUID teacherUserId, UUID passTypeId) {
        owned(teacherUserId, passTypeId);
        types.delete(passTypeId);
        log.info("Pass type {} deleted", passTypeId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean passesRequired() {
        return settings.of(teacherProfile.get().userId()).required();
    }

    @Override
    @Transactional(readOnly = true)
    public PassSettingsView settings() {
        return view(settings.of(teacherProfile.get().userId()));
    }

    @Override
    @Transactional
    public PassSettingsView changeSettings(UUID teacherUserId, Boolean required, Boolean onlinePayment) {
        if (Boolean.TRUE.equals(onlinePayment) && !gateway.available()) {
            throw new OnlinePaymentUnavailableException("The payment gateway has no keys in this server's environment");
        }
        PassSettings before = settings.of(teacherUserId);
        PassSettings after = before.changed(required, onlinePayment);
        settings.save(teacherUserId, after);
        if (after.required() != before.required()) {
            log.info("Passes {} to book", after.required() ? "required" : "not required");
        }
        if (after.onlinePayment() != before.onlinePayment()) {
            log.info("Online payment of passes switched {}", after.onlinePayment() ? "on" : "off");
        }
        return view(after);
    }

    private PassSettingsView view(PassSettings current) {
        return new PassSettingsView(current.required(), current.onlinePayment(), gateway.available());
    }

    /** Another teacher's type answers like one that does not exist. */
    private PassType owned(UUID teacherUserId, UUID passTypeId) {
        return types.findById(passTypeId)
                .filter(type -> type.teacherUserId().equals(teacherUserId))
                .orElseThrow(() -> new PassTypeNotFoundException("No pass type with id " + passTypeId));
    }
}
