import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ChatCenter } from "./chat-center";

const request = vi.hoisted(() => vi.fn());

vi.mock("./providers", () => ({
  useAuth: () => ({ request }),
}));

const conversations = [
  {
    id: "conversation-a",
    shopId: "shop-a",
    shopName: "Gian hàng A",
    customerId: "customer-a",
    customerEmail: "khach@example.test",
    status: "ACTIVE",
    createdAt: "2026-08-11T01:00:00Z",
    updatedAt: "2026-08-11T01:00:00Z",
  },
  {
    id: "conversation-b",
    shopId: "shop-b",
    shopName: "Gian hàng B",
    customerId: "customer-a",
    customerEmail: "khach@example.test",
    status: "ACTIVE",
    createdAt: "2026-08-11T02:00:00Z",
    updatedAt: "2026-08-11T02:00:00Z",
  },
];

function renderChat() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(<QueryClientProvider client={client}><ChatCenter initialConversationId="stale-conversation" /></QueryClientProvider>);
}

describe("Customer chat", () => {
  beforeEach(() => {
    request.mockReset();
    request.mockImplementation(async (path: string, options?: { method?: string; body?: unknown }) => {
      if (path === "/api/v1/chat/conversations?size=50") return { content: conversations };
      if (path.includes("conversation-a/messages")) return { content: [] };
      if (path.includes("conversation-b/messages") && options?.method === "POST") {
        return { id: "message-new", conversationId: "conversation-b", senderId: "customer-a", senderEmail: "khach@example.test", mine: true, type: "TEXT", textContent: "Còn hàng không?", sentAt: "2026-08-11T03:00:00Z" };
      }
      if (path.includes("conversation-b/messages")) {
        return { content: [{ id: "message-b", conversationId: "conversation-b", senderId: "seller-b", senderEmail: "seller@example.test", mine: false, type: "TEXT", textContent: "Shop xin chào", sentAt: "2026-08-11T02:10:00Z" }] };
      }
      throw new Error(`Unexpected request: ${path}`);
    });
  });

  it("bỏ qua deep link cũ và gửi tin vào hội thoại đang chọn", async () => {
    renderChat();

    const secondConversation = await screen.findByRole("button", { name: /Gian hàng B/i });
    expect(request.mock.calls.some(([path]) => String(path).includes("stale-conversation"))).toBe(false);
    expect(screen.getByRole("button", { name: /Gian hàng A/i })).toHaveAttribute("aria-pressed", "true");

    fireEvent.click(secondConversation);
    expect(await screen.findByText("Shop xin chào")).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Nội dung"), { target: { value: "  Còn hàng không?  " } });
    fireEvent.click(screen.getByRole("button", { name: "Gửi" }));

    await waitFor(() => expect(request).toHaveBeenCalledWith(
      "/api/v1/chat/conversations/conversation-b/messages",
      { method: "POST", body: { type: "TEXT", textContent: "Còn hàng không?" } },
    ));
  });
});
