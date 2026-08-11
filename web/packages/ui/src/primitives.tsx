import { useId, type ButtonHTMLAttributes, type InputHTMLAttributes, type ReactNode } from "react";
import { AlertCircle, Inbox, LoaderCircle, RotateCcw } from "lucide-react";

export function Button({ className = "", children, ...props }: ButtonHTMLAttributes<HTMLButtonElement>) {
  return (
    <button className={`sp-button ${className}`.trim()} {...props}>
      {children}
    </button>
  );
}

export interface FieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
  hint?: string;
}

export function Field({ label, error, hint, id, className = "", ...props }: FieldProps) {
  const generatedId = useId();
  const fieldId = id ?? props.name ?? generatedId;
  const labelId = `${fieldId}-label`;
  const descriptionId = `${fieldId}-description`;
  return (
    <label className={`sp-field ${className}`.trim()} htmlFor={fieldId}>
      <span className="sp-field__label" id={labelId}>{label}</span>
      <input
        id={fieldId}
        className="sp-field__input"
        aria-labelledby={labelId}
        aria-invalid={Boolean(error)}
        aria-describedby={error || hint ? descriptionId : undefined}
        {...props}
      />
      {(error || hint) && (
        <span className={error ? "sp-field__error" : "sp-field__hint"} id={descriptionId}>
          {error ?? hint}
        </span>
      )}
    </label>
  );
}

export function Spinner({ label = "Đang tải" }: { label?: string }) {
  return (
    <span className="sp-spinner" role="status">
      <LoaderCircle aria-hidden="true" />
      <span>{label}</span>
    </span>
  );
}

export function EmptyState({ title, description, action }: { title: string; description: string; action?: ReactNode }) {
  return (
    <section className="sp-state" aria-labelledby="empty-state-title">
      <Inbox aria-hidden="true" />
      <h2 id="empty-state-title">{title}</h2>
      <p>{description}</p>
      {action}
    </section>
  );
}

export function ErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <section className="sp-state sp-state--error" role="alert">
      <AlertCircle aria-hidden="true" />
      <h2>Chưa thể tải dữ liệu</h2>
      <p>{message}</p>
      {onRetry && (
        <Button type="button" onClick={onRetry}>
          <RotateCcw aria-hidden="true" /> Thử lại
        </Button>
      )}
    </section>
  );
}

export function Skeleton({ className = "" }: { className?: string }) {
  return <span className={`sp-skeleton ${className}`.trim()} aria-hidden="true" />;
}

export function Price({ value, currency = "VND", compareAt }: { value: number; currency?: string; compareAt?: number | null }) {
  const format = new Intl.NumberFormat("vi-VN", { style: "currency", currency, maximumFractionDigits: 0 });
  return (
    <span className="sp-price">
      <strong>{format.format(value)}</strong>
      {compareAt && compareAt > value ? <del>{format.format(compareAt)}</del> : null}
    </span>
  );
}
