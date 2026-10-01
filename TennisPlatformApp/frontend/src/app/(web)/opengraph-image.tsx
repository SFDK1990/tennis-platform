import { ImageResponse } from "next/og";
import { school } from "./_parts/school";

export const alt = `${school.name}, escuela de tenis`;
export const size = { width: 1200, height: 630 };
export const contentType = "image/png";

/**
 * The card shown when the website is shared. It is drawn into a PNG on the server, so the
 * inline styles here never reach a browser page or its CSP.
 */
export default function OpenGraphImage() {
  return new ImageResponse(
    (
      <div style={{ display: "flex", width: "100%", height: "100%", background: "#0b1e3f", padding: 80, alignItems: "center", gap: 64 }}>
        <svg viewBox="0 0 120 120" width="300" height="300">
          <path d="M36 30C30 18 32 8 42 2c0 10 4 18 10 22z" fill="#1f5fb8" />
          <path d="M48 24c-1-11 5-19 15-23-2 10 0 17 5 21z" fill="#1f5fb8" />
          <circle cx="56" cy="64" r="42" fill="#e4f03c" />
          <path d="M21 42c19 14 21 42 14 58" fill="none" stroke="#ffffff" strokeWidth="4" strokeLinecap="round" />
          <path d="M86 46c18-4 32 10 28 30-4-7-10-10-18-10 3 5 2 11-4 14-4-8-8-24-6-34z" fill="#0b1e3f" stroke="#ffffff" strokeWidth="3" strokeLinejoin="round" />
          <circle cx="74" cy="52" r="7.5" fill="#ffffff" />
          <circle cx="76" cy="52" r="3.8" fill="#0b1e3f" />
        </svg>
        <div style={{ display: "flex", flexDirection: "column", color: "#ffffff" }}>
          <div style={{ fontSize: 96, fontWeight: 700, lineHeight: 1 }}>{school.name}</div>
          <div style={{ fontSize: 44, color: "#c9d6ea", marginTop: 16 }}>Escuela de tenis</div>
        </div>
      </div>
    ),
    size,
  );
}
