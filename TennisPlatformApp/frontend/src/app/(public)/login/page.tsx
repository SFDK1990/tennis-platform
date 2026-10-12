"use client";

import { useRouter } from "next/navigation";
import { use } from "react";
import { homeFor } from "@/modules/identity/api";
import { LoginForm } from "@/modules/identity/components/LoginForm";
import { Notice } from "@/shared/ui/Notice";

export default function LoginPage({ searchParams }: PageProps<"/login">) {
  const router = useRouter();
  const { cuenta } = use(searchParams);
  return (
    <>
      <h1 className="mb-5 font-display text-3xl font-semibold leading-none">Entrar</h1>
      {cuenta === "borrada" ? (
        <div className="mb-5">
          <Notice tone="success">Hemos borrado tu cuenta y tus datos. Gracias por entrenar con nosotros.</Notice>
        </div>
      ) : null}
      <LoginForm onSignedIn={(role) => router.replace(homeFor(role))} />
    </>
  );
}
