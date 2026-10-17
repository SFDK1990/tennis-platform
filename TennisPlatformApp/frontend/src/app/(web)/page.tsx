import type { Metadata } from "next";
import Link from "next/link";
import { preload } from "react-dom";
import { ClassFinder } from "./_parts/ClassFinder";
import { Copy } from "./_parts/Copy";
import { HeroVideo } from "./_parts/HeroVideo";
import { placeholderParts, school } from "./_parts/school";
import { SiteFooter, SiteNav, WRAP } from "./_parts/SiteNav";
import { TrialButton } from "./_parts/TrialButton";

const DESCRIPTION =
  "Clases de tenis para niños y adultos, en grupos reducidos o particulares. Las da siempre Marcos, en la misma pista, y las plazas se reservan desde el móvil.";

export const metadata: Metadata = {
  title: { absolute: `${school.name}, escuela de tenis en ${school.area}` },
  description: DESCRIPTION,
  alternates: { canonical: "/" },
  openGraph: { type: "website", locale: "es_ES", siteName: school.name, title: school.name, description: DESCRIPTION },
};

/** The school's public page (31-fase31): direction A, "Pista", of the approved mock-up. */
export default function WebsitePage() {
  return (
    <>
      <StructuredData />
      <SiteNav />
      <Hero />
      <main>
        <Classes />
        <Steps />
        <Teacher />
        <Prices />
        <Questions />
      </main>
      <Contact />
      <SiteFooter underTrialBar />
      <TrialBar />
    </>
  );
}

const SECTION_TITLE = "font-display text-5xl font-bold leading-none sm:text-6xl";

function Hero() {
  return (
    <header className="relative isolate overflow-hidden bg-(--site-band) text-white">
      {school.heroVideo ? <HeroVideo {...school.heroVideo} /> : null}
      <div className={`${WRAP} relative grid items-center gap-12 pb-20 pt-8 lg:grid-cols-[1.1fr_1fr] lg:gap-16 lg:pb-24 lg:pt-10`}>
        <div className="site-rise flex flex-col gap-7">
          <h1 className="font-display text-6xl font-bold leading-[0.92] sm:text-7xl lg:text-[88px]">
            Clases de tenis en <Copy text={school.area} />
          </h1>
          <p className="max-w-xl text-lg leading-relaxed text-on-navy sm:text-xl">
            Grupos reducidos y clases particulares, para niños y adultos. Las da siempre Marcos, en la
            misma pista, y tu plaza la reservas desde el móvil.
          </p>
          <div className="flex flex-wrap items-center gap-5">
            <TrialButton layout="desktop" />
            <a href="#empezar" className="font-semibold text-white underline-offset-4 hover:underline">Cómo funciona</a>
          </div>
        </div>
        <CourtPhoto />
      </div>
    </header>
  );
}

const HERO = {
  src: "/web/marcos-golpe-800.webp",
  srcSet: "/web/marcos-golpe-800.webp 800w, /web/marcos-golpe-1400.webp 1400w",
  sizes: "(min-width: 1024px) 45vw, 100vw",
};

/** Marcos sits left of centre in the frame, so the crop follows him on the narrow boxes. */
function CourtPhoto() {
  // The largest paint of the page on a phone: asked for from the <head>, not when the parser
  // reaches the img.
  preload(HERO.src, { as: "image", imageSrcSet: HERO.srcSet, imageSizes: HERO.sizes, fetchPriority: "high" });
  return (
    <figure className="site-rise relative m-0 h-80 overflow-hidden rounded bg-(--site-band-photo) [animation-delay:150ms] sm:h-[460px] lg:h-[560px]">
      <Photo
        {...HERO}
        width={1400}
        height={788}
        alt={school.photos.action}
        className="object-[24%_center]"
        priority
      />
    </figure>
  );
}

