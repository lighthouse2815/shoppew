import type { Metadata } from "next";
import "@shoppew/ui/tokens.css";
import "./globals.css";
import { Providers } from "@/components/providers";
import { SiteHeader } from "@/components/site-header";
import { SiteFooter } from "@/components/site-footer";
import { getMetadataBase } from "./seo";

export const metadata: Metadata = {
  metadataBase: getMetadataBase(),
  applicationName: "shoppew",
  title: { default: "shoppew — Marketplace đa nhà bán", template: "%s · shoppew" },
  description: "Khám phá sản phẩm từ nhiều nhà bán trên shoppew với thông tin giá, phân loại và đơn hàng rõ ràng.",
  openGraph: {
    siteName: "shoppew",
    locale: "vi_VN",
    type: "website",
  },
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="vi" data-scroll-behavior="smooth">
      <body>
        <Providers>
          <SiteHeader />
          <div className="page-frame">{children}</div>
          <SiteFooter />
        </Providers>
      </body>
    </html>
  );
}
