# 4. DANH SÁCH MODULES & ENTITIES (DATABASE SCHEMA)

Danh sách này bao gồm đầy đủ các yêu cầu: Cloudinary, Phân quyền động (Dynamic RBAC), Thông tin Ngân hàng, Project Lifecycle, Kỳ lương và các logic kiểm toán.

## ⚠️ IMPLEMENTATION NOTE - Business Codes Auto-Generation

Các trường mã định danh (Business Codes) như `project_code`, `phase_code`, `request_code`, `transaction_code`, `period_code`, và `payslip_code` **PHẢI được auto-generated tại lớp Application Layer (Backend Service)** trước khi lưu vào database, chứ không được delegate cho database triggers. Nguyên do:
- Đảm bảo tính nhất quán của format mã trên toàn hệ thống
- Dễ dàng cho Unit Testing và Mock Data
- Tránh phụ thuộc vào database vendor-specific features
- Các trường này đều có constraints `UNIQUE` và `NOT NULL`, nên phải đảm bảo giá trị hợp lệ trước persistence.

---

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
* **job_title** (Varchar): Chức danh công việc (VD: Software Engineer).
* **employee_code** (Varchar, Unique): Mã nhân viên.
* **address** (Text): Địa chỉ liên hệ.
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
* **total_project_quota** (Decimal): Tổng ngân sách cấp cho phòng ban.
* **total_available_balance** (Decimal): Ngân sách còn lại (Cập nhật tự động khi duyệt yêu cầu).
---

## 4. MODULE DỰ ÁN (Project Lifecycle)
*Quản lý dự án theo tiến độ và giai đoạn.*

### `projects`
* **id** (PK, BigInt, Auto-increment)
* **project_code** (Varchar, Unique, Not Null): Mã định danh dự án (Cost Center). Format: `PRJ-ERP-2026`. Auto-generated at application layer.
* **name** (Varchar): Tên dự án.
* **department_id** (FK): Liên kết bảng `departments`.
* **manager_id** (FK): Liên kết bảng `users` (PM).
* **total_budget** (Decimal): Tổng ngân sách dự kiến.
* **total_spent** (Decimal): Tổng tiền thực chi (Cập nhật tự động).
* **status** (Enum): PLANNING, ACTIVE, PAUSED, CLOSED.
* **current_phase_id** (FK): Liên kết bảng `project_phases` (Phase đang chạy).

### `project_phases` (Lịch sử Giai đoạn)
* **id** (PK, BigInt, Auto-increment)
* **phase_code** (Varchar, Unique, Not Null): Mã định danh giai đoạn dự án. Format: `PH-UIUX-01`. Auto-generated at application layer.
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
* **request_code** (Varchar, Unique, Not Null): Mã đơn từ / Tờ trình. Mã quan trọng nhất dùng để kế toán đối soát và in PDF. Format: `REQ-IT-2602-001` (Type-Dept-MMYY-Sequence). Auto-generated at application layer.
* **requester_id** (FK): Người tạo (User).
* **project_id** (FK): Dự án liên quan.
* **phase_id** (FK): Giai đoạn liên quan (để tính cost cho Phase).
* **type** (Enum): ADVANCE (Ứng), EXPENSE (Chi), REIMBURSE (Hoàn ứng).
* **amount** (Decimal): Số tiền yêu cầu.
* **approved_amount** (Decimal): Số tiền được duyệt.
* **status** (Enum): PENDING_MANAGER, PENDING_ADMIN, APPROVED, PAID, REJECTED.
* **reject_reason** (Text): Lý do từ chối.
* **description** (Text): Mô tả chi tiết lý do chi/ứng tiền.

### `request_attachments` (Bảng trung gian)
*Lưu trữ mối quan hệ 1-N: Một Request có thể có nhiều File đính kèm (Hóa đơn, PDF, Excel...).*
* **request_id** (PK, FK): Liên kết bảng `requests`.
* **file_id** (PK, FK): Liên kết bảng `file_storages`.

### `request_histories` (Nhật ký duyệt / Audit Log)
* **id** (PK, BigInt, Auto-increment)
* **request_id** (FK): Liên kết bảng `requests`.
* **actor_id** (FK): Người thao tác (Manager/Admin).
* **action** (Enum): APPROVE, REJECT, ESCALATE.
* **request_history_status** (Enum):     PENDING, APPROVED, REJECTED, CANCELED  (Snapshot trạng thái của Request sau khi hành động diễn ra).
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

### `transactions` (Master Ledger)

Bảng Sổ cái tổng (Ledger) lưu trữ toàn bộ lịch sử biến động số dư của tất cả các ví trong hệ thống. Bảng này tuân thủ nghiêm ngặt nguyên tắc **Append-Only** (Chỉ thêm mới, không bao giờ được UPDATE hay DELETE).

