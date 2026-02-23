# PROJECT SPECIFICATION: INTERNAL FINANCE MANAGEMENT SYSTEM (IFMS)

## 1. TỔNG QUAN (OVERVIEW)
Hệ thống quản lý tài chính nội bộ dành cho doanh nghiệp SME, giúp số hóa quy trình quản lý dòng tiền tạm ứng, hoàn ứng và chi tiêu. Hệ thống được xây dựng theo kiến trúc **Modular Monolith** kết hợp nguyên lý **Clean Architecture** bên trong từng module, đảm bảo tính tách biệt nghiệp vụ cao, dễ bảo trì nhưng vẫn đơn giản trong việc triển khai. Hệ thống tích hợp bảo mật 2 lớp (PIN), lưu trữ chứng từ an toàn và cập nhật dữ liệu Real-time.

## 2. TECHNOLOGY STACK (CÔNG NGHỆ)
* **Backend Framework:** Spring Boot 3.x (Java 17 hoặc 21).
* **Frontend Framework:** React.js/ Next.js (Sử dụng Vite, TypeScript).
* **Styling & UI:** TailwindCSS, ShadcnUI (hoặc Ant Design).
* **State Management & Fetching:** React Query (TanStack Query), Zustand/Redux Toolkit.
* **Database:** PostgreSQL (Relational Database).
* **Real-time Communication:** Spring WebSocket (STOMP protocol, SockJS).
* **File Storage:** Cloudinary (Chế độ Private/Authenticated - bảo mật chứng từ).
* **Security:** Spring Security, JWT (JSON Web Token), BCrypt (Password & PIN hashing).
* **Object Mapping:** MapStruct.
* **Migration Tool:** Flyway (Quản lý version DB).
* **Build Tool:** Maven.

## 3. KIẾN TRÚC HỆ THỐNG (ARCHITECTURE)
Hệ thống áp dụng kiến trúc **Modular Monolith (Đơn khối Module hóa)**.

* **Định nghĩa:** Toàn bộ hệ thống chạy trên một tiến trình (Process) duy nhất (deploy 1 file jar), nhưng mã nguồn được tổ chức thành các Modules nghiệp vụ tách biệt thay vì phân chia theo layer kỹ thuật truyền thống (Controller/Service/Repository).
* **Cấu trúc bên trong Module:** Mỗi Module tuân thủ **Clean Architecture (Hexagonal)** để đảm bảo logic nghiệp vụ không phụ thuộc vào Framework:
    * **Domain:** Chứa Entities và Business Rules.
    * **Application:** Chứa Use Cases, DTOs, Interfaces.
    * **Infrastructure:** Chứa Controller, Repository Implementation, External Adapters.
* **Giao tiếp giữa các Module:**
    * Sử dụng Public Interface (Service) của module khác.
    * Sử dụng **Domain Events** (Spring Event Publisher) để giảm sự phụ thuộc (Loose Coupling).

## 4. DANH SÁCH MODULES & ENTITIES (DATABASE SCHEMA)
Hệ thống được chia thành 9 module nghiệp vụ chính:

* **Module File Storage:** Quản lý metadata file.
    * *Entities:* `FileStorage`
* **Module IAM (Identity & Access Management):** Quản lý định danh và hồ sơ.
    * *Entities:* `User`, `Role`, `UserProfile`, `UserSecuritySettings`
* **Module Organization:** Quản lý cơ cấu tổ chức.
    * *Entities:* `Department`
* **Module Wallet:** Quản lý ví và giao dịch (Core).
    * *Entities:* `Wallet` (Optimistic Locking), `Transaction`
* **Module Project:** Quản lý vòng đời dự án.
    * *Entities:* `Project`, `ProjectPhase`, `ProjectMember`
* **Module Request:** Quản lý luồng yêu cầu và phê duyệt.
    * *Entities:* `Request`, `RequestHistory`
* **Module Accounting:** Quản lý lương và quỹ.
    * *Entities:* `PayrollPeriod`, `Payslip`, `SystemFund`
* **Module Config:** Cấu hình hệ thống động.
    * *Entities:* `SystemConfig`
* **Module Notification:** Lưu trữ lịch sử thông báo (Persistence for WebSocket).
    * *Entities:* `Notification`

## 5. PHÂN QUYỀN & BẢO MẬT (ROLES & PERMISSIONS)

Hệ thống sử dụng cơ chế **Dynamic RBAC (Role-Based Access Control)**. 
- **Roles** được lưu động trong Database (Admin có thể tạo Role mới và gán quyền tùy ý).
- **Permissions** được định nghĩa cứng trong Source Code để đảm bảo logic nghiệp vụ.

