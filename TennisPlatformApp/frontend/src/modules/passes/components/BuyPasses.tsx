"use client";

import Link from "next/link";
import { describeType, useBuyPass, usePassTypes, usePurchase } from "@/modules/passes/api";
import { Button } from "@/shared/ui/Button";
import { ErrorNotice, Notice } from "@/shared/ui/Notice";

/**
 * The passes a student can buy in the app (42-fase27.5-analisis-pago-online.md). Buying sends
 * them to Stripe's own page, so no card or Bizum detail is ever typed into the app; the pass is
 * created when Stripe tells the server the money arrived.
 */
export function BuyPasses() {
  const types = usePassTypes();
  const buy = useBuyPass();

  if (types.error) {
    return <ErrorNotice error={types.error} />;
  }
  if (!types.data) {
    return <p className="text-muted">Cargando…</p>;
  }
  if (!types.data.onlinePayment) {
    return <Notice>Tu profesor no vende bonos en la app. Pídeselo a él: lo apunta cuando se lo pagas.</Notice>;
  }
  const forSale = types.data.passTypes.filter((type) => !type.retired);
  if (forSale.length === 0) {
    return <p className="text-muted">Ahora mismo no hay bonos a la venta.</p>;
  }

  function start(passTypeId: string) {
    buy.mutate(passTypeId, { onSuccess: (purchase) => {
      if (purchase.checkoutUrl) {
        window.location.assign(purchase.checkoutUrl);
      }
    } });
  }

  return (
    <section aria-labelledby="buy-heading" className="flex flex-col gap-3">
      <h2 id="buy-heading" className="font-display text-2xl font-semibold leading-none">Comprar un bono</h2>
      <p className="text-sm text-muted">Se paga con tarjeta o Bizum en la página segura de Stripe.</p>
      <ul className="flex flex-col divide-y divide-line-soft rounded-xl border border-line bg-paper">
        {forSale.map((type) => (
          <li key={type.id} className="flex flex-wrap items-center justify-between gap-3 px-4 py-3">
            <span className="flex min-w-0 flex-col">
              <span className="font-semibold">{type.name}</span>
              <span className="text-sm text-muted">{describeType(type)}</span>
            </span>
            <Button aria-label={`Comprar ${type.name}`} pending={buy.isPending && buy.variables === type.id}
              disabled={buy.isPending} onClick={() => start(type.id)}>
              Comprar
            </Button>
          </li>
        ))}
      </ul>
      <ErrorNotice error={buy.error} />
    </section>
  );
}

/** Back from Stripe's page: paid, still on its way, or left without paying. */
export function PurchaseOutcome({ id, cancelled }: { id: string; cancelled: boolean }) {
  const purchase = usePurchase(id);

  if (purchase.error) {
    return <ErrorNotice error={purchase.error} />;
  }
  if (!purchase.data) {
    return null;
  }
  if (purchase.data.status === "PAID") {
    return (
      <Notice tone="success">
        Pago recibido: ya tienes tu bono «{purchase.data.name}». <Link href="/calendar" className="font-semibold underline">Reserva una clase</Link>.
      </Notice>
    );
  }
  if (purchase.data.status === "EXPIRED" || cancelled) {
    return <Notice>No se ha cobrado nada. Puedes volver a intentarlo cuando quieras.</Notice>;
  }
  return <Notice>Pago recibido, preparando tu bono…</Notice>;
}
