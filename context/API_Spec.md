# IFMS – API Specification

> **Base URL:** `https://api.ifms.vn/api/v1`  
> **Auth:** `Authorization: Bearer <accessToken>`  
> **Phân trang:** `?page=1&limit=20` → `{ items, total, page, limit, totalPages }`

### Response Wrapper — `ApiResponse<T>`

Mọi response đều được wrap theo class `ApiResponse<T>`. Cấu trúc chung:

```json
{
  "success": true,
  "message": "Success",
  "data": { … },
  "timestamp": "2026-02-24T10:30:00"
}
```

| Field       | Type            | Mô tả                                                                 |
|-------------|-----------------|------------------------------------------------------------------------|
| `success`   | `boolean`       | `true` nếu thành công, `false` nếu lỗi.                               |
| `message`   | `string`        | `"Success"` mặc định, hoặc message lỗi khi `success = false`.         |
| `data`      | `T \| null`     | Payload chính. `null` khi lỗi hoặc response chỉ có message.           |
| `timestamp` | `LocalDateTime` | Thời điểm server trả response (`yyyy-MM-dd'T'HH:mm:ss`).             |

**Error response:**
```json
{
  "success": false,
  "message": "Email or password is incorrect",
  "data": null,
  "timestamp": "2026-02-24T10:30:00"
}
```

> **Quy ước trong tài liệu:** Các response example bên dưới **chỉ hiển thị nội dung của field `data`** để gọn. Khi implement, toàn bộ đều nằm trong `ApiResponse<T>`.  
> Với các endpoint trả message đơn giản (VD: `{ "message": "..." }`), `data` sẽ chứa object đó, VD: `{ "success": true, "message": "Success", "data": { "message": "Password changed successfully" }, "timestamp": "..." }`.

---

## 1. COMMON – Dùng chung (mọi role)

> **Convention:**  
> Các `id` trong response đều là `BigInt` auto-increment từ database trừ khi ghi chú khác.  
> Các trường `*Code` (VD: `transactionCode`, `payslipCode`) là mã nghiệp vụ auto-generated ở Backend — không phải Primary Key.  
> Avatar URL là Signed URL từ Cloudinary (Private mode, hết hạn 15 phút), được Backend sinh khi trả response.  
> Mã PIN giao dịch: **5 chữ số** (hash BCrypt lưu trong `user_security_settings.transaction_pin`).

---

### POST `/auth/login`
Đăng nhập, trả về token và thông tin user cơ bản.

**Body:**
```json
{ "email": "string", "password": "string" }
```

**Response:**
```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "eyJhbGci...",
  "user": {
    "id": 1,
    "fullName": "Nguyen Van A",
    "email": "nguyen.van.a@company.com",
    "role": "EMPLOYEE",
    "departmentId": 1,
    "departmentName": "Engineering",
    "avatar": "https://res.cloudinary.com/.../signed...",
    "isFirstLogin": false,
    "status": "ACTIVE"
  }
}
```
> **DB mapping:** `users` JOIN `roles` (qua `role_id`) JOIN `departments` (qua `department_id`) JOIN `user_profiles` → `file_storages` (qua `avatar_file_id`).  
> `id`: `users.id` (BigInt).  
> `role`: `roles.name` — EMPLOYEE | MANAGER | ACCOUNTANT | ADMIN.  
> `departmentId` / `departmentName`: nullable nếu chưa gán phòng ban.  
> `avatar`: nullable nếu chưa upload. Signed URL Cloudinary (15 phút).  
> `isFirstLogin`: `users.is_first_login`. Nếu `true` → FE redirect đổi mật khẩu.  
> `status`: `users.status` — `ACTIVE | LOCKED | PENDING`.

---

### POST `/auth/logout`
Đăng xuất, vô hiệu hoá refresh token.

**Body:** —  
**Response:** `{ "message": "Logged out successfully" }`

---

### POST `/auth/refresh-token`
Lấy access token mới từ refresh token.

**Body:**
```json
{ "refreshToken": "eyJhbGci..." }
```
**Response:**
```json
{ "accessToken": "eyJhbGci...", "refreshToken": "eyJhbGci..." }
```

---

### POST `/auth/forgot-password`
Gửi email đặt lại mật khẩu.

**Body:**
```json
{ "email": "string" }
```
**Response:** `{ "message": "Reset email sent if account exists" }`

---

### POST `/auth/reset-password`
Đặt lại mật khẩu bằng token từ email.

**Body:**
```json
{ "token": "string", "newPassword": "string", "confirmPassword": "string" }
```
**Response:** `{ "message": "Password reset successfully" }`

---

### POST `/auth/change-password`
Đổi mật khẩu lần đầu khi `isFirstLogin = true` (không cần mật khẩu cũ).

**Body:**
```json
{ "newPassword": "string" }
```
**Response:** `{ "message": "Password changed successfully" }`

---

### GET `/auth/me`
Lấy thông tin user hiện tại (dùng để restore session khi reload trang).

**Response:**
```json
{
  "id": 1,
  "fullName": "Nguyen Van A",
  "email": "nguyen.van.a@company.com",
  "role": "EMPLOYEE",
  "departmentId": 1,
  "departmentName": "Engineering",
  "avatar": "https://res.cloudinary.com/.../signed...",
  "isFirstLogin": false,
  "status": "ACTIVE"
}
```
> Response giống object `user` trong `POST /auth/login`.

---

### GET `/users/me/profile`
Lấy toàn bộ thông tin profile của user đang đăng nhập.

**DB mapping:** `users` JOIN `user_profiles` (qua `user_id`) JOIN `departments` (qua `department_id`) JOIN `file_storages` (qua `avatar_file_id`) JOIN `user_security_settings` (qua `user_id`).

**Response:**
```json
{
  "id": 1,
  "employeeCode": "MK001",
  "fullName": "Nguyen Van A",
  "email": "nguyen.van.a@company.com",
  "phoneNumber": "+84 901 234 567",
  "dateOfBirth": "1995-06-20",
  "address": "123 Nguyen Trai, Thanh Xuan, Ha Noi",
  "departmentId": 1,
  "departmentName": "Engineering",
  "jobTitle": "Senior Backend Developer",
  "citizenId": "079012345678",
  "avatar": "https://res.cloudinary.com/.../signed...",
  "bankInfo": {
    "bankName": "MB Bank",
    "accountNumber": "0123456789",
    "accountOwner": "NGUYEN VAN A"
  },
  "securitySettings": {
    "hasPIN": true,
    "pinLockedUntil": null
  }
}
```
> `id`: `users.id`.  
> `employeeCode`: `user_profiles.employee_code`.  
> `fullName`: `users.full_name`. `email`: `users.email`.  
> `phoneNumber`, `dateOfBirth`, `address`, `citizenId`, `jobTitle`: từ `user_profiles`.  
> `departmentId` / `departmentName`: từ `departments` qua `users.department_id`.  
> `avatar`: Signed URL Cloudinary, qua `user_profiles.avatar_file_id` → `file_storages`.  
> `bankInfo.bankName` = `user_profiles.bank_name`.  
> `bankInfo.accountNumber` = `user_profiles.bank_account_num`.  
> `bankInfo.accountOwner` = `user_profiles.bank_account_owner`.  
> `securitySettings.hasPIN`: `true` nếu `user_security_settings.transaction_pin IS NOT NULL`.  
> `securitySettings.pinLockedUntil`: `user_security_settings.locked_until`. `null` nếu không bị khoá hoặc đã hết hạn.

---

### PUT `/users/me/profile`
Cập nhật thông tin cá nhân.

**Body:**
```json
{
  "fullName": "string",
  "phoneNumber": "string",
  "dateOfBirth": "string (YYYY-MM-DD, optional)",
  "citizenId": "string (optional)",
  "address": "string (optional)"
}
```
**Response:** _(profile object đầy đủ như GET `/users/me/profile`)_
> `fullName` → cập nhật `users.full_name`.  
> Các trường còn lại → cập nhật `user_profiles`.

---

### PUT `/users/me/avatar`
Cập nhật avatar sau khi upload lên Cloudinary. Backend tạo record `file_storages` và cập nhật `user_profiles.avatar_file_id`. Nếu đã có avatar cũ, xoá file cũ trên Cloudinary và record `file_storages` tương ứng.

**Body:**
```json
{
  "cloudinaryPublicId": "avatars/user_1_1738900000",
  "fileName": "profile.jpg",
  "fileType": "image/jpeg",
  "size": 245000
}
```
**Response:**
```json
{ "avatar": "https://res.cloudinary.com/.../signed..." }
```
> Body chứa thông tin từ Cloudinary upload response → Backend tạo `file_storages` record.  
> `url` trong `file_storages` được Backend tự tạo từ `cloudinaryPublicId` + `cloudName`.  
> Response trả Signed URL (Private mode, 15 phút).

---

### PUT `/users/me/bank-info`
Cập nhật thông tin ngân hàng nhận lương.

**Body:**
```json
{ "bankName": "string", "accountNumber": "string", "accountOwner": "string" }
```
**Response:**
```json
{ "bankName": "MB Bank", "accountNumber": "0123456789", "accountOwner": "NGUYEN VAN A" }
```
> Map: `bankName` → `user_profiles.bank_name`, `accountNumber` → `user_profiles.bank_account_num`, `accountOwner` → `user_profiles.bank_account_owner`.

---

### PUT `/users/me/password`
Đổi mật khẩu (khi đã đăng nhập, yêu cầu mật khẩu hiện tại). Cập nhật `users.password` (BCrypt hash).

**Body:**
```json
{ "currentPassword": "string", "newPassword": "string", "confirmPassword": "string" }
```
**Response:** `{ "message": "Password changed successfully" }`

---

### POST `/users/me/pin`
Tạo PIN giao dịch lần đầu (khi `hasPIN = false`). Hash BCrypt → lưu `user_security_settings.transaction_pin`.

**Body:**
```json
{ "pin": "string (5 chữ số)" }
```
**Response:** `{ "message": "PIN created successfully" }`

---

### PUT `/users/me/pin`
Đổi PIN giao dịch (khi đã có PIN).

**Body:**
```json
{ "currentPin": "string", "newPin": "string (5 chữ số)" }
```
**Response:** `{ "message": "PIN updated successfully" }`

---

### POST `/users/me/pin/verify`
Xác minh PIN trước khi thực hiện giao dịch nhạy cảm (rút tiền, trả nợ, giải ngân).

**Body:**
```json
{ "pin": "string" }
```
**Response:** `{ "valid": true }` hoặc `401 Unauthorized`
> Nhập sai PIN → tăng `user_security_settings.retry_count`. Quá 5 lần → set `locked_until = NOW() + 30 phút`.  
> Khi `locked_until` chưa hết hạn → trả `423 Locked`.

---

### GET `/banks`
Danh sách ngân hàng hỗ trợ (dùng cho dropdown chọn ngân hàng). Static data — không map DB.

**Response:**
```json
[
  { "value": "MB Bank",     "label": "MB Bank (Quân đội)" },
  { "value": "Vietcombank", "label": "Vietcombank (VCB)" },
  { "value": "Techcombank", "label": "Techcombank (TCB)" },
  { "value": "BIDV",        "label": "BIDV" },
  { "value": "VietinBank",  "label": "VietinBank" },
  { "value": "ACB",         "label": "ACB (Á Châu)" },
  { "value": "VPBank",      "label": "VPBank (Việt Nam Thịnh Vượng)" },
  { "value": "TPBank",      "label": "TPBank (Tiên Phong)" },
  { "value": "Sacombank",   "label": "Sacombank" },
  { "value": "HDBank",      "label": "HDBank (Phát triển TP.HCM)" }
]
```
> `value` là giá trị lưu vào `user_profiles.bank_name`.

---

### GET `/wallet`
Lấy số dư ví của user hiện tại.

**DB mapping:** `wallets` WHERE `user_id = currentUser.id`.

**Response:**
```json
{
  "id": 1,
  "balance": 10250000,
  "pendingBalance": 2000000,
  "debtBalance": 0,
  "version": 5
}
```
> `id`: `wallets.id`.  
> `balance`: `wallets.balance` — tiền khả dụng (có thể rút/chi).  
> `pendingBalance`: `wallets.pending_balance` — tiền treo (đang chờ xử lý rút).  
> `debtBalance`: `wallets.debt_balance` — dư nợ tạm ứng.  
> `version`: `wallets.version` — dùng cho Optimistic Locking phía client (optional hiển thị).

---

