"use client";

import Link from "next/link";
import type { FormEvent } from "react";
import { useRegister } from "@/modules/identity/api";
import { fieldErrorOf } from "@/shared/api/errors";
import { Button } from "@/shared/ui/Button";
import { Field } from "@/shared/ui/Field";
import { ErrorNotice, Notice } from "@/shared/ui/Notice";

export function RegisterForm() {
  const register = useRegister();

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    register.mutate({ email: String(form.get("email")), password: String(form.get("password")) });
  }

  // The backend answers the same whether the address was new or taken, and so does the screen.
  if (register.isSuccess) {
    return (
      <div className="flex flex-col gap-4">
        <Notice tone="success">
          Te hemos enviado un correo con un enlace para verificar tu email. Mientras tanto ya puedes entrar
          y completar tu perfil.
        </Notice>
        <Link href="/login" className="text-court underline">Ir a entrar</Link>
      </div>
    );
  }

  return (
    <form onSubmit={submit} className="flex flex-col gap-4">
      <Field label="Email" name="email" type="email" autoComplete="email" required
        error={fieldErrorOf(register.error, "email")} />
      <Field label="Contraseña" name="password" type="password" autoComplete="new-password" required minLength={10}
        hint="Al menos 10 caracteres." error={fieldErrorOf(register.error, "password")} />
      <ErrorNotice error={register.error} />
      <Button type="submit" pending={register.isPending}>Crear cuenta</Button>
      {/* It informs, it does not ask: the data is needed to give the classes (31-fase31, decisions 15-16). */}
      <p className="text-sm text-muted">
        Las cuentas son para mayores de 14 años. Qué hacemos con tus datos lo explica
        la <Link href="/privacidad" className="text-court underline">política de privacidad</Link>.
      </p>
      <p className="text-sm">
        ¿Ya tienes cuenta? <Link href="/login" className="text-court underline">Entra</Link>
      </p>
    </form>
  );
}
