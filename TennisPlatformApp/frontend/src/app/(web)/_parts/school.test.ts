import { describe, expect, it } from "vitest";
import { placeholderParts, trialHref, whatsappUrl } from "./school";

describe("whatsappUrl", () => {
  it("opens a chat with the number and the message already written", () => {
    expect(whatsappUrl("34600111222", "Hola, Marcos. ¿Probamos?")).toBe(
      "https://wa.me/34600111222?text=Hola%2C%20Marcos.%20%C2%BFProbamos%3F",
    );
  });

  it("refuses a number wa.me would not understand", () => {
    expect(() => whatsappUrl("+34 600 111 222", "Hola")).toThrow();
  });
});

describe("trialHref", () => {
  it("points at the contact details while there is no WhatsApp number", () => {
    expect(trialHref(null)).toBe("#donde");
  });

  it("points at WhatsApp once there is one", () => {
    expect(trialHref("34600111222")).toMatch(/^https:\/\/wa\.me\/34600111222\?text=/);
  });
});

describe("placeholderParts", () => {
  it("marks each [placeholder] apart from the real text around it", () => {
    expect(placeholderParts("Hasta [n] alumnos")).toEqual([
      { text: "Hasta ", pending: false },
      { text: "[n]", pending: true },
      { text: " alumnos", pending: false },
    ]);
  });

  it("leaves a finished text as one plain part", () => {
    expect(placeholderParts("Tú solo, o con un amigo")).toEqual([{ text: "Tú solo, o con un amigo", pending: false }]);
  });
});
