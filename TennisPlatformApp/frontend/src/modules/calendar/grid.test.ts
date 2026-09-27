import { describe, expect, it } from "vitest";
import { hourRange, rowsOf } from "@/modules/calendar/grid";

const ZONE = "Europe/Madrid";

describe("rowsOf", () => {
  it("places a lesson by the teacher's wall clock, in quarters of an hour", () => {
    // 18:00-19:30 in Madrid (UTC+2), in a grid that starts at 9.
    expect(rowsOf({ startsAt: "2026-09-28T16:00:00Z", endsAt: "2026-09-28T17:30:00Z" }, ZONE, 9))
      .toEqual({ start: 37, span: 6 });
  });

  it("puts 18:00 in the same row on the day the clocks go back", () => {
    // 25 October 2026: Madrid goes from UTC+2 to UTC+1 at 03:00.
    const before = rowsOf({ startsAt: "2026-10-24T16:00:00Z", endsAt: "2026-10-24T17:00:00Z" }, ZONE, 9);
    const after = rowsOf({ startsAt: "2026-10-25T17:00:00Z", endsAt: "2026-10-25T18:00:00Z" }, ZONE, 9);
    expect(after).toEqual(before);
  });

  it("rounds a lesson that does not fall on a quarter out to the rows it touches", () => {
    // 10:10-11:00: from the 10:00 row to the end of 10:45.
    expect(rowsOf({ startsAt: "2026-09-28T08:10:00Z", endsAt: "2026-09-28T09:00:00Z" }, ZONE, 9))
      .toEqual({ start: 5, span: 4 });
  });

  it("lets an interval that ends at midnight reach the bottom of the day", () => {
    expect(rowsOf({ startsAt: "2026-09-28T20:00:00Z", endsAt: "2026-09-28T22:00:00Z" }, ZONE, 9))
      .toEqual({ start: 53, span: 8 });
  });
});

describe("hourRange", () => {
  it("shows 9 to 21 for an ordinary week", () => {
    expect(hourRange([{ startsAt: "2026-09-28T16:00:00Z", endsAt: "2026-09-28T17:00:00Z" }], ZONE)).toEqual({ first: 9, last: 21 });
  });

  it("stretches to an early lesson and a late one", () => {
    const items = [
      { startsAt: "2026-09-28T05:10:00Z", endsAt: "2026-09-28T06:00:00Z" }, // 07:10
      { startsAt: "2026-09-29T20:00:00Z", endsAt: "2026-09-29T20:30:00Z" }, // 22:00-22:30
    ];
    expect(hourRange(items, ZONE)).toEqual({ first: 7, last: 23 });
  });
});
