import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { cache } from "react";
import { EmptyState } from "@shoppew/ui";
import { ProductCard } from "@/components/product-card";
import { Pagination } from "@/components/pagination";
import { publicApi, queryString } from "@/lib/api";
import type { Category, Page, ProductSummary } from "@/lib/types";
import { conciseDescription, noIndexMetadata } from "../../seo";

export const dynamic = "force-dynamic";

const loadCategories = cache(() => publicApi.request<Category[]>("/api/v1/public/categories"));

function flattenCategories(categories: Category[]): Category[] {
  return categories.flatMap((category) => [category, ...flattenCategories(category.children ?? [])]);
}

async function findCategory(slug: string): Promise<Category | undefined> {
  return flattenCategories(await loadCategories()).find((item) => item.slug === slug);
}

export async function generateMetadata({
  params,
  searchParams,
}: {
  params: Promise<{ slug: string }>;
  searchParams: Promise<{ page?: string }>;
}): Promise<Metadata> {
  const [{ slug }, query] = await Promise.all([params, searchParams]);
  let category: Category | undefined;
  try {
    category = await findCategory(slug);
  } catch {
    return noIndexMetadata("Danh mục sản phẩm", "Không thể tải danh mục công khai này từ shoppew.");
  }
  if (!category) return noIndexMetadata("Danh mục không tồn tại", "Không tìm thấy danh mục công khai này trên shoppew.");

  const page = Math.max(Number(query.page) || 0, 0);
  const title = page > 0 ? `${category.name ?? "Danh mục"} — Trang ${page + 1}` : category.name ?? "Danh mục sản phẩm";
  const description = conciseDescription(
    undefined,
    `Khám phá sản phẩm trong danh mục ${category.name ?? "này"} từ các nhà bán trên shoppew.`,
  );
  const canonical = `/category/${encodeURIComponent(category.slug ?? slug)}${page > 0 ? `?page=${page}` : ""}`;
  const images = category.imageUrl ? [{ url: category.imageUrl, alt: category.name ?? "Danh mục shoppew" }] : [];

  return {
    title,
    description,
    alternates: { canonical },
    openGraph: {
      title: `${title} · shoppew`,
      description,
      url: canonical,
      type: "website",
      images,
    },
    twitter: {
      card: images.length ? "summary_large_image" : "summary",
      title: `${title} · shoppew`,
      description,
      images: images.map((image) => image.url),
    },
  };
}

export default async function CategoryPage({ params, searchParams }: { params: Promise<{ slug: string }>; searchParams: Promise<{ page?: string }> }) {
  const [{ slug }, query] = await Promise.all([params, searchParams]);
  const category = await findCategory(slug);
  if (!category?.id) notFound();
  const page = Math.max(Number(query.page) || 0, 0);
  const products = await publicApi.request<Page<ProductSummary>>(`/api/v1/public/products${queryString({ categoryId: category.id, page, size: 20 })}`);
  return (
    <main className="shell page-section">
      <div className="category-title"><span className="eyebrow">Danh mục</span><h1>{category.name}</h1><p>{products.totalElements ?? 0} sản phẩm đang hiển thị</p></div>
      {products.content?.length ? <><div className="product-grid">{products.content.map((product) => <ProductCard key={product.id} product={product} />)}</div><Pagination page={page} totalPages={products.totalPages ?? 0} href={(target) => `/category/${slug}?page=${target}`} /></> : <EmptyState title="Danh mục chưa có sản phẩm" description="Hãy quay lại sau khi nhà bán cập nhật catalog." />}
    </main>
  );
}
