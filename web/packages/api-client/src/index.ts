export type { paths, components } from "./schema";

export interface ApiErrorDetail {
  field?: string;
  message?: string;
  rejectedValue?: unknown;
}

export interface ApiErrorBody {
  code: string;
  message: string;
  details?: ApiErrorDetail[];
}

export interface ApiEnvelope<T> {
  success: boolean;
  data?: T;
  error?: ApiErrorBody;
  timestamp: string;
}

export class ShoppewApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly details: ApiErrorDetail[];

  constructor(status: number, error?: ApiErrorBody) {
    super(error?.message ?? "Không thể kết nối đến shoppew. Vui lòng thử lại.");
    this.name = "ShoppewApiError";
    this.status = status;
    this.code = error?.code ?? "NETWORK_ERROR";
    this.details = error?.details ?? [];
  }
}

export interface ApiRequestOptions extends Omit<RequestInit, "body"> {
  token?: string | null;
  body?: unknown;
  requestId?: string;
}

export class ShoppewApiClient {
  constructor(
    readonly baseUrl: string,
    private readonly onUnauthorized?: () => Promise<string | null>,
  ) {}

  async request<T>(path: string, options: ApiRequestOptions = {}, allowRefresh = true): Promise<T> {
    const headers = new Headers(options.headers);
    headers.set("Accept", "application/json");
    headers.set("X-Request-Id", options.requestId ?? crypto.randomUUID());
    if (options.token) headers.set("Authorization", `Bearer ${options.token}`);
    let body: BodyInit | undefined;
    if (options.body instanceof FormData) {
      body = options.body;
    } else if (options.body !== undefined) {
      headers.set("Content-Type", "application/json");
      body = JSON.stringify(options.body);
    }
    let response: Response;
    try {
      response = await fetch(`${this.baseUrl}${path}`, {
        ...options,
        headers,
        body,
        credentials: "include",
      });
    } catch {
      throw new ShoppewApiError(0);
    }
    if (response.status === 401 && allowRefresh && this.onUnauthorized) {
      const token = await this.onUnauthorized();
      if (token) return this.request<T>(path, { ...options, token }, false);
    }
    if (response.status === 204) return undefined as T;
    const envelope = (await response.json()) as ApiEnvelope<T>;
    if (!response.ok || !envelope.success || envelope.data === undefined) {
      throw new ShoppewApiError(response.status, envelope.error);
    }
    return envelope.data;
  }
}
