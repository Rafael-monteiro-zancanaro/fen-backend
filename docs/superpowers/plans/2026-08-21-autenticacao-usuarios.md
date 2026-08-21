# Authentication and Users Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver public employee registration with administrative approval, real JWT authentication, role authorization, and an Angular session integrated end to end.

**Architecture:** The backend is organized by feature around `usuario` and `security`; `Usuario` owns credentials/status while `Funcionario` owns professional data. Spring Security is stateless and issues eight-hour JWTs. Angular centralizes session state in `AuthService`, adds tokens through one interceptor, and uses real authentication/role guards.

**Tech Stack:** Java 25, Spring Boot 4.1, Spring Security, Spring Data JPA, Hibernate Envers, Liquibase YAML, PostgreSQL/H2, JJWT 0.13, Angular 22, Reactive Forms, functional interceptors/guards, Vitest.

**Spec:** `docs/superpowers/specs/2026-08-21-autenticacao-usuarios-design.md`

## Global Constraints

- Backend tables and columns use lowercase names without word-separating underscores; only Envers table suffix `_aud` uses an underscore.
- Domain identifiers are UUIDs.
- Audited entities contain `createdAt`, `updatedAt`, `createdBy`, and `updatedBy` and use schema `aud`.
- Public registration accepts only `FARMACEUTICO` and `ESTAGIARIO`; only `ATIVO` users authenticate.
- Passwords use BCrypt and are never logged, returned, or persisted in plaintext.
- JWT lifetime defaults to exactly 8 hours (28,800 seconds); secret, lifetime, CORS origins, and Angular API URL are configurable.
- Rejecting a pending registration deletes `Funcionario` and `Usuario` and frees their unique identifiers.
- Recovery password, refresh tokens, and complete employee administration remain out of scope.
- Use strict TDD: add one behavioral test, run it and observe the intended failure, add minimal production code, rerun, then refactor.
- Preserve unrelated user changes in both repositories.

---

## File structure

Backend feature files live under `fen/src/main/java/org/fen/fen/usuario` and security infrastructure under `fen/src/main/java/org/fen/fen/security`. Shared HTTP errors live under `fen/src/main/java/org/fen/fen/error`. This replaces the incomplete global DTO/JWT stubs without reorganizing unrelated base classes.

Frontend authentication files live under `fen-frontend/src/app/auth`; registration-review pages retain the existing page-folder convention. `TemporaryPharmacyEmployeeStore` stays in place because its employee-management screens are outside this integration.

### Task 1: Establish a green backend baseline and repair shared auditing

**Files:**
- Modify: `fen/build.gradle`
- Modify: `fen/src/main/java/org/fen/fen/FenApplication.java`
- Modify: `fen/src/main/java/org/fen/fen/infra/AuditableEntity.java`
- Modify: `fen/src/main/java/org/fen/fen/infra/AuditorAwareImpl.java`
- Create: `fen/src/main/java/org/fen/fen/config/AuditingConfig.java`
- Replace test: `fen/src/test/java/org/fen/fen/domain/EntidadesBasicasAuditoriaTest.java`
- Create test: `fen/src/test/java/org/fen/fen/infra/AuditorAwareImplTest.java`

**Interfaces:**
- Produces: `AuditableEntity#getCreatedAt()`, `getUpdatedAt()`, `getCreatedBy()`, `getUpdatedBy()` and an `AuditorAware<String>` bean.

- [ ] **Step 1: Run the existing backend suite and record baseline failures**

Run: `cd fen && ./gradlew test`

Expected: current compilation/tests may fail because `RegisterResponseDTO` references a missing `Usuario` and `JwtFilterTest` constructs obsolete infrastructure. Record these as baseline failures; do not claim a green baseline.

- [ ] **Step 2: Write failing auditing tests**

Test authenticated and anonymous auditor behavior, and use reflection assertions that `AuditableEntity` exposes exactly the four documented properties. A representative assertion is:

```java
assertThat(new AuditorAwareImpl().getCurrentAuditor()).contains("Sistema");
assertThat(AuditableEntity.class).hasDeclaredFields(
    "createdAt", "updatedAt", "createdBy", "updatedBy");
```

- [ ] **Step 3: Run the focused tests and verify RED**

Run: `cd fen && ./gradlew test --tests '*AuditorAwareImplTest' --tests '*EntidadesBasicasAuditoriaTest'`

