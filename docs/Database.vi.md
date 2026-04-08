# IFMS - Tu dien CSDL (Tieng Viet)

## 1) Pham vi va nguon tai lieu

Tai lieu nay duoc tong hop tu:
- `src/main/resources/db/migration/Database.sql` (schema vat ly)
- `.claude/CLAUDE.md` (convention kien truc)
- `docs/API_Spec.md` (DB mapping theo API)

Muc tieu:
- mo ta tung bang va vai tro nghiep vu
- mo ta cac cot quan trong
- tong hop PK/UK/FK/index anh huong hanh vi he thong
- mo ta quan he giua cac bang va muc dich cua tung quan he

## 2) Nhom domain

- Dinh danh va to chuc: `users`, `roles`, `role_permissions`, `user_profiles`, `user_security_settings`, `departments`
- Du an va ngan sach: `projects`, `project_phases`, `project_members`, `phase_category_budgets`, `expense_categories`
- Vong doi request: `requests`, `request_histories`, `request_attachments`, `advance_balances`
- Vi va dong tien: `wallets`, `transactions`, `ledger_entries`, `withdrawal_requests`
- Ke toan va bang luong: `company_funds`, `payroll_periods`, `payslips`
- Dich vu he thong: `file_storages`, `notifications`, `audit_logs`, `system_configs`

## 3) Tong quan quan he (muc cao)

- `roles (1) -> (N) users`: moi user thuoc 1 role.
- `departments (1) -> (N) users`: user co the thuoc 1 phong ban.
- `users (1) -> (1) user_profiles`: profile mo rong (nhan su/ngan hang).
- `users (1) -> (1) user_security_settings`: PIN giao dich + retry/lock.
- `users (1) -> (N) projects` qua `projects.manager_id`: quan ly du an.
- `departments (1) -> (N) projects`: phong ban so huu du an.
- `projects (1) -> (N) project_phases`: du an co nhieu phase.
- `projects (N) <-> (N) users` qua `project_members`: nhan su tham gia du an.
- `project_phases (N) <-> (N) expense_categories` qua `phase_category_budgets`: han muc theo phase + category.
- `users (1) -> (N) requests`: user la nguoi tao request.
- `requests (1) -> (N) request_histories`: timeline duyet/xu ly.
- `requests (N) <-> (N) file_storages` qua `request_attachments`: chung tu dinh kem.
- `wallets (1) -> (N) ledger_entries` va `transactions (1) -> (N) ledger_entries`: but toan kep.
- `users (1) -> (N) withdrawal_requests`: yeu cau rut tien cua user.
- `payroll_periods (1) -> (N) payslips` va `users (1) -> (N) payslips`: phieu luong theo ky.

## 4) Tu dien bang

> Ghi chu: Phan nay giu cau truc tuong duong ban tieng Anh (`docs/Database.md`) de doi chieu de dang.

### 4.1 `users`

Muc dich: tai khoan he thong, dinh danh dang nhap, gan RBAC, trang thai vong doi.

| Cot | Kieu | Y nghia |
|---|---|---|
| `id` | BIGINT PK | Dinh danh user. |
| `created_at`, `updated_at` | TIMESTAMP | Moc thoi gian audit (`BaseEntity`). |
| `created_by`, `updated_by` | BIGINT | Actor tao/cap nhat. |
| `email` | VARCHAR(255), UNIQUE | Dinh danh dang nhap. |
| `password` | VARCHAR(255) | Mat khau da hash. |
| `full_name` | VARCHAR(255) | Ho ten hien thi. |
| `is_first_login` | BOOLEAN | Co bat buoc setup lan dau hay khong. |
| `role_id` | BIGINT FK -> `roles.id` | Role RBAC. |
| `department_id` | BIGINT FK -> `departments.id`, nullable | Phong ban. |
| `status` | VARCHAR(20) | Trang thai tai khoan. |
| `token_version` | INTEGER | Version JWT de vo hieu hoa token cu. |

### 4.2 `roles`

Muc dich: danh muc role RBAC.

| Cot | Kieu | Y nghia |
|---|---|---|
| `id` | BIGINT PK | Dinh danh role. |
| `created_at`, `updated_at`, `created_by`, `updated_by` | Base fields | Metadata. |
| `name` | VARCHAR(50), UNIQUE | Ten role (`ADMIN`, `MANAGER`, ...). |
| `description` | VARCHAR(255) | Mo ta role. |

