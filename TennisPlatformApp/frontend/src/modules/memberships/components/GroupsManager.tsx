"use client";

import { useState, type FormEvent } from "react";
import {
  describeSeries, useAddMember, useCreateGroup, useDeleteGroup, useRemoveMember, useTeacherGroups, useUpdateGroup,
  type FixedGroup,
} from "@/modules/memberships/api";
import { centsOf, formatEuros } from "@/shared/money";
import { Button } from "@/shared/ui/Button";
import { CONTROL_CLASS, Field } from "@/shared/ui/Field";
import { ErrorNotice } from "@/shared/ui/Notice";

export interface StudentOption {
  userId: string;
  name: string;
}

interface GroupsManagerProps {
  zone: string;
  nameOf: (userId: string) => string;
  students: StudentOption[];
}

/**
 * The fixed groups (decision 1 of 43-fase28): a name, a monthly fee and the weekly series it
 * takes up. A series is attached from its lesson, where the teacher sees which one it is.
 */
export function GroupsManager({ zone, nameOf, students }: GroupsManagerProps) {
  const groups = useTeacherGroups();
  const create = useCreateGroup();

  function add(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    const monthlyFeeCents = centsOf(String(data.get("fee")));
    if (monthlyFeeCents === null) {
      return;
    }
    create.mutate({ name: String(data.get("name")).trim(), monthlyFeeCents }, { onSuccess: () => form.reset() });
  }

  return (
    <section aria-labelledby="groups-heading" className="flex flex-col gap-4 rounded-xl border border-line bg-paper p-5">
      <div className="flex flex-col gap-1">
        <h2 id="groups-heading" className="font-display text-2xl font-semibold leading-none">Grupos fijos</h2>
        <p className="text-sm text-muted">
          Quien se apunta tiene plaza en todas las clases que quedan de sus series y paga una cuota al mes, sin bono.
          Para añadir una serie a un grupo, abre una de sus clases en la agenda.
        </p>
      </div>
      <ErrorNotice error={groups.error} />
      {groups.data && groups.data.length === 0 ? <p className="text-muted">Todavía no tienes grupos fijos.</p> : null}
      {groups.data?.map((group) => (
        <GroupCard key={group.id} group={group} zone={zone} nameOf={nameOf} students={students} />
      ))}
      <form onSubmit={add} className="grid gap-3 sm:grid-cols-[2fr_1fr_auto] sm:items-end">
        <Field label="Nombre del grupo" name="name" maxLength={60} required placeholder="Martes y jueves, 18:00" />
        <Field label="Cuota al mes (€)" name="fee" inputMode="decimal" pattern="[0-9]+([.,][0-9]{1,2})?" required />
        <Button type="submit" variant="quiet" pending={create.isPending}>Crear grupo</Button>
      </form>
      <ErrorNotice error={create.error} />
    </section>
  );
}