### 5.1. Danh Sách Quyền Hạn (Permissions Reference)

Các quyền hạn được chia thành 6 nhóm module chính:

#### 🔐 Nhóm 1: IAM & Bảo mật (Identity & Security)
| Permission Key | Mô tả Quyền hạn |
| :--- | :--- |
| `USER_PROFILE_VIEW` | Xem hồ sơ cá nhân (Profile). |
| `USER_PROFILE_UPDATE` | Cập nhật thông tin cá nhân (Avatar, SĐT, Địa chỉ). |
| `USER_PIN_UPDATE` | Thiết lập hoặc đổi mã PIN giao dịch (Bảo mật cấp 2). |
| `NOTIFICATION_VIEW` | Xem thông báo biến động số dư/lương. |
| `USER_VIEW_LIST` | **(Admin)** Xem danh sách nhân viên toàn hệ thống. |
| `USER_CREATE` | **(Admin)** Cấp tài khoản mới (Onboarding). |
| `USER_UPDATE` | **(Admin)** Chỉnh sửa thông tin & Điều chuyển nhân sự. |
| `USER_LOCK` | **(Admin)** Khóa/Mở khóa tài khoản. |
| `ROLE_MANAGE` | **(Admin)** Quản lý Vai trò & Phân quyền (Tạo Role, Gán quyền động). |

#### 💳 Nhóm 2: Ví Điện Tử (Core Wallet)
| Permission Key | Mô tả Quyền hạn |
| :--- | :--- |
| `WALLET_VIEW_SELF` | Xem Dashboard ví cá nhân (Số dư, Tiền treo, Dư nợ). |
| `WALLET_DEPOSIT` | Nạp tiền vào ví (Nhập tiền + PIN/QR). |
| `WALLET_WITHDRAW` | Rút tiền về ngân hàng (Nhập TK + PIN). |
| `WALLET_TRANSACTION_VIEW` | Xem lịch sử giao dịch cá nhân. |
| `TRANSACTION_APPROVE_WITHDRAW` | **(Accountant)** Duyệt các lệnh rút tiền giá trị lớn hoặc bị treo. |

#### 🚀 Nhóm 3: Dự án & Tiến độ (Project Lifecycle)
| Permission Key | Mô tả Quyền hạn |
| :--- | :--- |
| `PROJECT_VIEW_ACTIVE` | **(Employee)** Xem danh sách Đề án/Phase đang Active (Để tạo Request). |
| `PROJECT_CREATE` | **(Manager)** Khởi tạo đề án mới. |
| `PROJECT_UPDATE` | **(Manager)** Cập nhật thông tin chung dự án (Tên, Deadline). |
| `PROJECT_PHASE_MANAGE` | **(Manager)** Quản lý Phase (Tạo mới, Cấp vốn Phase, Đóng/Mở Phase). |
| `PROJECT_PROGRESS_UPDATE` | **(Manager)** Cập nhật % tiến độ hoàn thành. |
| `PROJECT_MEMBER_MANAGE` | **(Manager)** Thêm hoặc Xóa thành viên khỏi dự án. |
| `PROJECT_STATUS_MANAGE` | **(Manager)** Tạm dừng (Pause) chặn chi tiêu hoặc Đóng (Close) đề án. |
| `PROJECT_VIEW_ALL` | **(Admin/Acc)** Xem danh sách tất cả dự án (Để Audit/Chi tiền). |

#### 📄 Nhóm 4: Quản lý Yêu cầu (Request Flow)
| Permission Key | Mô tả Quyền hạn |
| :--- | :--- |
| `REQUEST_CREATE` | **(Employee)** Tạo yêu cầu (Chi/Ứng/Hoàn ứng) & Upload chứng từ. |
| `REQUEST_VIEW_SELF` | **(Employee)** Xem danh sách & trạng thái yêu cầu của chính mình. |
| `REQUEST_VIEW_DEPT` | **(Manager)** Xem các yêu cầu thuộc phòng ban mình quản lý. |
| `REQUEST_APPROVE_TIER1` | **(Manager)** Duyệt yêu cầu cấp 1 (Trong hạn mức Manager). |
| `REQUEST_REJECT` | **(Manager/Admin)** Từ chối yêu cầu (Bắt buộc nhập lý do). |
| `REQUEST_APPROVE_TIER2` | **(Admin)** Duyệt yêu cầu cấp 2 (Vượt hạn mức Manager/Leo thang). |
| `REQUEST_VIEW_ALL` | **(Admin)** Xem tất cả yêu cầu toàn hệ thống. |
| `REQUEST_VIEW_APPROVED` | **(Accountant)** Xem danh sách yêu cầu ĐÃ DUYỆT (Chờ giải ngân). |
| `REQUEST_PAYOUT` | **(Accountant)** Thực hiện Thanh toán/Giải ngân (Trừ quỹ -> Cộng ví NV). |

