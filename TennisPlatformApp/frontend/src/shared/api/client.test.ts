import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { authenticatedFetch } from "@/shared/api/client";
import { ApiError, unwrap } from "@/shared/api/errors";
import { endSession, getSession, startSession } from "@/shared/api/session";

const ORIGIN = "http://localhost:3000";
const json = (status: number, body: unknown) =>
  new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });

describe("authenticatedFetch", () => {
  const fetchMock = vi.fn<typeof fetch>();

  beforeEach(() => {
    vi.stubGlobal("fetch", fetchMock);
    startSession("expired-token");
  });

  afterEach(() => {
    fetchMock.mockReset();
    vi.unstubAllGlobals();
    endSession();
  });

  const authorizationOf = (call: number) => (fetchMock.mock.calls[call][0] as Request).headers.get("Authorization");
  const refreshCalls = () => fetchMock.mock.calls.filter(([input]) => input === "/api/v1/auth/refresh");

  it("refreshes an expired token once and repeats the request with the new one", async () => {
    fetchMock
      .mockResolvedValueOnce(json(401, { code: "AUTH_TOKEN_EXPIRED" }))
      .mockResolvedValueOnce(json(200, { accessToken: "fresh-token" }))
      .mockResolvedValueOnce(json(200, { ok: true }));

    const response = await authenticatedFetch(new Request(`${ORIGIN}/api/v1/me`));

    expect(response.status).toBe(200);
    expect(authorizationOf(0)).toBe("Bearer expired-token");
    expect(authorizationOf(2)).toBe("Bearer fresh-token");
    expect(refreshCalls()).toHaveLength(1);
  });

  /** Two refreshes would present the same refresh token twice, and the backend reads that as theft. */
  it("shares one refresh between requests that expire together", async () => {
    fetchMock.mockImplementation(async (input) => {
      if (input === "/api/v1/auth/refresh") {
        return json(200, { accessToken: "fresh-token" });
      }
      const request = input as Request;
      return request.headers.get("Authorization") === "Bearer fresh-token" ? json(200, {}) : json(401, {});
    });

    const responses = await Promise.all([
      authenticatedFetch(new Request(`${ORIGIN}/api/v1/me`)),
      authenticatedFetch(new Request(`${ORIGIN}/api/v1/bookings`)),
    ]);

    expect(responses.map((response) => response.status)).toEqual([200, 200]);
    expect(refreshCalls()).toHaveLength(1);
  });

  it("gives up and signs out when the refresh itself is refused", async () => {
    fetchMock
      .mockResolvedValueOnce(json(401, {}))
      .mockResolvedValueOnce(json(401, { code: "AUTH_SESSION_EXPIRED" }));

    const response = await authenticatedFetch(new Request(`${ORIGIN}/api/v1/me`));

    expect(response.status).toBe(401);
    expect(getSession()).toEqual({ status: "signed-out", token: null });
  });

  it("does not treat a failed login as an expired token", async () => {
    fetchMock.mockResolvedValueOnce(json(401, { code: "AUTH_INVALID_CREDENTIALS" }));

    const response = await authenticatedFetch(new Request(`${ORIGIN}/api/v1/auth/login`, { method: "POST" }));

    expect(response.status).toBe(401);
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });
});

describe("unwrap", () => {
  it("turns a Problem Details body into an ApiError carrying its code", () => {
    const problem = {
      title: "Validation failed", status: 400, detail: "Validation failed", code: "VALIDATION_ERROR",
      errors: [{ field: "email", message: "must be a well-formed email address" }],
    };

    const call = () => unwrap({ error: problem, response: new Response(null, { status: 400 }) });

    expect(call).toThrow(ApiError);
    try {
      call();
    } catch (error) {
      expect(error).toMatchObject({ status: 400, code: "VALIDATION_ERROR", message: "Validation failed" });
      expect((error as ApiError).fieldErrors).toEqual(problem.errors);
    }
  });

  it("marks a 409 as stale, so the screen knows to re-read", () => {
    const error = ApiError.fromProblem(409, { title: "Conflict", status: 409, code: "LESSON_FULL" });

    expect(error.isStale).toBe(true);
  });

  it("still gives an ApiError when the body is not Problem Details", () => {
    const error = ApiError.fromProblem(502, "Bad Gateway");

    expect(error).toMatchObject({ status: 502, code: undefined, message: "HTTP 502" });
  });
});
