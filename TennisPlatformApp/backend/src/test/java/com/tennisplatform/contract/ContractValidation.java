package com.tennisplatform.contract;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Fails the test whose request got a response that {@code openapi.yaml} does not describe: an
 * undocumented status, a field the spec does not declare, a missing required one, a wrong type.
 *
 * <p>Only responses are checked. Many tests send malformed requests on purpose, and what the
 * contract promises about those is the response they get.
 *
 * <p>Installed on every integration test's client by {@code AbstractIntegrationTest}, so the
 * whole suite doubles as a contract check (24-fase13-analisis-revision-api.md). Before Fase 13
 * the spec was reviewed by hand, and a run with this in place found five differences nobody had
 * noticed.
 */
public final class ContractValidation implements ClientHttpRequestInterceptor {

    private static final String SPEC = Path.of("..", "openapi.yaml").toAbsolutePath().toUri().toString();
    private static final String API = "/api/v1";

    /** Called on purpose to show that an unknown route is closed too; it cannot be in the spec. */
    private static final Set<String> OUTSIDE_THE_CONTRACT = Set.of("/api/v1/anything");

    /**
     * Protocol errors: a method the route does not have, a body or an Accept it does not speak.
     * They can happen on any operation, so the spec does not list them one by one; what they
     * must still follow is the error format (11-contrato-api.md). 406 has no body by design.
     */
    private static final Set<Integer> PROTOCOL_ERRORS = Set.of(405, 406, 415);

    private static final OpenApiInteractionValidator VALIDATOR = OpenApiInteractionValidator
            .createForSpecificationUrl(SPEC)
            .withBasePathOverride(API)
            // Merges each allOf into one schema. Without it every part is checked on its own and,
            // since the validator rejects undeclared properties, each part rejects the other's.
            .withResolveCombinators(true)
            .build();

    private ContractValidation() {
    }

    /** The parsed contract, for the tests that check it without making requests. */
    public static OpenAPI specification() {
        return new OpenAPIV3Parser().read(SPEC);
    }

    /**
     * Idempotent: the client is one bean shared by every test class of the context, and each
     * test would otherwise add one more interceptor.
     */
    public static void installOn(RestTemplate client) {
        if (client.getInterceptors().stream().noneMatch(ContractValidation.class::isInstance)) {
            client.getInterceptors().add(new ContractValidation());
        }
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        ClientHttpResponse response = execution.execute(request, body);
        String path = request.getURI().getRawPath();
        if (!path.startsWith(API) || OUTSIDE_THE_CONTRACT.contains(path)) {
            return response;
        }

        byte[] bytes = StreamUtils.copyToByteArray(response.getBody());
        String responseBody = new String(bytes, StandardCharsets.UTF_8);
        int status = response.getStatusCode().value();
        if (PROTOCOL_ERRORS.contains(status)) {
            if (status != 406 && !responseBody.contains("\"code\"")) {
                throw new AssertionError("%s %s answered %d without the error contract: %s"
                        .formatted(request.getMethod(), path, status, responseBody));
            }
            return new ReadResponse(response, bytes);
        }
        SimpleResponse.Builder wire = SimpleResponse.Builder.status(response.getStatusCode().value())
                .withBody(responseBody);
        response.getHeaders().forEach(wire::withHeader);
        ValidationReport report = VALIDATOR.validateResponse(path,
                Request.Method.valueOf(request.getMethod().name()), wire.build());
        if (report.hasErrors()) {
            throw new AssertionError("%s %s answered %d, which openapi.yaml does not describe:%n%s%nBody: %s"
                    .formatted(request.getMethod(), path, response.getStatusCode().value(),
                            report.getMessages().stream()
                                    .map(message -> "  - " + message.getMessage())
                                    .collect(Collectors.joining(System.lineSeparator())),
                            responseBody));
        }
        return new ReadResponse(response, bytes);
    }

    /**
     * The response with its body already read, so the test can read it again. Buffering in the
     * request factory would do the same, but a test that installs its own factory, as
     * {@code AuthRateLimitTest} does, would silently lose it.
     */
    private record ReadResponse(ClientHttpResponse original, byte[] body) implements ClientHttpResponse {

        @Override
        public HttpStatusCode getStatusCode() throws IOException {
            return original.getStatusCode();
        }

        @Override
        public String getStatusText() throws IOException {
            return original.getStatusText();
        }

        @Override
        public HttpHeaders getHeaders() {
            return original.getHeaders();
        }

        @Override
        public InputStream getBody() {
            return new ByteArrayInputStream(body);
        }

        @Override
        public void close() {
            original.close();
        }
    }
}
