package com.tennisplatform.error;

import com.tennisplatform.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A request the API cannot serve is the client's mistake, and says which one. These all used
 * to answer 500 with an ERROR trace in the log (26-fase15-analisis-seguridad.md).
 */
@ExtendWith(OutputCaptureExtension.class)
class ProtocolErrorsTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "protocol-password";

    @Test
    @SuppressWarnings("rawtypes")
    void aRouteThatDoesNotExistIsNotFound(CapturedOutput log) {
        ResponseEntity<Map> response = rest.exchange("/api/v1/anything", HttpMethod.GET,
                new HttpEntity<>(bearer(tokenOfANewStudent(PASSWORD))), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("code", "NOT_FOUND");
        assertThat(log).doesNotContain("Unhandled exception");
    }

    @Test
    @SuppressWarnings("rawtypes")
    void aMethodTheRouteDoesNotHaveIsNotAllowedAndTheAnswerSaysWhichAre(CapturedOutput log) {
        ResponseEntity<Map> response = rest.exchange("/api/v1/me", HttpMethod.DELETE,
                new HttpEntity<>(bearer(tokenOfANewStudent(PASSWORD))), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).containsEntry("code", "METHOD_NOT_ALLOWED");
        assertThat(response.getHeaders().getAllow()).contains(HttpMethod.GET, HttpMethod.PATCH);
        assertThat(log).doesNotContain("Unhandled exception");
    }

    @Test
    @SuppressWarnings("rawtypes")
    void aBodyThatIsNotJsonIsAnUnsupportedMediaType(CapturedOutput log) {
        HttpHeaders headers = bearer(tokenOfANewStudent(PASSWORD));
        headers.setContentType(MediaType.TEXT_PLAIN);

        ResponseEntity<Map> response = rest.exchange("/api/v1/me", HttpMethod.PATCH,
                new HttpEntity<>("fullName=Ana", headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(response.getBody()).containsEntry("code", "UNSUPPORTED_MEDIA_TYPE");
        assertThat(log).doesNotContain("Unhandled exception");
    }

    @Test
    void aClientThatCannotReadJsonIsNotAcceptable(CapturedOutput log) {
        HttpHeaders headers = bearer(tokenOfANewStudent(PASSWORD));
        headers.setAccept(List.of(MediaType.TEXT_HTML));

        ResponseEntity<String> response = rest.exchange("/api/v1/me", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_ACCEPTABLE);
        assertThat(log).doesNotContain("Unhandled exception");
    }
}
