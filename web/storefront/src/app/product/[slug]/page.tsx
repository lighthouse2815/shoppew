import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { cache } from "react";
import { ShieldCheck, Star, Store } from "lucide-react";
import { EmptyState } from "@shoppew/ui";
import { ProductActions } from "@/components/product-actions";
import { publicApi } from "@/lib/api";
import { formatDateTime } from "@/lib/format";
import type { Page, ProductDetail, Review } from "@/lib/types";
import { SafeImage } from "@/components/safe-image";
import { ProductChatButton, ProductViewRecorder } from "@/components/product-engagement";
import { ProductCard } from "@/components/product-card";
import type { ProductSummary } from "@/lib/types";
import { buildProductStructuredData, conciseDescription, noIndexMetadata, serializeJsonLd } from "../../seo";

export const dynamic = "force-dynamic";

const loadProduct = cache((slug: string) =>
  publicApi.request<ProductDetail>(`/api/v1/public/products/${encodeURIComponent(slug)}`),
);

export async function generateMetadata({ params }: { params: Promise<{ slug: string }> }): Promise<Metadata> {
  const { slug } = await params;
  let product: ProductDetail;
  try {
    product = await loadProduct(slug);
  } catch {
    return noIndexMetadata("Sản phẩm không tồn tại", "Không tìm thấy sản phẩm công khai này trên shoppew.");
  }

  const name = product.name ?? "Sản phẩm";
  const description = conciseDescription(
    product.shortDescription || product.description,
    `${name} từ ${product.shopName ?? "nhà bán trên shoppew"}.`,
  );
  const canonical = `/product/${encodeURIComponent(product.slug ?? slug)}`;
  const images = [...(product.images ?? [])]
    .sort((left, right) => (left.sortOrder ?? 0) - (right.sortOrder ?? 0))
    .flatMap((image) => (image.url ? [{ url: image.url, alt: image.altText ?? name }] : []));
  const pricedVariants = (product.variants ?? []).filter(
    (variant) =>
      variant.status === "ACTIVE" &&
      typeof variant.price === "number" &&
      Number.isFinite(variant.price) &&
      variant.price >= 0,
  );
  const currency = pricedVariants.find((variant) => variant.currency)?.currency ?? "VND";
  const comparablePrices = pricedVariants
    .filter((variant) => (variant.currency ?? currency) === currency)
    .map((variant) => variant.price as number);
  const minimumPrice = comparablePrices.length ? Math.min(...comparablePrices) : undefined;

  return {
    title: name,
    description,
    alternates: { canonical },
    openGraph: {
      title: `${name} · shoppew`,
      description,
      url: canonical,
      type: "website",
      images,
    },
    twitter: {
      card: images.length ? "summary_large_image" : "summary",
      title: `${name} · shoppew`,
      description,
      images: images.map((image) => image.url),
    },
    ...(minimumPrice !== undefined
      ? {
          other: {
            "product:price:amount": String(minimumPrice),
            "product:price:currency": currency,
          },
        }
      : {}),
  };
}

async function loadOptional<T>(request: Promise<T>, fallback: T) {
  try {
    return { data: await request, failed: false };
  } catch {
    return { data: fallback, failed: true };
  }
}

