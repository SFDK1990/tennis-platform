package com.tennisplatform.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The cross-module rule run against fixtures, so it is seen refusing as well as accepting.
 *
 * <p>{@code ModuleBoundariesTest} only ever shows the rule passing on the real code, which says
 * nothing about whether it would catch the cases it exists for. Two fixture modules, alpha and
 * beta, stand in for {@code lesson} and {@code booking}; they live under test sources, which the
 * real rules do not import.
 */
class PublicPortsTest {

    private static final String ROOT = "com.tennisplatform.architecture.fixture";
    private static final JavaClasses FIXTURES = new ClassFileImporter().importPackages(ROOT);

    @Test
    void aModuleMayImplementAnInterfaceAnotherModuleDeclaresForIt() {
        assertThat(violates("ImplementsAlphaNeeds")).isFalse();
    }

    @Test
    void aModuleMayNotCallAnInterfaceAnotherModuleDeclaresForOthersToImplement() {
        assertThat(violates("CallsAlphaNeeds")).isTrue();
    }

    @Test
    void aModuleMayStillCallAnotherModulesInboundPorts() {
        assertThat(violates("CallsAlphaOffers")).isFalse();
    }

    @Test
    void aModuleMayStillNotReachAnotherModulesInternals() {
        assertThat(violates("ReachesAlphaInternals")).isTrue();
    }

    private static boolean violates(String simpleName) {
        return classes().that().haveSimpleName(simpleName)
                .should(PublicPorts.onlyCrossThroughPorts(ROOT, List.of("alpha", "beta"), "beta"))
                .evaluate(FIXTURES)
                .hasViolation();
    }
}
