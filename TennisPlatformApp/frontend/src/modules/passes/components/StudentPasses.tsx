"use client";

import { type FormEvent } from "react";
import {
  describeType, formatEuros, KIND_PLURAL, PAYMENT_METHODS, RECORDED_PAYMENT_METHODS, usePassTypes, useRecordPass, useTeacherPasses, useVoidPass,
  type Pass, type RecordedPaymentMethod,
} from "@/modules/passes/api";
import { ApiError } from "@/shared/api/errors";
import { formatShortDay, todayIn } from "@/shared/time";
import { Button } from "@/shared/ui/Button";
import { Chip } from "@/shared/ui/Chip";
import { CONTROL_CLASS, Field } from "@/shared/ui/Field";
import { ErrorNotice } from "@/shared/ui/Notice";

const STATUS: Record<Pass["status"], { label: string; tone: "court" | "neutral" | "warning" | "fault" }> = {
  ACTIVE: { label: "Activo", tone: "court" },
  USED_UP: { label: "Gastado", tone: "neutral" },
  EXPIRED: { label: "Caducado", tone: "warning" },
  VOIDED: { label: "Anulado", tone: "fault" },
};

/**
 * The passes of one student in their record, and the form to write down one they paid
 * (41-fase27, decision 3). The app does not charge: this records what was paid and how.
 */
export function StudentPasses({ studentUserId, zone }: { studentUserId: string; zone: string }) {
  const passes = useTeacherPasses(studentUserId);
  const types = usePassTypes();
  const record = useRecordPass(studentUserId);
  const forSale = types.data?.passTypes.filter((type) => !type.retired) ?? [];

  function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    record.mutate({
      passTypeId: String(data.get("passTypeId")),
      paidOn: String(data.get("paidOn")),
      paymentMethod: String(data.get("paymentMethod")) as RecordedPaymentMethod,
    }, { onSuccess: () => form.reset() });
  }

  return (
    <div className="flex flex-col gap-4">
      <ErrorNotice error={passes.error} />
      {passes.data && passes.data.items.length === 0 ? (
        <p className="text-muted">No tiene ningún bono.</p>
      ) : (
        <ul className="flex flex-col divide-y divide-line-soft rounded-xl border border-line bg-paper">
          {passes.data?.items.map((pass) => <PassRow key={pass.id} pass={pass} />)}
        </ul>
      )}
      {forSale.length === 0 ? (
        <p className="text-sm text-muted">Para apuntar un bono, crea antes sus tipos en Bonos.</p>
      ) : (
        <form onSubmit={save} aria-label="Apuntar un bono" className="grid gap-3 rounded-xl border border-line bg-paper p-4 sm:grid-cols-3">
          <div className="flex flex-col gap-1 sm:col-span-3">
            <label htmlFor="pass-type" className="text-sm font-semibold">Bono</label>
            <select id="pass-type" name="passTypeId" required className={CONTROL_CLASS}>
              {forSale.map((type) => (
                <option key={type.id} value={type.id}>{type.name}: {describeType(type)}</option>
              ))}
            </select>
          </div>
          <Field label="Pagado el" name="paidOn" type="date" required defaultValue={todayIn(zone)} />
          <div className="flex flex-col gap-1">
            <label htmlFor="pass-method" className="text-sm font-semibold">Cómo</label>
            <select id="pass-method" name="paymentMethod" className={CONTROL_CLASS} defaultValue="CASH">
              {Object.entries(RECORDED_PAYMENT_METHODS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
            </select>
          </div>
          <div className="flex items-end">
            <Button type="submit" pending={record.isPending}>Apuntar bono</Button>
          </div>
          <span className="sm:col-span-3"><ErrorNotice error={record.error} /></span>
        </form>
      )}
    </div>
  );
}

function PassRow({ pass }: { pass: Pass }) {
  const voidPass = useVoidPass();
  const used = voidPass.error instanceof ApiError && voidPass.error.code === "PASS_ALREADY_USED";
  const status = STATUS[pass.status];

  return (
    <li className="flex flex-wrap items-center justify-between gap-2 px-4 py-3">
      <span className="flex min-w-0 flex-col">
        <span className="font-semibold">{pass.name}</span>
        <span className="text-sm text-muted">
          Quedan {pass.remaining} de {pass.lessons} clases {KIND_PLURAL[pass.lessonType]}
          {pass.validUntil ? `, hasta el ${formatShortDay(pass.validUntil)}` : ""} · {formatEuros(pass.priceCents)},{" "}
          {PAYMENT_METHODS[pass.paymentMethod].toLowerCase()}, el {formatShortDay(pass.paidOn)}
        </span>
      </span>
      <span className="flex items-center gap-2">
        <Chip tone={status.tone}>{status.label}</Chip>
        {pass.status !== "VOIDED" && pass.used === 0 ? (
          <Button variant="quiet" pending={voidPass.isPending} onClick={() => voidPass.mutate(pass.id)}>Anular</Button>
        ) : null}
      </span>
      {used ? (
        <p role="alert" className="w-full text-sm text-fault">Ya se ha reservado alguna clase con este bono.</p>
      ) : voidPass.error ? <span className="w-full"><ErrorNotice error={voidPass.error} /></span> : null}
    </li>
  );
}