function Classes() {
  return (
    <section id="clases" aria-labelledby="h-clases" className={`lg:scroll-mt-22 ${WRAP} pb-24 pt-24 lg:pt-28`}>
      <div className="mb-12 grid items-end gap-6 lg:grid-cols-2 lg:gap-12">
        <h2 id="h-clases" className={SECTION_TITLE}>Clases para cada edad y nivel</h2>
        <p className="text-lg leading-relaxed text-(--site-muted)">
          En la primera clase Marcos ve cómo juegas y te propone el grupo. Si mejoras, cambias de grupo.
        </p>
      </div>
      <div className="grid border-t-[3px] border-(--site-text) lg:grid-cols-3">
        {school.classes.map((lesson) => (
          <article
            key={lesson.name}
            className="flex flex-col gap-3.5 border-(--site-line) py-8 not-last:border-b lg:px-8 lg:not-last:border-b-0 lg:not-first:border-l lg:first:pl-0 lg:last:pr-0"
          >
            <h3 className="font-display text-4xl font-bold">{lesson.name}</h3>
            <p className="text-[17px] leading-relaxed"><Copy text={lesson.summary} /></p>
            <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-2 pt-2">
              <dt className="text-(--site-muted)">Cuándo</dt>
              <dd><Copy text={lesson.when} /></dd>
              <dt className="text-(--site-muted)">Grupo</dt>
              <dd><Copy text={lesson.group} /></dd>
              <dt className="text-(--site-muted)">Precio</dt>
              <dd className="font-semibold"><Copy text={lesson.price} /></dd>
            </dl>
          </article>
        ))}
      </div>
      <ClassFinder />
    </section>
  );
}

const STEPS = [
  {
    title: "Escríbele a Marcos",
    text: "Cuéntale para quién es y si has jugado antes. Te propone un día para probar.",
  },
  {
    title: "Prueba una clase",
    text: `Conoces la pista, el grupo y su forma de trabajar. La clase de prueba: ${school.trialPrice.toLowerCase()}.`,
  },
  {
    title: "Reserva desde la app",
    text: "Creas tu cuenta, Marcos te da de alta y desde ahí reservas y cancelas tus clases.",
  },
];

function Steps() {
  return (
    <section id="empezar" aria-labelledby="h-empezar" className="lg:scroll-mt-22 bg-(--site-steps) text-white">
      <div className={`${WRAP} py-24`}>
        <h2 id="h-empezar" className={`${SECTION_TITLE} mb-14`}>Cómo empezar</h2>
        <ol className="site-steps relative grid gap-12 lg:grid-cols-3 lg:pt-24">
          <Trajectory />
          {STEPS.map((step, index) => (
            <li key={step.title} className="relative flex flex-col gap-3 border-t-2 border-white/50 pl-20 pt-6 lg:pl-0">
              {index < STEPS.length - 1 ? <TrajectoryStretch /> : null}
              <span aria-hidden className="absolute left-0 top-6 w-12 bg-(--site-steps) pb-2 text-center font-display text-[88px] font-bold leading-[0.8] text-ball lg:static lg:w-auto lg:pb-0 lg:text-left">{index + 1}</span>
              <h3 className="font-display text-3xl font-bold">{step.title}</h3>
              <p className="text-[17px] leading-relaxed text-court-tint"><Copy text={step.text} /></p>
            </li>
          ))}
        </ol>
      </div>
    </section>
  );
}

/**
 * The ball's way from one step to the next (decision 20), drawn as the section scrolls in. On a
 * computer it bounces on each step's line; on a phone, where the steps stack, it runs down
 * beside the numbers. Without scroll-driven animations it is simply there, already drawn.
 */
