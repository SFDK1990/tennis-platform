"use client";

import { useState } from "react";
import { describeSeries, formatMonth, useAddMember, useFees, useRemoveMember, useTeacherGroups } from "@/modules/memberships/api";
import { FeeRow } from "@/modules/memberships/components/FeeRow";
import { Button } from "@/shared/ui/Button";
import { CONTROL_CLASS } from "@/shared/ui/Field";
import { ErrorNotice } from "@/shared/ui/Notice";

/**
 * In the student's record: the fixed groups they are in, signing them up or off (decision 2 of
 * 43-fase28), and every month they owe or paid.
 */
export function StudentGroups({ studentUserId, zone }: { studentUserId: string; zone: string }) {
  const groups = useTeacherGroups();
  const fees = useFees({ studentUserId });
  const add = useAddMember();
  const remove = useRemoveMember();
  const [groupId, setGroupId] = useState("");
  const all = groups.data ?? [];
  const mine = all.flatMap((group) => group.members
    .filter((member) => member.studentUserId === studentUserId)
    .map((member) => ({ group, member })));
  const others = all.filter((group) => group.series.length > 0
    && !mine.some(({ group: joined }) => joined.id === group.id));

  return (
    <div className="flex flex-col gap-3">
      <ErrorNotice error={groups.error ?? fees.error} />
      {mine.length === 0 ? <p className="text-muted">No está en ningún grupo fijo.</p> : (
        <ul className="flex flex-col divide-y divide-line-soft rounded-lg border border-line bg-paper">
          {mine.map(({ group, member }) => (
            <li key={member.membershipId} className="flex flex-wrap items-center gap-2 px-3 py-2">
              <div className="flex min-w-48 flex-1 flex-col">
                <span className="font-semibold">{group.name}</span>
                <span className="text-sm text-muted">
                  {group.series.map((series) => describeSeries(series, zone)).join(" · ")}
                  {member.leftAt ? " · se va a fin de mes" : ""}
                </span>
              </div>
              {member.leftAt ? null : (
                <Button variant="quiet" pending={remove.isPending && remove.variables?.groupId === group.id}
                  onClick={() => remove.mutate({ groupId: group.id, studentUserId })}>
                  Dar de baja
                </Button>
              )}
            </li>
          ))}
        </ul>
      )}
      {others.length > 0 ? (
        <div className="flex flex-wrap items-end gap-2">
          <div className="flex flex-col gap-1">
            <label htmlFor="student-group" className="text-sm font-semibold">Apuntar a un grupo</label>
            <select id="student-group" value={groupId} onChange={(event) => setGroupId(event.target.value)} className={CONTROL_CLASS}>
              <option value="">Elige un grupo</option>
              {others.map((group) => <option key={group.id} value={group.id}>{group.name}</option>)}
            </select>
          </div>
          <Button variant="quiet" disabled={!groupId} pending={add.isPending}
            onClick={() => add.mutate({ groupId, studentUserId }, { onSuccess: () => setGroupId("") })}>
            Apuntar
          </Button>
        </div>
      ) : null}
      <ErrorNotice error={add.error ?? remove.error} />
      {fees.data && fees.data.items.length > 0 ? (
        <ul aria-label="Mensualidades" className="flex flex-col divide-y divide-line-soft rounded-lg border border-line bg-paper">
          {fees.data.items.map((line) => (
            <FeeRow key={`${line.membershipId}-${line.month}`} line={line} zone={zone} title={formatMonth(line.month)} />
          ))}
        </ul>
      ) : null}
    </div>
  );
}
