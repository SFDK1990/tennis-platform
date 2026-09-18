package com.tennisplatform.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * The module boundaries of 02-arquitectura.md, as executable rules.
 *
 * <p>They arrive in Fase 6 because until then there was a single module and nothing to keep
 * apart. Two rules here already caught real violations while the module was being written: the
 * teacher bootstrap wanted to live in {@code identity} and write into the teacher's table, and
 * {@code AuthenticatedUser} sat among identity's adapters where the first controller of another
 * module could only have reached it by importing an adapter.
 */
@AnalyzeClasses(packages = "com.tennisplatform", importOptions = ImportOption.DoNotIncludeTests.class)
class ModuleBoundariesTest {

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

    /**
     * The application layer defines the ports; the adapters implement them. If the layer that
     * declares the contract also reaches for the implementation, the dependency inversion that
     * the whole hexagon rests on has been undone.
     */
    @ArchTest
    static final ArchRule theApplicationLayerDoesNotUseAdapters = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..adapters..")
            .because("ports are implemented by adapters, not the other way round");

    /**
     * Cross-module access goes through public ports. An adapter is the inside of a module: its
     * JPA entities, its repositories, its controllers. Reaching one from outside turns two
     * modules into one, silently.
     *
     * <p>{@code com.tennisplatform.config} is exempt, and it is the only exemption. It is the
     * composition root: the one place whose entire job is to assemble concrete implementations,
     * which is why {@code SecurityConfig} builds the filter chain out of identity's JWT filter.
     * Forbidding it there would not improve the design, it would only move the wiring somewhere
     * less obvious. The rule protects the modules from each other, not the root from the modules.
     */
    @ArchTest
    static final ArchRule noModuleReachesIntoIdentityAdapters = noClasses()
            .that().resideOutsideOfPackages("com.tennisplatform.identity..",
                    "com.tennisplatform.config..")
            .should().dependOnClassesThat().resideInAPackage("com.tennisplatform.identity.adapters..")
            .because("other modules must use identity's public ports");

    @ArchTest
    static final ArchRule noModuleReachesIntoTeacherAdapters = noClasses()
            .that().resideOutsideOfPackages("com.tennisplatform.teacher..",
                    "com.tennisplatform.config..")
            .should().dependOnClassesThat().resideInAPackage("com.tennisplatform.teacher.adapters..")
            .because("other modules must use teacher's public ports");

    /**
     * The direction of the graph: identity is the root and depends on nobody. It is the rule the
     * teacher bootstrap broke, which is why the bootstrap moved to the teacher module and asks
     * identity for the account through a port.
     */
    @ArchTest
    static final ArchRule identityDependsOnNoOtherModule = noClasses()
            .that().resideInAPackage("com.tennisplatform.identity..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.tennisplatform.teacher..", "com.tennisplatform.student..",
                    "com.tennisplatform.availability..", "com.tennisplatform.lesson..",
                    "com.tennisplatform.booking..", "com.tennisplatform.administration..",
                    "com.tennisplatform.calendar..")
            .because("identity is the root of the dependency graph");

    /** teacher may know identity and shared, and nothing else (02-arquitectura.md). */
    @ArchTest
    static final ArchRule teacherOnlyDependsOnIdentity = noClasses()
            .that().resideInAPackage("com.tennisplatform.teacher..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.tennisplatform.student..", "com.tennisplatform.availability..",
                    "com.tennisplatform.lesson..", "com.tennisplatform.booking..",
                    "com.tennisplatform.administration..", "com.tennisplatform.calendar..")
            .because("teacher depends on identity only");

    /**
     * A cycle between modules means they can no longer be understood, tested or replaced
     * separately - which is the only thing a modular monolith buys over a single package.
     */
    @ArchTest
    static final ArchRule modulesAreFreeOfCycles = slices()
            .matching("com.tennisplatform.(*)..")
            .should().beFreeOfCycles();
}
