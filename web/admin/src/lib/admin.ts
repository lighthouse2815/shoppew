export type AdminQueryValue = string | number | boolean | null | undefined;

export function buildAdminQuery(values: Record<string, AdminQueryValue>): string {
  const params = new URLSearchParams();
  for (const [key, rawValue] of Object.entries(values)) {
    if (rawValue === undefined || rawValue === null) continue;
    const value = typeof rawValue === "string" ? rawValue.trim() : String(rawValue);
    if (!value) continue;
    params.set(key, value);
  }
  const query = params.toString();
  return query ? `?${query}` : "";
}

export function displayIdentity(displayName?: string, email?: string): string {
  return displayName?.trim() || email?.trim() || "Tài khoản chưa đặt tên";
}

export function bytesLabel(value?: number): string {
  if (value === undefined || value < 0 || !Number.isFinite(value)) return "—";
  if (value < 1024) return `${value} B`;
  const units = ["KB", "MB", "GB", "TB"];
  let amount = value / 1024;
  let unitIndex = 0;
  while (amount >= 1024 && unitIndex < units.length - 1) {
    amount /= 1024;
    unitIndex += 1;
  }
  return `${new Intl.NumberFormat("vi-VN", { maximumFractionDigits: amount >= 10 ? 0 : 1 }).format(amount)} ${units[unitIndex]}`;
}
