import { ChatCenter } from "@/components/chat-center";

export default async function MessagesPage({ searchParams }: { searchParams: Promise<{ conversation?: string }> }) {
  const { conversation = "" } = await searchParams;
  return <ChatCenter initialConversationId={conversation} />;
}
