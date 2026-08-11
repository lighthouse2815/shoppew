import Link from "next/link";

export function Pagination({ page, totalPages, href }: { page: number; totalPages: number; href: (page: number) => string }) {
  if (totalPages <= 1) return null;
  return (
    <nav className="pagination" aria-label="Phân trang">
      {page > 0 && <Link href={href(page - 1)}>Trang trước</Link>}
      <span>Trang {page + 1} / {totalPages}</span>
      {page + 1 < totalPages && <Link href={href(page + 1)}>Trang sau</Link>}
    </nav>
  );
}
