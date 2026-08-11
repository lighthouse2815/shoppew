import { AlertCircle, Inbox } from "lucide-react";
import { Button, Spinner } from "@shoppew/ui";
import { useShop } from "@/providers";

export function PageHeader({ eyebrow, title, description, action }: { eyebrow: string; title: string; description?: string; action?: React.ReactNode }) { return <div className="page-header"><div><span>{eyebrow}</span><h1>{title}</h1>{description && <p>{description}</p>}</div>{action}</div>; }
export function Loading({ label = "Đang tải dữ liệu" }: { label?: string }) { return <div className="state-block"><Spinner label={label} /></div>; }
export function ErrorBlock({ error, retry }: { error: Error; retry?: () => void }) { return <div className="state-block state-block--error"><AlertCircle /><h2>Chưa thể tải dữ liệu</h2><p>{error.message}</p>{retry && <Button onClick={retry}>Thử lại</Button>}</div>; }
export function Empty({ title, description, action }: { title: string; description: string; action?: React.ReactNode }) { return <div className="state-block"><Inbox /><h2>{title}</h2><p>{description}</p>{action}</div>; }
export function NeedShop({ children }: { children: React.ReactNode }) { const { shop, loading } = useShop(); if (loading) return <Loading label="Đang tải gian hàng" />; if (!shop) return <Empty title="Chưa có gian hàng" description="Tạo gian hàng tại trang Tổng quan trước khi dùng chức năng này." />; return children; }
export function Status({ value = "UNKNOWN" }: { value?: string }) { return <span className={`badge badge--${value.toLowerCase().replaceAll("_", "-")}`}>{value.replaceAll("_", " ")}</span>; }
