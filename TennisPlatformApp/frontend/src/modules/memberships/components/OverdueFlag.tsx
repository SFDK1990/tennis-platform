import { overdue, type FeeLine } from "@/modules/memberships/api";
import { Chip } from "@/shared/ui/Chip";

/** In the list of students: who owes a month past its due day. */
export function OverdueFlag({ lines }: { lines: FeeLine[] }) {
  return overdue(lines).length > 0 ? <Chip tone="fault">Mensualidad sin pagar</Chip> : null;
}
