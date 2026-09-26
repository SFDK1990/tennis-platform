"use client";

import { useConfiguration } from "@/modules/administration/api";
import { ConfigurationForm } from "@/modules/administration/components/ConfigurationForm";
import { ErrorNotice } from "@/shared/ui/Notice";

export default function AdminConfigurationPage() {
  const configuration = useConfiguration();
  return (
    <>
      <h1 className="font-display text-3xl font-semibold">Configuración</h1>
      <ErrorNotice error={configuration.error} />
      {configuration.data ? (
        <ConfigurationForm configuration={configuration.data} />
      ) : (
        <p className="text-muted">Cargando…</p>
      )}
    </>
  );
}
