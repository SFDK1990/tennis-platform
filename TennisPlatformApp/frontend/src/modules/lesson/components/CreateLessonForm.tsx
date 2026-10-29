"use client";

import { useState, type FormEvent } from "react";
import {
  useCreateLesson, useCreateSeries, useSeriesPreview, type CreateLesson, type Occurrence, type SeriesPreviewRequest,
} from "@/modules/lesson/api";
import { ApiError } from "@/shared/api/errors";
import { dateIn, formatDay, formatTime, zonedToInstant, type LocalDate } from "@/shared/time";
import { Button } from "@/shared/ui/Button";
import { CONTROL_CLASS, Field } from "@/shared/ui/Field";
import { CourtSelect } from "@/shared/courts";
import { LevelSelect } from "@/shared/levels";
import { ErrorNotice, Notice } from "@/shared/ui/Notice";

const DURATIONS = [30, 60, 90, 120];
const WEEKS = Array.from({ length: 15 }, (_, index) => index + 2);

const PROBLEM: Record<Occurrence["status"], string | null> = {
  OK: null,
  OUTSIDE_AVAILABILITY: "Fuera de tu horario",
  OVERLAP: "Choca con otra clase",
  IN_THE_PAST: "Ya ha pasado",
};

/** Times are the teacher's wall clock, turned into instants in their zone before sending. */
export function CreateLessonForm({ zone, defaultDate }: { zone: string; defaultDate: LocalDate }) {
  const create = useCreateLesson();
  const preview = useSeriesPreview();
  const [type, setType] = useState<CreateLesson["type"]>("INDIVIDUAL");
  const [repeat, setRepeat] = useState(false);
  const [levelId, setLevelId] = useState<string | null>(null);
  const [courtId, setCourtId] = useState<string | null>(null);
  const [series, setSeries] = useState<SeriesPreviewRequest | null>(null);
  const outsideHours = create.error instanceof ApiError && create.error.code === "LESSON_OUTSIDE_AVAILABILITY";

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const startsAt = zonedToInstant(String(form.get("date")), String(form.get("time")), zone);
    const minutes = Number(form.get("duration"));
    const lesson = {
      type,
      startsAt,
      endsAt: new Date(new Date(startsAt).getTime() + minutes * 60_000).toISOString(),
      capacity: type === "INDIVIDUAL" ? 1 : Number(form.get("capacity")),
      notes: String(form.get("notes") ?? "") || null,
      levelId,
      courtId,
    };
    if (repeat) {
      const request = { ...lesson, weeks: Number(form.get("weeks")) };
      setSeries(request);
      preview.mutate(request);
    } else {
      create.mutate({ ...lesson, overrideAvailability: form.get("override") === "on" });
    }
  }

  if (series && preview.data) {
    return (
      <SeriesDates series={series} occurrences={preview.data} zone={zone}
        onRefresh={() => preview.mutate(series)}
        onBack={() => {
          setSeries(null);
          preview.reset();
        }} />
    );
  }

  return (
    <form onSubmit={submit} className="flex flex-col gap-4">
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        <Field label="Día" name="date" type="date" defaultValue={defaultDate} required />
        <Field label="Hora" name="time" type="time" step={1800} defaultValue="18:00" required />
        <div className="flex flex-col gap-1">
          <label htmlFor="duration" className="text-sm font-semibold">Duración</label>
          <select id="duration" name="duration" defaultValue={60} className={CONTROL_CLASS}>
            {DURATIONS.map((minutes) => <option key={minutes} value={minutes}>{minutes} min</option>)}
          </select>
        </div>
        <fieldset className="flex flex-col gap-1">
          <legend className="text-sm font-semibold">Tipo</legend>
          <div className="flex min-h-10 items-center gap-4">
            {(["INDIVIDUAL", "GROUP"] as const).map((option) => (
              <label key={option} className="flex items-center gap-2">
                <input type="radio" name="type" checked={type === option} onChange={() => setType(option)} />
                {option === "INDIVIDUAL" ? "Individual" : "Grupo"}
              </label>
            ))}
          </div>
        </fieldset>
      </div>
      {type === "GROUP" ? (
        <Field label="Plazas" name="capacity" type="number" min={1} max={8} defaultValue={4} required className="max-w-32" />
      ) : null}
      <div className="flex flex-wrap gap-4">
        <div className="w-64">
          <LevelSelect id="lesson-level" label="Nivel" noneLabel="Abierta a todos" value={levelId} onChange={setLevelId} />
        </div>
        <div className="w-48">
          <CourtSelect id="lesson-court" label="Pista" value={courtId} onChange={setCourtId} />
        </div>
      </div>
      <Field label="Notas" name="notes" hint="Sólo las ves tú." />
      <div className="flex flex-wrap items-center gap-4">
        <label className="flex min-h-10 items-center gap-2">
          <input type="checkbox" checked={repeat} onChange={(event) => setRepeat(event.target.checked)} />
          Repetir cada semana
        </label>
        {repeat ? (
          <div className="flex items-center gap-2">
            <label htmlFor="weeks" className="text-sm font-semibold">Durante</label>
            <select id="weeks" name="weeks" defaultValue={8} className={CONTROL_CLASS}>
              {WEEKS.map((weeks) => <option key={weeks} value={weeks}>{weeks} semanas</option>)}
            </select>
          </div>
        ) : null}
      </div>
      {outsideHours && !repeat ? (
        <label className="flex items-center gap-2">
          <input type="checkbox" name="override" /> Crear fuera de horario
        </label>
      ) : null}
      <ErrorNotice error={repeat ? preview.error : create.error} />
      {create.isSuccess && !repeat ? <Notice tone="success">Clase creada. Ya la ven tus alumnos.</Notice> : null}
      <Button type="submit" pending={create.isPending || preview.isPending} className="self-start">
        {repeat ? "Ver las fechas" : "Crear clase"}
      </Button>
    </form>
  );
}

