import { describe, expect, it } from "vitest";
import { placeholderParts, recommendClass, trialHref, whatsappUrl } from "./school";

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

  it("carries the message it is given", () => {
    expect(trialHref("34600111222", "Hola")).toBe("https://wa.me/34600111222?text=Hola");
  });
});

describe("recommendClass", () => {
  it("suggests the group of the learner's age", () => {
    expect(recommendClass("me", "group").lesson.name).toBe("Adultos");
    expect(recommendClass("child", "group").lesson.name).toBe("Niños");
  });

  it("suggests a private class to whoever wants one, of any age", () => {
    expect(recommendClass("me", "private").lesson.name).toBe("Particulares");
    expect(recommendClass("child", "private").lesson.name).toBe("Particulares");
  });

  it("writes the trial message with what was chosen", () => {
    expect(recommendClass("child", "group").message).toBe(
      "Hola, Marcos. Me gustaría que mi hijo o hija probara una clase de tenis en el grupo de niños.",
    );
    expect(recommendClass("me", "private").message).toBe("Hola, Marcos. Me gustaría probar una clase particular de tenis.");
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
