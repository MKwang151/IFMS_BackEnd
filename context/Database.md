# 4. DANH SÁCH MODULES & ENTITIES (DATABASE SCHEMA)

Danh sách này bao gồm đầy đủ các yêu cầu: Cloudinary, Phân quyền động (Dynamic RBAC), Thông tin Ngân hàng, Project Lifecycle, Expense Categories, Kỳ lương và các logic kiểm toán.

> **📐 Kiến trúc tham chiếu:** Toàn bộ schema tuân thủ mô hình **Ngân sách Phân quyền (Decentralized Budget Management)** được mô tả trong `Request_architecture.md`.

## ⚠️ IMPLEMENTATION NOTE - Business Codes Auto-Generation

Các trường mã định danh (Business Codes) như `project_code`, `phase_code`, `request_code`, `transaction_code`, `period_code`, và `payslip_code` **PHẢI được auto-generated tại lớp Application Layer (Backend Service)** trước khi lưu vào database, chứ không được delegate cho database triggers. Nguyên do:
- Đảm bảo tính nhất quán của format mã trên toàn hệ thống
- Dễ dàng cho Unit Testing và Mock Data
- Tránh phụ thuộc vào database vendor-specific features
- Các trường này đều có constraints `UNIQUE` và `NOT NULL`, nên phải đảm bảo giá trị hợp lệ trước persistence.

## ⚠️ IMPLEMENTATION NOTE - Role Hierarchy (5 cấp) & Nguyên tắc Ủy quyền Tuyệt đối

Hệ thống sử dụng **5 vai trò** theo mô hình Ngân sách Phân quyền:
1. **ADMIN** — Cấp Quota cho Phòng ban (System Fund → Department Fund)
2. **MANAGER** — Phân bổ vốn cho Dự án (Department Fund → Project Fund)
3. **TEAM_LEADER** — Quản lý nội bộ Dự án (chia Phase/Category budget, duyệt chi tiêu Member)
4. **EMPLOYEE** — Tạo Request chi tiêu trong dự án được assign
5. **ACCOUNTANT** — Kiểm tra chứng từ & Giải ngân (Back-office độc lập)

### 🚫 KHÔNG CÓ CƠ CHẾ LEO THANG (NO Escalation)
- **KHÔNG** có biến `MANAGER_LIMIT`, `TIER1_LIMIT`, `TIER2_LIMIT`.
- **KHÔNG** có nhiều trạng thái pending theo role. Chỉ dùng **DUY NHẤT** `PENDING_APPROVAL`.
- **KHÔNG** có hành động `ESCALATE`.
- Mỗi luồng chỉ có **DUY NHẤT 1 cấp duyệt nghiệp vụ**.

### ✅ Nguyên tắc: "Ai quản lý quỹ nào → toàn quyền duyệt đơn rút tiền từ quỹ đó"
| Quỹ | Người quản lý | Duyệt đơn |
|:----|:-------------|:-----------|
| Category Budget (Phase) | **Team Leader** | MỌI đơn chi tiêu Member, bất kể số tiền |
| Department Fund | **Manager** | MỌI đơn xin cấp vốn dự án của Team Leader |
| System Fund | **Admin** | MỌI đơn xin cấp vốn phòng ban của Manager |

### 🛡️ Chốt chặn an toàn = Số dư khả dụng (Balance Limit)
- Hệ thống tự động **CHẶN tạo đơn** nếu `amount > available_balance` của quỹ tương ứng.
- VD: Category còn 3 triệu → Member không thể tạo đơn 5 triệu.

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
*Quản lý người dùng và cơ chế phân quyền động. Hệ thống sử dụng Dynamic RBAC — Roles lưu trong DB, Permissions định nghĩa cứng trong source code (Enum).*