### GET `/wallet/transactions`
Lịch sử giao dịch ví của user hiện tại, với filter và phân trang.

**Params:** `?type=DEPOSIT|WITHDRAW|REQUEST_PAYMENT|PAYSLIP_PAYMENT|SYSTEM_ADJUSTMENT&status=SUCCESS|PENDING|FAILED&from=2026-01-01&to=2026-02-28&page=1&limit=20`

**DB mapping:** `transactions` JOIN `wallets` (qua `wallet_id`) WHERE `wallets.user_id = currentUser.id`.

**Response:**
```json
{
  "items": [
    {
      "id": 101,
      "transactionCode": "TXN-8829145A",
      "type": "PAYSLIP_PAYMENT",
      "status": "SUCCESS",
      "amount": 15000000,
      "balanceAfter": 15250000,
      "referenceType": "PAYSLIP",
      "referenceId": 42,
      "description": "Lương T02/2026",
      "createdAt": "2026-02-10T09:00:00Z"
    },
    {
      "id": 95,
      "transactionCode": "TXN-6612A33B",
      "type": "WITHDRAW",
      "status": "SUCCESS",
      "amount": -2000000,
      "balanceAfter": 250000,
      "referenceType": null,
      "referenceId": null,
      "description": "Rút tiền về MB Bank",
      "createdAt": "2026-02-08T14:20:00Z"
    }
  ],
  "total": 42,
  "page": 1,
  "limit": 20,
  "totalPages": 3
}
```
> `id`: `transactions.id`.  
> `transactionCode`: `transactions.transaction_code` — mã giao dịch nội bộ, format `TXN-8829145A`.  
> `type`: `transactions.type` — `DEPOSIT | WITHDRAW | REQUEST_PAYMENT | PAYSLIP_PAYMENT | SYSTEM_ADJUSTMENT`.  
> `status`: `transactions.status` — `SUCCESS | PENDING | FAILED`.  
> `amount`: `transactions.amount` — số dương = tiền vào, số âm = tiền ra.  
> `balanceAfter`: `transactions.balance_after` — snapshot số dư sau giao dịch.  
> `referenceType`: `transactions.reference_type` — `REQUEST | PAYSLIP | null`.  
> `referenceId`: `transactions.reference_id` — ID chứng từ gốc (BigInt). Nullable.  
> `description`: `transactions.description`.  
> `createdAt`: `transactions.created_at`.

---

### POST `/wallet/withdraw`
Rút tiền về tài khoản ngân hàng đã đăng ký. Yêu cầu xác minh PIN (5 chữ số).

**Body:**
```json
{ "amount": 2000000, "pin": "string" }
```
**Response:**
```json
{
  "id": 102,
  "transactionCode": "TXN-9A33BC21",
  "type": "WITHDRAW",
  "status": "SUCCESS",
  "amount": -2000000,
  "balanceAfter": 8250000,
  "createdAt": "2026-02-22T10:05:00Z"
}
```
> Backend: verify PIN → kiểm tra `wallets.balance >= amount` → tạo `transactions` record → cập nhật `wallets.balance` (Optimistic Lock on `version`).  
> Nếu `balance < amount` → `400 Bad Request: INSUFFICIENT_FUNDS`.  
> Nếu PIN sai → `401 Unauthorized`. Quá 5 lần → `423 Locked`.

---

### POST `/wallet/deposit/generate-qr`
Tạo mã VietQR để nạp tiền vào ví.

**Body:**
```json
{ "amount": 500000, "description": "string (optional)" }
```
**Response:**
```json
{
  "qrCodeUrl": "https://img.vietqr.io/image/970415-0123456789-compact.png?amount=500000&addInfo=IFMS_DEP_1_500000",
  "amount": 500000,
  "bankAccount": "0123456789",
  "bankName": "MB Bank",
  "accountOwner": "IFMS",
  "description": "IFMS_DEP_1_500000",
  "expiresAt": "2026-02-22T11:30:00Z"
}
```
> `description` format gợi ý: `IFMS_DEP_{userId}_{amount}` — dùng để webhook xác nhận giao dịch.  
> Sau khi ngân hàng xác nhận (qua webhook), Backend tạo `transactions` record (`type=DEPOSIT`, `gateway_provider=PAYOS`) và cộng `wallets.balance`.

---

### GET `/projects`
Danh sách projects mà user đang tham gia (dùng populate dropdown khi tạo request).

**Params:** `?status=PLANNING|ACTIVE|PAUSED|CLOSED&page=1&limit=50`

**DB mapping:** `projects` JOIN `project_members` WHERE `project_members.user_id = currentUser.id`. Nếu role = MANAGER → lọc theo `projects.department_id`. Nếu ADMIN/ACCOUNTANT → xem toàn bộ tuỳ scope.

**Response:**
```json
{
  "items": [
    {
      "id": 1,
      "projectCode": "PRJ-ERP-2026",
      "name": "E-Commerce Platform",
      "status": "ACTIVE",
      "departmentId": 1,
      "totalBudget": 150000000,
      "totalSpent": 54500000,
      "currentPhaseId": 2,
      "currentPhaseName": "Phase 2: Payment Integration"
    }
  ],
  "total": 12,
  "page": 1,
  "limit": 50,
  "totalPages": 1
}
```
> `id`: `projects.id` (BigInt).  
> `projectCode`: `projects.project_code` — auto-generated, format `PRJ-ERP-2026`.  
> `status`: `projects.status` — `PLANNING | ACTIVE | PAUSED | CLOSED`.  
> `totalBudget` / `totalSpent`: `projects.total_budget` / `projects.total_spent`.  
> `currentPhaseId` / `currentPhaseName`: join `project_phases` qua `projects.current_phase_id`. Nullable.  
> `departmentId`: `projects.department_id`.

---

### GET `/projects/:id/phases`
Danh sách phases của một project (dùng populate dropdown chọn phase khi tạo request).

**Params:** `?status=ACTIVE|CLOSED`

**Response:**
```json
{
  "projectId": 1,
  "projectName": "E-Commerce Platform",
  "phases": [
    {
      "id": 2,
      "phaseCode": "PH-UIUX-01",
      "name": "Phase 2: Payment Integration",
      "budgetLimit": 60000000,
      "currentSpent": 54500000,
      "status": "ACTIVE",
      "startDate": "2026-01-01",
      "endDate": "2026-03-31"
    }
  ]
}
```
> `id`: `project_phases.id` (BigInt).  
> `phaseCode`: `project_phases.phase_code` — auto-generated, format `PH-UIUX-01`.  
> `budgetLimit` / `currentSpent`: `project_phases.budget_limit` / `project_phases.current_spent`.  
> `status`: `project_phases.status` — `ACTIVE | CLOSED`.

---

### GET `/files/signature`
Lấy Cloudinary upload signature để upload file trực tiếp từ client (Private mode).

**Params:** `?folder=requests|avatars|payroll`

**Response:**
```json
{
  "signature": "abc123...",
  "timestamp": 1738900000,
  "cloudName": "ifms-cloud",
  "apiKey": "123456789",
  "folder": "requests",
  "publicId": "requests/file_1738900000_abc123"
}
```
> `publicId`: auto-generated bởi Backend, đảm bảo unique trên Cloudinary.  
> Client dùng các params này để upload trực tiếp lên Cloudinary SDK.

---

### DELETE `/files/:publicId`
Xoá file khỏi Cloudinary khi user xoá attachment trước khi submit. Backend cũng xoá record tương ứng trong `file_storages`.

**Response:** `{ "message": "File deleted successfully" }`
> `:publicId` = `file_storages.cloudinary_public_id` (URL-encoded nếu có `/`).

---

### GET `/payslips`
Danh sách phiếu lương của user hiện tại.

**Params:** `?year=2025&status=DRAFT|PAID&page=1&limit=12`

**DB mapping:** `payslips` JOIN `payroll_periods` (qua `period_id`) WHERE `payslips.user_id = currentUser.id`.

**Response:**
```json
{
  "items": [
    {
      "id": 10,
      "payslipCode": "PSL-MK001-1025",
      "periodId": 5,
      "periodName": "Lương Tháng 10/2025",
      "month": 10,
      "year": 2025,
      "status": "PAID",
      "finalNetSalary": 22500000
    }
  ],
  "total": 10,
  "page": 1,
  "limit": 12,
  "totalPages": 1
}
```
> `id`: `payslips.id` (BigInt).  
> `payslipCode`: `payslips.payslip_code` — auto-generated, format `PSL-EMP001-0226`.  
> `periodId`: `payslips.period_id`. `periodName`: `payroll_periods.name`.  
> `month` / `year`: `payroll_periods.month` / `payroll_periods.year`.  
> `status`: `payslips.status` — `DRAFT | PAID`.  
> `finalNetSalary`: `payslips.final_net_salary`.

---

### GET `/payslips/:id`
Chi tiết phiếu lương.

**DB mapping:** `payslips` JOIN `payroll_periods` JOIN `users` JOIN `user_profiles` JOIN `departments`.

**Response:**
```json
{
  "id": 10,
  "payslipCode": "PSL-MK001-1025",
  "periodId": 5,
  "periodName": "Lương Tháng 10/2025",
  "month": 10,
  "year": 2025,
  "status": "PAID",
  "baseSalary": 20000000,
  "bonus": 5000000,
  "allowance": 2000000,
  "totalEarnings": 27000000,
  "deduction": 4000000,
  "advanceDeduct": 500000,
  "totalDeduction": 4500000,
  "finalNetSalary": 22500000,
  "employee": {
    "id": 1,
    "fullName": "Nguyen Van A",
    "employeeCode": "MK001",
    "departmentName": "Engineering",
    "jobTitle": "Senior Backend Developer",
    "bankName": "MB Bank",
    "bankAccountNum": "****6789"
  }
}
```
> `baseSalary`: `payslips.base_salary`.  
> `bonus`: `payslips.bonus`. `allowance`: `payslips.allowance`.  
> `deduction`: `payslips.deduction`. `advanceDeduct`: `payslips.advance_deduct`.  
> `totalEarnings = baseSalary + bonus + allowance` (computed).  
> `totalDeduction = deduction + advanceDeduct` (computed).  
> `finalNetSalary = totalEarnings - totalDeduction` = `payslips.final_net_salary`.  
> `employee.bankAccountNum`: chỉ trả 4 số cuối (masked) từ `user_profiles.bank_account_num`.  
> `employee`: join từ `users` + `user_profiles` + `departments`.

---

### GET `/notifications`
Danh sách thông báo của user hiện tại.

**Params:** `?isRead=true|false&type=SYSTEM|REQUEST_APPROVED|REQUEST_REJECTED|SALARY_PAID|WARN&page=1&limit=20`

**DB mapping:** `notifications` WHERE `user_id = currentUser.id`.

**Response:**
```json
{
  "items": [
    {
      "id": 1,
      "type": "REQUEST_APPROVED",
      "title": "Request Approved",
      "message": "Your request REQ-IT-2602-001 has been approved by manager",
      "isRead": false,
      "refId": 101,
      "refType": "REQUEST",
      "createdAt": "2026-02-19T10:30:00Z"
    }
  ],
  "unreadCount": 3,
  "total": 15,
  "page": 1,
  "limit": 20,
  "totalPages": 1
}
```
> `id`: `notifications.id` (BigInt).  
> `type`: `notifications.type` — `SYSTEM | REQUEST_APPROVED | REQUEST_REJECTED | SALARY_PAID | WARN`.  
> `title`: `notifications.title`. `message`: `notifications.message`.  
> `isRead`: `notifications.is_read`.  
> `refId`: `notifications.ref_id` — ID đối tượng liên quan (BigInt). Nullable.  
> `refType`: `notifications.ref_type` — `REQUEST | PAYSLIP | PROJECT`. Nullable.  
> `unreadCount`: COUNT WHERE `is_read = false` — cho badge notification.

---

### PUT `/notifications/:id/read`
Đánh dấu một thông báo đã đọc.

**Response:** `{ "id": 1, "isRead": true }`

---

### PUT `/notifications/read-all`
Đánh dấu tất cả thông báo đã đọc.

**Response:** `{ "message": "All notifications marked as read", "updatedCount": 3 }`

---

## 2. EMPLOYEE

---

### GET `/requests`
Danh sách requests của employee hiện tại.

**Params:** `?type=ADVANCE|EXPENSE|REIMBURSE&status=PENDING_MANAGER|PENDING_ADMIN|APPROVED|PAID|REJECTED|CANCELLED&search=string&page=1&limit=20`

