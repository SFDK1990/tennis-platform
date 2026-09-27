"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { homeFor, useMe, useSession } from "@/modules/identity/api";

/** No page of its own: it sends each person to where they start. */
export default function Home() {
  const router = useRouter();
  const { status } = useSession();
  const role = useMe().data?.role;

  useEffect(() => {
    if (status === "signed-out") {
      router.replace("/login");
    } else if (role) {
      router.replace(homeFor(role));
    }
  }, [status, role, router]);

  return <p className="py-24 text-center text-muted">Cargando…</p>;
}
