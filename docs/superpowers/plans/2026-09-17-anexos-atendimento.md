# Anexos de Atendimento Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Adicionar anexos persistentes e seguros a cada atendimento, com gestão pela visualização Angular e sem inclusão em PDF.

**Architecture:** Conteúdo em filesystem configurável fora do repositório; `AnexoAtendimento` contém apenas metadados auditados e FK. O backend expõe endpoints multipart/list/download/delete protegidos; a listagem calcula contagens em lote. O frontend usa o service existente, sinais e uma seção exclusiva na visualização.

**Tech Stack:** Spring Boot Web MVC, JPA/Envers, Liquibase/H2, Angular 22, RxJS, Vitest.

**Spec:** `fen/docs/superpowers/specs/2026-09-17-anexos-atendimento-design.md`

## Global Constraints

- Não usar subagents ou execução paralela.
- Conteúdo físico nunca é armazenado no banco, no JSON do atendimento ou no source tree.
- `nomeOriginal` é metadado, nunca path; chave UUID é o único identificador físico.
- `fen.attachments.max-file-size=10MB`; downloads usam attachment e nosniff.
- Upload, download, listagem e remoção exigem JWT e validam atendimento+anexo em conjunto.
- PDF continua limitado aos dados clínicos/administrativos existentes.

---

### Task 1: Persistência, configuração e storage de anexos

**Files:**
- Create: `fen/src/main/java/org/fen/fen/domain/AnexoAtendimento.java`
- Create: `fen/src/main/java/org/fen/fen/repository/AnexoAtendimentoRepository.java`
- Create: `fen/src/main/java/org/fen/fen/attachment/AttachmentProperties.java`
- Create: `fen/src/main/java/org/fen/fen/attachment/AttachmentStorage.java`
- Create: `fen/src/main/resources/db/changelog/changes/010-create-anexo-atendimento.yaml`
- Modify: `fen/src/main/resources/application.properties`
- Modify: `fen/src/main/resources/application-test.properties`
- Test: `fen/src/test/java/org/fen/fen/repository/AnexoAtendimentoRepositoryTest.java`

**Produces:** `AnexoAtendimentoRepository.findByIdAndServicoFarmaceuticoId(UUID, UUID)`, `countByServicoFarmaceuticoIdIn(List<UUID>)` e storage com `store`, `load`, `delete` e compensação.

- [ ] **Step 1: Write the failing repository/storage tests**

```java
@Test void contaAnexosPorAtendimentoEmUmaConsultaDeLote() { /* A=2, B=1 */ }
@Test void storageUsaChaveSemPermitirEscaparDaRaiz() { /* ../ é rejeitado */ }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests org.fen.fen.repository.AnexoAtendimentoRepositoryTest`
Expected: FAIL because attachment domain classes do not exist.

- [ ] **Step 3: Add the minimal audited persistence and storage implementation**

```java
@Entity @Audited @Table(name = "anexoatendimento")
class AnexoAtendimento extends AuditableEntity { /* atendimento, nomeOriginal, chaveArmazenamento, contentType, tamanho */ }
```

Create main/audit Liquibase tables, FK/index, external configurable root and staging cleanup on persistence failures.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests org.fen.fen.repository.AnexoAtendimentoRepositoryTest`
Expected: PASS.

### Task 2: Serviço, contrato HTTP e contagem paginada

**Files:**
- Create: `fen/src/main/java/org/fen/fen/servicofarmaceutico/dto/AnexoAtendimentoResponse.java`
- Create: `fen/src/main/java/org/fen/fen/service/AnexoAtendimentoService.java`
- Modify: `fen/src/main/java/org/fen/fen/controller/ServicoFarmaceuticoController.java`
- Modify: `fen/src/main/java/org/fen/fen/service/ServicoFarmaceuticoService.java`
- Modify: `fen/src/main/java/org/fen/fen/servicofarmaceutico/dto/ServicoFarmaceuticoResumoResponse.java`
- Modify: `fen/src/main/java/org/fen/fen/error/ApiExceptionHandler.java`
- Test: `fen/src/test/java/org/fen/fen/servicofarmaceutico/AnexoAtendimentoResourceITTest.java`
- Test: `fen/src/test/java/org/fen/fen/servicofarmaceutico/ServicoFarmaceuticoResourceITTest.java`

**Produces:** endpoints POST/GET/GET/DELETE; `attachmentCount` no resumo; erros multipart compreensíveis.

- [ ] **Step 1: Write failing HTTP tests**

```java
mockMvc.perform(multipart("/api/servicos-farmaceuticos/{id}/anexos", id).file(file))
  .andExpect(status().isCreated()).andExpect(jsonPath("$.nomeOriginal").value("receita.pdf"));
