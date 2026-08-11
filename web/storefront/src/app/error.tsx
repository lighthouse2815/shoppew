"use client";

import { ErrorState } from "@shoppew/ui";

export default function ErrorPage({ error, reset }: { error: Error & { digest?: string }; reset: () => void }) {
  return <main className="shell page-section"><ErrorState message={error.message || "Đã có lỗi ngoài dự kiến."} onRetry={reset} /></main>;
}
