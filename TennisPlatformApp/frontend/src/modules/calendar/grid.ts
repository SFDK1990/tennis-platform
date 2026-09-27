import { minutesIn } from "@/shared/time";

/** A quarter of an hour per grid row: the finest step lessons are created with. */
export const ROW_MINUTES = 15;
export const ROWS_PER_HOUR = 60 / ROW_MINUTES;

interface Span {
  startsAt: string;
  endsAt: string;
}

/**
 * The hours the week's grid shows: at least 9 to 21, stretched to whatever the week holds.
 * Wall-clock hours of the teacher's zone, so the day the clocks change looks like any other.
 */
export function hourRange(items: Span[], zone: string): { first: number; last: number } {
  let first = 9;
  let last = 21;
  for (const { startsAt, endsAt } of items) {
    first = Math.min(first, Math.floor(minutesIn(startsAt, zone) / 60));
    last = Math.max(last, Math.ceil(endOf(startsAt, endsAt, zone) / 60));
  }
  return { first, last: Math.min(last, 24) };
}

/**
 * The rows an interval covers in a day column (1-based, as CSS grid counts), rounded out to
 * whole quarters so a 50-minute lesson still reads as the block it is.
 */
export function rowsOf({ startsAt, endsAt }: Span, zone: string, firstHour: number): { start: number; span: number } {
  const offset = firstHour * 60;
  const start = Math.max(Math.floor((minutesIn(startsAt, zone) - offset) / ROW_MINUTES), 0);
  const end = Math.ceil((endOf(startsAt, endsAt, zone) - offset) / ROW_MINUTES);
  return { start: start + 1, span: Math.max(end - start, 1) };
}

/** An interval that ends at midnight ends at minute 1440 of its day, not at minute 0. */
function endOf(startsAt: string, endsAt: string, zone: string): number {
  const end = minutesIn(endsAt, zone);
  return end <= minutesIn(startsAt, zone) ? 24 * 60 : end;
}