**DB mapping:** `requests` WHERE `requester_id = currentUser.id` JOIN `projects` JOIN `project_phases`.

**Response:**
```json
{
  "items": [
    {
      "id": 101,
      "requestCode": "REQ-IT-2602-001",
      "type": "ADVANCE",
      "status": "PENDING_MANAGER",
      "amount": 5000000,
      "approvedAmount": null,
      "description": "Advance payment for development team travel expenses",
      "rejectReason": null,
      "projectId": 1,
      "projectName": "E-Commerce Platform",
      "phaseId": 2,
      "phaseName": "Phase 2: Payment Integration",
      "createdAt": "2026-02-15T09:30:00",
      "updatedAt": "2026-02-15T09:30:00"
    }
  ],
  "total": 24,
  "page": 1,
  "limit": 20,
  "totalPages": 2
}
```
> `id`: `requests.id` (Long).  
> `requestCode`: `requests.request_code` — auto-generated, format `REQ-{TYPE}-{MMYY}-{SEQ}`.  
> `type`: `requests.type` — `ADVANCE | EXPENSE | REIMBURSE`.  
> `status`: `requests.status` — `PENDING_MANAGER | PENDING_ADMIN | APPROVED | PAID | REJECTED | CANCELLED`.  
> `amount` / `approvedAmount`: `requests.amount` / `requests.approved_amount`. `approvedAmount` nullable nếu chưa duyệt.  
> `rejectReason`: `requests.reject_reason`. Nullable.  
> `projectId` / `projectName`: join `projects`. `phaseId` / `phaseName`: join `project_phases`.  
> `createdAt` / `updatedAt`: từ `BaseEntity`.

---

### GET `/requests/summary`
Tổng hợp số lượng request theo trạng thái (của employee hiện tại).

**Response:**
```json
{
  "totalPending": 3,
  "totalApproved": 12,
  "totalRejected": 2,
  "totalPaid": 8,
  "totalCancelled": 1
}
```
> Computed: COUNT GROUP BY `requests.status` WHERE `requester_id = currentUser.id`.

---

### GET `/requests/:id`
Chi tiết một request (employee chỉ xem request của mình).

**DB mapping:** `requests` JOIN `projects` JOIN `project_phases` + sub-query `request_attachments` → `file_storages` + sub-query `request_histories`.

**Response:**
```json
{
  "id": 101,
  "requestCode": "REQ-IT-2602-001",
  "type": "ADVANCE",
  "status": "PENDING_MANAGER",
  "amount": 5000000,
  "approvedAmount": null,
  "description": "Advance payment for development team travel expenses",
  "rejectReason": null,
  "projectId": 1,
  "projectCode": "PRJ-ERP-2026",
  "projectName": "E-Commerce Platform",
  "phaseId": 2,
  "phaseCode": "PH-PAY-01",
  "phaseName": "Phase 2: Payment Integration",
  "requesterId": 1,
  "requesterName": "Nguyen Van A",
  "attachments": [
    {
      "fileId": 10,
      "fileName": "Travel_Itinerary.pdf",
      "cloudinaryPublicId": "requests/file_adv_001",
      "url": "https://res.cloudinary.com/.../signed...",
      "fileType": "application/pdf",
      "size": 156789
    }
  ],
  "timeline": [
    {
      "id": 1,
      "action": "APPROVE",
      "statusAfterAction": "APPROVED",
      "actorId": 5,
      "actorName": "Tran Thi Bich Ngoc",
      "comment": "Approved",
      "createdAt": "2026-02-16T10:30:00"
    }
  ],
  "createdAt": "2026-02-15T09:30:00",
  "updatedAt": "2026-02-15T09:30:00"
}
```
> `attachments[]`: join `request_attachments` → `file_storages`. `url` là Signed URL Cloudinary (15 phút).  
> `timeline[]`: từ `request_histories`. `action`: `RequestAction` enum — `APPROVE | REJECT | ESCALATE`.  
> `statusAfterAction`: `RequestHistoryStatus` — `PENDING | APPROVED | REJECTED | CANCELED`. Snapshot trạng thái của Request sau khi action diễn ra.  
> `actorId` / `actorName`: join `users` qua `request_histories.actor_id`.

---

### POST `/requests`
Tạo request mới. `EXPENSE` và `REIMBURSE` bắt buộc có ít nhất 1 attachment. `amount` tối thiểu 10,000 VND. Backend auto-generate `requestCode`.

**Body:**
```json
{
  "type": "ADVANCE",
  "projectId": 1,
  "phaseId": 2,
  "amount": 5000000,
  "description": "Advance for development tools Q1",
  "attachmentFileIds": [10, 11]
}
```
> `type`: `ADVANCE | EXPENSE | REIMBURSE`.  
> `projectId` / `phaseId`: ID (Long) từ dropdown `GET /projects` và `GET /projects/:id/phases`. Phase phải `ACTIVE` và Project phải `ACTIVE`.  
> `attachmentFileIds`: danh sách `file_storages.id` đã upload trước đó (qua `GET /files/signature` + Cloudinary SDK). Backend tạo records trong `request_attachments`.  
> Validation: Employee phải là member của project (`project_members`). `EXPENSE` / `REIMBURSE` → `attachmentFileIds` không được rỗng.

**Response:**
```json
{
  "id": 102,
  "requestCode": "REQ-IT-2602-002",
  "type": "ADVANCE",
  "status": "PENDING_MANAGER",
  "amount": 5000000,
  "approvedAmount": null,
  "description": "Advance for development tools Q1",
  "rejectReason": null,
  "projectId": 1,
  "projectCode": "PRJ-ERP-2026",
  "projectName": "E-Commerce Platform",
  "phaseId": 2,
  "phaseCode": "PH-PAY-01",
  "phaseName": "Phase 2: Payment Integration",
  "requesterId": 1,
  "requesterName": "Nguyen Van A",
  "attachments": [
    {
      "fileId": 10,
      "fileName": "tool_invoice.pdf",
      "cloudinaryPublicId": "requests/file_tool_001",
      "url": "https://res.cloudinary.com/.../signed...",
      "fileType": "application/pdf",
      "size": 120000
    }
  ],
  "timeline": [],
  "createdAt": "2026-02-24T09:00:00",
  "updatedAt": "2026-02-24T09:00:00"
}
```

---

### PUT `/requests/:id`
Chỉnh sửa request. Chỉ cho phép khi `status = PENDING_MANAGER`. Chỉ owner (requester) được sửa.

**Body:**
```json
{
  "amount": 5000000,
  "description": "Updated description",
  "attachmentFileIds": [10, 12]
}
```
> `attachmentFileIds`: ghi đè (sync) — danh sách mới thay thế hoàn toàn danh sách cũ trong `request_attachments`. Các file cũ không còn trong list sẽ bị xóa khỏi `request_attachments` (và Cloudinary nếu cần).

**Response:**
```json
{
  "id": 101,
  "requestCode": "REQ-IT-2602-001",
  "type": "ADVANCE",
  "status": "PENDING_MANAGER",
  "amount": 5000000,
  "approvedAmount": null,
  "description": "Updated description",
  "rejectReason": null,
  "projectId": 1,
  "projectCode": "PRJ-ERP-2026",
  "projectName": "E-Commerce Platform",
  "phaseId": 2,
  "phaseCode": "PH-PAY-01",
  "phaseName": "Phase 2: Payment Integration",
  "requesterId": 1,
  "requesterName": "Nguyen Van A",
  "attachments": [
    {
      "fileId": 10,
      "fileName": "Travel_Itinerary.pdf",
      "cloudinaryPublicId": "requests/file_adv_001",
      "url": "https://res.cloudinary.com/.../signed...",
      "fileType": "application/pdf",
      "size": 156789
    },
    {
      "fileId": 12,
      "fileName": "Receipt_updated.jpg",
      "cloudinaryPublicId": "requests/file_adv_012",
      "url": "https://res.cloudinary.com/.../signed...",
      "fileType": "image/jpeg",
      "size": 89000
    }
  ],
  "timeline": [],
  "createdAt": "2026-02-15T09:30:00",
  "updatedAt": "2026-02-24T11:00:00"
}
```

---

### DELETE `/requests/:id`
Huỷ request (chuyển status sang `CANCELLED`). Chỉ cho phép khi `status = PENDING_MANAGER`. Chỉ owner được huỷ.

**Response:** `{ "message": "Request cancelled successfully" }`
> Backend cập nhật `requests.status = CANCELLED`.

---

## 3. MANAGER

---

### GET `/manager/approvals`
Danh sách requests chờ manager duyệt (status = `PENDING_MANAGER`, thuộc department của manager).

**Params:** `?type=ADVANCE|EXPENSE|REIMBURSE&search=string&page=1&limit=20`

**DB mapping:** `requests` WHERE `status = PENDING_MANAGER` AND requester thuộc department của manager. JOIN `users` (requester) JOIN `user_profiles` JOIN `projects` JOIN `project_phases` + `request_attachments` → `file_storages`.

**Response:**
```json
{
  "items": [
    {
      "id": 101,
      "requestCode": "REQ-IT-2602-001",
      "type": "ADVANCE",
      "status": "PENDING_MANAGER",
      "amount": 8500000,
      "description": "Advance payment for API licenses.",
      "requester": {
        "id": 1,
        "fullName": "Nguyen Van An",
        "avatar": "https://res.cloudinary.com/.../signed...",
        "employeeCode": "MK001",
        "jobTitle": "Backend Developer",
        "email": "van.an@ifms.vn"
      },
      "project": {
        "id": 1,
        "projectCode": "PRJ-ERP-2026",
        "name": "ERP Integration"
      },
      "phase": {
        "id": 2,
        "phaseCode": "PH-DEV-02",
        "name": "Phase 2 – Development",
        "budgetLimit": 80000000,
        "currentSpent": 62500000
      },
      "attachments": [
        {
          "fileId": 10,
          "fileName": "stripe_invoice_Q1.pdf",
          "url": "https://res.cloudinary.com/.../signed...",
          "fileType": "application/pdf",
          "size": 251000
        }
      ],
      "createdAt": "2026-02-18T09:15:00"
    }
  ],
  "total": 7,
  "page": 1,
  "limit": 20,
  "totalPages": 1
}
```
> `requester`: join `users` + `user_profiles` + `file_storages` (avatar).  
> `project` / `phase`: join `projects` + `project_phases`.  
> `attachments[]`: join `request_attachments` → `file_storages`. URL = Signed URL.

---

### GET `/manager/approvals/:id`
Chi tiết một request cần duyệt. Response giống `GET /requests/:id` nhưng bao gồm thông tin requester đầy đủ.

**Response:**
```json
{
  "id": 101,
  "requestCode": "REQ-IT-2602-001",
  "type": "ADVANCE",
  "status": "PENDING_MANAGER",
  "amount": 8500000,
  "approvedAmount": null,
  "description": "Advance payment for API licenses.",
  "rejectReason": null,
  "requester": {
    "id": 1,
    "fullName": "Nguyen Van An",
    "avatar": "https://res.cloudinary.com/.../signed...",
    "employeeCode": "MK001",
    "jobTitle": "Backend Developer",
    "email": "van.an@ifms.vn",
    "departmentName": "Engineering"
  },
  "project": {
    "id": 1,
    "projectCode": "PRJ-ERP-2026",
    "name": "ERP Integration"
  },
  "phase": {
    "id": 2,
    "phaseCode": "PH-DEV-02",
    "name": "Phase 2 – Development",
    "budgetLimit": 80000000,
    "currentSpent": 62500000
  },
  "attachments": [
    {
      "fileId": 10,
      "fileName": "stripe_invoice_Q1.pdf",
      "cloudinaryPublicId": "requests/stripe_invoice_Q1",
      "url": "https://res.cloudinary.com/.../signed...",
      "fileType": "application/pdf",
      "size": 251000
    }
  ],
  "timeline": [
    {
      "id": 1,
      "action": "ESCALATE",
      "statusAfterAction": "PENDING",
      "actorId": 1,
      "actorName": "Nguyen Van An",
      "comment": null,
      "createdAt": "2026-02-18T09:15:00"
    }
  ],
  "createdAt": "2026-02-18T09:15:00",
  "updatedAt": "2026-02-18T09:15:00"
}
```

---

### POST `/manager/approvals/:id/approve`
Duyệt request.  
- Nếu `amount <= MANAGER_LIMIT` (từ `system_configs`) → status chuyển sang `APPROVED`.  
- Nếu `amount > MANAGER_LIMIT` → status chuyển sang `PENDING_ADMIN` (escalate).  
- Manager không được duyệt request của chính mình (self-approval restriction).

