import type { NextConfig } from "next";

const backendUrl = process.env.BACKEND_URL ?? "http://localhost:8081";

// The browser only ever talks to this origin: the refresh cookie (path /api/v1/auth,
// SameSite=Strict) and the XSRF cookie then work without CORS (22-fase11-analisis-frontend.md).
// The rest of the security headers; the Content-Security-Policy is set per request in
// src/proxy.ts, because it carries a nonce (26-fase15-analisis-seguridad.md).
const securityHeaders = [
  // The verification and reset links carry their token in the URL.
  { key: "Referrer-Policy", value: "no-referrer" },
  { key: "X-Content-Type-Options", value: "nosniff" },
  { key: "X-Frame-Options", value: "DENY" },
  { key: "Permissions-Policy", value: "camera=(), microphone=(), geolocation=(), payment=()" },
  // Browsers ignore it over plain http, so it only takes effect once deployed with HTTPS.
  { key: "Strict-Transport-Security", value: "max-age=31536000" },
];

const nextConfig: NextConfig = {
  poweredByHeader: false,
  async headers() {
    return [{ source: "/:path*", headers: securityHeaders }];
  },
  async rewrites() {
    return [{ source: "/api/:path*", destination: `${backendUrl}/api/:path*` }];
  },
};

export default nextConfig;