### 4.3 `role_permissions`

Muc dich: bang anh xa role -> permission.

| Cot | Kieu | Y nghia |
|---|---|---|
| `role_id` | BIGINT FK -> `roles.id` | Role so huu quyen. |
| `permission` | VARCHAR(50) | Quyen theo format `RESOURCE_ACTION`. |

### 4.4 `departments`

Muc dich: don vi to chuc va han muc ngan sach cap phong ban.

| Cot | Kieu | Y nghia |
|---|---|---|
| `id` | BIGINT PK | ID phong ban. |
| `created_at`, `updated_at`, `created_by`, `updated_by` | Base fields | Metadata. |
| `name` | VARCHAR(255) | Ten phong ban. |
| `code` | VARCHAR(20), UNIQUE | Ma phong ban. |
| `manager_id` | BIGINT FK -> `users.id`, nullable | Nguoi quan ly phong ban. |
| `total_project_quota` | DECIMAL(19,2) | Tong quota cho du an. |
| `total_available_balance` | DECIMAL(19,2) | So du con co the cap phat. |

### 4.5 `user_profiles`

Muc dich: thong tin mo rong cua user (HR + ngan hang + avatar).

| Cot | Kieu | Y nghia |
|---|---|---|
| `user_id` | BIGINT PK/FK -> `users.id` | Khoa 1-1 voi `users`. |
| `employee_code` | VARCHAR(20), UNIQUE | Ma nhan vien nghiep vu. |
| `job_title` | VARCHAR(100) | Chuc danh. |
| `phone_number` | VARCHAR(15), UNIQUE | So dien thoai. |
| `date_of_birth` | DATE | Ngay sinh. |
| `citizen_id` | VARCHAR(20) | CCCD/giay to dinh danh. |
| `address` | VARCHAR(255) | Dia chi. |
| `avatar_file_id` | BIGINT FK -> `file_storages.id` | File avatar. |
| `bank_name` | VARCHAR(100) | Ngan hang mac dinh. |
| `bank_account_num` | VARCHAR(30) | So tai khoan. |
| `bank_account_owner` | VARCHAR(100) | Chu tai khoan. |

### 4.6 `user_security_settings`

Muc dich: cau hinh bao mat giao dich cua user.

| Cot | Kieu | Y nghia |
|---|---|---|
| `user_id` | BIGINT PK/FK -> `users.id` | Khoa 1-1 voi user. |
| `created_at`, `updated_at`, `created_by`, `updated_by` | Base fields | Metadata. |
| `transaction_pin` | VARCHAR(100) | PIN hash. |
| `retry_count` | INTEGER | So lan nhap PIN sai. |
| `locked_until` | TIMESTAMP | Thoi diem mo khoa PIN. |

### 4.7 `file_storages`

Muc dich: luu metadata file da upload.

| Cot | Kieu | Y nghia |
|---|---|---|
| `id` | BIGINT PK | ID file dung de tham chieu. |
| `created_at`, `updated_at`, `created_by`, `updated_by` | Base fields | Metadata. |
| `file_name` | VARCHAR(255) | Ten file hien thi/goc. |
| `cloudinary_public_id` | VARCHAR(255), UNIQUE | Dinh danh tai Cloudinary. |
| `url` | TEXT | URL gui ve client. |
| `file_type` | VARCHAR(100) | MIME/type. |
| `size` | BIGINT | Kich thuoc byte. |

### 4.8 `projects`

Muc dich: ho so du an + thong tin ngan sach tong.

| Cot | Kieu | Y nghia |
|---|---|---|
| `id` | BIGINT PK | ID du an. |
| `created_at`, `updated_at`, `created_by`, `updated_by` | Base fields | Metadata. |
| `project_code` | VARCHAR(30), UNIQUE | Ma du an nghiep vu. |
| `name` | VARCHAR(255) | Ten du an. |
| `description` | TEXT | Mo ta du an. |
| `department_id` | BIGINT FK -> `departments.id` | Phong ban so huu. |
| `manager_id` | BIGINT FK -> `users.id` | Quan ly du an. |
| `total_budget` | DECIMAL(19,2) | Tong ngan sach muc tieu. |
| `available_budget` | DECIMAL(19,2) | Ngan sach con lai. |
| `total_spent` | DECIMAL(19,2) | Tong da chi. |
| `status` | VARCHAR(20) | Trang thai du an. |
| `current_phase_id` | BIGINT FK -> `project_phases.id`, UNIQUE, nullable | Phase hien tai. |