Expected: FAIL because the base class still exposes `lastUpdatedAt/lastUpdatedBy` and auditing is not registered as a Spring bean.

- [ ] **Step 4: Implement the shared audit contract and Envers dependency**

Add `implementation 'org.hibernate.orm:hibernate-envers'`, rename mapped fields/columns to `updatedAt/updatedat` and `updatedBy/updatedby`, make the base class abstract, and register auditing:

```java
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
class AuditingConfig {
    @Bean AuditorAware<String> auditorAware() { return new AuditorAwareImpl(); }
}
```

Ensure `AuditorAwareImpl` ignores unauthenticated/anonymous authentication and otherwise returns `Authentication#getName()`.

- [ ] **Step 5: Verify GREEN and commit**

Run: `cd fen && ./gradlew test --tests '*AuditorAwareImplTest' --tests '*EntidadesBasicasAuditoriaTest'`

Expected: PASS.

Commit in `fen`: `git add build.gradle src && git commit -m "refactor: align shared auditing infrastructure"`

### Task 2: Add user/employee schema, entities, enums, and repositories

**Files:**
- Create: `fen/src/main/resources/db/changelog/changes/001-create-usuario-funcionario.yaml`
- Create: `fen/src/main/java/org/fen/fen/usuario/Role.java`
- Create: `fen/src/main/java/org/fen/fen/usuario/SituacaoUsuario.java`
- Create: `fen/src/main/java/org/fen/fen/usuario/TipoEstagio.java`
- Create: `fen/src/main/java/org/fen/fen/usuario/Usuario.java`
- Create: `fen/src/main/java/org/fen/fen/usuario/Funcionario.java`
- Create: `fen/src/main/java/org/fen/fen/usuario/UsuarioRepository.java`
- Create: `fen/src/main/java/org/fen/fen/usuario/FuncionarioRepository.java`
- Create: `fen/src/test/resources/datasets/usuario-repository.sql`
- Create: `fen/src/test/java/org/fen/fen/repository/UsuarioRepositoryTest.java`
- Create: `fen/src/test/java/org/fen/fen/repository/FuncionarioRepositoryTest.java`

**Interfaces:**
- Produces: `Optional<Usuario> findByEmailIgnoreCase(String email)`, `boolean existsByEmailIgnoreCase(String email)`, `List<Usuario> findAllBySituacao(SituacaoUsuario situacao)`, `Optional<Funcionario> findByUsuarioId(UUID usuarioId)`, and eligible-supervisor projection/query.

- [ ] **Step 1: Write repository tests before entities exist**

Cover saving UUID entities, case-insensitive e-mail lookup, database rejection of duplicate normalized e-mail/CPF, 1:1 user association, and an eligible-supervisor query that excludes pending users and interns. Use deterministic UUIDs in the dataset.

```java
assertThat(usuarioRepository.findByEmailIgnoreCase("FARMA@FEN.BR"))
    .get().extracting(Usuario::getRole).isEqualTo(Role.FARMACEUTICO);
assertThat(funcionarioRepository.findSupervisoresAtivos())
    .extracting(Funcionario::getNome).containsExactly("Farmacêutica Ativa");
```

- [ ] **Step 2: Run repository tests and verify RED**

Run: `cd fen && ./gradlew test --tests '*UsuarioRepositoryTest' --tests '*FuncionarioRepositoryTest'`

Expected: FAIL to compile because domain/repository types do not exist.

- [ ] **Step 3: Add Liquibase schema and domain model**

Create `usuario`, `funcionario`, `aud.revinfo`, `aud.usuario_aud`, and `aud.funcionario_aud`. Use the Liquibase `uuid` type, supported by PostgreSQL and H2 PostgreSQL mode. Persist enums with `@Enumerated(EnumType.STRING)`. Annotate both entities `@Audited`. Model `Funcionario.usuario` as non-null unique 1:1 and `Funcionario.supervisor` as nullable lazy many-to-one without cascade remove.

The repository contract must include:

```java
interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    Optional<Usuario> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<Usuario> findAllBySituacaoOrderByCreatedAtAsc(SituacaoUsuario situacao);
}
```

- [ ] **Step 4: Run repository tests and migration validation**

Run: `cd fen && ./gradlew test --tests '*UsuarioRepositoryTest' --tests '*FuncionarioRepositoryTest'`

Expected: PASS with Liquibase running on H2 PostgreSQL mode and Hibernate schema validation enabled by `BaseRepositoryTest`.

