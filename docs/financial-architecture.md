# Financial Architecture — IFMS

## 4-Tier Wallet Model

```
External Bank
    ↓ SYSTEM_TOPUP
SystemFund Wallet (WalletOwnerType.SYSTEM_FUND)
    ↓ DEPT_QUOTA_ALLOCATION  (CFO approves DEPARTMENT_TOPUP)
Department Wallet (WalletOwnerType.DEPARTMENT)
    ↓ PROJECT_QUOTA_ALLOCATION  (Manager approves PROJECT_TOPUP)
Project Wallet (WalletOwnerType.PROJECT)
    ↓ REQUEST_PAYMENT  (Accountant pays out ADVANCE/EXPENSE)
User Wallet (WalletOwnerType.USER)
    ↓ ADVANCE_RETURN  (nhân viên hoàn trả tạm ứng dư)
Project Wallet
```

### Wallet Model

```java
Wallet { ownerType, ownerId, balance, lockedBalance, version }
// availableBalance = balance - lockedBalance
// lock()   → reservations (request approved, awaiting payout)
// settle() → finalize locked → debit
// debit()  → direct debit (no prior reservation)
// credit() → money in
```

### Double-Entry Ledger

Mỗi `Transaction` tạo đúng 2 `LedgerEntry` (DEBIT + CREDIT).
`LedgerEntry` là source of truth cho wallet history và balance reconstruction.
Entries **không bao giờ UPDATE/DELETE** — corrections dùng REVERSAL transaction.

---

## AdvanceBalance Lifecycle

Mỗi ADVANCE payout tạo 1 `AdvanceBalance` record.
Giảm bằng 2 cách:

1. **Reimburse** — REIMBURSE request approved → không có wallet movement, chỉ update AdvanceBalance
2. **Return Cash** — cash returned → ADVANCE_RETURN transaction, rút tiền từ USER wallet → PROJECT wallet

---

## Request Approval Flows (Segregation of Duties)

### Flow 1 — Personal Expense (ADVANCE / EXPENSE / REIMBURSE)
Nguy hiểm cao (chứng từ gốc, dễ gian lận) → **Accountant phải execute** (checkpoint cuối)

```
Member tạo request (PENDING)
    ↓
TEAM_LEADER duyệt (DECISION)
    ↓
APPROVED_BY_TEAM_LEADER
    ↓
PENDING_ACCOUNTANT_EXECUTION (chờ Accountant xem chứng từ + click giải ngân)
    ↓
ACCOUNTANT execute + giải ngân (EXECUTION)
    ↓
PAID
```

**Trách nhiệm:**
- TEAM_LEADER: Quyết định approve/reject
- ACCOUNTANT: Review chứng từ, verify số tiền, check quỹ, rồi execute giải ngân

**Wallet operation:** `walletService.settleAndTransfer(PROJECT → USER, REQUEST_PAYMENT)`

---

### Flow 2 — Project Fund Top-up (PROJECT_TOPUP)
Nguy hiểm trung bình (internal allocation) → **Auto-pay** sau Manager approve

```
TEAM_LEADER request vốn (PENDING)
    ↓
MANAGER duyệt (DECISION)
    ↓
APPROVED_BY_MANAGER
    ↓
[Scheduler auto-pay 1 phút sau]
    ↓
PAID (Department → Project transfer)
```

**Trách nhiệm:**
- MANAGER: Quyết định cấp vốn hay không
- ACCOUNTANT: Chỉ ghi sổ (post-facto)

**Wallet operation:** `walletService.transfer(DEPARTMENT → PROJECT, PROJECT_QUOTA_ALLOCATION)`

---

### Flow 3 — Department Quota Top-up (DEPARTMENT_TOPUP)
Nguy hiểm thấp (strategic decision) → **Auto-pay** sau CFO approve

```
MANAGER request quota (PENDING)
    ↓
CFO duyệt (DECISION)
    ↓
APPROVED_BY_CFO
    ↓
[Scheduler auto-pay 1 phút sau]
    ↓
PAID (SystemFund → Department transfer)
```

**Trách nhiệm:**
- CFO: Quyết định cấp quota phòng ban
- ACCOUNTANT: Chỉ ghi sổ (post-facto)

**Wallet operation:** `walletService.transfer(SYSTEM_FUND → DEPARTMENT, DEPT_QUOTA_ALLOCATION)`

---

## Request Status Enum

```java
public enum RequestStatus {
    // ─ ADVANCE/EXPENSE/REIMBURSE ─
    PENDING,                        // Member vừa tạo
    APPROVED_BY_TEAM_LEADER,        // TL duyệt, chờ Accountant execute
    PENDING_ACCOUNTANT_EXECUTION,   // Chờ Accountant giải ngân
    PAID,                           // Accountant đã giải ngân
    
    // ─ PROJECT_TOPUP ─
    APPROVED_BY_MANAGER,            // Manager duyệt, chờ auto-pay
    
    // ─ DEPARTMENT_TOPUP ─
    APPROVED_BY_CFO,                // CFO duyệt, chờ auto-pay
    
    // ─ Common ─
    REJECTED,                       // Reject tại decision stage
    CANCELLED                       // Cancel sau khi đã approve
}
```

---

## Segregation of Duties (SoD) Principle

| Loại request | Decision (duyệt) | Execution (giải ngân) |
|---|---|---|
| ADVANCE/EXPENSE/REIMBURSE | TEAM_LEADER | ACCOUNTANT ✅ |
| PROJECT_TOPUP | MANAGER | Scheduler (auto) ✅ |
| DEPARTMENT_TOPUP | CFO | Scheduler (auto) ✅ |

**Nguyên tắc:** Người decide ≠ Người execute → chống gian lận, dễ audit
