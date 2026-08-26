# Funcionários Implementation Plan

> **For agentic workers:** Execute this plan inline in the primary session. The task explicitly forbids subagents and parallel delegation. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Persist and expose the administrative employee list/detail flow and integrate the Angular views with it.

**Architecture:** Reuse `Usuario` for authentication and `Funcionario` for employee profile data. Add paginated fetch-join repository queries, a `FuncionarioService` that maps explicit DTOs and enforces professional compatibility, and an ADMIN-only controller. Replace Angular's temporary employee store with a small HTTP service and remote page state.

**Tech Stack:** Java 21, Spring Boot, Spring Data JPA, Spring Security/JWT, Hibernate Envers, Liquibase, H2, Angular, RxJS and HttpClient.

**Spec:** `fen/docs/superpowers/specs/2026-08-26-funcionarios-design.md`

## Global Constraints

- Reuse `usuario` and `funcionario`; do not add duplicate e-mail, role, CPF or professional tables.
- All identifiers are UUIDs; all persistent audit fields remain backend-owned.
- All administrative endpoints require `hasRole('ADMIN')`.
- The list response is Spring `Page`, zero-based, and accepts sizes 10, 20, 50 and 100 from Angular.
- Do not impose a global single-responsible-technician rule.
- Preserve the existing Angular confirmation dialog and do not update displayed data until the backend replies successfully.

---

### Task 1: Characterize and implement backend employee querying

**Files:**
- Modify: `fen/src/main/java/org/fen/fen/repository/FuncionarioRepository.java`
- Create: `fen/src/main/java/org/fen/fen/domain/dto/FuncionarioSummaryResponse.java`
- Create: `fen/src/main/java/org/fen/fen/domain/dto/FuncionarioDetailResponse.java`
- Create: `fen/src/main/java/org/fen/fen/domain/dto/ResponsavelTecnicoRequest.java`
- Create: `fen/src/main/java/org/fen/fen/mapper/FuncionarioMapper.java`
- Test: `fen/src/test/java/org/fen/fen/repository/FuncionarioRepositoryTest.java`

**Interfaces:**
- Produces `Page<Funcionario> buscar(String query, Pageable pageable)` with `Usuario` and supervisor available.
- Produces summary and detail DTOs containing UUID, nome, e-mail, CPF, nascimento, role, situação and role-specific data.

- [ ] Write repository tests proving a query matches name/e-mail/role, pages results, and materializes `Usuario` and a supervisor.
- [ ] Run `./mvnw -Dtest=FuncionarioRepositoryTest test` and confirm the new test fails because `buscar` is absent.
- [ ] Add the fetch-aware paginated query and explicit DTO mapper.
- [ ] Run `./mvnw -Dtest=FuncionarioRepositoryTest test` and confirm it passes.

### Task 2: Characterize and implement employee application rules

**Files:**
- Create: `fen/src/main/java/org/fen/fen/service/FuncionarioService.java`
- Test: `fen/src/test/java/org/fen/fen/funcionario/FuncionarioServiceTest.java`

**Interfaces:**
- Produces `Page<FuncionarioSummaryResponse> listar(String query, Pageable pageable)`.
- Produces `FuncionarioDetailResponse buscar(UUID id)`.
- Produces `FuncionarioDetailResponse alterarResponsavelTecnico(UUID id, boolean responsavelTecnico)`.

- [ ] Write unit tests that prove listing/detail mapping, not-found handling, pharmacist marking/unmarking, rejection of intern changes, and no cross-employee state change.
- [ ] Run `./mvnw -Dtest=FuncionarioServiceTest test` and confirm the test fails because `FuncionarioService` is absent.
- [ ] Implement only the repository orchestration, mappings and professional-role validation required by the tests, marking the transaction for the update method.
- [ ] Run `./mvnw -Dtest=FuncionarioServiceTest test` and confirm it passes.

### Task 3: Expose the protected HTTP resource

**Files:**
- Create: `fen/src/main/java/org/fen/fen/controller/FuncionarioController.java`
- Test: `fen/src/test/java/org/fen/fen/funcionario/FuncionarioControllerTest.java`
- Test: `fen/src/test/java/org/fen/fen/funcionario/FuncionarioResourceITTest.java`

**Interfaces:**
- `GET /api/admin/funcionarios?query=&page=0&size=10` returns a page of summaries.
- `GET /api/admin/funcionarios/{id}` returns a detail DTO.
- `PATCH /api/admin/funcionarios/{id}/responsavel-tecnico` accepts `{"responsavelTecnico": true}` and returns the updated detail DTO.

- [ ] Write MVC tests for JSON/page serialization, 404 and invalid update errors, plus the controller-level ADMIN authorization declaration.
- [ ] Run `./mvnw -Dtest=FuncionarioControllerTest test` and confirm the test fails because the endpoint is absent.
- [ ] Add the controller with `@PreAuthorize("hasRole('ADMIN')")` and validation.
- [ ] Write and run `FuncionarioResourceITTest` proving ADMIN can list/update/re-read and FARMACEUTICO, ESTAGIARIO and anonymous requests receive 403/403/401.
- [ ] Run the backend targeted suite and confirm it passes.

### Task 4: Replace the Angular temporary employee data source

**Files:**
- Create: `fen-frontend/src/app/domain/funcionario.service.ts`
- Create: `fen-frontend/src/app/domain/funcionario.service.spec.ts`
- Modify: `fen-frontend/src/app/pages/admin-funcionarios-page/admin-funcionarios-page.ts`
- Modify: `fen-frontend/src/app/pages/admin-funcionarios-page/admin-funcionarios-page.html`
- Modify: `fen-frontend/src/app/pages/visualizar-funcionario-page/visualizar-funcionario-page.ts`
- Modify: `fen-frontend/src/app/pages/visualizar-funcionario-page/visualizar-funcionario-page.html`
- Delete: `fen-frontend/src/app/domain/temporary-pharmacy-employee-store.ts`
- Delete: `fen-frontend/src/app/domain/temporary-pharmacy-employee-store.spec.ts`
- Modify: `fen-frontend/src/app/app.spec.ts`

**Interfaces:**
- `FuncionarioService.list(query, page, size): Observable<ApiPage<FuncionarioSummary>>`.
- `FuncionarioService.get(id): Observable<FuncionarioDetail>`.
- `FuncionarioService.alterarResponsavelTecnico(id, responsavelTecnico): Observable<FuncionarioDetail>`.

- [ ] Write HTTP service tests that assert the three endpoint contracts and the single boolean update payload.
- [ ] Run `npm test -- --watch=false` with the focused service spec and confirm it fails because the service is absent.
- [ ] Implement the HTTP service and discriminated view models using UUID strings.
- [ ] Write component tests for remote initial load, query/page reload, detail load, failed load feedback, and only-after-success technical-responsibility replacement.
- [ ] Replace the store injections and local computed pagination with remote response state; retain the existing dialog and visual classes.
- [ ] Remove the temporary store after all imports and tests use `FuncionarioService`.
- [ ] Run the focused Angular tests and confirm they pass.

### Task 5: Verify the integrated change

**Files:**
- Modify: `fen/docs/superpowers/specs/2026-08-26-funcionarios-design.md` only if implementation exposes a documented incompatibility.

- [ ] Run `./mvnw test` in `fen`.
- [ ] Run `npm test -- --watch=false` and `npm run build` in `fen-frontend`.
- [ ] Inspect `git diff --check` and each repository status.
- [ ] Confirm all requested functional requirements against the spec, documenting any pre-existing incompatibility without expanding scope.
