/**
 * The access token lives here and only here, in memory: localStorage would hand it to any
 * XSS. A reload loses it on purpose, and the refresh cookie (HttpOnly) gets a new one.
 */
export type SessionStatus = "restoring" | "signed-in" | "signed-out";

export interface Session {
  status: SessionStatus;
  token: string | null;
}

const REFRESH_URL = "/api/v1/auth/refresh";
const LOGOUT_URL = "/api/v1/auth/logout";

let current: Session = { status: "restoring", token: null };
const listeners = new Set<() => void>();
let refreshing: Promise<boolean> | null = null;

export function getSession(): Session {
  return current;
}

export function subscribeToSession(listener: () => void): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function startSession(token: string): void {
  setSession({ status: "signed-in", token });
}

export function endSession(): void {
  setSession({ status: "signed-out", token: null });
}

function setSession(next: Session): void {
  current = next;
  listeners.forEach((listener) => listener());
}

/**
 * One refresh at a time: when several requests hit a 401 together, they all wait for the same
 * one. Two parallel refreshes would present the same refresh token twice, and the backend
 * treats a reused token as theft and revokes the whole family.
 */
export function refreshAccessToken(): Promise<boolean> {
  refreshing ??= refresh().finally(() => {
    refreshing = null;
  });
  return refreshing;
}

async function refresh(): Promise<boolean> {
  const response = await postWithXsrf(REFRESH_URL);
  if (!response.ok) {
    endSession();
    return false;
  }
  const body = (await response.json()) as { accessToken: string };
  startSession(body.accessToken);
  return true;
}

/** On page load: the refresh cookie, if there is one, brings the session back. */
export function restoreSession(): void {
  if (current.status === "restoring") {
    void refreshAccessToken();
  }
}

export async function signOut(): Promise<void> {
  try {
    await postWithXsrf(LOGOUT_URL);
  } finally {
    endSession();
  }
}

/** Without an XSRF-TOKEN cookie yet (a fresh browser), the 403 is what sets it: ask once more. */
async function postWithXsrf(url: string): Promise<Response> {
  const response = await post(url);
  return response.status === 403 ? post(url) : response;
}

function post(url: string): Promise<Response> {
  const xsrf = readCookie("XSRF-TOKEN");
  return fetch(url, {
    method: "POST",
    credentials: "same-origin",
    headers: xsrf ? { "X-XSRF-TOKEN": xsrf } : {},
  });
}

function readCookie(name: string): string | null {
  if (typeof document === "undefined") {
    return null;
  }
  const prefix = `${name}=`;
  const found = document.cookie.split("; ").find((cookie) => cookie.startsWith(prefix));
  return found ? decodeURIComponent(found.slice(prefix.length)) : null;
}
