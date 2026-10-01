package com.tennisplatform.contract;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The answers any operation can give, whatever it does, are documented on every one of them
 * (11-contrato-api.md). They are exactly the ones a test rarely provokes, so
 * {@link ContractValidation} alone would let them go missing: before Fase 13 only 8 of the
 * operations that require a token said they could answer 401.
 */
class ContractConventionsTest {

    private final OpenAPI spec = ContractValidation.specification();

    @Test
    void everyOperationThatNeedsATokenDocumentsUnauthorized() {
        assertThat(operationsMissing("401", this::needsABearerToken)).isEmpty();
    }

    /**
     * {@code AuthRateLimitFilter} throttles the whole prefix, logout and refresh included, and the
     * one route outside it that checks a password.
     */
    @Test
    void everyAuthenticationOperationDocumentsTheRateLimit() {
        assertThat(operationsMissing("429", (path, item, operation) -> path.startsWith("/auth/")
                || path.equals("/me/password"))).isEmpty();
    }

    /** A malformed body, id or query parameter is a 400, never a 500. */
    @Test
    void everyOperationThatTakesInputDocumentsBadRequest() {
        assertThat(operationsMissing("400", (path, item, operation) -> operation.getRequestBody() != null
                || operation.getParameters() != null && !operation.getParameters().isEmpty()
                || item.getParameters() != null && !item.getParameters().isEmpty())).isEmpty();
    }

    private boolean needsABearerToken(String path, PathItem item, Operation operation) {
        List<SecurityRequirement> security = operation.getSecurity() != null ? operation.getSecurity() : spec.getSecurity();
        return security.stream().anyMatch(requirement -> requirement.containsKey("bearerAuth"));
    }

    private List<String> operationsMissing(String status, Applies rule) {
        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, PathItem> path : spec.getPaths().entrySet()) {
            path.getValue().readOperationsMap().forEach((method, operation) -> {
                if (rule.to(path.getKey(), path.getValue(), operation)
                        && !operation.getResponses().containsKey(status)) {
                    missing.add(method + " " + path.getKey());
                }
            });
        }
        return missing;
    }

    @FunctionalInterface
    private interface Applies {
        boolean to(String path, PathItem item, Operation operation);
    }
}
