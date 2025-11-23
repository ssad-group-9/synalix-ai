# Synalix AI - GitHub Copilot Instructions

## Project Overview
Synalix AI is a Spring Boot-based large model training platform using Java 25, PostgreSQL, and RabbitMQ. The architecture follows a layered approach with JWT-based authentication and asynchronous audit logging.

## Key Architecture Patterns

### Authentication & Authorization
- **JWT-based stateless authentication** with access/refresh token pairs
- Access tokens expire in 5 minutes (`jwt.access-token-expiration=300000`)
- Get current user ID with: `@AuthenticationPrincipal JwtUserPrincipal principal` parameter, then `var userId = principal.getId();`
- All secured endpoints require `Authorization: Bearer <token>` header
- Use `@PreAuthorize("hasRole('ADMIN')")` for admin-only endpoints

```java
// Controller with authentication
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
        @AuthenticationPrincipal JwtUserPrincipal principal) {
        var userId = principal.getId();
        var user = userService.getUserById(userId);
        return ResponseEntity.ok(convertToResponse(user));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CreateUserResponse> createUser(
        @Valid @RequestBody CreateUserRequest request,
        @AuthenticationPrincipal JwtUserPrincipal principal) {
        var operatorId = principal.getId();
        // Implementation...
    }
}
```

### Data Layer
- **PostgreSQL** with JPA/Hibernate using UUID primary keys (`@UuidGenerator`)
- All entities use `@CreationTimestamp` for created timestamps
- Repository pattern: extend `JpaRepository<Entity, UUID>`
- Example: `UserRepository extends JpaRepository<User, UUID>`

### Service Layer Architecture
- **Business logic in services**, controllers are thin wrappers
- Use `@Transactional` on service methods that modify data
- Audit operations asynchronously via RabbitMQ (`AuditService.logOperation()`)
- Services inject repositories and other services via constructor injection

```java
/**
 * User service for user management operations
 */
@Service
public class UserService {
    
    private final UserRepository userRepository;
    private final AuditService auditService;
    
    @Autowired
    public UserService(UserRepository userRepository, AuditService auditService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
    }
    
    /**
     * Create a new user
     */
    @Transactional
    public User createUser(String username, String password, UUID operatorId) {
        var user = new User();
        user.setUsername(username);
        // Set other properties...
        
        var savedUser = userRepository.save(user);
        
        // Audit the operation
        auditService.logOperation(
            AuditOperationType.USER_CREATED,
            operatorId,
            savedUser.getId().toString(),
            Map.of("username", username)
        );
        
        return savedUser;
    }
}
```

### Async Audit System
- All user operations logged via RabbitMQ to `audit-log-queue`
- Use `AuditService.logOperation(AuditOperationType, UUID userId, String resourceId, Map<String, Object> details)`
- `AuditLogListener` processes messages and persists to database
- Configured queues: audit-log-queue, audit-exchange, routing key: audit.log

### Error Handling
- Custom `ApiException` with `ApiErrorCode` enum for business errors
- `GlobalExceptionHandler` provides consistent error responses
- Use `ApiErrorResponse` for structured error messages
- Validation errors automatically handled by `@Valid` and `@RestControllerAdvice`

## Development Conventions

### Code Style
- **Use `var` for local variables** when type is automatically deducible: `var user = userRepository.findById(id);`
- **Every class and method must have JavaDoc comments** describing purpose and behavior
- **Use Lombok** for boilerplate code: `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`
- **Constructor injection** preferred over field injection with `@Autowired`

### DTO Patterns
- Request DTOs in `dto.{domain}` packages (e.g., `dto.auth.LoginRequest`)
- Response DTOs co-located with requests
- Use Jakarta validation: `@NotBlank`, `@Size`, `@Email`
- Sensitive fields marked with `@ToString.Exclude`