- [ ] **Step 5: Commit**

Commit in `fen`: `git add src && git commit -m "feat: persist users and employees"`

### Task 3: Implement public registration and supervisor lookup

**Files:**
- Create: `fen/src/main/java/org/fen/fen/usuario/dto/UsuarioRegisterRequest.java`
- Create: `fen/src/main/java/org/fen/fen/usuario/dto/UsuarioRegisterResponse.java`
- Create: `fen/src/main/java/org/fen/fen/usuario/dto/SupervisorResponse.java`
- Create: `fen/src/main/java/org/fen/fen/usuario/UsuarioMapper.java`
- Create: `fen/src/main/java/org/fen/fen/usuario/UsuarioService.java`
- Create: `fen/src/main/java/org/fen/fen/usuario/UsuarioController.java`
- Create: `fen/src/main/java/org/fen/fen/error/ApiError.java`
- Create: `fen/src/main/java/org/fen/fen/error/ApiExceptionHandler.java`
- Create: `fen/src/main/java/org/fen/fen/error/ConflictException.java`
- Create: `fen/src/main/java/org/fen/fen/error/BusinessRuleException.java`
- Create test: `fen/src/test/java/org/fen/fen/usuario/UsuarioServiceTest.java`
- Create test: `fen/src/test/java/org/fen/fen/usuario/UsuarioControllerTest.java`

**Interfaces:**
- Consumes: repositories and `PasswordEncoder` from Tasks 1–2.
- Produces: `UsuarioRegisterResponse register(UsuarioRegisterRequest)`, `List<SupervisorResponse> findSupervisoresAtivos()`; `POST /api/auth/register`; `GET /api/public/supervisores`.

- [ ] **Step 1: Write failing service tests one behavior at a time**

Cover lowercasing/trim of e-mail, BCrypt output matching raw password, default `PENDENTE`, rejection of `ADMIN`, duplicate email/CPF, pharmacist-required CRF, intern-required supervisor/vigency, supervisor eligibility, and transactional creation. Do not assert calls as the outcome; capture the saved real entities and assert their state.

```java
assertThat(savedUsuario.getEmail()).isEqualTo("nova@fen.br");
assertThat(savedUsuario.getPasswordHash()).isNotEqualTo("segredo123");
assertThat(passwordEncoder.matches("segredo123", savedUsuario.getPasswordHash())).isTrue();
assertThat(savedUsuario.getSituacao()).isEqualTo(SituacaoUsuario.PENDENTE);
```

- [ ] **Step 2: Verify service RED**

Run: `cd fen && ./gradlew test --tests '*UsuarioServiceTest'`

Expected: FAIL because service/DTOs do not exist.

- [ ] **Step 3: Implement minimal registration service and DTO validation**

Use Bean Validation for structural rules and service validation for role-dependent rules. `UsuarioRegisterRequest` contains `nome`, `cpf`, optional `dataNascimento`, `email`, `senha`, `role`, optional `crf`, `responsavelTecnico`, optional `tipoEstagio`, `supervisorId`, `inicioVigencia`, and `fimVigencia`. Never accept `hashSenha`.

- [ ] **Step 4: Verify service GREEN**

Run: `cd fen && ./gradlew test --tests '*UsuarioServiceTest'`

Expected: PASS.

- [ ] **Step 5: Write controller tests and verify RED**

Use MockMvc with validation and exception advice. Assert `201` response omits `senha/passwordHash`, malformed email gets `400` with a field error, duplicate gets `409`, and supervisor list exposes only `id/nome`.

Run: `cd fen && ./gradlew test --tests '*UsuarioControllerTest'`

Expected: FAIL because mappings are absent.

- [ ] **Step 6: Implement controller/error contract, verify, and commit**

Run: `cd fen && ./gradlew test --tests '*UsuarioServiceTest' --tests '*UsuarioControllerTest'`

Expected: PASS.

Commit in `fen`: `git add src && git commit -m "feat: register pending users"`

### Task 4: Implement administrative registration review

**Files:**
- Create: `fen/src/main/java/org/fen/fen/usuario/dto/UsuarioPendenteSummaryResponse.java`
- Create: `fen/src/main/java/org/fen/fen/usuario/dto/UsuarioPendenteDetailResponse.java`
- Create: `fen/src/main/java/org/fen/fen/usuario/AdminUsuarioController.java`
- Modify: `fen/src/main/java/org/fen/fen/usuario/UsuarioService.java`
- Modify test: `fen/src/test/java/org/fen/fen/usuario/UsuarioServiceTest.java`
- Create test: `fen/src/test/java/org/fen/fen/usuario/AdminUsuarioControllerTest.java`

