package com.tennisplatform.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * The module boundaries of 02-arquitectura.md, as executable rules.
 *
 * <p>They arrive in Fase 6 because until then there was a single module and nothing to keep
 * apart. Two of them already caught real violations while the teacher module was being written:
 * the bootstrap wanted to live in {@code identity} and write into the teacher's table, and
 * {@code AuthenticatedUser} sat among identity's adapters where the first controller of another
 * module could only have reached it by importing an adapter.
 *
 * <p>The rules cover all nine modules, including the six that are still empty. The graph is
 * already decided and does not depend on the code existing: every module then starts its life
 * under the rules, instead of the rules having to negotiate with code that already breaks them.
 *
 * <p>{@code config}, {@code error} and {@code web} are not modules and stay outside the graph by
 * decision: {@code config} is the composition root, {@code error} the global error mapping and
 * {@code web} the correlation-id filter. They are exempt simply by not being listed in
 * {@link #MODULES} - any module may use them, and the composition root may see any module, since
 * assembling concrete implementations is precisely its job. Folding them into {@code shared}
 * instead would blur what shared means: technical primitives, not application wiring.
 */
@AnalyzeClasses(packages = "com.tennisplatform", importOptions = ImportOption.DoNotIncludeTests.class)
class ModuleBoundariesTest {

    private static final String ROOT = "com.tennisplatform";

    private static final List<String> MODULES = List.of(
            "identity", "teacher", "student", "availability", "lesson", "booking",
            "administration", "calendar", "platform", "shared");

    // --- A. The dependency graph -----------------------------------------------

    @ArchTest
    static final ArchRule identityDependsOnNoModule = mayOnlyDependOn("identity");

    @ArchTest
    static final ArchRule sharedDependsOnNoModule = mayOnlyDependOn("shared");

    /**
     * {@code platform} owns the global configuration of the installation and depends on nobody,
     * which is what lets every other module read it.
     *
     * <p>02-arquitectura.md filed {@code platform_configuration} under {@code administration}.
     * That was a mistake about ownership, not about cycles: {@code student} has to read the
     * student limit, {@code student} may not depend on {@code administration}, and no amount of
     * reordering fixes a table living in the module that merely edits it. The read side arrives
     * in Fase 6 with the student module that needs it; the console stays Fase 7.
     */
    @ArchTest
    static final ArchRule platformDependsOnNoModule = mayOnlyDependOn("platform");

    @ArchTest
    static final ArchRule teacherDependsOnIdentity = mayOnlyDependOn("teacher", "identity");

    @ArchTest
    static final ArchRule studentDependsOnIdentityTeacherAndPlatform =
            mayOnlyDependOn("student", "identity", "teacher", "platform");

    /**
     * 02-arquitectura.md lists only {@code availability -> teacher}, and that turns out to be
     * incomplete for every module that has a web adapter: a controller has to know who is
     * calling, and that is {@code identity}'s {@code AuthenticatedUser}. {@code teacher} and
     * {@code student} already depend on identity for exactly this reason; the graph in the
     * document simply never said so, because those two had other reasons to depend on it.
     *
     * <p>The edge is added rather than worked around - putting the controller in {@code web},
     * or copying the role check, would hide the dependency instead of removing it. The same
     * will apply to {@code lesson} and {@code booking} when they arrive, and saying it here is
     * what keeps that from looking like a new concession each time.
     *
     * <p>It is still widened one module at a time, and deliberately so. A general rule - every
     * module may depend on {@code identity} - would quietly punch a hole through
     * {@code platformDependsOnNoModule} and {@code sharedDependsOnNoModule}, whose whole job is
     * to forbid exactly that so everybody else can read them without a cycle. It would also
     * pre-authorise an edge for four modules whose own phase has not been analysed yet. The
     * repetition is the point: each module states the graph it was designed with, and a new one
     * is denied by default.
     */
    @ArchTest
    static final ArchRule availabilityDependsOnTeacherAndIdentity =
            mayOnlyDependOn("availability", "teacher", "identity");

    /**
     * 02-arquitectura.md lists {@code lesson -> teacher, availability}, and Fase 8 adds two more
     * edges that the document did not foresee rather than contradicting it.
     *
     * <p>{@code identity} for the reason already written above for {@code availability}: a web
     * adapter has to know who is calling. {@code platform} because the cap on how large a group
     * lesson may be is configuration, and configuration is what that module owns - the same way
     * {@code student} reads the student limit from it. {@code platform} depends on nothing by
     * design, precisely so that anyone may read it without creating a cycle.
     */
    @ArchTest
    static final ArchRule lessonDependsOnTeacherAvailabilityIdentityAndPlatform =
            mayOnlyDependOn("lesson", "teacher", "availability", "identity", "platform");

    @ArchTest
    static final ArchRule bookingDependsOnStudentAndLesson =
            mayOnlyDependOn("booking", "student", "lesson");

    @ArchTest
    static final ArchRule administrationDependsOnIdentityTeacherAndStudent =
            mayOnlyDependOn("administration", "identity", "teacher", "student");

    /**
     * A cycle between modules means they can no longer be understood, tested or replaced
     * separately - which is the only thing a modular monolith buys over a single package.
     */
    @ArchTest
    static final ArchRule modulesAreFreeOfCycles = slices()
            .matching(ROOT + ".(*)..")
            .should().beFreeOfCycles();

    // --- B. Hexagonal layers inside each module --------------------------------

    /**
     * The domain is where the business rules live, and it must be testable without a framework
     * and survive changing one. Persistence annotations are the usual way this leaks: an
     * {@code @Entity} in the domain marries the rules to JPA for good.
     */
    @ArchTest
    static final ArchRule theDomainDependsOnNoFramework = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta.persistence..", "jakarta.validation..",
                    "org.hibernate..")
            .because("the domain must be testable and survive replacing the framework");

    /** The innermost layer knows nothing about the ones that use it. */
    @ArchTest
    static final ArchRule theDomainDependsOnNoOuterLayer = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..application..", "..adapters..", "..configuration..")
            .because("the domain is the innermost layer");

    /**
     * The application layer defines the ports; the adapters implement them. If the layer that
     * declares the contract also reaches for the implementation, the dependency inversion the
     * whole hexagon rests on has been undone.
     *
     * <p>Spring itself is <em>not</em> banned here, on purpose: 02-arquitectura.md places the
     * transaction boundaries in {@code application/service}, so {@code @Transactional} belongs
     * there. A rule forbidding it would fail on day one and end up relaxed with silent
     * exclusions, which is worse than saying out loud what is allowed.
     */
    @ArchTest
    static final ArchRule theApplicationLayerDoesNotUseAdapters = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..adapters..")
            .because("ports are implemented by adapters, not the other way round");

    // --- C. Cross-module access goes through inbound ports ---------------------

    /**
     * What one module may see of another: {@code application/port/in}, and - since Fase 9 - the
     * interfaces in {@code application/port/spi}, which it may implement but never call. Its
     * domain, its services, its outbound ports, its adapters and its wiring are the inside of
     * that module, and reaching any of them turns two modules into one, silently. The spi half,
     * and why it is safe, is explained in {@link PublicPorts}.
     *
     * <p>One rule per module because the check needs both ends: a module reaching into its own
     * internals is not a violation, it is the point of having internals.
     */
    @ArchTest
    static final ArchRule identityCrossesOnlyThroughPorts = crossesOnlyThroughInboundPorts("identity");

    @ArchTest
    static final ArchRule teacherCrossesOnlyThroughPorts = crossesOnlyThroughInboundPorts("teacher");

    @ArchTest
    static final ArchRule studentCrossesOnlyThroughPorts = crossesOnlyThroughInboundPorts("student");

    @ArchTest
    static final ArchRule availabilityCrossesOnlyThroughPorts =
            crossesOnlyThroughInboundPorts("availability");

    @ArchTest
    static final ArchRule lessonCrossesOnlyThroughPorts = crossesOnlyThroughInboundPorts("lesson");

    @ArchTest
    static final ArchRule bookingCrossesOnlyThroughPorts = crossesOnlyThroughInboundPorts("booking");

    @ArchTest
    static final ArchRule administrationCrossesOnlyThroughPorts =
            crossesOnlyThroughInboundPorts("administration");

    @ArchTest
    static final ArchRule calendarCrossesOnlyThroughPorts = crossesOnlyThroughInboundPorts("calendar");

    @ArchTest
    static final ArchRule platformCrossesOnlyThroughPorts = crossesOnlyThroughInboundPorts("platform");

    // --- D. calendar only reads -------------------------------------------------

    /**
     * {@code calendar} aggregates availability, lessons and bookings, and must never write into
     * another module's domain. "Public query interfaces" is not checkable as prose, so the
     * agreed mechanical criterion is the port's name: it may use {@code Get}, {@code Find} and
     * {@code Query} ports, and no other. Data types returned by those ports - views and records -
     * are not interfaces and stay out of the rule.
     */
    @ArchTest
    static final ArchRule calendarOnlyUsesQueryPorts = noClasses()
            .that().resideInAPackage(packageOf("calendar"))
            .should().dependOnClassesThat(writePortsOfAnotherModule())
            .because("calendar reads other modules, it never changes them");

    // --- E. The web edge is not a back door ------------------------------------

    /**
     * {@code web} is not a module and is therefore exempt from the graph, which is what lets
     * {@code /me} compose an answer out of {@code identity}, {@code teacher} and
     * {@code student} at once - something no single module may do. An exemption with nothing
     * holding it is an invitation, so it gets its own rule: the edge may call inbound ports and
     * nothing else. No domain types, no services, no adapters, no entities.
     *
     * <p>This is what keeps "orchestrator" from drifting into "the place where the boundaries
     * do not apply". It also shows up in the design of the ports themselves: it is why
     * {@code UserSummary} carries the role as a string rather than identity's {@code Role}.
     */
    @ArchTest
    static final ArchRule theWebEdgeOnlyUsesInboundPorts = noClasses()
            .that().resideInAPackage(ROOT + ".web..")
            .should().dependOnClassesThat(insidesOfAnyModule())
            .because("the composition edge may only call a module's inbound ports");

    // --- helpers ---------------------------------------------------------------

    private static String packageOf(String module) {
        return ROOT + "." + module + "..";
    }

    /**
     * Builds the rule for one module out of the graph: everything not explicitly allowed is
     * forbidden. Stating the permitted edges rather than the forbidden ones is what keeps the
     * rules correct when a module is added - a new module is denied by default.
     */
    private static ArchRule mayOnlyDependOn(String module, String... allowed) {
        Set<String> permitted = Stream.concat(Stream.of(module), Arrays.stream(allowed))
                .collect(Collectors.toSet());
        String[] forbidden = MODULES.stream()
                .filter(other -> !permitted.contains(other))
                .map(ModuleBoundariesTest::packageOf)
                .toArray(String[]::new);

        return noClasses()
                .that().resideInAPackage(packageOf(module))
                .should().dependOnClassesThat().resideInAnyPackage(forbidden)
                .because(module + " may only depend on " + String.join(", ", permitted)
                        + " (02-arquitectura.md)");
    }

    private static ArchRule crossesOnlyThroughInboundPorts(String module) {
        return classes()
                .that().resideInAPackage(packageOf(module))
                .should(PublicPorts.onlyCrossThroughPorts(ROOT, MODULES, module))
                .because("a module's public API is its inbound ports, plus the spi interfaces it "
                        + "asks others to implement - see PublicPorts");
    }

    private static DescribedPredicate<JavaClass> insidesOfAnyModule() {
        return new DescribedPredicate<>("the insides of any module") {
            @Override
            public boolean test(JavaClass target) {
                return moduleOf(target) != null
                        && !target.getPackageName().contains(".application.port.in");
            }
        };
    }

    private static DescribedPredicate<JavaClass> writePortsOfAnotherModule() {
        return new DescribedPredicate<>("a write port of another module") {
            @Override
            public boolean test(JavaClass target) {
                String owner = moduleOf(target);
                return owner != null
                        && !"calendar".equals(owner)
                        && target.getPackageName().contains(".application.port.in")
                        && target.isInterface()
                        && !target.getSimpleName().matches("^(Get|Find|Query).*");
            }
        };
    }

    /** The module a class belongs to, or null when it is not inside one. */
    private static String moduleOf(JavaClass type) {
        return PublicPorts.moduleOf(ROOT, MODULES, type);
    }
}
