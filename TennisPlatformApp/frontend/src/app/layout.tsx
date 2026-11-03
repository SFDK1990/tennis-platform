import type { Metadata, Viewport } from "next";
import { Barlow, Barlow_Condensed } from "next/font/google";
import { connection } from "next/server";
import { BRAND_NAVY } from "@/app/brand";
import { Providers } from "@/app/providers";
import { siteUrl } from "@/app/site-url";
import "./globals.css";

const barlow = Barlow({ variable: "--font-barlow", subsets: ["latin"], weight: ["400", "500", "600"] });
// The face of the logo's name; next/font serves it from this origin, as the CSP requires.
const barlowCondensed = Barlow_Condensed({
  variable: "--font-barlow-condensed",
  subsets: ["latin"],
  weight: ["500", "600", "700"],
});

export const metadata: Metadata = {
  metadataBase: siteUrl(),
  title: "Oasis Papagayo",
  description: "Escuela de tenis Oasis Papagayo: reservas, horario y asistencia de las clases con Marcos.",
  // iPhone reads neither the manifest's name nor its icons; app/apple-icon.png is the icon.
  // "default": a light bar over the light frame, with the page below it and not under it.
  appleWebApp: { title: "Oasis", statusBarStyle: "default" },
};

export const viewport: Viewport = {
  themeColor: BRAND_NAVY,
};

export default async function RootLayout({ children }: LayoutProps<"/">) {
  // Rendered per request: the CSP nonce (src/proxy.ts) only exists once a request does.
  await connection();
  return (
    <html lang="es" className={`${barlow.variable} ${barlowCondensed.variable} h-full antialiased`}>
      {/* Extensions such as ColorZilla add attributes to <body> before React hydrates. This
          silences that one element's attributes, not its content. */}
      <body className="min-h-full" suppressHydrationWarning>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
