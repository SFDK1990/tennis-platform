package com.tennisplatform.architecture;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import java.util.List;

/**
 * What one module may touch of another: its inbound ports, which it may call, and its
 * {@code application.port.spi} interfaces, which it may only implement.
 *
 * <p>The second half arrives with Fase 9. Cancelling a lesson has to cancel its bookings, and
 * reading one has to count them, but {@code booking} depends on {@code lesson} and not the other
 * way round. {@code lesson} therefore declares the interface it needs and {@code booking}
 * implements it: the compile-time edge stays {@code booking -> lesson}, which the graph allows,
 * while the call runs the other way. See 20-fase9-analisis-booking.md.
 *
 * <p>"Implement, not call" is the whole point. A module that could also <em>call</em> another
 * module's spi would be using it as a second, unofficial set of inbound ports - so a dependency
 * on an spi type is accepted only from a class that implements it.
 *
 * <p>It is a separate class, parameterised by root and module list, so that
 * {@code PublicPortsTest} can run it against fixtures and show it refusing what it must refuse.
 * A rule only ever seen passing proves nothing about the cases it exists to catch.
 */
final class PublicPorts {

    static final String INBOUND = ".application.port.in";
    static final String PROVIDED = ".application.port.spi";

    private PublicPorts() {
    }

    static ArchCondition<JavaClass> onlyCrossThroughPorts(String root, List<String> modules, String module) {
        return new ArchCondition<>("reach other modules only through their inbound ports, "
                + "or by implementing their spi interfaces") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    String owner = moduleOf(root, modules, target);
                    if (owner == null || owner.equals(module) || isAllowed(origin, target)) {
                        continue;
                    }
                    events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                }
            }
        };
    }

    private static boolean isAllowed(JavaClass origin, JavaClass target) {
        String targetPackage = target.getPackageName();
        if (targetPackage.contains(INBOUND)) {
            return true;
        }
        return targetPackage.contains(PROVIDED)
                && target.isInterface()
                && origin.isAssignableTo(target.getName());
    }

    /** The module a class belongs to, or null when it is not inside one. */
    static String moduleOf(String root, List<String> modules, JavaClass type) {
        return modules.stream()
                .filter(module -> type.getPackageName().startsWith(root + "." + module + "."))
                .findFirst()
                .orElse(null);
    }
}
