import { describe, expect, it } from "vitest";
import { balancesOf, describeBalance, formatEuros, type Pass } from "@/modules/passes/api";

function pass(overrides: Partial<Pass>): Pass {
  return {
    id: crypto.randomUUID(), studentUserId: "s", passTypeId: "t", name: "Bono", lessonType: "GROUP", lessons: 10,
    used: 0, remaining: 10, priceCents: 12000, paidOn: "2026-10-01", paymentMethod: "CASH", validUntil: null,
    status: "ACTIVE", createdAt: "2026-10-01T10:00:00Z", voidedAt: null, ...overrides,
  };
}

describe("balancesOf", () => {
  it("adds up the active passes of each kind and keeps the first day one of them ends", () => {
    const balances = balancesOf([
      pass({ remaining: 3, validUntil: "2027-01-15" }),
      pass({ remaining: 2, validUntil: "2026-12-01" }),
      pass({ remaining: 5, status: "EXPIRED", validUntil: "2026-09-01" }),
      pass({ lessonType: "INDIVIDUAL", remaining: 0, status: "USED_UP" }),
      pass({ lessonType: "INDIVIDUAL", status: "VOIDED" }),
    ]);

    expect(balances).toEqual([
      { kind: "GROUP", remaining: 5, until: "2026-12-01" },
      { kind: "INDIVIDUAL", remaining: 0, until: null },
    ]);
    expect(describeBalance(balances[0])).toBe("5 clases de grupo, hasta el 1 dic");
  });

  it("says nothing of a kind the student never had a pass for", () => {
    expect(balancesOf([pass({ lessonType: "INDIVIDUAL", status: "VOIDED" })])).toEqual([]);
  });
});

describe("formatEuros", () => {
  it("writes cents as euros, with decimals only when there are any", () => {
    expect(formatEuros(12000).replace(/\s/g, " ")).toBe("120 €");
    expect(formatEuros(1250).replace(/\s/g, " ")).toBe("12,50 €");
  });
});
