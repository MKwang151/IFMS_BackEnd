# 📋 RÀNG BUỘC NGHIỆP VỤ TỔNG THỂ (Business Constraints)

> **Dự án:** Internal Finance Management System (IFMS)
> **Cập nhật:** 07/03/2026
> **Mục đích:** File này mô tả MỌI quy tắc nghiệp vụ xuyên suốt hệ thống mà BẤT KỲ module nào cũng phải tuân thủ. AI Agent PHẢI đọc file này trước khi triển khai bất kỳ logic nào.

---

## 1. KIẾN TRÚC NGÂN SÁCH PHÂN QUYỀN (Decentralized Budget Management)

### 1.1. Mô hình 4 tầng Fund (Top-Down)
```
Tầng 1: System Fund (Quỹ Tổng — Admin)
    │
    │ QUOTA_TOPUP (Manager xin, Admin duyệt)
    ▼
Tầng 2: Department Fund (Quỹ Phòng ban — Manager)
    │
    │ PROJECT_TOPUP (Team Leader xin, Manager duyệt)
    ▼
Tầng 3: Project Fund → Category Budgets (Team Leader quản lý)
    │
    │ ADVANCE/EXPENSE/REIMBURSE (Member xin, Team Leader duyệt)
    ▼
Tầng 4: Personal Wallet (Ví cá nhân — Nhận giải ngân từ Accountant)
```

### 1.2. Mapping sang Database
| Tầng | Table/Field | Chốt chặn cho |
|:---:|:---|:---|
| 1 | `system_funds.total_balance` | Luồng 3 (QUOTA_TOPUP) |
| 2 | `departments.total_available_balance` | Luồng 2 (PROJECT_TOPUP) |
| 3 | `projects.available_budget` | — (tổng quỹ dự án) |
| 3.1 | `phase_category_budgets.budget_limit - current_spent` | Luồng 1 (chi tiêu Member) |
| 4 | `wallets.balance` | — (nhận tiền giải ngân) |

---

## 2. 🚫 KHÔNG CÓ CƠ CHẾ LEO THANG (NO Escalation — Absolute Rule)

Đây là nguyên tắc kiến trúc QUAN TRỌNG NHẤT, ảnh hưởng đến TOÀN BỘ logic:

| KHÔNG được có | Thay thế bằng |
|:---|:---|
| Biến `MANAGER_LIMIT`, `TIER1_LIMIT`, `TIER2_LIMIT` | **Số dư khả dụng** của quỹ tương ứng |
| Nhiều trạng thái pending (`PENDING_MANAGER`, `PENDING_ADMIN`) | **DUY NHẤT** `PENDING_APPROVAL` |
| Action `ESCALATE` | Xóa hoàn toàn |
| Manager/Admin can thiệp đơn chi tiêu Member | **KHÔNG BAO GIỜ** xảy ra |

### Nguyên tắc: "Ai quản lý quỹ nào → toàn quyền duyệt đơn rút tiền từ quỹ đó"
| Quỹ | Người quản lý | Duyệt đơn |
|:---|:---|:---|
| Category Budget (Phase) | **Team Leader** | MỌI đơn chi tiêu Member, bất kể số tiền |
| Department Fund | **Manager** | MỌI đơn xin cấp vốn dự án của Team Leader |
| System Fund | **Admin** | MỌI đơn xin cấp vốn phòng ban của Manager |

### Chốt chặn an toàn DUY NHẤT = Balance Limit
- Hệ thống tự động **CHẶN tạo đơn** nếu `amount > available_balance` của quỹ tương ứng.
- Không cần bất kỳ hạn mức nào khác.

---

## 3. HỆ THỐNG 5 VAI TRÒ (Role Hierarchy)

| # | Role | Trách nhiệm chính | KHÔNG được làm |
|:---:|:---|:---|:---|
| 1 | **ADMIN** | Cấp Quota cho Phòng ban, quản trị hệ thống | Can thiệp đơn chi tiêu cá nhân |
| 2 | **MANAGER** | Tạo project + chỉ định Team Leader, cấp vốn cho Dự án | Can thiệp đơn chi tiêu cá nhân, thêm member/phase |
| 3 | **TEAM_LEADER** | Quản lý nội bộ dự án (thêm member, tạo phase, chia budget), duyệt MỌI chi tiêu Member | Tạo project, đổi Team Leader |
| 4 | **EMPLOYEE** | Tạo Request chi tiêu | Duyệt đơn, quản lý quỹ |
| 5 | **ACCOUNTANT** | Kiểm tra chứng từ, giải ngân, chi lương | Duyệt nghiệp vụ (chỉ kiểm tra chứng từ) |

### Workflow tạo Project:
```
Manager tạo project (name, description, totalBudget, teamLeaderId)
  → Backend auto-tạo project_members record (LEADER)
  → Team Leader nhận project
  → Team Leader thêm Members (chọn từ department)
  → Team Leader tạo Phases
  → Team Leader gán Category Budget cho Phases
  → Members bắt đầu tạo Request chi tiêu
```

