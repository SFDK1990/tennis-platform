"use client";

import { balancesOf, describeBalance, useMyPasses } from "@/modules/passes/api";
import { Notice } from "@/shared/ui/Notice";

/**
 * What the student has left to book with (41-fase27, decision 3): "Te quedan 4 clases de grupo,
 * hasta el 15 ene.". Nothing at all while the teacher does not require passes and the student
 * has none.
 */
export function MyPasses() {
  const passes = useMyPasses().data;
  if (!passes) {
    return null;
  }
  const balances = balancesOf(passes.items);
  if (balances.length === 0) {
    return passes.required ? (
      <Notice>Para reservar necesitas un bono. Pídeselo a tu profesor: él lo apunta cuando se lo pagas.</Notice>
    ) : null;
  }
  return (
    <section aria-labelledby="my-passes" className="flex flex-col gap-1 rounded-xl border border-line bg-paper px-4 py-3">
      <h2 id="my-passes" className="font-semibold">Tus bonos</h2>
      <ul className="text-muted">
        {balances.map((balance) => (
          <li key={balance.kind}>
            {balance.remaining === 0
              ? `No te quedan clases ${balance.kind === "GROUP" ? "de grupo" : "particulares"}.`
              : `Te quedan ${describeBalance(balance)}.`}
          </li>
        ))}
      </ul>
    </section>
  );
}