```

Cover lista, download com attachment/nosniff, delete, 401, ID cruzado 404, vazio/excedido e cenário duas inserções/contagem 2.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests org.fen.fen.servicofarmaceutico.AnexoAtendimentoResourceITTest`
Expected: FAIL because attachment endpoints do not exist.

- [ ] **Step 3: Implement minimal endpoint/service behavior**

```java
@PostMapping(path = "/{id}/anexos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
@ResponseStatus(HttpStatus.CREATED)
AnexoAtendimentoResponse anexar(@PathVariable UUID id, @RequestParam("file") MultipartFile file)
```

Use `findByIdAndServicoFarmaceuticoId` for download/delete and aggregate all page IDs once before mapping summaries.

- [ ] **Step 4: Run backend tests to verify green**

Run: `./gradlew test --tests org.fen.fen.servicofarmaceutico.AnexoAtendimentoResourceITTest --tests org.fen.fen.servicofarmaceutico.ServicoFarmaceuticoResourceITTest`
Expected: PASS.

### Task 3: Service Angular, navegação e seção reativa

**Files:**
- Modify: `fen-frontend/src/app/domain/clinical-records.ts`
- Modify: `fen-frontend/src/app/domain/servico-farmaceutico.service.ts`
- Modify: `fen-frontend/src/app/pages/servicos-farmaceuticos-page/servicos-farmaceuticos-page.ts`
- Modify: `fen-frontend/src/app/pages/visualizar-atendimento-page/visualizar-atendimento-page.ts`
- Modify: `fen-frontend/src/app/pages/visualizar-atendimento-page/visualizar-atendimento-page.html`
- Test: `fen-frontend/src/app/domain/servico-farmaceutico.service.spec.ts`
- Test: `fen-frontend/src/app/pages/visualizar-atendimento-page/visualizar-atendimento-page.spec.ts`
- Test: `fen-frontend/src/app/app.spec.ts`

**Produces:** `listAttachments`, `uploadAttachment`, `downloadAttachment`, `deleteAttachment`; navigation by response ID; multiple-file status feedback and confirmation dialog.

- [ ] **Step 1: Write failing frontend tests**

```ts
expect(router.url).toBe(`/atendimentos/${attendance.id}`);
expect(serviceRequest.request.method).toBe('POST');
expect(serviceRequest.request.body instanceof FormData).toBe(true);
```

Cover create/edit/return navigation, empty/list state, individual upload results, delete confirmation/update, and attachment-free print data.

- [ ] **Step 2: Run tests to verify failures**

Run: `yarn test --watch=false`
Expected: FAIL on absent attachment service/UI behavior.

- [ ] **Step 3: Implement service and view behavior**

```ts
request.subscribe({ next: (attendance) => void this.router.navigate(['/atendimentos', attendance.id]) });
```

Use an `<input type="file" multiple>`, signals for attachment state, `btn`, `card`, `item`, `empty`, `alert-dialog` and spinner classes. Do not alter print types/template.

- [ ] **Step 4: Run frontend tests to verify green**

Run: `yarn test --watch=false`
Expected: PASS.

### Task 4: Indicador de listagem e end-to-end verification

**Files:**
- Modify: `fen-frontend/src/app/pages/atendimentos-page/atendimentos-page.ts`
- Modify: `fen-frontend/src/app/pages/atendimentos-page/atendimentos-page.html`
- Modify: `fen-frontend/src/app/pages/atendimentos-page/atendimentos-page.spec.ts`
- Modify: `fen-frontend/src/app/app.spec.ts`

**Produces:** clipe e pluralização somente para `attachmentCount > 0`, sem N+1 HTTP.

- [ ] **Step 1: Write failing indicator tests**

```ts
expect(textFor({ attachmentCount: 1 })).toContain('1 anexo');
expect(textFor({ attachmentCount: 3 })).toContain('3 anexos');
expect(textFor({ attachmentCount: 0 })).not.toContain('anexo');
```

- [ ] **Step 2: Run frontend tests to verify failure**

Run: `yarn test --watch=false`
Expected: FAIL because summaries have no attachment indicator.

- [ ] **Step 3: Add the small list-only indicator and test fixture contract**

Use the existing ng-icons package paperclip icon; only render with positive count.

- [ ] **Step 4: Run full verification**

Run: `./gradlew test` in `fen` and `yarn test --watch=false && yarn build` in `fen-frontend`.
Expected: all commands exit 0; print DTO/template contains no attachment field or rendering.
