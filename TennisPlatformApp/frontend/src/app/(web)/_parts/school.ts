/**
 * Everything the public website says about the school, in one place (31-fase31, decision 5).
 * What is still missing stays as a visible [placeholder], never as a figure that could pass for a
 * real one. What it says about the app is only what the app does today.
 *
 * The values marked DEMO are invented to show the site while it is tested: they have to be
 * replaced before deploying (Fase 17), like any placeholder.
 */
/** Each price once: the class cards and the price list both read them from here. */
const PRICES = {
  groupOneDay: "100 € al mes",
  groupTwoDays: "150 € al mes",
  private: "30 € la hora",
  trial: "Gratis",
};

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
  /** Who answers for the website and the students' data: Marcos, as a self-employed teacher. */
  owner: {
    name: "Marcos Asencio Scattolo",
    /** DEMO */
    taxId: "00000000T",
    /** DEMO */
    address: "Calle de Ejemplo 1, 35660 Corralejo, La Oliva (Las Palmas)",
    /** DEMO: a reserved domain, like the contact email. */
    privacyEmail: "privacidad@oasispapagayo.example",
  },
  /** The date the legal texts last changed, as they show it. */
  legalUpdated: "12 de octubre de 2026",
  /**
   * The front page's background loop (decision 23), served from public/web: muted, a few
   * seconds, already compressed. Without one, the front page is plain navy.
   */
  heroVideo: {
    /** DEMO: a Pexels clip (free for commercial use, no credit needed), until there is one of Marcos. */
    src: "/web/portada.mp4",
    poster: "/web/portada-poster.webp",
    description: "Pelotas de tenis encajadas en la valla de una pista",
  } as { src: string; poster: string; description: string } | null,
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
      price: `Desde ${PRICES.groupOneDay}`,
    },
    {
      name: "Adultos",
      summary: "Empiezas de cero o vuelves después de años. Grupos por nivel, sin prisa.",
      /** DEMO */
      when: "Martes y jueves, de 19:00 a 20:00",
      group: "Hasta 4 alumnos",
      price: `Desde ${PRICES.groupOneDay}`,
    },
    {
      name: "Particulares",
      summary: "Una hora solo para ti: corregir un golpe, preparar un partido o avanzar más rápido.",
      when: "Según los huecos de Marcos",
      group: "Tú solo, o con un amigo",
      price: PRICES.private,
    },
  ],
  prices: [
    { label: "Grupo, un día a la semana", amount: PRICES.groupOneDay },
    { label: "Grupo, dos días a la semana", amount: PRICES.groupTwoDays },
    { label: "Clase particular", amount: PRICES.private },
    { label: "Clase de prueba", amount: PRICES.trial },
  ],
  trialPrice: PRICES.trial,
  faq: [
    {
      question: "¿Necesito raqueta para la prueba?",
      answer: "No. El día de la prueba Marcos te presta una.",
    },
    {
      question: "¿Qué tengo que traer?",
      answer: "Ropa y calzado de deporte, y agua. Si tienes raqueta, tráela; si no, el primer día te la presta Marcos.",
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
export function trialHref(number: string | null = school.whatsapp, message: string = TRIAL_MESSAGE): string {
  return number ? whatsappUrl(number, message) : "#donde";
}

export type Learner = "me" | "child";
export type Format = "group" | "private";

/**
 * "¿Qué clase es para mí?" (decision 21): the class to suggest, and the WhatsApp message that
 * asks Marcos for its trial already saying what was chosen.
 */
export function recommendClass(learner: Learner, format: Format) {
  const name = format === "private" ? "Particulares" : learner === "child" ? "Niños" : "Adultos";
  const lesson = school.classes.find((candidate) => candidate.name === name);
  if (!lesson) {
    throw new Error(`There is no "${name}" class in school.ts.`);
  }
  const who = learner === "child" ? "que mi hijo o hija probara" : "probar";
  const what = format === "private" ? "una clase particular de tenis" : `una clase de tenis en el grupo de ${name.toLowerCase()}`;
  return { lesson, message: `Hola, Marcos. Me gustaría ${who} ${what}.` };
}

/** Splits a text into plain parts and the [placeholders] still waiting for real content. */
export function placeholderParts(text: string): { text: string; pending: boolean }[] {
  return text
    .split(/(\[[^\]]+\])/)
    .filter(Boolean)
    .map((part) => ({ text: part, pending: /^\[[^\]]+\]$/.test(part) }));
}