### 4.9 `project_phases`

Muc dich: chia nho du an theo giai doan.

| Cot | Kieu | Y nghia |
|---|---|---|
| `id` | BIGINT PK | ID phase. |
| `created_at`, `updated_at`, `created_by`, `updated_by` | Base fields | Metadata. |
| `phase_code` | VARCHAR(30), UNIQUE | Ma phase nghiep vu. |
| `project_id` | BIGINT FK -> `projects.id` | Du an cha. |
| `name` | VARCHAR(255) | Ten phase. |
| `budget_limit` | DECIMAL(19,2) | Han muc phase. |
| `current_spent` | DECIMAL(19,2) | Da chi trong phase. |
| `status` | VARCHAR(20) | Trang thai phase. |
| `start_date`, `end_date` | DATE | Khoang thoi gian. |

### 4.10 `project_members`

Muc dich: bang lien ket N-N giua user va project.

| Cot | Kieu | Y nghia |
|---|---|---|
| `project_id` | BIGINT PK/FK -> `projects.id` | Du an. |
| `user_id` | BIGINT PK/FK -> `users.id` | Thanh vien. |
| `project_role` | VARCHAR(20) | Vai tro trong du an. |
| `position` | VARCHAR(100) | Vi tri/chuc danh hien thi. |
| `joined_at` | TIMESTAMP | Thoi diem tham gia. |

### 4.11 `expense_categories`

Muc dich: danh muc loai chi phi.

| Cot | Kieu | Y nghia |
|---|---|---|
| `id` | BIGINT PK | ID category. |
| `name` | VARCHAR(255), UNIQUE | Ten category. |
| `description` | TEXT | Mo ta. |
| `is_system_default` | BOOLEAN | Co phai du lieu mac dinh he thong. |
| `created_at`, `updated_at`, `created_by`, `updated_by` | Metadata | Metadata audit. |

### 4.12 `phase_category_budgets`

Muc dich: han muc theo cap `phase + category`.

| Cot | Kieu | Y nghia |
|---|---|---|
| `phase_id` | BIGINT PK/FK -> `project_phases.id` | Phase. |
| `category_id` | BIGINT PK/FK -> `expense_categories.id` | Category. |
| `budget_limit` | DECIMAL(19,2) | Han muc cap nay. |
| `current_spent` | DECIMAL(19,2) | Da chi cap nay. |

### 4.13 `requests`

Muc dich: bang trung tam luu yeu cau nghiep vu (advance/expense/reimburse/topup...).

| Cot | Kieu | Y nghia |
|---|---|---|
| `id` | BIGINT PK | ID request. |
| `created_at`, `updated_at`, `created_by`, `updated_by` | Base fields | Metadata. |
| `request_code` | VARCHAR(30), UNIQUE | Ma request nghiep vu. |
| `requester_id` | BIGINT FK -> `users.id` | Nguoi tao request. |
| `project_id` | BIGINT FK -> `projects.id`, nullable | Du an lien quan. |
| `phase_id` | BIGINT FK -> `project_phases.id`, nullable | Phase lien quan. |
| `category_id` | BIGINT FK -> `expense_categories.id`, nullable | Loai chi phi lien quan. |
| `type` | VARCHAR(20) | Loai request. |
| `advance_balance_id` | BIGINT FK -> `advance_balances.id`, nullable | Cong no tam ung lien ket. |
| `amount` | DECIMAL(19,2) | So tien de xuat. |
| `approved_amount` | DECIMAL(19,2), nullable | So tien duoc duyet. |
| `status` | VARCHAR(25) | Trang thai workflow. |
| `reject_reason` | TEXT | Ly do tu choi. |
| `description` | TEXT | Noi dung request. |
| `paid_at` | TIMESTAMP, nullable | Moc da chi tra. |

### 4.14 `request_histories`

Muc dich: lich su thao tac theo request (append-only).

| Cot | Kieu | Y nghia |
|---|---|---|
| `id` | BIGINT PK | ID lich su. |
| `request_id` | BIGINT FK -> `requests.id` | Request cha. |
| `actor_id` | BIGINT FK -> `users.id` | Nguoi thao tac. |
| `action` | VARCHAR(20) | Hanh dong (`APPROVE/REJECT/...`). |
| `status_after_action` | VARCHAR(25) | Trang thai sau hanh dong. |
| `comment` | VARCHAR(500) | Ghi chu. |
| `created_at` | TIMESTAMP | Thoi diem thao tac. |

