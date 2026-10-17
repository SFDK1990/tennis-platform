"use client";

import { useEffect, useRef, useState } from "react";

/**
 * The front page's loop (31-fase31, decision 23). Muted and without controls, so it starts on
 * its own; the photo is its poster until it plays. It only starts if the visitor has not asked
 * for less motion, and the button pauses it for anyone it bothers.
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
        preload="metadata"
        aria-label={description}
        onPlay={() => setPlaying(true)}
        onPause={() => setPlaying(false)}
        className="size-full object-cover"
      />
      <button
        type="button"
        onClick={toggle}
        aria-pressed={!playing}
        className="absolute bottom-4 right-4 flex size-11 items-center justify-center rounded-full bg-navy/70 text-white hover:bg-navy"
      >
        <span className="sr-only">Pausar el vídeo</span>
        <svg viewBox="0 0 24 24" aria-hidden className="size-5 fill-current">
          {playing ? <path d="M7 5h3.5v14H7zM13.5 5H17v14h-3.5z" /> : <path d="M8 5.5v13l10.5-6.5z" />}
        </svg>
      </button>
    </>
  );
}
