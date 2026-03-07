# 📐 RÀNG BUỘC KỸ THUẬT (Technical Stack & Constraints)

> **Dự án:** Internal Finance Management System (IFMS)
> **Cập nhật:** 07/03/2026
> **Mục đích:** File này là nguồn sự thật duy nhất (Single Source of Truth) cho mọi quyết định kỹ thuật. AI Agent và Developer PHẢI tuân thủ nghiêm ngặt khi sinh code.

---

## 1. RUNTIME & LANGUAGE

| Item | Giá trị | Ghi chú |
|:---|:---|:---|
| Java | **21** (LTS) | Sử dụng tính năng Java 21: Record, Pattern Matching, Virtual Threads (future) |
| Spring Boot | **3.4.1** | Quản lý bởi `spring-boot-starter-parent` |
| Build Tool | **Maven** | Wrapper: `mvnw` / `mvnw.cmd` |
| Package structure | `com.mkwang.backend` | Modular Monolith — mỗi module nằm trong `modules/{name}` |

---

## 2. DEPENDENCIES CHÍNH (pom.xml)

### 2.1. Spring Starters
| Dependency | Mục đích |
|:---|:---|
| `spring-boot-starter-web` | REST API (Tomcat embedded) |
| `spring-boot-starter-security` | Authentication & Authorization |
| `spring-boot-starter-oauth2-client` | OAuth2 (Reserved) |
| `spring-boot-starter-validation` | Bean Validation (`@Valid`, `@NotNull`, etc.) |
| `spring-boot-starter-data-jpa` | JPA/Hibernate ORM |
| `spring-boot-starter-data-redis` | Redis client (Lettuce) — Email Queue, Caching |
| `spring-boot-starter-mail` | JavaMailSender — SMTP |
| `spring-boot-starter-thymeleaf` | HTML Email templates |

### 2.2. Libraries
| Dependency | Version | Mục đích |
|:---|:---|:---|
| `postgresql` | Runtime | JDBC driver cho PostgreSQL |
| `jjwt-api/impl/jackson` | **0.12.6** | JWT token generation/validation |
| `springdoc-openapi-starter-webmvc-ui` | **2.8.6** | Swagger UI (`/swagger-ui.html`) |
| `cloudinary-http44` | **1.39.0** | File upload/management trên Cloudinary |
| `lombok` | **1.18.38** | Boilerplate reduction (`@Getter`, `@Builder`, etc.) |
| `flyway-core` + `flyway-database-postgresql` | Managed | Database schema versioning |

### 2.3. Annotation Processor
```xml
<!-- maven-compiler-plugin annotationProcessorPaths: -->
1. lombok (only)
```
> ⚠️ **KHÔNG dùng MapStruct.** DTO ↔ Entity mapping được triển khai thủ công bằng `@Service` class để dễ kiểm soát, debug, và xử lý logic phức tạp. Xem mục 11 (Coding Conventions) để biết quy ước.

---

## 3. DATABASE

| Item | Giá trị |
|:---|:---|
| Engine | **PostgreSQL** |
| Connection Pool | **HikariCP** (min-idle: 10, max-pool: 30) |
| Schema Migration | **Flyway** (`classpath:db/migration/V{N}__*.sql`) |
| DDL Strategy | `ddl-auto: validate` — Hibernate CHỈ validate, KHÔNG tự tạo/sửa schema |
| JSON Column | PostgreSQL `JSONB` type — map bằng `@JdbcTypeCode(SqlTypes.JSON)` |

### 3.1. Quy tắc Migration Files
- **Tên file:** `V{N}__{DESCRIPTION}.sql` (2 dấu gạch dưới)
- **KHÔNG BAO GIỜ** sửa file migration đã chạy. Tạo file mới (V3, V4...) cho thay đổi.
- Development: có thể sửa V1 nếu drop schema + re-migrate.
- Sequences cho Business Code nằm trong `V2__ADD_BUSINESS_CODE_SEQUENCES.sql`

