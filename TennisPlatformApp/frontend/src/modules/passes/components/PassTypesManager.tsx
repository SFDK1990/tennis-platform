"use client";

import { useState, type FormEvent } from "react";
import {
  describeType, useCreatePassType, useDeletePassType, usePassTypes, useRequirePasses, useSellOnline, useUpdatePassType,
  type LessonKind, type PassType,
} from "@/modules/passes/api";
import { ApiError } from "@/shared/api/errors";
import { Button } from "@/shared/ui/Button";
import { Chip } from "@/shared/ui/Chip";
import { CONTROL_CLASS, Field } from "@/shared/ui/Field";
import { ErrorNotice } from "@/shared/ui/Notice";

/**
 * The teacher's pass types and their two switches: the one that makes them required (41-fase27,
 * decisions 2 and 6) and the one that sells them in the app (42-fase27.5, decision 2). A type
 * somebody bought cannot be deleted, only retired: the message says so.
 */
export function PassTypesManager() {
  const types = usePassTypes();
  const requirement = useRequirePasses();
  const sellOnline = useSellOnline();
  const create = useCreatePassType();
  const remove = useDeletePassType();
  const inUse = remove.error instanceof ApiError && remove.error.code === "PASS_TYPE_IN_USE";
  const required = types.data?.required ?? false;
  const online = types.data?.onlinePayment ?? false;
  const onlineAvailable = types.data?.onlinePaymentAvailable ?? false;

  function add(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    const months = String(data.get("validityMonths")).trim();
    create.mutate({
      name: String(data.get("name")).trim(),
      lessonType: String(data.get("lessonType")) as LessonKind,
      lessons: Number(data.get("lessons")),
      priceCents: Math.round(Number(String(data.get("price")).replace(",", ".")) * 100),
      validityMonths: months ? Number(months) : null,
    }, { onSuccess: () => form.reset() });
  }

  return (
    <div className="flex max-w-3xl flex-col gap-6">
      <section aria-labelledby="switch-heading" className="flex flex-col gap-3 rounded-xl border border-line bg-paper p-5">
        <h2 id="switch-heading" className="font-display text-2xl font-semibold leading-none">Pedir bono para reservar</h2>
        <p className="text-sm text-muted">
          Encendido, un alumno sólo reserva si le quedan clases en un bono de ese tipo de clase. Enciéndelo cuando
          hayas apuntado los bonos de tus alumnos: hasta entonces se reserva como siempre.
        </p>
        <label className="flex min-h-11 items-center gap-3 font-semibold">
          <input type="checkbox" className="size-5 accent-court" checked={required} disabled={!types.data || requirement.isPending}
            onChange={(event) => requirement.mutate(event.target.checked)} />
          Sin bono no se reserva
        </label>
        <ErrorNotice error={requirement.error} />
      </section>

      <section aria-labelledby="online-heading" className="flex flex-col gap-3 rounded-xl border border-line bg-paper p-5">
        <h2 id="online-heading" className="font-display text-2xl font-semibold leading-none">Cobrar los bonos en la app</h2>
        <p className="text-sm text-muted">
          Encendido, tus alumnos compran cualquier tipo de bono que no esté retirado y lo pagan con tarjeta o Bizum en la
          página de Stripe. El bono aparece solo, como pagado en la app, en cuanto Stripe confirma el pago. La comisión de
          Stripe la pagas tú; las devoluciones se hacen desde su panel.
        </p>
        <label className="flex min-h-11 items-center gap-3 font-semibold">
          <input type="checkbox" className="size-5 accent-court" checked={online}
            disabled={!types.data || sellOnline.isPending || (!online && !onlineAvailable)}
            onChange={(event) => sellOnline.mutate(event.target.checked)} />
          Vender bonos en la app
        </label>
        {types.data && !onlineAvailable ? (
          <p className="text-sm text-muted">
            Para encenderlo, el servidor necesita las claves de tu cuenta de Stripe (STRIPE_SECRET_KEY y
            STRIPE_WEBHOOK_SECRET).
          </p>
        ) : null}
        <ErrorNotice error={sellOnline.error} />
      </section>

      <section aria-labelledby="types-heading" className="flex flex-col gap-4 rounded-xl border border-line bg-paper p-5">
        <div className="flex flex-col gap-1">
          <h2 id="types-heading" className="font-display text-2xl font-semibold leading-none">Tipos de bono</h2>
          <p className="text-sm text-muted">
            Cada bono es para clases de grupo o para particulares. Cambiar un tipo no cambia los bonos ya vendidos.
          </p>
        </div>
        <ErrorNotice error={types.error} />
        {types.data && types.data.passTypes.length === 0 ? (
          <p className="text-muted">Todavía no tienes tipos de bono.</p>
        ) : (
          <ul className="flex flex-col divide-y divide-line-soft rounded-lg border border-line">
            {types.data?.passTypes.map((type) => (
              <PassTypeRow key={type.id} type={type} onDelete={() => remove.mutate(type.id)}
                deleting={remove.isPending && remove.variables === type.id} />
            ))}
          </ul>
        )}
        {inUse ? (
          <p role="alert" className="text-sm text-fault">
            Algún alumno tiene un bono de ese tipo. Retíralo para que no se venda más.
          </p>
        ) : <ErrorNotice error={remove.error} />}

        <form onSubmit={add} className="grid gap-3 sm:grid-cols-2">
          <Field label="Nombre" name="name" maxLength={60} required placeholder="10 clases de grupo" />
          <div className="flex flex-col gap-1">
            <label htmlFor="pass-type-kind" className="text-sm font-semibold">Para clases</label>
            <select id="pass-type-kind" name="lessonType" className={CONTROL_CLASS} defaultValue="GROUP">
              <option value="GROUP">De grupo</option>
              <option value="INDIVIDUAL">Particulares</option>
            </select>
          </div>
          <Field label="Número de clases" name="lessons" type="number" min={1} max={200} required />
          <Field label="Precio (€)" name="price" inputMode="decimal" pattern="[0-9]+([.,][0-9]{1,2})?" required />
          <Field label="Caduca a los (meses)" name="validityMonths" type="number" min={1} max={36}
            hint="Vacío si no caduca. Cuenta desde el día del pago." />
          <div className="flex items-end">
            <Button type="submit" variant="quiet" pending={create.isPending}>Añadir tipo</Button>
          </div>
        </form>
        <ErrorNotice error={create.error} />
      </section>
    </div>
  );
}