* **id** (PK, BigInt, Auto-increment)
* **transaction_code** (Varchar, Unique, Not Null): Mã giao dịch nội bộ của hệ thống (phân biệt với `payment_ref` của cổng thanh toán bên thứ 3). Dùng để tra cứu sao kê. Format: `TXN-8829145A`. Auto-generated at application layer.
* **payment_ref** (Varchar, Nullable): Mã tham chiếu từ cổng thanh toán (VD: `MOMO_100299`, `VNP_...`). *Lưu ý: Không dùng Unique Index vì các giao dịch nội bộ sẽ có giá trị null.*
* **gateway_provider** (Enum): `PAYOS`, `MOMO`, `VNPAY`, `INTERNAL`. (Xác định nguồn hoặc cổng xử lý giao dịch).
* **wallet_id** (FK, BigInt): Khóa ngoại trỏ tới bảng `wallets` (Ví Công ty hoặc Ví Nhân viên bị tác động).
* **amount** (Decimal): Số tiền giao dịch (+/-).
* 🟢 **balance_after** (Decimal, Not Null): **[MỚI]** Bức ảnh chụp chính xác số dư của `wallet_id` ngay sau khi giao dịch thành công. Dùng để xuất sao kê nhanh chóng.
* **type** (Enum): `DEPOSIT`, `WITHDRAW`, `REQUEST_PAYMENT`, `PAYSLIP_PAYMENT`, `SYSTEM_ADJUSTMENT` (Phân loại tính chất dòng tiền).
* **status** (Enum): `SUCCESS`, `PENDING`, `FAILED`.
* 🟢 **reference_type** (Enum): **[MỚI]** Loại chứng từ gốc sinh ra giao dịch này (VD: `REQUEST`, `PAYSLIP`). Dùng để phân loại và tra cứu nhanh khi đối soát.
* 🟢 **reference_id** (BigInt): **[MỚI]** ID của chứng từ gốc tương ứng (ID của Request hoặc ID của Payslip).
* 🟢 **related_transaction_id** (FK, BigInt, Nullable): **[MỚI]** Khóa ngoại tự tham chiếu (Self-referencing) trỏ về chính bảng `transactions`. Dùng để liên kết dòng tiền đối ứng trong nguyên lý Bút toán kép (VD: Liên kết dòng tiền ra khỏi Ví Công Ty với dòng tiền vào Ví Nhân viên).
* 🟢 **actor_id** (FK, BigInt, Nullable): **[MỚI]** Khóa ngoại trỏ tới bảng `users`. Ghi nhận ai là người thao tác kích hoạt giao dịch này (Phục vụ truy vết kiểm toán).
* **description** (Text): Nội dung/Lý do diễn giải giao dịch.
* **created_at** (Timestamp): Thời gian giao dịch được tạo ra.
* **updated_at** (Timestamp): Thời gian cập nhật trạng thái cuối cùng.

## 7. MODULE KẾ TOÁN & LƯƠNG (Accounting)
*Quản lý bảng lương và quỹ hệ thống.*

### `payroll_periods` (Kỳ lương)
* **id** (PK, BigInt, Auto-increment)
* **period_code** (Varchar, Unique, Not Null): Mã kỳ lương. Format: `PR-2026-02`. Auto-generated at application layer.
* **name** (Varchar): Tên kỳ lương (VD: Lương T10/2025).
* **month** (Int): Tháng.
* **year** (Int): Năm.
* **start_date** (Date): Ngày bắt đầu tính công.
* **end_date** (Date): Ngày chốt công.
* **status** (Enum): DRAFT, PROCESSING, COMPLETED.

### `payslips` (Phiếu lương chi tiết)
* **id** (PK, BigInt, Auto-increment)
* **payslip_code** (Varchar, Unique, Not Null): Mã phiếu lương cá nhân. Dùng làm mã chứng từ khi xuất file PDF cho nhân viên. Format: `PSL-EMP001-0226`. Auto-generated at application layer.
* **period_id** (FK): Liên kết bảng `payroll_periods`.
* **user_id** (FK): Liên kết bảng `users`.
* **base_salary** (Decimal): Lương cứng.
* **bonus** (Decimal): Thưởng.
* **allowance** (Decimal): Các khoản phụ cấp.
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

## 10. MODULE KIỂM TOÁN HỆ THỐNG (System Audit Trail)
*Lưu vết toàn bộ các thao tác thay đổi dữ liệu cấu hình, phân quyền, ngân sách và trạng thái hệ thống. Bảng này tuân thủ nghiêm ngặt nguyên tắc Append-Only.*

### `audit_logs`
* **id** (PK, BigInt, Auto-increment)
* **actor_id** (FK, BigInt, Nullable): Liên kết bảng `users`. Người thực hiện thao tác.
* **action** (Enum): Phân loại hành động (VD: `USER_LOCKED`, `ROLE_ASSIGNED`, `QUOTA_TOPUP`).
* **entity_name** (Varchar): Tên bảng hoặc thực thể bị tác động (VD: `departments`, `users`).
* **entity_id** (Varchar): ID của dòng dữ liệu bị tác động.
* **old_values** (JSON, Nullable): Trạng thái dữ liệu TRƯỚC khi thay đổi.
* **new_values** (JSON, Nullable): Trạng thái dữ liệu SAU khi thay đổi.
* **created_at** (Timestamp): Thời gian chính xác hành động xảy ra.