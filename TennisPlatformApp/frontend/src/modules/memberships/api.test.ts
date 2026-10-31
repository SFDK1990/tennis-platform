import { describe, expect, it } from "vitest";
import { addMonths, describeSeries, formatMonth, overdue, type FeeLine, type GroupSeries } from "@/modules/memberships/api";
import { centsOf } from "@/shared/money";

const series = (startsAt: string | null, endsAt: string | null): GroupSeries => ({
  seriesId: "s", lessonType: "GROUP", nextStartsAt: startsAt, nextEndsAt: endsAt, upcomingLessons: 3,
});

describe("memberships", () => {
  it("moves months across the turn of the year", () => {
    expect(addMonths("2026-12", 1)).toBe("2027-01");
    expect(addMonths("2026-01", -1)).toBe("2025-12");
  });

  it("names a month in Spanish", () => {
    expect(formatMonth("2026-11")).toBe("noviembre de 2026");
  });

  it("says a series by its weekday on the teacher's clock, in plural", () => {
    // Tuesday 3 November 2026, 17:00 UTC is 18:00 in Madrid.
    expect(describeSeries(series("2026-11-03T17:00:00Z", "2026-11-03T18:30:00Z"), "Europe/Madrid"))
      .toBe("Los martes, 18:00 a 19:30");
    expect(describeSeries(series("2026-11-07T09:00:00Z", "2026-11-07T10:00:00Z"), "Europe/Madrid"))
      .toBe("Los sábados, 10:00 a 11:00");
    expect(describeSeries(series(null, null), "Europe/Madrid")).toBe("Sin clases por delante");
  });

  it("counts as overdue only what is past its day without anything recorded", () => {
    const line = (status: FeeLine["status"]) => ({ status }) as FeeLine;
    expect(overdue([line("OVERDUE"), line("PENDING"), line("WAIVED"), line("PAID")])).toHaveLength(1);
  });

  it("reads a typed amount in cents", () => {
    expect(centsOf("45")).toBe(4500);
    expect(centsOf("45,5")).toBe(4550);
    expect(centsOf(" 12.25 ")).toBe(1225);
    expect(centsOf("doce")).toBeNull();
  });
});