```java
/**
 * Login request DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Password cannot be blank")
    @ToString.Exclude
    private String password;
}

/**
 * Create user request DTO
 */
@Data
@NoArgsConstructor  
@AllArgsConstructor
public class CreateUserRequest {
    
    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 50)
    private String username;
    
    @Email(message = "Invalid email format")
    private String email;
    
    @NotBlank(message = "Nickname cannot be blank")
    @Size(max = 100)
    private String nickname;
}
```

### Entity Patterns
- UUID primary keys with `@UuidGenerator`
- Use `@Table(uniqueConstraints = @UniqueConstraint(columnNames = "username"))` for business constraints
- Password fields excluded from toString: `@ToString.Exclude`

```java
/**
 * User entity class
 */
@Entity
@Table(name = "users", 
       uniqueConstraints = @UniqueConstraint(columnNames = "username"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 50)
    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;

    @NotBlank(message = "Password hash cannot be blank")
    @Column(name = "password_hash", nullable = false)
    @ToString.Exclude
    private String passwordHash;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
```

## Essential Commands

### Environment Configuration
- **Local Testing**: Use `.env.local` for environment variables and run `docker-compose -f docker-compose.dev.yml up`
- **Production/Deployment**: Use `.env` for environment variables and run `docker-compose up`
- **Key Difference**: `docker-compose.dev.yml` doesn't build the application image (runs externally), while `docker-compose.yml` builds and runs the full stack
- **Environment Template**: Use `.env.example` as template for environment variables
- **Important**: When adding new environment variables to `application.properties`, always update `.env.example`

### Environment Setup
- Default admin user created on startup (configured in `application.properties`)
- RabbitMQ Management UI available at http://localhost:15672 (guest/guest)

## Critical Integration Points

### JWT Token Flow
1. Login via `/api/auth/login` returns access + refresh tokens
2. Include access token in `Authorization: Bearer <token>`
3. Refresh expired tokens via `/api/auth/refresh`
4. Access tokens validated by `JwtAuthenticationFilter`

### Audit Trail
- Every user action should call `AuditService.logOperation()`
- Messages sent to RabbitMQ queue for async processing
- `AuditLogListener` persists audit records to database

## AI Assistant Guidelines

### Before Starting Work
- **If user prompt contains ambiguous information, STOP and confirm with user before coding**
- **Don't do extra work** - if user asks for service implementation, don't implement tests unless requested
- **When implementing controllers without API documentation, STOP and confirm with user**
- **Leverage existing libraries** - Before implementing functionality, check if Spring Framework or existing dependencies already provide the needed features. Avoid "reinventing the wheel" by using established solutions
- **Follow established patterns** - Adhere to the project's existing architectural and coding conventions for consistency and maintainability. Examine existing code to understand its pattern.

### Implementation Priorities
1. Always implement proper JWT authentication flow for new endpoints
2. Add audit logging for state-changing operations
3. Follow existing DTO patterns for request/response objects
4. Use existing error handling patterns with `ApiException`

### Key Files to Reference
- `SecurityConfig.java` - Authentication setup
- `JwtAuthenticationFilter.java` - Token validation logic  
- `AuthService.java` - Authentication business logic
- `GlobalExceptionHandler.java` - Error response patterns
- `AuditService.java` - Audit logging patterns

### Error Handling Examples
```java
// Basic business exception
if (userRepository.findByUsername(username).isPresent()) {
    throw new ApiException(ApiErrorCode.USERNAME_EXISTS);
}

// Exception with custom message
if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
    throw new ApiException(ApiErrorCode.INVALID_CURRENT_PASSWORD, 
        "The provided current password does not match");
}

// Exception with additional details
if (user == null) {
    throw new ApiException(ApiErrorCode.USER_NOT_FOUND, 
        Map.of("userId", userId.toString()));
}

// Validation exception (handled automatically)
@PostMapping
public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
    // Validation errors automatically converted to ApiErrorResponse
    // by GlobalExceptionHandler
}
```