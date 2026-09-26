import { VerifyEmailPanel } from "@/modules/identity/components/VerifyEmailPanel";

/** The address the backend puts in the email: /verify-email?token=... */
export default async function VerifyEmailPage({ searchParams }: PageProps<"/verify-email">) {
  const { token } = await searchParams;
  return (
    <>
      <h1 className="mb-4 font-display text-2xl font-semibold">Verificar el email</h1>
      <VerifyEmailPanel token={typeof token === "string" ? token : null} />
    </>
  );
}