### 4.15 `request_attachments`

Muc dich: lien ket N-N giua request va file dinh kem.

| Cot | Kieu | Y nghia |
|---|---|---|
| `request_id` | BIGINT PK/FK -> `requests.id` | Request so huu. |
| `file_id` | BIGINT PK/FK -> `file_storages.id` | File chung tu. |

### 4.16 `advance_balances`

Muc dich: theo doi cong no tam ung cho den khi tat toan.

| Cot | Kieu | Y nghia |
|---|---|---|
| `id` | BIGINT PK | ID cong no tam ung. |
| `user_id` | BIGINT FK -> `users.id` | Chu no (nhan vien). |
| `advance_request_id` | BIGINT FK -> `requests.id`, UNIQUE | Request tam ung goc. |
| `original_amount` | DECIMAL(19,2) | Tien tam ung ban dau. |
| `reimbursed_amount` | DECIMAL(19,2) | Da quyet toan qua reimburse. |
| `returned_amount` | DECIMAL(19,2) | Da hoan tra truc tiep. |
| `remaining_amount` | DECIMAL(19,2) | Con no. |
| `status` | VARCHAR(20) | Trang thai OPEN/SETTLED... |
| `settled_at` | TIMESTAMP, nullable | Moc tat toan. |
| `created_at` | TIMESTAMP | Moc tao. |

### 4.17 `wallets`

Muc dich: vi so huu boi 1 owner (`owner_type`, `owner_id`).

| Cot | Kieu | Y nghia |
|---|---|---|
| `id` | BIGINT PK | ID vi. |
| `created_at`, `updated_at`, `created_by`, `updated_by` | Base fields | Metadata. |
| `owner_type` | VARCHAR(20) | Loai owner (`USER`, ...). |
| `owner_id` | BIGINT | ID owner. |
| `balance` | DECIMAL(19,2) | So du tong. |
| `locked_balance` | DECIMAL(19,2) | So du bi khoa tam thoi. |
| `version` | BIGINT | Version optimistic locking. |

### 4.18 `transactions`

Muc dich: header giao dich tai chinh + tham chieu nghiep vu.

| Cot | Kieu | Y nghia |
|---|---|---|
| `id` | BIGINT PK | ID giao dich. |
| `transaction_code` | VARCHAR(30), UNIQUE | Ma giao dich nghiep vu. |
| `amount` | DECIMAL(19,2) | So tien giao dich. |
| `type` | VARCHAR(30) | Loai giao dich. |
| `status` | VARCHAR(20) | Trang thai xu ly. |
| `payment_ref` | VARCHAR(100) | Ma tham chieu cong thanh toan. |
| `gateway_provider` | VARCHAR(20) | Nha cung cap cong thanh toan. |
| `reference_type` | VARCHAR(30) | Loai doi tuong tham chieu. |
| `reference_id` | BIGINT | ID doi tuong tham chieu. |
| `description` | TEXT | Mo ta giao dich. |
| `created_at` | TIMESTAMP | Moc tao. |

### 4.19 `ledger_entries`

Muc dich: but toan no/co append-only gan transaction va wallet.

| Cot | Kieu | Y nghia |
|---|---|---|
| `id` | BIGINT PK | ID but toan. |
| `transaction_id` | BIGINT FK -> `transactions.id` | Giao dich cha. |
| `wallet_id` | BIGINT FK -> `wallets.id` | Vi bi tac dong. |
| `direction` | VARCHAR(10) | Chieu but toan (`DEBIT/CREDIT`). |
| `amount` | DECIMAL(19,2) | So tien but toan. |
| `balance_after` | DECIMAL(19,2) | So du vi sau but toan. |
| `created_at` | TIMESTAMP | Moc ghi so. |

### 4.20 `withdrawal_requests`

Muc dich: workflow rut tien cua user den luc ke toan xu ly.

