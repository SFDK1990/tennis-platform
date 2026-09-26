"use client";

import Link from "next/link";
import type { FormEvent } from "react";
import { useForgotPassword, useResetPassword } from "@/modules/identity/api";
import { fieldErrorOf } from "@/shared/api/errors";
import { Button } from "@/shared/ui/Button";
import { Field } from "@/shared/ui/Field";
import { ErrorNotice, Notice } from "@/shared/ui/Notice";

export function ForgotPasswordForm() {
  const forgot = useForgotPassword();

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    forgot.mutate(String(new FormData(event.currentTarget).get("email")));
  }

  if (forgot.isSuccess) {
    return (
      <Notice tone="success">
        Si hay una cuenta con ese email, le hemos enviado un enlace para elegir una contraseña nueva.
      </Notice>
    );
  }

  return (
    <form onSubmit={submit} className="flex flex-col gap-4">
      <Field label="Email" name="email" type="email" autoComplete="email" required />
      <ErrorNotice error={forgot.error} />
      <Button type="submit" pending={forgot.isPending}>Enviar enlace</Button>
    </form>
  );
}

export function ResetPasswordForm({ token }: { token: string | null }) {
  const reset = useResetPassword();

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (token) {
      reset.mutate({ token, newPassword: String(new FormData(event.currentTarget).get("newPassword")) });
    }
  }

  if (!token) {
    return <Notice tone="error">Este enlace está incompleto. Ábrelo de nuevo desde el correo.</Notice>;
  }

  if (reset.isSuccess) {
    return (
      <div className="flex flex-col gap-4">
        <Notice tone="success">Contraseña cambiada. Hemos cerrado tus otras sesiones.</Notice>
        <Link href="/login" className="text-court underline">Entrar</Link>
      </div>
    );
  }

  return (
    <form onSubmit={submit} className="flex flex-col gap-4">
      <Field label="Contraseña nueva" name="newPassword" type="password" autoComplete="new-password" required
        minLength={10} hint="Al menos 10 caracteres." error={fieldErrorOf(reset.error, "newPassword")} />
      <ErrorNotice error={reset.error} />
      <Button type="submit" pending={reset.isPending}>Cambiar contraseña</Button>
    </form>
  );
}