#### 💰 Nhóm 5: Lương & Kế toán (Payroll & Accounting)
| Permission Key | Mô tả Quyền hạn |
| :--- | :--- |
| `PAYROLL_VIEW_SELF` | **(Employee)** Xem danh sách kỳ lương & Chi tiết phiếu lương. |
| `PAYROLL_DOWNLOAD` | **(Employee)** Tải phiếu lương cá nhân (PDF). |
| `PAYROLL_MANAGE` | **(Accountant)** Quản lý kỳ lương (Tạo mới, Upload Excel, Validate). |
| `PAYROLL_EXECUTE` | **(Accountant)** Chốt & Chi lương hàng loạt (Kèm Auto-netting trừ nợ). |
| `SYSTEM_FUND_VIEW` | **(Accountant)** Xem số dư Quỹ hệ thống (Mock Bank). |
| `SYSTEM_FUND_TOPUP` | **(Accountant)** Nạp tiền vào Quỹ hệ thống (Top-up). |

#### 🏢 Nhóm 6: Tổ chức & Cấu hình (Org & Config)
| Permission Key | Mô tả Quyền hạn |
| :--- | :--- |
| `DEPT_VIEW_DASHBOARD` | **(Manager)** Xem Dashboard phòng ban (Ngân sách, Báo cáo chi tiêu). |
| `DEPT_MANAGE` | **(Admin)** Quản lý danh sách phòng ban (Tạo, Sửa tên, Mã phòng). |
| `DEPT_BUDGET_ALLOCATE` | **(Admin)** Cấp vốn tổng (Top-up Quota) cho Manager phân bổ. |
| `SYSTEM_CONFIG_MANAGE` | **(Admin)** Cấu hình tham số hệ thống (Hạn mức Rút/Duyệt, Whitelist). |
| `DASHBOARD_VIEW_GLOBAL` | **(Admin)** Xem Dashboard tổng quan (Dòng tiền, Dư nợ toàn cty). |
| `AUDIT_LOG_VIEW` | **(Admin)** Xem nhật ký hệ thống (Audit Logs). |

---

### 5.2. Vai Trò Mặc Định (Default Roles)

Hệ thống khởi tạo sẵn 4 Role với các bộ quyền tương ứng:

#### 🧑‍💻 1. EMPLOYEE (Nhân viên)
* **Mục tiêu:** Thực hiện các nghiệp vụ cá nhân cơ bản.
* **Quyền hạn:** * Xem Profile, Ví cá nhân, Lịch sử giao dịch.
    * Tạo yêu cầu (Chi/Ứng) trong dự án đang tham gia.
    * Xem & Tải phiếu lương.

#### 👨‍💼 2. MANAGER (Trưởng phòng)
* **Mục tiêu:** Quản lý dự án, ngân sách phòng ban và duyệt chi cấp thấp.
* **Quyền hạn:**
    * *Kế thừa toàn bộ quyền Employee.*
    * Quản lý vòng đời Dự án (Tạo, Chia Phase, Cấp vốn Phase).
    * Duyệt yêu cầu của nhân viên (Cấp 1 - Tier 1).
    * Xem báo cáo tài chính phòng ban.

#### 👩‍💻 3. ACCOUNTANT (Kế toán)
* **Mục tiêu:** Quản lý dòng tiền thực tế, chi lương và giải ngân.
* **Quyền hạn:**
    * *Kế thừa toàn bộ quyền Employee.*
    * Thực hiện Giải ngân (Payout) cho phiếu đã duyệt.
    * Chạy bảng lương (Payroll) và Bù trừ công nợ (Auto-netting).
    * Quản lý Quỹ hệ thống và duyệt lệnh rút tiền rủi ro.

#### 👮‍♂️ 4. ADMIN (Quản trị viên)
* **Mục tiêu:** Quản trị hệ thống, nhân sự và xử lý ngoại lệ.
* **Quyền hạn:**
    * Quản lý User (Tạo, Khóa, Reset Pass).
    * **Quản lý Role & Permission (Dynamic RBAC).**
    * Quản lý Phòng ban & Cấp vốn tổng (Quota).
    * Duyệt yêu cầu cấp cao (Leo thang - Tier 2).
    * Cấu hình hệ thống & Xem Audit Log.

## 6. QUY TẮC NGHIỆP VỤ (BUSINESS RULES)