**Body:**
```json
{ "comment": "string (optional)" }
```
**Response:**
```json
{
  "id": 101,
  "requestCode": "REQ-IT-2602-001",
  "status": "PENDING_ADMIN",
  "comment": "Looks good, escalating to admin."
}
```
> Backend tạo `request_histories` record: `action = APPROVE`, `status_after_action` tương ứng.

---

### POST `/manager/approvals/:id/reject`
Từ chối request. Bắt buộc nhập lý do.

**Body:**
```json
{ "reason": "string" }
```
**Response:**
```json
{
  "id": 101,
  "requestCode": "REQ-IT-2602-001",
  "status": "REJECTED",
  "rejectReason": "Budget exceeded for this phase"
}
```
> Backend cập nhật `requests.status = REJECTED`, `requests.reject_reason = reason`. Tạo `request_histories`: `action = REJECT`, `status_after_action = REJECTED`.

---

### GET `/manager/department/members`
Danh sách nhân viên trong department của manager.

**Params:** `?search=string&page=1&limit=20`

**DB mapping:** `users` WHERE `department_id = manager.department_id` JOIN `user_profiles` JOIN `file_storages` (avatar) + aggregate từ `wallets` (debt_balance) + aggregate `requests` (pending count).

**Response:**
```json
{
  "items": [
    {
      "id": 1,
      "fullName": "Nguyen Van An",
      "email": "van.an@ifms.vn",
      "employeeCode": "MK001",
      "avatar": "https://res.cloudinary.com/.../signed...",
      "jobTitle": "Backend Developer",
      "status": "ACTIVE",
      "pendingRequestsCount": 1,
      "debtBalance": 8500000
    }
  ],
  "total": 12,
  "page": 1,
  "limit": 20,
  "totalPages": 1
}
```
> `id`: `users.id` (Long).  
> `debtBalance`: `wallets.debt_balance` (join qua `user_id`).  
> `pendingRequestsCount`: COUNT `requests` WHERE `requester_id = user.id` AND `status IN (PENDING_MANAGER, PENDING_ADMIN)`.

---

### GET `/manager/department/members/:id`
Chi tiết một nhân viên trong department.

**Response:**
```json
{
  "id": 1,
  "fullName": "Nguyen Van An",
  "email": "van.an@ifms.vn",
  "employeeCode": "MK001",
  "avatar": "https://res.cloudinary.com/.../signed...",
  "jobTitle": "Backend Developer",
  "phoneNumber": "+84 901 234 567",
  "status": "ACTIVE",
  "debtBalance": 8500000,
  "pendingRequestsCount": 1,
  "assignedProjects": [
    {
      "projectId": 1,
      "projectCode": "PRJ-ERP-2026",
      "projectName": "ERP Integration",
      "role": "Backend Developer"
    }
  ],
  "recentRequests": [
    {
      "id": 101,
      "requestCode": "REQ-IT-2602-001",
      "type": "ADVANCE",
      "amount": 8500000,
      "status": "PENDING_MANAGER",
      "createdAt": "2026-02-18T09:15:00"
    }
  ]
}
```
> `assignedProjects`: join `project_members` → `projects`. `role`: `project_members.role`.  
> `recentRequests`: top 5 gần nhất từ `requests` WHERE `requester_id = member.id`.

---

### GET `/manager/projects`
Danh sách projects thuộc department của manager.

**Params:** `?status=PLANNING|ACTIVE|PAUSED|CLOSED&search=string&page=1&limit=20`

**DB mapping:** `projects` WHERE `department_id = manager.department_id` + COUNT `project_members`.

**Response:**
```json
{
  "items": [
    {
      "id": 1,
      "projectCode": "PRJ-ERP-2026",
      "name": "ERP Integration",
      "status": "ACTIVE",
      "totalBudget": 150000000,
      "totalSpent": 113600000,
      "memberCount": 5,
      "currentPhaseId": 2,
      "currentPhaseName": "Phase 2: Development",
      "createdAt": "2026-01-05T09:00:00"
    }
  ],
  "total": 4,
  "page": 1,
  "limit": 20,
  "totalPages": 1
}
```
> `id`: `projects.id` (Long).  
> `projectCode`: `projects.project_code`.  
> `status`: `ProjectStatus` — `PLANNING | ACTIVE | PAUSED | CLOSED`.  
> `totalBudget` / `totalSpent`: `projects.total_budget` / `projects.total_spent`.  
> `memberCount`: COUNT `project_members` WHERE `project_id = project.id`.  
> `currentPhaseId` / `currentPhaseName`: join `project_phases` qua `projects.current_phase_id`.

---

### GET `/manager/projects/:id`
Chi tiết project với phases và members.

**DB mapping:** `projects` JOIN `project_phases` + `project_members` → `users` → `user_profiles`.

**Response:**
```json
{
  "id": 1,
  "projectCode": "PRJ-ERP-2026",
  "name": "ERP Integration",
  "description": "Full ERP integration with microservices and API layer.",
  "status": "ACTIVE",
  "totalBudget": 150000000,
  "totalSpent": 113600000,
  "departmentId": 1,
  "managerId": 5,
  "currentPhaseId": 2,
  "phases": [
    {
      "id": 2,
      "phaseCode": "PH-DEV-02",
      "name": "Phase 2: Payment Integration",
      "budgetLimit": 60000000,
      "currentSpent": 54500000,
      "status": "ACTIVE",
      "startDate": "2026-01-01",
      "endDate": "2026-03-31"
    }
  ],
  "members": [
    {
      "userId": 1,
      "fullName": "Nguyen Van An",
      "avatar": "https://res.cloudinary.com/.../signed...",
      "employeeCode": "MK001",
      "role": "Backend Developer",
      "joinedAt": "2026-01-05T09:00:00"
    }
  ],
  "createdAt": "2026-01-05T09:00:00",
  "updatedAt": "2026-02-20T14:00:00"
}
```
> `phases[]`: từ `project_phases` WHERE `project_id = project.id`.  
> `members[]`: join `project_members` → `users` → `user_profiles` → `file_storages`. `role`: `project_members.role`. `joinedAt`: `project_members.joined_at`.

---

### POST `/manager/projects`
Tạo project mới trong department của manager. Backend auto-generate `projectCode`. Tự động set `department_id` = department của manager, `manager_id` = manager hiện tại.

**Body:**
```json
{
  "name": "New Project Name",
  "description": "Project description",
  "totalBudget": 150000000,
  "phases": [
    {
      "name": "Phase 1: Kickoff",
      "budgetLimit": 50000000,
      "startDate": "2026-03-01",
      "endDate": "2026-05-31"
    }
  ]
}
```
> `phases`: optional, có thể tạo project không kèm phase. Mỗi phase Backend auto-generate `phaseCode`.  
> Validation: `SUM(phases.budgetLimit) <= totalBudget`.

**Response:**
```json
{
  "id": 5,
  "projectCode": "PRJ-NEW-2026",
  "name": "New Project Name",
  "description": "Project description",
  "status": "PLANNING",
  "totalBudget": 150000000,
  "totalSpent": 0,
  "departmentId": 1,
  "managerId": 5,
  "currentPhaseId": 10,
  "phases": [
    {
      "id": 10,
      "phaseCode": "PH-KICK-01",
      "name": "Phase 1: Kickoff",
      "budgetLimit": 50000000,
      "currentSpent": 0,
      "status": "ACTIVE",
      "startDate": "2026-03-01",
      "endDate": "2026-05-31"
    }
  ],
  "members": [],
  "createdAt": "2026-02-24T10:00:00",
  "updatedAt": "2026-02-24T10:00:00"
}
```

---

### PUT `/manager/projects/:id`
Cập nhật thông tin project. Chỉ cập nhật khi project thuộc department của manager.

**Body:**
```json
{
  "name": "string (optional)",
  "description": "string (optional)",
  "totalBudget": 150000000,
  "status": "ACTIVE"
}
```
> `status`: optional — cho phép Manager chuyển `ACTIVE → PAUSED`, `PAUSED → ACTIVE`, `ACTIVE → CLOSED`. Khi `PAUSED/CLOSED` → chặn tạo request mới.

**Response:**
```json
{
  "id": 1,
  "projectCode": "PRJ-ERP-2026",
  "name": "ERP Integration",
  "description": "Full ERP integration with microservices and API layer.",
  "status": "ACTIVE",
  "totalBudget": 150000000,
  "totalSpent": 113600000,
  "departmentId": 1,
  "managerId": 5,
  "currentPhaseId": 2,
  "phases": [
    {
      "id": 2,
      "phaseCode": "PH-DEV-02",
      "name": "Phase 2: Payment Integration",
      "budgetLimit": 60000000,
      "currentSpent": 54500000,
      "status": "ACTIVE",
      "startDate": "2026-01-01",
      "endDate": "2026-03-31"
    }
  ],
  "members": [
    {
      "userId": 1,
      "fullName": "Nguyen Van An",
      "avatar": "https://res.cloudinary.com/.../signed...",
      "employeeCode": "MK001",
      "role": "Backend Developer",
      "joinedAt": "2026-01-05T09:00:00"
    }
  ],
  "createdAt": "2026-01-05T09:00:00",
  "updatedAt": "2026-02-24T14:00:00"
}
```

---

### POST `/manager/projects/:id/phases`
Thêm phase mới vào project. Backend auto-generate `phaseCode`.

**Body:**
```json
{
  "name": "Phase 3: Testing",
  "budgetLimit": 50000000,
  "startDate": "2026-06-01",
  "endDate": "2026-08-31"
}
```
**Response:**
```json
{
  "id": 5,
  "phaseCode": "PH-TEST-03",
  "name": "Phase 3: Testing",
  "budgetLimit": 50000000,
  "currentSpent": 0,
  "status": "ACTIVE",
  "startDate": "2026-06-01",
  "endDate": "2026-08-31"
}
```

---

### PUT `/manager/projects/:id/phases/:phaseId`
Cập nhật thông tin phase.

**Body:**
```json
{
  "name": "string (optional)",
  "budgetLimit": 60000000,
  "endDate": "2026-09-30",
  "status": "ACTIVE"
}
```
> `status`: `ACTIVE | CLOSED`. Khi `CLOSED` → chặn tạo request cho phase này. Tiền chưa dùng có thể chuyển sang Phase mới hoặc hoàn về department budget.

**Response:**
```json
{
  "id": 2,
  "phaseCode": "PH-DEV-02",
  "name": "Phase 2: Payment Integration",
  "budgetLimit": 60000000,
  "currentSpent": 54500000,
  "status": "ACTIVE",
  "startDate": "2026-01-01",
  "endDate": "2026-09-30"
}
```

---

### POST `/manager/projects/:id/members`
Cập nhật toàn bộ danh sách members của project (sync — xoá cũ trong `project_members`, thêm mới theo list).

**Body:**
```json
{
  "members": [
    { "userId": 1, "role": "Backend Developer" },
    { "userId": 5, "role": "Frontend Developer" }
  ]
}
```
> `userId`: `users.id` (Long). `role`: vai trò trong project (free text).  
> Backend xoá toàn bộ `project_members` WHERE `project_id = :id` rồi insert lại theo list mới.

**Response:**
```json
{
  "projectId": 1,
  "members": [
    {
      "userId": 1,
      "fullName": "Nguyen Van An",
      "avatar": "https://res.cloudinary.com/.../signed...",
      "employeeCode": "MK001",
      "role": "Backend Developer",
      "joinedAt": "2026-02-24T10:00:00"
    }
  ]
}
```

---

## 4. ACCOUNTANT

---

### GET `/accountant/disbursements`
Danh sách requests đã được duyệt, chờ giải ngân (`status = APPROVED`).

**Params:** `?type=ADVANCE|EXPENSE|REIMBURSE&search=string&page=1&limit=20`

**DB mapping:** `requests` WHERE `status = APPROVED` JOIN `users` (requester) + `user_profiles` + `projects` + `project_phases` + `request_attachments` → `file_storages` + `request_histories` (tìm approval records).

