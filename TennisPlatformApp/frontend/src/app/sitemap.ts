import type { MetadataRoute } from "next";
import { siteUrl } from "@/app/site-url";

export default function sitemap(): MetadataRoute.Sitemap {
  return [{ url: new URL("/", siteUrl()).toString() }];
}
