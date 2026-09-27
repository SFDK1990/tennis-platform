import Link from "next/link";
import type { ReactNode } from "react";
import type { CalendarLesson } from "@/modules/calendar/api";
import { LESSON_STATUS, LESSON_TITLE, seatsLeft } from "@/modules/calendar/labels";
import { dateIn, formatShortWeekday, formatTime } from "@/shared/time";
import { Chip } from "@/shared/ui/Chip";

interface LessonLineProps {
  lesson: CalendarLesson;
  zone: string;
  /** For lists that span several days. */
  withDay?: boolean;
  /** The whole line opens the lesson; mutually exclusive with actions, a link cannot hold buttons. */
  href?: string;
  actions?: ReactNode;
}

/** One lesson in a short list: its time, what it is, how full, and what can be done. */
export function LessonLine({ lesson, zone, withDay = false, href, actions }: LessonLineProps) {
  const status = LESSON_STATUS[lesson.status];
  const inactive = lesson.status === "CANCELLED" || lesson.status === "COMPLETED";
  const body = (
    <>
      <p className="w-16 shrink-0 font-display leading-tight">
        {withDay ? <span className="block text-sm font-medium text-muted first-letter:uppercase">{formatShortWeekday(dateIn(lesson.startsAt, zone))}</span> : null}
        <span className="block text-2xl font-semibold">{formatTime(lesson.startsAt, zone)}</span>
      </p>
      <div className="min-w-0 flex-1">
        <p className="font-semibold">{LESSON_TITLE[lesson.type]}</p>
        <p className="text-sm text-muted">
          Hasta las {formatTime(lesson.endsAt, zone)} · {seatsLeft(lesson)}
        </p>
      </div>
      {status ? <Chip tone={lesson.status === "FULL" ? "court" : "neutral"}>{status}</Chip> : null}
    </>
  );
  const frame = `flex items-center gap-4 rounded-xl border border-line bg-paper px-4 py-3 ${inactive ? "opacity-60" : ""}`;

  return (
    <li>
      {href ? (
        <Link href={href} className={`${frame} hover:border-court`}>{body}</Link>
      ) : (
        <div className={`${frame} flex-wrap`}>
          {body}
          {actions ? <div className="ml-auto flex shrink-0 justify-end">{actions}</div> : null}
        </div>
      )}
    </li>
  );
}
