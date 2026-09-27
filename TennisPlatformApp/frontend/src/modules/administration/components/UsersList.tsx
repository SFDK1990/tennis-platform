"use client";

import { useState, type FormEvent } from "react";
import { useChangeUserStatus, useUsers, type AdminUser, type UserFilters } from "@/modules/administration/api";
import { dateIn, formatDay } from "@/shared/time";
import { Button } from "@/shared/ui/Button";
import { CONTROL_CLASS, Field } from "@/shared/ui/Field";
import { ErrorNotice } from "@/shared/ui/Notice";

const ROLE: Record<AdminUser["role"], string> = { ADMIN: "Admin", TEACHER: "Profesor", STUDENT: "Alumno" };
const STATUS: Record<AdminUser["status"], string> = {
  PENDING_VERIFICATION: "Sin verificar",
  ACTIVE: "Activa",
  DISABLED: "Desactivada",
};

const selectClass = CONTROL_CLASS;

export function UsersList({ zone }: { zone: string }) {
  const [filters, setFilters] = useState<UserFilters>({});
  const [page, setPage] = useState(0);
  const users = useUsers(filters, page);

  function search(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const value = (name: string) => String(form.get(name) ?? "") || undefined;
    setPage(0);
    setFilters({
      role: value("role") as UserFilters["role"],
      status: value("status") as UserFilters["status"],
      query: value("query"),
    });
  }

  return (
    <div className="flex flex-col gap-4">
      <form onSubmit={search} className="flex flex-wrap items-end gap-3">
        <Field label="Email" name="query" placeholder="Parte del email" className="min-w-56 flex-1" />
        <label className="flex flex-col gap-1 text-sm font-semibold">
          Rol
          <select name="role" defaultValue="" className={selectClass}>
            <option value="">Todos</option>
            {Object.entries(ROLE).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select>
        </label>
        <label className="flex flex-col gap-1 text-sm font-semibold">
          Estado
          <select name="status" defaultValue="" className={selectClass}>
            <option value="">Todos</option>
            {Object.entries(STATUS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select>
        </label>
        <Button type="submit" variant="quiet">Buscar</Button>
      </form>

      <ErrorNotice error={users.error} />
      {!users.data ? (
        <p className="text-muted">Cargando…</p>
      ) : users.data.items.length === 0 ? (
        <p className="text-muted">Ninguna cuenta coincide con la búsqueda.</p>
      ) : (
        <ul className="flex flex-col divide-y divide-line rounded-md border border-line bg-paper">
          {users.data.items.map((user) => <UserRow key={user.id} user={user} zone={zone} />)}
        </ul>
      )}

      {users.data && users.data.totalItems > users.data.size ? (
        <div className="flex justify-between">
          <Button variant="quiet" disabled={page === 0} onClick={() => setPage(page - 1)}>Más recientes</Button>
          <Button variant="quiet" disabled={(page + 1) * users.data.size >= users.data.totalItems}
            onClick={() => setPage(page + 1)}>
            Anteriores
          </Button>
        </div>
      ) : null}
    </div>
  );
}

/** Only student accounts can be switched; the backend refuses the rest, and the row says why not. */
function UserRow({ user, zone }: { user: AdminUser; zone: string }) {
  const change = useChangeUserStatus();
  const [confirming, setConfirming] = useState(false);
  const disabled = user.status === "DISABLED";

  return (
    <li className="flex flex-col gap-2 px-4 py-3">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <span className="min-w-0">
          <span className="block truncate font-semibold">{user.email}</span>
          <span className="text-sm text-muted">
            {ROLE[user.role]}, {STATUS[user.status].toLowerCase()}. Alta el {formatDay(dateIn(user.createdAt, zone))}.
          </span>
        </span>
        {user.role !== "STUDENT" ? null : disabled ? (
          <Button variant="quiet" pending={change.isPending} onClick={() => change.mutate({ id: user.id, status: "ACTIVE" })}>
            Reactivar
          </Button>
        ) : confirming ? (
          <span className="flex flex-wrap items-center gap-2">
            <span className="text-sm">No podrá entrar y perderá sus reservas futuras.</span>
            <Button variant="danger" pending={change.isPending}
              onClick={() => change.mutate({ id: user.id, status: "DISABLED" }, { onSuccess: () => setConfirming(false) })}>
              Sí, desactivar
            </Button>
            <Button variant="quiet" onClick={() => setConfirming(false)}>No</Button>
          </span>
        ) : (
          <Button variant="quiet" onClick={() => setConfirming(true)}>Desactivar</Button>
        )}
      </div>
      <ErrorNotice error={change.error} />
    </li>
  );
}
