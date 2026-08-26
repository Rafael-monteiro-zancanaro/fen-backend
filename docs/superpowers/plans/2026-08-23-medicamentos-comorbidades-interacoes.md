# Medicamentos, Comorbidades e Interações Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Persistir e integrar medicamentos, comorbidades e interações, substituindo a fonte local Angular por uma API Spring Boot autenticada.

**Architecture:** O backend usa entidades `Medicamento`, `Comorbidade` e `Interacao`, DTOs nas fronteiras e sincronização transacional das interações pelo agregado Comorbidade. O Angular mantém seus modelos públicos em inglês, mas passa a obter páginas, detalhes, autocomplete e interações por HTTP.

**Tech Stack:** Java 25, Spring Boot 4.1, Spring Data JPA, Spring Security/JWT, Hibernate Envers, Liquibase YAML, PostgreSQL, H2, JUnit 6, Mockito, Angular 22, RxJS 7, Vitest.

**Spec:** `docs/superpowers/specs/2026-08-23-medicamentos-comorbidades-interacoes-design.md`

## Global Constraints

- Não usar subagents.
- IDs persistentes são UUID e nunca são gerados no Angular.
- Tabelas e colunas de domínio usam português, minúsculas e sem `_`; somente tabelas Envers usam `_aud`.
- Requests/responses preservam `name`, `measurementUnit`, `administrationRoute` e `medicationInteractionIds` consumidos pelo Angular.
- Não expor entidades JPA nem adicionar regras clínicas, estoque ou cascades destrutivos.
- Todos os endpoints exigem JWT e aceitam `ADMIN`, `FARMACEUTICO` e `ESTAGIARIO`.
- Cada alteração de comportamento segue RED, verificação da falha, GREEN e verificação da passagem.
- Preservar alterações locais não relacionadas e não executar commits sem conferir o índice.

---

### Task 1: Schema definitivo e entidades auditadas

**Files:**
- Replace: `src/main/java/org/fen/fen/domain/Medication.java`
- Replace: `src/main/java/org/fen/fen/domain/Comorbity.java`
- Create: `src/main/java/org/fen/fen/domain/Medicamento.java`
- Create: `src/main/java/org/fen/fen/domain/Comorbidade.java`
- Create: `src/main/java/org/fen/fen/domain/Interacao.java`
- Replace: `src/main/resources/db/changelog/changes/004-create-medication-and-comorbity.yaml`
- Modify: `src/test/java/org/fen/fen/domain/EntidadesBasicasAuditoriaTest.java`
- Create: `src/test/java/org/fen/fen/repository/MedicamentoComorbidadeEnversTest.java`

**Interfaces:**
- Produces: `Medicamento extends AuditableEntity`, `Comorbidade extends AuditableEntity`, `Interacao extends AuditableEntity`.
- Produces: `Comorbidade#adicionarInteracao(Medicamento)`, `removerInteracao(UUID)`, `getInteracoes()`.

- [ ] **Step 1: Escrever testes de mapeamento e Envers que exijam UUID, auditoria e nomes definitivos**

```java
@Test
void deveAuditarCriacaoERemocaoDeInteracao() {
    Medicamento medicamento = medicamentoRepository.saveAndFlush(novoMedicamento("Dipirona"));
    Comorbidade comorbidade = comorbidadeRepository.saveAndFlush(new Comorbidade("Hipertensão"));
    Interacao interacao = interacaoRepository.saveAndFlush(new Interacao(comorbidade, medicamento));
    UUID id = interacao.getId();
    interacaoRepository.delete(interacao);
    interacaoRepository.flush();
    assertThat(auditReader.getRevisions(Interacao.class, id)).hasSize(2);
}
```

- [ ] **Step 2: Executar o teste e confirmar falha por ausência das entidades/tabelas**

Run: `./gradlew test --tests '*EntidadesBasicasAuditoriaTest' --tests '*MedicamentoComorbidadeEnversTest'`

