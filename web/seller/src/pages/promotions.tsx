import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Edit3, Plus, Trash2, Zap } from "lucide-react";
import { Button, Field } from "@shoppew/ui";
import { Empty, ErrorBlock, Loading, NeedShop, PageHeader, Status } from "@/components/common";
import { dateTime, money } from "@/lib/format";
import type { Page, ProductSummary, Promotion } from "@/lib/types";
import { useAuth, useShop } from "@/providers";

export function PromotionsPage() {
  const { request } = useAuth();
  const { shop } = useShop();
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<Promotion | "new" | null>(null);
  const shopId = shop?.id;
  const query = useQuery({
    queryKey: ["seller-promotions", shopId],
    queryFn: () => request<Promotion[]>(`/api/v1/seller/shops/${shopId}/promotions`),
    enabled: Boolean(shopId),
  });
  const status = useMutation({
    mutationFn: ({ promotionId, action }: { promotionId: string; action: string }) =>
      request<Promotion>(`/api/v1/seller/shops/${shopId}/promotions/${promotionId}/${action}`, { method: "POST" }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["seller-promotions", shopId] }),
  });

  return (
    <NeedShop>
      <PageHeader
        eyebrow="Campaign studio"
        title="Khuyến mãi"
        description="Giá khuyến mãi được server tính lại và khóa ngân sách khi đặt hàng."
        action={<Button onClick={() => setEditing("new")}><Plus /> Tạo khuyến mãi</Button>}
      />
      {query.isPending ? <Loading /> : query.error ? <ErrorBlock error={query.error} /> : query.data?.length ? (
        <div className="campaign-grid">
          {query.data.map((promotion) => (
            <article className="campaign-card" key={promotion.id}>
              <div className="campaign-code"><Zap /><strong>{promotion.promotionType?.replaceAll("_", " ")}</strong><Status value={promotion.status} /></div>
              <h2>{promotion.name}</h2>
              <p>{promotion.discountType === "PERCENTAGE" ? `${promotion.discountValue ?? 0}%` : money(promotion.discountValue)} · {promotion.targets?.length ?? 0} mục tiêu</p>
              <dl>
                <div><dt>Bắt đầu</dt><dd>{dateTime(promotion.startsAt)}</dd></div>
                <div><dt>Kết thúc</dt><dd>{dateTime(promotion.endsAt)}</dd></div>
              </dl>
              <div className="card-actions">
                <Button className="button-quiet" onClick={() => setEditing(promotion)}><Edit3 /> Sửa</Button>
                {["DRAFT", "PAUSED"].includes(promotion.status ?? "") && <Button className="button-quiet" onClick={() => promotion.id && status.mutate({ promotionId: promotion.id, action: "activate" })}>Kích hoạt</Button>}
                {promotion.status === "ACTIVE" && <Button className="button-quiet" onClick={() => promotion.id && status.mutate({ promotionId: promotion.id, action: "pause" })}>Tạm dừng</Button>}
                {promotion.status !== "ARCHIVED" && <Button className="button-quiet" onClick={() => promotion.id && status.mutate({ promotionId: promotion.id, action: "archive" })}>Lưu trữ</Button>}
              </div>
            </article>
          ))}
        </div>
      ) : <Empty title="Chưa có khuyến mãi" description="Tạo chương trình giảm giá có mục tiêu sản phẩm cụ thể." action={<Button onClick={() => setEditing("new")}><Plus /> Tạo khuyến mãi</Button>} />}
      {status.error && <p className="form-error">{status.error.message}</p>}
      {editing && <PromotionDrawer promotion={editing === "new" ? undefined : editing} shopId={shopId!} close={() => setEditing(null)} />}
    </NeedShop>
  );
}

type PromotionTargetForm = { key: string; productId: string; variantId: string; promotionalPrice: string; quantityLimit: string };
type PromotionForm = {
  name: string;
  promotionType: "PRODUCT_DISCOUNT" | "SHOP_DISCOUNT" | "FLASH_SALE";
  discountType: "FIXED" | "PERCENTAGE";
  discountValue: string;
  maxDiscount: string;
  startsAt: string;
  endsAt: string;
  targets: PromotionTargetForm[];
};