> **5 Default Roles:** `ADMIN`, `MANAGER`, `TEAM_LEADER` 🟢, `EMPLOYEE`, `ACCOUNTANT`
>
> **3 Permissions MỚI cho Team Leader:**
> - `REQUEST_APPROVE_TEAM_LEADER` — Duyệt MỌI yêu cầu chi tiêu của Member trong dự án (không giới hạn số tiền)
> - `PROJECT_CATEGORY_MANAGE` — Quản lý danh mục chi tiêu dự án (gán Category cho Phase)
> - `PROJECT_BUDGET_ALLOCATE` — Phân bổ ngân sách Phase/Category trong dự án
>
> **Permissions XÓA BỎ (do hủy leo thang):**
> - ~~`REQUEST_APPROVE_TIER1`~~ → Thay bằng `REQUEST_APPROVE_TEAM_LEADER` (Team Leader) và `REQUEST_APPROVE_PROJECT_TOPUP` (Manager)
> - ~~`REQUEST_APPROVE_TIER2`~~ → Thay bằng `REQUEST_APPROVE_DEPT_TOPUP` (Admin)

#### Permission Matrix (Default Roles)

| Permission | EMPLOYEE | TEAM_LEADER | MANAGER | ACCOUNTANT | ADMIN |
|:---|:---:|:---:|:---:|:---:|:---:|
| USER_PROFILE_VIEW | ✅ | ✅ | ✅ | ✅ | ✅ |
| USER_PROFILE_UPDATE | ✅ | ✅ | ✅ | ✅ | ✅ |
| USER_PIN_UPDATE | ✅ | ✅ | ✅ | ✅ | ✅ |
| NOTIFICATION_VIEW | ✅ | ✅ | ✅ | ✅ | ✅ |
| WALLET_VIEW_SELF | ✅ | ✅ | ✅ | ✅ | ✅ |
| WALLET_DEPOSIT | ✅ | ✅ | ✅ | ✅ | ✅ |
| WALLET_WITHDRAW | ✅ | ✅ | ✅ | ✅ | ✅ |
| WALLET_TRANSACTION_VIEW | ✅ | ✅ | ✅ | ✅ | ✅ |
| PROJECT_VIEW_ACTIVE | ✅ | ✅ | ✅ | ✅ | ✅ |
| REQUEST_CREATE | ✅ | ✅ | ✅ | ✅ | ✅ |
| REQUEST_VIEW_SELF | ✅ | ✅ | ✅ | ✅ | ✅ |
| PAYROLL_VIEW_SELF | ✅ | ✅ | ✅ | ✅ | ✅ |
| PAYROLL_DOWNLOAD | ✅ | ✅ | ✅ | ✅ | ✅ |
| **REQUEST_APPROVE_TEAM_LEADER** 🟢 | | ✅ | | | ✅ |
| **PROJECT_CATEGORY_MANAGE** 🟢 | | ✅ | | | ✅ |
| **PROJECT_BUDGET_ALLOCATE** 🟢 | | ✅ | | | ✅ |
| **PROJECT_PHASE_MANAGE** | | ✅ | | | ✅ |
| **PROJECT_MEMBER_MANAGE** | | ✅ | | | ✅ |
| PROJECT_CREATE | | | ✅ | | ✅ |
| PROJECT_UPDATE | | | ✅ | | ✅ |
| PROJECT_STATUS_MANAGE | | | ✅ | | ✅ |
| **PROJECT_ASSIGN_LEADER** 🟢 | | | ✅ | | ✅ |
| REQUEST_VIEW_DEPT | | | ✅ | | ✅ |
| **REQUEST_APPROVE_PROJECT_TOPUP** 🟢 | | | ✅ | | ✅ |
| REQUEST_REJECT | | | ✅ | | ✅ |
| DEPT_VIEW_DASHBOARD | | | ✅ | | ✅ |
| REQUEST_VIEW_APPROVED | | | | ✅ | ✅ |
| REQUEST_PAYOUT | | | | ✅ | ✅ |
| TRANSACTION_APPROVE_WITHDRAW | | | | ✅ | ✅ |
| PAYROLL_MANAGE | | | | ✅ | ✅ |
| PAYROLL_EXECUTE | | | | ✅ | ✅ |
| SYSTEM_FUND_VIEW | | | | ✅ | ✅ |
| SYSTEM_FUND_TOPUP | | | | ✅ | ✅ |
| PROJECT_VIEW_ALL | | | | ✅ | ✅ |
| USER_VIEW_LIST | | | | | ✅ |
| USER_CREATE | | | | | ✅ |
| USER_UPDATE | | | | | ✅ |
| USER_LOCK | | | | | ✅ |
| ROLE_MANAGE | | | | | ✅ |
| **REQUEST_APPROVE_DEPT_TOPUP** 🟢 | | | | | ✅ |
| REQUEST_VIEW_ALL | | | | | ✅ |
| DEPT_MANAGE | | | | | ✅ |
| DEPT_BUDGET_ALLOCATE | | | | | ✅ |
| SYSTEM_CONFIG_MANAGE | | | | | ✅ |
| DASHBOARD_VIEW_GLOBAL | | | | | ✅ |
| AUDIT_LOG_VIEW | | | | | ✅ |

