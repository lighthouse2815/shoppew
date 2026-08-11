import Link from "next/link";
import { CheckCircle2 } from "lucide-react";
import { Price } from "@shoppew/ui";

export default async function OrderSuccessPage({ searchParams }: { searchParams: Promise<{ checkout?: string; total?: string; currency?: string; orders?: string }> }) {
  const params = await searchParams; const orderIds = params.orders?.split(",").filter(Boolean) ?? [];
  return <main className="shell success-page page-section"><CheckCircle2 /><span className="eyebrow">Đặt hàng thành công</span><h1>Cảm ơn bạn đã mua sắm tại shoppew.</h1><p>Mã checkout: <strong>{params.checkout || "Đã ghi nhận"}</strong></p><Price value={Number(params.total) || 0} currency={params.currency} /><div className="hero-actions">{orderIds[0] ? <Link className="sp-button" href={`/account/orders/${orderIds[0]}`}>Xem đơn hàng</Link> : <Link className="sp-button" href="/account/orders">Xem đơn mua</Link>}<Link className="sp-button sp-button--secondary" href="/search">Tiếp tục mua sắm</Link></div><div className="notice">Nếu checkout chứa sản phẩm từ nhiều shop, shoppew đã tách thành nhiều đơn để mỗi nhà bán xử lý độc lập.</div></main>;
}
