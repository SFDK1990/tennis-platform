/**
 * The MAS mark (concept 05, "Court-Line Letters"): the letters are holes in a court-shaped tile,
 * so it takes the colour of the text around it.
 */
export function Logo({ className = "h-7 w-auto" }: { className?: string }) {
  return (
    <svg viewBox="0 0 240 110" role="img" aria-label="MAS Tennis Academy" className={`fill-current ${className}`}>
      <path d="M8 0H232C236.42 0 240 3.58 240 8V102C240 106.42 236.42 110 232 110H8C3.58 110 0 106.42 0 102V8C0 3.58 3.58 0 8 0ZM7 7V103H233V7ZM11.5 11.5H228.5V98.5H11.5ZM97 29V81H104V58.5H136V81H143V29ZM159 29V58.5H198V74H159V81H205V51.5H166V36H205V29ZM35 29V81H42V36H54.5V61.24H61.5V36H74V81H81V29ZM136 36V51.5H104V36Z" />
    </svg>
  );
}
