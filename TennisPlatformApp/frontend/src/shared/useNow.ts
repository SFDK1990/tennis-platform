import { useEffect, useState } from "react";

/**
 * The current time, re-read every minute. Reading the clock during render would make a
 * component impure; this way "has the lesson started" updates on its own while the screen is open.
 */
export function useNow(): number {
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 60_000);
    return () => clearInterval(timer);
  }, []);
  return now;
}
