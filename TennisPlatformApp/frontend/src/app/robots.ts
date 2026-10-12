import type { MetadataRoute } from "next";
import { siteUrl } from "@/app/site-url";

/** Only the public website is for search engines; the app's screens are behind an account. */
export default function robots(): MetadataRoute.Robots {
  return {
    rules: {
      userAgent: "*",
      allow: "/",
      disallow: ["/home", "/calendar", "/bookings", "/profile", "/teacher", "/admin", "/verify-email", "/reset-password"],
    },
    sitemap: new URL("/sitemap.xml", siteUrl()).toString(),
  };
}
