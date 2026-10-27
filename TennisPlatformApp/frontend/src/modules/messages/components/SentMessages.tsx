"use client";

import { useState } from "react";
import { useDeleteMessage, useSentMessages, type SentMessage } from "@/modules/messages/api";
import { formatDateTime } from "@/shared/time";
import { Button } from "@/shared/ui/Button";
import { EmptyState } from "@/shared/ui/EmptyState";
import { ErrorNotice } from "@/shared/ui/Notice";

/** What the teacher has sent, with whom it reached when it went out (37-fase24, decision 5). */
export function SentMessages({ zone }: { zone: string }) {
  const [page, setPage] = useState(0);
  const sent = useSentMessages(page);

  if (sent.error) {
    return <ErrorNotice error={sent.error} />;
  }
  if (!sent.data) {
    return <p className="text-muted">Cargando…</p>;
  }
  const { items, size, totalItems } = sent.data;
  return (
    <section aria-labelledby="sent-heading" className="flex flex-col gap-3">
      <h2 id="sent-heading" className="font-display text-2xl font-semibold">Enviados</h2>
      {items.length === 0 ? (
        <EmptyState title="Aún no has escrito a nadie">
          Lo que envíes aparece aquí, con a quién fue.
        </EmptyState>
      ) : (
        <ul className="flex flex-col gap-2">
          {items.map((message) => <SentRow key={message.id} message={message} zone={zone} />)}
        </ul>
      )}
      {totalItems > size ? (
        <div className="flex gap-2">
          <Button variant="quiet" disabled={page === 0} onClick={() => setPage(page - 1)}>Más recientes</Button>
          <Button variant="quiet" disabled={(page + 1) * size >= totalItems} onClick={() => setPage(page + 1)}>
            Anteriores
          </Button>
        </div>
      ) : null}
    </section>
  );
}

function SentRow({ message, zone }: { message: SentMessage; zone: string }) {
  const remove = useDeleteMessage();
  const [confirming, setConfirming] = useState(false);
  const reached = `${message.recipientCount} ${message.recipientCount === 1 ? "alumno" : "alumnos"}`;

  return (
    <li className="flex flex-col gap-2 rounded-xl border border-line bg-paper px-4 py-3">
      <div className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1">
        <h3 className="font-semibold">{message.subject}</h3>
        <p className="text-sm text-muted first-letter:uppercase">{formatDateTime(message.sentAt, zone)}</p>
      </div>
      <p className="text-sm text-muted">Para {lowerFirst(message.audienceLabel)}: {reached}</p>
      <details className="group">
        <summary className="min-h-11 cursor-pointer content-center text-sm font-semibold text-court">
          <span className="group-open:hidden">Leer el mensaje</span>
          <span className="hidden group-open:inline">Ocultar el mensaje</span>
        </summary>
        <p className="whitespace-pre-line pb-2">{message.body}</p>
      </details>
      <div className="flex flex-wrap items-center gap-2">
        {confirming ? (
          <>
            <span className="text-sm">Desaparece de sus avisos; el correo ya les llegó.</span>
            <Button variant="danger" pending={remove.isPending} onClick={() => remove.mutate(message.id)}>
              Quitar
            </Button>
            <Button variant="quiet" onClick={() => setConfirming(false)}>No</Button>
          </>
        ) : (
          <Button variant="quiet" onClick={() => setConfirming(true)}>Quitar de sus avisos</Button>
        )}
      </div>
      <ErrorNotice error={remove.error} />
    </li>
  );
}

/** "Todos los alumnos" reads "Para todos los alumnos"; a level or a lesson keeps its capital. */
function lowerFirst(label: string): string {
  return label.startsWith("Todos") ? label.charAt(0).toLowerCase() + label.slice(1) : label;
}
