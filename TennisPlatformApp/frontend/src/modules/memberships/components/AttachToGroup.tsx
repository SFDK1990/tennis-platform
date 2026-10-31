"use client";

import { useState } from "react";
import { useAttachSeries, useTeacherGroups } from "@/modules/memberships/api";
import { Button } from "@/shared/ui/Button";
import { CONTROL_CLASS } from "@/shared/ui/Field";
import { ErrorNotice } from "@/shared/ui/Notice";

/**
 * On a lesson of a weekly series: which fixed group the series belongs to, or a way to give it
 * one. Its members get a seat in every lesson left of the series at once.
 */
export function AttachToGroup({ seriesId }: { seriesId: string }) {
  const groups = useTeacherGroups();
  const attach = useAttachSeries();
  const [groupId, setGroupId] = useState("");
  if (!groups.data) {
    return null;
  }
  const owner = groups.data.find((group) => group.series.some((series) => series.seriesId === seriesId));

  return (
    <section aria-labelledby="group-heading" className="flex flex-col gap-2 rounded-xl border border-line bg-paper p-5">
      <h2 id="group-heading" className="font-display text-2xl font-semibold leading-none">Grupo fijo</h2>
      {owner ? (
        <p>Esta serie es del grupo <strong>{owner.name}</strong>: sus apuntados tienen plaza en todas sus clases.</p>
      ) : groups.data.length === 0 ? (
        <p className="text-sm text-muted">Crea un grupo fijo en Mensualidades para que sus alumnos tengan plaza fija aquí.</p>
      ) : (
        <div className="flex flex-wrap items-end gap-2">
          <div className="flex flex-col gap-1">
            <label htmlFor="attach-group" className="text-sm font-semibold">Añadir la serie al grupo</label>
            <select id="attach-group" value={groupId} onChange={(event) => setGroupId(event.target.value)} className={CONTROL_CLASS}>
              <option value="">Elige un grupo</option>
              {groups.data.map((group) => <option key={group.id} value={group.id}>{group.name}</option>)}
            </select>
          </div>
          <Button variant="quiet" disabled={!groupId} pending={attach.isPending}
            onClick={() => attach.mutate({ groupId, seriesId })}>
            Añadir
          </Button>
        </div>
      )}
      <ErrorNotice error={attach.error} />
    </section>
  );
}
