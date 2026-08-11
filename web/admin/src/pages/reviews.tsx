import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Button, Field } from "@shoppew/ui";
import { Eye, EyeOff, Search, Trash2 } from "lucide-react";
import { useState } from "react";
import { EmptyPanel, ErrorPanel, LoadingPanel, Notice, PageHeader, SelectField, StatusBadge } from "@/components/common";
import { apiErrorMessage, formatBusinessTime } from "@/lib/format";
import { isUuid } from "@/lib/forms";
import type { Review, ReviewPage } from "@/lib/types";
import { useAuth } from "@/providers";

type ReviewAction = "publish" | "hide" | "remove";

export function ReviewsPage() {
  const { request } = useAuth(); const queryClient = useQueryClient();
  const [draftProductId, setDraftProductId] = useState(""); const [productId, setProductId] = useState(""); const [manualReviewId, setManualReviewId] = useState(""); const [manualAction, setManualAction] = useState<ReviewAction>("publish"); const [notice, setNotice] = useState("");
  const query = useQuery({ queryKey: ["admin-product-reviews", productId], queryFn: () => request<ReviewPage>(`/api/v1/public/products/${productId}/reviews?page=0&size=50`), enabled: Boolean(productId) });
  const mutation = useMutation({ mutationFn: ({ review, action }: { review: Review; action: ReviewAction }) => request<Review>(`/api/v1/admin/reviews/${review.id}/${action}`, { method: "POST" }), onSuccess: (_, variables) => { setNotice(variables.action === "publish" ? "Đánh giá đã được hiển thị." : variables.action === "hide" ? "Đánh giá đã được ẩn." : "Đánh giá đã được gỡ."); void queryClient.invalidateQueries({ queryKey: ["admin-product-reviews", productId] }); } });
  const reviews = query.data?.content ?? [];
  const invalidId = draftProductId && !isUuid(draftProductId);
  return (
    <>
      <PageHeader eyebrow="Content integrity" title="Kiểm duyệt đánh giá" description="Tải đánh giá theo ID sản phẩm từ API công khai, sau đó áp dụng quyết định kiểm duyệt có ghi audit." />
      <form className="filter-bar review-search" onSubmit={(event) => { event.preventDefault(); if (isUuid(draftProductId)) { setProductId(draftProductId.trim()); setNotice(""); } }}><Field label="ID sản phẩm" required value={draftProductId} onChange={(event) => setDraftProductId(event.target.value)} error={invalidId ? "ID sản phẩm phải là UUID hợp lệ." : undefined} placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" /><Button disabled={!isUuid(draftProductId) || query.isFetching}><Search aria-hidden="true" /> {query.isFetching ? "Đang tải…" : "Tải đánh giá"}</Button></form>
      <section className="panel direct-review-action"><div><h2>Xử lý trực tiếp theo ID đánh giá</h2><p>Dùng khi đánh giá đã ẩn/gỡ không còn xuất hiện trong danh sách công khai.</p></div><Field label="ID đánh giá" value={manualReviewId} onChange={(event) => setManualReviewId(event.target.value)} error={manualReviewId && !isUuid(manualReviewId) ? "ID đánh giá phải là UUID hợp lệ." : undefined} /><SelectField label="Quyết định" value={manualAction} onChange={(event) => setManualAction(event.target.value as ReviewAction)}><option value="publish">Hiển thị</option><option value="hide">Ẩn</option><option value="remove">Gỡ</option></SelectField><Button type="button" disabled={!isUuid(manualReviewId) || mutation.isPending} onClick={() => { if (manualAction !== "remove" || window.confirm("Gỡ đánh giá này? Hành động sẽ được ghi nhật ký.")) mutation.mutate({ review: { id: manualReviewId.trim() }, action: manualAction }); }}>{mutation.isPending ? "Đang xử lý…" : "Áp dụng"}</Button></section>
      {notice ? <Notice>{notice}</Notice> : null}
      {!productId ? <EmptyPanel title="Nhập ID sản phẩm" description="Chọn một sản phẩm cần kiểm tra rồi tải danh sách đánh giá thật từ backend." /> : query.isPending ? <LoadingPanel rows={6} /> : query.isError ? <ErrorPanel error={query.error} onRetry={() => void query.refetch()} /> : reviews.length === 0 ? <EmptyPanel title="Sản phẩm chưa có đánh giá" description="Không có nội dung cần kiểm duyệt cho sản phẩm này." /> : <section className="panel review-list">{reviews.map((review) => <article key={review.id}><header><div><strong>{review.reviewerName || "Người mua shoppew"}</strong><span aria-label={`${review.rating ?? 0} trên 5 sao`}>{"★".repeat(review.rating ?? 0)}{"☆".repeat(5 - (review.rating ?? 0))}</span></div><StatusBadge value={review.status} /></header><p>{review.content || "Đánh giá không có nội dung chữ."}</p>{review.images?.length ? <div className="review-images">{review.images.map((image) => <a href={image.url} target="_blank" rel="noreferrer" key={image.id}>Xem ảnh</a>)}</div> : null}<footer><small>{formatBusinessTime(review.createdAt)}</small><div className="row-actions">{review.status !== "PUBLISHED" ? <Button className="button-quiet action-approve" type="button" disabled={mutation.isPending} onClick={() => mutation.mutate({ review, action: "publish" })}><Eye aria-hidden="true" /> Hiện</Button> : null}{review.status !== "HIDDEN" && review.status !== "REMOVED" ? <Button className="button-quiet" type="button" disabled={mutation.isPending} onClick={() => mutation.mutate({ review, action: "hide" })}><EyeOff aria-hidden="true" /> Ẩn</Button> : null}{review.status !== "REMOVED" ? <Button className="button-quiet action-reject" type="button" disabled={mutation.isPending} onClick={() => { if (window.confirm("Gỡ đánh giá này? Hành động sẽ được ghi nhật ký.")) mutation.mutate({ review, action: "remove" }); }}><Trash2 aria-hidden="true" /> Gỡ</Button> : null}</div></footer></article>)}{mutation.isError ? <Notice tone="error">{apiErrorMessage(mutation.error)}</Notice> : null}</section>}
    </>
  );
}