function GroupCard({ group, zone, nameOf, students }: { group: FixedGroup } & GroupsManagerProps) {
  const update = useUpdateGroup();
  const remove = useDeleteGroup();
  const addMember = useAddMember();
  const removeMember = useRemoveMember();
  const [name, setName] = useState(group.name);
  const [fee, setFee] = useState(String(group.monthlyFeeCents / 100).replace(".", ","));
  const [adding, setAdding] = useState("");
  const typedFee = centsOf(fee);
  const changed = (name.trim() !== "" && name.trim() !== group.name) || (typedFee !== null && typedFee !== group.monthlyFeeCents);
  const members = group.members.filter((member) => member.leftAt === null);
  const leaving = group.members.filter((member) => member.leftAt !== null);
  const inGroup = new Set(members.map((member) => member.studentUserId));
  const candidates = students.filter((student) => !inGroup.has(student.userId));

  return (
    <article aria-label={group.name} className="flex flex-col gap-3 rounded-lg border border-line p-4">
      <div className="flex flex-wrap items-end gap-2">
        <div className="flex min-w-48 flex-1 flex-col gap-1">
          <label htmlFor={`group-name-${group.id}`} className="text-sm font-semibold">Nombre</label>
          <input id={`group-name-${group.id}`} value={name} maxLength={60} onChange={(event) => setName(event.target.value)}
            className={CONTROL_CLASS} />
        </div>
        <div className="flex flex-col gap-1">
          <label htmlFor={`group-fee-${group.id}`} className="text-sm font-semibold">Cuota (€)</label>
          <input id={`group-fee-${group.id}`} value={fee} inputMode="decimal" onChange={(event) => setFee(event.target.value)}
            className={`${CONTROL_CLASS} w-28`} />
        </div>
        {changed ? (
          <Button variant="quiet" pending={update.isPending}
            onClick={() => update.mutate({ id: group.id, name: name.trim() || undefined, monthlyFeeCents: typedFee ?? undefined })}>
            Guardar
          </Button>
        ) : null}
        {group.memberCount === 0 ? (
          <Button variant="danger" pending={remove.isPending} onClick={() => remove.mutate(group.id)}>Borrar</Button>
        ) : null}
      </div>
      {changed && typedFee !== group.monthlyFeeCents ? (
        <p className="text-sm text-muted">La cuota nueva vale para los meses que aún no has apuntado.</p>
      ) : null}
      <ErrorNotice error={update.error ?? remove.error} />

      <div className="flex flex-col gap-1">
        <h3 className="font-semibold">Series</h3>
        {group.series.length === 0 ? (
          <p className="text-sm text-muted">Ninguna todavía. Abre una clase de la serie en la agenda y añádela desde allí.</p>
        ) : (
          <ul className="text-sm">
            {group.series.map((series) => (
              <li key={series.seriesId}>
                {describeSeries(series, zone)} · quedan {series.upcomingLessons}{" "}
                {series.upcomingLessons === 1 ? "clase" : "clases"}
              </li>
            ))}
          </ul>
        )}
        {group.freeSeats !== null ? (
          <p className="text-sm text-muted">
            {group.freeSeats === 0 ? "Sin plazas libres" : `${group.freeSeats} ${group.freeSeats === 1 ? "plaza libre" : "plazas libres"}`}
            {" "}en la clase más llena.
          </p>
        ) : null}
      </div>

      <div className="flex flex-col gap-2">
        <h3 className="font-semibold">Apuntados ({members.length})</h3>
        {members.length > 0 ? (
          <ul className="flex flex-col divide-y divide-line-soft rounded-lg border border-line-soft">
            {members.map((member) => (
              <li key={member.membershipId} className="flex items-center gap-2 px-3 py-1">
                <span className="flex-1">{nameOf(member.studentUserId)}</span>
                <Button variant="quiet"
                  pending={removeMember.isPending && removeMember.variables?.studentUserId === member.studentUserId}
                  onClick={() => removeMember.mutate({ groupId: group.id, studentUserId: member.studentUserId })}>
                  Dar de baja
                </Button>
              </li>
            ))}
          </ul>
        ) : null}
        {leaving.length > 0 ? (
          <p className="text-sm text-muted">
            Se van a fin de mes: {leaving.map((member) => nameOf(member.studentUserId)).join(", ")}.
          </p>
        ) : null}
        {group.series.length > 0 && candidates.length > 0 ? (
          <div className="flex flex-wrap items-end gap-2">
            <div className="flex flex-col gap-1">
              <label htmlFor={`group-add-${group.id}`} className="text-sm font-semibold">Apuntar a</label>
              <select id={`group-add-${group.id}`} value={adding} onChange={(event) => setAdding(event.target.value)}
                className={CONTROL_CLASS}>
                <option value="">Elige un alumno</option>
                {candidates.map((student) => <option key={student.userId} value={student.userId}>{student.name}</option>)}
              </select>
            </div>
            <Button variant="quiet" disabled={!adding} pending={addMember.isPending}
              onClick={() => addMember.mutate({ groupId: group.id, studentUserId: adding }, { onSuccess: () => setAdding("") })}>
              Apuntar
            </Button>
          </div>
        ) : null}
        <ErrorNotice error={addMember.error ?? removeMember.error} />
        <p className="text-sm text-muted">Dar de baja deja al alumno en sus clases hasta fin de mes.</p>
      </div>
      <p className="text-sm text-muted">{formatEuros(group.monthlyFeeCents)} al mes.</p>
    </article>
  );
}