Expected: FAIL porque `Medicamento`, `Comorbidade`, `Interacao` ou `aud.interacao_aud` ainda não existem.

- [ ] **Step 3: Implementar entidades e reescrever o changeset 004**

```java
@Entity
@Audited
@Table(name = "interacao", uniqueConstraints =
        @UniqueConstraint(name = "uk_interacao_comorbidade_medicamento",
                columnNames = {"comorbidadeid", "medicamentoid"}))
public class Interacao extends AuditableEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comorbidadeid", nullable = false)
    private Comorbidade comorbidade;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicamentoid", nullable = false)
    private Medicamento medicamento;
}
```

O YAML deve criar `medicamento`, `comorbidade`, `interacao` e as três tabelas `_aud`, com PKs, FKs para domínio/revisão, índices por `lower(nome)` quando compatível ou por `nome`, e a constraint única do par.

- [ ] **Step 4: Executar os testes de domínio/Envers e validar o schema H2**

Run: `./gradlew test --tests '*EntidadesBasicasAuditoriaTest' --tests '*MedicamentoComorbidadeEnversTest'`

Expected: PASS, incluindo duas revisões da interação criada e removida.

- [ ] **Step 5: Conferir diff e registrar checkpoint sem incluir arquivos alheios**

Run: `git diff --check && git status --short`

### Task 2: Repositories, datasets, paginação e autocomplete

**Files:**
- Replace: `src/main/java/org/fen/fen/repository/MedicationRepository.java`
- Replace: `src/main/java/org/fen/fen/repository/ComorbityRepository.java`
- Create: `src/main/java/org/fen/fen/repository/MedicamentoRepository.java`
- Create: `src/main/java/org/fen/fen/repository/ComorbidadeRepository.java`
- Create: `src/main/java/org/fen/fen/repository/InteracaoRepository.java`
- Replace: `src/test/java/org/fen/fen/repository/MedicationRepositoryTest.java`
- Replace: `src/test/java/org/fen/fen/repository/ComorbityRepositoryTest.java`
- Create: `src/test/java/org/fen/fen/repository/MedicamentoRepositoryTest.java`
- Create: `src/test/java/org/fen/fen/repository/ComorbidadeRepositoryTest.java`
- Create: `src/test/java/org/fen/fen/repository/InteracaoRepositoryTest.java`
- Replace: `src/test/resources/datasets/medication-repository.sql`
- Replace: `src/test/resources/datasets/comorbity-repository.sql`
- Replace: `src/test/resources/datasets/interactions.sql`

**Interfaces:**
- Produces: `Page<Medicamento> buscar(String query, Pageable pageable)`.
- Produces: `List<Medicamento> autocomplete(String query, Pageable limit)`.
- Produces: `Optional<Comorbidade> buscarDetalhe(UUID id)` com interações e medicamentos.
- Produces: `List<Interacao> buscarPares(Set<UUID> medicamentoIds, Set<UUID> comorbidadeIds)`.

- [ ] **Step 1: Escrever testes de queries e constraint com fixtures literais**

```java
@Test
void autocompleteFiltraSemAcentoIgnorandoCaixaELimitaResultado() {
    List<Medicamento> result = repository.autocomplete("dip", PageRequest.of(0, 8));
    assertThat(result).extracting(Medicamento::getNome).containsExactly("Dipirona");
}

@Test
void rejeitaInteracaoDuplicadaNoBanco() {
    repository.saveAndFlush(new Interacao(comorbidade, medicamento));
    assertThatThrownBy(() -> repository.saveAndFlush(new Interacao(comorbidade, medicamento)))
            .isInstanceOf(DataIntegrityViolationException.class);
}
```

- [ ] **Step 2: Executar repository tests e confirmar falhas nas queries ausentes**

Run: `./gradlew test --tests '*MedicamentoRepositoryTest' --tests '*ComorbidadeRepositoryTest' --tests '*InteracaoRepositoryTest'`

- [ ] **Step 3: Implementar repositories focados e datasets determinísticos**

