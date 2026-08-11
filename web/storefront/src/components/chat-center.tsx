"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { EmptyState, ErrorState, Spinner } from "@shoppew/ui";
import { ImageIcon, MessageCircle, Package, Send, Store } from "lucide-react";
import Link from "next/link";
import { FormEvent, useState } from "react";
import { formatDateTime } from "@/lib/format";
import type { ChatMessage, Conversation, Page } from "@/lib/types";
import { useAuth } from "./providers";

export function ChatCenter({ initialConversationId = "" }: { initialConversationId?: string }) {
  const { request } = useAuth();
  const queryClient = useQueryClient();
  const [selectedConversationId, setSelectedConversationId] = useState(initialConversationId);
  const [text, setText] = useState("");
  const conversations = useQuery({
    queryKey: ["customer-chat-conversations"],
    queryFn: () => request<Page<Conversation>>("/api/v1/chat/conversations?size=50"),
    refetchInterval: 8_000,
  });
  const availableConversations = conversations.data?.content ?? [];
  const selectedId = availableConversations.some((conversation) => conversation.id === selectedConversationId)
    ? selectedConversationId
    : availableConversations[0]?.id ?? "";
  const messages = useQuery({
    queryKey: ["customer-chat-messages", selectedId],
    queryFn: () => request<Page<ChatMessage>>(`/api/v1/chat/conversations/${selectedId}/messages?size=100`),
    enabled: Boolean(selectedId),
    refetchInterval: selectedId ? 4_000 : false,
  });
  const send = useMutation({
    mutationFn: ({ conversationId, textContent }: { conversationId: string; textContent: string }) => request<ChatMessage>(`/api/v1/chat/conversations/${conversationId}/messages`, {
      method: "POST",
      body: { type: "TEXT", textContent },
    }),
    onSuccess: async (_message, variables) => {
      if (variables.conversationId === selectedId) setText("");
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["customer-chat-messages", variables.conversationId] }),
        queryClient.invalidateQueries({ queryKey: ["customer-chat-conversations"] }),
      ]);
    },
  });
  const submit = (event: FormEvent) => {
    event.preventDefault();
    const textContent = text.trim();
    if (textContent && selectedId && !messages.error) send.mutate({ conversationId: selectedId, textContent });
  };

  if (conversations.isPending) return <Spinner label="Đang tải hội thoại" />;
  if (conversations.error) return <ErrorState message={conversations.error.message} onRetry={() => void conversations.refetch()} />;
  if (!conversations.data?.content?.length) return <EmptyState title="Chưa có cuộc trò chuyện" description="Mở một sản phẩm và chọn Nhắn tin cho shop để bắt đầu." action={<Link className="sp-button" href="/search">Khám phá sản phẩm</Link>} />;

  const orderedMessages = [...(messages.data?.content ?? [])].reverse();
  return (
    <section>
      <div className="section-heading"><div><span className="eyebrow">Hỗ trợ từ nhà bán</span><h1>Tin nhắn</h1><p>Hội thoại được lưu trên máy chủ và chỉ các bên tham gia mới truy cập được.</p></div></div>
      <div className="chat-layout">
        <aside className="chat-conversations" aria-label="Danh sách hội thoại">
          {conversations.data.content.map((conversation) => (
            <button key={conversation.id} type="button" className={selectedId === conversation.id ? "active" : ""} aria-pressed={selectedId === conversation.id} onClick={() => { send.reset(); setText(""); setSelectedConversationId(conversation.id); }}>
              <Store aria-hidden="true" />
              <span><strong>{conversation.shopName}</strong><small>{conversation.lastMessagePreview || "Chưa có tin nhắn"}</small></span>
              {conversation.lastMessageAt && <time>{formatDateTime(conversation.lastMessageAt)}</time>}
            </button>
          ))}
        </aside>
        <div className="chat-thread">
          {messages.isPending ? <Spinner label="Đang tải tin nhắn" /> : messages.error ? <ErrorState message={messages.error.message} onRetry={() => void messages.refetch()} /> : (
            <div className="chat-messages" aria-live="polite">
              {orderedMessages.length ? orderedMessages.map((message) => <MessageBubble key={message.id} message={message} />) : <EmptyState title="Chưa có tin nhắn" description="Hãy gửi câu hỏi đầu tiên tới gian hàng." />}
            </div>
          )}
          <form className="chat-composer" method="post" onSubmit={submit}>
            <label htmlFor="chat-message">Nội dung</label>
            <textarea id="chat-message" rows={2} maxLength={4000} value={text} onChange={(event) => setText(event.target.value)} placeholder="Nhập câu hỏi cho nhà bán..." disabled={messages.isPending || Boolean(messages.error)} aria-describedby={send.error ? "customer-chat-send-error" : undefined} />
            <button className="sp-button" type="submit" disabled={!text.trim() || send.isPending || messages.isPending || Boolean(messages.error)} title={messages.error ? "Tải lại hội thoại trước khi gửi" : undefined}>{send.isPending ? "Đang gửi..." : <><Send aria-hidden="true" /> Gửi</>}</button>
            {send.error && <p className="notice notice--error" id="customer-chat-send-error" role="alert">{send.error.message}</p>}
          </form>
        </div>
      </div>
    </section>
  );
}

function MessageBubble({ message }: { message: ChatMessage }) {
  return (
    <article className={message.mine ? "chat-message mine" : "chat-message"}>
      <small>{message.mine ? "Bạn" : message.senderEmail}</small>
      {message.type === "TEXT" && <p>{message.textContent}</p>}
      {message.type === "IMAGE" && <a href={message.mediaUrl} target="_blank" rel="noreferrer"><ImageIcon aria-hidden="true" /> Xem hình ảnh</a>}
      {message.type === "PRODUCT" && <Link href={`/search?q=${encodeURIComponent(message.productName ?? "")}`}><MessageCircle aria-hidden="true" /> {message.productName}</Link>}
      {message.type === "ORDER" && <Link href={`/account/orders/${message.orderId}`}><Package aria-hidden="true" /> Đơn #{message.orderNumber}</Link>}
      <time>{formatDateTime(message.sentAt)}</time>
    </article>
  );
}
