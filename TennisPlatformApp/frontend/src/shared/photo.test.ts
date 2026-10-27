import { describe, expect, it } from "vitest";
import { centerSquare } from "@/shared/photo";

describe("centerSquare", () => {
  it("keeps the middle of a landscape photo", () => {
    expect(centerSquare(4000, 3000)).toEqual({ x: 500, y: 0, side: 3000 });
  });

  it("keeps the middle of a portrait photo", () => {
    expect(centerSquare(1080, 1921)).toEqual({ x: 0, y: 420, side: 1080 });
  });

  it("leaves a square as it is", () => {
    expect(centerSquare(400, 400)).toEqual({ x: 0, y: 0, side: 400 });
  });
});
