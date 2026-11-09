import { readFileSync } from "node:fs";
import { describe, expect, it } from "vitest";

/**
 * Every text the app writes, on every background it writes it on, passes WCAG AA in both themes
 * (47-fase32-analisis-modo-oscuro.md, rule 2). axe only sees the states a screen happens to show;
 * this reads the tokens of globals.css themselves, so a hover or an error nobody opened counts too.
 */
const css = readFileSync(new URL("./globals.css", import.meta.url), "utf8");

function tokensIn(block: string): Record<string, string> {
  return Object.fromEntries([...block.matchAll(/--color-([\w-]+):\s*(#[0-9a-f]{6})\s*;/gi)].map(([, name, value]) => [name, value]));
}

function blockAfter(selector: string, from = 0): string {
  const start = css.indexOf(`${selector} {`, from);
  expect(start, selector).toBeGreaterThanOrEqual(0);
  return css.slice(start, css.indexOf("}", start));
}

const light = tokensIn(blockAfter("@theme"));
const forcedDark = tokensIn(blockAfter(':root[data-theme="dark"]'));
const systemDark = tokensIn(blockAfter(':root:not([data-theme="light"])', css.indexOf("@media (prefers-color-scheme: dark)")));
const dark = { ...light, ...forcedDark };

function luminance(hex: string): number {
  const [r, g, b] = [1, 3, 5]
    .map((i) => parseInt(hex.slice(i, i + 2), 16) / 255)
    .map((c) => (c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4));
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}

/** White at `alpha` over `hex`, as the browser paints text-white/85. */
function whiteOver(hex: string, alpha: number): string {
  const channel = (i: number) => Math.round(alpha * 255 + (1 - alpha) * parseInt(hex.slice(i, i + 2), 16));
  return `#${[1, 3, 5].map((i) => channel(i).toString(16).padStart(2, "0")).join("")}`;
}

function contrast(a: string, b: string): number {
  const [high, low] = [luminance(a), luminance(b)].sort((x, y) => y - x);
  return (high + 0.05) / (low + 0.05);
}

/**
 * [text, background, where]: "white" is the literal text-white of the brand's fills, and
 * "white/85" the softer one of the court card's second lines, blended over its background.
 */
const PAIRS: [string, string, string][] = [
  ["ink", "paper", "text on cards"],
  ["ink", "ground", "text on the page"],
  ["muted", "paper", "muted text on cards"],
  ["muted", "ground", "muted text on the page"],
  ["muted", "line-soft", "neutral chip"],
  ["link", "paper", "links, outlined blocks, the button on the court"],
  ["link", "ground", "links on the page, the tab bar"],
  ["link", "court-tint", "court chip, «Ir a la web»"],
  ["ink", "court-tint", "chosen option, secondary button under the pointer"],
  ["white", "court", "primary button, the court card, group blocks"],
  ["white/85", "court", "the court card's second lines"],
  ["white", "court-deep", "primary button under the pointer"],
  ["white", "navy", "avatar, taken seat, «Faltó»"],
  ["on-navy", "frame", "sidebar and sign-in screens"],
  ["white", "frame", "sidebar and sign-in screens"],
  ["white", "surround", "«Vino», «Tienes plaza»"],
  ["surround-ink", "surround-tint", "success chip and notice"],
  ["warning", "warning-tint", "warning chip and notice"],
  ["fault", "fault-tint", "error chip and notice"],
  ["fault", "paper", "danger button, field errors"],
  ["white", "fault-fill", "danger button under the pointer"],
];

describe.each([
  ["light", light],
  ["dark", dark],
])("the %s palette", (_, palette) => {
  const colour = (token: string) => (token === "white" ? "#ffffff" : palette[token]);

  it.each(PAIRS)("%s on %s passes AA (%s)", (text, background) => {
    const under = colour(background);
    expect(under, background).toBeDefined();
    const over = text === "white/85" ? whiteOver(under, 0.85) : colour(text);
    expect(over, text).toBeDefined();
    expect(contrast(over, under)).toBeGreaterThanOrEqual(4.5);
  });
});

describe("the dark theme", () => {
  it("is the same whether the system asks for it or the visitor chose it", () => {
    expect(Object.keys(forcedDark).length).toBeGreaterThan(10);
    expect(systemDark).toEqual(forcedDark);
  });
});
