"use client";

import { useMe } from "@/modules/identity/api";
import { ProfileForm } from "@/modules/identity/components/ProfileForm";
import { SignOutButton } from "@/modules/identity/components/SignOutButton";

export default function ProfilePage() {
  const me = useMe().data;
  return (
    <div className="flex max-w-2xl flex-col gap-6">
      <h1 className="font-display text-4xl font-semibold leading-none">Tu perfil</h1>
      {me ? (
        <section className="rounded-xl border border-line bg-paper p-5">
          <ProfileForm key={me.id} me={me} />
        </section>
      ) : null}
      {/* On a desk the sidebar has it; on a phone the tab bar has no room, so it lives here. */}
      <SignOutButton className="flex min-h-11 items-center justify-center gap-2 rounded-lg border border-line bg-paper px-4 font-semibold text-ink hover:border-court lg:hidden" />
    </div>
  );
}