### `roles` (Bảng vai trò)
* **id** (PK, BigInt, Auto-increment)
* **name** (Varchar, Unique): Tên role (VD: MANAGER, ADMIN, TEAM_LEADER).
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
* **enabled** (Boolean): Spring Security - Tài khoản có được kích hoạt không.
* **account_non_expired** (Boolean): Spring Security - Tài khoản chưa hết hạn.
* **account_non_locked** (Boolean): Spring Security - Tài khoản không bị khóa.
* **credentials_non_expired** (Boolean): Spring Security - Mật khẩu chưa hết hạn.

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

### `jwt_tokens` (Quản lý JWT Token)
*Bảng kỹ thuật để quản lý vòng đời JWT token (Access/Refresh Token), hỗ trợ revoke token và blacklist.*
* **id** (PK, BigInt, Auto-increment)
* **token** (Varchar(1000), Not Null): Chuỗi JWT token.
* **user_id** (FK, BigInt, Not Null): Liên kết bảng `users` - Chủ sở hữu token.
* **token_type** (Varchar(20), Not Null): Loại token (ACCESS, REFRESH).
* **expiry_date** (Timestamp, Not Null): Thời gian hết hạn của token.
* **revoked** (Boolean, Not Null): Trạng thái token có bị thu hồi không (Default: FALSE).
* **revoked_at** (Timestamp, Nullable): Thời gian token bị thu hồi.
* **created_at** (Timestamp, Not Null): Thời gian token được tạo.

---

## 3. MODULE TỔ CHỨC (Organization)
*Quản lý phòng ban và hạn mức cấp vốn. Bảng `departments` đóng vai trò là **Department Fund** trong kiến trúc 4 tầng Fund (xem `Request_architecture.md` Mục 4).*

> **Dòng tiền:** System Fund → `departments.total_available_balance` (TĂNG khi Admin duyệt QUOTA_TOPUP) → `projects.available_budget` (TĂNG khi Manager duyệt PROJECT_TOPUP, đồng thời departments GIẢM)

### `departments`
* **id** (PK, BigInt, Auto-increment)
* **name** (Varchar): Tên phòng ban.
* **code** (Varchar, Unique): Mã phòng ban.
* **manager_id** (FK): Liên kết bảng `users` (Trưởng phòng).
* **total_project_quota** (Decimal, DEFAULT 0): Tổng ngân sách đã nhận từ Admin (chỉ TĂNG khi Admin duyệt QUOTA_TOPUP).
* **total_available_balance** (Decimal, DEFAULT 0): Ngân sách còn lại khả dụng. TĂNG khi nhận top-up, GIẢM khi Manager phân bổ cho Project.
---

## 4. MODULE DỰ ÁN (Project Lifecycle)
*Quản lý dự án theo tiến độ và giai đoạn.*

