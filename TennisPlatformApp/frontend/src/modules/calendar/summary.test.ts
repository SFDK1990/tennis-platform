import { describe, expect, it } from "vitest";
import { attendance, formatMonth, occupancy, shiftMonth, type MonthFigures } from "@/modules/calendar/summary";

const EMPTY: MonthFigures = {
  lessonsGiven: 0, individualLessons: 0, groupLessons: 0, seatsOffered: 0, seatsTaken: 0, attended: 0, noShow: 0,
  unmarked: 0, activeStudents: 0, cancelledLessons: 0, cancelledAtShortNotice: 0,
};

describe("summary", () => {
  it("moves across the turn of the year", () => {
    expect(shiftMonth("2026-01", -1)).toBe("2025-12");
    expect(shiftMonth("2026-12", 1)).toBe("2027-01");
    expect(shiftMonth("2026-10", -1)).toBe("2026-09");
  });

  it("names the month in Spanish", () => {
    expect(formatMonth("2026-10")).toBe("octubre de 2026");
  });

  it("leaves the unmarked seats out of the attendance", () => {
    expect(attendance({ ...EMPTY, attended: 3, noShow: 1, unmarked: 4 })).toBe(75);
    expect(attendance({ ...EMPTY, unmarked: 4 })).toBeNull();
  });

  it("has no occupancy without seats", () => {
    expect(occupancy(EMPTY)).toBeNull();
    expect(occupancy({ ...EMPTY, seatsOffered: 3, seatsTaken: 2 })).toBe(67);
  });
});
