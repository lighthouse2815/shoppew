import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { CalendarDays, Star } from "lucide-react";
import { cache } from "react";
import { publicApi, queryString } from "@/lib/api";
import { formatDateTime } from "@/lib/format";
import type { Page, ProductSummary, Shop } from "@/lib/types";
import { ProductCard } from "@/components/product-card";
import { EmptyState } from "@shoppew/ui";
import { SafeImage } from "@/components/safe-image";
import { conciseDescription, noIndexMetadata } from "../../seo";

export const dynamic = "force-dynamic";

const loadShop = cache((slug: string) => publicApi.request<Shop>(`/api/v1/public/shops/${encodeURIComponent(slug)}`));

export async function generateMetadata({ params }: { params: Promise<{ slug: string }> }): Promise<Metadata> {
  const { slug } = await params;
  let shop: Shop;
  try {
    shop = await loadShop(slug);
  } catch {
    return noIndexMetadata("Gian hàng không tồn tại", "Không tìm thấy gian hàng công khai này trên shoppew.");
  }

  const name = shop.name ?? "Gian hàng";
  const description = conciseDescription(shop.description, `Khám phá sản phẩm đã duyệt từ ${name} trên shoppew.`);
  const canonical = `/shop/${encodeURIComponent(shop.slug ?? slug)}`;
  const images = [
    ...(shop.bannerUrl ? [{ url: shop.bannerUrl, alt: `Ảnh bìa ${name}` }] : []),
    ...(shop.logoUrl ? [{ url: shop.logoUrl, alt: `Logo ${name}` }] : []),
  ];

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
  };
}

export default async function ShopPage({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params;
  const shop = await loadShop(slug).catch(() => notFound());
  const products = shop.id
    ? await publicApi.request<Page<ProductSummary>>(`/api/v1/public/products${queryString({ shopId: shop.id, size: 20 })}`)
    : { content: [] };
  return (
    <main>
      <section className="shop-hero">
        {shop.bannerUrl && <SafeImage src={shop.bannerUrl} alt="" fill priority sizes="100vw" />}
        <div className="shell shop-hero__content">
          <div className="shop-avatar">{shop.logoUrl ? <SafeImage src={shop.logoUrl} alt={`Logo ${shop.name}`} fill sizes="96px" /> : <span>{shop.name?.slice(0, 1)}</span>}</div>
          <div><span className="eyebrow">Gian hàng trên shoppew</span><h1>{shop.name}</h1><p>{shop.description || "Nhà bán chưa bổ sung phần giới thiệu."}</p><div className="shop-stats"><span><Star /> {shop.ratingAverage?.toFixed(1) ?? "Chưa có đánh giá"} ({shop.reviewCount ?? 0})</span><span><CalendarDays /> Tham gia {formatDateTime(shop.createdAt)}</span></div></div>
        </div>
      </section>
      <section className="shell page-section">
        <div className="section-heading"><div><span className="eyebrow">Catalog của shop</span><h2>Sản phẩm đang bán</h2><p>{products.totalElements ?? 0} sản phẩm đã được duyệt</p></div></div>
        {products.content?.length ? <div className="product-grid">{products.content.map((product) => <ProductCard key={product.id} product={product} />)}</div> : <EmptyState title="Shop chưa có sản phẩm" description="Sản phẩm đã duyệt của shop sẽ xuất hiện tại đây." />}
      </section>
    </main>
  );
}