let targetSequence = 0;
function blankTarget(): PromotionTargetForm {
  targetSequence += 1;
  return { key: `new-${targetSequence}`, productId: "", variantId: "", promotionalPrice: "", quantityLimit: "" };
}

function localInput(value?: string, hours = 1) {
  const date = value ? new Date(value) : new Date(Date.now() + hours * 3_600_000);
  const offset = date.getTimezoneOffset() * 60_000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 16);
}

function initial(promotion?: Promotion): PromotionForm {
  const targets = promotion?.targets?.map((target, index) => ({
    key: target.id ?? `existing-${index}`,
    productId: target.productId ?? "",
    variantId: target.variantId ?? "",
    promotionalPrice: target.promotionalPrice?.toString() ?? "",
    quantityLimit: target.quantityLimit?.toString() ?? "",
  })) ?? [];
  return {
    name: promotion?.name ?? "",
    promotionType: (promotion?.promotionType === "PLATFORM_CAMPAIGN" ? "PRODUCT_DISCOUNT" : promotion?.promotionType) ?? "PRODUCT_DISCOUNT",
    discountType: promotion?.discountType ?? "PERCENTAGE",
    discountValue: promotion?.discountValue?.toString() ?? "",
    maxDiscount: promotion?.maxDiscount?.toString() ?? "",
    startsAt: localInput(promotion?.startsAt, 1),
    endsAt: localInput(promotion?.endsAt, 24 * 14),
    targets: targets.length ? targets : [blankTarget()],
  };
}

const optionalNumber = (value: string) => value.trim() ? Number(value) : undefined;