### 3.2. Quy tắc Entity
- Entity có audit fields (`created_at`, `updated_at`, `created_by`, `updated_by`) → **extends `BaseEntity`**
- Entity append-only (audit_logs, request_histories) → **KHÔNG extends BaseEntity**, tự khai báo `created_at`
- Optimistic Locking: Dùng `@Version` trên entity có concurrent write (Wallet, Department, Project)
- Enum mapping: **`@Enumerated(EnumType.STRING)`** — KHÔNG dùng `EnumType.ORDINAL`
- Decimal/Money: **`precision = 19, scale = 2`** — kiểu `BigDecimal`
- FetchType: **`FetchType.LAZY`** cho tất cả `@ManyToOne`, `@OneToOne`
- Composite PK: Dùng `@EmbeddedId` + `@Embeddable` class

---

## 4. SECURITY

### 4.1. Authentication
| Item | Chi tiết |
|:---|:---|
| Cơ chế | **JWT (Access Token + Refresh Token)** |
| Thuật toán | HMAC-SHA256 (`HS256`) |
| Access Token TTL | 30 phút (`1800000ms`) |
| Refresh Token TTL | 7 ngày (`604800000ms`) |
| Token Storage | Bảng `jwt_tokens` trong DB (hỗ trợ revoke/blacklist) |
| Password Hash | **BCrypt** (Spring Security default encoder) |

### 4.2. Authorization
| Item | Chi tiết |
|:---|:---|
| Cơ chế | **Dynamic RBAC** |
| Roles | Lưu trong DB (`roles` table), tạo/sửa runtime bởi Admin |
| Permissions | **Enum cứng** trong code (`Permission.java`), gán cho Role qua `role_permissions` |
| UserDetails | `UserDetailsAdapter` wraps `User` entity → Spring Security `UserDetails` |
| Authorities | `ROLE_{name}` + tất cả `Permission` enum values từ Role |

### 4.3. Transaction PIN (Bảo mật cấp 2)
| Item | Chi tiết                                          |
|:---|:--------------------------------------------------|
| Độ dài | 5 số                                              |
| Hash | BCrypt (giống password)                           |
| Max retry | 5 lần (configurable qua `application.yml`)        |
| Lock duration | 5 phút                                            |
| Cấu hình | Inject từ `application.yml` → `PinValidator.java` |

---

## 5. API & SERIALIZATION

| Item | Chi tiết |
|:---|:---|
| API Format | RESTful JSON |
| Response Wrapper | `ApiResponse<T>` (`success`, `message`, `data`, `errors`) |
| Null handling | `spring.jackson.default-property-inclusion: non_null` — field null bị loại khỏi JSON |
| DateTime format | ISO-8601: `yyyy-MM-dd'T'HH:mm:ss` |
| API Docs | SpringDoc OpenAPI 3 → Swagger UI tại `/swagger-ui.html` |
| Validation | `spring-boot-starter-validation` (`@Valid`, `@NotBlank`, `@Size`, etc.) |
| Exception Handling | `GlobalExceptionHandler` (`@ControllerAdvice`) |

---

## 6. FILE STORAGE (Cloudinary)

| Item | Chi tiết |
|:---|:---|
| SDK | `cloudinary-http44` v1.39.0 |
| Access Mode | `authenticated` (Private — không public URL) |
| Root Folder | Configurable: `${CLOUDINARY_ROOT_FOLDER:ifms}` |
| Max File Size | 10MB per file |
| Max Request Size | 20MB (multiple files) |
| URL Delivery | Backend sinh **Signed URL** có thời hạn (không trả raw URL) |

---

## 7. ASYNC & BACKGROUND JOBS

### 7.1. Spring Async TaskExecutor
| Item | Chi tiết |
|:---|:---|
| Core Pool | 10 threads |
| Max Pool | 20 threads |
| Queue Capacity | 100 |
| Thread Prefix | `ifms-async-` |

### 7.2. Email Queue (Redis-based)
| Item | Chi tiết |
|:---|:---|
| Cơ chế | **Redis List** (Producer-Consumer pattern, KHÔNG dùng Pub/Sub) |
| Queue Name | `ifms:email:queue` |
| Dead Letter Queue | `ifms:email:dead-letter` |
| Max Retry | 3 lần |
| Worker Poll | 2000ms (BLPOP / leftPop) |
| Serialization | **Jackson JSON** (EmailPayload ↔ JSON string) |
| Template Engine | Thymeleaf (HTML files trong `resources/templates/`) |

