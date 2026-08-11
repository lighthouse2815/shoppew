import { noIndexMetadata } from "../seo";

export const metadata = noIndexMetadata(
  "Giỏ hàng",
  "Giỏ hàng riêng của khách hàng shoppew.",
);

export default function CartLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return children;
}
