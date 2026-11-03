import { describe, expect, it } from "vitest";
import { installStepsFor } from "@/modules/student/install";

const IPHONE = "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1";
const IPAD = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15";
const ANDROID = "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36";
const DESKTOP = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36";

describe("installStepsFor", () => {
  it("gives Safari's steps on an iPhone and on an iPad, which says it is a Mac", () => {
    expect(installStepsFor(IPHONE, false, 5)).toBe("ios");
    expect(installStepsFor(IPAD, false, 5)).toBe("ios");
  });

  it("gives Chrome's steps on Android", () => {
    expect(installStepsFor(ANDROID, false, 5)).toBe("android");
  });

  it("says nothing on a computer, a real Mac included", () => {
    expect(installStepsFor(DESKTOP, false, 0)).toBeNull();
    expect(installStepsFor(IPAD, false, 0)).toBeNull();
  });

  it("says nothing once the app is installed", () => {
    expect(installStepsFor(IPHONE, true, 5)).toBeNull();
    expect(installStepsFor(ANDROID, true, 5)).toBeNull();
  });
});