export default async function ProductPage({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params;
  const product = await loadProduct(slug).catch(() => notFound());
  const [reviewResult, relatedResult, sameShopResult] = product.id ? await Promise.all([
    loadOptional(publicApi.request<Page<Review>>(`/api/v1/public/products/${product.id}/reviews?size=10`), { content: [] }),
    loadOptional(publicApi.request<ProductSummary[]>(`/api/v1/public/recommendations/products/${product.id}/related?size=10`), []),
    loadOptional(publicApi.request<ProductSummary[]>(`/api/v1/public/recommendations/shops/${product.shopId}?excludeProductId=${product.id}&size=10`), []),
  ]) : [
    { data: { content: [] } as Page<Review>, failed: false },
    { data: [] as ProductSummary[], failed: false },
    { data: [] as ProductSummary[], failed: false },
  ];
  const reviews = reviewResult.data;
  const related = relatedResult.data;
  const sameShop = sameShopResult.data;
  const images = [...(product.images ?? [])].sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0));
  const structuredData = buildProductStructuredData({ ...product, slug: product.slug ?? slug });
  return (
    <main className="shell page-section">
      <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: serializeJsonLd(structuredData) }} />
      {product.id && <ProductViewRecorder productId={product.id} />}
      <nav className="breadcrumbs"><Link href="/">Trang chủ</Link><span>/</span><Link href="/search">Sản phẩm</Link><span>/</span><span>{product.name}</span></nav>
      <section className="product-detail">
        <div className="product-gallery">
          <div className="product-gallery__main"><SafeImage src={images[0]?.url ?? ""} alt={images[0]?.altText ?? product.name ?? "Sản phẩm"} fill priority sizes="(max-width: 767px) 100vw, 50vw" /></div>
          {images.length > 1 && <div className="product-gallery__rail">{images.slice(1, 5).map((image) => <div key={image.id}><SafeImage src={image.url ?? ""} alt={image.altText ?? "Ảnh sản phẩm"} fill sizes="100px" /></div>)}</div>}
        </div>
        <div className="product-summary">
          <span className="eyebrow">{product.categoryName ?? "Sản phẩm"}</span>
          <h1>{product.name}</h1>
          <div className="product-rating"><Star /> <strong>{product.ratingAverage?.toFixed(1) ?? "Chưa có"}</strong><span>{product.reviewCount ?? 0} đánh giá</span><span>Đã bán {product.soldCount ?? 0}</span></div>
          {product.shortDescription && <p className="product-lead">{product.shortDescription}</p>}
          <ProductActions product={product} />
          {product.id && product.shopId && product.slug && <ProductChatButton productId={product.id} shopId={product.shopId} slug={product.slug} />}
          <div className="commerce-notes"><span><ShieldCheck /> Giá và tồn kho được máy chủ kiểm tra lại khi thanh toán.</span></div>
        </div>
      </section>
      <section className="product-info-grid">
        <article className="surface product-description"><h2>Mô tả sản phẩm</h2><p>{product.description || "Nhà bán chưa bổ sung mô tả chi tiết."}</p>{product.attributes?.length ? <dl>{product.attributes.map((attribute) => <div key={attribute.attributeId}><dt>{attribute.name}</dt><dd>{attribute.value}</dd></div>)}</dl> : null}</article>
        <aside className="surface shop-brief"><Store /><div><span className="muted">Được bán bởi</span><h2>{product.shopName}</h2><Link className="sp-button sp-button--secondary" href={`/shop/${product.shopSlug}`}>Xem shop</Link></div></aside>
      </section>
      <section className="reviews-section">
        <div className="section-heading"><div><span className="eyebrow">Người mua đã xác minh</span><h2>Đánh giá sản phẩm</h2></div></div>
        {reviewResult.failed ? <p className="notice notice--error" role="status">Chưa thể tải đánh giá lúc này. Thông tin và thao tác mua sản phẩm vẫn hoạt động bình thường.</p> : reviews.content?.length ? <div className="review-list">{reviews.content.map((review) => <article key={review.id}><div className="review-list__head"><strong>{review.reviewerName ?? "Người mua shoppew"}</strong><span>{"★".repeat(review.rating ?? 0)}{"☆".repeat(5 - (review.rating ?? 0))}</span><time>{formatDateTime(review.createdAt)}</time></div><p>{review.content || "Người mua không để lại nhận xét."}</p>{review.sellerReply && <div className="seller-reply"><strong>Phản hồi từ nhà bán</strong><p>{review.sellerReply}</p></div>}</article>)}</div> : <EmptyState title="Chưa có đánh giá" description="Đánh giá từ đơn đã mua sẽ xuất hiện tại đây." />}
      </section>
      {(relatedResult.failed || sameShopResult.failed) && <p className="notice notice--error" role="status">Một số gợi ý sản phẩm đang tạm gián đoạn; bạn vẫn có thể xem sản phẩm và gian hàng này.</p>}
      {related.length > 0 && <section className="related-section"><div className="section-heading"><div><span className="eyebrow">Cùng nhu cầu</span><h2>Sản phẩm liên quan</h2></div></div><div className="product-grid">{related.map((item) => <ProductCard key={item.id} product={item} />)}</div></section>}
      {sameShop.length > 0 && <section className="related-section"><div className="section-heading"><div><span className="eyebrow">Từ cùng nhà bán</span><h2>Khám phá thêm tại {product.shopName}</h2></div><Link href={`/shop/${product.shopSlug}`}>Xem gian hàng</Link></div><div className="product-grid">{sameShop.map((item) => <ProductCard key={item.id} product={item} />)}</div></section>}
    </main>
  );
}
