import { describe, expect, it } from "vitest";
import { addDays, dateIn, formatTime, weekOf, zonedToInstant } from "@/shared/time";

const MADRID = "Europe/Madrid";

describe("zonedToInstant", () => {
  it("reads the wall clock of the teacher's zone, in winter and in summer", () => {
    expect(zonedToInstant("2026-01-15", "18:00", MADRID)).toBe("2026-01-15T17:00:00.000Z");
    expect(zonedToInstant("2026-07-15", "18:00", MADRID)).toBe("2026-07-15T16:00:00.000Z");
  });

  // The dates the clocks change are written out: a computed one could miss the change.
  it("gets the offset right on the days the clocks change", () => {
    expect(zonedToInstant("2026-03-29", "10:00", MADRID)).toBe("2026-03-29T08:00:00.000Z");
    expect(zonedToInstant("2026-10-25", "10:00", MADRID)).toBe("2026-10-25T09:00:00.000Z");
  });

  it("round-trips through the formatting", () => {
    const instant = zonedToInstant("2026-10-25", "09:30", MADRID);

    expect(dateIn(instant, MADRID)).toBe("2026-10-25");
    expect(formatTime(instant, MADRID)).toBe("09:30");
  });
});

describe("weekOf", () => {
  it("runs from Monday to Sunday", () => {
    expect(weekOf("2026-09-26")).toEqual({ from: "2026-09-21", to: "2026-09-27" });
    expect(weekOf("2026-09-27")).toEqual({ from: "2026-09-21", to: "2026-09-27" });
    expect(weekOf("2026-09-28")).toEqual({ from: "2026-09-28", to: "2026-10-04" });
  });
});

describe("addDays", () => {
  it("crosses months and years", () => {
    expect(addDays("2026-12-31", 1)).toBe("2027-01-01");
    expect(addDays("2026-03-01", -1)).toBe("2026-02-28");
  });
});
