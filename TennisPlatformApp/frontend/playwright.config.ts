import { existsSync } from "node:fs";
import { defineConfig, devices } from "@playwright/test";

// Locally the ports and the bootstrap accounts come from the stack's own .env; CI sets them
// in the job. Variables already set win over the file.
if (existsSync("../.env")) {
  process.loadEnvFile("../.env");
}

const PORT = 3100;

/**
 * The end-to-end flows of 25-fase14-analisis-cobertura-e2e.md, against the real stack: the
 * backend, Postgres and Mailpit in Docker, and this frontend built and started here.
 */
export default defineConfig({
  testDir: "e2e",
  // One teacher and one calendar are shared by every test, so they run one after another.
  workers: 1,
  fullyParallel: false,
  // A test that passes on the second try is a test that fails.
  retries: 0,
  forbidOnly: !!process.env.CI,
  reporter: process.env.CI
    ? [["list"], ["html", { open: "never" }], ["json", { outputFile: "test-results/results.json" }]]
    : "list",
  use: {
    baseURL: `http://localhost:${PORT}`,
    locale: "es-ES",
    timezoneId: process.env.TEACHER_TIMEZONE || "Europe/Madrid",
    trace: "retain-on-failure",
  },
  projects: [
    // Students use it on their phone (a PWA); the teacher and the admin at a desk. The phone
    // navigation is checked for both.
    { name: "mobile", use: { ...devices["Pixel 7"] }, testMatch: /(student|navigation)\.spec\.ts/ },
    { name: "desktop", use: { ...devices["Desktop Chrome"] }, testIgnore: /(student|navigation)\.spec\.ts/ },
  ],
  webServer: [
    {
      command: `npm run build && npx next start -p ${PORT}`,
      url: `http://localhost:${PORT}/login`,
      env: { BACKEND_URL: `http://localhost:${process.env.SERVER_PORT || 8080}` },
      reuseExistingServer: !process.env.CI,
      timeout: 240_000,
    },
    // Stripe, for buying a pass (Fase 27.5); the backend finds it through compose.e2e.yaml.
    {
      command: "node e2e/support/fake-stripe.mjs",
      url: "http://localhost:12111/health",
      env: { FAKE_STRIPE_FRONTEND: `http://localhost:${PORT}` },
      reuseExistingServer: !process.env.CI,
    },
  ],
});