```java
@Query("""
    select m from Medicamento m
    where :query = '' or lower(m.nome) like lower(concat('%', :query, '%'))
    order by lower(m.nome), m.id
    """)
Page<Medicamento> buscar(@Param("query") String query, Pageable pageable);
```

Usar `@EntityGraph(attributePaths = {"interacoes", "interacoes.medicamento"})` somente na consulta detalhada, não na página.

- [ ] **Step 4: Executar repository tests e a suíte de repositories**

Run: `./gradlew test --tests 'org.fen.fen.repository.*'`

Expected: PASS sem N+1 na consulta detalhada inspecionada pelo teste/query única.

### Task 3: DTOs e mapeadores

**Files:**
- Create: `src/main/java/org/fen/fen/medicamento/dto/MedicamentoRequest.java`
- Create: `src/main/java/org/fen/fen/medicamento/dto/MedicamentoResponse.java`
- Create: `src/main/java/org/fen/fen/medicamento/dto/MedicamentoSummaryResponse.java`
- Create: `src/main/java/org/fen/fen/comorbidade/dto/ComorbidadeRequest.java`
- Create: `src/main/java/org/fen/fen/comorbidade/dto/ComorbidadeResponse.java`
- Create: `src/main/java/org/fen/fen/comorbidade/dto/ComorbidadeSummaryResponse.java`
- Create: `src/main/java/org/fen/fen/interacao/dto/InteracaoResponse.java`
- Create: `src/main/java/org/fen/fen/mapper/MedicamentoMapper.java`
- Create: `src/main/java/org/fen/fen/mapper/ComorbidadeMapper.java`
- Create: `src/test/java/org/fen/fen/mapper/MedicamentoMapperTest.java`
- Create: `src/test/java/org/fen/fen/mapper/ComorbidadeMapperTest.java`

**Interfaces:**
- Produces: JSON fields exactly `name`, `measurementUnit`, `administrationRoute`, `medicationInteractionIds`, `interactionMedications`.

- [ ] **Step 1: Escrever testes literais de mapeamento**

```java
@Test
void respostaDetalhadaIncluiIdsESummariesSemExporEntidades() {
    ComorbidadeResponse response = mapper.toResponse(comorbidade);
    assertThat(response.medicationInteractionIds()).containsExactly(medicamentoId);
    assertThat(response.interactionMedications().getFirst().name()).isEqualTo("Dipirona");
}
```

- [ ] **Step 2: Executar e confirmar falha por DTOs/mappers ausentes**

Run: `./gradlew test --tests '*MapperTest'`

- [ ] **Step 3: Implementar records validados e mapeadores explícitos**

```java
public record ComorbidadeRequest(
        @NotBlank @Size(max = 254) String name,
        @NotNull List<@NotNull UUID> medicationInteractionIds
) {}
```

- [ ] **Step 4: Executar mapper tests**

Run: `./gradlew test --tests '*MapperTest'`

### Task 4: MedicamentoService com regras e conflitos

**Files:**
- Replace: `src/main/java/org/fen/fen/service/MedicationService.java`
- Create: `src/main/java/org/fen/fen/service/MedicamentoService.java`
- Replace: `src/test/java/org/fen/fen/service/MedicationServiceTest.java`
- Create: `src/test/java/org/fen/fen/service/MedicamentoServiceTest.java`
- Modify: `src/main/java/org/fen/fen/error/ApiExceptionHandler.java`

**Interfaces:**
- Produces: `criar`, `buscarPorId`, `listar`, `atualizar`, `excluir`, `autocomplete` usando DTOs.

- [ ] **Step 1: Escrever testes para criação, edição, inexistência, paginação, autocomplete e exclusão impedida**

```java
@Test
void exclusaoReferenciadaRetornaConflitoDeDominio() {
    doThrow(new DataIntegrityViolationException("fk_interacao_medicamento"))
            .when(repository).deleteById(medicamentoId);
    assertThatThrownBy(() -> service.excluir(medicamentoId))
            .isInstanceOf(ConflictException.class)
            .hasMessage("Medicamento possui referências e não pode ser excluído");
}
```

