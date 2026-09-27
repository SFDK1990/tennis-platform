"use client";

import { useRouter } from "next/navigation";
import { homeFor } from "@/modules/identity/api";
import { LoginForm } from "@/modules/identity/components/LoginForm";

export default function LoginPage() {
  const router = useRouter();
  return (
    <>
      <h1 className="mb-4 font-display text-2xl font-semibold">Entrar</h1>
      <LoginForm onSignedIn={(role) => router.replace(homeFor(role))} />
    </>
  );
}
