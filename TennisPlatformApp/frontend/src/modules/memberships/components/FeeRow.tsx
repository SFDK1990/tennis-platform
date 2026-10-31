"use client";

import { useState, type FormEvent } from "react";
import {
  FEE_PAYMENT_METHODS, FEE_STATUS, formatMonth, useRecordFee, useVoidFee, type FeeLine, type FeePaymentMethod,
} from "@/modules/memberships/api";
import { centsOf, formatEuros } from "@/shared/money";
import { formatShortDay, todayIn } from "@/shared/time";
import { Button } from "@/shared/ui/Button";
import { Chip } from "@/shared/ui/Chip";
import { CONTROL_CLASS } from "@/shared/ui/Field";
import { ErrorNotice } from "@/shared/ui/Notice";

interface FeeRowProps {
  line: FeeLine;
  zone: string;
  /** Who the line is about, in the list of the month; the month itself, in a student's record. */
  title: string;
}

/**
 * One month of one member: what it says, and what the teacher writes down about it. A recorded
 * month is undone with "Anular" and written again (decision 3 of 43-fase28): nothing is edited
 * in place, so what was collected stays traceable.
 */
export function FeeRow({ line, zone, title }: FeeRowProps) {
  const record = useRecordFee();
  const voidFee = useVoidFee();
  const [paying, setPaying] = useState(false);
  const status = FEE_STATUS[line.status];

  function pay(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    const amountCents = centsOf(String(data.get("amount")));
    if (amountCents === null) {
      return;
    }
    record.mutate({
      membershipId: line.membershipId, month: line.month, kind: "PAID", amountCents,
      paidOn: todayIn(zone), paymentMethod: String(data.get("method")) as FeePaymentMethod,
    }, { onSuccess: () => setPaying(false) });
  }

  return (
    <li className="flex flex-col gap-2 px-3 py-3" data-testid="fee-line">
      <div className="flex flex-wrap items-center gap-2">
        <div className="flex min-w-48 flex-1 flex-col">
          <span className="font-semibold">{title}</span>
          <span className="text-sm text-muted">
            {line.groupName}, {formatEuros(line.monthlyFeeCents)} · {describe(line)}
          </span>
        </div>
        <Chip tone={status.tone}>{status.label}</Chip>
        {line.fee ? (
          <Button variant="quiet" pending={voidFee.isPending} onClick={() => voidFee.mutate(line.fee!.id)}>Anular</Button>
        ) : paying ? null : (
          <>
            <Button variant="quiet" onClick={() => setPaying(true)}>Apuntar pago</Button>
            <Button variant="quiet" pending={record.isPending && record.variables?.kind === "WAIVED"}
              onClick={() => record.mutate({ membershipId: line.membershipId, month: line.month, kind: "WAIVED" })}>
              No se cobra
            </Button>
          </>
        )}
      </div>
      {paying && !line.fee ? (
        <form onSubmit={pay} className="flex flex-wrap items-end gap-2">
          <div className="flex flex-col gap-1">
            <label htmlFor={`fee-amount-${line.membershipId}-${line.month}`} className="text-sm font-semibold">Importe (€)</label>
            <input id={`fee-amount-${line.membershipId}-${line.month}`} name="amount" inputMode="decimal" required
              pattern="[0-9]+([.,][0-9]{1,2})?" defaultValue={String(line.monthlyFeeCents / 100).replace(".", ",")}
              className={`${CONTROL_CLASS} w-28`} />
          </div>
          <div className="flex flex-col gap-1">
            <label htmlFor={`fee-method-${line.membershipId}-${line.month}`} className="text-sm font-semibold">Cómo</label>
            <select id={`fee-method-${line.membershipId}-${line.month}`} name="method" defaultValue="CASH" className={CONTROL_CLASS}>
              {Object.entries(FEE_PAYMENT_METHODS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
            </select>
          </div>
          <Button type="submit" pending={record.isPending}>Pagado hoy</Button>
          <Button type="button" variant="quiet" onClick={() => setPaying(false)}>No</Button>
        </form>
      ) : null}
      <ErrorNotice error={record.error ?? voidFee.error} />
    </li>
  );
}

/** "pagada el 3 nov., Bizum, 45 €" or "vence el 5 nov.". */
function describe(line: FeeLine): string {
  const fee = line.fee;
  if (fee?.kind === "PAID" && fee.paidOn && fee.paymentMethod) {
    return `pagada el ${formatShortDay(fee.paidOn)}, ${FEE_PAYMENT_METHODS[fee.paymentMethod].toLowerCase()}, ${formatEuros(fee.amountCents)}`;
  }
  if (fee?.kind === "WAIVED") {
    return `${formatMonth(line.month)} no se cobra`;
  }
  return `vence el ${formatShortDay(line.dueOn)}`;
}
