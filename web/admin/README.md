# shoppew Admin

Ứng dụng quản trị marketplace dùng Vite, React, TypeScript và TanStack Query tại `http://localhost:3002`. Giao diện hiển thị tiếng Việt, VND và thời gian kinh doanh `Asia/Ho_Chi_Minh`; dữ liệu và quyền quyết định đến từ backend `/api/v1`.

## Chạy cục bộ

Khởi động backend trước, tạo lại TypeScript contract từ OpenAPI, rồi chạy Admin:

```powershell
corepack pnpm --filter @shoppew/api-client generate
corepack pnpm --filter @shoppew/admin dev
```

Backend mặc định ở `http://localhost:28080`; có thể đổi bằng `VITE_API_URL` trong `.env`. Admin chạy ở `http://localhost:3002`.

## Phân quyền

- `ADMIN` và `SUPER_ADMIN` dùng đầy đủ analytics, người dùng/seller/shop, catalog, đơn hàng/thanh toán, hoàn tiền/tranh chấp, voucher/promotion, kiểm duyệt review và audit log.
- `MODERATOR` chỉ vào phần kiểm duyệt sản phẩm và review, đúng với quyền backend.
- Backend kiểm tra role và object ownership cho từng API; ẩn liên kết ở UI không được xem là kiểm soát truy cập.
- Access token chỉ ở bộ nhớ của tab. Refresh token là cookie `HttpOnly` được backend xoay vòng; client retry tối đa một lần sau `401` và không lưu token trong `localStorage`/`sessionStorage`.

## Bề mặt vận hành

Các route chính:

- `/dashboard`: GMV-like, đơn hoàn tất, người dùng mới, shop hoạt động, hàng chờ kiểm duyệt và giá trị hoàn tiền từ dữ liệu thật.
- `/users`, `/sellers`, `/shops`: tìm kiếm, xem chi tiết, trạng thái và thao tác quản trị được audit.
- `/products`, `/categories`, `/brands`: kiểm duyệt sản phẩm và quản lý taxonomy/brand/thuộc tính.
- `/orders`, `/payments`, `/refunds`, `/disputes`: đọc snapshot/lịch sử và thực hiện các command hợp lệ; xử lý refund dùng `Idempotency-Key`.
- `/vouchers`, `/promotions`, `/reviews`: chiến dịch cấp nền tảng và kiểm duyệt review.
- `/audit-logs`: timeline hành động quan trọng có actor, resource, request và thời gian.
- `/settings`: **chỉ đọc** cấu hình hiệu lực từ `GET /api/v1/admin/settings` gồm locale, tiền tệ, múi giờ, provider đang có, kiểu object storage và giới hạn upload. Hiện không có API hay form ghi cấu hình hệ thống.

Các alias `/catalog`, `/campaigns` và `/audit` vẫn ánh xạ tới trang tương ứng. Mọi metric, hàng đợi và bản ghi đều đến từ API; ứng dụng không tạo dữ liệu giả.

## Kiểm tra

```powershell
corepack pnpm --filter @shoppew/admin lint
corepack pnpm --filter @shoppew/admin typecheck
corepack pnpm --filter @shoppew/admin test
corepack pnpm --filter @shoppew/admin build
```

Bằng chứng hiện tại ngày 2026-08-11:

- ESLint và TypeScript hoàn tất không lỗi; Vitest qua 18/18 test; production build hoàn tất với JS 400.04 kB và CSS 34.85 kB trước gzip.
- Browser QA đăng nhập bằng admin thật, mở đủ 16 route chính không có page alert hay console error, đọc dashboard live, tìm/xem chi tiết người dùng và xem snapshot/lịch sử đơn hàng.
- Một tài khoản buyer synthetic cũ được tạm đình chỉ rồi khôi phục ngay qua UI; `USER_SUSPENDED` và `USER_RESTORED` xuất hiện trong audit log.
- Ở viewport 390 x 844, drawer điều hướng mở đúng vai trò dialog, đóng bằng Escape và trả focus về nút mở.