**Interfaces:**
- Produces: `findPendentes()`, `findPendente(UUID)`, `aprovar(UUID)`, `rejeitar(UUID)` and the four `/api/admin/usuarios...` endpoints from the spec.

- [ ] **Step 1: Write failing service tests**

Assert chronological pending list, full role-specific detail, `PENDENTE -> ATIVO`, refusal to approve/reject active users, and deletion order/transaction semantics for rejection.

- [ ] **Step 2: Verify RED, implement service behavior, verify GREEN**

Run before and after implementation: `cd fen && ./gradlew test --tests '*UsuarioServiceTest'`

Expected before: FAIL on missing methods. Expected after: PASS.

- [ ] **Step 3: Write failing controller authorization/contract tests**

Assert list/detail JSON, `404`, approval `200`, rejection `204`, no authentication `401`, FARMACEUTICO `403`, and ADMIN success using Spring Security test users with `roles("ADMIN")`.

- [ ] **Step 4: Implement controller mappings and method security**

Use `@PreAuthorize("hasRole('ADMIN')")` on the administrative controller or equivalent centralized request rules; do not manually compare enums inside controller methods.

- [ ] **Step 5: Verify and commit**

Run: `cd fen && ./gradlew test --tests '*UsuarioServiceTest' --tests '*AdminUsuarioControllerTest'`

Expected: PASS.

Commit in `fen`: `git add src && git commit -m "feat: review pending registrations"`

### Task 5: Replace JWT stubs with real Spring Security authentication

**Files:**
- Delete: `fen/src/main/java/org/fen/fen/infra/JwtUtil.java`
- Delete: `fen/src/main/java/org/fen/fen/infra/JwtFilter.java`
- Delete: `fen/src/test/java/org/fen/fen/infra/JwtFilterTest.java`
- Delete: `fen/src/main/java/org/fen/fen/dto/request/LoginDTO.java`
- Delete: `fen/src/main/java/org/fen/fen/dto/request/RegisterRequestDTO.java`
- Delete: `fen/src/main/java/org/fen/fen/dto/response/RegisterResponseDTO.java`
- Create: `fen/src/main/java/org/fen/fen/security/JwtProperties.java`
- Create: `fen/src/main/java/org/fen/fen/security/JwtService.java`
- Create: `fen/src/main/java/org/fen/fen/security/FenUserDetailsService.java`
- Create: `fen/src/main/java/org/fen/fen/security/JwtAuthenticationFilter.java`
- Create: `fen/src/main/java/org/fen/fen/security/RestAuthenticationEntryPoint.java`
- Create: `fen/src/main/java/org/fen/fen/security/RestAccessDeniedHandler.java`
- Replace: `fen/src/main/java/org/fen/fen/config/SecurityConfig.java`
- Create test: `fen/src/test/java/org/fen/fen/security/JwtServiceTest.java`
- Create test: `fen/src/test/java/org/fen/fen/security/JwtAuthenticationFilterTest.java`
- Create test: `fen/src/test/java/org/fen/fen/security/FenUserDetailsServiceTest.java`

**Interfaces:**
- Produces: `IssuedToken issue(Usuario usuario, Instant now)`, `JwtPrincipal parse(String token)`, `UserDetails loadUserByUsername(String email)` and authenticated authority `ROLE_<role>`.

- [ ] **Step 1: Write JWT tests with an injected Clock**

Use a fixed instant and a test-only 256-bit secret. Assert subject UUID, role claim, exact expiry at `now + 8h`, invalid signature rejection, and expiration rejection without sleeping.

```java
assertThat(result.expiresAt()).isEqualTo(Instant.parse("2026-08-21T20:00:00Z"));
assertThat(jwtService.parse(result.token()).role()).isEqualTo(Role.ADMIN);
```

- [ ] **Step 2: Verify JWT RED, implement, verify GREEN**

Run before/after: `cd fen && ./gradlew test --tests '*JwtServiceTest'`

- [ ] **Step 3: Write and run failing user-details/filter tests**

Assert an active account loads with `ROLE_FARMACEUTICO`, a pending account is disabled, valid bearer authentication populates UUID/e-mail/role, and malformed/expired tokens leave context empty for the entry point to produce `401`.

