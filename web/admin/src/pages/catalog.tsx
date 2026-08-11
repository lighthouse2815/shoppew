import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Button, Field } from "@shoppew/ui";
import { Edit3, Plus, Power } from "lucide-react";
import { useState } from "react";
import { EmptyPanel, ErrorPanel, LoadingPanel, Notice, PageHeader, SectionHeader, SelectField, StatusBadge, TabSet, TextAreaField } from "@/components/common";
import { apiErrorMessage, shortId } from "@/lib/format";
import { toOptionalNumber } from "@/lib/forms";
import type { AttributeDefinition, AttributeDefinitionRequest, Brand, BrandRequest, Category, CategoryRequest } from "@/lib/types";
import { useAuth } from "@/providers";

export type CatalogTab = "categories" | "brands" | "attributes";

const catalogTabs = [
  { value: "categories", label: "Danh mục" },
  { value: "brands", label: "Thương hiệu" },
  { value: "attributes", label: "Thuộc tính" },
] as const;

export function CatalogPage({ initialTab = "categories" }: { initialTab?: CatalogTab }) {
  const [tab, setTab] = useState<CatalogTab>(initialTab);
  return (
    <>
      <PageHeader eyebrow="Catalog governance" title="Danh mục, thương hiệu & thuộc tính" description="Quản trị cấu trúc catalog dùng chung cho storefront, Seller Center và Android từ cùng API backend." />
      <TabSet
        activeTab={tab}
        ariaLabel="Nhóm dữ liệu catalog"
        idPrefix="catalog"
        onChange={setTab}
        renderPanel={(activeTab) => activeTab === "categories" ? <Categories /> : activeTab === "brands" ? <Brands /> : <Attributes />}
        tabs={catalogTabs}
      />
    </>
  );
}

const emptyCategoryForm = { name: "", slug: "", parentId: "", description: "", imageUrl: "", sortOrder: "" };