function PromotionDrawer({ promotion, shopId, close }: { promotion?: Promotion; shopId: string; close: () => void }) {
  const { request } = useAuth();
  const queryClient = useQueryClient();
  const [form, setForm] = useState(() => initial(promotion));
  const products = useQuery({
    queryKey: ["seller-products-for-campaign", shopId],
    queryFn: () => request<Page<ProductSummary>>(`/api/v1/seller/shops/${shopId}/products?size=100`),
  });
  const save = useMutation({
    mutationFn: () => request<Promotion>(
      promotion?.id ? `/api/v1/seller/shops/${shopId}/promotions/${promotion.id}` : `/api/v1/seller/shops/${shopId}/promotions`,
      {
        method: promotion?.id ? "PUT" : "POST",
        body: {
          name: form.name.trim(),
          promotionType: form.promotionType,
          discountType: form.discountType,
          discountValue: Number(form.discountValue),
          maxDiscount: optionalNumber(form.maxDiscount),
          startsAt: new Date(form.startsAt).toISOString(),
          endsAt: new Date(form.endsAt).toISOString(),
          targets: form.targets.map((target) => ({
            productId: target.productId,
            variantId: target.variantId.trim() || undefined,
            promotionalPrice: optionalNumber(target.promotionalPrice),
            quantityLimit: optionalNumber(target.quantityLimit),
          })),
        },
      },
    ),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["seller-promotions", shopId] });
      close();
    },
  });
  const invalid = !form.name.trim()
    || !form.discountValue
    || Number(form.discountValue) <= 0
    || (form.discountType === "PERCENTAGE" && Number(form.discountValue) > 100)
    || !form.startsAt
    || !form.endsAt
    || new Date(form.endsAt) <= new Date(form.startsAt)
    || form.targets.some((target) => !target.productId);

  function updateTarget(key: string, patch: Partial<PromotionTargetForm>) {
    setForm({ ...form, targets: form.targets.map((target) => target.key === key ? { ...target, ...patch } : target) });
  }

  return (
    <div className="drawer-backdrop" onMouseDown={close}>
      <aside className="drawer" role="dialog" aria-modal="true" aria-labelledby="promotion-title" onMouseDown={(event) => event.stopPropagation()}>
        <div className="drawer-head">
          <div><span>{promotion ? "Chỉnh sửa" : "Tạo mới"}</span><h2 id="promotion-title">Khuyến mãi</h2><p>Một chiến dịch có thể áp dụng cho nhiều sản phẩm hoặc biến thể thuộc shop.</p></div>
          <button aria-label="Đóng" onClick={close}>×</button>
        </div>
        <form className="drawer-form" onSubmit={(event) => { event.preventDefault(); save.mutate(); }}>
          <div className="form-grid">
            <Field label="Tên chiến dịch" required value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} />
            <label className="select-field"><span>Loại</span><select value={form.promotionType} onChange={(event) => setForm({ ...form, promotionType: event.target.value as PromotionForm["promotionType"] })}><option value="PRODUCT_DISCOUNT">Giảm giá sản phẩm</option><option value="SHOP_DISCOUNT">Giảm toàn shop</option><option value="FLASH_SALE">Flash sale</option></select></label>
            <label className="select-field"><span>Kiểu giảm</span><select value={form.discountType} onChange={(event) => setForm({ ...form, discountType: event.target.value as PromotionForm["discountType"] })}><option value="PERCENTAGE">Phần trăm</option><option value="FIXED">Số tiền</option></select></label>
            <Field label="Mức giảm" required type="number" min="0.01" max={form.discountType === "PERCENTAGE" ? "100" : undefined} step="0.01" value={form.discountValue} onChange={(event) => setForm({ ...form, discountValue: event.target.value })} />
            <Field label="Giảm tối đa" type="number" min="0.01" step="0.01" value={form.maxDiscount} onChange={(event) => setForm({ ...form, maxDiscount: event.target.value })} />
            <Field label="Bắt đầu" required type="datetime-local" value={form.startsAt} onChange={(event) => setForm({ ...form, startsAt: event.target.value })} />
            <Field label="Kết thúc" required type="datetime-local" value={form.endsAt} onChange={(event) => setForm({ ...form, endsAt: event.target.value })} />
          </div>
          <section className="promotion-targets">
            <div className="section-heading"><div><h3>Mục tiêu áp dụng</h3><p>Tối đa 500 mục tiêu; server xác minh mọi sản phẩm và biến thể thuộc đúng shop.</p></div><Button type="button" className="button-secondary" disabled={form.targets.length >= 500} onClick={() => setForm({ ...form, targets: [...form.targets, blankTarget()] })}><Plus /> Thêm mục tiêu</Button></div>
            {form.targets.map((target, index) => (
              <article key={target.key}>
                <label className="select-field"><span>Sản phẩm mục tiêu {index + 1} *</span><select required value={target.productId} onChange={(event) => updateTarget(target.key, { productId: event.target.value, variantId: "" })}><option value="">Chọn sản phẩm</option>{products.data?.content?.map((product) => <option key={product.id} value={product.id}>{product.name}</option>)}</select></label>
                <Field label="ID biến thể (không bắt buộc)" value={target.variantId} onChange={(event) => updateTarget(target.key, { variantId: event.target.value })} />
                <Field label="Giá khuyến mãi" type="number" min="0" step="0.01" value={target.promotionalPrice} onChange={(event) => updateTarget(target.key, { promotionalPrice: event.target.value })} />
                <Field label="Giới hạn số lượng" type="number" min="1" value={target.quantityLimit} onChange={(event) => updateTarget(target.key, { quantityLimit: event.target.value })} />
                <button type="button" aria-label={`Xóa mục tiêu ${index + 1}`} disabled={form.targets.length === 1} onClick={() => setForm({ ...form, targets: form.targets.filter((item) => item.key !== target.key) })}><Trash2 /></button>
              </article>
            ))}
          </section>
          {products.error && <p className="form-error">{products.error.message}</p>}
          {save.error && <p className="form-error">{save.error.message}</p>}
          <Button disabled={save.isPending || products.isPending || invalid}>{save.isPending ? "Đang lưu..." : "Lưu khuyến mãi"}</Button>
        </form>
      </aside>
    </div>
  );
}
