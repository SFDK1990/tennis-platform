"use client";

import { useRouter } from "next/navigation";
import { homeFor } from "@/modules/identity/api";
import { LoginForm } from "@/modules/identity/components/LoginForm";

export default function LoginPage() {
  const router = useRouter();
  return (
    <>
      <h1 className="mb-5 font-display text-3xl font-semibold leading-none">Entrar</h1>
      <LoginForm onSignedIn={(role) => router.replace(homeFor(role))} />
    </>
  );
}
