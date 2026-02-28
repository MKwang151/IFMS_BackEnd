# KẾ HOẠCH TRIỂN KHAI DỰ ÁN IFMS
## Internal Finance Management System — Implementation Roadmap

> **Ngày lập:** 27/02/2026  
> **Timeline dự kiến:** 10 Sprint (20 tuần — 5 tháng)  
> **Sprint Duration:** 2 tuần / sprint  
> **Team Structure:** 2 Backend + 2 Frontend + 1 PM/QA

---

## MỤC LỤC

1. [Đánh Giá Hiện Trạng (As-Is Assessment)](#1-đánh-giá-hiện-trạng-as-is-assessment)
2. [Nguyên Tắc Triển Khai](#2-nguyên-tắc-triển-khai)
3. [Dependency Graph — Thứ Tự Module](#3-dependency-graph--thứ-tự-module)
4. [Sprint Plan Chi Tiết](#4-sprint-plan-chi-tiết)
5. [Task Breakdown Theo Role (BE / FE)](#5-task-breakdown-theo-role-be--fe)
6. [Definition of Done (DoD)](#6-definition-of-done-dod)
7. [Risk & Mitigation](#7-risk--mitigation)
8. [Checklist Tổng Thể](#8-checklist-tổng-thể)

---

## 1. ĐÁNH GIÁ HIỆN TRẠNG (As-Is Assessment)

### 1.1 Backend (Spring Boot 3.4.1 / Java 21)

| Module | Entity | Repository | Service | Controller | DTO | Trạng thái |
|--------|:------:|:----------:|:-------:|:----------:|:---:|:----------:|
| **auth** | ✅ | ✅ | ✅ | ✅ | ✅ | **Hoàn thành** |
| **user** | ✅ | ✅ | ❌ | ❌ | ❌ | Entity+Repo |
| **wallet** | ✅ | ✅ | ❌ | ❌ | ❌ | Entity+Repo |
| **accounting** | ✅ | ✅ | ❌ | ❌ | ❌ | Entity+Repo |
| **audit** | ✅ | ✅ | ❌ | ❌ | ❌ | Entity+Repo |
| **project** | ✅ | ❌ | ❌ | ❌ | ❌ | Entity only |
| **request** | ✅ | ❌ | ❌ | ❌ | ❌ | Entity only |
| **organization** | ✅ | ❌ | ❌ | ❌ | ❌ | Entity only |
| **file** | ✅ | ❌ | ❌ | ❌ | ❌ | Entity only |
| **notification** | ✅ | ❌ | ❌ | ❌ | ❌ | Entity only |
| **config** | ✅ | ❌ | ❌ | ❌ | ❌ | Entity only |

**Tóm tắt:** Domain model (JPA entities) đã hoàn chỉnh 100%. Chỉ có module `auth` chạy end-to-end. Các module còn lại cần xây dựng: Repository → Service → DTO → Controller.

### 1.2 Frontend (React 18 + Vite + shadcn/ui)

| Hạng mục | Trạng thái |
|----------|:----------:|
| UI Pages (22 trang) | ✅ Prototype hoàn chỉnh |
| Components (Layout, Feature, Dashboard) | ✅ Đầy đủ |
| Mock Data (13 file) | ✅ Đầy đủ |
| Form Validation (Zod) | ✅ Đã có |
| Router (react-router-dom) | ❌ Chưa cài |
| HTTP Client (axios / fetch) | ❌ Chưa cài |
| State Management (Zustand / TanStack Query) | ❌ Chưa cài |
| API Service Layer | ❌ Chưa có |
| WebSocket Client | ❌ Chưa có |
| Auth Token Management | ❌ Chưa có |

**Tóm tắt:** UI prototype hoàn chỉnh 100%, toàn bộ chạy bằng mock data. Cần cài đặt infrastructure (router, HTTP client, state) rồi thay thế mock → API thật.

---

## 2. NGUYÊN TẮC TRIỂN KHAI

### 2.1 Chiến lược tổng thể: **Vertical Slice Delivery**
- Mỗi Sprint deliver **1 luồng nghiệp vụ hoàn chỉnh** (từ DB → API → UI) thay vì làm xong toàn bộ BE rồi mới làm FE.
- BE và FE chạy song song: BE expose API → FE integrate ngay trong cùng Sprint.

### 2.2 Quy ước kỹ thuật

| # | Quy ước | Mô tả |
|---|---------|-------|
| 1 | **API Contract First** | BE và FE thống nhất Request/Response DTO trước khi code. Tham chiếu `API_Spec.md`. |
| 2 | **Branch Strategy** | `main` ← `develop` ← `feature/{module}-{task}`. Merge qua PR + Code Review. |
| 3 | **DB Migration** | Dùng Flyway thay `ddl-auto: create`. Mỗi thay đổi schema = 1 migration file. |
| 4 | **Testing** | BE: Unit Test cho Service layer (JUnit 5 + Mockito). FE: Manual + Smoke test trên Swagger. |
| 5 | **Code Review** | Mỗi PR tối thiểu 1 reviewer approve. Không self-merge. |
| 6 | **Daily Standup** | 15 phút/ngày: Hôm qua làm gì? Hôm nay làm gì? Có blocker không? |

### 2.3 Quy ước đặt tên

```
Backend:
  Controller:  XxxController.java       (REST endpoint)
  Service:     XxxService.java           (interface)
                XxxServiceImpl.java       (implementation)
  Repository:  XxxRepository.java        (JPA Repository)
  DTO:         XxxRequest.java           (input)
                XxxResponse.java          (output)

Frontend:
  API:         src/api/xxx.api.ts        (axios calls)
  Hooks:       src/hooks/use-xxx.ts      (TanStack Query hooks)
  Store:       src/stores/auth.store.ts  (Zustand)
  Types:       src/lib/xxx-types.ts      (shared types)
```

---

## 3. DEPENDENCY GRAPH — THỨ TỰ MODULE

```
Tầng 0 (Foundation — Không phụ thuộc ai):
  ├── common (BaseEntity, ApiResponse, Exception)
  ├── config (Security, Flyway, Cloudinary)
  └── file (FileStorage — Cloudinary integration)

Tầng 1 (Core IAM — Nền tảng phân quyền):
  ├── auth (Login, JWT, Refresh Token)        ← [ĐÃ XONG]
  └── user (Profile, PIN, Bank Info)          ← Phụ thuộc: auth, file

Tầng 2 (Tổ chức & Dự án):
  ├── organization (Department, Budget)       ← Phụ thuộc: user
  ├── config-module (SystemConfig CRUD)       ← Phụ thuộc: user (RBAC)
  └── project (Project, Phase, Member)        ← Phụ thuộc: user, organization

Tầng 3 (Luồng nghiệp vụ chính):
  ├── request (Create, Approve, Reject)       ← Phụ thuộc: user, project, file
  └── wallet (Balance, Deposit, Withdraw)     ← Phụ thuộc: user

Tầng 4 (Tài chính & Kế toán):
  ├── accounting (Payroll, Payslip, Disburse) ← Phụ thuộc: wallet, request, user
  └── audit (AuditLog, System Trail)          ← Phụ thuộc: all modules

Tầng 5 (Real-time & UX):
  └── notification (WebSocket, Push)          ← Phụ thuộc: all modules
```

---

## 4. SPRINT PLAN CHI TIẾT

---

### 🟦 SPRINT 0 — Foundation & Infrastructure (Tuần 1-2)
> **Mục tiêu:** Setup môi trường phát triển, CI/CD cơ bản, Flyway migration, Frontend infrastructure.

#### Backend Tasks

| # | Task | Assignee | Priority | Story Points |
|---|------|----------|----------|:------------:|
| B0.1 | **Chuyển từ `ddl-auto: create` sang Flyway migration**. Viết file `V1__init_schema.sql` chứa toàn bộ DDL từ Database.md (tất cả 10 module). | BE-1 | 🔴 Critical | 5 |
| B0.2 | **Viết `DataInitializer.java`** (uncomment + hoàn thiện): Seed 4 Roles + Permissions, 4 Users mẫu (1 mỗi role), Departments, SystemFund, SystemConfigs mặc định. | BE-2 | 🔴 Critical | 3 |
| B0.3 | **Bổ sung dependencies vào `pom.xml`**: Cloudinary SDK, MapStruct, Flyway-PostgreSQL, SpringDoc improvements. | BE-1 | 🟡 High | 2 |
| B0.4 | **Refactor `SecurityConfig.java`**: Cấu hình endpoint whitelist chính xác theo API_Spec (public: `/auth/**`, `/banks`; protected: còn lại). Thêm role-based `@PreAuthorize` annotation pattern. | BE-2 | 🟡 High | 3 |
| B0.5 | **Setup Cloudinary configuration** trong `application.yml` + tạo `CloudinaryConfig.java`. | BE-1 | 🟡 High | 2 |
| B0.6 | **Tạo shared utility classes**: `BusinessCodeGenerator.java` (auto-gen requestCode, transactionCode, projectCode, etc.), `PinValidator.java`. | BE-2 | 🟡 High | 3 |

#### Frontend Tasks

| # | Task | Assignee | Priority | Story Points |
|---|------|----------|----------|:------------:|
| F0.1 | **Cài đặt dependencies**: `axios`, `@tanstack/react-query`, `zustand`, `@stomp/stompjs`, `sockjs-client`. | FE-1 | 🔴 Critical | 1 |
| F0.2 | **Tạo Axios instance** (`src/api/client.ts`): Base URL, interceptors (attach Bearer token, auto-refresh on 401, global error handling theo `ApiResponse<T>` format). | FE-1 | 🔴 Critical | 3 |
| F0.3 | **Tạo Auth Store** (`src/stores/auth.store.ts` — Zustand): Lưu `accessToken`, `refreshToken`, `user` object. Actions: `login()`, `logout()`, `refreshToken()`, `setUser()`. Persist vào `localStorage`. | FE-2 | 🔴 Critical | 3 |
| F0.4 | **Tạo QueryClient Provider** + `src/api/query-keys.ts` (TanStack Query key factory pattern). | FE-1 | 🟡 High | 2 |
| F0.5 | **Tạo cấu trúc thư mục API**: `src/api/auth.api.ts`, `src/api/user.api.ts`, … (placeholder files theo từng module, mỗi file export các functions gọi axios). | FE-2 | 🟡 High | 2 |
| F0.6 | **Setup Environment Variables**: `.env.development` (API_URL, WS_URL). | FE-1 | 🟢 Medium | 1 |

#### Deliverable Sprint 0:
- ✅ Database schema khởi tạo tự động qua Flyway
- ✅ Seed data chạy thành công (4 roles, 4 users, configs)
- ✅ Swagger UI accessible tại `/swagger-ui.html`
- ✅ Frontend axios instance + auth store ready
- ✅ Cả BE lẫn FE đều chạy local không lỗi

---

### 🟦 SPRINT 1 — Authentication & User Profile (Tuần 3-4)
> **Mục tiêu:** Hoàn thiện luồng Auth end-to-end + User Profile CRUD. FE kết nối API Auth thật.

#### Backend Tasks

| # | Task | Assignee | Priority | SP |
|---|------|----------|----------|:--:|
| B1.1 | **Hoàn thiện Auth module**: Bổ sung endpoints `POST /auth/change-password` (first login), `POST /auth/forgot-password`, `POST /auth/reset-password`, `GET /auth/me`. | BE-1 | 🔴 | 5 |
| B1.2 | **User Module — Service Layer**: Tạo `UserService` + `UserServiceImpl` với các methods: `getMyProfile()`, `updateProfile()`, `updateAvatar()`, `updateBankInfo()`, `changePassword()`. | BE-2 | 🔴 | 5 |
| B1.3 | **User Module — PIN Management**: Tạo `POST /users/me/pin` (create), `PUT /users/me/pin` (change), `POST /users/me/pin/verify`. Implement logic retry count + lock 30 phút. | BE-1 | 🔴 | 5 |
| B1.4 | **User Module — DTOs**: Tạo `UserProfileResponse`, `UpdateProfileRequest`, `UpdateAvatarRequest`, `BankInfoRequest/Response`, `PinRequest`, etc. theo API_Spec. | BE-2 | 🟡 | 3 |
| B1.5 | **User Module — Controller**: `UserController.java` expose tất cả endpoints `/users/me/**`. | BE-1 | 🟡 | 3 |
| B1.6 | **File Module — Cloudinary Integration**: Tạo `FileStorageRepository`, `FileService` (upload signature, delete file), `FileController` (`GET /files/signature`, `DELETE /files/:publicId`). | BE-2 | 🟡 | 5 |
| B1.7 | **Banks endpoint**: `GET /banks` — trả static JSON danh sách ngân hàng. | BE-1 | 🟢 | 1 |

#### Frontend Tasks

| # | Task | Assignee | Priority | SP |
|---|------|----------|----------|:--:|
| F1.1 | **Kết nối Login Page**: Thay mock `setTimeout` → gọi `POST /auth/login` thật. Lưu token vào Zustand store. Redirect theo `isFirstLogin` và `role`. | FE-1 | 🔴 | 5 |
| F1.2 | **Kết nối Change Password (First Login)**: Gọi `POST /auth/change-password`. | FE-2 | 🔴 | 2 |
| F1.3 | **Kết nối Create PIN**: Gọi `POST /users/me/pin`. | FE-2 | 🔴 | 2 |
| F1.4 | **Kết nối Forgot/Reset Password flow**: 2 màn hình. | FE-1 | 🟡 | 3 |
| F1.5 | **Kết nối Settings Page — Profile Tab**: Gọi `GET /users/me/profile`, `PUT /users/me/profile`, `PUT /users/me/avatar` (Cloudinary direct upload + `GET /files/signature`). | FE-2 | 🟡 | 5 |
| F1.6 | **Kết nối Settings Page — Bank Info Tab**: Gọi `PUT /users/me/bank-info`, `GET /banks`. | FE-1 | 🟡 | 3 |
| F1.7 | **Kết nối Settings Page — Security Tab**: Gọi `PUT /users/me/pin`, `PUT /users/me/password`. | FE-1 | 🟡 | 3 |
| F1.8 | **Implement Protected Route logic**: Check auth token trước khi render page. Auto-redirect về Login nếu hết hạn. | FE-2 | 🔴 | 3 |

#### Deliverable Sprint 1:
- ✅ Đăng nhập/đăng xuất thực tế hoạt động
- ✅ First-login flow: đổi mật khẩu → tạo PIN
- ✅ User có thể xem/sửa profile, avatar, bank info, PIN, password
- ✅ Cloudinary upload hoạt động
- ✅ Protected routes hoạt động

---

### 🟦 SPRINT 2 — Organization & System Config (Tuần 5-6)
> **Mục tiêu:** Admin quản lý phòng ban, user, cấu hình hệ thống. Dynamic RBAC.

#### Backend Tasks

| # | Task | Assignee | Priority | SP |
|---|------|----------|----------|:--:|
| B2.1 | **Organization Module**: Tạo `DepartmentRepository`, `DepartmentService`, `DepartmentController`. CRUD endpoints: `GET /admin/departments`, `GET /admin/departments/:id`, `POST /admin/departments`, `PUT /admin/departments/:id`. | BE-1 | 🔴 | 5 |
| B2.2 | **Admin User Management**: Tạo `AdminUserService`, `AdminUserController`. Endpoints: `GET /admin/users`, `GET /admin/users/:id`, `POST /admin/users` (create user + auto wallet + email temp password), `PUT /admin/users/:id`, `POST /admin/users/:id/lock`, `POST /admin/users/:id/unlock`, `POST /admin/users/:id/reset-password`. | BE-2 | 🔴 | 8 |
| B2.3 | **Config Module**: Tạo `SystemConfigRepository`, `SystemConfigService`, `SystemConfigController`. Endpoints: `GET /admin/settings`, `PUT /admin/settings`. | BE-1 | 🟡 | 3 |
| B2.4 | **Audit Module — Service Layer**: Tạo `AuditLogService` (interface để các module khác gọi `log(action, entity, oldVal, newVal)`). Integrate vào User/Dept/Config services. | BE-2 | 🟡 | 3 |
| B2.5 | **Audit Module — Controller**: `GET /admin/audit` với filter + phân trang. | BE-1 | 🟡 | 3 |
| B2.6 | **RBAC enforcement**: Implement `@PreAuthorize("hasPermission('USER_CREATE')")` pattern. Tạo custom `PermissionEvaluator`. | BE-2 | 🟡 | 5 |

#### Frontend Tasks

| # | Task | Assignee | Priority | SP |
|---|------|----------|----------|:--:|
| F2.1 | **Kết nối Admin Users Page**: Gọi `GET /admin/users` (list + filter + search + pagination). CRUD modal (create, edit, lock/unlock, reset password). | FE-1 | 🔴 | 8 |
| F2.2 | **Kết nối Admin Departments Page** (nếu có UI): Gọi CRUD departments. Nếu chưa có UI → tạo mới. | FE-2 | 🔴 | 5 |
| F2.3 | **Kết nối Admin Settings Page**: Gọi `GET /admin/settings`, `PUT /admin/settings`. | FE-1 | 🟡 | 3 |
| F2.4 | **Kết nối Admin Audit Page**: Gọi `GET /admin/audit` + filter. | FE-2 | 🟡 | 3 |
| F2.5 | **Role-based menu rendering**: Đảm bảo sidebar chỉ hiển thị menu items đúng role (Employee không thấy Admin pages, etc.). | FE-1 | 🟡 | 2 |

#### Deliverable Sprint 2:
- ✅ Admin có thể CRUD users, departments, settings
- ✅ Audit log ghi nhận mọi thao tác thay đổi
- ✅ RBAC enforced — endpoint trả 403 nếu không đủ quyền
- ✅ Admin UI kết nối API thật

---

### 🟦 SPRINT 3 — Wallet Core (Tuần 7-8)
> **Mục tiêu:** Ví điện tử hoạt động: Xem số dư, lịch sử giao dịch, rút tiền (PIN verify), nạp tiền (VietQR mock).

#### Backend Tasks

| # | Task | Assignee | Priority | SP |
|---|------|----------|----------|:--:|
| B3.1 | **Wallet Module — Repository**: Tạo `TransactionRepository` (findByWalletId + filter + pagination). | BE-1 | 🔴 | 2 |
| B3.2 | **Wallet Module — Service Layer**: Tạo `WalletService` + `WalletServiceImpl`: `getBalance()`, `getTransactions()`, `withdraw()` (PIN verify → balance check → Optimistic Lock → tạo transaction → update balance), `generateDepositQR()`. | BE-2 | 🔴 | 8 |
| B3.3 | **Wallet Module — Controller**: `WalletController.java` expose: `GET /wallet`, `GET /wallet/transactions`, `POST /wallet/withdraw`, `POST /wallet/deposit/generate-qr`. | BE-1 | 🔴 | 3 |
| B3.4 | **Wallet Module — DTOs**: `WalletResponse`, `TransactionResponse`, `WithdrawRequest`, `DepositQRRequest/Response`, etc. | BE-2 | 🟡 | 3 |
| B3.5 | **Transaction code generator**: Implement `TXN-{8 chars hex}` format trong `BusinessCodeGenerator`. | BE-1 | 🟡 | 1 |
| B3.6 | **Deposit Webhook simulation**: Tạo internal endpoint (dev-only) `/dev/simulate-deposit` để test nạp tiền mà không cần payment gateway thật. | BE-2 | 🟢 | 2 |

#### Frontend Tasks

| # | Task | Assignee | Priority | SP |
|---|------|----------|----------|:--:|
| F3.1 | **Kết nối Wallet Page — Balance Section**: Gọi `GET /wallet`, hiển thị balance/pendingBalance/debtBalance. | FE-1 | 🔴 | 3 |
| F3.2 | **Kết nối Wallet Page — Transaction List**: Gọi `GET /wallet/transactions` + filter (type, status, date range) + pagination. | FE-2 | 🔴 | 5 |
| F3.3 | **Kết nối Withdraw Modal**: Nhập số tiền → nhập PIN (OTP input) → gọi `POST /wallet/withdraw`. Xử lý error: INSUFFICIENT_FUNDS, PIN wrong, PIN locked. | FE-1 | 🔴 | 5 |
| F3.4 | **Kết nối Deposit Modal**: Gọi `POST /wallet/deposit/generate-qr` → hiển thị QR image + thông tin chuyển khoản. | FE-2 | 🟡 | 3 |
| F3.5 | **Transaction Detail Sheet**: Gọi detail (nếu có) hoặc hiển thị từ list data. | FE-1 | 🟢 | 2 |

#### Deliverable Sprint 3:
- ✅ User xem được số dư ví thật
- ✅ User rút tiền với PIN verify
- ✅ Lịch sử giao dịch hiển thị real-time
- ✅ QR nạp tiền generated

---

### 🟦 SPRINT 4 — Project Management (Tuần 9-10)
> **Mục tiêu:** Manager quản lý dự án, phase, member. Employee xem project mình tham gia.

#### Backend Tasks

| # | Task | Assignee | Priority | SP |
|---|------|----------|----------|:--:|
| B4.1 | **Project Module — Repositories**: Tạo `ProjectRepository`, `ProjectPhaseRepository`, `ProjectMemberRepository`. | BE-1 | 🔴 | 3 |
| B4.2 | **Project Module — Service Layer**: Tạo `ProjectService` + impl: `getMyProjects()` (Employee), `getManagerProjects()` (Manager), `createProject()`, `updateProject()`, `getProjectDetail()`. | BE-2 | 🔴 | 8 |
| B4.3 | **Phase Management**: `createPhase()`, `updatePhase()`, `closePhase()` (hoàn budget chưa dùng về department). | BE-1 | 🔴 | 5 |
| B4.4 | **Member Management**: `syncMembers()` (sync strategy — delete all + re-insert), validation user thuộc cùng department. | BE-2 | 🟡 | 3 |
| B4.5 | **Project Module — Controller**: `ProjectController` (Employee: `GET /projects`, `GET /projects/:id/phases`) + `ManagerProjectController` (full CRUD). | BE-1 | 🟡 | 3 |
| B4.6 | **Project Code Generator**: `PRJ-{dept code}-{year}` format. Phase: `PH-{name abbr}-{seq}`. | BE-2 | 🟡 | 2 |

#### Frontend Tasks

| # | Task | Assignee | Priority | SP |
|---|------|----------|----------|:--:|
| F4.1 | **Kết nối Projects List Page** (Manager): Gọi `GET /manager/projects` + filter + search. | FE-1 | 🔴 | 3 |
| F4.2 | **Kết nối Project Detail Page** (Manager): Gọi `GET /manager/projects/:id`. Hiển thị phases, members, budget chart. | FE-2 | 🔴 | 5 |
| F4.3 | **Create/Edit Project Modal**: Gọi `POST /manager/projects`, `PUT /manager/projects/:id`. Form với dynamic phase list. | FE-1 | 🔴 | 5 |
| F4.4 | **Phase Management UI**: Add/Edit/Close phase. Gọi `POST/PUT /manager/projects/:id/phases/:phaseId`. | FE-2 | 🟡 | 3 |
| F4.5 | **Member Management UI**: Multi-select user dropdown + role input. Gọi `POST /manager/projects/:id/members`. | FE-1 | 🟡 | 3 |
| F4.6 | **Kết nối Department Page** (Manager): Gọi `GET /manager/department/members`. | FE-2 | 🟡 | 3 |

#### Deliverable Sprint 4:
- ✅ Manager tạo/sửa dự án, quản lý phase & member
- ✅ Employee xem danh sách project mình tham gia
- ✅ Budget tracking per phase hoạt động
- ✅ Manager xem department members

---

### 🟦 SPRINT 5 — Request Flow — Employee & Manager (Tuần 11-12)
> **Mục tiêu:** Employee tạo/sửa/hủy request. Manager duyệt/từ chối. Upload chứng từ.

#### Backend Tasks

| # | Task | Assignee | Priority | SP |
|---|------|----------|----------|:--:|
| B5.1 | **Request Module — Repositories**: Tạo `RequestRepository`, `RequestHistoryRepository`, `RequestAttachmentRepository` (nếu cần). | BE-1 | 🔴 | 3 |
| B5.2 | **Request Module — Employee Service**: `createRequest()` (validate project ACTIVE, phase ACTIVE, user is member, EXPENSE/REIMBURSE must have attachments), `updateRequest()` (only PENDING_MANAGER), `cancelRequest()`, `getMyRequests()`, `getRequestDetail()`, `getRequestSummary()`. Auto-generate `requestCode`. | BE-2 | 🔴 | 8 |
| B5.3 | **Request Module — Manager Approval Service**: `getApprovals()` (PENDING_MANAGER + same dept), `approveRequest()` (check MANAGER_LIMIT → auto-escalate to PENDING_ADMIN), `rejectRequest()` (mandatory reason). Self-approval restriction. Create `request_histories` record. | BE-1 | 🔴 | 8 |
| B5.4 | **Request Module — DTOs**: `CreateRequestRequest`, `RequestResponse`, `RequestDetailResponse`, `ApprovalListResponse`, `ApproveRequest`, `RejectRequest`, etc. | BE-2 | 🟡 | 3 |
| B5.5 | **Request Module — Controllers**: `RequestController` (Employee endpoints), `ManagerApprovalController` (Manager endpoints). | BE-1 | 🟡 | 3 |
| B5.6 | **File attachment integration**: Khi tạo/sửa request → link `file_storages` records qua `request_attachments`. Return Signed URL trong response. | BE-2 | 🟡 | 3 |

#### Frontend Tasks

| # | Task | Assignee | Priority | SP |
|---|------|----------|----------|:--:|
| F5.1 | **Kết nối Requests Page** (Employee): Gọi `GET /requests` + filter + search + pagination. Summary counters từ `GET /requests/summary`. | FE-1 | 🔴 | 5 |
| F5.2 | **Kết nối Create Request Modal**: Chọn project (`GET /projects`) → chọn phase (`GET /projects/:id/phases`) → nhập amount, description, upload files (Cloudinary → `attachmentFileIds`). Gọi `POST /requests`. | FE-2 | 🔴 | 8 |
| F5.3 | **Kết nối Edit Request Modal**: Pre-fill data. Gọi `PUT /requests/:id`. Sync attachments. | FE-1 | 🟡 | 3 |
| F5.4 | **Kết nối Request Detail Page**: Gọi `GET /requests/:id`. Hiển thị attachments (Signed URL), timeline, status badge. Cancel button. | FE-2 | 🔴 | 5 |
| F5.5 | **Kết nối Approvals Page** (Manager): Gọi `GET /manager/approvals`. Card view với requester info, project, attachments preview. | FE-1 | 🔴 | 5 |
| F5.6 | **Approve/Reject Actions** (Manager): Gọi `POST /manager/approvals/:id/approve`, `POST /manager/approvals/:id/reject`. | FE-2 | 🔴 | 3 |

#### Deliverable Sprint 5:
- ✅ Employee tạo request với chứng từ đính kèm
- ✅ Manager xem & duyệt/từ chối request
- ✅ Auto-escalate khi vượt hạn mức Manager
- ✅ Timeline hiển thị lịch sử duyệt

---

### 🟦 SPRINT 6 — Request Flow — Admin Approval & Disbursement (Tuần 13-14)
> **Mục tiêu:** Admin duyệt request cấp 2. Accountant giải ngân. Bút toán kép.

#### Backend Tasks

| # | Task | Assignee | Priority | SP |
|---|------|----------|----------|:--:|
| B6.1 | **Admin Approval Service**: `getAdminApprovals()` (PENDING_ADMIN), `approveRequest()` (→ APPROVED), `rejectRequest()`. Create `request_histories`. | BE-1 | 🔴 | 5 |
| B6.2 | **Disbursement Service**: `getDisbursements()` (APPROVED requests), `disburse()` (PIN verify → trừ SystemFund → cộng Employee Wallet → tạo 2 transactions bút toán kép → link `related_transaction_id` → update Request status PAID → update project phase `current_spent`). | BE-2 | 🔴 | 8 |
| B6.3 | **Disbursement — Debt handling**: Nếu request type = `ADVANCE` → cộng `wallet.debt_balance`. | BE-1 | 🟡 | 3 |
| B6.4 | **Accountant — Reject Disbursement**: Revert request → REJECTED (phút chót phát hiện sai). | BE-2 | 🟡 | 2 |
| B6.5 | **Admin/Accountant Controllers**: `AdminApprovalController`, `DisbursementController`. | BE-1 | 🟡 | 3 |
| B6.6 | **Accountant — Request Detail**: `GET /accountant/requests/:requestId` (view any request). | BE-2 | 🟢 | 2 |

#### Frontend Tasks

| # | Task | Assignee | Priority | SP |
|---|------|----------|----------|:--:|
| F6.1 | **Kết nối Admin Approvals Page**: Gọi `GET /admin/approvals` + filter. Approve/Reject actions. | FE-1 | 🔴 | 5 |
| F6.2 | **Kết nối Disbursements Page** (Accountant): Gọi `GET /accountant/disbursements`. Hiển thị bank info, attachments. | FE-2 | 🔴 | 5 |
| F6.3 | **Disburse Action**: PIN input modal → gọi `POST /accountant/disbursements/:id/disburse`. Success → show transactionCode. | FE-1 | 🔴 | 5 |
| F6.4 | **Accountant — Reject Disbursement**: Gọi `POST /accountant/disbursements/:id/reject`. | FE-2 | 🟡 | 2 |
| F6.5 | **Kết nối Accountant Request Detail**: Gọi `GET /accountant/requests/:requestId`. | FE-1 | 🟡 | 2 |

#### Deliverable Sprint 6:
- ✅ Luồng request hoàn chỉnh: Employee tạo → Manager duyệt → (Admin duyệt nếu vượt limit) → Accountant giải ngân
- ✅ Bút toán kép ghi nhận chính xác
- ✅ ADVANCE request tạo debt_balance
- ✅ Tiền vào ví Employee sau giải ngân

---

### 🟦 SPRINT 7 — Payroll & Accounting (Tuần 15-16)
> **Mục tiêu:** Accountant tạo kỳ lương, import Excel, auto-netting, chạy lương.

#### Backend Tasks

| # | Task | Assignee | Priority | SP |
|---|------|----------|----------|:--:|
| B7.1 | **Payroll Module — Service Layer**: `createPeriod()`, `getPeriods()`, `getPeriodDetail()`, `importExcel()` (parse .xlsx, validate employee codes, tạo payslips), `confirmOverwrite()`. | BE-1 | 🔴 | 8 |
| B7.2 | **Auto-Netting Logic**: `autoNetting()` — scan `wallets.debt_balance` → tính `advanceDeduct = min(debt, 50% * netBeforeDeduct)` → update payslips. | BE-2 | 🔴 | 5 |
| B7.3 | **Payroll Run**: `runPayroll()` — iterate payslips → trừ SystemFund → cộng Employee Wallet → trừ debt_balance → tạo transactions (PAYSLIP_PAYMENT) → update payslip status PAID → update period status COMPLETED. | BE-1 | 🔴 | 8 |
| B7.4 | **Payslip CRUD**: `editPayslipEntry()` (before run), `getPayslipDetail()` (Employee), `getMyPayslips()` (Employee). | BE-2 | 🟡 | 5 |
| B7.5 | **Excel Template Generation**: `GET /accountant/payroll/template` — generate .xlsx với header chuẩn. Add dependency Apache POI hoặc EasyExcel. | BE-1 | 🟡 | 3 |
| B7.6 | **Ledger Module**: `GET /accountant/ledger` (system transactions), `GET /accountant/ledger/summary`, `GET /accountant/ledger/:transactionId`. | BE-2 | 🟡 | 5 |
| B7.7 | **Controllers**: `PayrollController`, `PayslipController` (Employee), `LedgerController`. | BE-1 | 🟡 | 3 |

#### Frontend Tasks

| # | Task | Assignee | Priority | SP |
|---|------|----------|----------|:--:|
| F7.1 | **Kết nối Payroll Page** (Accountant): List kỳ lương. Create period modal. | FE-1 | 🔴 | 3 |
| F7.2 | **Payroll Detail — Import Excel**: Upload .xlsx → gọi `POST /accountant/payroll/:id/import`. Hiển thị result table (success/error rows). Handle 409 → confirm overwrite. | FE-2 | 🔴 | 8 |
| F7.3 | **Auto-Netting UI**: Button "Tính bù trừ" → gọi `POST .../auto-netting` → hiển thị summary. | FE-1 | 🔴 | 3 |
| F7.4 | **Run Payroll UI**: Confirm dialog → gọi `POST .../run`. Loading state. | FE-2 | 🔴 | 2 |
| F7.5 | **Edit Payslip Entry**: Inline edit hoặc modal → gọi `PUT .../entries/:payslipId`. | FE-1 | 🟡 | 3 |
| F7.6 | **Kết nối Payslips Page** (Employee): Gọi `GET /payslips`, `GET /payslips/:id`. | FE-2 | 🟡 | 3 |
| F7.7 | **Kết nối Ledger Page** (Accountant): Gọi `GET /accountant/ledger` + filter. Transaction detail sheet. | FE-1 | 🟡 | 5 |

#### Deliverable Sprint 7:
- ✅ Accountant tạo kỳ lương, import Excel, tính bù trừ, chạy lương
- ✅ Tiền lương vào ví nhân viên, nợ được trừ tự động
- ✅ Employee xem phiếu lương
- ✅ Sổ cái giao dịch hệ thống hoạt động

---

### 🟦 SPRINT 8 — Notifications & WebSocket (Tuần 17-18)
> **Mục tiêu:** Real-time notifications, live wallet update, live request status.

#### Backend Tasks

| # | Task | Assignee | Priority | SP |
|---|------|----------|----------|:--:|
| B8.1 | **Notification Module**: Tạo `NotificationRepository`, `NotificationService` (`createNotification()`, `getMyNotifications()`, `markAsRead()`, `markAllAsRead()`), `NotificationController`. | BE-1 | 🔴 | 5 |
| B8.2 | **WebSocket Configuration**: `WebSocketConfig.java` — enable STOMP, SockJS fallback, JWT authentication interceptor. | BE-2 | 🔴 | 5 |
| B8.3 | **Integrate notification triggers**: Khi approve/reject request → push `/user/queue/requests` + tạo notification record. Khi disburse → push `/user/queue/wallet`. Khi payroll run → batch push wallet updates. | BE-1 | 🔴 | 5 |
| B8.4 | **Notification persistence**: Mỗi khi push WebSocket → cũng tạo record trong `notifications` table. | BE-2 | 🟡 | 3 |
| B8.5 | **WebSocket auth**: STOMP CONNECT frame → validate JWT → reject nếu expired. | BE-1 | 🟡 | 3 |

#### Frontend Tasks

| # | Task | Assignee | Priority | SP |
|---|------|----------|----------|:--:|
| F8.1 | **WebSocket Client Setup**: `src/lib/websocket.ts` — STOMP client, auto-reconnect (exponential backoff), JWT auth. | FE-1 | 🔴 | 5 |
| F8.2 | **Subscribe `/user/queue/wallet`**: Nhận message → update wallet Zustand store → animate balance change → toast notification. | FE-2 | 🔴 | 5 |
| F8.3 | **Subscribe `/user/queue/requests`**: Nhận message → update request status in cache → toast. | FE-1 | 🔴 | 3 |
| F8.4 | **Subscribe `/user/queue/notifications`**: Nhận message → prepend to notification list → update unread badge. | FE-2 | 🔴 | 3 |
| F8.5 | **Kết nối Notification Dropdown**: Gọi `GET /notifications` on mount. Mark read `PUT /notifications/:id/read`. Mark all read. | FE-1 | 🟡 | 3 |
| F8.6 | **Reconnection handling**: Khi reconnect → gọi `GET /wallet` + `GET /notifications` để sync missed messages. | FE-2 | 🟡 | 2 |

#### Deliverable Sprint 8:
- ✅ Real-time wallet balance update khi nhận tiền
- ✅ Real-time request status change notification
- ✅ Notification bell badge + dropdown
- ✅ Auto-reconnect khi mất kết nối

---

### 🟦 SPRINT 9 — Dashboards & Reports (Tuần 19-20)
> **Mục tiêu:** Dashboard cho từng role có data thật. Kết nối các biểu đồ.

#### Backend Tasks

| # | Task | Assignee | Priority | SP |
|---|------|----------|----------|:--:|
| B9.1 | **Employee Dashboard API**: `GET /dashboard/employee` — wallet summary, pending requests count, recent transactions, recent payslip. | BE-1 | 🟡 | 5 |
| B9.2 | **Manager Dashboard API**: `GET /dashboard/manager` — department budget overview, project status summary, pending approvals count, team debt summary. | BE-2 | 🟡 | 5 |
| B9.3 | **Accountant Dashboard API**: `GET /dashboard/accountant` — system fund balance, pending disbursements count, monthly inflow/outflow chart data, payroll status. | BE-1 | 🟡 | 5 |
| B9.4 | **Admin Dashboard API**: `GET /dashboard/admin` — total users, total departments, total wallet balance system-wide, recent audit events. | BE-2 | 🟡 | 5 |
| B9.5 | **Monthly Chart Data**: Aggregate SUM transactions by month cho biểu đồ spending trend. | BE-1 | 🟢 | 3 |

#### Frontend Tasks

| # | Task | Assignee | Priority | SP |
|---|------|----------|----------|:--:|
| F9.1 | **Kết nối Employee Dashboard**: Stats cards, expense pie chart, monthly chart, pending requests list, activity feed → API thật. | FE-1 | 🔴 | 5 |
| F9.2 | **Kết nối Manager Dashboard**: Department budget, approval queue, project overview chart. | FE-2 | 🔴 | 5 |
| F9.3 | **Kết nối Accountant Dashboard**: System fund card, disbursement queue, monthly cashflow chart. | FE-1 | 🟡 | 5 |
| F9.4 | **Kết nối Admin Dashboard**: System stats, recent audits, user growth. | FE-2 | 🟡 | 3 |
| F9.5 | **Cleanup mock data**: Xóa tất cả mock-data files không còn sử dụng. | FE-1 | 🟢 | 1 |

#### Deliverable Sprint 9:
- ✅ 4 Dashboards hiển thị data thật từ API
- ✅ Charts hiển thị dữ liệu realtime
- ✅ Mock data được cleanup hoàn toàn

---

### 🟦 SPRINT 10 — Polish, Testing & Go-Live Prep (Tuần 21-22)
> **Mục tiêu:** Bug fix, performance, E2E testing, deployment prep.

#### Toàn Team

| # | Task | Assignee | Priority | SP |
|---|------|----------|----------|:--:|
| T10.1 | **Bug Fix Sprint**: Fix tất cả bugs từ Sprint 1-9. | ALL | 🔴 | 8 |
| T10.2 | **E2E Test Scenarios**: Chạy manual test cho 5 user flows chính (xem bảng bên dưới). | QA/PM | 🔴 | 5 |
| T10.3 | **Performance check**: Pagination hoạt động đúng, không N+1 query, Optimistic Lock test. | BE | 🟡 | 3 |
| T10.4 | **Security audit**: Check tất cả endpoints có `@PreAuthorize` đúng, PIN brute-force protection, JWT expiry. | BE | 🟡 | 3 |
| T10.5 | **FE Error Handling**: Đảm bảo mọi API call đều handle error case (toast thông báo, retry logic). | FE | 🟡 | 3 |
| T10.6 | **Loading/Empty States**: Skeleton loading, empty state UI cho tất cả list pages. | FE | 🟢 | 3 |
| T10.7 | **Responsive check**: Desktop layout hoạt động ổn định. | FE | 🟢 | 2 |
| T10.8 | **Documentation**: Cập nhật README, Swagger annotations, deploy guide. | ALL | 🟢 | 2 |
| T10.9 | **Deployment config**: Dockerfile, docker-compose (PostgreSQL + Backend + Frontend). Environment variables. | BE-1 | 🟡 | 3 |

#### E2E Test Scenarios (Critical Paths)

| # | Scenario | Steps |
|---|----------|-------|
| TC-01 | **Onboarding Flow** | Admin tạo user → User login first time → Đổi MK → Tạo PIN → Vào Dashboard |
| TC-02 | **Request → Approval → Disbursement** | Employee tạo EXPENSE request + upload chứng từ → Manager approve → (Admin approve nếu vượt limit) → Accountant disburse → Tiền vào ví Employee |
| TC-03 | **Advance → Payroll Netting** | Employee tạo ADVANCE → Duyệt → Giải ngân (debt_balance tăng) → Accountant chạy payroll → debt_balance giảm → Lương thực lĩnh = lương - nợ |
| TC-04 | **Wallet Withdraw** | Employee nhập amount → Nhập PIN → Rút tiền → Balance giảm → Transaction history cập nhật |
| TC-05 | **Real-time Notification** | Manager approve request → Employee nhận WebSocket notification → Badge + toast hiện → Wallet cập nhật real-time |

---

## 5. TASK BREAKDOWN THEO ROLE (BE / FE)

### 5.1 Tổng hợp Story Points

| Sprint | Backend SP | Frontend SP | Tổng |
|--------|:----------:|:-----------:|:----:|
| Sprint 0 | 18 | 12 | **30** |
| Sprint 1 | 27 | 26 | **53** |
| Sprint 2 | 27 | 21 | **48** |
| Sprint 3 | 19 | 18 | **37** |
| Sprint 4 | 24 | 22 | **46** |
| Sprint 5 | 28 | 29 | **57** |
| Sprint 6 | 23 | 19 | **42** |
| Sprint 7 | 37 | 27 | **64** |
| Sprint 8 | 21 | 21 | **42** |
| Sprint 9 | 23 | 19 | **42** |
| Sprint 10 | — | — | **32** |
| **TỔNG** | **247** | **214** | **493** |

### 5.2 Velocity Target
- **2 Backend devs**: ~25 SP / sprint (mỗi người ~12-13 SP)
- **2 Frontend devs**: ~22 SP / sprint (mỗi người ~11 SP)
- **Buffer**: 15-20% cho bugs, rework, meeting

### 5.3 Phân bổ Chuyên môn

| Dev | Chuyên môn chính | Module phụ trách chính |
|-----|-------------------|----------------------|
| **BE-1** | Security, Transaction Logic | Auth, Wallet, Request Approval, Payroll Run, WebSocket |
| **BE-2** | CRUD, Integration, Data Processing | User, Organization, Project, File/Cloudinary, Excel Import |
| **FE-1** | Forms, State, API Integration | Auth flow, Request CRUD, Wallet actions, Dashboard charts |
| **FE-2** | UI/UX, Components, Real-time | Profile settings, Project UI, Payroll UI, WebSocket client |

---

## 6. DEFINITION OF DONE (DoD)

Một task/story được coi là **DONE** khi đạt tất cả tiêu chí:

### Backend DoD
- [ ] Code biên dịch thành công (`mvn clean compile`)
- [ ] API endpoint hoạt động trên Swagger UI
- [ ] Request/Response DTO khớp với `API_Spec.md`
- [ ] Validation rules hoạt động (400 Bad Request khi input sai)
- [ ] Authorization check hoạt động (403 khi sai role/permission)
- [ ] Unit test cho Service layer (coverage > 70% cho logic nghiệp vụ)
- [ ] Không có N+1 query (check qua Hibernate SQL log)
- [ ] Code review approved (1+ reviewer)

### Frontend DoD
- [ ] Component render không lỗi console
- [ ] Kết nối API thật thành công (không còn mock data)
- [ ] Loading state hiển thị khi đang fetch
- [ ] Error state hiển thị khi API lỗi (toast message)
- [ ] Empty state hiển thị khi không có data
- [ ] Form validation hoạt động (Zod schema)
- [ ] Code review approved (1+ reviewer)

### Sprint DoD
- [ ] Tất cả tasks trong Sprint ở trạng thái DONE
- [ ] Demo thành công cho PM/Stakeholder
- [ ] Không có blocker cho Sprint tiếp theo
- [ ] Sprint retrospective completed

---

## 7. RISK & MITIGATION

| # | Rủi ro | Xác suất | Tác động | Giải pháp |
|---|--------|:--------:|:--------:|-----------|
| R1 | **Cloudinary integration trễ** — API key chưa có, SDK lỗi | Trung bình | Cao | Tạo Cloudinary free account ngay Sprint 0. Mock FileService nếu chưa sẵn sàng. |
| R2 | **Payment Gateway (VietQR/PayOS) phức tạp** | Cao | Trung bình | Sprint 3 chỉ cần mock deposit. Integrate thật sau khi core ổn định (Sprint 8+). |
| R3 | **Optimistic Locking gây UX kém** — nhiều retry | Thấp | Trung bình | FE implement retry logic tự động (max 3 lần). Hiển thị "Vui lòng thử lại" cho user. |
| R4 | **Excel import lỗi nhiều edge case** | Trung bình | Cao | Validate kỹ: header format, empty rows, duplicate employeeCode. Trả chi tiết `errors[]` cho FE hiển thị. |
| R5 | **WebSocket mất kết nối thường xuyên** | Trung bình | Trung bình | Exponential backoff reconnect. Sync state khi reconnect (`GET /wallet`, `GET /notifications`). |
| R6 | **Sprint 5 & 7 quá tải** (57 + 64 SP) | Cao | Cao | Sẵn sàng move tasks sang Sprint tiếp theo. Ưu tiên core flow, defer nice-to-have. |
| R7 | **FE và BE không sync API contract** | Trung bình | Cao | Dùng `API_Spec.md` làm source of truth. Mỗi Sprint đầu BE + FE ngồi review contract 30 phút. |

---

## 8. CHECKLIST TỔNG THỂ

### Phase 1: Foundation (Sprint 0-1) ⬜
- [ ] Flyway migration chạy thành công
- [ ] Seed data tạo đủ 4 roles, 4 users
- [ ] Login/Logout API hoạt động
- [ ] First-login flow (change PW + create PIN) hoạt động
- [ ] User profile CRUD hoạt động
- [ ] Cloudinary upload hoạt động
- [ ] FE kết nối BE qua axios thành công
- [ ] Protected routes hoạt động

### Phase 2: Administrative (Sprint 2) ⬜
- [ ] Admin CRUD users
- [ ] Admin CRUD departments
- [ ] Admin CRUD system configs
- [ ] Audit log ghi nhận changes
- [ ] RBAC enforced mọi endpoint

### Phase 3: Financial Core (Sprint 3-4) ⬜
- [ ] Wallet balance hiển thị đúng
- [ ] Withdraw + PIN verify hoạt động
- [ ] Transaction history hoạt động
- [ ] Project CRUD (Manager)
- [ ] Phase management hoạt động
- [ ] Member management hoạt động

### Phase 4: Business Flow (Sprint 5-6) ⬜
- [ ] Employee tạo request + upload chứng từ
- [ ] Manager approve/reject request
- [ ] Auto-escalate PENDING_ADMIN hoạt động
- [ ] Admin approve cấp 2 hoạt động
- [ ] Accountant disburse → tiền vào ví
- [ ] Bút toán kép ghi nhận chính xác
- [ ] ADVANCE → tạo debt_balance

### Phase 5: Payroll & Accounting (Sprint 7) ⬜
- [ ] Tạo kỳ lương + import Excel
- [ ] Auto-netting tính đúng
- [ ] Run payroll → lương vào ví, nợ giảm
- [ ] Sổ cái giao dịch hoạt động
- [ ] Employee xem payslip

### Phase 6: Real-time & Polish (Sprint 8-10) ⬜
- [ ] WebSocket hoạt động (3 channels)
- [ ] Real-time wallet update
- [ ] Real-time request status
- [ ] Notification bell + dropdown
- [ ] 4 Dashboards có data thật
- [ ] 5 E2E test scenarios passed
- [ ] Bug fix completed
- [ ] Performance acceptable
- [ ] Security audit passed

---

## APPENDIX A: API ENDPOINT MAP (Quick Reference)

### Common (All Roles)
| Method | Endpoint | Sprint |
|--------|----------|:------:|
| POST | `/auth/login` | 1 |
| POST | `/auth/logout` | 1 |
| POST | `/auth/refresh-token` | 1 |
| POST | `/auth/forgot-password` | 1 |
| POST | `/auth/reset-password` | 1 |
| POST | `/auth/change-password` | 1 |
| GET | `/auth/me` | 1 |
| GET | `/users/me/profile` | 1 |
| PUT | `/users/me/profile` | 1 |
| PUT | `/users/me/avatar` | 1 |
| PUT | `/users/me/bank-info` | 1 |
| PUT | `/users/me/password` | 1 |
| POST | `/users/me/pin` | 1 |
| PUT | `/users/me/pin` | 1 |
| POST | `/users/me/pin/verify` | 1 |
| GET | `/banks` | 1 |
| GET | `/wallet` | 3 |
| GET | `/wallet/transactions` | 3 |
| POST | `/wallet/withdraw` | 3 |
| POST | `/wallet/deposit/generate-qr` | 3 |
| GET | `/projects` | 4 |
| GET | `/projects/:id/phases` | 4 |
| GET | `/files/signature` | 1 |
| DELETE | `/files/:publicId` | 1 |
| GET | `/payslips` | 7 |
| GET | `/payslips/:id` | 7 |
| GET | `/notifications` | 8 |
| PUT | `/notifications/:id/read` | 8 |
| PUT | `/notifications/read-all` | 8 |

### Employee
| Method | Endpoint | Sprint |
|--------|----------|:------:|
| GET | `/requests` | 5 |
| GET | `/requests/summary` | 5 |
| GET | `/requests/:id` | 5 |
| POST | `/requests` | 5 |
| PUT | `/requests/:id` | 5 |
| DELETE | `/requests/:id` | 5 |

### Manager
| Method | Endpoint | Sprint |
|--------|----------|:------:|
| GET | `/manager/approvals` | 5 |
| GET | `/manager/approvals/:id` | 5 |
| POST | `/manager/approvals/:id/approve` | 5 |
| POST | `/manager/approvals/:id/reject` | 5 |
| GET | `/manager/department/members` | 4 |
| GET | `/manager/department/members/:id` | 4 |
| GET | `/manager/projects` | 4 |
| GET | `/manager/projects/:id` | 4 |
| POST | `/manager/projects` | 4 |
| PUT | `/manager/projects/:id` | 4 |
| POST | `/manager/projects/:id/phases` | 4 |
| PUT | `/manager/projects/:id/phases/:phaseId` | 4 |
| POST | `/manager/projects/:id/members` | 4 |

### Accountant
| Method | Endpoint | Sprint |
|--------|----------|:------:|
| GET | `/accountant/disbursements` | 6 |
| GET | `/accountant/disbursements/:id` | 6 |
| POST | `/accountant/disbursements/:id/disburse` | 6 |
| POST | `/accountant/disbursements/:id/reject` | 6 |
| GET | `/accountant/payroll` | 7 |
| GET | `/accountant/payroll/:periodId` | 7 |
| POST | `/accountant/payroll` | 7 |
| GET | `/accountant/payroll/template` | 7 |
| POST | `/accountant/payroll/:periodId/import` | 7 |
| POST | `/accountant/payroll/:periodId/confirm-overwrite` | 7 |
| POST | `/accountant/payroll/:periodId/auto-netting` | 7 |
| POST | `/accountant/payroll/:periodId/run` | 7 |
| PUT | `/accountant/payroll/:periodId/entries/:payslipId` | 7 |
| GET | `/accountant/ledger` | 7 |
| GET | `/accountant/ledger/summary` | 7 |
| GET | `/accountant/ledger/:transactionId` | 7 |
| GET | `/accountant/requests/:requestId` | 6 |
| GET | `/accountant/payslips/:payslipId` | 7 |

### Admin
| Method | Endpoint | Sprint |
|--------|----------|:------:|
| GET | `/admin/approvals` | 6 |
| GET | `/admin/approvals/:id` | 6 |
| POST | `/admin/approvals/:id/approve` | 6 |
| POST | `/admin/approvals/:id/reject` | 6 |
| GET | `/admin/users` | 2 |
| GET | `/admin/users/:id` | 2 |
| POST | `/admin/users` | 2 |
| PUT | `/admin/users/:id` | 2 |
| POST | `/admin/users/:id/lock` | 2 |
| POST | `/admin/users/:id/unlock` | 2 |
| POST | `/admin/users/:id/reset-password` | 2 |
| GET | `/admin/departments` | 2 |
| GET | `/admin/departments/:id` | 2 |
| POST | `/admin/departments` | 2 |
| PUT | `/admin/departments/:id` | 2 |
| GET | `/admin/audit` | 2 |
| GET | `/admin/settings` | 2 |
| PUT | `/admin/settings` | 2 |

### WebSocket Channels
| Channel | Sprint |
|---------|:------:|
| `/user/queue/wallet` | 8 |
| `/user/queue/requests` | 8 |
| `/user/queue/notifications` | 8 |

---

## APPENDIX B: GANTT CHART (Text-Based)

```
Tuần     1  2  3  4  5  6  7  8  9  10 11 12 13 14 15 16 17 18 19 20 21 22
Sprint   ├──S0──┤  ├──S1──┤  ├──S2──┤  ├──S3──┤  ├──S4──┤  ├──S5──┤  ├──S6──┤  ├──S7──┤  ├──S8──┤  ├──S9──┤  ├─S10─┤

BE:
Auth     ████████████████
User           ██████████
File           ██████████
Org                  ██████████
Config               ██████████
Audit                ██████████
Wallet                     ██████████
Project                          ██████████
Request                                ██████████████████████
Accounting                                               ██████████
Notif                                                          ██████████
Dashboard                                                            ██████████

FE:
Infra    ██████████
Auth           ██████████
Settings       ██████████
Admin UI             ██████████
Wallet                     ██████████
Project                          ██████████
Request                                ██████████████████████
Payroll                                                  ██████████
WebSocket                                                      ██████████
Dashboard                                                            ██████████
Polish                                                                     ██████████
```

---

> **Ghi chú cuối:** Kế hoạch này là *living document* — cần được review và điều chỉnh tại mỗi Sprint Planning dựa trên velocity thực tế và feedback. Nếu team gặp blocker ở Sprint nào, PM sẽ re-prioritize và defer tasks ít critical sang Sprint sau.
