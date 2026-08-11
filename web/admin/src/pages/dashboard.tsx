import { useQuery } from "@tanstack/react-query";
import { Button, Field } from "@shoppew/ui";
import { ArrowRight, BadgeDollarSign, Building2, PackageCheck, RotateCcw, Users, WalletCards } from "lucide-react";
import { Link } from "react-router-dom";
import { ErrorPanel, LoadingPanel, PageHeader, SectionHeader } from "@/components/common";
import { formatBusinessTime, formatMoney, formatNumber, localDateTimeToIso } from "@/lib/format";
import type { AdminAnalytics } from "@/lib/types";
import { useAuth } from "@/providers";
import { useState } from "react";

export function DashboardPage() {
  const { request } = useAuth();
  const [draftFrom, setDraftFrom] = useState("");
  const [draftTo, setDraftTo] = useState("");
  const [range, setRange] = useState({ from: "", to: "" });
  const [rangeError, setRangeError] = useState("");
  const query = useQuery({
    queryKey: ["admin-analytics", range],
    queryFn: () => {
      const params = new URLSearchParams();
      if (range.from) params.set("from", localDateTimeToIso(range.from));
      if (range.to) params.set("to", localDateTimeToIso(range.to));
      return request<AdminAnalytics>(`/api/v1/admin/analytics${params.size ? `?${params}` : ""}`);
    },
  });

  function applyRange(event: React.FormEvent) {
    event.preventDefault();
    if (draftFrom && draftTo && new Date(draftFrom).getTime() >= new Date(draftTo).getTime()) {
      setRangeError("Mốc kết thúc phải sau mốc bắt đầu.");
      return;
    }
    setRangeError("");
    setRange({ from: draftFrom, to: draftTo });
  }

  return (
    <>
      <PageHeader eyebrow="Marketplace pulse" title="Tổng quan vận hành" description="GMV, đơn hoàn tất, người dùng, gian hàng và rủi ro trong khoảng thời gian do backend tổng hợp." />
      <form className="filter-bar" onSubmit={applyRange}>
        <Field label="Từ thời điểm" type="datetime-local" value={draftFrom} onChange={(event) => setDraftFrom(event.target.value)} />
        <Field label="Đến thời điểm" type="datetime-local" value={draftTo} onChange={(event) => setDraftTo(event.target.value)} error={rangeError || undefined} />
        <Button disabled={query.isFetching}>{query.isFetching ? "Đang cập nhật…" : "Áp dụng"}</Button>
        {(draftFrom || draftTo) ? <Button className="button-secondary" type="button" onClick={() => { setDraftFrom(""); setDraftTo(""); setRange({ from: "", to: "" }); setRangeError(""); }}>Xóa khoảng lọc</Button> : null}
      </form>
      {query.isPending ? <LoadingPanel rows={6} /> : query.isError ? <ErrorPanel error={query.error} onRetry={() => void query.refetch()} /> : query.data ? (
        <>
          <section className="metric-grid" aria-label="Chỉ số vận hành">
            <Metric icon={BadgeDollarSign} label="GMV" value={formatMoney(query.data.gmv)} />
            <Metric icon={WalletCards} label="Đơn hoàn tất" value={formatNumber(query.data.completedOrders)} />
            <Metric icon={Users} label="Người dùng mới" value={formatNumber(query.data.newUsers)} />
            <Metric icon={Building2} label="Gian hàng hoạt động" value={formatNumber(query.data.activeShops)} />
            <Metric icon={PackageCheck} label="Chờ kiểm duyệt" value={formatNumber(query.data.pendingModeration)} tone="warning" />
            <Metric icon={RotateCcw} label="Giá trị hoàn tiền" value={formatMoney(query.data.refundVolume)} tone="coral" />
          </section>
          <section className="panel analytics-period">
            <SectionHeader title="Phạm vi dữ liệu" description="Múi giờ hiển thị: Asia/Ho_Chi_Minh" />
            <dl><div><dt>Bắt đầu</dt><dd>{formatBusinessTime(query.data.from)}</dd></div><div><dt>Kết thúc</dt><dd>{formatBusinessTime(query.data.to)}</dd></div></dl>
          </section>
        </>
      ) : null}
      <section className="panel operation-links">
        <SectionHeader title="Hàng đợi cần xử lý" description="Mở đúng không gian tác vụ; số liệu trong từng hàng đợi được tải trực tiếp khi truy cập." />
        <div>
          <OperationLink to="/users" label="Quản lý người dùng" />
          <OperationLink to="/products" label="Duyệt sản phẩm" />
          <OperationLink to="/orders" label="Kiểm tra đơn hàng" />
          <OperationLink to="/refunds" label="Xử lý hoàn tiền" />
          <OperationLink to="/disputes" label="Điều phối tranh chấp" />
          <OperationLink to="/audit-logs" label="Kiểm tra nhật ký" />
        </div>
      </section>
    </>
  );
}

function Metric({ icon: Icon, label, value, tone = "brand" }: { icon: React.ComponentType<{ "aria-hidden"?: boolean }>; label: string; value: string; tone?: string }) {
  return <article className={`metric metric--${tone}`}><Icon aria-hidden={true} /><span>{label}</span><strong>{value}</strong></article>;
}

function OperationLink({ to, label }: { to: string; label: string }) {
  return <Link to={to}>{label}<ArrowRight aria-hidden="true" /></Link>;
}