function Categories() {
  const { request } = useAuth();
  const queryClient = useQueryClient();
  const [form, setForm] = useState(emptyCategoryForm);
  const [editing, setEditing] = useState<Category | null>(null);
  const [notice, setNotice] = useState("");
  const query = useQuery({ queryKey: ["admin-categories"], queryFn: () => request<Category[]>("/api/v1/admin/categories") });
  const save = useMutation({
    mutationFn: () => {
      const body: CategoryRequest = {
        name: form.name.trim(),
        slug: form.slug.trim() || undefined,
        parentId: form.parentId || undefined,
        description: form.description.trim() || undefined,
        imageUrl: form.imageUrl.trim() || undefined,
        sortOrder: toOptionalNumber(form.sortOrder),
      };
      return request<Category>(editing?.id ? `/api/v1/admin/categories/${editing.id}` : "/api/v1/admin/categories", { method: editing ? "PUT" : "POST", body });
    },
    onSuccess: () => {
      setNotice(editing ? "Đã cập nhật danh mục." : "Đã tạo danh mục.");
      setEditing(null); setForm(emptyCategoryForm);
      void queryClient.invalidateQueries({ queryKey: ["admin-categories"] });
    },
  });
  const status = useMutation({
    mutationFn: ({ item, next }: { item: Category; next: string }) => request<Category>(`/api/v1/admin/categories/${item.id}/status`, { method: "PATCH", body: { status: next } }),
    onSuccess: () => { setNotice("Đã cập nhật trạng thái danh mục."); void queryClient.invalidateQueries({ queryKey: ["admin-categories"] }); },
  });

  function edit(item: Category) {
    setEditing(item); setNotice(""); save.reset();
    setForm({ name: item.name ?? "", slug: item.slug ?? "", parentId: item.parentId ?? "", description: item.description ?? "", imageUrl: item.imageUrl ?? "", sortOrder: item.sortOrder?.toString() ?? "" });
    document.getElementById("category-form")?.scrollIntoView({ behavior: "smooth", block: "start" });
  }

  return (
    <div className="split-workspace">
      <section className="panel editor-panel" id="category-form">
        <SectionHeader title={editing ? "Sửa danh mục" : "Tạo danh mục"} description="Tên là bắt buộc; slug có thể để backend chuẩn hóa." />
        <form onSubmit={(event) => { event.preventDefault(); setNotice(""); save.mutate(); }}>
          <div className="form-grid">
            <Field label="Tên danh mục" required value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} />
            <Field label="Slug" value={form.slug} onChange={(event) => setForm({ ...form, slug: event.target.value })} />
            <SelectField label="Danh mục cha" value={form.parentId} onChange={(event) => setForm({ ...form, parentId: event.target.value })}>
              <option value="">Không có</option>{(query.data ?? []).filter((item) => item.id !== editing?.id).map((item) => <option value={item.id} key={item.id}>{item.name}</option>)}
            </SelectField>
            <Field label="Thứ tự" type="number" min="0" value={form.sortOrder} onChange={(event) => setForm({ ...form, sortOrder: event.target.value })} />
            <Field className="full-span" label="URL ảnh" type="url" value={form.imageUrl} onChange={(event) => setForm({ ...form, imageUrl: event.target.value })} />
            <TextAreaField className="full-span" label="Mô tả" rows={4} value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} />
          </div>
          {save.isError ? <Notice tone="error">{apiErrorMessage(save.error)}</Notice> : null}
          <div className="form-actions"><Button disabled={save.isPending || !form.name.trim()}>{save.isPending ? "Đang lưu…" : editing ? "Lưu thay đổi" : "Tạo danh mục"}</Button>{editing ? <Button className="button-secondary" type="button" disabled={save.isPending} onClick={() => { setEditing(null); setForm(emptyCategoryForm); }}>Hủy sửa</Button> : null}</div>
        </form>
      </section>
      <section>
        {notice ? <Notice>{notice}</Notice> : null}
        {query.isPending ? <LoadingPanel rows={7} /> : query.isError ? <ErrorPanel error={query.error} onRetry={() => void query.refetch()} /> : !query.data?.length ? <EmptyPanel title="Chưa có danh mục" description="Tạo danh mục đầu tiên bằng biểu mẫu bên cạnh." /> : (
          <div className="panel compact-list">
            <SectionHeader title="Danh mục hiện có" description={`${query.data.length} mục`} />
            {query.data.map((item) => <article key={item.id}><div><strong>{item.name}</strong><small>{item.slug || shortId(item.id)} · thứ tự {item.sortOrder ?? 0}</small></div><StatusBadge value={item.status} /><div className="row-actions"><Button className="button-quiet" type="button" onClick={() => edit(item)}><Edit3 aria-hidden="true" /> Sửa</Button><Button className="button-quiet" type="button" disabled={status.isPending} onClick={() => { const next = item.status === "ACTIVE" ? "INACTIVE" : "ACTIVE"; if (window.confirm(`Chuyển “${item.name}” sang ${next}?`)) status.mutate({ item, next }); }}><Power aria-hidden="true" /> {item.status === "ACTIVE" ? "Ngừng" : "Bật"}</Button></div></article>)}
            {status.isError ? <Notice tone="error">{apiErrorMessage(status.error)}</Notice> : null}
          </div>
        )}
      </section>
    </div>
  );
}

const emptyBrandForm = { name: "", slug: "", logoUrl: "" };

