/**
 * Where the court is, drawn (31-fase31, decision 26): the north tip of Fuerteventura with
 * Corralejo, Lobos and the dunes. A drawing instead of an embedded map, which would bring
 * third-party cookies and need the CSP opened; "Cómo llegar" still links to the real one.
 * Orientative, not to scale, and it says so.
 */
export function CourtMap() {
  return (
    <figure className="m-0 flex flex-col gap-2">
      <svg
        viewBox="0 0 480 320"
        role="img"
        aria-labelledby="map-title"
        className="h-auto w-full overflow-hidden rounded-lg bg-(--site-sea)"
      >
        <title id="map-title">
          Dibujo del norte de Fuerteventura: la pista está en Corralejo, frente a la isla de Lobos y junto a las dunas
        </title>
        {/* Waves, a few, so the sea reads as sea. */}
        <g className="fill-none stroke-(--site-wave) stroke-[1.5]" strokeLinecap="round">
          <path d="M40 40q10-6 20 0t20 0" />
          <path d="M300 30q10-6 20 0t20 0" />
          <path d="M400 110q10-6 20 0t20 0" />
          <path d="M70 120q10-6 20 0t20 0" />
        </g>
        {/* Lanzarote, off the top edge. */}
        <path d="M330 0h150v18q-40 14-80 6t-70-24z" className="fill-(--site-land)" />
        <text x="470" y="14" textAnchor="end" className="fill-(--site-map-muted) text-[12px]">
          Lanzarote
        </text>
        {/* Lobos. */}
        <path d="M150 70q14-14 34-8t22 20q-4 16-26 18t-30-12q-6-10 0-18z" className="fill-(--site-land)" />
        <text x="178" y="58" textAnchor="middle" className="fill-(--site-map-muted) text-[12px]">
          Isla de Lobos
        </text>
        {/* Fuerteventura's north coast, and the east coast running south. */}
        <path
          d="M0 200q40-14 80-10t70-8q30-12 60-4t50 0q20-14 46-34t44-22q22 2 28 22t16 40q10 26 26 46t30 40V320H0z"
          className="fill-(--site-land)"
        />
        {/* The dunes, down the east coast. */}
        <path
          d="M392 220q14 22 30 38t32 32q4 12 0 30h-80q-6-30 0-56t18-44z"
          className="fill-(--site-dune)"
        />
        <text x="400" y="296" textAnchor="middle" className="fill-(--site-map-muted) text-[12px]">
          Dunas
        </text>
        {/* Corralejo. */}
        <g className="fill-(--site-town)">
          <rect x="296" y="190" width="10" height="10" rx="2" />
          <rect x="310" y="184" width="12" height="14" rx="2" />
          <rect x="326" y="194" width="9" height="9" rx="2" />
          <rect x="302" y="204" width="12" height="9" rx="2" />
          <rect x="318" y="207" width="10" height="11" rx="2" />
        </g>
        <text x="312" y="238" textAnchor="middle" className="fill-(--site-map-text) text-[15px] font-semibold">
          Corralejo
        </text>
        {/* The court. */}
        <g transform="translate(368 214)">
          <circle r="22" className="fill-ball opacity-30" />
          <path d="M0 0c-9-12-14-20-14-27a14 14 0 0 1 28 0c0 7-5 15-14 27z" className="fill-navy stroke-white stroke-2" />
          <circle cy="-27" r="5" className="fill-ball" />
        </g>
        <text x="368" y="252" textAnchor="middle" className="fill-(--site-map-text) text-[13px] font-semibold">
          La pista
        </text>
        {/* North. */}
        <g transform="translate(36 268)" className="fill-(--site-map-muted)">
          <path d="M0-18l7 16h-14z" />
          <text y="14" textAnchor="middle" className="text-[12px] font-semibold">
            N
          </text>
        </g>
      </svg>
      <figcaption className="text-sm text-(--site-band-muted)">Dibujo orientativo, no a escala.</figcaption>
    </figure>
  );
}
