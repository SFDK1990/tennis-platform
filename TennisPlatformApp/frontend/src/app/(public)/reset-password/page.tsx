import { ResetPasswordForm } from "@/modules/identity/components/PasswordForms";

/** The address the backend puts in the email: /reset-password?token=... */
export default async function ResetPasswordPage({ searchParams }: PageProps<"/reset-password">) {
  const { token } = await searchParams;
  return (
    <>
      <h1 className="mb-4 font-display text-2xl font-semibold">Elegir una contraseña nueva</h1>
      <ResetPasswordForm token={typeof token === "string" ? token : null} />
    </>
  );
}
