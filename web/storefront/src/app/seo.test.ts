import { describe, expect, it } from "vitest";
import {
  absoluteStorefrontUrl,
  buildProductStructuredData,
  conciseDescription,
  getMetadataBase,
  serializeJsonLd,
} from "./seo";

describe("storefront SEO helpers", () => {
  it("normalizes configured storefront origins and rejects unsafe protocols", () => {
    expect(getMetadataBase({ NEXT_PUBLIC_SITE_URL: "shoppew.vn/catalog?q=old" }).toString()).toBe("https://shoppew.vn/");
    expect(getMetadataBase({ NEXT_PUBLIC_SITE_URL: "javascript:alert(1)" }).toString()).toBe("http://localhost:3000/");
    expect(absoluteStorefrontUrl("/product/ao-thun", new URL("https://shoppew.vn/base/"))).toBe(
      "https://shoppew.vn/product/ao-thun",
    );
  });

  it("creates concise plain-text descriptions", () => {
    expect(conciseDescription("  Áo <strong>cotton</strong>  thoáng mát ", "fallback")).toBe("Áo cotton thoáng mát");
    expect(conciseDescription("", "Mô tả dự phòng")).toBe("Mô tả dự phòng");
    expect(conciseDescription("từ ".repeat(100), "fallback").length).toBeLessThanOrEqual(160);
  });

  it("builds product data only from available API facts", () => {
    const data = buildProductStructuredData(
      {
        name: "Áo cotton",
        slug: "ao-cotton",
        shortDescription: "Áo cotton mềm",
        brandName: "Nhãn Việt",
        categoryName: "Thời trang",
        shopName: "Xưởng Việt",
        shopSlug: "xuong-viet",
        ratingAverage: 4.7,
        reviewCount: 12,
        images: [{ url: "/media/ao.png", sortOrder: 0 }],
        variants: [
          { status: "ACTIVE", price: 120000, currency: "VND" },
          { status: "ACTIVE", price: 150000, currency: "VND" },
          { status: "INACTIVE", price: 1000, currency: "VND" },
        ],
      },
      new URL("https://shoppew.vn"),
    );

    expect(data).toMatchObject({
      "@type": "Product",
      name: "Áo cotton",
      url: "https://shoppew.vn/product/ao-cotton",
      image: ["https://shoppew.vn/media/ao.png"],
      aggregateRating: { ratingValue: 4.7, ratingCount: 12 },
      offers: { lowPrice: 120000, highPrice: 150000, offerCount: 2, priceCurrency: "VND" },
    });
    expect(data).not.toHaveProperty("availability");
  });

  it("escapes script-breaking characters in JSON-LD", () => {
    const serialized = serializeJsonLd({ name: "</script><script>alert(1)</script>\u2028" });
    expect(serialized).not.toContain("</script>");
    expect(serialized).toContain("\\u003c/script>");
    expect(serialized).toContain("\\u2028");
  });
});
