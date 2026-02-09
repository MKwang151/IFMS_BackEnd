# 4. DANH SÁCH MODULES & ENTITIES (DATABASE SCHEMA)

Danh sách này bao gồm đầy đủ các yêu cầu: Cloudinary, Phân quyền động (Dynamic RBAC), Thông tin Ngân hàng, Project Lifecycle, Kỳ lương và các logic kiểm toán.

## 1. MODULE QUẢN LÝ FILE (File Storage - Cloudinary)
*Dùng để lưu trữ URL và ID của ảnh/tài liệu, tránh lưu trực tiếp vào các bảng nghiệp vụ.*

### `file_storages`
* **id** (PK, BigInt, Auto-increment)
* **file_name** (Varchar): Tên file gốc.
* **cloudinary_public_id** (Varchar, Unique): ID định danh trên Cloudinary.
* **url** (Text): Đường dẫn truy cập file.
* **file_type** (Varchar): Loại file (MIME type).
* **size** (BigInt): Kích thước file (bytes).
* **created_at** (Timestamp): Thời gian tạo.

---

## 2. MODULE ĐỊNH DANH & PHÂN QUYỀN (IAM & RBAC)
*Quản lý người dùng và cơ chế phân quyền động.*

### `roles` (Bảng vai trò)
* **id** (PK, BigInt, Auto-increment)
* **name** (Varchar, Unique): Tên role (VD: MANAGER, ADMIN).
* **description** (Varchar): Mô tả vai trò.

### `role_permissions` (Bảng trung gian gán quyền cho Role)
* **role_id** (PK, FK): Liên kết bảng `roles`.
* **permission** (PK, Varchar/Enum): Mã quyền (VD: WALLET_WITHDRAW).

### `users` (Tài khoản đăng nhập)
* **id** (PK, BigInt, Auto-increment)
* **email** (Varchar, Unique): Email đăng nhập.
* **password** (Varchar): Mật khẩu mã hóa BCrypt.
* **full_name** (Varchar): Tên hiển thị.
* **is_first_login** (Boolean): Cờ đánh dấu chưa đổi mật khẩu lần đầu.
* **role_id** (FK): Liên kết bảng `roles`.
* **department_id** (FK): Liên kết bảng `departments`.
* **status** (Enum): ACTIVE, LOCKED, PENDING.

### `user_profiles` (Hồ sơ cá nhân & Ngân hàng)
* **user_id** (PK, FK): Khóa chính chung với bảng `users`.
* **phone_number** (Varchar): Số điện thoại.
* **date_of_birth** (Date): Ngày sinh.
* **citizen_id** (Varchar): Số CCCD.
* **avatar_file_id** (FK): Liên kết bảng `file_storages`.
* **bank_name** (Varchar): Tên ngân hàng (VD: MBBank).
* **bank_account_num** (Varchar): Số tài khoản ngân hàng.
* **bank_account_owner** (Varchar): Tên chủ tài khoản.

### `user_security_settings` (Bảo mật giao dịch)
* **user_id** (PK, FK): Khóa chính chung với bảng `users`.
* **transaction_pin** (Varchar): Mã PIN 5 số (Hash).
* **retry_count** (Int): Số lần nhập sai PIN liên tiếp.
* **locked_until** (Timestamp): Thời gian mở khóa PIN.

---

## 3. MODULE TỔ CHỨC (Organization)
*Quản lý phòng ban và hạn mức cấp vốn.*

### `departments`
* **id** (PK, BigInt, Auto-increment)
* **name** (Varchar): Tên phòng ban.
* **code** (Varchar, Unique): Mã phòng ban.
* **manager_id** (FK): Liên kết bảng `users` (Trưởng phòng).
* **budget_quota** (Decimal): Tổng hạn mức Admin cấp cho phòng.
* **available_balance** (Decimal): Số tiền khả dụng hiện tại của phòng.

---

## 4. MODULE DỰ ÁN (Project Lifecycle)
*Quản lý dự án theo tiến độ và giai đoạn.*

### `projects`
* **id** (PK, BigInt, Auto-increment)
* **name** (Varchar): Tên dự án.
* **department_id** (FK): Liên kết bảng `departments`.
* **manager_id** (FK): Liên kết bảng `users` (PM).
* **total_budget** (Decimal): Tổng ngân sách dự kiến.
* **total_spent** (Decimal): Tổng tiền thực chi (Cập nhật tự động).
* **status** (Enum): PLANNING, ACTIVE, PAUSED, CLOSED.
* **current_phase_id** (FK): Liên kết bảng `project_phases` (Phase đang chạy).

### `project_phases` (Lịch sử Giai đoạn)
* **id** (PK, BigInt, Auto-increment)
* **project_id** (FK): Liên kết bảng `projects`.
* **name** (Varchar): Tên giai đoạn.
* **budget_limit** (Decimal): Hạn mức vốn cấp riêng cho Phase này.
* **current_spent** (Decimal): Số tiền đã chi trong Phase này.
* **status** (Enum): ACTIVE, CLOSED.
* **start_date** (Date): Ngày bắt đầu.
* **end_date** (Date): Ngày kết thúc.

### `project_members` (Thành viên dự án)
* **project_id** (PK, FK): Liên kết bảng `projects`.
* **user_id** (PK, FK): Liên kết bảng `users`.
* **role** (Varchar): Vai trò trong dự án (Dev, Tester...).
* **joined_at** (Timestamp): Ngày tham gia.

