import { Button, EmptyState, ErrorState, Skeleton } from "@shoppew/ui";
import { ChevronLeft, ChevronRight, X } from "lucide-react";
import { useEffect, useId, useRef, type KeyboardEvent, type ReactNode } from "react";

export function PageHeader({
  eyebrow,
  title,
  description,
  action,
}: {
  eyebrow: string;
  title: string;
  description: string;
  action?: ReactNode;
}) {
  return (
    <header className="page-header">
      <div>
        <span>{eyebrow}</span>
        <h1>{title}</h1>
        <p>{description}</p>
      </div>
      {action ? <div className="page-header__action">{action}</div> : null}
    </header>
  );
}

export function SectionHeader({ title, description, action }: { title: string; description?: string; action?: ReactNode }) {
  return (
    <header className="section-header">
      <div><h2>{title}</h2>{description ? <p>{description}</p> : null}</div>
      {action}
    </header>
  );
}

export function LoadingPanel({ rows = 5, label = "Đang tải dữ liệu" }: { rows?: number; label?: string }) {
  return (
    <section className="panel loading-panel" role="status" aria-label={label}>
      <Skeleton className="skeleton-heading" />
      {Array.from({ length: rows }, (_, index) => <Skeleton className="skeleton-row" key={index} />)}
    </section>
  );
}

export function EmptyPanel({ title, description, action }: { title: string; description: string; action?: ReactNode }) {
  return <div className="panel state-panel"><EmptyState title={title} description={description} action={action} /></div>;
}

export function ErrorPanel({ error, onRetry }: { error: unknown; onRetry?: () => void }) {
  const message = error instanceof Error ? error.message : "Không thể tải dữ liệu quản trị.";
  return <div className="panel state-panel"><ErrorState message={message} onRetry={onRetry} /></div>;
}

export function StatusBadge({ value = "UNKNOWN" }: { value?: string }) {
  const normalized = value.toLowerCase().replaceAll("_", "-");
  const labels: Record<string, string> = {
    active: "Đang hoạt động",
    pending: "Chờ duyệt",
    "pending-verification": "Chờ xác minh",
    "pending-payment": "Chờ thanh toán",
    "pending-review": "Chờ xem xét",
    paid: "Đã thanh toán",
    authorized: "Đã ủy quyền",
    succeeded: "Thành công",
    failed: "Thất bại",
    confirmed: "Đã xác nhận",
    processing: "Đang xử lý",
    "ready-to-ship": "Chờ lấy hàng",
    shipped: "Đang giao",
    delivered: "Đã giao",
    completed: "Hoàn tất",
    "refund-requested": "Yêu cầu hoàn",
    "partially-refunded": "Hoàn một phần",
    requested: "Mới yêu cầu",
    "under-review": "Đang xem xét",
    approved: "Đã duyệt",
    rejected: "Đã từ chối",
    refunding: "Đang hoàn tiền",
    refunded: "Đã hoàn tiền",
    cancelled: "Đã hủy",
    suspended: "Tạm đình chỉ",
    banned: "Đã cấm",
    inactive: "Ngừng hoạt động",
    draft: "Bản nháp",
    scheduled: "Đã lên lịch",
    paused: "Tạm dừng",
    expired: "Hết hạn",
    ended: "Đã kết thúc",
    archived: "Đã lưu trữ",
    published: "Đã hiển thị",
    hidden: "Đã ẩn",
    removed: "Đã gỡ",
    open: "Đang mở",
    "awaiting-customer": "Chờ khách hàng",
    "awaiting-seller": "Chờ người bán",
    resolved: "Đã giải quyết",
    closed: "Đã đóng",
  };
  return <span className={`status-badge status-badge--${normalized}`}>{labels[normalized] ?? value}</span>;
}

export function Notice({ tone = "success", children }: { tone?: "success" | "error" | "info"; children: ReactNode }) {
  return <div className={`notice notice--${tone}`} role={tone === "error" ? "alert" : "status"}>{children}</div>;
}

export function Pagination({ page, totalPages, onChange, disabled = false }: { page: number; totalPages: number; onChange: (page: number) => void; disabled?: boolean }) {
  if (totalPages <= 1) return null;
  return (
    <nav className="pagination" aria-label="Phân trang">
      <Button className="button-secondary" type="button" disabled={disabled || page <= 0} onClick={() => onChange(page - 1)}>
        <ChevronLeft aria-hidden="true" /> Trang trước
      </Button>
      <span>Trang {page + 1} / {totalPages}</span>
      <Button className="button-secondary" type="button" disabled={disabled || page >= totalPages - 1} onClick={() => onChange(page + 1)}>
        Trang sau <ChevronRight aria-hidden="true" />
      </Button>
    </nav>
  );
}

