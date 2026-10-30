"use client";

import { use } from "react";
import { BuyPasses, PurchaseOutcome } from "@/modules/passes/components/BuyPasses";
import { MyPasses } from "@/modules/passes/components/MyPasses";

/** Buying a pass, and where Stripe's page sends the student back (`?purchase=<id>`). */
export default function PassesPage({ searchParams }: PageProps<"/passes">) {
  const { purchase, cancelled } = use(searchParams);
  const purchaseId = typeof purchase === "string" ? purchase : null;
  return (
    <>
      <h1 className="font-display text-4xl font-semibold leading-none">Bonos</h1>
      <div className="flex max-w-3xl flex-col gap-6">
        {purchaseId ? <PurchaseOutcome id={purchaseId} cancelled={cancelled === "true"} /> : null}
        <MyPasses />
        <BuyPasses />
      </div>
    </>
  );
}