- [ ] **Step 4: Implement security components and filter chain**

Enable method security, stateless sessions, public matchers only for register/login/supervisors, BCrypt bean, DAO provider/`AuthenticationManager`, explicit CORS, JSON entry point, JSON denied handler, and filter before `UsernamePasswordAuthenticationFilter`.

- [ ] **Step 5: Verify and commit**

Run: `cd fen && ./gradlew test --tests '*JwtServiceTest' --tests '*JwtAuthenticationFilterTest' --tests '*FenUserDetailsServiceTest'`

Expected: PASS.

Commit in `fen`: `git add -A && git commit -m "feat: authenticate requests with jwt"`

### Task 6: Add login, current-user endpoint, configuration, and ADMIN migration

**Files:**
- Create: `fen/src/main/java/org/fen/fen/security/dto/LoginRequest.java`
- Create: `fen/src/main/java/org/fen/fen/security/dto/AuthUserResponse.java`
- Create: `fen/src/main/java/org/fen/fen/security/dto/LoginResponse.java`
- Create: `fen/src/main/java/org/fen/fen/security/AuthenticationService.java`
- Create: `fen/src/main/java/org/fen/fen/security/AuthenticationController.java`
- Create: `fen/src/main/resources/db/changelog/changes/002-seed-admin.yaml`
- Modify: `fen/src/main/resources/application.properties`
- Modify: `fen/src/main/resources/application-test.properties`
- Create test: `fen/src/test/java/org/fen/fen/security/AuthenticationServiceTest.java`
- Create test: `fen/src/test/java/org/fen/fen/security/AuthenticationControllerTest.java`
- Create test: `fen/src/test/java/org/fen/fen/security/AdminSeedResourceITTest.java`

**Interfaces:**
- Produces: `LoginResponse login(LoginRequest)`, `AuthUserResponse currentUser(Authentication)`; `POST /api/auth/login`; `GET /api/auth/me`.

- [ ] **Step 1: Write failing login service/controller tests**

Assert that `AuthenticationManager` performs credential checking, token response contains `token/expiresAt/user`, response never contains password/hash, invalid credentials and pending users return the same `401` body, and `/me` returns the JWT principal.

- [ ] **Step 2: Verify RED, implement login/me, verify GREEN**

Run before/after: `cd fen && ./gradlew test --tests '*AuthenticationServiceTest' --tests '*AuthenticationControllerTest'`

- [ ] **Step 3: Add configuration**

Use properties equivalent to:

```properties
fen.security.jwt.secret=${FEN_JWT_SECRET}
fen.security.jwt.expiration=8h
fen.security.cors.allowed-origins=${FEN_CORS_ALLOWED_ORIGINS:http://localhost:4200}
```

Tests override the secret with a non-production 32-byte value. Do not provide a production fallback secret.

- [ ] **Step 4: Generate and verify the ADMIN BCrypt hash against the migrated record**

Write `AdminSeedResourceITTest` first: start the H2/Liquibase application, load `admin@fen.br` from `UsuarioRepository`, assert it is active ADMIN, assert the stored value differs from `admin123`, and assert `passwordEncoder.matches("admin123", usuario.getPasswordHash())`. Run it and observe failure because the seed is absent. Generate one BCrypt cost-10 hash using the application encoder, paste only the hash into `002-seed-admin.yaml`, then rerun. Use fixed UUID `00000000-0000-0000-0000-000000000001` and audit values `Sistema`.

- [ ] **Step 5: Run authentication tests and commit**

Run: `cd fen && ./gradlew test --tests '*Authentication*' --tests '*AdminSeed*'`

Expected: PASS.

Commit in `fen`: `git add src && git commit -m "feat: expose login and seed administrator"`

### Task 7: Prove the complete backend flow

**Files:**
- Create: `fen/src/test/java/org/fen/fen/security/AuthenticationResourceITTest.java`
- Create: `fen/src/test/resources/application-integrationtest.properties` if isolation from unit-test timing is needed.

**Interfaces:**
- Consumes all backend HTTP contracts.
- Produces an executable proof of register → approve → login → `/me` and authorization boundaries.

- [ ] **Step 1: Write the failing integration scenarios**

With real MockMvc/application context and H2/Liquibase, test:

```text
register FARMACEUTICO -> login is 401 -> ADMIN login -> detail/list -> approve
-> professional login returns JWT -> /api/auth/me is 200
-> FARMACEUTICO calling /api/admin/usuarios/pendentes is 403
-> anonymous calling /api/auth/me is 401
```

Also register/reject and verify the same e-mail can register again.

- [ ] **Step 2: Run and verify RED for any integration gaps**

Run: `cd fen && ./gradlew test --tests '*AuthenticationResourceITTest'`

Expected: at least one failure exposing missing wiring/configuration rather than a test syntax error.

- [ ] **Step 3: Make only the necessary wiring fixes and rerun**

Run: `cd fen && ./gradlew test --tests '*AuthenticationResourceITTest'`

Expected: PASS.

- [ ] **Step 4: Run complete backend verification and commit**

Run: `cd fen && ./gradlew clean test`

Expected: PASS with no compilation failure, no external PostgreSQL dependency, and Liquibase migrations applied.

Commit in `fen`: `git add -A && git commit -m "test: cover authentication flow end to end"`

### Task 8: Add Angular API/session types and AuthService

**Files:**
- Create: `fen-frontend/src/environments/environment.ts`
- Create: `fen-frontend/src/environments/environment.development.ts`
- Modify: `fen-frontend/angular.json`
- Create: `fen-frontend/src/app/auth/auth.models.ts`
- Create: `fen-frontend/src/app/auth/auth-storage.service.ts`
- Create: `fen-frontend/src/app/auth/auth.service.ts`
- Create test: `fen-frontend/src/app/auth/auth-storage.service.spec.ts`
- Create test: `fen-frontend/src/app/auth/auth.service.spec.ts`
- Modify: `fen-frontend/src/app/app.config.ts`

**Interfaces:**
- Produces: `UserRole`, `AuthUser`, `LoginResponse`, `RegisterRequest`, `RegistrationDetail`; `currentUser`, `isAuthenticated`, `login`, `register`, `restoreSession`, `logout`, `token`.

- [ ] **Step 1: Run the current frontend suite/build as baseline**

Run: `cd fen-frontend && yarn test --run` and `yarn build`

Expected: record current results before edits.

- [ ] **Step 2: Write failing storage/service tests**

Use Angular HTTP testing. Assert exact API payloads/URLs, persisted token/user/expiry, valid reload restoration through `/api/auth/me`, expired-token removal without HTTP, failed restoration clearing state, and logout clearing state.

```typescript
expect(service.isAuthenticated()).toBe(false);
service.login({ email: 'admin@fen.br', senha: 'admin123' }).subscribe();
http.expectOne(`${environment.apiUrl}/api/auth/login`)
  .flush({ token: 'jwt', expiresAt: '2026-08-22T01:00:00Z', user: admin });
expect(service.currentUser()).toEqual(admin);
```

- [ ] **Step 3: Verify RED**

Run: `cd fen-frontend && yarn test --run src/app/auth/auth-storage.service.spec.ts src/app/auth/auth.service.spec.ts`

Expected: FAIL because auth infrastructure is absent.

- [ ] **Step 4: Implement types, environments, storage, service, and HttpClient provider**

Use `provideHttpClient(...)` and `provideAppInitializer(() => inject(AuthService).restoreSession())` in `app.config.ts`. Store one serialized session under a single key. Determine expiration from server `expiresAt`; do not trust mere token presence and do not store credentials.

- [ ] **Step 5: Verify GREEN and commit**

Run: `cd fen-frontend && yarn test --run src/app/auth/auth-storage.service.spec.ts src/app/auth/auth.service.spec.ts`

Expected: PASS.

Commit in `fen-frontend`: `git add . && git commit -m "feat: add real authentication session"`

### Task 9: Add Angular interceptor, startup restoration, and guards

**Files:**
- Create: `fen-frontend/src/app/auth/auth.interceptor.ts`
- Create: `fen-frontend/src/app/auth/auth.guard.ts`
- Replace: `fen-frontend/src/app/domain/admin-only.guard.ts`
- Create tests: `fen-frontend/src/app/auth/auth.interceptor.spec.ts`
- Create tests: `fen-frontend/src/app/auth/auth.guard.spec.ts`
- Modify: `fen-frontend/src/app/app.config.ts`
- Modify: `fen-frontend/src/app/app.routes.ts`

**Interfaces:**
- Produces: `authInterceptor`, `authGuard`, `adminOnlyGuard`; all internal routes require authentication and admin routes require `ADMIN`.