Hệ thống tuân thủ nghiêm ngặt các quy tắc nghiệp vụ sau để đảm bảo tính toàn vẹn dữ liệu và an toàn tài chính.

### 6.1. Quy tắc Ví & Giao dịch (Wallet & Transaction Rules)

1.  **Nguyên tắc "Tiền không âm" (No Negative Balance):**
    * Số dư khả dụng (`balance`) của ví cá nhân và Quỹ hệ thống (`SystemFund`) **không bao giờ được phép âm**.
    * Mọi giao dịch trừ tiền (`WITHDRAW`, `PAYMENT`) phải kiểm tra: `if (currentBalance >= amount)`. Nếu không đủ, hệ thống từ chối ngay lập tức với lỗi `INSUFFICIENT_FUNDS`.

2.  **Xử lý Đồng thời (Concurrency Control):**
    * Sử dụng cơ chế **Optimistic Locking** (Khóa lạc quan) thông qua trường `@Version` trong entity `Wallet`.
    * Khi hai giao dịch cùng cập nhật một ví tại một thời điểm, transaction đến sau sẽ bị từ chối (throw `OptimisticLockingFailureException`) và yêu cầu Client thử lại (Retry). Điều này ngăn chặn lỗi "Race Condition" làm sai lệch số dư.

3.  **Tính Bất biến của Giao dịch (Immutable History):**
    * Dữ liệu trong bảng `transactions` là **bất biến**. Không được phép `UPDATE` hay `DELETE` record đã tạo.
    * Nếu có sai sót (ví dụ: chuyển nhầm tiền), phải tạo một giao dịch mới với số tiền âm (hoặc giao dịch đảo chiều - Revert Transaction) để sửa sai, tuyệt đối không sửa dữ liệu cũ.

4.  **Bảo mật Giao dịch (Transaction Security):**
    * Các hành động nhạy cảm: **Rút tiền (Withdraw)**, **Chuyển tiền trả nợ (Repay Debt)** và **Accountant duyệt chi (Payout)** bắt buộc phải xác thực lớp thứ 2 bằng **Mã PIN 5 số**.

### 6.2. Quy tắc Vòng đời Dự án & Ngân sách (Project Lifecycle & Budgeting)

1.  **Phân bổ vốn theo Giai đoạn (Phasing Budget):**
    * Ngân sách của Dự án (`total_budget`) là con số kế hoạch. Tiền thực tế được giải ngân dựa trên **Giai đoạn (Phase)**.
    * Nhân viên chỉ được tạo yêu cầu chi tiêu dựa trên hạn mức của **Phase đang Active**.
    * *Ví dụ:* Dự án 1 tỷ, Phase 1 cấp 200tr. Nhân viên chỉ được request trong phạm vi 200tr đó.

2.  **Ràng buộc Trạng thái Dự án:**
    * Chỉ được tạo Request cho dự án có trạng thái `ACTIVE`.
    * Khi Dự án hoặc Phase chuyển sang `PAUSED` hoặc `CLOSED`: Hệ thống chặn toàn bộ việc tạo Request mới liên quan đến Dự án/Phase đó.
    * Khi đóng Phase cũ (`CLOSED`), số tiền chưa dùng hết sẽ được hoàn lại vào "Tiền khả dụng" của Phòng ban (hoặc chuyển tiếp sang Phase sau tùy cấu hình Manager).

3.  **Phạm vi Truy cập (Data Scope):**
    * **Employee:** Chỉ nhìn thấy và tạo request cho các Dự án mà mình có tên trong danh sách Thành viên (`project_members`).
    * **Manager:** Chỉ quản lý các Dự án thuộc Phòng ban (`department_id`) của mình.

### 6.3. Quy trình Phê duyệt & Yêu cầu (Request Workflow)

1.  **Validate Chứng từ (Evidence Validation):**
    * Loại yêu cầu `EXPENSE` (Thanh toán chi phí) và `REIMBURSEMENT` (Hoàn ứng) **bắt buộc** phải có file đính kèm (Hóa đơn/Chứng từ).
    * Loại yêu cầu `ADVANCE` (Tạm ứng) có thể không cần chứng từ lúc tạo, nhưng phải bổ sung khi làm thủ tục Hoàn ứng.