**Response:**
```json
{
  "items": [
    {
      "id": 101,
      "requestCode": "REQ-IT-2602-001",
      "type": "ADVANCE",
      "status": "APPROVED",
      "amount": 8500000,
      "approvedAmount": 8500000,
      "description": "Q1 advance for dev tools and API licenses.",
      "requester": {
        "id": 1,
        "fullName": "Nguyen Van An",
        "avatar": "https://res.cloudinary.com/.../signed...",
        "employeeCode": "MK001",
        "jobTitle": "Backend Developer",
        "departmentName": "Engineering",
        "bankName": "Techcombank",
        "bankAccountNum": "19036277381012",
        "bankAccountOwner": "NGUYEN VAN AN"
      },
      "project": {
        "id": 1,
        "projectCode": "PRJ-ERP-2026",
        "name": "ERP Integration"
      },
      "phase": {
        "id": 2,
        "phaseCode": "PH-DEV-02",
        "name": "Phase 2 – Development Sprint"
      },
      "attachments": [
        {
          "fileId": 10,
          "fileName": "jetbrains_invoice_Q1.jpg",
          "url": "https://res.cloudinary.com/.../signed...",
          "fileType": "image/jpeg",
          "size": 245000
        }
      ],
      "createdAt": "2026-02-18T09:00:00"
    }
  ],
  "total": 8,
  "page": 1,
  "limit": 20,
  "totalPages": 1
}
```
> `requester.bankName` / `bankAccountNum` / `bankAccountOwner`: từ `user_profiles` — đầy đủ (unmasked) cho Accountant giải ngân.

---

### GET `/accountant/disbursements/:id`
Chi tiết một disbursement (request đã duyệt). Response giống `GET /manager/approvals/:id` nhưng kèm đầy đủ bank info và approval timeline.

**Response:**
```json
{
  "id": 101,
  "requestCode": "REQ-IT-2602-001",
  "type": "ADVANCE",
  "status": "APPROVED",
  "amount": 8500000,
  "approvedAmount": 8500000,
  "description": "Q1 advance for dev tools and API licenses.",
  "rejectReason": null,
  "requester": {
    "id": 1,
    "fullName": "Nguyen Van An",
    "avatar": "https://res.cloudinary.com/.../signed...",
    "employeeCode": "MK001",
    "jobTitle": "Backend Developer",
    "departmentName": "Engineering",
    "bankName": "Techcombank",
    "bankAccountNum": "19036277381012",
    "bankAccountOwner": "NGUYEN VAN AN"
  },
  "project": {
    "id": 1,
    "projectCode": "PRJ-ERP-2026",
    "name": "ERP Integration"
  },
  "phase": {
    "id": 2,
    "phaseCode": "PH-DEV-02",
    "name": "Phase 2 – Development Sprint",
    "budgetLimit": 80000000,
    "currentSpent": 62500000
  },
  "attachments": [
    {
      "fileId": 10,
      "fileName": "jetbrains_invoice_Q1.jpg",
      "cloudinaryPublicId": "requests/jetbrains_invoice_Q1",
      "url": "https://res.cloudinary.com/.../signed...",
      "fileType": "image/jpeg",
      "size": 245000
    }
  ],
  "timeline": [
    {
      "id": 1,
      "action": "APPROVE",
      "statusAfterAction": "PENDING",
      "actorId": 5,
      "actorName": "Tran Thi Bich Ngoc",
      "comment": "Approved",
      "createdAt": "2026-02-19T10:30:00"
    },
    {
      "id": 2,
      "action": "APPROVE",
      "statusAfterAction": "APPROVED",
      "actorId": 10,
      "actorName": "Le Van Duc",
      "comment": "Final approval",
      "createdAt": "2026-02-20T09:00:00"
    }
  ],
  "createdAt": "2026-02-18T09:00:00",
  "updatedAt": "2026-02-20T09:00:00"
}
```

---

### POST `/accountant/disbursements/:id/disburse`
Xác nhận giải ngân cho một request. Yêu cầu PIN. Backend: trừ `system_funds.total_balance` → cộng `wallets.balance` của employee → tạo 2 `transactions` records (bút toán kép: DEBIT system + CREDIT employee, liên kết qua `related_transaction_id`) → cập nhật `requests.status = PAID`.

**Body:**
```json
{
  "pin": "string",
  "note": "string (optional)"
}
```
**Response:**
```json
{
  "id": 101,
  "requestCode": "REQ-IT-2602-001",
  "status": "PAID",
  "transactionCode": "TXN-8829145A",
  "amount": 8500000,
  "disbursedAt": "2026-02-22T10:00:00"
}
```
> `pin`: PIN 5 chữ số của Accountant — xác thực qua `user_security_settings`.  
> `note`: ghi chú giải ngân → lưu vào `transactions.description`.  
> `transactionCode`: mã giao dịch nội bộ auto-generated.

---

### POST `/accountant/disbursements/:id/reject`
Từ chối giải ngân (revert request về `REJECTED`). Dùng khi phát hiện sai sót chứng từ phút chót.

**Body:**
```json
{ "reason": "string" }
```
**Response:**
```json
{
  "id": 101,
  "requestCode": "REQ-IT-2602-001",
  "status": "REJECTED",
  "rejectReason": "Invalid bank account information"
}
```
> Backend cập nhật `requests.status = REJECTED`, `requests.reject_reason`. Tạo `request_histories`: `action = REJECT`.

---

### GET `/accountant/payroll`
Danh sách tất cả các kỳ lương.

**Params:** `?year=2026&status=DRAFT|PROCESSING|COMPLETED&page=1&limit=12`

**DB mapping:** `payroll_periods` + aggregate COUNT/SUM từ `payslips`.

**Response:**
```json
{
  "items": [
    {
      "id": 3,
      "periodCode": "PR-2026-02",
      "name": "Lương T02/2026",
      "month": 2,
      "year": 2026,
      "startDate": "2026-02-01",
      "endDate": "2026-02-28",
      "status": "DRAFT",
      "employeeCount": 12,
      "totalNetPayroll": 285600000,
      "createdAt": "2026-02-22T10:00:00",
      "updatedAt": "2026-02-22T10:00:00"
    }
  ],
  "total": 14,
  "page": 1,
  "limit": 12,
  "totalPages": 2
}
```
> `id`: `payroll_periods.id` (Long).  
> `periodCode`: `payroll_periods.period_code` — format `PR-{YEAR}-{MM}`.  
> `status`: `PayrollStatus` — `DRAFT | PROCESSING | COMPLETED`.  
> `employeeCount`: COUNT `payslips` WHERE `period_id`.  
> `totalNetPayroll`: SUM `payslips.final_net_salary` WHERE `period_id`.

---

### GET `/accountant/payroll/:periodId`
Chi tiết bảng lương của một kỳ cụ thể, bao gồm toàn bộ entries (payslips).

**DB mapping:** `payroll_periods` + `payslips` JOIN `users` → `user_profiles` → `file_storages` (avatar).

**Response:**
```json
{
  "id": 3,
  "periodCode": "PR-2026-02",
  "name": "Lương T02/2026",
  "month": 2,
  "year": 2026,
  "startDate": "2026-02-01",
  "endDate": "2026-02-28",
  "status": "DRAFT",
  "employeeCount": 12,
  "totalNetPayroll": 285600000,
  "entries": [
    {
      "id": 42,
      "payslipCode": "PSL-MK001-0226",
      "userId": 1,
      "fullName": "Nguyen Van An",
      "avatar": "https://res.cloudinary.com/.../signed...",
      "employeeCode": "MK001",
      "jobTitle": "Backend Developer",
      "baseSalary": 28000000,
      "bonus": 2000000,
      "allowance": 0,
      "deduction": 2800000,
      "advanceDeduct": 8500000,
      "finalNetSalary": 18700000,
      "status": "DRAFT"
    }
  ],
  "createdAt": "2026-02-22T10:00:00",
  "updatedAt": "2026-02-22T10:00:00"
}
```
> `entries[]`: từ `payslips` WHERE `period_id`. Mỗi entry = 1 payslip.  
> `id`: `payslips.id` (Long). `payslipCode`: `payslips.payslip_code`.  
> `baseSalary`, `bonus`, `allowance`, `deduction`, `advanceDeduct`, `finalNetSalary`: map trực tiếp từ `payslips`.  
> `finalNetSalary = baseSalary + bonus + allowance - deduction - advanceDeduct`.  
> `status`: `PayslipStatus` — `DRAFT | PAID`.

---

### POST `/accountant/payroll`
Tạo kỳ lương mới. Backend auto-generate `periodCode`.

**Body:**
```json
{
  "name": "Lương T03/2026",
  "month": 3,
  "year": 2026,
  "startDate": "2026-03-01",
  "endDate": "2026-03-31"
}
```
**Response:**
```json
{
  "id": 4,
  "periodCode": "PR-2026-03",
  "name": "Lương T03/2026",
  "month": 3,
  "year": 2026,
  "startDate": "2026-03-01",
  "endDate": "2026-03-31",
  "status": "DRAFT",
  "employeeCount": 0,
  "totalNetPayroll": 0,
  "entries": [],
  "createdAt": "2026-02-24T09:00:00",
  "updatedAt": "2026-02-24T09:00:00"
}
```
> Trả lỗi `409 Conflict` nếu đã tồn tại period cùng `month` + `year`.

---

### GET `/accountant/payroll/template`
Tải file Excel mẫu để nhập bảng lương. File có sẵn header và format chuẩn.

**Response:** File Excel (`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`) — download trực tiếp.  
> `Content-Disposition: attachment; filename="payroll_template.xlsx"`  
> Các cột: `employeeCode`, `employeeName`, `baseSalary`, `bonus`, `allowance`, `deduction`.  
> **Lưu ý:** `advanceDeduct` KHÔNG có trong template — field này được tính tự động bởi `POST auto-netting`.

---

### POST `/accountant/payroll/:periodId/import`
Import file Excel bảng lương vào kỳ lương đã tạo. Backend parse, validate từng dòng, map `employeeCode` với `user_profiles.employee_code`, tạo `payslips` records.

**Content-Type:** `multipart/form-data`

| Field  | Type | Required | Mô tả                                  |
|--------|------|----------|----------------------------------------|
| `file` | File | ✔        | File `.xlsx` hoặc `.xls`, tối đa 10 MB |

**Response:**
```json
{
  "periodId": 4,
  "periodCode": "PR-2026-03",
  "status": "DRAFT",
  "totalRows": 15,
  "successCount": 13,
  "errorCount": 2,
  "entries": [
    {
      "id": 50,
      "payslipCode": "PSL-MK001-0326",
      "userId": 1,
      "fullName": "Nguyen Van An",
      "employeeCode": "MK001",
      "baseSalary": 28000000,
      "bonus": 2000000,
      "allowance": 0,
      "deduction": 2800000,
      "advanceDeduct": 0,
      "finalNetSalary": 27200000,
      "status": "DRAFT",
      "importStatus": "ok",
      "importError": null
    },
    {
      "id": null,
      "payslipCode": null,
      "userId": null,
      "fullName": "Nguyen Van X",
      "employeeCode": "MK999",
      "baseSalary": 15000000,
      "bonus": 0,
      "allowance": 0,
      "deduction": 0,
      "advanceDeduct": 0,
      "finalNetSalary": 15000000,
      "status": null,
      "importStatus": "error",
      "importError": "Employee not found in system (row 14)"
    }
  ],
  "errors": [
    { "row": 14, "field": "employeeCode", "message": "Employee not found in system" },
    { "row": 7,  "field": "baseSalary",  "message": "baseSalary must be a positive number" }
  ],
  "totalNetPayroll": 285600000
}
```
> `importStatus`: `ok | error`. Dòng lỗi không được tính vào `totalNetPayroll` và không tạo `payslips` record.  
> Sau import, **bắt buộc** gọi `POST /accountant/payroll/:periodId/auto-netting` rồi mới `POST /accountant/payroll/:periodId/run`.  
> Nếu period đã có payslips, trả `409 Conflict` → gọi `POST /accountant/payroll/:periodId/confirm-overwrite` để xác nhận ghi đè.

---

### POST `/accountant/payroll/:periodId/confirm-overwrite`
Xác nhận ghi đè bảng lương đã có. Gọi sau khi nhận `409` từ import. Backend xoá toàn bộ `payslips` WHERE `period_id = :periodId`.

**Body:** —  
**Response:** `{ "message": "Previous payroll data cleared. Ready for re-import." }`

---

### POST `/accountant/payroll/:periodId/auto-netting`
Tự động tính và điền `advanceDeduct` cho từng payslip dựa trên `wallets.debt_balance` của từng nhân viên. **Bắt buộc gọi trước `/run`.**

**Body:** —

