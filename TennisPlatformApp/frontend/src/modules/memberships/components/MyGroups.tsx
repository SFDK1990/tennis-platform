"use client";

import { useState } from "react";
import {
  describeSeries, FEE_STATUS, formatMonth, useGroups, useJoinGroup, useLeaveGroup, useMyMemberships,
  type FixedGroup,
} from "@/modules/memberships/api";
import { formatEuros } from "@/shared/money";
import { formatShortDay } from "@/shared/time";
import { useNow } from "@/shared/useNow";
import { Button } from "@/shared/ui/Button";
import { Chip } from "@/shared/ui/Chip";
import { ErrorNotice } from "@/shared/ui/Notice";

/**
 * The student's fixed groups and their months (43-fase28), and the groups they can sign up for
 * (decision 2). Leaving keeps the lessons until the end of the month (decision 5).
 */
export function MyGroups({ zone, myLevelId }: { zone: string; myLevelId: string | null }) {
  const memberships = useMyMemberships();
  const groups = useGroups();
  const leave = useLeaveGroup();
  const [confirming, setConfirming] = useState<string | null>(null);
  const now = useNow();
  // A membership that ended is history: the months it owes still show in the teacher's record.
  const mine = (memberships.data ?? []).filter((membership) => membership.leftAt === null || Date.parse(membership.leftAt) > now);
  const current = new Set(mine.map((membership) => membership.group.id));
  const open = (groups.data ?? []).filter((group) => !current.has(group.id) && group.series.some((series) => series.upcomingLessons > 0)
    && (!group.levelId || !myLevelId || group.levelId === myLevelId));

  if (mine.length === 0 && open.length === 0) {
    return null;
  }
  return (
    <section aria-labelledby="my-groups-heading" className="flex max-w-3xl flex-col gap-3">
      <h2 id="my-groups-heading" className="font-display text-2xl font-semibold">Grupos fijos</h2>
      <ErrorNotice error={memberships.error ?? groups.error} />
      {mine.map((membership) => {
        const unpaid = membership.fees.filter((line) => line.status === "PENDING" || line.status === "OVERDUE");
        return (
          <article key={membership.id} aria-label={membership.group.name}
            className="flex flex-col gap-2 rounded-xl border border-line bg-paper p-4">
            <div className="flex flex-wrap items-center gap-2">
              <div className="flex min-w-48 flex-1 flex-col">
                <span className="font-semibold">{membership.group.name}</span>
                <span className="text-sm text-muted">
                  {membership.group.series.map((series) => describeSeries(series, zone)).join(" · ")}
                  {" · "}{formatEuros(membership.group.monthlyFeeCents)} al mes
                </span>
              </div>
              {membership.leftAt ? (
                <Chip>Te vas a fin de mes</Chip>
              ) : confirming === membership.group.id ? (
                <>
                  <span className="max-w-56 text-xs">Sigues en tus clases hasta fin de mes; las siguientes se liberan.</span>
                  <Button variant="danger" pending={leave.isPending}
                    onClick={() => leave.mutate(membership.group.id, { onSuccess: () => setConfirming(null) })}>
                    Sí, darme de baja
                  </Button>
                  <Button variant="quiet" onClick={() => setConfirming(null)}>No</Button>
                </>
              ) : (
                <Button variant="quiet" onClick={() => setConfirming(membership.group.id)}>Darme de baja</Button>
              )}
            </div>
            {unpaid.length > 0 ? (
              <ul className="flex flex-col gap-1 text-sm">
                {unpaid.map((line) => (
                  <li key={line.month} className="flex flex-wrap items-center gap-2">
                    <Chip tone={FEE_STATUS[line.status].tone}>{FEE_STATUS[line.status].label}</Chip>
                    <span className="first-letter:uppercase">
                      {formatMonth(line.month)}, {formatEuros(line.monthlyFeeCents)}, vence el {formatShortDay(line.dueOn)}
                    </span>
                  </li>
                ))}
              </ul>
            ) : membership.leftAt ? null : <p className="text-sm text-muted">Estás al día con la cuota.</p>}
          </article>
        );
      })}
      <ErrorNotice error={leave.error} />
      {open.map((group) => <JoinableGroup key={group.id} group={group} zone={zone} />)}
    </section>
  );
}

function JoinableGroup({ group, zone }: { group: FixedGroup; zone: string }) {
  const join = useJoinGroup();
  const [confirming, setConfirming] = useState(false);
  const full = group.freeSeats === 0;
  return (
    <article aria-label={group.name} className="flex flex-col gap-2 rounded-xl border border-dashed border-line p-4">
      <div className="flex flex-wrap items-center gap-2">
        <div className="flex min-w-48 flex-1 flex-col">
          <span className="font-semibold">{group.name}</span>
          <span className="text-sm text-muted">
            {group.series.map((series) => describeSeries(series, zone)).join(" · ")}
            {" · "}{formatEuros(group.monthlyFeeCents)} al mes
          </span>
        </div>
        {full ? <Chip>Completo</Chip> : confirming ? (
          <>
            <span className="max-w-56 text-xs">
              Tendrás plaza en todas sus clases y pagarás {formatEuros(group.monthlyFeeCents)} cada mes a tu profesor.
            </span>
            <Button pending={join.isPending} onClick={() => join.mutate(group.id)}>Apuntarme</Button>
            <Button variant="quiet" onClick={() => setConfirming(false)}>No</Button>
          </>
        ) : (
          <Button variant="quiet" onClick={() => setConfirming(true)}>Apuntarme</Button>
        )}
      </div>
      <ErrorNotice error={join.error} />
    </article>
  );
}
