import type { CalendarLesson } from "@/modules/calendar/api";

/** Until lessons have a name of their own (Fase 20), their type is what the screens call them. */
export const LESSON_TITLE: Record<CalendarLesson["type"], string> = {
  INDIVIDUAL: "Clase individual",
  GROUP: "Clase de grupo",
};

export const LESSON_STATUS: Partial<Record<CalendarLesson["status"], string>> = {
  FULL: "Completa",
  CANCELLED: "Cancelada",
  COMPLETED: "Terminada",
};

export function seatsLeft({ capacity, bookedCount }: Pick<CalendarLesson, "capacity" | "bookedCount">): string {
  const free = Math.max(capacity - bookedCount, 0);
  return free === 0 ? "Sin plazas" : free === 1 ? "1 plaza libre" : `${free} plazas libres`;
}