---

## 8. CACHING & REDIS

| Item | Chi tiết |
|:---|:---|
| Client | **Lettuce** (non-blocking, default Spring Boot) |
| Pool | min-idle: 2, max-idle: 8, max-active: 16 |
| Timeout | 5000ms |
| Chức năng hiện tại | Email Message Queue |
| Chức năng tương lai | API Response Cache, Rate Limiting |

---

## 9. BUSINESS CODE GENERATION

| Item | Chi tiết |
|:---|:---|
| Pattern | **Strategy Pattern** + PostgreSQL Sequences |
| Sequence Service | `SequenceService.java` — gọi `SELECT nextval('seq_name')` |
| Strategy Registry | `BusinessCodeStrategyRegistry.java` — map `BusinessCodeType` → Strategy |
| Code Generator | `BusinessCodeGenerator.java` — facade gọi registry |
| Propagation | `@Transactional(propagation = REQUIRES_NEW)` — sequence value không bị rollback |

### Business Code Formats
| Type | Format | Example | Sequence |
|:---|:---|:---|:---|
| EMPLOYEE | `MK{SEQ:3}` | `MK001` | `seq_employee_code` |
| PROJECT | `PRJ-{SLUG}-{YEAR}` | `PRJ-ERP-2026` | — |
| PHASE | `PH-{SLUG}-{SEQ:2}` | `PH-UIUX-01` | `seq_phase_code` |
| REQUEST | `REQ-{DEPT}-{MMYY}-{SEQ:3}` | `REQ-IT-0326-001` | `seq_request_code` |
| TRANSACTION | `TXN-{HEX:8}` | `TXN-8829145A` | — (random hex) |
| PERIOD | `PR-{YEAR}-{MM}` | `PR-2026-03` | — |
| PAYSLIP | `PSL-{EMP}-{MMYY}` | `PSL-MK001-0326` | — |

---

## 10. PROJECT STRUCTURE (Source Code Layout)

```
src/main/java/com/mkwang/backend/
├── BackendApplication.java           # @SpringBootApplication entry point
├── common/                           # Shared utilities
│   ├── base/BaseEntity.java          # Audit fields (created_at, updated_at, etc.)
│   ├── dto/ApiResponse.java          # Standard API response wrapper
│   ├── exception/                    # GlobalExceptionHandler, custom exceptions
│   └── utils/                        # PinValidator, BusinessCodeGenerator, etc.
├── config/                           # Application configuration
│   ├── SecurityConfig.java           # Spring Security filter chain
│   ├── DataInitializer.java          # Seed data (Roles, Departments, Users, etc.)
│   ├── CloudinaryConfig.java         # Cloudinary SDK bean
│   ├── RedisConfig.java              # Redis connection + StringRedisTemplate
│   ├── OpenApiConfig.java            # Swagger/OpenAPI configuration
│   ├── JpaAuditingConfig.java        # @EnableJpaAuditing + AuditorAware
│   └── SchedulerConfig.java          # @EnableScheduling
└── modules/                          # Business modules (Modular Monolith)
    ├── auth/                         # Login, Register, JWT, Token management
    ├── user/                         # User, Role, Permission, UserProfile, SecuritySettings
    ├── organization/                 # Department management
    ├── wallet/                       # Wallet, Transaction (Core Ledger)
    ├── project/                      # Project, Phase, ProjectMember, ProjectRole
    ├── expense/                      # ExpenseCategory, PhaseCategoryBudget
    ├── request/                      # Request, RequestHistory, RequestAttachment
    ├── accounting/                   # PayrollPeriod, Payslip, SystemFund
    ├── file/                         # FileStorage (Cloudinary)
    ├── notification/                 # Notification (WebSocket persistence)
    ├── audit/                        # AuditLog (System audit trail)
    ├── config/                       # SystemConfig (Dynamic key-value)
    └── mail/                         # EmailPayload, Producer, Worker, MailService
```

---

## 11. CODING CONVENTIONS

