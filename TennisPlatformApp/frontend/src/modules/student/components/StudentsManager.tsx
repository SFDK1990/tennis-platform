"use client";

import Link from "next/link";
import { useState, type FormEvent } from "react";
import { useManagedStudents, useManageStudent, useStopManaging, useStudentLookup } from "@/modules/student/api";
import { StudentAvatar } from "@/modules/student/components/StudentAvatar";
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
    <div className="flex max-w-3xl flex-col gap-8">
      <section className="flex flex-col gap-3">
        <h2 className="font-display text-2xl font-semibold">Añadir un alumno</h2>
        <p className="text-muted">El alumno se registra primero y rellena su perfil. Después lo buscas por su email.</p>
        <form onSubmit={search} className="flex flex-wrap items-end gap-3">
          <Field label="Email del alumno" name="email" type="email" required className="min-w-64 flex-1" />
          <Button type="submit" variant="quiet" pending={lookup.isFetching}>Buscar</Button>
        </form>
        <ErrorNotice error={lookup.error} />
        {found ? (
          <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-line bg-paper px-4 py-3">
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
          <ul className="flex flex-col divide-y divide-line rounded-xl border border-line bg-paper">
            {active.map((student) => (
              <li key={student.userId} className="flex flex-wrap items-center justify-between gap-3 px-4 py-3">
                <span className="flex min-w-0 items-center gap-3">
                  <StudentAvatar userId={student.userId} name={student.fullName} hasPhoto={student.hasPhoto} />
                  <span className="min-w-0">
                    <Link href={`/teacher/students/${student.userId}`} className="block font-semibold text-court underline-offset-4 hover:underline">
                      {student.fullName}
                    </Link>
                    <span className="block truncate text-sm text-muted">{student.email}</span>
                  </span>
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
