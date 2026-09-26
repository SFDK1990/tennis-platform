package com.tennisplatform.contract;

import com.tennisplatform.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The routes the application serves and the ones {@code openapi.yaml} documents are the same
 * set. {@link ContractValidation} checks the responses a test asks for; this catches the route no
 * test asks for, and the documented one nobody implemented.
 */
class ImplementedRoutesMatchTheContractTest extends AbstractIntegrationTest {

    private static final String API = "/api/v1";

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mappings;

    @Test
    void everyRouteOfTheApiIsDocumentedAndEveryDocumentedRouteExists() {
        assertThat(implemented()).containsExactlyInAnyOrderElementsOf(documented());
    }

    private Set<String> implemented() {
        Set<String> routes = new TreeSet<>();
        for (RequestMappingInfo mapping : mappings.getHandlerMethods().keySet()) {
            for (String pattern : mapping.getPatternValues()) {
                if (!pattern.startsWith(API)) {
                    continue;
                }
                // A mapping without a method answers all of them, and no route here should.
                assertThat(mapping.getMethodsCondition().getMethods())
                        .as("HTTP method of %s", pattern).isNotEmpty();
                mapping.getMethodsCondition().getMethods().forEach(method ->
                        routes.add(method + " " + withoutParameterNames(pattern.substring(API.length()))));
            }
        }
        return routes;
    }

    private static Set<String> documented() {
        Set<String> routes = new TreeSet<>();
        ContractValidation.specification().getPaths().forEach((path, item) ->
                item.readOperationsMap().keySet().forEach(method ->
                        routes.add(method + " " + withoutParameterNames(path))));
        return routes;
    }

    /** {@code {id}} in the spec and {@code {lessonId}} in a controller are the same route. */
    private static String withoutParameterNames(String path) {
        return path.replaceAll("\\{[^}]+}", "{}");
    }
}