| Quy tắc | Chi tiết |
|:---|:---|
| Naming | camelCase (Java), snake_case (DB columns), UPPER_SNAKE (Enum) |
| DTO Pattern | Request DTO (input) + Response DTO (output), map bằng **`@Service` Mapper class** thủ công (KHÔNG dùng MapStruct) |
| DTO Mapper | Đặt trong `modules/{name}/mapper/`, class `{Entity}Mapper`, annotate `@Component @RequiredArgsConstructor` |
| Service Pattern | Interface + Impl (`AuthService` → `AuthServiceImpl`) |
| Repository | Extends `JpaRepository<Entity, ID>`, đặt trong `modules/{name}/repository/` |
| Entity | Đặt trong `modules/{name}/entity/`, `@Builder @NoArgsConstructor @AllArgsConstructor` |
| Enum | Đặt cùng folder entity, `@Enumerated(EnumType.STRING)` |
| Controller | `@RestController @RequestMapping("/api/v1/{module}")` |
| Lombok | `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor` cho Entity |
| Transactions | `@Transactional` trên Service methods, KHÔNG trên Controller |
| Logging | SLF4J via Lombok `@Slf4j` |

---

## 12. DTO & JSON NAMING CONVENTION (BẮT BUỘC)

### 12.1. Tất cả DTO field PHẢI viết theo camelCase
```java
// ✅ ĐÚNG
private String fullName;
private String accessToken;
private Long expiresIn;

// ❌ SAI — KHÔNG dùng @JsonProperty để override sang snake_case
@JsonProperty("access_token")   // CẤM
private String accessToken;

// ❌ SAI — KHÔNG dùng @JsonNaming(SnakeCaseStrategy.class)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)  // CẤM
```

### 12.2. Quy tắc
- Java field name = JSON key name = **camelCase** (mặc định Jackson)
- **KHÔNG** dùng `@JsonProperty("snake_case")` để override tên field
- **KHÔNG** cấu hình `spring.jackson.property-naming-strategy: SNAKE_CASE` trong application.yml
- **KHÔNG** dùng `@JsonNaming(SnakeCaseStrategy.class)` trên class DTO
- Jackson đã cấu hình `default-property-inclusion: non_null` → field null tự động bị loại khỏi JSON

### 12.3. Ví dụ JSON Response chuẩn
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 1800000,
    "user": {
      "id": 1,
      "email": "admin@ifms.vn",
      "fullName": "Phạm Thị Thanh Hà",
      "role": "ADMIN",
      "permissions": ["USER_VIEW_LIST", "ROLE_MANAGE"]
    }
  },
  "timestamp": "2026-03-07T10:15:30"
}
```

---

## 13. API RESPONSE WRAPPER (BẮT BUỘC)

### 13.1. Mọi Controller method PHẢI bọc trong `ApiResponse<T>`
```java
// ✅ ĐÚNG — Trả về thành công
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable Long id) {
    UserResponse user = userService.getUserById(id);
    return ResponseEntity.ok(ApiResponse.success(user));
}

// ✅ ĐÚNG — Trả về thành công với message tùy chỉnh
@PostMapping
public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest req) {
    UserResponse user = userService.createUser(req);
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("User created successfully", user));
}

// ❌ SAI — Không bao giờ trả ApiResponse.error() từ Controller
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<Void>> getUser(@PathVariable Long id) {
    if (user == null) {
        return ResponseEntity.status(404).body(ApiResponse.error("Not found")); // CẤM!
    }
}
```

### 13.2. Cú pháp
| Trường hợp | Cú pháp |
|:---|:---|
| Thành công (data) | `ResponseEntity.ok(ApiResponse.success(data))` |
| Thành công (message + data) | `ResponseEntity.ok(ApiResponse.success("msg", data))` |
| Thành công (201 Created) | `ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Created", data))` |
| Lỗi | **KHÔNG trả trực tiếp** — ném Exception, `GlobalExceptionHandler` tự xử lý |

### 13.3. ApiResponse Structure
```java
public class ApiResponse<T> {
    private boolean success;     // true/false
    private String message;      // Mô tả kết quả
    private T data;              // Payload (null khi error)
    private LocalDateTime timestamp;  // LUÔN CÓ trong mọi response
}
```

---

## 14. EXCEPTION HANDLING (BẮT BUỘC)

### 14.1. Kiến trúc Exception
```
RuntimeException
  └── BaseException (abstract base — có HttpStatus + errorCode)
        ├── ResourceNotFoundException (404)
        ├── BadRequestException (400)
        ├── UnauthorizedException (401)
        └── {Custom}Exception extends BaseException (tạo thêm khi cần)
```

