import type { CalendarLesson } from "@/modules/calendar/api";
import { dateIn, type LocalDate } from "@/shared/time";

const byStart = (a: CalendarLesson, b: CalendarLesson) => a.startsAt.localeCompare(b.startsAt);
const endsAfter = (lesson: CalendarLesson, now: number) => new Date(lesson.endsAt).getTime() > now;

export interface TeacherDay {
  /** The lesson on court now or the next one today; null once the day is done or empty. */
  focus: CalendarLesson | null;
  /** Today's other lessons still on, earlier and later, in order; cancelled ones stay in the agenda. */
  rest: CalendarLesson[];
  /** The next few lessons after today, for a day without any. */
  upcoming: CalendarLesson[];
}

/** What the teacher's "Hoy" shows (27-fase15.5, decision 5). */
export function teacherDay(lessons: CalendarLesson[], zone: string, today: LocalDate, now: number): TeacherDay {
  const sorted = lessons.toSorted(byStart);
  const todays = sorted.filter((lesson) => dateIn(lesson.startsAt, zone) === today);
  const focus = todays.find((lesson) => lesson.status !== "CANCELLED" && endsAfter(lesson, now)) ?? null;
  return {
    focus,
    rest: todays.filter((lesson) => lesson !== focus && lesson.status !== "CANCELLED"),
    upcoming: sorted
      .filter((lesson) => dateIn(lesson.startsAt, zone) > today && lesson.status !== "CANCELLED")
      .slice(0, 3),
  };
}

export interface StudentHome {
  /** The first lesson the student has a seat in that has not ended. */
  next: CalendarLesson | null;
  /** The next lessons they could still book. */
  open: CalendarLesson[];
}

/** What the student's "Inicio" shows (27-fase15.5, decision 6). */
export function studentHome(lessons: CalendarLesson[], now: number, openCount = 3): StudentHome {
  const sorted = lessons.toSorted(byStart);
  const booked = (lesson: CalendarLesson) => lesson.myBooking?.status === "CONFIRMED";
  return {
    next: sorted.find((lesson) => booked(lesson) && endsAfter(lesson, now)) ?? null,
    open: sorted
      .filter((lesson) => lesson.status === "OPEN" && !booked(lesson) && new Date(lesson.startsAt).getTime() > now)
      .slice(0, openCount),
  };
}