- [ ] **Step 2: Executar e confirmar falhas pelo service especializado ausente**

Run: `./gradlew test --tests '*MedicamentoServiceTest'`

- [ ] **Step 3: Implementar service transacional e mensagem específica de integridade**

```java
@Transactional(readOnly = true)
public Page<MedicamentoResponse> listar(String query, Pageable pageable) {
    return repository.buscar(normalizar(query), pageable).map(mapper::toResponse);
}
```

- [ ] **Step 4: Executar service tests**

Run: `./gradlew test --tests '*MedicamentoServiceTest'`

### Task 5: ComorbidadeService e sincronização incremental

**Files:**
- Replace: `src/main/java/org/fen/fen/service/ComorbityService.java`
- Replace: `src/main/java/org/fen/fen/service/InteracaoService.java`
- Create: `src/main/java/org/fen/fen/service/ComorbidadeService.java`
- Create: `src/main/java/org/fen/fen/service/InteracaoConsultaService.java`
- Replace: `src/test/java/org/fen/fen/service/ComorbityServiceTest.java`
- Create: `src/test/java/org/fen/fen/service/ComorbidadeServiceTest.java`
- Create: `src/test/java/org/fen/fen/service/InteracaoConsultaServiceTest.java`

**Interfaces:**
- Produces: sincronização que mantém IDs existentes e rejeita IDs repetidos/inexistentes.
- Produces: consulta em lote de pares por conjuntos de UUID.

- [ ] **Step 1: Escrever os nove cenários obrigatórios de comorbidade**

Cobrir separadamente: criar sem interação, com uma, com várias, editar nome, adicionar, remover, manter identidade, medicamento inexistente, IDs duplicados e exclusão.

```java
@Test
void mantemIdDaInteracaoQuePermaneceSelecionada() {
    UUID interactionId = existente.getId();
    service.atualizar(comorbidadeId, new ComorbidadeRequest("Hipertensão", List.of(medicamentoA)));
    assertThat(interacaoRepository.findByComorbidadeId(comorbidadeId))
            .extracting(Interacao::getId).containsExactly(interactionId);
}
```

- [ ] **Step 2: Executar e confirmar falhas da sincronização ausente**

Run: `./gradlew test --tests '*ComorbidadeServiceTest' --tests '*InteracaoConsultaServiceTest'`

- [ ] **Step 3: Implementar sincronização por diferença dentro de `@Transactional`**

```java
Set<UUID> desejados = validarIdsUnicos(request.medicationInteractionIds());
Map<UUID, Interacao> atuais = comorbidade.interacoesPorMedicamentoId();
atuais.values().stream()
        .filter(interacao -> !desejados.contains(interacao.getMedicamento().getId()))
        .toList().forEach(comorbidade::removerInteracao);
desejados.stream().filter(id -> !atuais.containsKey(id))
        .map(medicamentos::get).forEach(comorbidade::adicionarInteracao);
```

- [ ] **Step 4: Executar tests e confirmar identidade/auditoria preservadas**

Run: `./gradlew test --tests '*ComorbidadeServiceTest' --tests '*InteracaoConsultaServiceTest' --tests '*MedicamentoComorbidadeEnversTest'`

### Task 6: Controllers REST, validação e autenticação

**Files:**
- Replace: `src/main/java/org/fen/fen/controller/MedicationController.java`
- Replace: `src/main/java/org/fen/fen/controller/ComorbityController.java`
- Create: `src/main/java/org/fen/fen/controller/MedicamentoController.java`
- Create: `src/main/java/org/fen/fen/controller/ComorbidadeController.java`
- Create: `src/main/java/org/fen/fen/controller/InteracaoConsultaController.java`
- Replace: `src/test/java/org/fen/fen/controller/MedicationControllerTest.java`
- Replace: `src/test/java/org/fen/fen/controller/ComorbityControllerTest.java`
- Create: `src/test/java/org/fen/fen/controller/MedicamentoControllerTest.java`
- Create: `src/test/java/org/fen/fen/controller/ComorbidadeControllerTest.java`
- Create: `src/test/java/org/fen/fen/controller/InteracaoConsultaControllerTest.java`

