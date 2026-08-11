import Link from "next/link";
import { EmptyState } from "@shoppew/ui";

export default function NotFound() {
  return <main className="shell page-section"><EmptyState title="Không tìm thấy trang" description="Nội dung có thể đã được di chuyển hoặc không còn tồn tại." action={<Link className="sp-button" href="/">Về trang chủ</Link>} /></main>;
}
