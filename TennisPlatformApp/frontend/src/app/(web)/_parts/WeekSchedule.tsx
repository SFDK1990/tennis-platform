import { school, type Slot } from "./school";

const DAYS = ["Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"];

/** Literal classes, so Tailwind generates them: the CSP forbids placing blocks with style. */
const COLUMN = ["col-start-2", "col-start-3", "col-start-4", "col-start-5", "col-start-6", "col-start-7", "col-start-8"];

/** Each group class keeps one colour across the timetable. */
const TONE: Record<string, string> = {
  Niños: "bg-(--site-kids) text-(--site-kids-text)",
  Adultos: "bg-(--site-adults) text-(--site-adults-text)",
};

type Block = Slot & { name: string; tone: string };

const minutes = (time: string) => Number(time.slice(0, 2)) * 60 + Number(time.slice(3));

const blocks: Block[] = school.classes
  .flatMap((lesson) => lesson.slots.map((slot) => ({ ...slot, name: lesson.name, tone: TONE[lesson.name] ?? TONE.Adultos })))
  .sort((a, b) => a.day - b.day || minutes(a.start) - minutes(b.start));

/**
 * Only the hours with classes get rows, two per hour; a stretch with none (midday) takes a
 * single row, so a Saturday morning does not leave half the grid empty.
 */
const hours = Array.from(
  new Set(
    blocks.flatMap((block) => {
      const covered = [];
      for (let hour = Math.floor(minutes(block.start) / 60) * 60; hour < minutes(block.end); hour += 60) {
        covered.push(hour);
      }
      return covered;
    }),
  ),
).sort((a, b) => a - b);

/** Row 1 is the days' header; each hour's first row, after the gaps before it. */
const firstRow = new Map<number, number>();
let next = 2;
hours.forEach((hour, index) => {
  if (index > 0 && hours[index - 1] !== hour - 60) {
    next += 1;
  }
  firstRow.set(hour, next);
  next += 2;
});
const gaps = hours.filter((hour, index) => index > 0 && hours[index - 1] !== hour - 60);

const row = (time: number) => {
  const hour = Math.floor(time / 60) * 60;
  return (firstRow.get(hour) ?? 2) + (time - hour) / 30;
};

/** Monday to Friday always; the weekend only if a class falls on it. */
const days = Math.max(5, ...blocks.map((block) => block.day));

/**
 * The week's group classes at a glance (31-fase31, decision 25). On a computer, a grid of days
 * and hours; on a phone, where seven columns do not fit, the same classes day by day.
 */
export function WeekSchedule() {
  if (blocks.length === 0) {
    return null;
  }
  return (
    <div className="rounded-xl border border-(--site-line) bg-(--site-panel) p-4 sm:p-8">
      <div
        className={`hidden md:grid ${days === 7 ? "grid-cols-[3.5rem_repeat(7,minmax(0,1fr))]" : days === 6 ? "grid-cols-[3.5rem_repeat(6,minmax(0,1fr))]" : "grid-cols-[3.5rem_repeat(5,minmax(0,1fr))]"} grid-rows-[auto] auto-rows-[2.6rem] gap-x-2`}
      >
        {DAYS.slice(0, days).map((day, index) => (
          <div key={day} className={`${COLUMN[index]} row-start-1 pb-3 font-display text-xl font-bold`}>
            {day}
          </div>
        ))}
        {hours.map((hour) => (
          <div
            key={hour}
            className={`col-span-full row-span-2 border-t border-(--site-line) pt-1 text-sm tabular-nums text-(--site-muted) ${rowStart(row(hour))}`}
          >
            {String(hour / 60).padStart(2, "0")}:00
          </div>
        ))}
        {gaps.map((hour) => (
          <div key={`gap-${hour}`} aria-hidden className={`col-span-full h-3 self-center border-t border-dashed border-(--site-line) ${rowStart((firstRow.get(hour) ?? 2) - 1)}`} />
        ))}
        {blocks.map((block) => (
          <div
            key={`${block.name}-${block.day}-${block.start}`}
            className={`${COLUMN[block.day - 1]} ${rowStart(row(minutes(block.start)))} ${rowSpan((minutes(block.end) - minutes(block.start)) / 30)} z-10 m-px flex flex-col gap-0.5 overflow-hidden rounded-md px-3 py-2 ${block.tone}`}
          >
            <span className="font-semibold leading-tight">{block.name}</span>
            <span className="text-sm leading-tight">{block.detail}</span>
            <span className="text-sm tabular-nums leading-tight">
              {block.start}–{block.end}
            </span>
          </div>
        ))}
      </div>
      <ol className="flex flex-col gap-6 md:hidden">
        {DAYS.map((day, index) => {
          const ofDay = blocks.filter((block) => block.day === index + 1);
          return ofDay.length > 0 ? (
            <li key={day} className="flex flex-col gap-2">
              <span className="font-display text-xl font-bold">{day}</span>
              {ofDay.map((block) => (
                <div key={`${block.name}-${block.start}`} className={`flex items-baseline justify-between gap-4 rounded-md px-4 py-3 ${block.tone}`}>
                  <span>
                    <span className="font-semibold">{block.name}</span>
                    <span className="block text-sm">{block.detail}</span>
                  </span>
                  <span className="shrink-0 text-sm tabular-nums">
                    {block.start}–{block.end}
                  </span>
                </div>
              ))}
            </li>
          ) : null;
        })}
      </ol>
    </div>
  );
}

/*
 * Written out so Tailwind finds them in the source; the full ranges are also generated by
 * @source inline in globals.css, which the teacher's agenda already needs.
 */
function rowStart(value: number) {
  return `row-start-${value}`;
}

function rowSpan(value: number) {
  return `row-span-${value}`;
}