**Interfaces:**
- Produces: endpoints descritos na especificação e `@Valid` nos corpos.

- [ ] **Step 1: Escrever MockMvc tests para contratos válidos, inválidos, paginação, filtros e erros**

```java
mockMvc.perform(post("/api/medicamentos")
        .contentType(APPLICATION_JSON)
        .content("""{"name":"","measurementUnit":"mg","administrationRoute":"Oral"}"""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors.name").exists());
```

- [ ] **Step 2: Executar e confirmar falhas nos endpoints definitivos ausentes**

Run: `./gradlew test --tests '*MedicamentoControllerTest' --tests '*ComorbidadeControllerTest' --tests '*InteracaoConsultaControllerTest'`

- [ ] **Step 3: Implementar controllers finos e limites de `size`/`limit`**

```java
@GetMapping("/autocomplete")
List<MedicamentoSummaryResponse> autocomplete(
        @RequestParam String query,
        @RequestParam(defaultValue = "8") @Min(1) @Max(20) int limit) {
    return service.autocomplete(query, limit);
}
```

- [ ] **Step 4: Executar controller tests**

Run: `./gradlew test --tests 'org.fen.fen.controller.*'`

### Task 7: ResourceITTests e fluxo autenticado completo

**Files:**
- Create: `src/test/java/org/fen/fen/medicamento/MedicamentoResourceITTest.java`
- Create: `src/test/java/org/fen/fen/comorbidade/ComorbidadeResourceITTest.java`

**Interfaces:**
- Consumes: JWT real, migrations, controllers, services e repositories.

- [ ] **Step 1: Escrever fluxo integrado autenticado e teste de 401**

```java
@Test
void cadastraDoisMedicamentosSincronizaInteracoesERemoveSomenteUma() throws Exception {
    String token = loginAdmin();
    UUID a = criarMedicamento(token, "Dipirona");
    UUID b = criarMedicamento(token, "Ibuprofeno");
    UUID c = criarComorbidade(token, "Hipertensão", List.of(a, b));
    consultarEEsperarMedicamentos(token, c, List.of(a, b));
    atualizarComorbidade(token, c, List.of(a));
    consultarEEsperarMedicamentos(token, c, List.of(a));
}
```

- [ ] **Step 2: Executar e confirmar falha antes dos ajustes finais de integração**

Run: `./gradlew test --tests '*MedicamentoResourceITTest' --tests '*ComorbidadeResourceITTest'`

- [ ] **Step 3: Corrigir somente lacunas reveladas pelo fluxo real**

Não criar endpoint de escrita para `Interacao`; manter autenticação global e auditoria pelo principal autenticado.

- [ ] **Step 4: Executar ResourceITTests e suíte backend completa**

Run: `./gradlew test`

Expected: PASS sem mensagens de credenciais/tokens em logs.

### Task 8: Models, paginação HTTP e MedicationService Angular

**Files:**
- Modify: `../fen-frontend/src/app/domain/clinical-records.ts`
- Create: `../fen-frontend/src/app/domain/api-page.ts`
- Create: `../fen-frontend/src/app/domain/medication.service.ts`
- Create: `../fen-frontend/src/app/domain/medication.service.spec.ts`

**Interfaces:**
- Produces: `MedicationService.list`, `get`, `create`, `update`, `delete`, `autocomplete` retornando Observables.

- [ ] **Step 1: Escrever HttpTestingController tests com URLs e bodies literais**

