"use client";

import { useState } from "react";
import { useRenameCourt } from "@/modules/lesson/api";
import { useCourts, type Court } from "@/shared/courts";
import { Button } from "@/shared/ui/Button";
import { ErrorNotice } from "@/shared/ui/Notice";

/** The two tennis courts (40-fase25.6): the teacher names them; adding or removing them comes with the club. */
export function CourtsManager() {
  const courts = useCourts();
  return (
    <section aria-labelledby="courts-heading" className="flex flex-col gap-3">
      <div className="flex flex-col gap-1">
        <h2 id="courts-heading" className="font-display text-2xl font-semibold">Pistas</h2>
        <p className="text-sm text-muted">Cada clase dice en qué pista es. Las nuevas van a la primera si no eliges otra.</p>
      </div>
      <ErrorNotice error={courts.error} />
      <ul className="flex flex-col divide-y divide-line-soft rounded-xl border border-line bg-paper">
        {courts.data?.map((court) => <CourtRow key={court.id} court={court} />)}
      </ul>
    </section>
  );
}

function CourtRow({ court }: { court: Court }) {
  const rename = useRenameCourt();
  const [name, setName] = useState(court.name);
  const changed = name.trim() !== "" && name.trim() !== court.name;
  return (
    <li className="flex flex-wrap items-center gap-2 px-3 py-2">
      <label className="sr-only" htmlFor={`court-${court.id}`}>Nombre de la pista {court.position}</label>
      <input id={`court-${court.id}`} value={name} maxLength={60} onChange={(event) => setName(event.target.value)}
        className="min-h-11 min-w-40 flex-1 rounded-lg border border-transparent bg-transparent px-2 font-semibold hover:border-line focus:border-court" />
      {changed ? (
        <Button variant="quiet" pending={rename.isPending} onClick={() => rename.mutate({ id: court.id, name: name.trim() })}>
          Guardar
        </Button>
      ) : null}
      {rename.error ? <span className="w-full"><ErrorNotice error={rename.error} /></span> : null}
    </li>
  );
}
