"use client";

import type { FormEvent } from "react";
import { useUpdateMe, type Me } from "@/modules/identity/api";
import { fieldErrorOf } from "@/shared/api/errors";
import { Button } from "@/shared/ui/Button";
import { Field } from "@/shared/ui/Field";
import { ErrorNotice, Notice } from "@/shared/ui/Notice";

/** Only the fields of the caller's role: sending another role's is a 400 by design. */
const FIELDS_BY_ROLE: Record<string, { name: keyof Me & string; label: string; required?: boolean; hint?: string }[]> = {
  STUDENT: [
    { name: "fullName", label: "Nombre y apellidos", required: true },
    { name: "phone", label: "Teléfono" },
    { name: "nationalId", label: "DNI", hint: "Sólo lo ve tu profesor." },
    { name: "address", label: "Dirección", hint: "Sólo la ve tu profesor." },
  ],
  TEACHER: [
    { name: "displayName", label: "Nombre visible", required: true },
    { name: "phone", label: "Teléfono" },
    { name: "timezone", label: "Zona horaria", required: true, hint: "Por ejemplo, Europe/Madrid." },
  ],
};

export function ProfileForm({ me }: { me: Me }) {
  const update = useUpdateMe();
  const fields = FIELDS_BY_ROLE[me.role] ?? [];

  if (fields.length === 0) {
    return <p className="text-muted">{me.email}. Una cuenta de administración no tiene datos de perfil.</p>;
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    update.mutate(Object.fromEntries(fields.map(({ name }) => [name, String(form.get(name) ?? "")])));
  }

  return (
    <form onSubmit={submit} className="flex flex-col gap-4">
      <p className="text-muted">{me.email}</p>
      {fields.map(({ name, label, required, hint }) => (
        <Field key={name} label={label} name={name} required={required} hint={hint}
          defaultValue={(me[name] as string | null) ?? ""} error={fieldErrorOf(update.error, name)} />
      ))}
      <ErrorNotice error={update.error} />
      {update.isSuccess ? <Notice tone="success">Datos guardados.</Notice> : null}
      <Button type="submit" pending={update.isPending} className="self-start">Guardar datos</Button>
    </form>
  );
}