### `projects`
* **id** (PK, BigInt, Auto-increment)
* **project_code** (Varchar, Unique, Not Null): Mã định danh dự án (Cost Center). Format: `PRJ-ERP-2026`. Auto-generated at application layer.
* **name** (Varchar): Tên dự án.
* **description** (Text, Nullable): Mô tả chi tiết dự án.
* **department_id** (FK): Liên kết bảng `departments`.
* **manager_id** (FK): Liên kết bảng `users` (PM / Team Leader quản lý dự án).
* **total_budget** (Decimal): Tổng ngân sách dự kiến (Kế hoạch).
* **available_budget** (Decimal, DEFAULT 0): 🟢 **[MỚI]** Ngân sách khả dụng thực tế (Project Fund). TĂNG khi Manager duyệt PROJECT_TOPUP, GIẢM khi Accountant giải ngân. Luôn >= 0.
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
* **project_role** (Enum, Not Null): Cấp bậc phân quyền logic trong dự án. Values: `LEADER` (Team Leader — do Manager chỉ định khi tạo project, toàn quyền duyệt/chia budget/quản lý member), `MEMBER` (Thành viên — do Team Leader thêm vào, chỉ tạo Request).
* **position** (Varchar, Nullable): Chức danh hiển thị tự do (VD: "Backend Dev", "Tester", "BA", "AI Engineer"). Không ảnh hưởng logic phân quyền — chỉ dùng cho UI/Report. Team Leader gán khi thêm member.
* **joined_at** (Timestamp): Ngày tham gia.

### `expense_categories` (Danh mục chi tiêu)
*Định nghĩa các nhóm chi phí dùng để phân loại mục đích sử dụng tiền trong dự án. Team Leader thiết lập và gán budget cho từng Category per Phase. Xem `Request_architecture.md` Mục 5 để hiểu chi tiết.*

* **id** (PK, BigInt, Auto-increment)
* **name** (Varchar, Unique): Tên danh mục (VD: Travel & Accommodation, Equipment & Software).
* **description** (Text, Nullable): Mô tả chi tiết danh mục.
* **is_system_default** (Boolean, DEFAULT FALSE): Danh mục mặc định hệ thống (không thể xóa).
* **created_at** (Timestamp): Thời gian tạo.
* **updated_at** (Timestamp): Thời gian cập nhật.
* **created_by** (BigInt, Nullable): Người tạo.
* **updated_by** (BigInt, Nullable): Người cập nhật.

### `phase_category_budgets` (Ngân sách Danh mục theo Phase)
*Bảng trung gian Many-to-Many giữa `project_phases` và `expense_categories`. Team Leader thiết lập trần chi tiêu cho từng Category trong mỗi Phase.*

* **phase_id** (PK, FK): Liên kết bảng `project_phases`.
* **category_id** (PK, FK): Liên kết bảng `expense_categories`.
* **budget_limit** (Decimal): Hạn mức tối đa cho danh mục này trong Phase.
* **current_spent** (Decimal, DEFAULT 0): Số tiền đã chi trong danh mục này (cập nhật tự động khi giải ngân).

---

## 5. MODULE YÊU CẦU (Request Flow)
*Xử lý nghiệp vụ xin tiền, duyệt tiền và upload chứng từ. Mỗi luồng chỉ có DUY NHẤT 1 cấp duyệt nghiệp vụ — KHÔNG leo thang, KHÔNG vượt cấp.*

> **Luồng 1 — Chi tiêu cá nhân (ADVANCE/EXPENSE/REIMBURSE):** Member → Team Leader duyệt → Accountant giải ngân. Manager/Admin KHÔNG can thiệp.
> **Luồng 2 — Xin cấp vốn Dự án (PROJECT_TOPUP):** Team Leader → Manager duyệt → Hệ thống tự cập nhật Project Fund.
> **Luồng 3 — Xin cấp vốn Phòng ban (QUOTA_TOPUP):** Manager → Admin duyệt → Hệ thống tự cập nhật Dept Fund.