**Response:**
```json
{
  "periodId": 3,
  "periodCode": "PR-2026-02",
  "totalAdvanceDeducted": 35500000,
  "summary": [
    {
      "userId": 1,
      "employeeCode": "MK001",
      "fullName": "Nguyen Van An",
      "outstandingDebt": 8500000,
      "deductedAmount": 8500000,
      "remainingDebt": 0,
      "note": "Full clearance"
    },
    {
      "userId": 5,
      "employeeCode": "MK005",
      "fullName": "Le Thi Hoa",
      "outstandingDebt": 30000000,
      "deductedAmount": 10000000,
      "remainingDebt": 20000000,
      "note": "Partial deduction — capped at 50% net salary"
    }
  ]
}
```
> Quy tắc: `deductedAmount = min(outstandingDebt, 50% * (baseSalary + bonus + allowance - deduction))` — đảm bảo nhân viên nhận ít nhất 50% lương.  
> Sau auto-netting, `payslips.advance_deduct` và `payslips.final_net_salary` được cập nhật tự động.  
> Có thể gọi lại (re-netting) bất kỳ lúc nào khi `status = DRAFT`.

---

### POST `/accountant/payroll/:periodId/run`
Chạy tính lương chính thức. Sinh `transactions` (PAYSLIP_PAYMENT) cho từng payslip hợp lệ, chuyển `final_net_salary` vào `wallets.balance` từng nhân viên, trừ `wallets.debt_balance`, trừ `system_funds.total_balance`.

> **Bắt buộc:** auto-netting phải được gọi trước. Nếu chưa → `422 Unprocessable Entity`.

**Body:** —  
**Response:**
```json
{
  "periodId": 3,
  "periodCode": "PR-2026-02",
  "status": "PROCESSING",
  "payslipsGenerated": 12,
  "totalNetPayroll": 285600000
}
```
> Sau khi xử lý xong: `payroll_periods.status = COMPLETED`, `payslips.status = PAID`.

---

### PUT `/accountant/payroll/:periodId/entries/:payslipId`
Chỉnh sửa một dòng lương (payslip) trước khi run. Chỉ cho phép khi `payroll_periods.status = DRAFT`.

**Body:**
```json
{
  "baseSalary": 28000000,
  "bonus": 3000000,
  "allowance": 500000,
  "deduction": 2800000,
  "advanceDeduct": 8500000
}
```
> Tất cả fields optional — chỉ cập nhật field được gửi. Backend auto-recalculate `finalNetSalary`.

**Response:**
```json
{
  "id": 42,
  "payslipCode": "PSL-MK001-0226",
  "userId": 1,
  "fullName": "Nguyen Van An",
  "employeeCode": "MK001",
  "baseSalary": 28000000,
  "bonus": 3000000,
  "allowance": 500000,
  "deduction": 2800000,
  "advanceDeduct": 8500000,
  "finalNetSalary": 20200000,
  "status": "DRAFT"
}
```

---

### GET `/accountant/ledger`
Sổ cái giao dịch hệ thống (transactions từ System Fund wallet).

**Params:** `?type=DEPOSIT|WITHDRAW|REQUEST_PAYMENT|PAYSLIP_PAYMENT|SYSTEM_ADJUSTMENT&status=SUCCESS|PENDING|FAILED&referenceType=REQUEST|PAYSLIP&from=2026-01-01&to=2026-02-28&page=1&limit=20`

**DB mapping:** `transactions` từ system wallet (hoặc toàn bộ nếu cần tổng quan). JOIN `users` (actor).

**Response:**
```json
{
  "items": [
    {
      "id": 501,
      "transactionCode": "TXN-8829145A",
      "type": "REQUEST_PAYMENT",
      "status": "SUCCESS",
      "amount": -8500000,
      "balanceAfter": 1250000000,
      "referenceType": "REQUEST",
      "referenceId": 101,
      "description": "Advance — dev tools Q1",
      "actorId": 20,
      "actorName": "Nguyen Thi Thu Ha",
      "createdAt": "2026-02-19T10:30:00"
    }
  ],
  "total": 20,
  "page": 1,
  "limit": 20,
  "totalPages": 1
}
```
> `id`: `transactions.id` (Long).  
> `transactionCode`: `transactions.transaction_code`.  
> `type`: `TransactionType` — `DEPOSIT | WITHDRAW | REQUEST_PAYMENT | PAYSLIP_PAYMENT | SYSTEM_ADJUSTMENT`.  
> `status`: `TransactionStatus` — `SUCCESS | PENDING | FAILED`.  
> `referenceType`: `ReferenceType` — `REQUEST | PAYSLIP`. Nullable.  
> `referenceId`: `transactions.reference_id` (Long). Nullable.  
> `actorId` / `actorName`: join `users` qua `transactions.actor_id`.

---

### GET `/accountant/ledger/summary`
Tổng hợp số dư và dòng tiền.

**Params:** `?from=2026-02-01&to=2026-02-28`

**DB mapping:** `system_funds` + aggregate SUM từ `transactions`.

**Response:**
```json
{
  "currentBalance": 1250000000,
  "totalInflow": 3500000000,
  "totalOutflow": 2250000000,
  "transactionCount": 156
}
```
> `currentBalance`: `system_funds.total_balance`.  
> `totalInflow` / `totalOutflow`: SUM `transactions.amount` WHERE `amount > 0` / `amount < 0` trong khoảng thời gian.

---

### GET `/accountant/ledger/:transactionId`
Chi tiết một giao dịch trong sổ cái.

**DB mapping:** `transactions` JOIN `users` (actor) + `wallets` → `users` (wallet owner).

**Response:**
```json
{
  "id": 501,
  "transactionCode": "TXN-8829145A",
  "paymentRef": null,
  "gatewayProvider": "INTERNAL_WALLET",
  "type": "REQUEST_PAYMENT",
  "status": "SUCCESS",
  "amount": -8500000,
  "balanceAfter": 1250000000,
  "referenceType": "REQUEST",
  "referenceId": 101,
  "relatedTransactionId": 502,
  "walletId": 1,
  "walletOwnerName": "System Fund",
  "actorId": 20,
  "actorName": "Nguyen Thi Thu Ha",
  "description": "Advance — dev tools Q1",
  "createdAt": "2026-02-19T10:30:00",
  "updatedAt": "2026-02-19T10:30:00"
}
```
> `paymentRef`: `transactions.payment_ref` — mã tham chiếu cổng thanh toán. Nullable.  
> `gatewayProvider`: `PaymentProvider` — `PAYOS | MOMO | VNPAY | ZALOPAY | INTERNAL_WALLET`.  
> `relatedTransactionId`: `transactions.related_transaction_id` — ID giao dịch đối ứng (bút toán kép). Nullable.

---

### GET `/accountant/requests/:requestId`
Chi tiết một request bất kỳ (không giới hạn theo user). Dùng khi Accountant cần xem request liên quan đến disbursement hoặc ledger.

**Response:**
```json
{
  "id": 101,
  "requestCode": "REQ-IT-2602-001",
  "type": "ADVANCE",
  "status": "PAID",
  "amount": 8500000,
  "approvedAmount": 8500000,
  "description": "Q1 advance for dev tools and API licenses.",
  "rejectReason": null,
  "requester": {
    "id": 1,
    "fullName": "Nguyen Van An",
    "avatar": "https://res.cloudinary.com/.../signed...",
    "employeeCode": "MK001",
    "jobTitle": "Backend Developer",
    "departmentName": "Engineering",
    "bankName": "Techcombank",
    "bankAccountNum": "19036277381012",
    "bankAccountOwner": "NGUYEN VAN AN"
  },
  "project": {
    "id": 1,
    "projectCode": "PRJ-ERP-2026",
    "name": "ERP Integration"
  },
  "phase": {
    "id": 2,
    "phaseCode": "PH-DEV-02",
    "name": "Phase 2 – Development Sprint",
    "budgetLimit": 80000000,
    "currentSpent": 62500000
  },
  "attachments": [
    {
      "fileId": 10,
      "fileName": "jetbrains_invoice_Q1.jpg",
      "cloudinaryPublicId": "requests/jetbrains_invoice_Q1",
      "url": "https://res.cloudinary.com/.../signed...",
      "fileType": "image/jpeg",
      "size": 245000
    }
  ],
  "timeline": [
    {
      "id": 1,
      "action": "APPROVE",
      "statusAfterAction": "PENDING",
      "actorId": 5,
      "actorName": "Tran Thi Bich Ngoc",
      "comment": "Approved",
      "createdAt": "2026-02-19T10:30:00"
    },
    {
      "id": 2,
      "action": "APPROVE",
      "statusAfterAction": "APPROVED",
      "actorId": 10,
      "actorName": "Le Van Duc",
      "comment": "Final approval",
      "createdAt": "2026-02-20T09:00:00"
    }
  ],
  "createdAt": "2026-02-18T09:00:00",
  "updatedAt": "2026-02-22T10:00:00"
}
```

---

### GET `/accountant/payslips/:payslipId`
Chi tiết một payslip cụ thể. Dùng khi Accountant cần tra cứu payslip từ ledger → `referenceId`.

**DB mapping:** `payslips` JOIN `payroll_periods` + `users` → `user_profiles` + `departments`.

**Response:**
```json
{
  "id": 42,
  "payslipCode": "PSL-MK001-0226",
  "periodId": 3,
  "periodCode": "PR-2026-02",
  "periodName": "Lương T02/2026",
  "month": 2,
  "year": 2026,
  "status": "PAID",
  "baseSalary": 28000000,
  "bonus": 2000000,
  "allowance": 0,
  "totalEarnings": 30000000,
  "deduction": 2800000,
  "advanceDeduct": 8500000,
  "totalDeduction": 11300000,
  "finalNetSalary": 18700000,
  "employee": {
    "id": 1,
    "fullName": "Nguyen Van An",
    "employeeCode": "MK001",
    "departmentName": "Engineering",
    "jobTitle": "Backend Developer",
    "bankName": "Techcombank",
    "bankAccountNum": "****1012"
  },
  "createdAt": "2026-02-22T10:00:00",
  "updatedAt": "2026-02-25T10:00:00"
}
```
> `totalEarnings = baseSalary + bonus + allowance` (computed).  
> `totalDeduction = deduction + advanceDeduct` (computed).  
> `employee.bankAccountNum`: masked — chỉ hiển thị 4 số cuối.

---

## 5. ADMIN

---

### GET `/admin/approvals`
Danh sách requests chờ admin duyệt (`status = PENDING_ADMIN`).

**Params:** `?type=ADVANCE|EXPENSE|REIMBURSE|QUOTA_TOPUP&search=string&page=1&limit=20`

**DB mapping:** `requests` WHERE `status = PENDING_ADMIN` JOIN `users` (requester) + `user_profiles` + `projects` + `project_phases` + `request_attachments` → `file_storages`.

**Response:**
```json
{
  "items": [
    {
      "id": 101,
      "requestCode": "REQ-IT-2602-001",
      "type": "ADVANCE",
      "status": "PENDING_ADMIN",
      "amount": 85000000,
      "description": "Large advance for server migration.",
      "requester": {
        "id": 1,
        "fullName": "Nguyen Van An",
        "avatar": "https://res.cloudinary.com/.../signed...",
        "employeeCode": "MK001",
        "jobTitle": "Backend Developer",
        "email": "van.an@ifms.vn"
      },
      "project": {
        "id": 1,
        "projectCode": "PRJ-ERP-2026",
        "name": "ERP Integration"
      },
      "phase": {
        "id": 2,
        "phaseCode": "PH-DEV-02",
        "name": "Phase 2 – Development",
        "budgetLimit": 80000000,
        "currentSpent": 62500000
      },
      "attachments": [
        {
          "fileId": 10,
          "fileName": "server_quote.pdf",
          "url": "https://res.cloudinary.com/.../signed...",
          "fileType": "application/pdf",
          "size": 310000
        }
      ],
      "createdAt": "2026-02-18T09:15:00"
    }
  ],
  "total": 3,
  "page": 1,
  "limit": 20,
  "totalPages": 1
}
```

---

### GET `/admin/approvals/:id`
Chi tiết một request cần admin duyệt.

