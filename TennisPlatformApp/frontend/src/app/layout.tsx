import type { Metadata } from "next";
import { Barlow, Barlow_Semi_Condensed } from "next/font/google";
import { connection } from "next/server";
import { Providers } from "@/app/providers";
import "./globals.css";

const barlow = Barlow({ variable: "--font-barlow", subsets: ["latin"], weight: ["400", "600"] });
const barlowCondensed = Barlow_Semi_Condensed({
  variable: "--font-barlow-condensed",
  subsets: ["latin"],
  weight: ["600"],
});

export const metadata: Metadata = {
  title: "Tennis Platform",
  description: "Clases de tenis: disponibilidad, reservas y asistencia.",
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
