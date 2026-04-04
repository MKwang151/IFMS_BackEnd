# Implementation Status — IFMS (2026-04-04)

## ✅ Completed

| Component | Details |
|-----------|---------|
| **Auth Module** | Login, logout, refresh token, forgot password (OTP), reset password (OTP), change password |
| **JWT + Single-Session** | Access token (30m), refresh token (7d), token version cache (Redis), token version invalidation |
| **Audit Logging** | Async audit via Hibernate PostCommit → RabbitMQ → audit_logs table, AuditContextHolder (ThreadLocal), trace ID per-request |
| **Mail System** | RabbitMQ consumers (ONBOARD, FORGET_PASSWORD, WARNING), Brevo API integration, retry + DLQ, async publish |
| **File Upload** | Cloudinary signature generation, upload token pattern |
| **Database Schema** | All entities + Flyway migrations (V1–V11), PostgreSQL, sequences for business codes |
| **RBAC Model** | 6 roles (EMPLOYEE, TEAM_LEADER, MANAGER, ACCOUNTANT, CFO, ADMIN), dynamic permissions table, @EnableMethodSecurity |
| **Wallet Unified Model** | Wallet entity (4 owner types), balance + lockedBalance, available balance formula |
| **Double-Entry Ledger** | Transaction entity, LedgerEntry (append-only, DEBIT/CREDIT), reversal pattern |
| **AdvanceBalance** | Entity for tracking ADVANCE debt, reimburse + return cash lifecycle |
| **BusinessCodeGenerator** | Strategy pattern, 8 code types (EMPLOYEE, DEPARTMENT, PROJECT, PHASE, REQUEST, TRANSACTION, PERIOD, PAYSLIP), sequences + random hex |
| **Exception Handling** | BaseException + 10 custom exceptions, GlobalExceptionHandler with 15 handlers, cover BaseException, Auth, Validation, HTTP, DB, generic |
| **Seeded Data** | 10 seeded users across 6 roles (departments: IT, Sales, Finance) |

---

## ❌ Not Yet Implemented

| Component | Notes |
|-----------|-------|
| **WalletService** | Implement fund transfers (lock → settle → transfer), Transaction + LedgerEntry pair creation, balance calculations |
| **RequestService** | Implement submit/approve/reject/payout flow, wallet locking/unlocking, AdvanceBalance lifecycle, status transitions |
| **UserService** | CRUD user, update profile, PIN management (5-digit), change password, disable/enable account |
| **UserController** | GET /users/:id, PUT /users/:id, POST /users/{id}/pin, POST /users/{id}/change-password |
| **ProjectService** | Create project (assign budget), manage phases, allocate phase budget, add/remove members |
| **ProjectController** | CRUD project, manage phases, allocate budget, list members |
| **DepartmentService** | Create/update department, track quota consumption, allocate to projects |
| **DepartmentController** | CRUD department, view quota usage |
| **NotificationService** | WebSocket broadcast for notifications, persist to DB, read/unread tracking |
| **PayrollService** | Create payroll period, upload payslips, execute payout with advance netting |
| **PayrollController** | Upload payslips, execute payroll, view payslips |
| **Dashboard / Reporting** | Global dashboard (CFO), department dashboard (Manager), financial summary, charts |
| **POST /auth/reset-password** | Token validation in reset handler (currently has TODO comment) |

---

## Database Migrations (V1–V11)

| Version | Content |
|---------|---------|
| V1 | Init full schema (users, roles, permissions, wallets, transactions, requests, projects, etc.) |
| V2 | PostgreSQL sequences for business codes (employee, project, phase, request) |
| V3 | Add `description` to projects table |
| V4 | Add `token_version` to users, drop Spring Security boolean cols |
| V5 | Drop jwt_tokens table (stateless auth) |
| V6 | Refactor audit_logs: add `trace_id`, normalize action → INSERT/UPDATE/DELETE |
| V7 | Fix Hibernate 6 CHECK constraint on audit_logs.action |
| V8 | Refactor Wallet (unified owner model), Transaction (strip audit cols), add ledger_entries |
| V9 | Add `paid_at` to requests, add 3 indexes |
| V10 | Add advance_balances table, FK in requests |
| V11 | Fix seq_employee_code RESTART WITH 11 (stale from V2) |

---

## Tech Stack Summary

| Layer | Technology | Version |
|-------|-----------|---------|
| Framework | Spring Boot | 3.4.1 |
| Java | JDK | 21 |
| Security | Spring Security + JJWT | 0.12+ |
| ORM | Spring Data JPA + Hibernate | 6.x |
| DB | PostgreSQL | 13+ |
| Cache | Redis + Lettuce | 7.x |
| Messaging | RabbitMQ + Spring AMQP | 3.x |
| Email | Brevo API | transactional |
| WebSocket | Spring WebSocket + STOMP | 6.x |
| Docs | SpringDoc OpenAPI | 2.x |
| Build | Maven | 3.8+ |
| Lombok | Code generation | 1.18+ |

---

## Key Metrics / Targets

- **Request approval latency:** SLA by role (TL duyệt < 24h, Accountant execute < 1h)
- **Audit trail:** All financial operations logged
- **Wallet balance accuracy:** Double-entry ledger, ledger entries immutable
- **Single-session:** Only 1 token valid per user at any time (token version enforcement)
- **Role segregation:** Decision ≠ Execution (TEAM_LEADER duyệt, ACCOUNTANT thanh toán)