### Nhánh hoạt động song song:
- **Nhánh Vận hành:** Admin → Manager → Team Leader → Member (ra quyết định chi tiêu)
- **Nhánh Kế toán:** Accountant (thực thi giải ngân, độc lập với nhánh vận hành)

---

## 4. BA LUỒNG YÊU CẦU (3 Request Flows)

### Luồng 1: Chi tiêu cá nhân (ADVANCE / EXPENSE / REIMBURSE)
```
Member tạo đơn
  → Validate: amount ≤ Category Budget remaining
  → Status: PENDING_APPROVAL (chờ Team Leader)
  → Team Leader APPROVE → PENDING_ACCOUNTANT
  → Accountant PAYOUT → PAID ✅ (Project Fund → Personal Wallet)
```
**Ràng buộc:**
- BẮT BUỘC: `project_id`, `phase_id`, `category_id`
- EXPENSE/REIMBURSE: BẮT BUỘC có file đính kèm (chứng từ)
- Manager/Admin **KHÔNG BAO GIỜ** xuất hiện trong luồng này

### Luồng 2: Xin cấp vốn Dự án (PROJECT_TOPUP)
```
Team Leader tạo đơn
  → Validate: amount ≤ Department available_balance
  → Status: PENDING_APPROVAL (chờ Manager)
  → Manager APPROVE → APPROVED → PAID ✅ (auto: Dept Fund -= amount, Project Fund += amount)
```
**Ràng buộc:**
- BẮT BUỘC: `project_id`
- `phase_id` = NULL, `category_id` = NULL
- Không qua Accountant — hệ thống tự cập nhật balance

### Luồng 3: Xin cấp vốn Phòng ban (QUOTA_TOPUP)
```
Manager tạo đơn
  → Validate: amount ≤ SystemFund total_balance
  → Status: PENDING_APPROVAL (chờ Admin)
  → Admin APPROVE → APPROVED → PAID ✅ (auto: System Fund -= amount, Dept Fund += amount)
```
**Ràng buộc:**
- `project_id` = NULL, `phase_id` = NULL, `category_id` = NULL
- Không qua Accountant — hệ thống tự cập nhật balance

### Status Enum thống nhất (6 values)
| Status | Ý nghĩa | Chuyển từ |
|:---|:---|:---|
| `PENDING_APPROVAL` | Chờ 1 cấp duyệt (TL/Manager/Admin tùy type) | Tạo mới |
| `PENDING_ACCOUNTANT` | Chỉ Luồng 1 — chờ Kế toán | `PENDING_APPROVAL` (sau TL approve) |
| `APPROVED` | Đã duyệt nghiệp vụ | `PENDING_APPROVAL` (Luồng 2&3) |
| `PAID` | Đã giải ngân / cấp vốn xong | `PENDING_ACCOUNTANT` hoặc `APPROVED` |
| `REJECTED` | Bị từ chối | `PENDING_APPROVAL` hoặc `PENDING_ACCOUNTANT` |
| `CANCELLED` | Người tạo tự hủy | Chỉ từ `PENDING_APPROVAL` |

---

## 5. QUY TẮC TÀI CHÍNH

### 5.1. Tiền không âm (No Negative Balance)
- `wallets.balance >= 0` — LUÔN LUÔN
- `system_funds.total_balance >= 0` — LUÔN LUÔN
- `departments.total_available_balance >= 0` — LUÔN LUÔN
- `projects.available_budget >= 0` — LUÔN LUÔN
- `phase_category_budgets.budget_limit - current_spent >= 0` — LUÔN LUÔN
- Mọi thao tác trừ tiền phải kiểm tra trước khi trừ. Nếu không đủ → **throw exception, KHÔNG trừ**.

### 5.2. Tính bất biến giao dịch (Immutable Ledger)
- Bảng `transactions` là **append-only** — KHÔNG UPDATE, KHÔNG DELETE.
- Sửa sai bằng cách tạo giao dịch đảo chiều (reverse transaction).
- `balance_after` snapshot lại số dư ví tại thời điểm giao dịch → xuất sao kê nhanh.

### 5.3. Bút toán kép (Double-Entry)
- Mỗi dòng tiền tạo 2 records trong `transactions` (ví nguồn trừ, ví đích cộng).
- Liên kết qua `related_transaction_id` (self-referencing FK).

### 5.4. Auto-Netting (Bù trừ công nợ tự động khi chi lương)
```
Net Salary = (base_salary + bonus + allowance - deduction) - advance_deduct
```
- Nếu `Net Salary > 0`: Trừ hết nợ, chuyển phần còn lại vào Ví → Tạo Transaction `PAYSLIP_PAYMENT`
- Nếu `Net Salary ≤ 0`: Trừ một phần nợ (bằng đúng lương), nợ bảo lưu sang kỳ sau

