import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ChatPage } from "@/pages/chat";

const request = vi.hoisted(() => vi.fn());

vi.mock("@/providers", () => ({
  useAuth: () => ({ request }),
  useShop: () => ({ shop: { id: "shop-a" }, loading: false }),
}));

const conversations = [
  {
    id: "conversation-a",
    shopId: "shop-a",
    shopName: "Gian hàng A",
    customerId: "customer-a",
    customerEmail: "khach-a@example.test",
    status: "ACTIVE",
    createdAt: "2026-08-11T01:00:00Z",
    updatedAt: "2026-08-11T01:00:00Z",
  },
  {
    id: "conversation-b",
    shopId: "shop-a",
    shopName: "Gian hàng A",
    customerId: "customer-b",
    customerEmail: "khach-b@example.test",
    status: "ACTIVE",
    createdAt: "2026-08-11T02:00:00Z",
    updatedAt: "2026-08-11T02:00:00Z",
  },
];

function renderChat() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(<MemoryRouter><QueryClientProvider client={client}><ChatPage /></QueryClientProvider></MemoryRouter>);
}

describe("Seller chat", () => {
  beforeEach(() => {
    request.mockReset();
    request.mockImplementation(async (path: string, options?: { method?: string; body?: unknown }) => {
      if (path.endsWith("/chat/conversations?size=50")) return { content: conversations };
      if (path.includes("conversation-a/messages")) return { content: [] };
      if (path.includes("conversation-b/messages") && options?.method === "POST") {
        return { id: "message-new", conversationId: "conversation-b", senderId: "seller-a", senderEmail: "seller@example.test", mine: true, type: "TEXT", textContent: "Xin chào", sentAt: "2026-08-11T03:00:00Z" };
      }
      if (path.includes("conversation-b/messages")) {
        return { content: [{ id: "message-b", conversationId: "conversation-b", senderId: "customer-b", senderEmail: "khach-b@example.test", mine: false, type: "TEXT", textContent: "Tin nhắn B", sentAt: "2026-08-11T02:10:00Z" }] };
      }
      throw new Error(`Unexpected request: ${path}`);
    });
  });

  it("chọn đúng hội thoại và gửi nội dung đã cắt khoảng trắng", async () => {
    renderChat();

    const secondConversation = await screen.findByRole("button", { name: /khach-b@example\.test/i });
    expect(screen.getByRole("button", { name: /khach-a@example\.test/i })).toHaveAttribute("aria-pressed", "true");
    fireEvent.click(secondConversation);

    expect(await screen.findByText("Tin nhắn B")).toBeInTheDocument();
    expect(secondConversation).toHaveAttribute("aria-pressed", "true");

    fireEvent.change(screen.getByLabelText("Phản hồi"), { target: { value: "  Xin chào  " } });
    fireEvent.click(screen.getByRole("button", { name: "Gửi tin nhắn" }));

    await waitFor(() => expect(request).toHaveBeenCalledWith(
      "/api/v1/seller/shops/shop-a/chat/conversations/conversation-b/messages",
      { method: "POST", body: { type: "TEXT", textContent: "Xin chào" } },
    ));
  });
});