### `requests`
* **id** (PK, BigInt, Auto-increment)
* **request_code** (Varchar, Unique, Not Null): Mã đơn từ / Tờ trình. Format: `REQ-IT-2602-001`. Auto-generated at application layer.
* **requester_id** (FK): Người tạo (User).
* **project_id** (FK, Nullable): Dự án liên quan. **BẮT BUỘC** cho ADVANCE/EXPENSE/REIMBURSE/PROJECT_TOPUP. **NULL** cho QUOTA_TOPUP.
* **phase_id** (FK, Nullable): Giai đoạn liên quan. **BẮT BUỘC** cho ADVANCE/EXPENSE/REIMBURSE. **NULL** cho PROJECT_TOPUP/QUOTA_TOPUP.
* **category_id** (FK, Nullable): Liên kết bảng `expense_categories`. **BẮT BUỘC** cho ADVANCE/EXPENSE/REIMBURSE. **NULL** cho PROJECT_TOPUP/QUOTA_TOPUP.
* **type** (Enum): `ADVANCE` (Ứng), `EXPENSE` (Chi), `REIMBURSE` (Hoàn ứng), `PROJECT_TOPUP` (Xin cấp vốn Dự án), `QUOTA_TOPUP` (Xin cấp vốn Phòng ban).
* **amount** (Decimal): Số tiền yêu cầu. **Chốt chặn:** phải ≤ số dư khả dụng của quỹ tương ứng tại thời điểm tạo đơn.
* **approved_amount** (Decimal): Số tiền được duyệt (có thể ≤ amount).
* **status** (Enum): `PENDING_APPROVAL`, `PENDING_ACCOUNTANT`, `APPROVED`, `PAID`, `REJECTED`, `CANCELLED`.
* **reject_reason** (Text): Lý do từ chối.
* **description** (Text): Mô tả chi tiết lý do chi/ứng tiền.

> **Giải thích Status Enum (Đã đơn giản hóa — KHÔNG CÒN LEO THANG):**
> - `PENDING_APPROVAL`: Trạng thái chung — chờ **DUY NHẤT 1 cấp duyệt** tùy theo `type`:
>   - ADVANCE/EXPENSE/REIMBURSE → chờ **Team Leader**
>   - PROJECT_TOPUP → chờ **Manager**
>   - QUOTA_TOPUP → chờ **Admin**
> - `PENDING_ACCOUNTANT`: Chỉ dùng cho Luồng 1 — sau khi Team Leader duyệt, chờ Kế toán kiểm tra chứng từ & giải ngân.
> - `APPROVED`: Đã duyệt nghiệp vụ (Luồng 2 & 3 chuyển thẳng sang đây rồi auto → PAID).
> - `PAID`: Đã giải ngân / đã cấp vốn xong.
> - `REJECTED`: Bị từ chối bởi cấp duyệt hoặc Accountant.
> - `CANCELLED`: Người tạo tự hủy (chỉ được hủy khi đang `PENDING_APPROVAL`).

### `request_attachments` (Bảng trung gian)
*Lưu trữ mối quan hệ 1-N: Một Request có thể có nhiều File đính kèm (Hóa đơn, PDF, Excel...).*
* **request_id** (PK, FK): Liên kết bảng `requests`.
* **file_id** (PK, FK): Liên kết bảng `file_storages`.

### `request_histories` (Nhật ký duyệt / Audit Log)
* **id** (PK, BigInt, Auto-increment)
* **request_id** (FK): Liên kết bảng `requests`.
* **actor_id** (FK): Người thao tác (Team Leader/Manager/Admin/Accountant).
* **action** (Enum): `APPROVE`, `REJECT`, `PAYOUT` (Accountant giải ngân), `CANCEL` (Người tạo tự hủy).
* **status_after_action** (Enum): Snapshot trạng thái Request SAU khi hành động. Values: `PENDING_APPROVAL`, `PENDING_ACCOUNTANT`, `APPROVED`, `PAID`, `REJECTED`, `CANCELLED`.
* **comment** (Text): Ghi chú kèm theo.
* **created_at** (Timestamp): Thời gian thao tác.

> ⚠️ **KHÔNG CÓ action `ESCALATE`** — đã hủy bỏ hoàn toàn cơ chế leo thang.

### State Transition Diagrams (Request Lifecycle)