```typescript
service.list({ query: 'dipi', page: 0, size: 10 }).subscribe((page) => result = page);
const request = http.expectOne(
  `${environment.apiUrl}/api/medicamentos?query=dipi&page=0&size=10`,
);
expect(request.request.method).toBe('GET');
request.flush({ content: [medication], number: 0, size: 10, totalElements: 1, totalPages: 1 });
```

- [ ] **Step 2: Executar e confirmar falha por service ausente**

Run: `yarn test --run src/app/domain/medication.service.spec.ts`

- [ ] **Step 3: Implementar service HTTP usando `HttpParams` e tipos UUID como string**

```typescript
autocomplete(query: string, limit = 8): Observable<Medication[]> {
  const params = new HttpParams().set('query', query.trim()).set('limit', limit);
  return this.http.get<Medication[]>(`${this.resourceUrl}/autocomplete`, { params });
}
```

- [ ] **Step 4: Executar os testes do service**

Run: `yarn test --run src/app/domain/medication.service.spec.ts`

### Task 9: Telas de medicamento e autocomplete assíncrono

**Files:**
- Modify: `../fen-frontend/src/app/components/medication-autocomplete/medication-autocomplete.ts`
- Modify: `../fen-frontend/src/app/components/medication-autocomplete/medication-autocomplete.html`
- Create: `../fen-frontend/src/app/components/medication-autocomplete/medication-autocomplete.spec.ts`
- Modify: `../fen-frontend/src/app/pages/medicamentos-page/medicamentos-page.ts`
- Modify: `../fen-frontend/src/app/pages/medicamentos-page/medicamentos-page.html`
- Create: `../fen-frontend/src/app/pages/medicamentos-page/medicamentos-page.spec.ts`
- Modify: `../fen-frontend/src/app/pages/novo-medicamento-page/novo-medicamento-page.ts`
- Modify: `../fen-frontend/src/app/pages/novo-medicamento-page/novo-medicamento-page.html`
- Create: `../fen-frontend/src/app/pages/novo-medicamento-page/novo-medicamento-page.spec.ts`
- Modify: `../fen-frontend/src/app/pages/visualizar-medicamento-page/visualizar-medicamento-page.ts`
- Modify: `../fen-frontend/src/app/pages/visualizar-medicamento-page/visualizar-medicamento-page.html`
- Create: `../fen-frontend/src/app/pages/visualizar-medicamento-page/visualizar-medicamento-page.spec.ts`

**Interfaces:**
- Consumes: `MedicationService` e `ApiPage<Medication>`.
- Preserves: `valueChange` e `medicationSelected` do autocomplete.

- [ ] **Step 1: Escrever testes de debounce/cancelamento, paginação e double submit**

```typescript
it('cancela a busca anterior após debounce', fakeAsync(() => {
  component.updateValue('di'); tick(300);
  component.updateValue('dipi'); tick(300);
  expect(service.autocomplete).toHaveBeenCalledTimes(2);
  firstRequest.next([oldMedication]);
  expect(component.results()).not.toContain(oldMedication);
}));
```

- [ ] **Step 2: Executar e confirmar falhas porque páginas ainda usam store síncrono**

Run: `yarn test --run src/app/components/medication-autocomplete/medication-autocomplete.spec.ts src/app/pages/medicamentos-page/medicamentos-page.spec.ts src/app/pages/novo-medicamento-page/novo-medicamento-page.spec.ts src/app/pages/visualizar-medicamento-page/visualizar-medicamento-page.spec.ts`

- [ ] **Step 3: Implementar signals de loading/error e pipelines `debounceTime` + `distinctUntilChanged` + `switchMap`**

```typescript
this.searchTerms.pipe(
  map((term) => term.trim()),
  debounceTime(300),
  distinctUntilChanged(),
  switchMap((term) => term ? this.medications.autocomplete(term) : of([])),
  takeUntilDestroyed(),
).subscribe({ next: (items) => this.results.set(items), error: () => this.error.set(true) });
```

- [ ] **Step 4: Executar testes das telas de medicamento**

