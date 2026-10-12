import type { ReactNode } from "react";
import { school } from "./school";
import { SiteFooter, SiteNav, WRAP } from "./SiteNav";

/**
 * The frame of the legal texts: the website's band and footer around a page that is meant to be
 * read, so one column at a reading measure and nothing else competing with it.
 */
export function LegalDocument({ title, intro, children }: { title: string; intro: string; children: ReactNode }) {
  return (
    <>
      <SiteNav />
      <header className="bg-(--site-band) text-white">
        <div className={`${WRAP} flex flex-col gap-4 pb-16 pt-8`}>
          <h1 className="font-display text-6xl font-bold leading-[0.92] sm:text-7xl">{title}</h1>
          <p className="max-w-2xl text-lg leading-relaxed text-on-navy">{intro}</p>
          <p className="text-sm text-on-navy">Actualizado el {school.legalUpdated}</p>
        </div>
      </header>
      <main className={`${WRAP} py-16`}>
        <div className="flex max-w-[68ch] flex-col gap-4 text-[17px] leading-relaxed [&_a]:text-(--site-link) [&_a]:underline [&_a]:underline-offset-4 [&_h2]:mt-8 [&_h2:first-child]:mt-0 [&_h2]:font-display [&_h2]:text-3xl [&_h2]:font-bold [&_h2]:leading-tight [&_li]:pl-1 [&_ul]:flex [&_ul]:list-disc [&_ul]:flex-col [&_ul]:gap-2 [&_ul]:pl-6">
          {children}
        </div>
      </main>
      <SiteFooter />
    </>
  );
}

/** The owner's details, which both texts give in the same form. */
export function OwnerDetails() {
  const { owner } = school;
  return (
    <dl className="grid grid-cols-[auto_1fr] gap-x-6 gap-y-2 border-y border-(--site-line) py-5">
      <dt className="text-(--site-muted)">Titular</dt>
      <dd>{owner.name}, profesor de tenis (autónomo)</dd>
      <dt className="text-(--site-muted)">NIF</dt>
      <dd>{owner.taxId}</dd>
      <dt className="text-(--site-muted)">Domicilio</dt>
      <dd>{owner.address}</dd>
      <dt className="text-(--site-muted)">Correo</dt>
      <dd><a href={`mailto:${owner.privacyEmail}`}>{owner.privacyEmail}</a></dd>
      <dt className="text-(--site-muted)">Teléfono</dt>
      <dd>{school.phone}</dd>
    </dl>
  );
}
