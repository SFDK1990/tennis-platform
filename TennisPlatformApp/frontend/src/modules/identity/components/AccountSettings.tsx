"use client";

import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { useChangePassword, useDeleteMyAccount, useDownloadMyData } from "@/modules/identity/api";
import { fieldErrorOf } from "@/shared/api/errors";
import { Button } from "@/shared/ui/Button";
import { Field } from "@/shared/ui/Field";
import { ErrorNotice, Notice } from "@/shared/ui/Notice";

const SECTION = "flex flex-col gap-4 rounded-xl border border-line bg-paper p-5";
const HEADING = "font-display text-2xl font-semibold leading-none";

/** Password managers pair a password with its account; without it they cannot fill or save it. */
function AccountName({ email }: { email: string }) {
  return <input type="text" name="username" autoComplete="username" value={email} readOnly hidden />;
}

export function ChangePasswordForm({ email }: { email: string }) {
  const change = useChangePassword();

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    change.mutate(
      { currentPassword: String(data.get("currentPassword")), newPassword: String(data.get("newPassword")) },
      { onSuccess: () => form.reset() },
    );
  }

  return (
    <section aria-labelledby="password-heading" className={SECTION}>
      <h2 id="password-heading" className={HEADING}>Contraseña</h2>
      <form onSubmit={submit} className="flex flex-col gap-4">
        <AccountName email={email} />
        <Field label="Contraseña actual" name="currentPassword" type="password" autoComplete="current-password" required />
        <Field label="Contraseña nueva" name="newPassword" type="password" autoComplete="new-password" required
          minLength={10} hint="Al menos 10 caracteres." error={fieldErrorOf(change.error, "newPassword")} />
        <ErrorNotice error={change.error} />
        {change.isSuccess ? (
          <Notice tone="success">Contraseña cambiada. Hemos cerrado tu sesión en los demás dispositivos.</Notice>
        ) : null}
        <Button type="submit" pending={change.isPending} className="self-start">Cambiar contraseña</Button>
      </form>
    </section>
  );
}

export function DownloadMyData() {
  const download = useDownloadMyData();
  return (
    <section aria-labelledby="data-heading" className={SECTION}>
      <h2 id="data-heading" className={HEADING}>Copia de tus datos</h2>
      <p className="text-muted">Un archivo con tu cuenta, tu perfil y todas tus reservas.</p>
      <ErrorNotice error={download.error} />
      <Button variant="quiet" pending={download.isPending} className="self-start" onClick={() => download.mutate()}>
        Descargar mis datos
      </Button>
    </section>
  );
}

/**
 * The one action in the app that cannot be undone, so it says plainly what goes and what stays,
 * and asks for the password before doing it.
 */
export function DeleteMyAccount({ email }: { email: string }) {
  const router = useRouter();
  const remove = useDeleteMyAccount();
  const [confirming, setConfirming] = useState(false);

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    remove.mutate(String(new FormData(event.currentTarget).get("password")), {
      onSuccess: () => router.replace("/login?cuenta=borrada"),
    });
  }

  return (
    <section aria-labelledby="delete-heading" className={SECTION}>
      <h2 id="delete-heading" className={HEADING}>Borrar tu cuenta</h2>
      {confirming ? (
        <form onSubmit={submit} className="flex flex-col gap-4">
          <AccountName email={email} />
          <p>
            Se borran tu nombre, tu email, tu teléfono, tu DNI y tu dirección, y se cancelan tus reservas
            futuras. Tus clases pasadas quedan en el registro de tu profesor como «Alumno eliminado». No se puede
            deshacer.
          </p>
          <Field label="Tu contraseña" name="password" type="password" autoComplete="current-password" required />
          <ErrorNotice error={remove.error} />
          <div className="flex flex-wrap gap-2">
            <Button type="submit" variant="danger" pending={remove.isPending}>Sí, borrar mi cuenta</Button>
            <Button type="button" variant="quiet" onClick={() => setConfirming(false)}>No, mantenerla</Button>
          </div>
        </form>
      ) : (
        <>
          <p className="text-muted">Si dejas de entrenar con nosotros, puedes borrar tu cuenta y tus datos.</p>
          <Button variant="danger" className="self-start" onClick={() => setConfirming(true)}>Borrar mi cuenta</Button>
        </>
      )}
    </section>
  );
}