**Response:**
```json
{
  "id": 101,
  "requestCode": "REQ-IT-2602-001",
  "type": "ADVANCE",
  "status": "PENDING_ADMIN",
  "amount": 85000000,
  "approvedAmount": null,
  "description": "Large advance for server migration.",
  "rejectReason": null,
  "requester": {
    "id": 1,
    "fullName": "Nguyen Van An",
    "avatar": "https://res.cloudinary.com/.../signed...",
    "employeeCode": "MK001",
    "jobTitle": "Backend Developer",
    "email": "van.an@ifms.vn",
    "departmentName": "Engineering"
  },
  "project": {
    "id": 1,
    "projectCode": "PRJ-ERP-2026",
    "name": "ERP Integration"
  },
  "phase": {
    "id": 2,
    "phaseCode": "PH-DEV-02",
    "name": "Phase 2 – Development",
    "budgetLimit": 80000000,
    "currentSpent": 62500000
  },
  "attachments": [
    {
      "fileId": 10,
      "fileName": "server_quote.pdf",
      "cloudinaryPublicId": "requests/server_quote",
      "url": "https://res.cloudinary.com/.../signed...",
      "fileType": "application/pdf",
      "size": 310000
    }
  ],
  "timeline": [
    {
      "id": 1,
      "action": "APPROVE",
      "statusAfterAction": "PENDING",
      "actorId": 5,
      "actorName": "Tran Thi Bich Ngoc",
      "comment": "Escalated to admin — exceeds manager limit",
      "createdAt": "2026-02-19T14:00:00"
    }
  ],
  "createdAt": "2026-02-18T09:15:00",
  "updatedAt": "2026-02-19T14:00:00"
}
```

---

### POST `/admin/approvals/:id/approve`
Admin duyệt request (chuyển sang `APPROVED`).

**Body:**
```json
{ "comment": "string (optional)" }
```
**Response:**
```json
{
  "id": 101,
  "requestCode": "REQ-IT-2602-001",
  "status": "APPROVED",
  "comment": "Final approval granted."
}
```
> Backend tạo `request_histories`: `action = APPROVE`, `status_after_action = APPROVED`.

---

### POST `/admin/approvals/:id/reject`
Admin từ chối request.

**Body:**
```json
{ "reason": "string" }
```
**Response:**
```json
{
  "id": 101,
  "requestCode": "REQ-IT-2602-001",
  "status": "REJECTED",
  "rejectReason": "Policy violation"
}
```

---

### GET `/admin/users`
Danh sách tất cả users trong hệ thống.

**Params:** `?role=EMPLOYEE|MANAGER|ACCOUNTANT|ADMIN&departmentId=1&status=ACTIVE|LOCKED|PENDING&search=nguyen&page=1&limit=20`

**DB mapping:** `users` JOIN `roles` + `departments` + `user_profiles` + LEFT JOIN `wallets`.

**Response:**
```json
{
  "items": [
    {
      "id": 1,
      "fullName": "Nguyen Van An",
      "email": "van.an@ifms.vn",
      "employeeCode": "MK001",
      "role": "EMPLOYEE",
      "departmentId": 1,
      "departmentName": "Engineering",
      "jobTitle": "Backend Developer",
      "avatar": "https://res.cloudinary.com/.../signed...",
      "debtBalance": 0,
      "status": "ACTIVE",
      "createdAt": "2024-01-15T00:00:00"
    }
  ],
  "total": 21,
  "page": 1,
  "limit": 20,
  "totalPages": 2
}
```
> `id`: `users.id` (Long).  
> `role`: `roles.name`.  
> `employeeCode`: `user_profiles.employee_code`.  
> `debtBalance`: `wallets.debt_balance`. `0` nếu chưa có ví.

---

### GET `/admin/users/:id`
Chi tiết đầy đủ một user.

**DB mapping:** `users` JOIN `roles` + `departments` + `user_profiles` → `file_storages` + `user_security_settings` + `wallets`.

**Response:**
```json
{
  "id": 1,
  "fullName": "Nguyen Van An",
  "email": "van.an@ifms.vn",
  "employeeCode": "MK001",
  "role": "EMPLOYEE",
  "departmentId": 1,
  "departmentName": "Engineering",
  "jobTitle": "Backend Developer",
  "phoneNumber": "+84 901 234 567",
  "dateOfBirth": "1995-06-20",
  "citizenId": "079012345678",
  "address": "123 Nguyen Trai, Thanh Xuan, Ha Noi",
  "avatar": "https://res.cloudinary.com/.../signed...",
  "status": "ACTIVE",
  "isFirstLogin": false,
  "bankInfo": {
    "bankName": "MB Bank",
    "accountNumber": "0123456789",
    "accountOwner": "NGUYEN VAN AN"
  },
  "wallet": {
    "balance": 10250000,
    "pendingBalance": 0,
    "debtBalance": 8500000
  },
  "securitySettings": {
    "hasPIN": true,
    "pinLockedUntil": null,
    "retryCount": 0
  },
  "createdAt": "2024-01-15T00:00:00",
  "updatedAt": "2026-02-20T14:00:00"
}
```
> `wallet`: từ `wallets` WHERE `user_id = user.id`. Nullable nếu chưa có ví.  
> `securitySettings.hasPIN`: `true` nếu `user_security_settings.transaction_pin IS NOT NULL`.  
> `securitySettings.pinLockedUntil`: `user_security_settings.locked_until`.  
> `securitySettings.retryCount`: `user_security_settings.retry_count`.

---

### POST `/admin/users`
Tạo tài khoản mới. Hệ thống tự generate mật khẩu tạm (BCrypt hash), gửi qua email. Auto-create `user_profiles`, `user_security_settings`, `wallet`.

**Body:**
```json
{
  "fullName": "Nguyen Van X",
  "email": "van.x@ifms.vn",
  "roleId": 1,
  "departmentId": 1
}
```
> `roleId`: `roles.id` (Long).  
> `departmentId`: `departments.id` (Long). Optional — nullable nếu chưa gán phòng ban.

**Response:**
```json
{
  "id": 22,
  "fullName": "Nguyen Van X",
  "email": "van.x@ifms.vn",
  "role": "EMPLOYEE",
  "departmentId": 1,
  "departmentName": "Engineering",
  "status": "ACTIVE",
  "isFirstLogin": true,
  "createdAt": "2026-02-22T10:00:00"
}
```

---

### PUT `/admin/users/:id`
Cập nhật thông tin user (role, department, tên).

**Body:**
```json
{
  "fullName": "string (optional)",
  "roleId": 2,
  "departmentId": 1
}
```
> Tất cả fields optional. Backend tạo `audit_logs` record: `action = USER_UPDATED`.

**Response:**
```json
{
  "id": 1,
  "fullName": "Nguyen Van An",
  "email": "van.an@ifms.vn",
  "employeeCode": "MK001",
  "role": "EMPLOYEE",
  "departmentId": 1,
  "departmentName": "Engineering",
  "jobTitle": "Backend Developer",
  "phoneNumber": "+84 901 234 567",
  "dateOfBirth": "1995-06-20",
  "citizenId": "079012345678",
  "address": "123 Nguyen Trai, Thanh Xuan, Ha Noi",
  "avatar": "https://res.cloudinary.com/.../signed...",
  "status": "ACTIVE",
  "isFirstLogin": false,
  "bankInfo": {
    "bankName": "MB Bank",
    "accountNumber": "0123456789",
    "accountOwner": "NGUYEN VAN AN"
  },
  "wallet": {
    "balance": 10250000,
    "pendingBalance": 0,
    "debtBalance": 8500000
  },
  "securitySettings": {
    "hasPIN": true,
    "pinLockedUntil": null,
    "retryCount": 0
  },
  "createdAt": "2024-01-15T00:00:00",
  "updatedAt": "2026-02-24T14:00:00"
}
```

---

### POST `/admin/users/:id/lock`
Khoá tài khoản (`status = LOCKED`). User không thể đăng nhập.

**Response:**
```json
{ "id": 8, "status": "LOCKED" }
```
> Backend tạo `audit_logs`: `action = USER_LOCKED`.

---

### POST `/admin/users/:id/unlock`
Mở khoá tài khoản (`status = ACTIVE`).

**Response:**
```json
{ "id": 8, "status": "ACTIVE" }
```
> Backend tạo `audit_logs`: `action = USER_UNLOCKED`.

---

### POST `/admin/users/:id/reset-password`
Reset mật khẩu về mật khẩu tạm, gửi qua email. Set `is_first_login = true`.

**Response:** `{ "message": "Temporary password sent to user email" }`

---

### GET `/admin/departments`
Danh sách department.

**Params:** `?search=string&page=1&limit=20`

**DB mapping:** `departments` JOIN `users` (manager) + COUNT `users` WHERE `department_id`.

**Response:**
```json
{
  "items": [
    {
      "id": 1,
      "name": "Engineering",
      "code": "ENG",
      "manager": {
        "id": 5,
        "fullName": "Tran Thi Bich Ngoc"
      },
      "employeeCount": 12,
      "totalProjectQuota": 500000000,
      "totalAvailableBalance": 150000000,
      "createdAt": "2024-01-01T00:00:00"
    }
  ],
  "total": 5,
  "page": 1,
  "limit": 20,
  "totalPages": 1
}
```
> `id`: `departments.id` (Long).  
> `code`: `departments.code` — mã phòng ban unique.  
> `manager`: nullable nếu chưa gán.  
> `totalProjectQuota`: `departments.total_project_quota` — tổng ngân sách đã cấp.  
> `totalAvailableBalance`: `departments.total_available_balance` — ngân sách còn lại.  
> `employeeCount`: COUNT `users` WHERE `department_id = dept.id`.

---

### GET `/admin/departments/:id`
Chi tiết department kèm danh sách members.

**Response:**
```json
{
  "id": 1,
  "name": "Engineering",
  "code": "ENG",
  "manager": {
    "id": 5,
    "fullName": "Tran Thi Bich Ngoc"
  },
  "totalProjectQuota": 500000000,
  "totalAvailableBalance": 150000000,
  "members": [
    {
      "id": 1,
      "fullName": "Nguyen Van An",
      "employeeCode": "MK001",
      "email": "van.an@ifms.vn",
      "jobTitle": "Backend Developer",
      "avatar": "https://res.cloudinary.com/.../signed...",
      "status": "ACTIVE"
    }
  ],
  "createdAt": "2024-01-01T00:00:00",
  "updatedAt": "2026-02-20T14:00:00"
}
```
> `members[]`: `users` WHERE `department_id = dept.id` JOIN `user_profiles` + `file_storages`.

---

### POST `/admin/departments`
Tạo department mới. Backend auto-generate `code`.

**Body:**
```json
{
  "name": "Data & Analytics",
  "code": "DNA",
  "managerId": 15,
  "totalProjectQuota": 80000000
}
```
> `managerId`: optional — `users.id` (Long). Nếu không truyền, department chưa có manager.  
> `totalProjectQuota`: optional, default `0`.  
> `code`: optional nếu muốn auto-generate.  
> Trả lỗi `409 Conflict` nếu `name` hoặc `code` đã tồn tại.

**Response:**
```json
{
  "id": 6,
  "name": "Data & Analytics",
  "code": "DNA",
  "manager": {
    "id": 15,
    "fullName": "Pham Van Tuan"
  },
  "totalProjectQuota": 80000000,
  "totalAvailableBalance": 80000000,
  "members": [],
  "createdAt": "2026-02-24T10:00:00",
  "updatedAt": "2026-02-24T10:00:00"
}
```

---

### PUT `/admin/departments/:id`
Cập nhật department.

**Body:**
```json
{
  "name": "string (optional)",
  "managerId": 5,
  "totalProjectQuota": 200000000
}
```
> Backend tạo `audit_logs`: `action = DEPARTMENT_UPDATED` hoặc `QUOTA_ADJUSTED` nếu thay đổi quota.

**Response:**
```json
{
  "id": 1,
  "name": "Engineering",
  "code": "ENG",
  "manager": {
    "id": 5,
    "fullName": "Tran Thi Bich Ngoc"
  },
  "totalProjectQuota": 200000000,
  "totalAvailableBalance": 150000000,
  "members": [
    {
      "id": 1,
      "fullName": "Nguyen Van An",
      "employeeCode": "MK001",
      "email": "van.an@ifms.vn",
      "jobTitle": "Backend Developer",
      "avatar": "https://res.cloudinary.com/.../signed...",
      "status": "ACTIVE"
    }
  ],
  "createdAt": "2024-01-01T00:00:00",
  "updatedAt": "2026-02-24T14:00:00"
}
```

---

### GET `/admin/audit`
Lịch sử audit log toàn hệ thống.

**Params:** `?actorId=1&action=USER_CREATED|USER_LOCKED|USER_UNLOCKED|DEPARTMENT_CREATED|QUOTA_TOPUP|CONFIG_UPDATED|ROLE_ASSIGNED&entityName=users|departments|system_configs&from=2026-01-01&to=2026-02-28&page=1&limit=50`

