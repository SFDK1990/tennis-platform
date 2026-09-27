"use client";

import { useRouter } from "next/navigation";
import { useSignOut } from "@/modules/identity/api";
import { Icon } from "@/shared/ui/Icon";

/** In the sidebar on a desk, and on the profile screen on a phone, where the tab bar has no room. */
export function SignOutButton({ className }: { className: string }) {
  const router = useRouter();
  const signOut = useSignOut();
  return (
    <button type="button" className={className}
      onClick={() => signOut.mutate(undefined, { onSettled: () => router.replace("/login") })}>
      <Icon name="logout" />
      Salir
    </button>
  );
}
