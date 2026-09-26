"use client";

import { useState, type FormEvent } from "react";
import { useManagedStudents, useManageStudent, useStopManaging, useStudentLookup } from "@/modules/student/api";
import { Button } from "@/shared/ui/Button";
import { Field } from "@/shared/ui/Field";
import { ErrorNotice, Notice } from "@/shared/ui/Notice";

export function StudentsManager() {
  const [email, setEmail] = useState<string | null>(null);
  const lookup = useStudentLookup(email);
  const manage = useManageStudent();
  const stop = useStopManaging();
  const students = useManagedStudents();
  const [leaving, setLeaving] = useState<string | null>(null);

  function search(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    manage.reset();
    setEmail(String(new FormData(event.currentTarget).get("email")).trim());
  }

  const found = lookup.data;
  const active = students.data?.items.filter((student) => student.managedStatus === "MANAGED") ?? [];

  return (
    <div className="flex flex-col gap-8">
      <section className="flex flex-col gap-3">
        <h2 className="font-display text-2xl font-semibold">Añadir un alumno</h2>
        <p className="text-muted">El alumno se registra primero y rellena su perfil. Después lo buscas por su email.</p>
        <form onSubmit={search} className="flex flex-wrap items-end gap-3">
          <Field label="Email del alumno" name="email" type="email" required className="min-w-64 flex-1" />
          <Button type="submit" variant="quiet" pending={lookup.isFetching}>Buscar</Button>
        </form>
        <ErrorNotice error={lookup.error} />
        {found ? (
          <div className="flex flex-wrap items-center justify-between gap-3 rounded-md border border-line bg-paper px-4 py-3">
            <span>
              <span className="font-semibold">{found.fullName ?? "Sin nombre todavía"}</span>{" "}
              <span className="text-muted">{found.email}</span>
            </span>
            {found.managedStatus === "MANAGED" ? (
              <span className="text-surround">Ya es alumno tuyo</span>
            ) : (
              <Button pending={manage.isPending} onClick={() => manage.mutate(found.userId)}>Añadir a mis alumnos</Button>
            )}
          </div>
        ) : null}
        <ErrorNotice error={manage.error} />
        {manage.isSuccess ? <Notice tone="success">Alumno añadido. Ya puede reservar tus clases.</Notice> : null}
      </section>

      <section className="flex flex-col gap-3">
        <h2 className="font-display text-2xl font-semibold">Mis alumnos</h2>
        <ErrorNotice error={students.error ?? stop.error} />
        {active.length === 0 ? (
          <p className="text-muted">Todavía no tienes alumnos.</p>
        ) : (
          <ul className="flex flex-col divide-y divide-line rounded-md border border-line bg-paper">
            {active.map((student) => (
              <li key={student.userId} className="flex flex-wrap items-center justify-between gap-3 px-4 py-3">
                <span>
                  <span className="font-semibold">{student.fullName}</span>{" "}
                  <span className="text-muted">{student.email}</span>
                </span>
                {leaving === student.userId ? (
                  <span className="flex flex-wrap items-center gap-2">
                    <span className="text-sm">Se cancelarán sus reservas futuras.</span>
                    <Button variant="danger" pending={stop.isPending}
                      onClick={() => stop.mutate(student.userId, { onSuccess: () => setLeaving(null) })}>
                      Sí, dar de baja
                    </Button>
                    <Button variant="quiet" onClick={() => setLeaving(null)}>No</Button>
                  </span>
                ) : (
                  <Button variant="quiet" onClick={() => setLeaving(student.userId)}>Dar de baja</Button>
                )}
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