2.  **Phân cấp Duyệt (Multi-level Approval):**
    Hệ thống tự động điều hướng luồng duyệt dựa trên `amount` và cấu hình `SYSTEM_CONFIG`:
    * **Cấp 1 (Manager):** Nếu `amount <= MANAGER_LIMIT` -> Manager duyệt -> Trạng thái chuyển `APPROVED` (Chờ chi).
    * **Cấp 2 (Admin/Leo thang):** Nếu `amount > MANAGER_LIMIT` -> Manager duyệt -> Trạng thái chuyển `PENDING_ADMIN` -> Admin duyệt -> Trạng thái chuyển `APPROVED`.
    * *Lưu ý:* Manager không được duyệt yêu cầu của chính mình tạo ra (Self-approval restriction).

3.  **Luồng Trạng thái (State Transitions):**
    * `PENDING` -> `APPROVED` (Hợp lệ).
    * `PENDING` -> `REJECTED` (Kết thúc, không thể mở lại).
    * `PENDING` -> `CANCELLED` (Người tạo tự hủy trước khi được duyệt).
    * `APPROVED` -> `PAID` (Kế toán đã giải ngân, tiền về ví).

### 6.4. Quy tắc Kế toán & Lương (Accounting & Payroll)

1.  **Cơ chế Bù trừ Tự động (Auto-Netting):**
    * Khi chạy Bảng lương (`Payroll Execution`), hệ thống tự động quét số dư nợ (`debt_balance`) trong ví nhân viên.
    * Công thức thực lĩnh: `Net Salary = (Base Salary + Bonus - Deductions) - Debt Balance`.
    * Nếu `Net Salary > 0`: Trừ hết nợ, chuyển phần còn lại vào Ví.
    * Nếu `Net Salary <= 0`: Trừ một phần nợ (bằng đúng số lương), nợ còn lại được bảo lưu sang kỳ sau.

2.  **Nguyên tắc Giải ngân (Disbursement):**
    * Kế toán (Accountant) **không có quyền sửa đổi** nội dung/số tiền của Request đã được duyệt (`APPROVED`). Họ chỉ có quyền thực hiện lệnh chi (`PAYOUT`) hoặc từ chối chi nếu phát hiện sai sót chứng từ phút chót (Revert về `REJECTED`).
    * Tiền giải ngân được trừ từ `SystemFund` và cộng vào `Wallet` của nhân viên.

### 6.5. Quy tắc Lưu trữ & Bảo mật (Storage & Security)

1.  **Private Cloud Storage:**
    * Tất cả file upload lên Cloudinary phải được set flag `type="authenticated"` (Private Mode).
    * Không bao giờ trả về URL gốc (Raw URL) cho Client.
    * Backend phải sinh ra **Signed URL** (hoặc Token-based URL) có thời hạn (ví dụ: 15 phút) khi Client request xem ảnh.

2.  **Định danh & Phiên làm việc:**
    * Token JWT có thời hạn ngắn (ví dụ: 1 giờ). Sử dụng Refresh Token để gia hạn.
    * Tài khoản mới tạo (`is_first_login = true`) bắt buộc phải đổi mật khẩu ngay trong lần đăng nhập đầu tiên.
    * Nhập sai PIN quá 5 lần sẽ khóa chức năng giao dịch trong 30 phút.

## 7. TÍNH NĂNG CHÍNH (KEY FEATURES)
* **Create Request:** Tạo yêu cầu Tạm ứng/Thanh toán/Hoàn ứng, chọn Dự án & Phase, đính kèm ảnh hóa đơn.
* **Real-time Dashboard:** Cập nhật số dư ví và thông báo duyệt yêu cầu tức thì qua WebSocket.
* **Review & Approve:** Giao diện duyệt cho Manager/Admin với khả năng xem chứng từ, lịch sử log và lý do từ chối.
* **Payroll Processing:** Tính toán lương tự động, trừ nợ và tạo giao dịch trả lương vào ví nhân viên.
* **Security PIN:** Yêu cầu nhập mã PIN khi thực hiện giao dịch nhạy cảm (Rút tiền, Duyệt chi).

## 8. CẤU TRÚC MÃ NGUỒN (SOURCE CODE STRUCTURE)
```plaintext
src/main/java/com/mkwang/backend
├── common                  # Các class dùng chung (BaseEntity, Utils, GlobalException)
├── config                  # Cấu hình (Security, WebSocket, Cloudinary, OpenAPI)
└── modules                 # CÁC MODULE NGHIỆP VỤ
    ├── auth                # Login, Register, Token
    ├── user                # User Profile, Security PIN
    ├── organization        # Department Management
    ├── wallet              # Wallet, Transaction Logic
    ├── project             # Project Lifecycle, Phases
    ├── request             # Request CRUD, Approval Logic
    ├── accounting          # Payroll, System Fund
    ├── file                # Cloudinary Upload Service
    ├── notification        # WebSocket Controller, Notification Persistence
    └── config              # System Configuration