---

## 5. MODULE YÊU CẦU (Request Flow)
*Xử lý nghiệp vụ xin tiền, duyệt tiền và upload chứng từ.*

### `requests`
* **id** (PK, BigInt, Auto-increment)
* **requester_id** (FK): Người tạo (User).
* **project_id** (FK): Dự án liên quan.
* **phase_id** (FK): Giai đoạn liên quan (để tính cost cho Phase).
* **type** (Enum): ADVANCE (Ứng), EXPENSE (Chi), REIMBURSE (Hoàn ứng).
* **amount** (Decimal): Số tiền yêu cầu.
* **approved_amount** (Decimal): Số tiền được duyệt.
* **proof_file_id** (FK): Liên kết bảng `file_storages` (Ảnh chứng từ).
* **status** (Enum): PENDING_MANAGER, PENDING_ADMIN, APPROVED, PAID, REJECTED.
* **reject_reason** (Text): Lý do từ chối.

### `request_histories` (Nhật ký duyệt / Audit Log)
* **id** (PK, BigInt, Auto-increment)
* **request_id** (FK): Liên kết bảng `requests`.
* **actor_id** (FK): Người thao tác (Manager/Admin).
* **action** (Enum): APPROVE, REJECT, ESCALATE.
* **comment** (Text): Ghi chú kèm theo.
* **created_at** (Timestamp): Thời gian thao tác.

---

## 6. MODULE VÍ ĐIỆN TỬ (Core Wallet)
*Quản lý số dư và lịch sử giao dịch.*

### `wallets`
* **id** (PK, BigInt, Auto-increment)
* **user_id** (FK, Unique): Chủ ví.
* **balance** (Decimal): Tiền thực (Khả dụng).
* **pending_balance** (Decimal): Tiền treo (Đang chờ rút).
* **debt_balance** (Decimal): Dư nợ tạm ứng.
* **version** (BigInt): Optimistic Locking.

### `transactions`
* **id** (PK, BigInt, Auto-increment)
* **wallet_id** (FK): Ví phát sinh.
* **amount** (Decimal): Số tiền (+/-).
* **type** (Enum): DEPOSIT, WITHDRAW, EXPENSE, SALARY, DEBT.
* **status** (Enum): SUCCESS, PENDING, FAILED.
* **ref_request_id** (FK): Liên kết bảng `requests` (Nullable).
* **description** (Text): Nội dung giao dịch.
* **created_at** (Timestamp): Thời gian tạo.

---

## 7. MODULE KẾ TOÁN & LƯƠNG (Accounting)
*Quản lý bảng lương và quỹ hệ thống.*

### `payroll_periods` (Kỳ lương)
* **id** (PK, BigInt, Auto-increment)
* **name** (Varchar): Tên kỳ lương (VD: Lương T10/2025).
* **month** (Int): Tháng.
* **year** (Int): Năm.
* **start_date** (Date): Ngày bắt đầu tính công.
* **end_date** (Date): Ngày chốt công.
* **status** (Enum): DRAFT, PROCESSING, COMPLETED.

### `payslips` (Phiếu lương chi tiết)
* **id** (PK, BigInt, Auto-increment)
* **period_id** (FK): Liên kết bảng `payroll_periods`.
* **user_id** (FK): Liên kết bảng `users`.
* **base_salary** (Decimal): Lương cứng.
* **bonus** (Decimal): Thưởng.
* **deduction** (Decimal): Các khoản giảm trừ khác.
* **advance_deduct** (Decimal): Trừ nợ tạm ứng (Snapshot lịch sử).
* **final_net_salary** (Decimal): Thực lĩnh (Chuyển vào ví).
* **status** (Enum): DRAFT, PAID.

### `system_funds` (Quỹ hệ thống/Mock Bank)
* **id** (PK, BigInt): Thường là 1.
* **total_balance** (Decimal): Tiền mặt thực tế của công ty.
* **bank_account** (Varchar): Số tài khoản ngân hàng công ty.
* **bank_name** (Varchar): Tên ngân hàng.

---

## 8. MODULE CẤU HÌNH (Config)
*Lưu các tham số hệ thống.*

### `system_configs`
* **key** (Varchar, PK): Tên cấu hình (VD: PIN_MAX_RETRY).
* **value** (Varchar): Giá trị.
* **description** (Text): Mô tả ý nghĩa.

## 9. MODULE THÔNG BÁO (Notification)
*Lưu trữ lịch sử thông báo để hiển thị lại trên giao diện (Persistence for WebSocket), đảm bảo user không mất tin khi offline.*

### `notifications`
* **id** (PK, BigInt, Auto-increment)
* **user_id** (FK): Liên kết bảng `users` (Người nhận thông báo).
* **title** (Varchar): Tiêu đề ngắn gọn (VD: "Lương tháng 10").
* **message** (Text): Nội dung chi tiết.
* **type** (Enum): SYSTEM, REQUEST_APPROVED, REQUEST_REJECTED, SALARY_PAID, WARN.
* **ref_id** (BigInt): ID tham chiếu của đối tượng liên quan (VD: ID của Request).
* **ref_type** (Varchar): Loại đối tượng tham chiếu (VD: "REQUEST", "PAYSLIP", "PROJECT").
* **is_read** (Boolean): Trạng thái đã xem (Default: `FALSE`).
* **created_at** (Timestamp): Thời gian tạo.