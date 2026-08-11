import { noIndexMetadata } from "../seo";

export const metadata = noIndexMetadata(
  "Thanh toán",
  "Luồng thanh toán riêng của khách hàng shoppew.",
);

export default function CheckoutLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return children;
}
