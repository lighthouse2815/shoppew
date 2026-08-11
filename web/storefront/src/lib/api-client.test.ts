import { afterEach, describe, expect, it, vi } from "vitest";
import { ShoppewApiClient, ShoppewApiError } from "@shoppew/api-client";

afterEach(() => vi.restoreAllMocks());

describe("ShoppewApiClient", () => {
  it("giải envelope thành dữ liệu và gửi request id", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(JSON.stringify({ success: true, data: { ok: true }, timestamp: new Date().toISOString() }), { status: 200, headers: { "Content-Type": "application/json" } }));
    const value = await new ShoppewApiClient("http://localhost:28080").request<{ ok: boolean }>("/api/v1/public/system");
    expect(value.ok).toBe(true);
    expect(new Headers(fetchMock.mock.calls[0]?.[1]?.headers).has("X-Request-Id")).toBe(true);
  });

  it("refresh đúng một lần khi access token hết hạn", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(new Response(JSON.stringify({ success: false, error: { code: "UNAUTHORIZED", message: "Hết hạn" }, timestamp: new Date().toISOString() }), { status: 401 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ success: true, data: { ok: true }, timestamp: new Date().toISOString() }), { status: 200 }));
    const refresh = vi.fn().mockResolvedValue("new-token");
    const value = await new ShoppewApiClient("http://localhost:28080", refresh).request<{ ok: boolean }>("/protected", { token: "old-token" });
    expect(value.ok).toBe(true);
    expect(refresh).toHaveBeenCalledOnce();
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });

  it("ném lỗi có code từ API", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(JSON.stringify({ success: false, error: { code: "OUT_OF_STOCK", message: "Hết hàng" }, timestamp: new Date().toISOString() }), { status: 409 }));
    await expect(new ShoppewApiClient("http://localhost:28080").request("/cart")).rejects.toMatchObject({ status: 409, code: "OUT_OF_STOCK" } satisfies Partial<ShoppewApiError>);
  });
});
