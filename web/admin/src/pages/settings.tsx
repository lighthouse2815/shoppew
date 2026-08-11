import { useQuery } from "@tanstack/react-query";
import { Button } from "@shoppew/ui";
import { Boxes, Clock3, Coins, Languages, RefreshCw, UploadCloud } from "lucide-react";
import { EmptyPanel, ErrorPanel, LoadingPanel, PageHeader, SectionHeader } from "@/components/common";
import { bytesLabel } from "@/lib/admin";
import type { AdminSettings } from "@/lib/types";
import { useAuth } from "@/providers";

export function SettingsPage() {
  const { request } = useAuth();
  const query = useQuery({ queryKey: ["admin-settings"], queryFn: () => request<AdminSettings>("/api/v1/admin/settings") });
  return (
    <>
      <PageHeader eyebrow="Runtime contract" title="Cấu hình vận hành" description="Thông số hiệu lực mà backend công bố cho locale, tiền tệ, múi giờ, tích hợp và giới hạn tải lên." action={<Button className="button-secondary" type="button" disabled={query.isFetching} onClick={() => void query.refetch()}><RefreshCw aria-hidden="true" /> {query.isFetching ? "Đang tải…" : "Làm mới"}</Button>} />
      {query.isPending ? <LoadingPanel rows={7} label="Đang tải cấu hình vận hành" /> : query.isError ? <ErrorPanel error={query.error} onRetry={() => void query.refetch()} /> : query.data ? <SettingsView settings={query.data} /> : <EmptyPanel title="Backend chưa công bố cấu hình" description="Không có dữ liệu cấu hình khả dụng trong phản hồi hiện tại." />}
    </>
  );
}

function SettingsView({ settings }: { settings: AdminSettings }) {
  return (
    <div className="settings-layout">
      <section className="settings-metrics" aria-label="Cấu hình nền tảng">
        <SettingMetric icon={Languages} label="Locale" value={settings.locale || "—"} />
        <SettingMetric icon={Coins} label="Tiền tệ" value={settings.currency || "—"} />
        <SettingMetric icon={Clock3} label="Múi giờ kinh doanh" value={settings.timeZone || "—"} />
        <SettingMetric icon={UploadCloud} label="Giới hạn mỗi tệp" value={bytesLabel(settings.maxUploadBytes)} />
      </section>
      <section className="panel provider-panel"><SectionHeader title="Nhà cung cấp thanh toán" description="Các provider backend cho phép sử dụng trong môi trường hiện tại." />{settings.availablePaymentProviders?.length ? <ul className="provider-list">{settings.availablePaymentProviders.map((provider) => <li key={provider}>{provider}</li>)}</ul> : <p className="muted">Chưa có nhà cung cấp thanh toán khả dụng.</p>}</section>
      <section className="panel provider-panel"><SectionHeader title="Nhà cung cấp vận chuyển" description="Các provider backend cho phép sử dụng trong môi trường hiện tại." />{settings.availableShippingProviders?.length ? <ul className="provider-list">{settings.availableShippingProviders.map((provider) => <li key={provider}>{provider}</li>)}</ul> : <p className="muted">Chưa có nhà cung cấp vận chuyển khả dụng.</p>}</section>
      <section className="panel storage-panel"><Boxes aria-hidden="true" /><div><h2>Lưu trữ đối tượng</h2><p>Provider hiệu lực</p></div><strong>{settings.objectStorageProvider || "—"}</strong></section>
    </div>
  );
}

function SettingMetric({ icon: Icon, label, value }: { icon: React.ComponentType<{ "aria-hidden"?: boolean }>; label: string; value: string }) {
  return <article><Icon aria-hidden={true} /><span>{label}</span><strong>{value}</strong></article>;
}
