import type { NextConfig } from "next";

const backendUrl = process.env.BACKEND_URL ?? "http://localhost:8081";

// The browser only ever talks to this origin: the refresh cookie (path /api/v1/auth,
// SameSite=Strict) and the XSRF cookie then work without CORS (22-fase11-analisis-frontend.md).
const nextConfig: NextConfig = {
  async rewrites() {
    return [{ source: "/api/:path*", destination: `${backendUrl}/api/:path*` }];
  },
};

export default nextConfig;
