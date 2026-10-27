"use client";

import Link from "next/link";
import { useState } from "react";
import { useMyMessages, type ReceivedMessage } from "@/modules/messages/api";
import { formatDateTime } from "@/shared/time";
import { Button } from "@/shared/ui/Button";
import { EmptyState } from "@/shared/ui/EmptyState";
import { ErrorNotice } from "@/shared/ui/Notice";

const WEEK_MS = 7 * 24 * 60 * 60 * 1000;

/** The student's board: what the teacher sent them, newest first (37-fase24, decision 5). */
export function MessageBoard({ zone, teacher }: { zone: string; teacher: string }) {
  const [page, setPage] = useState(0);
  const messages = useMyMessages(page);

  if (messages.error) {
    return <ErrorNotice error={messages.error} />;
  }
  if (!messages.data) {
    return <p className="text-muted">Cargando…</p>;
  }
  const { items, size, totalItems } = messages.data;
  if (items.length === 0) {
    return (
      <EmptyState title="Todavía no tienes avisos">
        Cuando {teacher} escriba a tu grupo o a tu nivel, lo verás aquí y te llegará por correo.
      </EmptyState>
    );
  }
  return (
    <>
      <ul className="flex max-w-3xl flex-col gap-3">
        {items.map((message) => (
          <li key={message.id}>
            <MessageCard message={message} zone={zone} />
          </li>
        ))}
      </ul>
      {totalItems > size ? (
        <div className="flex gap-2">
          <Button variant="quiet" disabled={page === 0} onClick={() => setPage(page - 1)}>Más recientes</Button>
          <Button variant="quiet" disabled={(page + 1) * size >= totalItems} onClick={() => setPage(page + 1)}>
            Anteriores
          </Button>
        </div>
      ) : null}
    </>
  );
}

function MessageCard({ message, zone, clamp = false }: { message: ReceivedMessage; zone: string; clamp?: boolean }) {
  return (
    <article className="flex flex-col gap-2 rounded-xl border border-line border-l-4 border-l-court bg-paper px-5 py-4">
      <div className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1">
        <h2 className="font-display text-xl font-semibold">{message.subject}</h2>
        <p className="text-sm text-muted first-letter:uppercase">{formatDateTime(message.sentAt, zone)}</p>
      </div>
      <p className={`whitespace-pre-line ${clamp ? "line-clamp-3" : ""}`}>{message.body}</p>
    </article>
  );
}

/** On the student's home: the newest message of the last seven days, if there is one. */
export function LatestMessage({ zone, now }: { zone: string; now: number }) {
  const latest = useMyMessages(0, 1).data?.items[0];
  if (!latest || now - new Date(latest.sentAt).getTime() > WEEK_MS) {
    return null;
  }
  return (
    <section aria-labelledby="latest-message" className="flex flex-col gap-3">
      <div className="flex items-baseline justify-between gap-3">
        <h2 id="latest-message" className="font-display text-2xl font-semibold">Último aviso</h2>
        <Link href="/messages" className="text-sm font-semibold text-court">Ver todos</Link>
      </div>
      <MessageCard message={latest} zone={zone} clamp />
    </section>
  );
}
