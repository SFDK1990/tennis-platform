import createClient from "openapi-fetch";
import type { paths } from "@/shared/api/schema";
import { getSession, refreshAccessToken } from "@/shared/api/session";

/** The only way into the API. Paths, parameters and bodies are checked against openapi.yaml. */
export const api = createClient<paths>({ baseUrl: "/api/v1", fetch: authenticatedFetch });

/** Their 401 is a wrong password or a dead session, not an access token to renew. */
const NOT_RENEWABLE = new Set(["/api/v1/auth/login", "/api/v1/auth/refresh"]);

/** The header the backend puts on every log line of the request (28-fase16-analisis-observabilidad.md). */
export const CORRELATION_HEADER = "X-Correlation-Id";

/**
 * Adds the access token and a correlation id and, on a 401, refreshes the token once and repeats
 * the request. The repetition keeps the id: it is the same action.
 */
export async function authenticatedFetch(request: Request): Promise<Response> {
  request.headers.set(CORRELATION_HEADER, newCorrelationId());
  const retry = request.clone();
  const response = await fetch(withToken(request));
  if (response.status !== 401 || NOT_RENEWABLE.has(new URL(request.url).pathname)) {
    return response;
  }
  if (!(await refreshAccessToken())) {
    return response;
  }
  return fetch(withToken(retry));
}

/**
 * Not crypto.randomUUID: the browser only has it over HTTPS or on localhost, and the app is also
 * opened by its address on the local network. Hex fits what the backend accepts as an id.
 */
function newCorrelationId(): string {
  return Array.from(crypto.getRandomValues(new Uint8Array(16)), (byte) => byte.toString(16).padStart(2, "0")).join("");
}

function withToken(request: Request): Request {
  const token = getSession().token;
  if (token) {
    request.headers.set("Authorization", `Bearer ${token}`);
  }
  return request;
}
