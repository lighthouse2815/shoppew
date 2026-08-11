import Link from "next/link";
import type { Metadata } from "next";
import { ArrowRight, BadgeCheck, RefreshCcw, Store } from "lucide-react";
import { EmptyState } from "@shoppew/ui";
import { ProductCard } from "@/components/product-card";
import { publicApi } from "@/lib/api";
import type { Category, Page, ProductSummary } from "@/lib/types";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Marketplace đa nhà bán Việt Nam",
  description: "Khám phá sản phẩm đã duyệt từ nhiều nhà bán, xem giá và phân loại rõ ràng, rồi theo dõi đơn mua trên shoppew.",
  alternates: { canonical: "/" },
  openGraph: {
    title: "shoppew — Marketplace đa nhà bán Việt Nam",
    description: "Khám phá sản phẩm đã duyệt từ nhiều nhà bán và theo dõi hành trình đơn mua trên shoppew.",
    url: "/",
    type: "website",
  },
  twitter: {
    card: "summary",
    title: "shoppew — Marketplace đa nhà bán Việt Nam",
    description: "Khám phá sản phẩm đã duyệt từ nhiều nhà bán trên shoppew.",
  },
};

async function loadSection<T>(request: Promise<T>, fallback: T) {
  try {
    return { data: await request, failed: false };
  } catch {
    return { data: fallback, failed: true };
  }
}

export default async function HomePage() {
  const [categoryResult, latestResult, popularResult, trendingResult] = await Promise.all([
    loadSection(publicApi.request<Category[]>("/api/v1/public/categories"), []),
    loadSection(publicApi.request<Page<ProductSummary>>("/api/v1/public/products?sort=NEWEST&size=10"), { content: [] }),
    loadSection(publicApi.request<ProductSummary[]>("/api/v1/public/recommendations/popular?size=10"), []),
    loadSection(publicApi.request<ProductSummary[]>("/api/v1/public/recommendations/trending?size=10"), []),
  ]);
  const categories = categoryResult.data;
  const latest = latestResult.data;
  const popular = popularResult.data;
  const trending = trendingResult.data;
  const topCategories = categories.flatMap((category) => [category, ...(category.children ?? [])]).slice(0, 8);

  return (
    <main>
      <section className="home-hero">
        <div className="shell home-hero__grid">
          <div className="home-hero__copy">
            <span className="eyebrow">Marketplace đa nhà bán</span>
            <h1>Mua đúng thứ bạn cần, từ nhà bán bạn tin.</h1>
            <p>Khám phá sản phẩm theo danh mục, so sánh thông tin minh bạch và theo dõi từng bước đơn hàng trong một tài khoản.</p>
            <div className="hero-actions"><Link className="sp-button" href="/search">Khám phá ngay <ArrowRight /></Link><Link className="sp-button sp-button--secondary" href="/register">Tạo tài khoản</Link></div>
          </div>
          <div className="hero-ledger" aria-label="Cam kết trải nghiệm">
            <div><BadgeCheck /><strong>Thông tin rõ ràng</strong><span>Giá, biến thể và nhà bán hiển thị trước khi đặt hàng.</span></div>
            <div><RefreshCcw /><strong>Luồng hậu mãi</strong><span>Yêu cầu hoàn tiền và tranh chấp được lưu vết.</span></div>
            <div><Store /><strong>Nhiều nhà bán</strong><span>Một giỏ hàng, tách đơn đúng theo từng shop.</span></div>
          </div>
        </div>
      </section>

      <section className="shell page-section">
        <div className="section-heading"><div><span className="eyebrow">Đi nhanh theo nhu cầu</span><h2>Danh mục nổi bật</h2></div><Link href="/search">Xem toàn bộ <ArrowRight /></Link></div>
        {categoryResult.failed ? <p className="notice notice--error" role="status">Chưa thể tải danh mục. Bạn vẫn có thể tìm sản phẩm bằng ô tìm kiếm phía trên.</p> : topCategories.length ? <div className="category-rail">{topCategories.map((category) => <Link key={category.id} href={`/category/${category.slug}`}><span>{category.name}</span><ArrowRight /></Link>)}</div> : <EmptyState title="Chưa có danh mục" description="Danh mục sẽ xuất hiện khi quản trị viên công bố dữ liệu catalog." />}
      </section>

      <section className="home-products">
        <div className="shell page-section">
          <div className="section-heading"><div><span className="eyebrow">Catalog đang hoạt động</span><h2>Sản phẩm mới lên kệ</h2><p>Dữ liệu được lấy trực tiếp từ các sản phẩm đã duyệt.</p></div><Link href="/search">Xem thêm <ArrowRight /></Link></div>
          {latestResult.failed ? <p className="notice notice--error" role="status">Catalog tạm thời chưa phản hồi. Hãy thử lại sau ít phút.</p> : latest.content?.length ? <div className="product-grid">{latest.content.map((product) => <ProductCard key={product.id} product={product} />)}</div> : <EmptyState title="Chưa có sản phẩm" description="Sản phẩm đã được duyệt sẽ xuất hiện tại đây." />}
        </div>
      </section>

      <section className="shell page-section recommendation-section">
        <div className="section-heading"><div><span className="eyebrow">Được khách hàng lựa chọn</span><h2>Bán chạy trên shoppew</h2><p>Xếp hạng từ số lượng bán và đánh giá thực tế, không dùng dữ liệu trình diễn.</p></div><Link href="/search?sort=BEST_SELLING">Xem bảng đầy đủ <ArrowRight /></Link></div>
        {popularResult.failed ? <p className="notice notice--error" role="status">Gợi ý bán chạy đang tạm gián đoạn; các phần catalog khác vẫn dùng được.</p> : popular.length ? <div className="product-grid">{popular.map((product) => <ProductCard key={product.id} product={product} />)}</div> : <EmptyState title="Chưa đủ dữ liệu bán chạy" description="Danh sách sẽ cập nhật khi có đơn hàng thực tế." />}
      </section>

      <section className="trending-band">
        <div className="shell page-section">
          <div className="section-heading"><div><span className="eyebrow">Xu hướng 7 ngày</span><h2>Đang được quan tâm</h2><p>Ưu tiên sản phẩm phát sinh mua gần đây, sau đó dùng tín hiệu catalog khi chưa đủ dữ liệu.</p></div></div>
          {trendingResult.failed ? <p className="notice notice--error" role="status">Chưa thể tải xu hướng lúc này. Bạn có thể tiếp tục duyệt catalog bình thường.</p> : trending.length ? <div className="product-grid">{trending.map((product) => <ProductCard key={product.id} product={product} />)}</div> : <EmptyState title="Chưa có xu hướng" description="Hoạt động mua gần đây sẽ tạo danh sách này." />}
        </div>
      </section>
    </main>
  );
}