type TabOption<T extends string> = {
  value: T;
  label: string;
};

export function TabSet<T extends string>({
  activeTab,
  ariaLabel,
  idPrefix,
  onChange,
  renderPanel,
  tabs,
}: {
  activeTab: T;
  ariaLabel: string;
  idPrefix: string;
  onChange: (tab: T) => void;
  renderPanel: (tab: T) => ReactNode;
  tabs: readonly TabOption<T>[];
}) {
  const tabRefs = useRef<Array<HTMLButtonElement | null>>([]);

  function activate(index: number) {
    const option = tabs[index];
    if (!option) return;
    onChange(option.value);
    tabRefs.current[index]?.focus();
  }

  function handleKeyDown(event: KeyboardEvent<HTMLButtonElement>, index: number) {
    let nextIndex: number | null = null;
    if (event.key === "ArrowRight") nextIndex = (index + 1) % tabs.length;
    if (event.key === "ArrowLeft") nextIndex = (index - 1 + tabs.length) % tabs.length;
    if (event.key === "Home") nextIndex = 0;
    if (event.key === "End") nextIndex = tabs.length - 1;
    if (nextIndex === null) return;
    event.preventDefault();
    activate(nextIndex);
  }

  return (
    <>
      <div className="tabs" role="tablist" aria-label={ariaLabel}>
        {tabs.map((option, index) => {
          const selected = option.value === activeTab;
          return (
            <button
              aria-controls={`${idPrefix}-panel-${option.value}`}
              aria-selected={selected}
              id={`${idPrefix}-tab-${option.value}`}
              key={option.value}
              onClick={() => onChange(option.value)}
              onKeyDown={(event) => handleKeyDown(event, index)}
              ref={(element) => { tabRefs.current[index] = element; }}
              role="tab"
              tabIndex={selected ? 0 : -1}
              type="button"
            >
              {option.label}
            </button>
          );
        })}
      </div>
      {tabs.map((option) => {
        const selected = option.value === activeTab;
        return (
          <div
            aria-labelledby={`${idPrefix}-tab-${option.value}`}
            hidden={!selected}
            id={`${idPrefix}-panel-${option.value}`}
            key={option.value}
            role="tabpanel"
            tabIndex={selected ? 0 : -1}
          >
            {selected ? renderPanel(option.value) : null}
          </div>
        );
      })}
    </>
  );
}

export function SelectField({ label, children, error, ...props }: React.SelectHTMLAttributes<HTMLSelectElement> & { label: string; error?: string }) {
  const id = useId();
  return (
    <label className="select-field" htmlFor={id}>
      <span>{label}</span>
      <select id={id} aria-invalid={Boolean(error)} aria-describedby={error ? `${id}-error` : undefined} {...props}>{children}</select>
      {error ? <small className="field-error" id={`${id}-error`}>{error}</small> : null}
    </label>
  );
}

export function TextAreaField({ label, error, ...props }: React.TextareaHTMLAttributes<HTMLTextAreaElement> & { label: string; error?: string }) {
  const id = useId();
  return (
    <label className="textarea-field" htmlFor={id}>
      <span>{label}</span>
      <textarea id={id} aria-invalid={Boolean(error)} aria-describedby={error ? `${id}-error` : undefined} {...props} />
      {error ? <small className="field-error" id={`${id}-error`}>{error}</small> : null}
    </label>
  );
}

export function Dialog({ open, title, description, children, onClose }: { open: boolean; title: string; description?: string; children: ReactNode; onClose: () => void }) {
  const ref = useRef<HTMLDialogElement>(null);
  const id = useId();
  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (open && !dialog.open) dialog.showModal();
    if (!open && dialog.open) dialog.close();
  }, [open]);
  return (
    <dialog
      aria-describedby={description ? `${id}-description` : undefined}
      aria-labelledby={`${id}-title`}
      className="admin-dialog"
      ref={ref}
      onCancel={onClose}
      onClose={onClose}
    >
      <header>
        <div><h2 id={`${id}-title`}>{title}</h2>{description ? <p id={`${id}-description`}>{description}</p> : null}</div>
        <button className="icon-button" type="button" aria-label="Đóng hộp thoại" onClick={onClose}><X aria-hidden="true" /></button>
      </header>
      {children}
    </dialog>
  );
}
