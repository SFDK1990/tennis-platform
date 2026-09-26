import { defineConfig, globalIgnores } from "eslint/config";
import nextVitals from "eslint-config-next/core-web-vitals";
import nextTs from "eslint-config-next/typescript";

// The frontend mirrors the backend modules, and this is its ArchUnit: a module may use
// `shared` and itself, never another module. What two modules need goes up to `shared`;
// the pages in `app/` are the composition root and may combine modules.
const MODULES = ["identity", "teacher", "student", "availability", "lesson", "booking", "calendar"];

const noParentImports = {
  group: ["../*"],
  message: "Import through '@/...': a relative path can cross a module boundary unnoticed.",
};

const moduleBoundaries = MODULES.map((module) => ({
  files: [`src/modules/${module}/**`],
  rules: {
    "no-restricted-imports": ["error", {
      patterns: [
        noParentImports,
        {
          group: ["@/modules/*", `!@/modules/${module}`, "@/app/*"],
          message: "A module only uses '@/shared' and itself (see eslint.config.mjs).",
        },
      ],
    }],
  },
}));

export default defineConfig([
  ...nextVitals,
  ...nextTs,
  {
    files: ["src/**"],
    rules: { "no-restricted-imports": ["error", { patterns: [noParentImports] }] },
  },
  ...moduleBoundaries,
  {
    files: ["src/shared/**"],
    rules: {
      "no-restricted-imports": ["error", {
        patterns: [
          noParentImports,
          { group: ["@/modules/*", "@/app/*"], message: "'shared' depends on nothing above it." },
        ],
      }],
    },
  },
  globalIgnores([".next/**", "out/**", "build/**", "next-env.d.ts", "src/shared/api/schema.d.ts"]),
]);