| Cot | Kieu | Y nghia |
|---|---|---|
| `id` | BIGINT PK | ID yeu cau rut. |
| `created_at`, `updated_at`, `created_by`, `updated_by` | Base fields | Metadata. |
| `withdraw_code` | VARCHAR(30), UNIQUE | Ma rut tien nghiep vu. |
| `user_id` | BIGINT FK -> `users.id` | User tao yeu cau. |
| `amount` | DECIMAL(19,2) | So tien rut. |
| `credit_account` | VARCHAR(30) | Tai khoan nhan. |
| `credit_account_name` | VARCHAR(100) | Chu tai khoan nhan. |
| `credit_bank_code` | VARCHAR(20) | Ma ngan hang nhan. |
| `credit_bank_name` | VARCHAR(100) | Ten ngan hang nhan. |
| `user_note` | VARCHAR(500) | Ghi chu user. |
| `status` | VARCHAR(20) | Trang thai xu ly. |
| `bank_transaction_id` | VARCHAR(50) | Ma giao dich ngan hang. |
| `accountant_note` | VARCHAR(500) | Ghi chu ke toan. |
| `executed_by` | BIGINT | Nguoi thuc thi (dang de logical ref). |
| `executed_at` | TIMESTAMP | Moc thuc thi. |
| `transaction_id` | BIGINT | Logical ref toi transaction. |
| `failure_reason` | VARCHAR(500) | Ly do that bai. |

### 4.21 `payroll_periods`

Muc dich: dinh nghia ky luong.

| Cot | Kieu | Y nghia |
|---|---|---|
| `id` | BIGINT PK | ID ky luong. |
| `created_at`, `updated_at`, `created_by`, `updated_by` | Base fields | Metadata. |
| `period_code` | VARCHAR(30), UNIQUE | Ma ky luong. |
| `name` | VARCHAR(255) | Ten ky luong. |
| `month`, `year` | INTEGER | Thang/nam ky luong. |
| `start_date`, `end_date` | DATE | Khoang thoi gian. |
| `status` | VARCHAR(20) | Trang thai ky luong. |

### 4.22 `payslips`

Muc dich: ket qua tinh luong cua 1 user trong 1 ky.

| Cot | Kieu | Y nghia |
|---|---|---|
| `id` | BIGINT PK | ID phieu luong. |
| `created_at`, `updated_at`, `created_by`, `updated_by` | Base fields | Metadata. |
| `payslip_code` | VARCHAR(30), UNIQUE | Ma phieu luong. |
| `period_id` | BIGINT FK -> `payroll_periods.id` | Ky luong. |
| `user_id` | BIGINT FK -> `users.id` | Nhan vien. |
| `base_salary` | DECIMAL(19,2) | Luong co ban. |
| `bonus` | DECIMAL(19,2) | Thuong. |
| `allowance` | DECIMAL(19,2) | Phu cap. |
| `deduction` | DECIMAL(19,2) | Khau tru. |
| `advance_deduct` | DECIMAL(19,2) | Khau tru cong no tam ung. |
| `final_net_salary` | DECIMAL(19,2) | Luong thuc linh. |
| `status` | VARCHAR(20) | Trang thai phieu luong. |
| `payment_date` | TIMESTAMP | Ngay chi tra thuc te. |

### 4.23 `company_funds`

Muc dich: thong tin treasury/bank cua cong ty (metadata snapshot).

| Cot | Kieu | Y nghia |
|---|---|---|
| `id` | BIGINT PK | ID record. |
| `created_at`, `updated_at`, `created_by`, `updated_by` | Base fields | Metadata. |
| `bank_account` | VARCHAR(30) | Tai khoan ngan hang chinh. |
| `bank_name` | VARCHAR(100) | Ten ngan hang. |
| `external_bank_balance` | DECIMAL(19,2) | So du doi soat ben ngoai. |
| `last_statement_date` | DATE | Ngay sao ke gan nhat. |
| `last_statement_updated_by` | BIGINT | Nguoi cap nhat sao ke. |

### 4.24 `notifications`

Muc dich: hop thong bao trong ung dung.

| Cot | Kieu | Y nghia |
|---|---|---|
| `id` | BIGINT PK | ID thong bao. |
| `created_at`, `updated_at`, `created_by`, `updated_by` | Base fields | Metadata. |
| `user_id` | BIGINT FK -> `users.id` | Nguoi nhan. |
| `title` | VARCHAR(255) | Tieu de. |
| `message` | TEXT | Noi dung. |
| `type` | VARCHAR(30) | Loai thong bao. |
| `ref_id` | BIGINT | ID doi tuong lien quan (optional). |
| `ref_type` | VARCHAR(50) | Loai doi tuong lien quan (optional). |
| `is_read` | BOOLEAN | Da doc/chua doc. |

