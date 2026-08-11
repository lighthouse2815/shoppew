import type { Metadata } from "next";
import type { ProductDetail } from "@/lib/types";

const DEFAULT_SITE_URL = "http://localhost:3000";
const MAX_DESCRIPTION_LENGTH = 160;

type Environment = Record<string, string | undefined>;

export function getMetadataBase(environment: Environment = process.env): URL {
  const configured = (
    environment.NEXT_PUBLIC_SITE_URL ??
    environment.APP_WEB_BASE_URL ??
    environment.SHOPPEW_STOREFRONT_URL ??
    environment.VERCEL_PROJECT_PRODUCTION_URL ??
    environment.VERCEL_URL
  )?.trim();
  const hasUnsupportedScheme = configured ? /^[a-z][a-z\d+.-]*:/i.test(configured) && !/^https?:\/\//i.test(configured) : false;
  const candidate = configured
    ? hasUnsupportedScheme
      ? DEFAULT_SITE_URL
      : /^https?:\/\//i.test(configured)
      ? configured
      : `https://${configured}`
    : DEFAULT_SITE_URL;

  try {
    const url = new URL(candidate);
    if (url.protocol !== "http:" && url.protocol !== "https:") return new URL(DEFAULT_SITE_URL);
    url.pathname = "/";
    url.search = "";
    url.hash = "";
    return url;
  } catch {
    return new URL(DEFAULT_SITE_URL);
  }
}

export function absoluteStorefrontUrl(path: string, base = getMetadataBase()): string {
  return new URL(path, base).toString();
}

export function conciseDescription(value: string | undefined | null, fallback: string): string {
  const normalized = (value || fallback)
    .replace(/<[^>]*>/g, " ")
    .replace(/\s+/g, " ")
    .trim();

  if (normalized.length <= MAX_DESCRIPTION_LENGTH) return normalized;
  const candidate = normalized.slice(0, MAX_DESCRIPTION_LENGTH - 1);
  const lastWordBoundary = candidate.lastIndexOf(" ");
  const truncated = lastWordBoundary >= 100 ? candidate.slice(0, lastWordBoundary) : candidate;
  return `${truncated.trimEnd()}…`;
}

export function noIndexMetadata(title: string, description: string, follow = false): Metadata {
  return {
    title,
    description,
    robots: {
      index: false,
      follow,
      googleBot: { index: false, follow },
    },
  };
}

function httpUrl(value: string | undefined | null, base: URL): string | undefined {
  if (!value) return undefined;
  try {
    const url = new URL(value, base);
    return url.protocol === "http:" || url.protocol === "https:" ? url.toString() : undefined;
  } catch {
    return undefined;
  }
}

export function buildProductStructuredData(product: ProductDetail, base = getMetadataBase()): Record<string, unknown> {
  const slug = product.slug ? encodeURIComponent(product.slug) : "";
  const productUrl = absoluteStorefrontUrl(`/product/${slug}`, base);
  const images = [...(product.images ?? [])]
    .sort((left, right) => (left.sortOrder ?? 0) - (right.sortOrder ?? 0))
    .flatMap((image) => {
      const url = httpUrl(image.url, base);
      return url ? [url] : [];
    });
  const activeVariants = (product.variants ?? []).filter(
    (variant) => variant.status === "ACTIVE" && typeof variant.price === "number" && Number.isFinite(variant.price) && variant.price >= 0,
  );
  const currency = activeVariants.find((variant) => variant.currency)?.currency ?? "VND";
  const comparableVariants = activeVariants.filter((variant) => (variant.currency ?? currency) === currency);
  const prices = comparableVariants.map((variant) => variant.price as number);
  const rating = product.ratingAverage;
  const reviewCount = product.reviewCount;
  const description = conciseDescription(
    product.shortDescription || product.description,
    `${product.name ?? "Sản phẩm"} tại shoppew.`,
  );

  return {
    "@context": "https://schema.org",
    "@type": "Product",
    name: product.name ?? "Sản phẩm",
    description,
    url: productUrl,
    ...(images.length ? { image: images } : {}),
    ...(product.categoryName ? { category: product.categoryName } : {}),
    ...(product.brandName ? { brand: { "@type": "Brand", name: product.brandName } } : {}),
    ...(typeof rating === "number" && rating >= 1 && rating <= 5 && typeof reviewCount === "number" && reviewCount > 0
      ? {
          aggregateRating: {
            "@type": "AggregateRating",
            ratingValue: rating,
            bestRating: 5,
            worstRating: 1,
            ratingCount: reviewCount,
          },
        }
      : {}),
    ...(prices.length
      ? {
          offers: {
            "@type": "AggregateOffer",
            url: productUrl,
            priceCurrency: currency,
            lowPrice: Math.min(...prices),
            highPrice: Math.max(...prices),
            offerCount: prices.length,
            ...(product.shopName
              ? {
                  seller: {
                    "@type": "Organization",
                    name: product.shopName,
                    ...(product.shopSlug
                      ? { url: absoluteStorefrontUrl(`/shop/${encodeURIComponent(product.shopSlug)}`, base) }
                      : {}),
                  },
                }
              : {}),
          },
        }
      : {}),
  };
}

export function serializeJsonLd(value: unknown): string {
  return JSON.stringify(value)
    .replace(/</g, "\\u003c")
    .replace(/\u2028/g, "\\u2028")
    .replace(/\u2029/g, "\\u2029");
}
