import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Button } from "@shoppew/ui";
import { Check, EyeOff, ShieldAlert, XCircle } from "lucide-react";
import { useState } from "react";
import { Dialog, EmptyPanel, ErrorPanel, LoadingPanel, Notice, PageHeader, Pagination, StatusBadge, TextAreaField } from "@/components/common";
import { apiErrorMessage, formatMoney, shortId } from "@/lib/format";
import type { ProductDetail, ProductPage, ProductSummary } from "@/lib/types";
import { useAuth } from "@/providers";

type ProductAction = "approve" | "reject" | "hide";

export function ProductsPage() {
  const { request } = useAuth();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [selection, setSelection] = useState<{ product: ProductSummary; action: ProductAction } | null>(null);
  const [reason, setReason] = useState("");
  const [notice, setNotice] = useState("");
  const query = useQuery({
    queryKey: ["admin-products-pending", page],
    queryFn: () => request<ProductPage>(`/api/v1/admin/products/pending?page=${page}&size=24`),
  });
  const mutation = useMutation({
    mutationFn: ({ product, action, reason: note }: { product: ProductSummary; action: ProductAction; reason: string }) =>
      request<ProductDetail>(`/api/v1/admin/products/${product.id}/${action}`, {
        method: "POST",
        ...(action === "approve" ? {} : { body: { reason: note } }),
      }),
    onSuccess: (_, variables) => {
      setNotice(variables.action === "approve" ? "Sản phẩm đã được duyệt." : variables.action === "reject" ? "Sản phẩm đã bị từ chối." : "Sản phẩm đã được ẩn.");
      setSelection(null);
      setReason("");
      void queryClient.invalidateQueries({ queryKey: ["admin-products-pending"] });
      void queryClient.invalidateQueries({ queryKey: ["admin-analytics"] });
    },
  });
  const products = query.data?.content ?? [];

  function openAction(product: ProductSummary, action: ProductAction) {
    setNotice("");
    mutation.reset();
    setReason("");
    setSelection({ product, action });
  }

  return (
    <>
      <PageHeader eyebrow="Trust & catalog" title="Duyệt sản phẩm" description="Xem sản phẩm đã được người bán gửi duyệt, sau đó phê duyệt hoặc ghi rõ lý do từ chối/ẩn." />
      {notice ? <Notice>{notice}</Notice> : null}
      {query.isPending ? <LoadingPanel rows={8} /> : query.isError ? <ErrorPanel error={query.error} onRetry={() => void query.refetch()} /> : products.length === 0 ? (
        <EmptyPanel title="Không có sản phẩm chờ duyệt" description="Hàng đợi hiện đã được xử lý hết. Có thể tải lại để kiểm tra dữ liệu mới." action={<Button type="button" onClick={() => void query.refetch()}>Tải lại</Button>} />
      ) : (
        <section className="panel table-panel">
          <div className="data-table product-table" role="table" aria-label="Sản phẩm chờ duyệt">
            <div className="data-row data-row--head" role="row"><span>Tên sản phẩm</span><span>Gian hàng</span><span>Danh mục</span><span>Giá thấp nhất</span><span>Trạng thái</span><span>Hành động</span></div>
            {products.map((product) => (
              <article className="data-row" role="row" key={product.id}>
                <div data-label="Tên sản phẩm"><strong>{product.name || "Sản phẩm chưa đặt tên"}</strong><small>ID {shortId(product.id)}</small></div>
                <div data-label="Gian hàng"><strong>{product.shopName || "—"}</strong><small>{shortId(product.shopId)}</small></div>
                <span data-label="Danh mục">{product.categoryName || "—"}</span>
                <span data-label="Giá thấp nhất" className="numeric">{formatMoney(product.minimumPrice, product.currency)}</span>
                <span data-label="Trạng thái"><StatusBadge value={product.status} /></span>
                <div className="row-actions" data-label="Hành động">
                  <Button className="button-quiet action-approve" type="button" onClick={() => openAction(product, "approve")}><Check aria-hidden="true" /> Duyệt</Button>
                  <Button className="button-quiet action-reject" type="button" onClick={() => openAction(product, "reject")}><XCircle aria-hidden="true" /> Từ chối</Button>
                  <Button className="button-quiet" type="button" onClick={() => openAction(product, "hide")}><EyeOff aria-hidden="true" /> Ẩn</Button>
                </div>
              </article>
            ))}
          </div>
          <Pagination page={page} totalPages={query.data?.totalPages ?? 1} onChange={setPage} disabled={query.isFetching} />
        </section>
      )}
      <Dialog open={Boolean(selection)} title={selection?.action === "approve" ? "Phê duyệt sản phẩm" : selection?.action === "reject" ? "Từ chối sản phẩm" : "Ẩn sản phẩm"} description={selection?.product.name} onClose={() => { if (!mutation.isPending) setSelection(null); }}>
        <div className="dialog-body">
          {selection?.action === "approve" ? <p className="decision-copy"><ShieldAlert aria-hidden="true" /> Xác nhận sản phẩm đáp ứng yêu cầu hiển thị của marketplace. Hành động này được ghi vào nhật ký kiểm toán.</p> : <TextAreaField label="Lý do" required rows={5} value={reason} onChange={(event) => setReason(event.target.value)} error={!reason.trim() && mutation.isError ? "Cần ghi lý do để người bán hiểu quyết định." : undefined} />}
          {mutation.isError ? <Notice tone="error">{apiErrorMessage(mutation.error)}</Notice> : null}
        </div>
        <footer className="dialog-actions">
          <Button className="button-secondary" type="button" disabled={mutation.isPending} onClick={() => setSelection(null)}>Hủy</Button>
          <Button type="button" disabled={mutation.isPending || !selection || (selection.action !== "approve" && !reason.trim())} onClick={() => selection && mutation.mutate({ ...selection, reason: reason.trim() })}>{mutation.isPending ? "Đang xử lý…" : "Xác nhận"}</Button>
        </footer>
      </Dialog>
    </>
  );
}
