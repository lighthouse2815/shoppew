import { noIndexMetadata } from "../seo";

export const metadata = noIndexMetadata(
  "Kết quả đặt hàng",
  "Kết quả checkout riêng của khách hàng shoppew.",
);

export default function OrderLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return children;
}
