import type { MetadataRoute } from "next";
import { siteUrl } from "@/app/site-url";

export default function sitemap(): MetadataRoute.Sitemap {
  return ["/", "/aviso-legal", "/privacidad"].map((path) => ({ url: new URL(path, siteUrl()).toString() }));
}