Run: `yarn test --run src/app/components/medication-autocomplete/medication-autocomplete.spec.ts src/app/pages/medicamentos-page/medicamentos-page.spec.ts src/app/pages/novo-medicamento-page/novo-medicamento-page.spec.ts src/app/pages/visualizar-medicamento-page/visualizar-medicamento-page.spec.ts`

### Task 10: ComorbidityService e telas de comorbidade

**Files:**
- Create: `../fen-frontend/src/app/domain/comorbidity.service.ts`
- Create: `../fen-frontend/src/app/domain/comorbidity.service.spec.ts`
- Modify: `../fen-frontend/src/app/pages/comorbidades-page/comorbidades-page.ts`
- Modify: `../fen-frontend/src/app/pages/comorbidades-page/comorbidades-page.html`
- Create: `../fen-frontend/src/app/pages/comorbidades-page/comorbidades-page.spec.ts`
- Modify: `../fen-frontend/src/app/pages/nova-comorbidade-page/nova-comorbidade-page.ts`
- Modify: `../fen-frontend/src/app/pages/nova-comorbidade-page/nova-comorbidade-page.html`
- Create: `../fen-frontend/src/app/pages/nova-comorbidade-page/nova-comorbidade-page.spec.ts`
- Modify: `../fen-frontend/src/app/pages/visualizar-comorbidade-page/visualizar-comorbidade-page.ts`
- Modify: `../fen-frontend/src/app/pages/visualizar-comorbidade-page/visualizar-comorbidade-page.html`
- Create: `../fen-frontend/src/app/pages/visualizar-comorbidade-page/visualizar-comorbidade-page.spec.ts`

**Interfaces:**
- Produces: `Comorbidity` com `interactionMedications: Medication[]` no detalhe.
- Consumes: request contendo apenas `medicationInteractionIds`.

- [ ] **Step 1: Escrever testes HTTP e de edição com medicamentos pré-selecionados**

```typescript
expect(service.update(id, input)).toIssue('PUT', `/api/comorbidades/${id}`, {
  name: 'Hipertensão', medicationInteractionIds: [medicationA.id],
});
```

O teste de página deve carregar o detalhe, mostrar A e B, remover B e verificar que o PUT envia somente o UUID de A.

- [ ] **Step 2: Executar e confirmar falhas porque o store local ainda é fonte de verdade**

Run: `yarn test --run src/app/domain/comorbidity.service.spec.ts src/app/pages/comorbidades-page/comorbidades-page.spec.ts src/app/pages/nova-comorbidade-page/nova-comorbidade-page.spec.ts src/app/pages/visualizar-comorbidade-page/visualizar-comorbidade-page.spec.ts`

- [ ] **Step 3: Implementar service/páginas assíncronas e usar summaries do detalhe**

Não buscar cada medicamento por ID; preencher `selectedMedications` diretamente de `interactionMedications` e manter um `Set<string>` para impedir duplicação visual.

- [ ] **Step 4: Executar testes das telas de comorbidade**

Run: `yarn test --run src/app/domain/comorbidity.service.spec.ts src/app/pages/comorbidades-page/comorbidades-page.spec.ts src/app/pages/nova-comorbidade-page/nova-comorbidade-page.spec.ts src/app/pages/visualizar-comorbidade-page/visualizar-comorbidade-page.spec.ts`

### Task 11: Consulta em lote e compatibilidade com serviços farmacêuticos

**Files:**
- Create: `../fen-frontend/src/app/domain/interaction.service.ts`
- Create: `../fen-frontend/src/app/domain/interaction.service.spec.ts`
- Modify: `../fen-frontend/src/app/domain/temporary-pharmaceutical-service-store.ts`
- Modify: `../fen-frontend/src/app/domain/temporary-pharmaceutical-service-store.spec.ts`
- Modify: `../fen-frontend/src/app/pages/servicos-farmaceuticos-page/servicos-farmaceuticos-page.ts`
- Modify: `../fen-frontend/src/app/pages/servicos-farmaceuticos-page/servicos-farmaceuticos-page.html`

