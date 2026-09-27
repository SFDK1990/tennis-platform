/** Where the stack is and who the bootstrap created. Loaded by playwright.config.ts. */

function required(name: string): string {
  const value = process.env[name];
  if (!value) {
    throw new Error(`${name} is not set: the E2E suite needs the account the bootstrap creates (see .env.example).`);
  }
  return value;
}

export const BACKEND = `http://localhost:${process.env.SERVER_PORT || 8080}/api/v1`;
export const MAILPIT = `http://localhost:${process.env.MAILPIT_UI_PORT || 8025}`;
export const ZONE = process.env.TEACHER_TIMEZONE || "Europe/Madrid";

export const TEACHER = { email: required("TEACHER_EMAIL"), password: required("TEACHER_PASSWORD") };
export const ADMIN = { email: required("ADMIN_EMAIL"), password: required("ADMIN_PASSWORD") };

export const STUDENT_PASSWORD = "e2e-student-password";

export function uniqueEmail(): string {
  return `e2e-${Date.now()}-${Math.random().toString(36).slice(2, 8)}@example.com`;
}
