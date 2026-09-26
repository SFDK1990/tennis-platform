"use client";

import Link from "next/link";
import type { FormEvent } from "react";
import { useLogin, type Role } from "@/modules/identity/api";
import { Button } from "@/shared/ui/Button";
import { Field } from "@/shared/ui/Field";
import { ErrorNotice } from "@/shared/ui/Notice";

export function LoginForm({ onSignedIn }: { onSignedIn: (role: Role) => void }) {
  const login = useLogin();

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    login.mutate(
      { email: String(form.get("email")), password: String(form.get("password")) },
      { onSuccess: (user) => onSignedIn(user.role) },
    );
  }

  return (
    <form onSubmit={submit} className="flex flex-col gap-4">
      <Field label="Email" name="email" type="email" autoComplete="email" required />
      <Field label="Contraseña" name="password" type="password" autoComplete="current-password" required />
      <ErrorNotice error={login.error} />
      <Button type="submit" pending={login.isPending}>Entrar</Button>
      <p className="flex justify-between text-sm">
        <Link href="/register" className="text-court underline">Crear una cuenta</Link>
        <Link href="/forgot-password" className="text-court underline">He olvidado la contraseña</Link>
      </p>
    </form>
  );
}