**DB mapping:** `audit_logs` JOIN `users` (actor).

**Response:**
```json
{
  "items": [
    {
      "id": 1,
      "actorId": 10,
      "actorName": "Le Van Duc",
      "action": "USER_LOCKED",
      "entityName": "users",
      "entityId": "8",
      "oldValues": { "status": "ACTIVE" },
      "newValues": { "status": "LOCKED" },
      "createdAt": "2026-02-22T10:00:00"
    }
  ],
  "total": 342,
  "page": 1,
  "limit": 50,
  "totalPages": 7
}
```
> `id`: `audit_logs.id` (Long).  
> `actorId` / `actorName`: join `users` qua `audit_logs.actor_id`. Nullable (system-triggered).  
> `action`: `AuditAction` enum — `USER_CREATED | USER_UPDATED | USER_LOCKED | USER_UNLOCKED | BANK_INFO_UPDATED | ROLE_ASSIGNED | ROLE_REVOKED | PERMISSION_GRANTED | PERMISSION_REVOKED | DEPARTMENT_CREATED | DEPARTMENT_UPDATED | DEPARTMENT_DELETED | QUOTA_TOPUP | QUOTA_ADJUSTED | CONFIG_UPDATED | SYSTEM_FUND_ADJUSTED | PIN_RESET | PIN_LOCKED | USER_LOGIN_SUCCESS | USER_LOGIN_FAILED | DATA_EXPORTED | MANUAL_ADJUSTMENT`.  
> `entityName`: tên bảng bị tác động (VD: `users`, `departments`, `system_configs`).  
> `entityId`: ID dòng dữ liệu bị tác động (String).  
> `oldValues` / `newValues`: JSON snapshot trạng thái trước/sau. Nullable.

---

### GET `/admin/settings`
Lấy cấu hình hệ thống. Mỗi config là một record trong `system_configs`.

**DB mapping:** `system_configs` — trả về dạng object aggregated.

**Response:**
```json
{
  "items": [
    { "key": "WITHDRAWAL_LIMIT", "value": "50000000", "description": "Hạn mức rút tiền tối đa mỗi lần (VND)" },
    { "key": "MINIMUM_WITHDRAWAL", "value": "10000", "description": "Số tiền rút tối thiểu (VND)" },
    { "key": "MAX_FILE_SIZE_MB", "value": "5", "description": "Dung lượng file upload tối đa (MB)" },
    { "key": "MAX_FILES_PER_REQUEST", "value": "5", "description": "Số file tối đa mỗi request" },
    { "key": "MANAGER_APPROVAL_LIMIT", "value": "50000000", "description": "Hạn mức Manager duyệt (VND), vượt sẽ escalate lên Admin" },
    { "key": "PIN_MAX_RETRY", "value": "5", "description": "Số lần nhập sai PIN tối đa trước khi khoá" },
    { "key": "PIN_LOCK_DURATION_MINUTES", "value": "30", "description": "Thời gian khoá PIN (phút)" },
    { "key": "REQUIRE_PIN_FOR_WITHDRAWAL", "value": "true", "description": "Yêu cầu PIN khi rút tiền" }
  ]
}
```
> Mỗi item = 1 record `system_configs`. `key` = PK.

---

### PUT `/admin/settings`
Cập nhật cấu hình hệ thống. Gửi danh sách key-value cần cập nhật.

**Body:**
```json
{
  "configs": [
    { "key": "WITHDRAWAL_LIMIT", "value": "100000000" },
    { "key": "PIN_MAX_RETRY", "value": "3" }
  ]
}
```
> Chỉ cập nhật các key được gửi. Backend tạo `audit_logs`: `action = CONFIG_UPDATED` cho mỗi key thay đổi.

**Response:**
```json
{
  "items": [
    { "key": "WITHDRAWAL_LIMIT", "value": "100000000", "description": "Hạn mức rút tiền tối đa mỗi lần (VND)" },
    { "key": "MINIMUM_WITHDRAWAL", "value": "10000", "description": "Số tiền rút tối thiểu (VND)" },
    { "key": "MAX_FILE_SIZE_MB", "value": "5", "description": "Dung lượng file upload tối đa (MB)" },
    { "key": "MAX_FILES_PER_REQUEST", "value": "5", "description": "Số file tối đa mỗi request" },
    { "key": "MANAGER_APPROVAL_LIMIT", "value": "50000000", "description": "Hạn mức Manager duyệt (VND), vượt sẽ escalate lên Admin" },
    { "key": "PIN_MAX_RETRY", "value": "3", "description": "Số lần nhập sai PIN tối đa trước khi khoá" },
    { "key": "PIN_LOCK_DURATION_MINUTES", "value": "30", "description": "Thời gian khoá PIN (phút)" },
    { "key": "REQUIRE_PIN_FOR_WITHDRAWAL", "value": "true", "description": "Yêu cầu PIN khi rút tiền" }
  ]
}
```

---

## 6. WEBSOCKET – Real-time Channels

> **Transport:** SockJS + STOMP over WebSocket (Spring Boot `spring-boot-starter-websocket`).  
> **Endpoint kết nối:** `wss://api.ifms.vn/ws` — Client gửi `Authorization: Bearer <accessToken>` qua STOMP `CONNECT` header.  
> **Thư viện FE:** `@stomp/stompjs` + `sockjs-client` (hoặc native WebSocket).  
> **Quy ước:** Mỗi user subscribe vào các channel riêng theo `userId`. Backend publish message qua `SimpMessagingTemplate.convertAndSendToUser()`.

---

### Channel 1: `/user/queue/wallet` — Live Wallet Balance

**Mục đích:** Cập nhật số dư ví real-time khi có giao dịch thành công (giải ngân, chi lương, nạp tiền, rút tiền).

**Trigger (Backend publish khi):**
- `POST /accountant/disbursements/:id/disburse` → employee nhận tiền
- `POST /accountant/payroll/:periodId/run` → batch publish cho tất cả employee có payslip
- `POST /wallet/withdraw` → chính user rút tiền (confirm từ gateway callback)
- Webhook deposit (nạp tiền qua VietQR) → confirm từ payment gateway
- `SYSTEM_ADJUSTMENT` transaction được tạo

**Subscribe:** `/user/queue/wallet`  
> STOMP route tự động prefix `/user/{userId}` — mỗi user chỉ nhận message của mình.

**Message payload:**
```json
{
  "type": "WALLET_UPDATED",
  "data": {
    "walletId": 1,
    "balance": 20000000,
    "pendingBalance": 0,
    "debtBalance": 0,
    "version": 12,
    "transaction": {
      "id": 501,
      "transactionCode": "TXN-8829145A",
      "type": "PAYSLIP_PAYMENT",
      "status": "SUCCESS",
      "amount": 15000000,
      "balanceAfter": 20000000,
      "referenceType": "PAYSLIP",
      "referenceId": 42,
      "description": "Lương T02/2026",
      "createdAt": "2026-02-25T10:00:00"
    }
  },
  "timestamp": "2026-02-25T10:00:00"
}
```
> `balance`, `pendingBalance`, `debtBalance`, `version`: snapshot mới nhất từ `wallets` sau giao dịch.  
> `transaction`: thông tin giao dịch vừa SUCCESS — FE dùng để hiển thị toast/animation.  
> `transaction.type`: xác định loại hiệu ứng — `PAYSLIP_PAYMENT` / `REQUEST_PAYMENT` → "Ting ting" + flash xanh lá, `WITHDRAW` → flash cam, `DEPOSIT` → flash xanh dương.

**FE xử lý:**
1. Nhận message → cập nhật wallet state (Redux/Zustand) → re-render số dư.
2. Hiển thị toast notification: _"+ 15.000.000đ — Lương T02/2026"_.
3. Animate số dư (count-up từ giá trị cũ → giá trị mới).
4. Phát âm thanh "Ting ting" (nếu `amount > 0`).
5. Cập nhật `version` để đảm bảo Optimistic Lock consistency.

---

### Channel 2: `/user/queue/requests` — Live Request Status

**Mục đích:** Cập nhật trạng thái request real-time khi Manager/Admin approve/reject hoặc Accountant giải ngân.

**Trigger (Backend publish khi):**
- `POST /manager/approvals/:id/approve` → gửi cho requester
- `POST /manager/approvals/:id/reject` → gửi cho requester
- `POST /admin/approvals/:id/approve` → gửi cho requester
- `POST /admin/approvals/:id/reject` → gửi cho requester
- `POST /accountant/disbursements/:id/disburse` → gửi cho requester (`status: PAID`)
- `POST /accountant/disbursements/:id/reject` → gửi cho requester

**Subscribe:** `/user/queue/requests`

**Message payload:**
```json
{
  "type": "REQUEST_STATUS_CHANGED",
  "data": {
    "id": 101,
    "requestCode": "REQ-IT-2602-001",
    "previousStatus": "PENDING_MANAGER",
    "newStatus": "APPROVED",
    "approvedAmount": 8500000,
    "rejectReason": null,
    "actor": {
      "id": 5,
      "fullName": "Tran Thi Bich Ngoc",
      "role": "MANAGER"
    },
    "comment": "Approved — looks good.",
    "updatedAt": "2026-02-25T10:30:00"
  },
  "timestamp": "2026-02-25T10:30:00"
}
```
> `previousStatus` / `newStatus`: trạng thái trước/sau — FE dùng để animate badge transition.  
> `actor`: người thực hiện action — hiển thị trong toast _"Tran Thi Bich Ngoc đã duyệt đơn REQ-IT-2602-001"_.  
> `rejectReason`: chỉ có khi `newStatus = REJECTED`.  
> `approvedAmount`: chỉ có khi approve (có thể khác `amount` gốc).  
> `comment`: ghi chú từ người duyệt. Nullable.

**FE xử lý:**
1. Nhận message → cập nhật request status trong state/cache.
2. Nếu đang ở trang request detail (`/requests/101`) → animate badge color transition (vàng → xanh / đỏ).
3. Nếu đang ở trang request list → cập nhật row tương ứng + update request summary counters.
4. Hiển thị toast: _"Đơn REQ-IT-2602-001 đã được duyệt bởi Tran Thi Bich Ngoc"_.
5. Cập nhật notification badge (unread +1).

---

### Channel 3: `/user/queue/notifications` — Live Notifications

**Mục đích:** Push notification real-time (thay vì polling `GET /notifications`).

**Trigger:** Mỗi khi Backend tạo record mới trong bảng `notifications`.

**Subscribe:** `/user/queue/notifications`

**Message payload:**
```json
{
  "type": "NEW_NOTIFICATION",
  "data": {
    "id": 55,
    "type": "REQUEST_APPROVED",
    "title": "Request Approved",
    "message": "Your request REQ-IT-2602-001 has been approved by manager",
    "isRead": false,
    "refId": 101,
    "refType": "REQUEST",
    "createdAt": "2026-02-25T10:30:00"
  },
  "timestamp": "2026-02-25T10:30:00"
}
```
> Payload giống 1 item trong `GET /notifications` response.  
> FE nhận → prepend vào notification list + tăng `unreadCount` badge.

---

### Backend Implementation Notes

**Spring Boot config:**
```
// WebSocketConfig.java
@EnableWebSocketMessageBroker
- Registry: /ws (SockJS fallback)
- Application prefix: /app
- User destination prefix: /user
- STOMP broker: /queue, /topic
```

**Publish pattern:**
```java
// Khi disbursement thành công:
messagingTemplate.convertAndSendToUser(
    userId.toString(),          // username = userId
    "/queue/wallet",            // destination
    walletUpdateMessage         // payload
);

// Khi request status thay đổi:
messagingTemplate.convertAndSendToUser(
    requesterId.toString(),
    "/queue/requests",
    requestStatusMessage
);

// Batch payroll (50 người):
payslips.forEach(payslip -> {
    messagingTemplate.convertAndSendToUser(
        payslip.getUserId().toString(),
        "/queue/wallet",
        buildWalletMessage(payslip)
    );
});
```

**Authentication:** Intercept STOMP `CONNECT` frame → extract JWT from header → validate → set `Principal.name = userId`. Reject nếu token invalid/expired.

**Reconnection:** FE xử lý auto-reconnect (exponential backoff: 1s → 2s → 4s → 8s → max 30s). Khi reconnect thành công → gọi `GET /wallet` + `GET /notifications` để sync lại state (tránh miss message khi offline).