### 14.2. Quy tắc phân tầng
| Tình huống | Exception sử dụng | Ví dụ |
|:---|:---|:---|
| Không tìm thấy entity | `ResourceNotFoundException` | `new ResourceNotFoundException("User", "id", userId)` |
| Vi phạm logic nghiệp vụ | `BadRequestException` hoặc custom extends `BaseException` | `new BadRequestException("Insufficient balance")` |
| Chưa đăng nhập / Token hết hạn | `UnauthorizedException` | `new UnauthorizedException("Token expired")` |
| Validation DTO (`@NotBlank`, `@Min`...) | **Tự động** — `MethodArgumentNotValidException` | Spring tự bắt, trả Map field lỗi |

### 14.3. CẤM LÀM
```java
// ❌ CẤM — Không dùng RuntimeException chung chung
throw new RuntimeException("Something went wrong");

// ❌ CẤM — Không dùng Exception chung chung
throw new Exception("Error");

// ❌ CẤM — Không trả error response từ Controller
return ResponseEntity.badRequest().body(ApiResponse.error("Bad request"));

// ✅ ĐÚNG — Luôn ném Exception cụ thể
throw new ResourceNotFoundException("Wallet", "userId", userId);
throw new BadRequestException("Amount exceeds available balance");
```

### 14.4. GlobalExceptionHandler tự động xử lý
| Exception | HTTP Status | Response |
|:---|:---:|:---|
| `BaseException` (và subclasses) | Từ `ex.getStatus()` | `{ success: false, message: "...", timestamp: "..." }` |
| `MethodArgumentNotValidException` | 400 | `{ success: false, message: "Validation failed", data: { fieldName: "error msg" }, timestamp: "..." }` |
| `AuthenticationException` | 401 | `{ success: false, message: "Authentication failed: ...", timestamp: "..." }` |
| `AccessDeniedException` | 403 | `{ success: false, message: "Access denied", timestamp: "..." }` |
| `Exception` (catch-all) | 500 | `{ success: false, message: "An unexpected error occurred", timestamp: "..." }` |

### 14.5. Tạo Custom Exception khi cần
```java
// Ví dụ: Khi ví hết tiền
public class InsufficientBalanceException extends BaseException {
    public InsufficientBalanceException(String walletType, BigDecimal requested, BigDecimal available) {
        super(
            String.format("%s insufficient: requested %s, available %s", walletType, requested, available),
            HttpStatus.BAD_REQUEST,
            "INSUFFICIENT_BALANCE"
        );
    }
}
```

---

## 15. LOGGING & SECURITY (BẮT BUỘC)

### 15.1. Sử dụng `@Slf4j` (Lombok)
```java
@Slf4j        // BẮT BUỘC trên GlobalExceptionHandler và tất cả Service
@Service
public class WalletServiceImpl implements WalletService {
    // log.info(), log.warn(), log.error() — KHÔNG dùng System.out.println()
}
```

### 15.2. Log Level
| Level | Khi nào dùng |
|:---|:---|
| `log.error("...", ex)` | Exception 500, BaseException, lỗi nghiêm trọng. **Luôn kèm stack trace** |
| `log.warn("...")` | Cảnh báo (PIN sắp bị khóa, balance thấp, retry) |
| `log.info("...")` | Hành động nghiệp vụ thành công (request approved, payout completed) |
| `log.debug("...")` | Chi tiết kỹ thuật (query params, token validation steps) |

### 15.3. CẤM log thông tin nhạy cảm
```java
// ❌ CẤM
log.info("User login: email={}, password={}", email, password);
log.debug("PIN entered: {}", pin);
log.info("Token: {}", jwtToken);

// ✅ ĐÚNG
log.info("User login attempt: email={}", email);
log.debug("PIN validation result: {}", isValid);
log.info("Token issued for userId={}", userId);
```

### 15.4. Structured Logging Best Practices
```java
// ✅ Dùng placeholder {} thay vì String concatenation
log.info("Request {} approved by actor={}, amount={}", requestCode, actorId, amount);

// ❌ Không concatenate string
log.info("Request " + requestCode + " approved by " + actorId);  // CẤM
```

