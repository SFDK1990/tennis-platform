/** "120 €", "12,50 €": every amount is kept in cents, so none is ever a float. */
export function formatEuros(cents: number): string {
  return new Intl.NumberFormat("es-ES", { style: "currency", currency: "EUR", minimumFractionDigits: cents % 100 ? 2 : 0 })
    .format(cents / 100);
}

/** "45" or "45,50" typed in a field, in cents; null when it is not an amount. */
export function centsOf(typed: string): number | null {
  const text = typed.trim().replace(",", ".");
  if (!/^[0-9]+(\.[0-9]{1,2})?$/.test(text)) {
    return null;
  }
  return Math.round(Number(text) * 100);
}
