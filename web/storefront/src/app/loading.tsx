import { Skeleton } from "@shoppew/ui";

export default function Loading() {
  return <main className="shell page-section"><Skeleton className="skeleton-heading" /><div className="product-grid">{Array.from({ length: 10 }).map((_, index) => <Skeleton className="skeleton-card" key={index} />)}</div></main>;
}
