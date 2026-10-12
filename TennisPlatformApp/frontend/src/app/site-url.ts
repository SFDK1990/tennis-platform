/**
 * The address the website is published at, for the links that leave it: the sitemap, robots
 * and the Open Graph image. Until it is deployed (phase 17) it is the local one.
 */
export function siteUrl(): URL {
  return new URL(process.env.SITE_URL ?? "http://localhost:3000");
}
