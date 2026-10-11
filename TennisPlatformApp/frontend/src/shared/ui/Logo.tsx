/**
 * The Oasis Papagayo mark (31-fase31, decision 1): a tennis ball with a parrot's beak, eye and
 * blue crest. It keeps its own colours wherever it goes; on navy the beak gets a light edge, or
 * its tip disappears into the background.
 *
 * Pass an empty `label` when the name is written next to it, so it is not read out twice.
 */
export function Logo({
  className = "h-8 w-8",
  onDark = false,
  label = "Oasis Papagayo",
}: {
  className?: string;
  onDark?: boolean;
  label?: string;
}) {
  const a11y = label ? { role: "img", "aria-label": label } : { "aria-hidden": true };
  return (
    <svg viewBox="0 0 120 120" className={className} {...a11y}>
      <path d="M36 30C30 18 32 8 42 2c0 10 4 18 10 22z" className="fill-court" />
      <path d="M48 24c-1-11 5-19 15-23-2 10 0 17 5 21z" className="fill-court" />
      <circle cx="56" cy="64" r="42" className="fill-ball" />
      <path d="M21 42c19 14 21 42 14 58" fill="none" strokeWidth="4" strokeLinecap="round" className="stroke-white" />
      <path
        d="M86 46c18-4 32 10 28 30-4-7-10-10-18-10 3 5 2 11-4 14-4-8-8-24-6-34z"
        strokeWidth="3"
        strokeLinejoin="round"
        className={onDark ? "fill-navy stroke-white" : "fill-navy"}
      />
      <circle cx="74" cy="52" r="7.5" className="fill-white" />
      <circle cx="76" cy="52" r="3.8" className="fill-navy" />
    </svg>
  );
}