#### Luồng 1: Chi tiêu cá nhân (ADVANCE / EXPENSE / REIMBURSE)
```
Member tạo Request (validate: amount ≤ Category Budget remaining)
       │
       ▼
 PENDING_APPROVAL ──── Cancel ────► CANCELLED
   (chờ Team Leader)
       │
       ├── Team Leader APPROVE
       │       │
       │       ▼
       │   PENDING_ACCOUNTANT
       │       │
       │       ├── Accountant PAYOUT ──► PAID ✅
       │       │     (Project Fund → Personal Wallet)
       │       │
       │       └── Accountant REJECT ──► REJECTED ❌
       │             (Chứng từ không hợp lệ)
       │
       └── Team Leader REJECT ──► REJECTED ❌
```
> **Manager / Admin KHÔNG BAO GIỜ xuất hiện trong luồng này.**

#### Luồng 2: Xin cấp vốn Dự án (PROJECT_TOPUP)
```
Team Leader tạo Request (validate: amount ≤ Department available_balance)
       │
       ▼
 PENDING_APPROVAL ──── Cancel ────► CANCELLED
   (chờ Manager)
       │
       ├── Manager APPROVE ──► APPROVED ──► PAID ✅
       │     (Auto: Department Fund -= amount, Project Fund += amount)
       │
       └── Manager REJECT ──► REJECTED ❌
```

#### Luồng 3: Xin cấp vốn Phòng ban (QUOTA_TOPUP)
```
Manager tạo Request (validate: amount ≤ SystemFund total_balance)
       │
       ▼
 PENDING_APPROVAL ──── Cancel ────► CANCELLED
   (chờ Admin)
       │
       ├── Admin APPROVE ──► APPROVED ──► PAID ✅
       │     (Auto: System Fund -= amount, Department Fund += amount)
       │
       └── Admin REJECT ──► REJECTED ❌
```

--- 

## 6. MODULE VÍ ĐIỆN TỬ (Core Wallet)
*Quản lý số dư và lịch sử giao dịch. Bảng `wallets` đại diện cho **Personal Wallet** (Ví Cá nhân) trong kiến trúc 4 tầng Fund.*

> **Kiến trúc 4 tầng Fund + Balance-as-Safety-Gate:**
> | Tầng | Entity/Table | Trường tiền | Chốt chặn cho | Người quản lý |
> |:---:|:---|:---|:---|:---|
> | 1 | `system_funds` | `total_balance` | Luồng 3 (QUOTA_TOPUP) | Admin |
> | 2 | `departments` | `total_available_balance` | Luồng 2 (PROJECT_TOPUP) | Manager |
> | 3 | `projects` | `available_budget` | — | Team Leader |
> | 3.1 | `phase_category_budgets` | `budget_limit - current_spent` | Luồng 1 (chi tiêu) | Team Leader |
> | 4 | `wallets` | `balance` | — (nhận giải ngân) | Employee |
>
> ⚠️ **Không có amount limit — chốt chặn duy nhất là số dư khả dụng của quỹ tương ứng.**

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
* **type** (Enum): `DEPOSIT`, `WITHDRAW`, `REQUEST_PAYMENT`, `PAYSLIP_PAYMENT`, `SYSTEM_ADJUSTMENT`, 🟢 `DEPT_QUOTA_ALLOCATION` (Admin cấp vốn Phòng ban), 🟢 `PROJECT_QUOTA_ALLOCATION` (Manager cấp vốn Dự án). *(Phân loại tính chất dòng tiền).*
* **status** (Enum): `SUCCESS`, `PENDING`, `FAILED`.
* 🟢 **reference_type** (Enum): **[MỚI]** Loại chứng từ gốc sinh ra giao dịch này (VD: `REQUEST`, `PAYSLIP`, 🟢 `PROJECT`, 🟢 `DEPARTMENT`, 🟢 `SYSTEM`). Dùng để phân loại và tra cứu nhanh khi đối soát.
* 🟢 **reference_id** (BigInt): **[MỚI]** ID của chứng từ gốc tương ứng (ID của Request hoặc ID của Payslip).
* 🟢 **related_transaction_id** (FK, BigInt, Nullable): **[MỚI]** Khóa ngoại tự tham chiếu (Self-referencing) trỏ về chính bảng `transactions`. Dùng để liên kết dòng tiền đối ứng trong nguyên lý Bút toán kép (VD: Liên kết dòng tiền ra khỏi Ví Công Ty với dòng tiền vào Ví Nhân viên).
* 🟢 **actor_id** (FK, BigInt, Nullable): **[MỚI]** Khóa ngoại trỏ tới bảng `users`. Ghi nhận ai là người thao tác kích hoạt giao dịch này (Phục vụ truy vết kiểm toán).
* **description** (Text): Nội dung/Lý do diễn giải giao dịch.
* **created_at** (Timestamp): Thời gian giao dịch được tạo ra.
* **updated_at** (Timestamp): Thời gian cập nhật trạng thái cuối cùng.

