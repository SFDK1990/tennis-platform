import { describe, expect, it } from "vitest";
import type { CalendarLesson } from "@/modules/calendar/api";
import { studentHome, teacherDay } from "@/modules/calendar/select";

const ZONE = "Europe/Madrid";

function lesson(id: string, startsAt: string, minutes: number, extra: Partial<CalendarLesson> = {}): CalendarLesson {
  return {
    id, teacherUserId: "t", type: "GROUP", startsAt,
    endsAt: new Date(new Date(startsAt).getTime() + minutes * 60_000).toISOString(),
    capacity: 4, bookedCount: 1, status: "OPEN", myBooking: null, ...extra,
  };
}

// Monday 28 September 2026, 17:30 in Madrid (UTC+2).
const NOW = Date.parse("2026-09-28T15:30:00Z");
const booked = { id: "b", status: "CONFIRMED" as const, attendance: "PENDING" as const };

describe("teacherDay", () => {
  const morning = lesson("morning", "2026-09-28T08:00:00Z", 60);
  const onCourt = lesson("on-court", "2026-09-28T15:00:00Z", 90);
  const evening = lesson("evening", "2026-09-28T18:00:00Z", 60);
  const cancelled = lesson("cancelled", "2026-09-28T17:00:00Z", 60, { status: "CANCELLED" });
  const tomorrow = lesson("tomorrow", "2026-09-29T15:00:00Z", 60);

  it("focuses the lesson on court, not the next one", () => {
    const day = teacherDay([evening, tomorrow, onCourt, morning], ZONE, "2026-09-28", NOW);
    expect(day.focus?.id).toBe("on-court");
    expect(day.rest.map((l) => l.id)).toEqual(["morning", "evening"]);
    expect(day.upcoming.map((l) => l.id)).toEqual(["tomorrow"]);
  });

  it("skips a cancelled lesson when choosing what comes next", () => {
    const day = teacherDay([cancelled, evening], ZONE, "2026-09-28", Date.parse("2026-09-28T16:45:00Z"));
    expect(day.focus?.id).toBe("evening");
  });

  it("has nothing to focus once today's lessons are over", () => {
    const day = teacherDay([morning], ZONE, "2026-09-28", NOW);
    expect(day.focus).toBeNull();
    expect(day.rest.map((l) => l.id)).toEqual(["morning"]);
  });

  it("counts a lesson in the day of the teacher's wall clock, not of UTC", () => {
    // 00:30 on Tuesday in Madrid is still Monday in UTC.
    const lateNight = lesson("late", "2026-09-28T22:30:00Z", 30);
    expect(teacherDay([lateNight], ZONE, "2026-09-28", NOW).focus).toBeNull();
    expect(teacherDay([lateNight], ZONE, "2026-09-28", NOW).upcoming.map((l) => l.id)).toEqual(["late"]);
  });
});

describe("studentHome", () => {
  it("picks the first booked lesson that has not ended, and the next ones still open", () => {
    const over = lesson("over", "2026-09-28T08:00:00Z", 60, { myBooking: booked });
    const mine = lesson("mine", "2026-09-30T17:00:00Z", 60, { myBooking: booked });
    const full = lesson("full", "2026-09-29T15:00:00Z", 60, { status: "FULL" });
    const open = ["a", "b", "c", "d"].map((id, day) => lesson(id, `2026-10-0${day + 1}T15:00:00Z`, 60));

    const home = studentHome([...open, full, mine, over], NOW);

    expect(home.next?.id).toBe("mine");
    expect(home.open.map((l) => l.id)).toEqual(["a", "b", "c"]);
  });

  it("does not offer again a lesson the student already has a seat in", () => {
    const mine = lesson("mine", "2026-09-30T17:00:00Z", 60, { myBooking: booked });
    expect(studentHome([mine], NOW).open).toEqual([]);
  });

  it("keeps a lesson in progress as the next one", () => {
    const now = lesson("now", "2026-09-28T15:00:00Z", 90, { myBooking: booked });
    expect(studentHome([now], NOW).next?.id).toBe("now");
  });
});
