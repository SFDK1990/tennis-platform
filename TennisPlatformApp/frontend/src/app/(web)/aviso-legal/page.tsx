import type { Metadata } from "next";
import Link from "next/link";
import { LegalDocument, OwnerDetails } from "@/app/(web)/_parts/LegalDocument";
import { school } from "@/app/(web)/_parts/school";

export const metadata: Metadata = {
  title: `Aviso legal de ${school.name}`,
  description: `Quién está detrás de la web y la app de ${school.name}, escuela de tenis, y las condiciones para usarlas.`,
  alternates: { canonical: "/aviso-legal" },
};

/** What the LSSI (art. 10) asks a website to say about its owner (31-fase31, decision 13). */
export default function LegalNoticePage() {
  return (
    <LegalDocument
      title="Aviso legal"
      intro="Quién está detrás de esta web y de la app de reservas, y las condiciones para usarlas."
    >
      <h2>Titular</h2>
      <p>
        La web y la app son de {school.owner.name}, que da las clases de la escuela de tenis {school.name} como
        profesional autónomo.
      </p>
      <OwnerDetails />

      <h2>Qué ofrecen</h2>
      <p>
        La web informa de las clases de tenis que da Marcos en las pistas del {school.name}, en {school.area}. La app
        sirve a sus alumnos para reservar y cancelar sus plazas. La app no cobra: las clases se pagan a Marcos en la
        escuela.
      </p>

      <h2>Condiciones de uso</h2>
      <p>Al usar la web y la app te comprometes a:</p>
      <ul>
        <li>dar datos ciertos al crear tu cuenta y mantenerlos al día;</li>
        <li>no compartir tu contraseña ni entrar en la cuenta de otra persona;</li>
        <li>no intentar saltarte las medidas de seguridad ni sobrecargar el servicio.</li>
      </ul>
      <p>Una cuenta que incumpla estas condiciones puede desactivarse.</p>

      <h2>Propiedad intelectual</h2>
      <p>
        Los textos, el logotipo y las fotografías son del titular o se publican con permiso de quien aparece en ellas.
        No se pueden copiar ni reutilizar sin su autorización.
      </p>

      <h2>Responsabilidad</h2>
      <p>
        Los precios y horarios de la web se mantienen al día, pero si alguno cambia, vale lo que Marcos te confirme.
        El titular no responde de las interrupciones del servicio por causas ajenas a él, ni del contenido de las webs
        a las que se enlaza, como WhatsApp o Google Maps.
      </p>

      <h2>Datos personales</h2>
      <p>
        Cómo se tratan los datos de los alumnos lo explica la <Link href="/privacidad">política de privacidad</Link>.
      </p>

      <h2>Ley aplicable</h2>
      <p>
        Estas condiciones se rigen por la ley española. Si eres consumidor, cualquier conflicto se resuelve en los
        juzgados de tu domicilio.
      </p>
    </LegalDocument>
  );
}