function Brands() {
  const { request } = useAuth(); const queryClient = useQueryClient();
  const [form, setForm] = useState(emptyBrandForm); const [editing, setEditing] = useState<Brand | null>(null); const [notice, setNotice] = useState("");
  const query = useQuery({ queryKey: ["admin-brands"], queryFn: () => request<Brand[]>("/api/v1/admin/brands") });
  const save = useMutation({ mutationFn: () => { const body: BrandRequest = { name: form.name.trim(), slug: form.slug.trim() || undefined, logoUrl: form.logoUrl.trim() || undefined }; return request<Brand>(editing?.id ? `/api/v1/admin/brands/${editing.id}` : "/api/v1/admin/brands", { method: editing ? "PUT" : "POST", body }); }, onSuccess: () => { setNotice(editing ? "Đã cập nhật thương hiệu." : "Đã tạo thương hiệu."); setEditing(null); setForm(emptyBrandForm); void queryClient.invalidateQueries({ queryKey: ["admin-brands"] }); } });
  const status = useMutation({ mutationFn: ({ item, next }: { item: Brand; next: string }) => request<Brand>(`/api/v1/admin/brands/${item.id}/status`, { method: "PATCH", body: { status: next } }), onSuccess: () => { setNotice("Đã cập nhật trạng thái thương hiệu."); void queryClient.invalidateQueries({ queryKey: ["admin-brands"] }); } });
  return (
    <div className="split-workspace">
      <section className="panel editor-panel">
        <SectionHeader title={editing ? "Sửa thương hiệu" : "Tạo thương hiệu"} />
        <form onSubmit={(event) => { event.preventDefault(); setNotice(""); save.mutate(); }}><div className="form-grid"><Field label="Tên thương hiệu" required value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} /><Field label="Slug" value={form.slug} onChange={(event) => setForm({ ...form, slug: event.target.value })} /><Field className="full-span" label="URL logo" type="url" value={form.logoUrl} onChange={(event) => setForm({ ...form, logoUrl: event.target.value })} /></div>{save.isError ? <Notice tone="error">{apiErrorMessage(save.error)}</Notice> : null}<div className="form-actions"><Button disabled={save.isPending || !form.name.trim()}>{save.isPending ? "Đang lưu…" : editing ? "Lưu thay đổi" : "Tạo thương hiệu"}</Button>{editing ? <Button className="button-secondary" type="button" onClick={() => { setEditing(null); setForm(emptyBrandForm); }}>Hủy sửa</Button> : null}</div></form>
      </section>
      <section>{notice ? <Notice>{notice}</Notice> : null}{query.isPending ? <LoadingPanel /> : query.isError ? <ErrorPanel error={query.error} onRetry={() => void query.refetch()} /> : !query.data?.length ? <EmptyPanel title="Chưa có thương hiệu" description="Tạo thương hiệu đầu tiên bằng biểu mẫu bên cạnh." /> : <div className="panel compact-list"><SectionHeader title="Thương hiệu hiện có" description={`${query.data.length} mục`} />{query.data.map((item) => <article key={item.id}><div><strong>{item.name}</strong><small>{item.slug || shortId(item.id)}</small></div><StatusBadge value={item.status} /><div className="row-actions"><Button className="button-quiet" type="button" onClick={() => { setEditing(item); setForm({ name: item.name ?? "", slug: item.slug ?? "", logoUrl: item.logoUrl ?? "" }); }}><Edit3 aria-hidden="true" /> Sửa</Button><Button className="button-quiet" type="button" disabled={status.isPending} onClick={() => { const next = item.status === "ACTIVE" ? "INACTIVE" : "ACTIVE"; if (window.confirm(`Chuyển “${item.name}” sang ${next}?`)) status.mutate({ item, next }); }}><Power aria-hidden="true" /> {item.status === "ACTIVE" ? "Ngừng" : "Bật"}</Button></div></article>)}{status.isError ? <Notice tone="error">{apiErrorMessage(status.error)}</Notice> : null}</div>}</section>
    </div>
  );
}

const emptyAttributeForm = { categoryId: "", name: "", valueType: "TEXT" as AttributeDefinitionRequest["valueType"], required: false, sortOrder: "" };

