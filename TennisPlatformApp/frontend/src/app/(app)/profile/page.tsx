"use client";

import { useMe } from "@/modules/identity/api";
import { ProfileForm } from "@/modules/identity/components/ProfileForm";

export default function ProfilePage() {
  const me = useMe().data;
  return (
    <>
      <h1 className="font-display text-3xl font-semibold">Tu perfil</h1>
      {me ? <ProfileForm key={me.id} me={me} /> : null}
    </>
  );
}
