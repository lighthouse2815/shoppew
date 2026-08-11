"use client";

import { useMutation } from "@tanstack/react-query";
import { MessageCircle } from "lucide-react";
import { useRouter } from "next/navigation";
import { useEffect } from "react";
import type { Conversation } from "@/lib/types";
import { useAuth } from "./providers";

export function ProductViewRecorder({ productId }: { productId: string }) {
  const { status, request } = useAuth();
  useEffect(() => {
    if (status === "authenticated") {
      void request(`/api/v1/recommendations/recently-viewed/${productId}`, { method: "POST" }).catch(() => undefined);
    }
  }, [productId, request, status]);
  return null;
}

export function ProductChatButton({ productId, shopId, slug }: { productId: string; shopId: string; slug: string }) {
  const router = useRouter();
  const { status, request } = useAuth();
  const start = useMutation({
    mutationFn: () => request<Conversation>("/api/v1/chat/conversations", {
      method: "POST",
      body: { shopId, productId },
    }),
    onSuccess: (conversation) => router.push(`/account/messages?conversation=${conversation.id}`),
  });
  const openChat = () => {
    if (status !== "authenticated") {
      router.push(`/login?returnTo=${encodeURIComponent(`/product/${slug}`)}`);
      return;
    }
    start.mutate();
  };
  return (
    <div className="product-chat-action">
      <button className="sp-button sp-button--secondary" type="button" onClick={openChat} disabled={start.isPending}>
        <MessageCircle aria-hidden="true" /> {start.isPending ? "Đang mở hội thoại..." : "Nhắn tin cho shop"}
      </button>
      {start.error && <p className="notice notice--error">{start.error.message}</p>}
    </div>
  );
}
