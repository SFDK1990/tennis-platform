"use client";

import { useState } from "react";
import { addMonths, formatMonth, monthIn, overdue, useChangeDueDay, useFees } from "@/modules/memberships/api";
import { FeeRow } from "@/modules/memberships/components/FeeRow";
import { formatEuros } from "@/shared/money";
import { Button } from "@/shared/ui/Button";
import { CONTROL_CLASS } from "@/shared/ui/Field";
import { Icon } from "@/shared/ui/Icon";
import { ErrorNotice } from "@/shared/ui/Notice";

const DAYS = Array.from({ length: 28 }, (_, index) => index + 1);

/**
 * The months of the members, one month at a time, and the day of the month they fall due on
 * (decision 3 of 43-fase28). The next month is there too, for whoever pays ahead.
 */
export function FeesBoard({ zone, nameOf }: { zone: string; nameOf: (userId: string) => string }) {
  const current = monthIn(zone);
  const [month, setMonth] = useState(current);
  const fees = useFees({ month });
  const changeDueDay = useChangeDueDay();
  const lines = fees.data?.items ?? [];
  const collected = lines.reduce((sum, line) => sum + (line.fee?.kind === "PAID" ? line.fee.amountCents : 0), 0);
  const late = overdue(lines).length;

  return (
    <section aria-labelledby="fees-heading" className="flex flex-col gap-4 rounded-xl border border-line bg-paper p-5">
      <div className="flex flex-wrap items-center gap-3">
        <h2 id="fees-heading" className="font-display text-2xl font-semibold leading-none first-letter:uppercase">
          {formatMonth(month)}
        </h2>
        <div className="ml-auto flex gap-1">
          <Button variant="quiet" aria-label="Mes anterior" onClick={() => setMonth(addMonths(month, -1))}>
            <Icon name="chevronLeft" />
          </Button>
          <Button variant="quiet" aria-label="Mes siguiente" disabled={month > current}
            onClick={() => setMonth(addMonths(month, 1))}>
            <Icon name="chevronRight" />
          </Button>
        </div>
      </div>
      <p className="text-sm text-muted">
        {lines.length === 0 ? "Nadie debe este mes." : `${formatEuros(collected)} cobrados de ${lines.length} ${lines.length === 1 ? "cuota" : "cuotas"}.`}
        {late > 0 ? ` ${late} sin pagar pasado el día de vencimiento.` : ""}
      </p>
      <ErrorNotice error={fees.error} />
      {lines.length > 0 ? (
        <ul className="flex flex-col divide-y divide-line-soft rounded-lg border border-line">
          {lines.map((line) => (
            <FeeRow key={`${line.membershipId}-${line.month}`} line={line} zone={zone} title={nameOf(line.studentUserId)} />
          ))}
        </ul>
      ) : null}
      <div className="flex flex-wrap items-center gap-2 border-t border-line-soft pt-4">
        <label htmlFor="due-day" className="text-sm font-semibold">La cuota vence el día</label>
        {fees.data ? (
          <select id="due-day" className={`${CONTROL_CLASS} w-20`} value={fees.data.dueDay} disabled={changeDueDay.isPending}
            onChange={(event) => changeDueDay.mutate(Number(event.target.value))}>
            {DAYS.map((day) => <option key={day} value={day}>{day}</option>)}
          </select>
        ) : null}
        <span className="text-sm text-muted">de cada mes. Quien se apunta después de ese día, lo paga el día que se apunta.</span>
        <ErrorNotice error={changeDueDay.error} />
      </div>
    </section>
  );
}