interface SeriesDatesProps {
  series: SeriesPreviewRequest;
  occurrences: Occurrence[];
  zone: string;
  onRefresh: () => void;
  onBack: () => void;
}

/**
 * Every date of the series before anything is created (35-fase22, decision 2). The ones with a
 * problem start unticked and say why; outside the hours can be ticked anyway, a clash cannot.
 */
function SeriesDates({ series, occurrences, zone, onRefresh, onBack }: SeriesDatesProps) {
  const create = useCreateSeries();
  const [chosen, setChosen] = useState(
    () => new Set(occurrences.filter((occurrence) => occurrence.status === "OK").map((occurrence) => occurrence.startsAt)),
  );

  function toggle(startsAt: string, on: boolean) {
    setChosen((previous) => {
      const next = new Set(previous);
      if (on) next.add(startsAt);
      else next.delete(startsAt);
      return next;
    });
  }

  function confirm() {
    const overrideAvailability = occurrences.some(
      (occurrence) => occurrence.status === "OUTSIDE_AVAILABILITY" && chosen.has(occurrence.startsAt));
    create.mutate({ ...series, dates: [...chosen], overrideAvailability }, {
      // Somebody took one of the dates meanwhile: the list is read again, nothing was created.
      onError: (error) => {
        if (error instanceof ApiError && error.code === "LESSON_OVERLAP") onRefresh();
      },
    });
  }

  if (create.isSuccess) {
    return (
      <div className="flex flex-col gap-3">
        <Notice tone="success">Serie creada: {create.data.length} clases. Ya las ven tus alumnos.</Notice>
        <Button variant="quiet" className="self-start" onClick={onBack}>Crear otra clase</Button>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-4">
      <p className="text-sm text-muted">Marca las fechas que quieres crear. Se crean todas a la vez, o ninguna.</p>
      <ul className="flex flex-col divide-y divide-line-soft rounded-xl border border-line bg-paper">
        {occurrences.map((occurrence) => {
          const problem = PROBLEM[occurrence.status];
          const blocked = occurrence.status === "OVERLAP" || occurrence.status === "IN_THE_PAST";
          return (
            <li key={occurrence.startsAt}>
              <label className={`flex min-h-12 items-center gap-3 px-4 py-2 ${blocked ? "text-muted" : ""}`}>
                <input type="checkbox" disabled={blocked} checked={chosen.has(occurrence.startsAt)}
                  onChange={(event) => toggle(occurrence.startsAt, event.target.checked)} />
                <span className="font-semibold first-letter:uppercase">
                  {formatDay(dateIn(occurrence.startsAt, zone))}, {formatTime(occurrence.startsAt, zone)}
                </span>
                {problem ? <span className="ml-auto text-sm text-muted">{problem}</span> : null}
              </label>
            </li>
          );
        })}
      </ul>
      <ErrorNotice error={create.error} />
      <div className="flex flex-wrap gap-2">
        <Button pending={create.isPending} disabled={chosen.size === 0} onClick={confirm}>
          {chosen.size === 1 ? "Crear 1 clase" : `Crear ${chosen.size} clases`}
        </Button>
        <Button variant="quiet" onClick={onBack}>Cambiar algo</Button>
      </div>
    </div>
  );
}
