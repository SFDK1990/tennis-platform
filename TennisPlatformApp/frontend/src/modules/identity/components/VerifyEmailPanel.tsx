"use client";

import Link from "next/link";
import { useVerifyEmail } from "@/modules/identity/api";
import { Button } from "@/shared/ui/Button";
import { ErrorNotice, Notice } from "@/shared/ui/Notice";

/**
 * A button rather than verifying on arrival: mail scanners open links on their own, and a
 * page that acted on load would spend the token before the student ever saw it.
 */
export function VerifyEmailPanel({ token }: { token: string | null }) {
  const verify = useVerifyEmail();

  if (!token) {
    return <Notice tone="error">Este enlace está incompleto. Ábrelo de nuevo desde el correo.</Notice>;
  }

  if (verify.isSuccess) {
    return (
      <div className="flex flex-col gap-4">
        <Notice tone="success">Email verificado. Ya puedes reservar clases.</Notice>
        <Link href="/" className="text-court underline">Continuar</Link>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-4">
      <p>Confirma que este email es tuyo.</p>
      <ErrorNotice error={verify.error} />
      <Button onClick={() => verify.mutate(token)} pending={verify.isPending}>Verificar mi email</Button>
    </div>
  );
}