- [ ] **Step 1: Write failing interceptor tests**

Assert bearer header on API calls when a token exists, no header on login/register, `401` clears session and navigates to `/login`, and `403` preserves session and navigates to `/inicio`.

- [ ] **Step 2: Write failing guard tests**

Assert unauthenticated users receive a login `UrlTree`, authenticated FARMACEUTICO reaches normal routes, ADMIN reaches admin routes, and FARMACEUTICO receives `/inicio` for admin routes.

- [ ] **Step 3: Verify RED, implement, verify GREEN**

Run before/after: `cd fen-frontend && yarn test --run src/app/auth/auth.interceptor.spec.ts src/app/auth/auth.guard.spec.ts`

- [ ] **Step 4: Apply guards without blocking public routes**

Group or annotate routes so only `/login`, `/cadastro`, and `/recuperar-senha` remain public. Add `/admin/cadastros-pendentes` and `/admin/cadastros-pendentes/:id` as ADMIN routes in later tasks without weakening backend authorization.

- [ ] **Step 5: Commit**

Commit in `fen-frontend`: `git add src && git commit -m "feat: protect routes with jwt session"`

### Task 10: Integrate the login page and application shell

**Files:**
- Modify: `fen-frontend/src/app/pages/login-page/login-page.ts`
- Modify: `fen-frontend/src/app/pages/login-page/login-page.html`
- Create test: `fen-frontend/src/app/pages/login-page/login-page.spec.ts`
- Modify: `fen-frontend/src/app/app.ts`
- Modify: `fen-frontend/src/app/app.html`
- Modify: `fen-frontend/src/app/app.spec.ts`
- Delete: `fen-frontend/src/app/domain/temporary-access-control.ts`
- Delete: `fen-frontend/src/app/domain/temporary-access-control.spec.ts`

**Interfaces:**
- Consumes: `AuthService.login/logout/currentUser`.
- Produces: real credential login, real ADMIN navigation visibility, and logout.

- [ ] **Step 1: Write failing login/shell tests**

Assert submitted e-mail/password, disabled submit while pending, uniform invalid-credentials feedback, navigation to `/inicio` after success, no role selector, ADMIN-only links from real current user, and logout clearing the session.

- [ ] **Step 2: Verify RED**

Run: `cd fen-frontend && yarn test --run src/app/pages/login-page/login-page.spec.ts src/app/app.spec.ts`

- [ ] **Step 3: Implement Reactive Forms login and shell session behavior**

Preserve existing visual classes. Replace the temporary profile selector with validation/error status and wire the existing “Sair” action to `AuthService.logout()`.

- [ ] **Step 4: Verify and commit**

Run: `cd fen-frontend && yarn test --run src/app/pages/login-page/login-page.spec.ts src/app/app.spec.ts`

Expected: PASS.

Commit in `fen-frontend`: `git add -A && git commit -m "feat: integrate login with backend"`

### Task 11: Integrate registration and supervisor selection

**Files:**
- Modify: `fen-frontend/src/app/pages/cadastro-usuario-page/cadastro-usuario-page.ts`
- Modify: `fen-frontend/src/app/pages/cadastro-usuario-page/cadastro-usuario-page.html`
- Create test: `fen-frontend/src/app/pages/cadastro-usuario-page/cadastro-usuario-page.spec.ts`
- Create: `fen-frontend/src/app/auth/registration.service.ts`
- Create test: `fen-frontend/src/app/auth/registration.service.spec.ts`

**Interfaces:**
- Consumes: `/api/auth/register`, `/api/public/supervisores`.
- Produces: role-specific registration payload with supervisor UUID and pending confirmation.

- [ ] **Step 1: Write failing API/page tests**

Assert supervisors load into the intern select, pharmacist payload omits intern-only values, intern payload includes `supervisorId`, mismatched passwords stay client-side, invalid fields display feedback, and successful registration shows pending-approval confirmation without logging in.

- [ ] **Step 2: Verify RED**

Run: `cd fen-frontend && yarn test --run src/app/auth/registration.service.spec.ts src/app/pages/cadastro-usuario-page/cadastro-usuario-page.spec.ts`

- [ ] **Step 3: Implement Reactive Forms registration**

Use conditional validators that are added/removed when role changes. Send `FARMACEUTICO`/`ESTAGIARIO`, never lowercase profile strings or `ADMIN`. Preserve CPF presentation behavior and existing visual system.

