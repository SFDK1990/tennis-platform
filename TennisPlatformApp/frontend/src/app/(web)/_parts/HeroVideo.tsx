"use client";

import { useEffect, useRef, useState } from "react";

/**
 * The front page's background loop (31-fase31, decision 23), under a navy veil that keeps the
 * white text readable. Muted and without controls, so it starts on its own, and only after
 * hydration: it never competes with the photo for the largest paint. It does not start if the
 * visitor asked for less motion, and the button pauses it for anyone it bothers.
 */
export function HeroVideo({ src, poster, description }: { src: string; poster: string; description: string }) {
  const video = useRef<HTMLVideoElement>(null);
  const [playing, setPlaying] = useState(false);

  useEffect(() => {
    if (!matchMedia("(prefers-reduced-motion: reduce)").matches) {
      // A browser that refuses autoplay leaves the poster, which is enough.
      video.current?.play().catch(() => undefined);
    }
  }, []);

  function toggle() {
    if (video.current?.paused) {
      video.current.play().catch(() => undefined);
    } else {
      video.current?.pause();
    }
  }

  return (
    <>
      <video
        ref={video}
        src={src}
        poster={poster}
        muted
        loop
        playsInline
        preload="none"
        aria-label={description}
        onPlay={() => setPlaying(true)}
        onPause={() => setPlaying(false)}
        className="absolute inset-0 -z-10 size-full object-cover"
      />
      <div aria-hidden className="absolute inset-0 -z-10 bg-(--site-band)/80 lg:bg-linear-to-r lg:from-(--site-band)/90 lg:via-(--site-band)/75 lg:to-(--site-band)/45" />
      <button
        type="button"
        onClick={toggle}
        aria-pressed={!playing}
        className="absolute bottom-5 right-5 z-10 flex size-11 items-center justify-center rounded-full bg-navy/70 text-white hover:bg-navy"
      >
        <span className="sr-only">Pausar el vídeo</span>
        <svg viewBox="0 0 24 24" aria-hidden className="size-5 fill-current">
          {playing ? <path d="M7 5h3.5v14H7zM13.5 5H17v14h-3.5z" /> : <path d="M8 5.5v13l10.5-6.5z" />}
        </svg>
      </button>
    </>
  );
}