### 5.5. Concurrency Control (Dual Locking)
- **Primary:** `@Lock(PESSIMISTIC_WRITE)` via `findByIdForUpdate()` — lock row tại DB level khi ghi balance.
- **Safety Net:** `@Version` (Optimistic Locking) trên entity — bắt edge case nếu pessimistic lock bị bypass.
- Áp dụng cho: `Wallet`, `Department`, `Project`, `SystemFund`, `PhaseCategoryBudget`.
- Service method ghi balance **BẮT BUỘC** `@Transactional` + `findByIdForUpdate()`.
- Fallback: nếu `OptimisticLockingFailureException` xảy ra → Client retry.

---

## 6. QUY TẮC BẢO MẬT

### 6.1. Xác thực PIN (Lớp bảo mật thứ 2)
**BẮT BUỘC PIN khi:**
- Rút tiền (Withdraw)
- Accountant giải ngân (Payout)
- Chuyển tiền trả nợ (Repay Debt)

**Quy tắc:**
- Nhập sai > 5 lần → khóa 30 phút
- PIN hash bằng BCrypt (giống password)
- Cấu hình qua `application.yml`, KHÔNG hard-code

### 6.2. File Storage
- Upload lên Cloudinary ở chế độ `authenticated` (private)
- KHÔNG BAO GIỜ trả raw URL cho client
- Backend sinh Signed URL có thời hạn (15 phút)

### 6.3. Self-Approval Restriction
- Người tạo đơn KHÔNG ĐƯỢC tự duyệt đơn của chính mình.
- Backend phải kiểm tra `request.requester_id != current_user_id` khi approve.

### 6.4. First Login
- User mới (`is_first_login = true`) **BẮT BUỘC** đổi mật khẩu lần đầu tiên.

---

## 7. QUY TẮC DỰ ÁN & NGÂN SÁCH

### 7.1. Project Lifecycle States
| Status | Cho phép tạo Request? | Chuyển từ |
|:---|:---:|:---|
| `PLANNING` | ❌ | Mới tạo |
| `ACTIVE` | ✅ | PLANNING |
| `PAUSED` | ❌ | ACTIVE (tạm dừng) |
| `CLOSED` | ❌ | ACTIVE / PAUSED (kết thúc) |

### 7.2. Phase Budget
- Nhân viên chỉ tạo Request trong phạm vi **Phase đang ACTIVE**.
- Phase `CLOSED` → chặn toàn bộ Request mới.

### 7.3. Project Member Access (Data Scope)
- **EMPLOYEE/TEAM_LEADER:** Chỉ thấy/tạo Request cho dự án mình là `project_members`.
- **MANAGER:** Chỉ quản lý dự án thuộc `department_id` của mình.
- **Team Leader** được xác định bằng `project_members.project_role = LEADER`.

### 7.4. Expense Categories
- Member BẮT BUỘC chọn Category khi tạo đơn chi tiêu (ADVANCE/EXPENSE/REIMBURSE).
- Hệ thống validate: `amount ≤ (category.budget_limit - category.current_spent)`.
- Team Leader có toàn quyền chia/sửa Category budget trong dự án.

---

## 8. QUY TẮC CHỨNG TỪ & FILE ĐÍNH KÈM

| Loại Request | Bắt buộc đính kèm? | Ghi chú |
|:---|:---:|:---|
| ADVANCE | ❌ | Cần bổ sung khi REIMBURSE |
| EXPENSE | ✅ | Hóa đơn / Chứng từ |
| REIMBURSE | ✅ | Hóa đơn + Chứng minh chi tiêu |
| PROJECT_TOPUP | ❌ | — |
| QUOTA_TOPUP | ❌ | — |

- Max 5 files per Request
- Max 10MB per file
- Accountant có thể REJECT nếu chứng từ không hợp lệ (ngay cả khi Team Leader đã approve nghiệp vụ)

---

## 9. QUY TẮC THÔNG BÁO & AUDIT

### 9.1. Notification Events
| Event | Người nhận |
|:---|:---|
| Request tạo mới | Approver (TL/Manager/Admin tùy type) |
| Request approved → PENDING_ACCOUNTANT | Accountant |
| Request approved/rejected | Requester (người tạo đơn) |
| Lương đã trả | Employee |
| Quota approved | Manager (phòng ban nhận) |
| Project top-up approved | Team Leader (dự án nhận) |

### 9.2. Audit Log
- Bảng `audit_logs` là **append-only** — KHÔNG UPDATE, KHÔNG DELETE.
- Ghi nhận: thay đổi config, phân quyền, ngân sách, lock/unlock user.
- Lưu `old_values` và `new_values` dạng JSONB để trace change history.

---

## 10. BUSINESS CODE GENERATION

- Mã code **PHẢI** được generate ở Application Layer (Service), KHÔNG ở DB trigger.
- Mã code là `UNIQUE NOT NULL` — phải đảm bảo giá trị hợp lệ trước khi persist.
- Sequence-based codes dùng **PostgreSQL Sequence** (`SELECT nextval(...)`) — atomic, lock-free, cluster-safe.
- Transaction codes dùng random hex — không cần sequence.