- [ ] **Step 4: Verify and commit**

Run: `cd fen-frontend && yarn test --run src/app/auth/registration.service.spec.ts src/app/pages/cadastro-usuario-page/cadastro-usuario-page.spec.ts`

Expected: PASS.

Commit in `fen-frontend`: `git add src && git commit -m "feat: register employees through api"`

### Task 12: Add pending-registration list and detail/decision pages

**Files:**
- Create: `fen-frontend/src/app/auth/admin-registration.service.ts`
- Create test: `fen-frontend/src/app/auth/admin-registration.service.spec.ts`
- Create: `fen-frontend/src/app/pages/cadastros-pendentes-page/cadastros-pendentes-page.ts`
- Create: `fen-frontend/src/app/pages/cadastros-pendentes-page/cadastros-pendentes-page.html`
- Create test: `fen-frontend/src/app/pages/cadastros-pendentes-page/cadastros-pendentes-page.spec.ts`
- Create: `fen-frontend/src/app/pages/visualizar-cadastro-pendente-page/visualizar-cadastro-pendente-page.ts`
- Create: `fen-frontend/src/app/pages/visualizar-cadastro-pendente-page/visualizar-cadastro-pendente-page.html`
- Create test: `fen-frontend/src/app/pages/visualizar-cadastro-pendente-page/visualizar-cadastro-pendente-page.spec.ts`
- Modify: `fen-frontend/src/app/app.routes.ts`
- Modify: `fen-frontend/src/app/app.html`

**Interfaces:**
- Consumes: admin list/detail/approve/reject endpoints.
- Produces: ADMIN list route `/admin/cadastros-pendentes`, detail route `/admin/cadastros-pendentes/:id`, approval and rejection.

- [ ] **Step 1: Write failing admin API tests**

Assert exact list/detail/approve/delete HTTP methods and URLs, including `DELETE /api/admin/usuarios/{id}/rejeitar`.

- [ ] **Step 2: Write failing page tests**

Assert empty/loading/error/list states; link from row/card to detail; role-specific professional fields; supervisor display for interns; approval returning to refreshed list; and rejection requiring an explicit confirmation before DELETE.

- [ ] **Step 3: Verify RED**

Run: `cd fen-frontend && yarn test --run src/app/auth/admin-registration.service.spec.ts src/app/pages/cadastros-pendentes-page/cadastros-pendentes-page.spec.ts src/app/pages/visualizar-cadastro-pendente-page/visualizar-cadastro-pendente-page.spec.ts`

- [ ] **Step 4: Implement services/pages/routes/navigation**

Reuse semantic classes from `app.css` and patterns from the existing password-recovery list/detail pages. Show no credential field. Add an ADMIN-only navigation item “Cadastros pendentes”.

- [ ] **Step 5: Verify and commit**

Run the same focused command; expected PASS.

Commit in `fen-frontend`: `git add src && git commit -m "feat: review pending registrations"`

### Task 13: Cross-project verification and cleanup

**Files:**
- Modify only files implicated by failing verification.
- Update: `fen/docs/superpowers/specs/2026-08-21-autenticacao-usuarios-design.md` only if implementation reveals an approved contract correction.

**Interfaces:**
- Produces two green projects with no duplicate source of authenticated-user truth.

- [ ] **Step 1: Search for obsolete authentication paths**

Run:

```bash
rg -n "TemporaryAccessControl|fen-temporary-role|selectedRole|JwtUtil|hashSenha" fen/src fen-frontend/src
```

Expected: no production references. References in historical documentation are acceptable; temporary employee/password-recovery stores remain.

- [ ] **Step 2: Run complete backend verification**

Run: `cd fen && ./gradlew clean test`

Expected: PASS.

- [ ] **Step 3: Run complete frontend verification**

Run: `cd fen-frontend && yarn test --run && yarn build`

Expected: PASS and production build within configured budgets.

- [ ] **Step 4: Inspect diffs and migration portability**

Run `git diff --check` and `git status --short` in each repository. Confirm the JWT secret is absent from tracked production configuration, ADMIN plaintext password appears only in tests/docs and not migration data, migrations use the required names, and no unrelated files changed.

- [ ] **Step 5: Commit any verification-driven fixes separately**

Backend commit: `test: finalize authentication verification`

Frontend commit: `test: finalize authentication integration`

Do not create empty commits.
