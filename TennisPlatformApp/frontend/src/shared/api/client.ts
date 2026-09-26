import createClient from "openapi-fetch";
import type { paths } from "@/shared/api/schema";
import { getSession, refreshAccessToken } from "@/shared/api/session";

/** The only way into the API. Paths, parameters and bodies are checked against openapi.yaml. */
export const api = createClient<paths>({ baseUrl: "/api/v1", fetch: authenticatedFetch });

/** Their 401 is a wrong password or a dead session, not an access token to renew. */
const NOT_RENEWABLE = new Set(["/api/v1/auth/login", "/api/v1/auth/refresh"]);

/** Adds the access token and, on a 401, refreshes it once and repeats the request. */
export async function authenticatedFetch(request: Request): Promise<Response> {
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

function withToken(request: Request): Request {
  const token = getSession().token;
  if (token) {
    request.headers.set("Authorization", `Bearer ${token}`);
  }
  return request;
}
