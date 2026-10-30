"use client";

import { balancesOf, type Pass } from "@/modules/passes/api";
import { addDays, todayIn, type LocalDate } from "@/shared/time";
import { Chip } from "@/shared/ui/Chip";

/** How many days ahead a pass counts as about to expire in the list of students. */
const SOON_DAYS = 14;

/**
 * Who has run out or is about to (41-fase27, decision 6), for the list of students. A student who
 * never had a pass gets nothing: that is not news while the teacher does not require them.
 */
export function PassFlag({ passes, zone }: { passes: Pass[]; zone: string }) {
  const balances = balancesOf(passes);
  if (balances.length === 0) {
    return null;
  }
  if (balances.every((balance) => balance.remaining === 0)) {
    return <Chip tone="fault">Sin saldo</Chip>;
  }
  const soon: LocalDate = addDays(todayIn(zone), SOON_DAYS);
  if (balances.some((balance) => balance.remaining > 0 && balance.until !== null && balance.until <= soon)) {
    return <Chip tone="warning">Bono a punto de caducar</Chip>;
  }
  return null;
}
