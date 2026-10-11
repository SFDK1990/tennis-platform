/**
 * Everything the public website says about the school, in one place (31-fase31, decision 5).
 * What is still missing stays as a visible [placeholder], never as a figure that could pass for a
 * real one. What it says about the app is only what the app does today.
 *
 * The values marked DEMO are invented to show the site while it is tested: they have to be
 * replaced before deploying (Fase 17), like any placeholder.
 */
export const school = {
  name: "Oasis Papagayo",
  area: "Corralejo, Fuerteventura",
  /**
   * International format, digits only (34600111222). Until it exists, the trial button points at
   * the contact details. DEMO: left empty on purpose, so the button never opens a chat with
   * whoever owns an invented number.
   */
  whatsapp: null as string | null,
  /** DEMO */
  phone: "+34 600 000 000",
  /** DEMO: a reserved domain, so nothing sent there reaches anyone. */
  email: "hola@oasispapagayo.example",
  address: "Pista de tenis del Oasis Papagayo, Corralejo, La Oliva (Fuerteventura)",
  /** A link to the court on a map. */
  mapsUrl: "https://www.google.com/maps/search/?api=1&query=Oasis+Papagayo+Corralejo" as string | null,
  teacher: {
    name: "Marcos Asencio Scattolo",
    background:
      "Jugador de competición con muchos años de experiencia en los torneos de Fuerteventura. En 2026 obtuvo la titulación de profesor de tenis y hoy dirige la escuela del Oasis Papagayo, donde enseña lo que ha aprendido compitiendo.",
    /** DEMO: a first draft, until Marcos says it in his own words. */
    approach:
      "Con quien empieza, lo primero es pasarlo bien y que la pelota pase la red. Con quien ya juega, corregir lo que le frena y llevarlo al partido.",
  },
  photos: {
    action: "Marcos golpea una derecha en salto en la pista",
    championship: "Marcos en el Campeonato de Fuerteventura 2026",
  },
  classes: [
    {
      name: "Niños",
      summary: "De 4 a 14 años. Aprenden jugando, en grupos de su edad y su nivel: minitenis hasta los 7 y escuela a partir de los 8.",
      /** DEMO */
      when: "Lunes y miércoles, de 17:00 a 18:00",
      group: "Hasta 6 alumnos",
    },
    {
      name: "Adultos",
      summary: "Empiezas de cero o vuelves después de años. Grupos por nivel, sin prisa.",
      /** DEMO */
      when: "Martes y jueves, de 19:00 a 20:00",
      group: "Hasta 4 alumnos",
    },
    {
      name: "Particulares",
      summary: "Una hora solo para ti: corregir un golpe, preparar un partido o avanzar más rápido.",
      when: "Según los huecos de Marcos",
      group: "Tú solo, o con un amigo",
    },
  ],
  prices: [
    { label: "Grupo, un día a la semana", amount: "100 € al mes" },
    { label: "Grupo, dos días a la semana", amount: "150 € al mes" },
    { label: "Clase particular", amount: "30 € la hora" },
    { label: "Clase de prueba", amount: "Gratis" },
  ],
  faq: [
    {
      question: "¿Necesito raqueta para la prueba?",
      answer: "No. El día de la prueba Marcos te presta una raqueta; tú solo trae ropa y calzado de deporte.",
    },
    {
      question: "¿Y si un día no puedo ir?",
      answer: "Cancelas la reserva en la app hasta 24 horas antes y la plaza queda libre para otro alumno.",
    },
    {
      question: "¿Qué pasa si llueve?",
      answer: "La clase se aplaza. Marcos te avisa y te propone otro día.",
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
