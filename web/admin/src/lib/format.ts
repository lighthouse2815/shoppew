const moneyFormatter = new Intl.NumberFormat("vi-VN", {
  style: "currency",
  currency: "VND",
  maximumFractionDigits: 0,
});

const numberFormatter = new Intl.NumberFormat("vi-VN");
const dateTimeFormatter = new Intl.DateTimeFormat("vi-VN", {
  timeZone: "Asia/Ho_Chi_Minh",
  dateStyle: "short",
  timeStyle: "short",
});

export function formatMoney(value?: number | null, currency = "VND"): string {
  if (value == null || Number.isNaN(value)) return "—";
  if (currency === "VND") return moneyFormatter.format(value);
  return new Intl.NumberFormat("vi-VN", { style: "currency", currency }).format(value);
}

export function formatNumber(value?: number | null): string {
  return value == null ? "—" : numberFormatter.format(value);
}

export function formatBusinessTime(value?: string | null): string {
  if (!value) return "—";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "—" : dateTimeFormatter.format(date);
}

export function toDateTimeLocal(value?: string | null): string {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  const parts = new Intl.DateTimeFormat("sv-SE", {
    timeZone: "Asia/Ho_Chi_Minh",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
  }).formatToParts(date);
  const part = (type: Intl.DateTimeFormatPartTypes) => parts.find((item) => item.type === type)?.value ?? "";
  return `${part("year")}-${part("month")}-${part("day")}T${part("hour")}:${part("minute")}`;
}

export function localDateTimeToIso(value: string): string {
  if (!value) return "";
  return new Date(`${value}:00+07:00`).toISOString();
}

export function parseIds(value: string): string[] {
  return [...new Set(value.split(/[\s,;]+/).map((item) => item.trim()).filter(Boolean))];
}

export function apiErrorMessage(error: unknown): string {
  return error instanceof Error ? error.message : "Không thể hoàn tất yêu cầu. Vui lòng thử lại.";
}

export function shortId(value?: string | null): string {
  return value ? `${value.slice(0, 8)}…` : "—";
}