function Attributes() {
  const { request } = useAuth(); const queryClient = useQueryClient();
  const [categoryId, setCategoryId] = useState(""); const [form, setForm] = useState(emptyAttributeForm); const [editing, setEditing] = useState<AttributeDefinition | null>(null); const [notice, setNotice] = useState("");
  const categories = useQuery({ queryKey: ["admin-categories"], queryFn: () => request<Category[]>("/api/v1/admin/categories") });
  const query = useQuery({ queryKey: ["admin-attributes", categoryId], queryFn: () => request<AttributeDefinition[]>(`/api/v1/admin/products/attributes${categoryId ? `?categoryId=${categoryId}` : ""}`) });
  const save = useMutation({ mutationFn: () => { const body: AttributeDefinitionRequest = { categoryId: form.categoryId || undefined, name: form.name.trim(), valueType: form.valueType, required: form.required, sortOrder: toOptionalNumber(form.sortOrder) }; return request<AttributeDefinition>(editing?.id ? `/api/v1/admin/products/attributes/${editing.id}` : "/api/v1/admin/products/attributes", { method: editing ? "PUT" : "POST", body }); }, onSuccess: () => { setNotice(editing ? "Đã cập nhật thuộc tính." : "Đã tạo thuộc tính."); setEditing(null); setForm({ ...emptyAttributeForm, categoryId }); void queryClient.invalidateQueries({ queryKey: ["admin-attributes"] }); } });
  return (
    <div className="split-workspace">
      <section className="panel editor-panel"><SectionHeader title={editing ? "Sửa thuộc tính" : "Tạo thuộc tính"} description="Định nghĩa kiểu dữ liệu mà Seller Center phải cung cấp." /><form onSubmit={(event) => { event.preventDefault(); save.mutate(); }}><div className="form-grid"><SelectField label="Danh mục áp dụng" value={form.categoryId} onChange={(event) => setForm({ ...form, categoryId: event.target.value })}><option value="">Mọi danh mục</option>{(categories.data ?? []).map((item) => <option value={item.id} key={item.id}>{item.name}</option>)}</SelectField><Field label="Tên thuộc tính" required value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} /><SelectField label="Kiểu giá trị" value={form.valueType} onChange={(event) => setForm({ ...form, valueType: event.target.value as AttributeDefinitionRequest["valueType"] })}><option value="TEXT">Văn bản</option><option value="NUMBER">Số</option><option value="BOOLEAN">Đúng/sai</option><option value="SELECT">Lựa chọn</option></SelectField><Field label="Thứ tự" type="number" min="0" value={form.sortOrder} onChange={(event) => setForm({ ...form, sortOrder: event.target.value })} /><label className="check-field full-span"><input type="checkbox" checked={form.required} onChange={(event) => setForm({ ...form, required: event.target.checked })} /> Bắt buộc người bán cung cấp</label></div>{save.isError ? <Notice tone="error">{apiErrorMessage(save.error)}</Notice> : null}<div className="form-actions"><Button disabled={save.isPending || !form.name.trim()}>{save.isPending ? "Đang lưu…" : editing ? "Lưu thay đổi" : <><Plus aria-hidden="true" /> Tạo thuộc tính</>}</Button>{editing ? <Button className="button-secondary" type="button" onClick={() => { setEditing(null); setForm({ ...emptyAttributeForm, categoryId }); }}>Hủy sửa</Button> : null}</div></form></section>
      <section>{notice ? <Notice>{notice}</Notice> : null}<div className="filter-bar filter-bar--single"><SelectField label="Lọc theo danh mục" value={categoryId} onChange={(event) => { setCategoryId(event.target.value); setForm((current) => ({ ...current, categoryId: event.target.value })); }}><option value="">Tất cả</option>{(categories.data ?? []).map((item) => <option value={item.id} key={item.id}>{item.name}</option>)}</SelectField></div>{query.isPending || categories.isPending ? <LoadingPanel /> : query.isError || categories.isError ? <ErrorPanel error={query.error ?? categories.error} onRetry={() => { void query.refetch(); void categories.refetch(); }} /> : !query.data?.length ? <EmptyPanel title="Chưa có thuộc tính" description="Tạo định nghĩa thuộc tính bằng biểu mẫu bên cạnh." /> : <div className="panel compact-list"><SectionHeader title="Định nghĩa thuộc tính" description={`${query.data.length} mục`} />{query.data.map((item) => <article key={item.id}><div><strong>{item.name}</strong><small>{item.valueType} · {item.required ? "bắt buộc" : "không bắt buộc"}</small></div><span className="mono">{shortId(item.categoryId)}</span><Button className="button-quiet" type="button" onClick={() => { setEditing(item); setForm({ categoryId: item.categoryId ?? "", name: item.name ?? "", valueType: (item.valueType as AttributeDefinitionRequest["valueType"]) ?? "TEXT", required: item.required ?? false, sortOrder: item.sortOrder?.toString() ?? "" }); }}><Edit3 aria-hidden="true" /> Sửa</Button></article>)}</div>}</section>
    </div>
  );
}
