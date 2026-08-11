import Link from "next/link";
import { Star } from "lucide-react";
import { Price } from "@shoppew/ui";
import type { ProductSummary } from "@/lib/types";
import { SafeImage } from "./safe-image";

export function ProductCard({ product }: { product: ProductSummary }) {
  return (
    <article className="product-card">
      <Link href={`/product/${product.slug}`} className="product-card__media" aria-label={product.name}>
        <SafeImage src={product.primaryImageUrl ?? ""} alt={product.name ?? "Sản phẩm"} fill sizes="(max-width: 640px) 50vw, (max-width: 1024px) 25vw, 20vw" />
      </Link>
      <div className="product-card__body">
        <Link href={`/product/${product.slug}`} className="product-card__name">{product.name}</Link>
        <Price value={product.minimumPrice ?? 0} currency={product.currency} compareAt={product.originalMinimumPrice} />
        <div className="product-card__meta">
          <span><Star aria-hidden="true" /> {product.ratingAverage?.toFixed(1) ?? "Mới"}</span>
          <span>Đã bán {product.soldCount ?? 0}</span>
        </div>
        <span className="product-card__shop">{product.shopName}</span>
      </div>
    </article>
  );
}