## 7. MODULE KẾ TOÁN & LƯƠNG (Accounting)
*Quản lý bảng lương và quỹ hệ thống. Hỗ trợ cơ chế **Auto-Netting** (Bù trừ công nợ tự động): khi chi lương, hệ thống tự trừ `wallets.debt_balance` trước khi cộng phần còn lại vào ví nhân viên.*

> **Công thức Auto-Netting:** `Net Salary = (base_salary + bonus + allowance - deduction) - advance_deduct`
> - Nếu `Net Salary > 0`: Trừ hết nợ, chuyển phần còn lại vào Ví → Tạo Transaction `PAYSLIP_PAYMENT`
> - Nếu `Net Salary ≤ 0`: Trừ một phần nợ (bằng đúng lương), nợ còn lại bảo lưu sang kỳ sau

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
* **payment_date** (Timestamp, Nullable): Ngày thực tế chuyển lương vào ví nhân viên.

### `system_funds` (Quỹ hệ thống/Mock Bank)
*Tầng 1 trong kiến trúc 4 tầng Fund. Mọi dòng tiền đều bắt nguồn từ đây: Admin duyệt QUOTA_TOPUP → trừ `total_balance` → cộng vào `departments.total_available_balance`.*
* **id** (PK, BigInt): Thường là 1 (Singleton — hệ thống chỉ có 1 quỹ tổng).
* **total_balance** (Decimal): Tiền mặt thực tế của công ty (giảm khi cấp vốn, tăng khi nạp thêm).
* **bank_account** (Varchar): Số tài khoản ngân hàng công ty.
* **bank_name** (Varchar): Tên ngân hàng.

---

## 8. MODULE CẤU HÌNH (Config)
*Lưu các tham số hệ thống.*

### `system_configs`
* **key** (Varchar, PK): Tên cấu hình (VD: PIN_MAX_RETRY).
* **value** (Varchar): Giá trị.
* **description** (Text): Mô tả ý nghĩa.

---

## 9. MODULE THÔNG BÁO (Notification)
*Lưu trữ lịch sử thông báo để hiển thị lại trên giao diện (Persistence for WebSocket), đảm bảo user không mất tin khi offline.*

### `notifications`
* **id** (PK, BigInt, Auto-increment)
* **user_id** (FK): Liên kết bảng `users` (Người nhận thông báo).
* **title** (Varchar): Tiêu đề ngắn gọn (VD: "Lương tháng 10").
* **message** (Text): Nội dung chi tiết.
* **type** (Enum): `SYSTEM`, `REQUEST_APPROVED`, `REQUEST_REJECTED`, `REQUEST_PENDING_ACCOUNTANT`, `SALARY_PAID`, `QUOTA_APPROVED`, `PROJECT_TOPUP_APPROVED`, `WARN`.
* **ref_id** (BigInt): ID tham chiếu của đối tượng liên quan (VD: ID của Request).
* **ref_type** (Varchar): Loại đối tượng tham chiếu (VD: "REQUEST", "PAYSLIP", "PROJECT").
* **is_read** (Boolean): Trạng thái đã xem (Default: `FALSE`).
* **created_at** (Timestamp): Thời gian tạo.

---

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