**Interfaces:**
- Produces: `InteractionService.findPairs(medicationIds, comorbidityIds)` em uma única requisição.
- Preserves: warning informativo e não bloqueante.

- [ ] **Step 1: Escrever teste que exige uma única chamada HTTP para múltiplos pares**

```typescript
service.findPairs([medA, medB], [comA, comB]).subscribe();
const request = http.expectOne((req) => req.url.endsWith('/api/interacoes'));
expect(request.request.params.get('medicamentoIds')).toBe(`${medA},${medB}`);
expect(request.request.params.get('comorbidadeIds')).toBe(`${comA},${comB}`);
```

- [ ] **Step 2: Executar e confirmar falha por consulta local atual**

Run: `yarn test --run src/app/domain/interaction.service.spec.ts src/app/domain/temporary-pharmaceutical-service-store.spec.ts`

- [ ] **Step 3: Implementar consulta em lote e adaptar warning sem persistir atendimento**

Manter pacientes/atendimentos temporários; remover somente a dependência das coleções locais de medicamentos/comorbidades para calcular interações.

- [ ] **Step 4: Executar testes de interação e serviços farmacêuticos**

Run: `yarn test --run src/app/domain/interaction.service.spec.ts src/app/domain/temporary-pharmaceutical-service-store.spec.ts`

### Task 12: Remoção da fonte local e verificação final

**Files:**
- Delete: `../fen-frontend/src/app/domain/temporary-clinical-records-store.ts`
- Delete: `../fen-frontend/src/app/domain/temporary-clinical-records-store.spec.ts`
- Modify: `../fen-frontend/src/app/pages/novo-paciente-page/novo-paciente-page.ts`
- Modify: `../fen-frontend/src/app/pages/novo-paciente-page/novo-paciente-page.html`
- Modify: `../fen-frontend/src/app/app.spec.ts`
- Modify: `../fen-frontend/src/app/domain/temporary-pharmaceutical-service-store.spec.ts`

**Interfaces:**
- Produces: nenhuma geração/localStorage para IDs, medicamentos, comorbidades ou interações.

- [ ] **Step 1: Escrever/ajustar teste que demonstra ausência de persistência local desses domínios**

O comportamento observável será a inexistência de consumidores do store e a persistência real exercitada pelos testes dos services/páginas; não criar teste que apenas procure texto em arquivos.

- [ ] **Step 2: Confirmar todos os consumidores migrados antes da remoção**

Run: `rg -n 'TemporaryClinicalRecordsStore|fen-temporary-clinical-records|createId\(' ../fen-frontend/src/app`

Expected: nenhum consumidor de medicamentos/comorbidades; se pacientes ainda dependerem do arquivo, extrair apenas essas responsabilidades antes de remover as coleções integradas.

- [ ] **Step 3: Remover arrays, geração de IDs e localStorage substituídos**

Preservar `TemporaryPharmaceuticalServiceStore` e demais responsabilidades de pacientes/atendimentos explicitamente fora do escopo.

- [ ] **Step 4: Executar formatação e suítes completas**

Run backend: `./gradlew test`

Run frontend: `yarn test --run`

Run frontend build: `npm run build`

- [ ] **Step 5: Verificar migrations, diffs e ausência de resíduos**

Run:

```bash
git -C ../fen diff --check
git -C ../fen-frontend diff --check
rg -n 'Medication|Comorbity|comorbitymedication|TemporaryClinicalRecordsStore' src/main ../fen-frontend/src/app
```

Expected: nomes provisórios ausentes do backend definitivo; referências frontend restantes justificadas somente pelos tipos públicos ou módulos temporários fora do escopo.

- [ ] **Step 6: Preparar entrega resumida**

Relatar entidades, relações JPA, migrations, endpoints, paginação, autocomplete, testes, arquivos Angular, estruturas locais removidas e responsabilidades ainda temporárias, incluindo qualquer incompatibilidade efetivamente encontrada durante a execução.