function Trajectory() {
  return (
    <svg
        viewBox="0 0 1072 96"
        preserveAspectRatio="none"
        aria-hidden
        className="pointer-events-none absolute inset-x-0 top-0 hidden h-24 w-full overflow-visible lg:block"
      >
        <path d={BALL_PATH} className="fill-none stroke-white/35 stroke-2 [stroke-dasharray:2_8]" vectorEffect="non-scaling-stroke" />
        <path d={BALL_PATH} pathLength={1} className="site-trajectory fill-none stroke-ball stroke-[3]" vectorEffect="non-scaling-stroke" strokeLinecap="round" />
        {BOUNCES.map((x, index) => (
          <ellipse key={x} cx={x} cy={92} rx={11} ry={3.5} className={`site-bounce site-bounce-${index + 1} fill-ball`} />
        ))}
    </svg>
  );
}

/** On a phone, the stretch of the ball's way from one step's number down to the next one. */
function TrajectoryStretch() {
  return (
    <>
      <span aria-hidden className="absolute -bottom-18 left-6 top-28 border-l-2 border-dashed border-white/35 lg:hidden" />
      <span aria-hidden className="site-steps-line absolute -bottom-18 left-[23px] top-28 w-[3px] origin-top rounded-full bg-ball lg:hidden" />
    </>
  );
}

/** Lands on each step's number, then flies on towards the app. Units: the 1072px of the widest layout. */
const BALL_PATH = "M20 92Q206 -70 393 92Q580 -70 767 92Q900 -20 1072 30";
const BOUNCES = [20, 393, 767];

function Teacher() {
  return (
    <section aria-labelledby="h-marcos" className="bg-(--site-surface)">
      <div className={`${WRAP} grid items-center gap-12 py-24 lg:grid-cols-[440px_1fr] lg:gap-20`}>
        <figure className="m-0 flex flex-col gap-3">
          <div className="h-96 overflow-hidden rounded bg-(--site-photo) lg:h-[520px]">
            <Photo
              src="/web/marcos-campeonato-600.webp"
              width={600}
              height={680}
              alt=""
              className="object-[center_30%]"
            />
          </div>
          <figcaption className="text-sm text-(--site-muted)">{school.photos.championship}</figcaption>
        </figure>
        <div className="flex max-w-2xl flex-col gap-5">
          <h2 id="h-marcos" className={SECTION_TITLE}><Copy text={school.teacher.name} /></h2>
          <p className="text-lg leading-relaxed sm:text-xl"><Copy text={school.teacher.background} /></p>
          <p className="text-lg leading-relaxed text-(--site-muted) sm:text-xl"><Copy text={school.teacher.approach} /></p>
          <p className="text-[17px] leading-relaxed">
            Todas las clases las da él, así que conoce a cada alumno y sabe qué le toca trabajar.
          </p>
        </div>
      </div>
    </section>
  );
}

function Prices() {
  return (
    <section id="precios" aria-labelledby="h-precios" className={`lg:scroll-mt-22 ${WRAP} grid gap-10 pb-24 pt-24 lg:grid-cols-[1fr_1.2fr] lg:gap-20 lg:pt-28`}>
      <div className="flex flex-col gap-4">
        <h2 id="h-precios" className={SECTION_TITLE}>Precios</h2>
        <p className="text-lg leading-relaxed text-(--site-muted)">
          Sin matrícula ni permanencia. Se paga a Marcos en la pista; la app no cobra.
        </p>
      </div>
      <dl className="text-lg">
        {school.prices.map((price) => (
          <div key={price.label} className="flex items-baseline justify-between gap-6 border-t border-(--site-line) py-5 last:border-b">
            <dt>{price.label}</dt>
            <dd className="text-right font-display text-3xl font-bold"><Copy text={price.amount} /></dd>
          </div>
        ))}
      </dl>
    </section>
  );
}