### 4.25 `audit_logs`

Muc dich: nhat ky audit append-only.

| Cot | Kieu | Y nghia |
|---|---|---|
| `id` | BIGINT PK | ID audit. |
| `trace_id` | VARCHAR(36) | Correlation ID. |
| `actor_id` | BIGINT FK -> `users.id`, nullable | Nguoi thuc hien hanh dong. |
| `action` | VARCHAR(50) | Ten hanh dong. |
| `entity_name` | VARCHAR(100) | Bang/thuc the bi tac dong. |
| `entity_id` | VARCHAR(100) | ID doi tuong bi tac dong. |
| `old_values` | JSONB | Snapshot cu. |
| `new_values` | JSONB | Snapshot moi. |
| `created_at` | TIMESTAMP | Moc phat sinh. |

### 4.26 `system_configs`

Muc dich: kho cau hinh dang key-value trong DB.

| Cot | Kieu | Y nghia |
|---|---|---|
| `config_key` | VARCHAR(100) PK | Khoa cau hinh. |
| `created_at`, `updated_at`, `created_by`, `updated_by` | Base fields | Metadata. |
| `config_value` | VARCHAR(255) | Gia tri cau hinh. |
| `description` | TEXT | Mo ta muc dich su dung. |

## 5) Danh muc quan he chi tiet va muc dich

- `users.role_id -> roles.id`: gan role co tham quyen cho user.
- `users.department_id -> departments.id`: phuc vu phan quyen/scope theo phong ban.
- `departments.manager_id -> users.id`: xac dinh nguoi chiu trach nhiem phong ban.
- `user_profiles.user_id -> users.id`: tach profile khoi tai khoan dang nhap (1-1).
- `user_profiles.avatar_file_id -> file_storages.id`: tai su dung metadata file cho avatar.
- `user_security_settings.user_id -> users.id`: bo cau hinh PIN rieng cho user (1-1).
- `projects.department_id -> departments.id`: du an thuoc phong ban nao.
- `projects.manager_id -> users.id`: nguoi quan ly du an.
- `projects.current_phase_id -> project_phases.id`: phase hien tai cua du an.
- `project_phases.project_id -> projects.id`: phase thuoc 1 du an.
- `project_members.project_id -> projects.id`, `project_members.user_id -> users.id`: staffing N-N.
- `phase_category_budgets.phase_id -> project_phases.id`, `phase_category_budgets.category_id -> expense_categories.id`: han muc theo cap phase/category.
- `requests.requester_id -> users.id`: ownership cua request.
- `requests.project_id/phase_id/category_id`: gan request vao ngu canh ngan sach.
- `requests.advance_balance_id -> advance_balances.id`: lien ket quyet toan voi cong no tam ung.
- `advance_balances.user_id -> users.id`: cong no theo tung nhan vien.
- `advance_balances.advance_request_id -> requests.id` (unique): 1 request tam ung goc -> 1 record cong no.
- `request_histories.request_id -> requests.id`, `request_histories.actor_id -> users.id`: audit timeline co actor.
- `request_attachments.request_id -> requests.id`, `request_attachments.file_id -> file_storages.id`: chung tu dinh kem.
- `notifications.user_id -> users.id`: thong bao theo user.
- `audit_logs.actor_id -> users.id`: truy vet nguoi thao tac.
- `payslips.period_id -> payroll_periods.id`, `payslips.user_id -> users.id`: bang luong theo ky va nhan vien.
- `ledger_entries.transaction_id -> transactions.id`, `ledger_entries.wallet_id -> wallets.id`: but toan cho giao dich va vi.
- `withdrawal_requests.user_id -> users.id`: ownership rut tien.
- `role_permissions.role_id -> roles.id`: mo rong quyen theo role.

## 6) Luu y va diem can canh bao

- `withdrawal_requests.transaction_id` hien la logical reference, chua co FK vat ly trong schema nay.
- `withdrawal_requests.executed_by` cung chua co FK vat ly toi `users.id`.
- `requests` va `advance_balances` dang tao lien ket 2 chieu qua FK (`requests.advance_balance_id` va `advance_balances.advance_request_id`) de phuc vu vong doi tam ung/quyet toan.
- Tien te su dung dong nhat `DECIMAL(19,2)`.
- Cac bang co tinh chat append-only ro rang: `audit_logs`, `ledger_entries`, `request_histories`.

