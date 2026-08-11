"use client";

import { useQuery } from "@tanstack/react-query";
import { EmptyState, ErrorState, Spinner } from "@shoppew/ui";
import { useAuth } from "@/components/providers";
import { formatDateTime } from "@/lib/format";
import type { Page, Review } from "@/lib/types";

export default function ReviewsPage() {
  const { request } = useAuth(); const query = useQuery({ queryKey: ["my-reviews"], queryFn: () => request<Page<Review>>("/api/v1/reviews/me?size=50") });
  if (query.isPending) return <Spinner label="Đang tải đánh giá" />; if (query.error) return <ErrorState message={query.error.message} onRetry={() => void query.refetch()} />;
  return <section><div className="section-heading"><div><span className="eyebrow">Nội dung đã chia sẻ</span><h1>Đánh giá của tôi</h1><p>{query.data?.totalElements ?? 0} đánh giá</p></div></div>{query.data?.content?.length ? <div className="review-list">{query.data.content.map((review) => <article className="surface" key={review.id}><div className="review-list__head"><strong>{"★".repeat(review.rating ?? 0)}{"☆".repeat(5 - (review.rating ?? 0))}</strong><span className="status-pill">{review.status}</span><time>{formatDateTime(review.createdAt)}</time></div><p>{review.content || "Không có nội dung nhận xét."}</p>{review.sellerReply && <div className="seller-reply"><strong>Nhà bán phản hồi</strong><p>{review.sellerReply}</p></div>}</article>)}</div> : <EmptyState title="Bạn chưa viết đánh giá" description="Sau khi hoàn tất đơn hàng, bạn có thể đánh giá từng sản phẩm từ chi tiết đơn mua." />}</section>;
}
