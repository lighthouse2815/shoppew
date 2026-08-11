export const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

export function isUuid(value: string): boolean {
  return UUID_PATTERN.test(value.trim());
}

export function validateDateRange(startsAt: string, endsAt: string): string | null {
  if (!startsAt || !endsAt) return "Chọn đầy đủ thời gian bắt đầu và kết thúc.";
  if (new Date(startsAt).getTime() >= new Date(endsAt).getTime()) return "Thời gian kết thúc phải sau thời gian bắt đầu.";
  return null;
}

export function toOptionalNumber(value: string): number | undefined {
  if (!value.trim()) return undefined;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : undefined;
}

export function isPositive(value: string, allowZero = false): boolean {
  const parsed = Number(value);
  return Number.isFinite(parsed) && (allowZero ? parsed >= 0 : parsed > 0);
}
