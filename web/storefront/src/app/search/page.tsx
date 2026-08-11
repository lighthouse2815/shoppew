import type { Metadata } from "next";
import { EmptyState } from "@shoppew/ui";
import { Pagination } from "@/components/pagination";
import { ProductCard } from "@/components/product-card";
import { publicApi, queryString } from "@/lib/api";
import type { Brand, Category, Page, ProductSummary } from "@/lib/types";

export const dynamic = "force-dynamic";

async function loadOptional<T>(request: Promise<T>, fallback: T) {
  try {
    return { data: await request, failed: false };
  } catch {
    return { data: fallback, failed: true };
  }
}

type Params = {
  q?: string;
  categoryId?: string;
  brandId?: string;
  shopId?: string;
  minPrice?: string;
  maxPrice?: string;
  minRating?: string;
  sort?: string;
  page?: string;
};

export async function generateMetadata({ searchParams }: { searchParams: Promise<Params> }): Promise<Metadata> {
  const params = await searchParams;
  const query = params.q?.replace(/\s+/g, " ").trim().slice(0, 80);
  const hasFilters = Object.values(params).some((value) => Boolean(value?.trim()));
  const title = query ? `Tìm kiếm “${query}”` : hasFilters ? "Kết quả lọc sản phẩm" : "Khám phá sản phẩm";
  const description = query
    ? `Xem sản phẩm phù hợp với “${query}” từ các nhà bán trên shoppew.`
    : hasFilters
      ? "Xem kết quả sản phẩm theo bộ lọc giá, danh mục, thương hiệu và đánh giá trên shoppew."
      : "Duyệt catalog sản phẩm đã được công khai từ nhiều nhà bán trên shoppew.";
  const pageUrl = `/search${queryString(params)}`;

  return {
    title,
    description,
    ...(hasFilters ? {} : { alternates: { canonical: "/search" } }),
    robots: {
      index: !hasFilters,
      follow: true,
      googleBot: { index: !hasFilters, follow: true },
    },
    openGraph: {
      title: `${title} · shoppew`,
      description,
      url: pageUrl,
      type: "website",
    },
    twitter: {
      card: "summary",
      title: `${title} · shoppew`,
      description,
    },
  };
}

export default async function SearchPage({ searchParams }: { searchParams: Promise<Params> }) {
  const params = await searchParams;
  const page = Math.max(Number(params.page) || 0, 0);
  const [products, categoryResult, brandResult] = await Promise.all([
    publicApi.request<Page<ProductSummary>>(`/api/v1/public/products${queryString({ ...params, page, size: 20 })}`),
    loadOptional(publicApi.request<Category[]>("/api/v1/public/categories"), []),
    loadOptional(publicApi.request<Brand[]>("/api/v1/public/brands"), []),
  ]);
  const categories = categoryResult.data;
  const brands = brandResult.data;
  const categoryOptions = categories.flatMap((category) => [category, ...(category.children ?? [])]);
  const pageHref = (target: number) => `/search${queryString({ ...params, page: target })}`;

  return (
    <main className="shell page-section">
      <div className="section-heading"><div><span className="eyebrow">Catalog shoppew</span><h1>{params.q ? `Kết quả cho “${params.q}”` : "Tất cả sản phẩm"}</h1><p>{products.totalElements ?? 0} sản phẩm phù hợp</p></div></div>
      <div className="catalog-layout">
        <aside className="catalog-filter">
          <h2>Bộ lọc</h2>
          {(categoryResult.failed || brandResult.failed) && <p className="notice notice--error" role="status">Chưa thể tải đủ danh mục hoặc thương hiệu. Bạn vẫn có thể tìm theo từ khóa, giá, đánh giá và thứ tự.</p>}
          <form action="/search" method="get" className="stack">
            {params.shopId && <input type="hidden" name="shopId" value={params.shopId} />}
            <label>Từ khóa<input className="form-control" name="q" defaultValue={params.q} placeholder="Tên sản phẩm" /></label>
            <label>Danh mục<select className="form-control" name="categoryId" defaultValue={params.categoryId ?? ""}><option value="">Tất cả danh mục</option>{categoryOptions.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}</select></label>
            <label>Thương hiệu<select className="form-control" name="brandId" defaultValue={params.brandId ?? ""}><option value="">Tất cả thương hiệu</option>{brands.map((brand) => <option key={brand.id} value={brand.id}>{brand.name}</option>)}</select></label>
            <div className="price-filter"><label>Giá từ<input className="form-control" type="number" name="minPrice" min="0" step="1000" defaultValue={params.minPrice} inputMode="numeric" /></label><label>Đến<input className="form-control" type="number" name="maxPrice" min="0" step="1000" defaultValue={params.maxPrice} inputMode="numeric" /></label></div>
            <label>Đánh giá tối thiểu<select className="form-control" name="minRating" defaultValue={params.minRating ?? ""}><option value="">Mọi mức</option><option value="4">4 sao trở lên</option><option value="3">3 sao trở lên</option><option value="2">2 sao trở lên</option></select></label>
            <label>Sắp xếp<select className="form-control" name="sort" defaultValue={params.sort ?? "RELEVANCE"}><option value="RELEVANCE">Liên quan</option><option value="NEWEST">Mới nhất</option><option value="PRICE_ASC">Giá tăng dần</option><option value="PRICE_DESC">Giá giảm dần</option><option value="BEST_SELLING">Bán chạy</option><option value="RATING">Đánh giá cao</option></select></label>
            <button className="sp-button" type="submit">Áp dụng bộ lọc</button>
            <a className="text-link" href="/search">Xóa bộ lọc</a>
          </form>
        </aside>
        <section className="catalog-results">
          {products.content?.length ? <><div className="product-grid product-grid--catalog">{products.content.map((product) => <ProductCard key={product.id} product={product} />)}</div><Pagination page={page} totalPages={products.totalPages ?? 0} href={pageHref} /></> : <EmptyState title="Không có sản phẩm phù hợp" description="Hãy thử từ khóa ngắn hơn hoặc bỏ bớt bộ lọc." />}
        </section>
      </div>
    </main>
  );
}
