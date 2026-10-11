/**
 * Everything the public website says about the school, in one place (31-fase31, decision 5).
 * What Daniel has not given yet stays as a visible [placeholder], never as a figure that could
 * pass for a real one. What it says about the app is only what the app does today.
 */
export const school = {
  name: "Oasis Papagayo",
  area: "[barrio, ciudad]",
  /** International format, digits only (34600111222). Until it exists, the trial button points at the contact details. */
  whatsapp: null as string | null,
  phone: "[Teléfono]",
  email: "[Correo]",
  address: "[Dirección de la pista]",
  /** A link to the court on a map. */
  mapsUrl: null as string | null,
  teacher: {
    name: "Marcos [apellido]",
    background: "[Titulación de entrenador, dónde se formó y cuántos años lleva enseñando.]",
    approach: "[Cómo entiende las clases: qué busca en un niño que empieza y en un adulto que vuelve a la pista.]",
  },
  classes: [
    {
      name: "Niños",
      summary: "[Edades]. Aprenden jugando, en grupos de su edad y su nivel.",
      when: "[Días y horas]",
      group: "Hasta [n] alumnos",
    },
    {
      name: "Adultos",
      summary: "Empiezas de cero o vuelves después de años. Grupos por nivel, sin prisa.",
      when: "[Días y horas]",
      group: "Hasta [n] alumnos",
    },
    {
      name: "Particulares",
      summary: "Una hora solo para ti: corregir un golpe, preparar un partido o avanzar más rápido.",
      when: "Según los huecos de Marcos",
      group: "Tú solo, o con un amigo",
    },
  ],
  prices: [
    { label: "Grupo, un día a la semana", amount: "[€] al mes" },
    { label: "Grupo, dos días a la semana", amount: "[€] al mes" },
    { label: "Clase particular", amount: "[€] la hora" },
    { label: "Clase de prueba", amount: "[Gratis / €]" },
  ],
  faq: [
    {
      question: "¿Necesito raqueta para la prueba?",
      answer: "[Respuesta de Marcos: si presta raqueta y pelotas el primer día.]",
    },
    {
      question: "¿Y si un día no puedo ir?",
      answer: "Cancelas la reserva en la app hasta 24 horas antes y la plaza queda libre para otro alumno.",
    },
    {
      question: "¿Qué pasa si llueve?",
      answer: "[Qué hace Marcos: si la clase se recupera y cómo avisa.]",
    },
    {
      question: "¿Cómo apunto a mi hijo?",
      answer: "Escríbele a Marcos y te dice qué grupo le va y cómo reservar su plaza.",
    },
  ],
};

export const TRIAL_MESSAGE = "Hola, Marcos. Me gustaría probar una clase de tenis.";

/** A WhatsApp chat with the message already written; wa.me only takes the number's digits. */
export function whatsappUrl(number: string, message: string): string {
  if (!/^\d{8,15}$/.test(number)) {
    throw new Error("A WhatsApp number is its international digits, with no '+' or spaces.");
  }
  return `https://wa.me/${number}?text=${encodeURIComponent(message)}`;
}

/** Where the trial-class button goes: WhatsApp once there is a number, the contact details until then. */
export function trialHref(number: string | null = school.whatsapp): string {
  return number ? whatsappUrl(number, TRIAL_MESSAGE) : "#donde";
}

/** Splits a text into plain parts and the [placeholders] still waiting for real content. */
export function placeholderParts(text: string): { text: string; pending: boolean }[] {
  return text
    .split(/(\[[^\]]+\])/)
    .filter(Boolean)
    .map((part) => ({ text: part, pending: /^\[[^\]]+\]$/.test(part) }));
}
