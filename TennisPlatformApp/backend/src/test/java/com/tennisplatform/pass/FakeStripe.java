package com.tennisplatform.pass;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * A local server that answers {@code POST /v1/checkout/sessions} the way Stripe does, so the real
 * adapter is exercised without the suite ever talking to Stripe (42-fase27.5, Tests). It keeps
 * what the last request carried, and can be told to fail.
 */
final class FakeStripe {

    static final String SECRET_KEY = "sk_test_fake";
    static final String WEBHOOK_SECRET = "whsec_fake";

    private static final HttpServer SERVER;
    private static final AtomicInteger SESSIONS = new AtomicInteger();

    private static volatile int status = 200;
    private static volatile Map<String, String> lastForm = Map.of();
    private static volatile String lastAuthorization;
    private static volatile String lastIdempotencyKey;

    static {
        try {
            SERVER = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        SERVER.createContext("/v1/checkout/sessions", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            lastForm = Arrays.stream(body.split("&"))
                    .map(pair -> pair.split("=", 2))
                    .collect(Collectors.toMap(pair -> decode(pair[0]), pair -> pair.length > 1 ? decode(pair[1]) : "",
                            (first, second) -> second, LinkedHashMap::new));
            lastAuthorization = exchange.getRequestHeaders().getFirst("Authorization");
            lastIdempotencyKey = exchange.getRequestHeaders().getFirst("Idempotency-Key");
            String id = "cs_test_" + SESSIONS.incrementAndGet();
            byte[] answer = (status == 200
                    ? "{\"id\":\"" + id + "\",\"object\":\"checkout.session\",\"url\":\"https://checkout.stripe.com/c/pay/" + id + "\"}"
                    : "{\"error\":{\"type\":\"api_error\"}}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, answer.length);
            exchange.getResponseBody().write(answer);
            exchange.close();
        });
        SERVER.start();
    }

    private FakeStripe() {
    }

    static String baseUrl() {
        return "http://127.0.0.1:" + SERVER.getAddress().getPort();
    }

    static void answerWith(int newStatus) {
        status = newStatus;
    }

    static Map<String, String> lastForm() {
        return lastForm;
    }

    static String lastAuthorization() {
        return lastAuthorization;
    }

    static String lastIdempotencyKey() {
        return lastIdempotencyKey;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
