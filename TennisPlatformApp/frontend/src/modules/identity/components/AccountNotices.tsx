"use client";

import Link from "next/link";
import { useResendVerification, type Me } from "@/modules/identity/api";
import { Button } from "@/shared/ui/Button";
import { ErrorNotice, Notice } from "@/shared/ui/Notice";

/** What stands between this account and booking, said where it can be fixed. */
export function AccountNotices({ me }: { me: Me }) {
  const resend = useResendVerification();
  const pending = me.status === "PENDING_VERIFICATION";
  const noProfile = me.role === "STUDENT" && me.fullName === null;

  if (!pending && !noProfile) {
    return null;
  }

  return (
    <div className="flex flex-col gap-3">
      {noProfile ? (
        <Notice>
          Completa <Link href="/profile" className="text-court underline">tu perfil</Link>: tu profesor lo necesita
          para añadirte a sus alumnos.
        </Notice>
      ) : null}
      {pending ? (
        <Notice>
          <div className="flex flex-wrap items-center justify-between gap-3">
            <span>
              {resend.isSuccess
                ? `Te hemos enviado otro enlace a ${me.email}.`
                : `Verifica tu email con el enlace que enviamos a ${me.email}. Sin verificarlo no puedes reservar.`}
            </span>
            {resend.isSuccess ? null : (
              <Button variant="quiet" onClick={() => resend.mutate()} pending={resend.isPending}>
                Reenviar correo
              </Button>
            )}
          </div>
        </Notice>
      ) : null}
      <ErrorNotice error={resend.error} />
    </div>
  );
}
