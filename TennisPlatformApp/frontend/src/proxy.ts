import { NextResponse, type NextRequest } from "next/server";

/**
 * A Content-Security-Policy with a fresh nonce per request (26-fase15-analisis-seguridad.md).
 * The access token lives in memory, so a script injected into the page is the one thing that
 * could read it; the nonce is what lets only Next's own scripts run. Next reads the nonce back
 * from the request header and puts it on every script it renders, which is why every page is
 * rendered per request (the root layout awaits `connection()`).
 */
export function proxy(request: NextRequest) {
  const nonce = Buffer.from(crypto.randomUUID()).toString("base64");
  // React uses eval in development only, to rebuild server error stacks in the browser.
  const devEval = process.env.NODE_ENV === "development" ? " 'unsafe-eval'" : "";
  const policy = [
    "default-src 'self'",
    `script-src 'self' 'nonce-${nonce}' 'strict-dynamic'${devEval}`,
    `style-src 'self' 'nonce-${nonce}'`,
    "img-src 'self' blob: data:",
    "font-src 'self'",
    "connect-src 'self'",
    // The service worker and the manifest (45-fase30). With 'strict-dynamic' the 'self' of
    // script-src does not count, and the browser would refuse the worker without a word.
    "worker-src 'self'",
    "manifest-src 'self'",
    "object-src 'none'",
    "base-uri 'self'",
    "form-action 'self'",
    "frame-ancestors 'none'",
  ].join("; ");

  const requestHeaders = new Headers(request.headers);
  requestHeaders.set("x-nonce", nonce);
  requestHeaders.set("Content-Security-Policy", policy);

  const response = NextResponse.next({ request: { headers: requestHeaders } });
  response.headers.set("Content-Security-Policy", policy);
  return response;
}

export const config = {
  matcher: [
    {
      // Pages only: /api is the backend's, and static files carry no script to protect. The
      // offline page keeps its CSP: it is a page, with no script that would need the nonce.
      source: "/((?!api|_next/static|_next/image|icon.svg|icons/|apple-icon|manifest.webmanifest|sw.js).*)",
      missing: [
        { type: "header", key: "next-router-prefetch" },
        { type: "header", key: "purpose", value: "prefetch" },
      ],
    },
  ],
};
