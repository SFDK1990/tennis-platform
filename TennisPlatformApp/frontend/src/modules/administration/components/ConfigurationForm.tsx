"use client";

import type { FormEvent } from "react";
import { useUpdateConfiguration, type PlatformConfiguration } from "@/modules/administration/api";
import { fieldErrorOf } from "@/shared/api/errors";
import { Button } from "@/shared/ui/Button";
import { Field } from "@/shared/ui/Field";
import { ErrorNotice, Notice } from "@/shared/ui/Notice";

export function ConfigurationForm({ configuration }: { configuration: PlatformConfiguration }) {
  const update = useUpdateConfiguration();

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    update.mutate({
      studentLimit: Number(form.get("studentLimit")),
      maxGroupCapacity: Number(form.get("maxGroupCapacity")),
    });
  }

  return (
    <form onSubmit={submit} className="flex flex-col gap-4">
      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Límite de alumnos" name="studentLimit" type="number" min={1} required
          defaultValue={configuration.studentLimit} error={fieldErrorOf(update.error, "studentLimit")}
          hint="Bajarlo no da de baja a nadie: sólo impide añadir alumnos hasta estar por debajo." />
        <Field label="Plazas máximas por clase de grupo" name="maxGroupCapacity" type="number" min={1} required
          defaultValue={configuration.maxGroupCapacity} error={fieldErrorOf(update.error, "maxGroupCapacity")}
          hint="Las clases ya creadas conservan sus plazas." />
      </div>
      <ErrorNotice error={update.error} />
      {update.isSuccess ? <Notice tone="success">Configuración guardada.</Notice> : null}
      <Button type="submit" pending={update.isPending} className="self-start">Guardar configuración</Button>
    </form>
  );
}