function Questions() {
  return (
    <section id="preguntas" aria-labelledby="h-preguntas" className={`lg:scroll-mt-22 ${WRAP} grid gap-10 pb-28 lg:grid-cols-[1fr_1.2fr] lg:gap-20`}>
      <h2 id="h-preguntas" className={SECTION_TITLE}>Preguntas</h2>
      <div>
        {school.faq.map((item, index) => (
          <details key={item.question} open={index === 0} className="group border-t border-(--site-line) last:border-b">
            <summary className="flex min-h-11 cursor-pointer list-none items-center justify-between gap-6 py-5 text-xl font-semibold [&::-webkit-details-marker]:hidden">
              {item.question}
              <svg viewBox="0 0 24 24" aria-hidden className="size-6 shrink-0 fill-none stroke-current stroke-[1.8] transition-transform group-open:rotate-45">
                <path d="M12 5v14M5 12h14" strokeLinecap="round" />
              </svg>
            </summary>
            <p className="pb-6 text-[17px] leading-relaxed text-(--site-muted)"><Copy text={item.answer} /></p>
          </details>
        ))}
      </div>
    </section>
  );
}

function Contact() {
  return (
    <section id="donde" aria-labelledby="h-donde" className="lg:scroll-mt-22 bg-(--site-band) text-white">
      <div className={`${WRAP} grid items-center gap-12 py-24 lg:grid-cols-2 lg:gap-20`}>
        <div className="flex flex-col gap-5">
          <h2 id="h-donde" className={SECTION_TITLE}>Ven a probar una clase</h2>
          <p className="max-w-lg text-lg leading-relaxed text-on-navy sm:text-xl">
            Escríbele a Marcos por WhatsApp y te propone un día. Si ya eres alumno, reserva en la app.
          </p>
          <div className="flex flex-wrap items-center gap-5 pt-2">
            <TrialButton layout="desktop" />
            <Link href="/login" className="font-semibold text-white underline-offset-4 hover:underline">Entrar en la app</Link>
          </div>
        </div>
        <address className="flex flex-col gap-2 text-lg not-italic">
          <span className="font-display text-3xl font-bold">Dónde está la pista</span>
          <span><Copy text={school.address} /></span>
          <span className="text-on-navy"><Copy text={school.phone} /> · <Copy text={school.email} /></span>
          {school.mapsUrl ? (
            <a href={school.mapsUrl} className="mt-2 font-semibold text-white underline underline-offset-4">Cómo llegar</a>
          ) : null}
        </address>
      </div>
    </section>
  );
}

/** On a phone the trial button stays at hand while scrolling (the approved mobile mock-up). */
function TrialBar() {
  return (
    <div className="fixed inset-x-0 bottom-0 z-10 border-t border-white/20 bg-(--site-band) px-4 pb-5 pt-3 lg:hidden">
      <TrialButton layout="wide" />
    </div>
  );
}

type PhotoProps = {
  src: string;
  srcSet?: string;
  sizes?: string;
  width: number;
  height: number;
  alt: string;
  className: string;
  priority?: boolean;
};

/**
 * A plain img, not next/image: that one writes a style attribute, which the CSP blocks without
 * a word. The files are already sized and in webp (public/web), so there is nothing to optimise.
 */
function Photo({ priority = false, className, alt, ...image }: PhotoProps) {
  return (
    // eslint-disable-next-line @next/next/no-img-element -- see above: next/image breaks the CSP.
    <img
      {...image}
      alt={alt}
      loading={priority ? "eager" : "lazy"}
      fetchPriority={priority ? "high" : "auto"}
      decoding="async"
      className={`size-full object-cover ${className}`}
    />
  );
}

/**
 * What search engines read about the school; only the facts that are no longer placeholders.
 * A JSON-LD block is data the browser never runs, so the CSP has nothing to block in it.
 */
function StructuredData() {
  const finished = (text: string) => placeholderParts(text).every((part) => !part.pending);
  const data = {
    "@context": "https://schema.org",
    "@type": "SportsActivityLocation",
    name: `${school.name}, escuela de tenis`,
    description: DESCRIPTION,
    sport: "Tenis",
    ...(finished(school.address) ? { address: school.address } : {}),
    ...(finished(school.phone) ? { telephone: school.phone } : {}),
  };
  // "<" escaped so no text in the data can close the script element early.
  const json = JSON.stringify(data).replace(/</g, "\\u003c");
  return <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: json }} />;
}
