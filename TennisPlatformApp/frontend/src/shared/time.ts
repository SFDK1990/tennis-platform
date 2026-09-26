/**
 * Everything is shown in the teacher's zone, which the calendar sends (22-fase11, decision 7).
 * A day is a "YYYY-MM-DD" string: it is what the API takes, and it has no zone to get wrong.
 */
export type LocalDate = string;

const LOCALE = "es-ES";

export function todayIn(zone: string): LocalDate {
  return dateIn(new Date(), zone);
}

export function dateIn(instant: Date | string, zone: string): LocalDate {
  // en-CA formats as YYYY-MM-DD.
  return new Intl.DateTimeFormat("en-CA", { timeZone: zone, year: "numeric", month: "2-digit", day: "2-digit" })
    .format(new Date(instant));
}

export function addDays(date: LocalDate, days: number): LocalDate {
  const [year, month, day] = date.split("-").map(Number);
  return new Date(Date.UTC(year, month - 1, day + days)).toISOString().slice(0, 10);
}

/** Monday to Sunday, the week the day belongs to. */
export function weekOf(date: LocalDate): { from: LocalDate; to: LocalDate } {
  const [year, month, day] = date.split("-").map(Number);
  const isoWeekday = new Date(Date.UTC(year, month - 1, day)).getUTCDay() || 7;
  const from = addDays(date, 1 - isoWeekday);
  return { from, to: addDays(from, 6) };
}

export function formatTime(instant: string, zone: string): string {
  return new Intl.DateTimeFormat(LOCALE, { timeZone: zone, hour: "2-digit", minute: "2-digit" }).format(new Date(instant));
}

export function formatDay(date: LocalDate): string {
  return new Intl.DateTimeFormat(LOCALE, { timeZone: "UTC", weekday: "long", day: "numeric", month: "long" })
    .format(new Date(`${date}T00:00:00Z`));
}

/** "21 sept." - for ranges, where the weekday would only be noise. */
export function formatShortDay(date: LocalDate): string {
  return new Intl.DateTimeFormat(LOCALE, { timeZone: "UTC", day: "numeric", month: "short" })
    .format(new Date(`${date}T00:00:00Z`));
}

export function formatDateTime(instant: string, zone: string): string {
  return `${formatDay(dateIn(instant, zone))}, ${formatTime(instant, zone)}`;
}

/**
 * The instant at which the wall clock of `zone` reads `date time`. Computed twice because the
 * offset of the first guess can be the wrong one on the day the clocks change.
 */
export function zonedToInstant(date: LocalDate, time: string, zone: string): string {
  const [year, month, day] = date.split("-").map(Number);
  const [hour, minute] = time.split(":").map(Number);
  const wall = Date.UTC(year, month - 1, day, hour, minute);
  let instant = wall - offsetOf(wall, zone);
  instant = wall - offsetOf(instant, zone);
  return new Date(instant).toISOString();
}

function offsetOf(instant: number, zone: string): number {
  const parts = new Intl.DateTimeFormat("en-US", {
    timeZone: zone, hourCycle: "h23",
    year: "numeric", month: "numeric", day: "numeric", hour: "numeric", minute: "numeric", second: "numeric",
  }).formatToParts(new Date(instant));
  const part = (type: Intl.DateTimeFormatPartTypes) => Number(parts.find((p) => p.type === type)?.value);
  const asUtc = Date.UTC(part("year"), part("month") - 1, part("day"), part("hour"), part("minute"), part("second"));
  return asUtc - Math.floor(instant / 1000) * 1000;
}
