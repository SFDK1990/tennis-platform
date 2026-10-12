import type { Metadata } from "next";
import Link from "next/link";
import { LegalDocument, OwnerDetails } from "@/app/(web)/_parts/LegalDocument";
import { school } from "@/app/(web)/_parts/school";
import { THEME_COOKIE } from "@/app/(web)/_parts/theme";

export const metadata: Metadata = {
  title: `Política de privacidad de ${school.name}`,
  description: `Qué datos personales trata ${school.name}, escuela de tenis, para qué, cuánto tiempo y cómo ejercer tus derechos.`,
  alternates: { canonical: "/privacidad" },
};

/**
 * What the GDPR (art. 13) asks to tell whoever gives their data (31-fase31, decisions 13-17).
 * Every fact here is one the app makes true: change the app, change this text.
 */
export default function PrivacyPage() {
  const { owner } = school;
  return (
    <LegalDocument
      title="Privacidad"
      intro="Qué datos tuyos guarda la escuela, para qué, cuánto tiempo y cómo puedes consultarlos, corregirlos o borrarlos."
    >
      <h2>Responsable</h2>
      <p>Tus datos los trata {owner.name}, que da las clases como profesional autónomo.</p>
      <OwnerDetails />

      <h2>Qué datos se guardan</h2>
      <ul>
        <li>Para tu cuenta: tu email y tu contraseña, que se guarda cifrada y nadie puede leer.</li>
        <li>En tu perfil: tu nombre y apellidos y, si lo das, tu teléfono.</li>
        <li>Al usar la app: tus reservas, tus cancelaciones y la asistencia que marca Marcos.</li>
      </ul>
      <p>
        La web no recoge ningún dato: no tiene formularios, ni analítica, ni publicidad. La clase de prueba se pide por
        WhatsApp, fuera de esta web.
      </p>

      <h2>Para qué y con qué base</h2>
      <ul>
        <li>
          Gestionar tu cuenta, tus clases y tus reservas, y enviarte los correos del servicio, como el de verificar tu
          email o el de recuperar la contraseña. Es necesario para darte las clases que contratas (RGPD, art. 6.1.b).
        </li>
        <li>
          Proteger el servicio, por ejemplo limitando los intentos de entrar con una contraseña. Para eso se usa tu
          dirección IP durante unos minutos, sin guardarla. Es un interés legítimo (RGPD, art. 6.1.f).
        </li>
      </ul>
      <p>Tus datos no se usan para publicidad ni para tomar decisiones automáticas sobre ti.</p>

      <h2>Cuánto tiempo</h2>
      <ul>
        <li>
          Mientras tengas cuenta. Si la borras, se borran tus datos personales; tus reservas pasadas se quedan sin nada
          que te identifique, para que las cuentas de cada clase sigan cuadrando.
        </li>
        <li>Las copias de seguridad y los registros técnicos del servidor se guardan 14 días.</li>
      </ul>

      <h2>Quién más los trata</h2>
      <p>No se ceden a nadie, salvo que la ley lo obligue. Dos proveedores los tratan por encargo y con contrato:</p>
      <ul>
        <li>
          Cloudflare, Inc., que conecta de forma segura la app con internet: tus peticiones pasan por su red. Puede
          tratarlas en Estados Unidos, al amparo del Marco de Privacidad de Datos UE-EE. UU.
        </li>
        <li>El servicio de envío de correo, para los correos de la app.</li>
      </ul>

      <h2>Tus derechos</h2>
      <p>
        Puedes consultar, corregir y borrar tus datos, llevártelos, oponerte a que se traten o pedir que se limite su
        uso. Desde <Link href="/profile">tu perfil</Link> puedes corregirlos, descargarlos en un archivo y borrar tu
        cuenta. Para lo demás, escribe a <a href={`mailto:${owner.privacyEmail}`}>{owner.privacyEmail}</a>: tienes
        respuesta en menos de un mes.
      </p>
      <p>
        Si crees que no se han tratado bien tus datos, puedes reclamar ante la{" "}
        <a href="https://www.aepd.es">Agencia Española de Protección de Datos</a>.
      </p>

      <h2>Menores</h2>
      <p>
        Las cuentas son para mayores de 14 años. Para apuntar a un niño, sus padres hablan con Marcos, que les dice
        cómo reservar su plaza.
      </p>

      <h2>Cookies</h2>
      <p>La web y la app sólo usan las cookies imprescindibles para funcionar, así que no te piden permiso:</p>
      <ul>
        <li><code>refresh_token</code>: mantiene tu sesión abierta. Dura 14 días y sólo la lee el servidor.</li>
        <li><code>XSRF-TOKEN</code>: impide que otra web haga cosas en tu nombre. Se borra al cerrar el navegador.</li>
        <li><code>{THEME_COOKIE}</code>: recuerda si elegiste el modo claro o el oscuro en la web. Dura un año.</li>
      </ul>
      <p>Ninguna sirve para seguirte ni para mostrarte publicidad.</p>

      <h2>Cambios</h2>
      <p>
        Si esta política cambia, se actualiza la fecha de arriba. Si el cambio afecta a lo que se hace con tus datos,
        Marcos avisa a sus alumnos antes.
      </p>
    </LegalDocument>
  );
}
