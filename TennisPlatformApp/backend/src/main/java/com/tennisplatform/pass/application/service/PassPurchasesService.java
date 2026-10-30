package com.tennisplatform.pass.application.service;

import com.tennisplatform.pass.application.port.in.BuyPasses;
import com.tennisplatform.pass.application.port.in.ConfirmPayments;
import com.tennisplatform.pass.application.port.in.PurchaseView;
import com.tennisplatform.pass.application.port.out.PassPurchaseRepository;
import com.tennisplatform.pass.application.port.out.PassRepository;
import com.tennisplatform.pass.application.port.out.PassSettingsRepository;
import com.tennisplatform.pass.application.port.out.PassTypeRepository;
import com.tennisplatform.pass.application.port.out.PaymentGateway;
import com.tennisplatform.pass.application.port.out.PaymentGateway.Checkout;
import com.tennisplatform.pass.application.port.out.PaymentGateway.CheckoutRequest;
import com.tennisplatform.pass.domain.OnlinePaymentDisabledException;
import com.tennisplatform.pass.domain.OnlinePaymentUnavailableException;
import com.tennisplatform.pass.domain.Pass;
import com.tennisplatform.pass.domain.PassPurchase;
import com.tennisplatform.pass.domain.PassType;
import com.tennisplatform.pass.domain.PassTypeNotFoundException;
import com.tennisplatform.pass.domain.PurchaseNotFoundException;
import com.tennisplatform.shared.domain.ForbiddenOperationException;
import com.tennisplatform.student.application.port.in.QueryManagedStudent;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import com.tennisplatform.teacher.application.port.in.TeacherProfileView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Selling passes in the app (42-fase27.5-analisis-pago-online.md). The pass is born when the
 * gateway's signed notice says the money arrived, never when the student comes back to the app:
 * they may close the tab before, and the way back can be forged (decision 3).
 */
public class PassPurchasesService implements BuyPasses, ConfirmPayments {

    private static final Logger log = LoggerFactory.getLogger(PassPurchasesService.class);

    /** How long the gateway's page can be paid. Stripe asks for at least 30 minutes. */
    static final Duration CHECKOUT_LIFETIME = Duration.ofHours(1);

    private final PassPurchaseRepository purchases;
    private final PassRepository passes;
    private final PassTypeRepository types;
    private final PassSettingsRepository settings;
    private final PaymentGateway gateway;
    private final QueryManagedStudent managedStudents;
    private final GetTeacherProfile teacherProfile;
    private final Clock clock;

    public PassPurchasesService(PassPurchaseRepository purchases, PassRepository passes, PassTypeRepository types,
                                PassSettingsRepository settings, PaymentGateway gateway,
                                QueryManagedStudent managedStudents, GetTeacherProfile teacherProfile, Clock clock) {
        this.purchases = purchases;
        this.passes = passes;
        this.types = types;
        this.settings = settings;
        this.gateway = gateway;
        this.managedStudents = managedStudents;
        this.teacherProfile = teacherProfile;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PurchaseView start(UUID studentUserId, boolean emailVerified, UUID passTypeId) {
        if (!emailVerified) {
            throw new ForbiddenOperationException("EMAIL_NOT_VERIFIED", "Verify your email address before buying a pass");
        }
        PassType type = types.findById(passTypeId)
                .orElseThrow(() -> new PassTypeNotFoundException("No pass type with id " + passTypeId));
        if (!managedStudents.isManagedBy(type.teacherUserId(), studentUserId)) {
            throw new ForbiddenOperationException("STUDENT_NOT_MANAGED", "The teacher of this pass does not manage you");
        }
        if (!settings.of(type.teacherUserId()).onlinePayment()) {
            throw new OnlinePaymentDisabledException("The teacher does not sell passes in the app");
        }
        if (!gateway.available()) {
            throw new OnlinePaymentUnavailableException("The payment gateway has no keys in this server's environment");
        }
        Instant now = clock.instant();
        PassPurchase started = purchases.save(PassPurchase.start(type, studentUserId, now, now.plus(CHECKOUT_LIFETIME)));
        Checkout checkout = gateway.open(new CheckoutRequest(started.id(), started.name(), started.priceCents(),
                PassPurchase.CURRENCY, started.expiresAt()));
        PassPurchase opened = purchases.save(started.withCheckout(checkout.sessionId(), checkout.url()));
        log.info("Purchase {} of pass type {} started", opened.id(), passTypeId);
        return PurchaseView.from(opened);
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseView ofStudent(UUID studentUserId, UUID purchaseId) {
        return purchases.findById(purchaseId)
                .filter(found -> found.studentUserId().equals(studentUserId))
                .map(PurchaseView::from)
                .orElseThrow(() -> new PurchaseNotFoundException("No purchase with id " + purchaseId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseView> allOf(UUID studentUserId) {
        return purchases.findByStudent(studentUserId).stream().map(PurchaseView::from).toList();
    }

    @Override
    @Transactional
    public void paid(String checkoutSessionId, long amountCents, String currency) {
        Optional<PassPurchase> found = purchases.lockByCheckoutSessionId(checkoutSessionId);
        if (found.isEmpty()) {
            log.warn("Payment notice for a checkout session the app does not know; ignored");
            return;
        }
        PassPurchase purchase = found.get();
        if (purchase.isPaid()) {
            log.info("Purchase {} was already paid; repeated notice ignored", purchase.id());
            return;
        }
        if (!purchase.charged(amountCents, currency)) {
            log.error("Purchase {} was charged {} {} instead of {} {}; no pass created", purchase.id(), amountCents,
                    currency, purchase.priceCents(), PassPurchase.CURRENCY);
            return;
        }
        Instant now = clock.instant();
        LocalDate paidOn = LocalDate.ofInstant(now, zoneOf(purchase.teacherUserId()));
        Pass pass = passes.save(Pass.boughtOnline(purchase, paidOn, now));
        purchases.save(purchase.paid(pass.id(), now));
        log.info("Purchase {} paid: pass {} for student {}", purchase.id(), pass.id(), purchase.studentUserId());
    }

    @Override
    @Transactional
    public void expired(String checkoutSessionId) {
        purchases.lockByCheckoutSessionId(checkoutSessionId).ifPresent(purchase -> {
            PassPurchase expired = purchase.expired();
            if (expired != purchase) {
                purchases.save(expired);
                log.info("Purchase {} expired without payment", purchase.id());
            }
        });
    }

    private ZoneId zoneOf(UUID teacherUserId) {
        return ZoneId.of(teacherProfile.byUserId(teacherUserId).map(TeacherProfileView::timezone).orElseThrow());
    }
}