---

## 16. TÍNH TOÁN TÀI CHÍNH (BẮT BUỘC)

Mọi logic liên quan đến tiền tệ, tỷ lệ, và con số tài chính PHẢI tuân thủ:

### 16.1. BẮT BUỘC dùng `BigDecimal`
```java
// ✅ ĐÚNG
BigDecimal amount = new BigDecimal("1000000.50");
BigDecimal zero = BigDecimal.ZERO;
BigDecimal fromLong = BigDecimal.valueOf(5000000L);

// ❌ CẤM — Tuyệt đối KHÔNG dùng double/float cho tiền
double amount = 1000000.50;   // CẤM — mất precision
float rate = 0.1f;            // CẤM — floating point error
```

### 16.2. Khởi tạo an toàn
```java
// ✅ ĐÚNG — String constructor (chính xác tuyệt đối)
BigDecimal price = new BigDecimal("19.99");

// ✅ ĐÚNG — valueOf (an toàn cho giá trị đơn giản)
BigDecimal tax = BigDecimal.valueOf(0.1);

// ❌ CẤM — double constructor (mất precision)
BigDecimal bad = new BigDecimal(0.1);  // = 0.1000000000000000055511151231257827021181... 
```

### 16.3. Phép chia (Division) — BẮT BUỘC chỉ định scale + RoundingMode
```java
// ✅ ĐÚNG
BigDecimal result = a.divide(b, 2, RoundingMode.HALF_UP);

// ❌ CẤM — ArithmeticException nếu kết quả vô hạn
BigDecimal result = a.divide(b);  // CẤM
```

### 16.4. So sánh
```java
// ✅ ĐÚNG — compareTo() so sánh giá trị toán học
if (balance.compareTo(amount) >= 0) { /* đủ tiền */ }
if (balance.compareTo(BigDecimal.ZERO) == 0) { /* bằng 0 */ }

// ❌ CẤM — equals() so sánh cả scale (2.0 ≠ 2.00)
if (balance.equals(amount)) { /* CẤM — sai kết quả */ }
```

### 16.5. Database Mapping
```java
@Column(precision = 19, scale = 2)  // DECIMAL(19,2) — đủ cho 99 triệu tỷ VND
private BigDecimal amount;
```

### 16.6. Constants
```java
// Hằng số tiền tệ dùng trong logic
private static final BigDecimal MIN_WITHDRAWAL = new BigDecimal("10000");
private static final int MONEY_SCALE = 2;
private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;
```

---

## 17. MAPPING & DTO — MANUAL MAPPER (BẮT BUỘC)

### 17.1. Cấu trúc Mapper Class
```java
@Component
@RequiredArgsConstructor
public class UserMapper {

    /**
     * Entity → Response DTO (cho API output)
     */
    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .status(user.getStatus().name())
                .departmentName(user.getDepartment().getName())
                .build();
    }

    /**
     * Request DTO → Entity (cho Create)
     * Chỉ copy dữ liệu — KHÔNG có logic nghiệp vụ
     */
    public User toEntity(CreateUserRequest dto) {
        return User.builder()
                .email(dto.getEmail())
                .fullName(dto.getFullName())
                .build();
    }
}
```

### 17.2. Quy tắc
| Quy tắc | Chi tiết |
|:---|:---|
| Annotation | `@Component @RequiredArgsConstructor` |
| Vị trí | `modules/{name}/mapper/{Entity}Mapper.java` |
| `toResponse()` | Entity → Response DTO |
| `toEntity()` | Request DTO → Entity (Create) |
| `updateEntity()` | Request DTO + Entity hiện tại → Entity đã cập nhật (Update) |
| **CẤM logic trong Mapper** | Mapper CHỈ copy data. Tính toán, validation, fetch FK → đặt ở **Service** |
| **CẤM gọi Repository** | Mapper KHÔNG inject Repository. Nếu cần resolve FK (VD: `departmentId` → `Department`), Service phải fetch trước rồi truyền vào |

### 17.3. Inject Mapper vào Service
```java
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final UserMapper userMapper;  // Inject mapper
    
    public UserResponse getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return userMapper.toResponse(user);
    }
}
```

---




















