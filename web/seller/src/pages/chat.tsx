import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Image, MessageCircle, Package, Send, UserRound } from "lucide-react";
import { Link } from "react-router-dom";
import { Button } from "@shoppew/ui";
import { Empty, ErrorBlock, Loading, NeedShop, PageHeader } from "@/components/common";
import { dateTime } from "@/lib/format";
import type { ChatMessage, Conversation, Page } from "@/lib/types";
import { useAuth, useShop } from "@/providers";

export function ChatPage() {
  const { request } = useAuth();
  const { shop } = useShop();
  const queryClient = useQueryClient();
  const [selectedConversationId, setSelectedConversationId] = useState("");
  const [text, setText] = useState("");
  const shopId = shop?.id;
  const conversations = useQuery({
    queryKey: ["seller-chat-conversations", shopId],
    queryFn: () => request<Page<Conversation>>(`/api/v1/seller/shops/${shopId}/chat/conversations?size=50`),
    enabled: Boolean(shopId),
    refetchInterval: 8_000,
  });
  const availableConversations = conversations.data?.content ?? [];
  const selectedId = availableConversations.some((conversation) => conversation.id === selectedConversationId)
    ? selectedConversationId
    : availableConversations[0]?.id ?? "";
  const messages = useQuery({
    queryKey: ["seller-chat-messages", shopId, selectedId],
    queryFn: () => request<Page<ChatMessage>>(`/api/v1/seller/shops/${shopId}/chat/conversations/${selectedId}/messages?size=100`),
    enabled: Boolean(shopId && selectedId),
    refetchInterval: selectedId ? 4_000 : false,
  });
  const send = useMutation({
    mutationFn: ({ targetShopId, conversationId, textContent }: { targetShopId: string; conversationId: string; textContent: string }) => request<ChatMessage>(`/api/v1/seller/shops/${targetShopId}/chat/conversations/${conversationId}/messages`, {
      method: "POST",
      body: { type: "TEXT", textContent },
    }),
    onSuccess: async (_message, variables) => {
      if (variables.conversationId === selectedId) setText("");
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["seller-chat-messages", variables.targetShopId, variables.conversationId] }),
        queryClient.invalidateQueries({ queryKey: ["seller-chat-conversations", variables.targetShopId] }),
      ]);
    },
  });
  const ordered = [...(messages.data?.content ?? [])].reverse();

  return <NeedShop><PageHeader eyebrow="Hỗ trợ khách hàng" title="Chat với khách hàng" description="Tin nhắn được lưu an toàn; chỉ khách và thành viên shop đang hoạt động mới truy cập được." />
    {conversations.isPending ? <Loading label="Đang tải hội thoại" /> : conversations.error ? <ErrorBlock error={conversations.error} retry={() => void conversations.refetch()} /> : !conversations.data?.content?.length ? <Empty title="Chưa có hội thoại" description="Khi khách nhắn từ trang sản phẩm, hội thoại sẽ xuất hiện tại đây." /> : <div className="seller-chat-layout">
      <aside className="seller-chat-list" aria-label="Danh sách hội thoại">{conversations.data.content.map((conversation) => <button key={conversation.id} type="button" className={selectedId === conversation.id ? "active" : ""} aria-pressed={selectedId === conversation.id} onClick={() => { send.reset(); setText(""); setSelectedConversationId(conversation.id); }}><UserRound aria-hidden="true" /><span><strong>{conversation.customerEmail}</strong><small>{conversation.lastMessagePreview || "Chưa có tin nhắn"}</small></span>{conversation.lastMessageAt && <time>{dateTime(conversation.lastMessageAt)}</time>}</button>)}</aside>
      <section className="seller-chat-thread">{messages.isPending ? <Loading label="Đang tải tin nhắn" /> : messages.error ? <ErrorBlock error={messages.error} retry={() => void messages.refetch()} /> : <div className="seller-chat-messages" aria-live="polite">{ordered.length ? ordered.map((message) => <SellerMessage key={message.id} message={message} />) : <Empty title="Chưa có tin nhắn" description="Hãy gửi phản hồi đầu tiên cho khách hàng." />}</div>}<form className="seller-chat-composer" onSubmit={(event) => { event.preventDefault(); const textContent = text.trim(); if (textContent && shopId && selectedId && !messages.error) send.mutate({ targetShopId: shopId, conversationId: selectedId, textContent }); }}><label><span>Phản hồi</span><textarea rows={3} maxLength={4000} value={text} onChange={(event) => setText(event.target.value)} disabled={messages.isPending || Boolean(messages.error)} aria-describedby={send.error ? "seller-chat-send-error" : undefined} /></label>{send.error && <p className="form-error" id="seller-chat-send-error" role="alert">{send.error.message}</p>}<Button type="submit" disabled={send.isPending || messages.isPending || Boolean(messages.error) || !text.trim()} title={messages.error ? "Tải lại hội thoại trước khi gửi" : undefined}>{send.isPending ? "Đang gửi..." : <><Send aria-hidden="true" /> Gửi tin nhắn</>}</Button></form></section>
    </div>}
  </NeedShop>;
}

function SellerMessage({ message }: { message: ChatMessage }) {
  return <article className={message.mine ? "mine" : ""}><small>{message.mine ? "Shop" : message.senderEmail}</small>{message.type === "TEXT" && <p>{message.textContent}</p>}{message.type === "IMAGE" && <a href={message.mediaUrl} target="_blank" rel="noreferrer"><Image /> Xem hình ảnh</a>}{message.type === "PRODUCT" && <Link to={`/products/${message.productId}`}><MessageCircle /> {message.productName}</Link>}{message.type === "ORDER" && <Link to={`/orders/${message.orderId}`}><Package /> Đơn #{message.orderNumber}</Link>}<time>{dateTime(message.sentAt)}</time></article>;
}