function PassTypeRow({ type, deleting, onDelete }: { type: PassType; deleting: boolean; onDelete: () => void }) {
  const update = useUpdatePassType();
  const [name, setName] = useState(type.name);
  const renamed = name.trim() !== "" && name.trim() !== type.name;

  return (
    <li className="flex flex-wrap items-center gap-2 px-3 py-2">
      <div className="flex min-w-48 flex-1 flex-col">
        <label className="sr-only" htmlFor={`pass-type-${type.id}`}>Nombre del tipo de bono</label>
        <input id={`pass-type-${type.id}`} value={name} maxLength={60} onChange={(event) => setName(event.target.value)}
          className="min-h-11 rounded-lg border border-transparent bg-transparent px-2 font-semibold hover:border-line focus:border-court" />
        <span className="px-2 text-sm text-muted">{describeType(type)}</span>
      </div>
      {type.retired ? <Chip>Retirado</Chip> : null}
      {renamed ? (
        <Button variant="quiet" pending={update.isPending} onClick={() => update.mutate({ id: type.id, name: name.trim() })}>
          Guardar
        </Button>
      ) : null}
      <Button variant="quiet" pending={update.isPending && update.variables?.retired !== undefined}
        onClick={() => update.mutate({ id: type.id, retired: !type.retired })}>
        {type.retired ? "Volver a vender" : "Retirar"}
      </Button>
      <Button variant="danger" pending={deleting} onClick={onDelete}>Borrar</Button>
      {update.error ? <span className="w-full"><ErrorNotice error={update.error} /></span> : null}
    </li>
  );
}
