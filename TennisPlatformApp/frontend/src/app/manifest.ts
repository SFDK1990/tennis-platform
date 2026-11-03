import type { MetadataRoute } from "next";
import { BRAND_NAVY } from "@/app/brand";

/**
 * What makes the app installable (45-fase30-analisis-pwa-accesibilidad.md). It starts at the
 * student's home: RequireAccount sends the teacher and the admin to theirs and anyone signed
 * out to /login, while "/" is the school's website.
 */
export default function manifest(): MetadataRoute.Manifest {
  return {
    name: "Oasis Papagayo",
    short_name: "Oasis",
    description: "Tus clases de tenis con Marcos: reservas, horario y avisos.",
    lang: "es",
    start_url: "/home",
    scope: "/",
    display: "standalone",
    background_color: BRAND_NAVY,
    theme_color: BRAND_NAVY,
    icons: [
      { src: "/icons/icon-192.png", sizes: "192x192", type: "image/png", purpose: "any" },
      { src: "/icons/icon-512.png", sizes: "512x512", type: "image/png", purpose: "any" },
      { src: "/icons/icon-maskable-512.png", sizes: "512x512", type: "image/png", purpose: "maskable" },
    ],
  };
}
