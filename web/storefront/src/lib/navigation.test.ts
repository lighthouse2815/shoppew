import { describe, expect, it } from "vitest";
import { safeInternalPath } from "./navigation";

describe("safeInternalPath", () => {
  it("keeps a local path with query and fragment", () => {
    expect(safeInternalPath("/products/sku-1?variant=red#details")).toBe(
      "/products/sku-1?variant=red#details",
    );
  });

  it.each([
    "https://attacker.example/phish",
    "//attacker.example/phish",
    "/\\attacker.example/phish",
    "/account\nhttps://attacker.example",
    "javascript:alert(1)",
  ])("rejects an unsafe return path: %s", (candidate) => {
    expect(safeInternalPath(candidate)).toBe("/account");
  });
});
