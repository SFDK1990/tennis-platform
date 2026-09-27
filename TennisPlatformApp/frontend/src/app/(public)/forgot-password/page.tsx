import Link from "next/link";
import { ForgotPasswordForm } from "@/modules/identity/components/PasswordForms";

export default function ForgotPasswordPage() {
  return (
    <>
      <h1 className="mb-5 font-display text-3xl font-semibold leading-none">Recuperar la contraseña</h1>
      <ForgotPasswordForm />
      <Link href="/login" className="mt-4 inline-block text-sm text-court underline">Volver a entrar</Link>
    </>
  );
}
