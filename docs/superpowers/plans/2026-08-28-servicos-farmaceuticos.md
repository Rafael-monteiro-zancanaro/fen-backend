# Serviços Farmacêuticos e Atendimentos Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Execute inline in the primary session; do not use subagents.

**Goal:** Persistir e integrar atendimentos farmacêuticos, retornos e acompanhamentos, eliminando a fonte local Angular deste módulo.

**Architecture:** `ServicoFarmaceutico` é o agregado auditado que guarda dados de serviços opcionais e itens `MedicamentoAtendimento`; `Acompanhamento` mantém a cadeia e o atendimento atual. Um resolvedor central, baseado em `Clock`, traduz status persistido em status efetivo sem mutar registros durante leitura. Angular usa um único service HTTP e reutiliza o formulário para criação, edição e retorno.

**Tech Stack:** Java 25, Spring Boot 4.1, Spring Data JPA, Spring Security/JWT, Hibernate Envers, Liquibase YAML, PostgreSQL, H2, JUnit 6, Mockito, Angular, RxJS e Vitest.

**Spec:** `docs/superpowers/specs/2026-08-28-servicos-farmaceuticos-design.md`

## Global Constraints

- Não usar subagents, worktrees adicionais ou persistência local para atendimentos.
- Toda entidade nova usa UUID, `AuditableEntity`, Envers e migration Liquibase YAML.
- Tabelas e colunas usam português, minúsculas e sem `_`; apenas tabelas Envers terminam em `_aud`.
- `codigo` vem exclusivamente da sequence do banco; nunca usar `MAX(codigo)`.
- `EXPIRADO` é status efetivo calculado por `StatusEfetivoResolver`; consultas nunca o persistem.
- Nenhum request recebe `createdBy`, `updatedBy`, entidade persistente completa ou código.
- Não adicionar regras clínicas bloqueantes, estoque, exclusão física, snapshot do paciente ou estado de rascunho.
- Preservar a API pública Angular quando possível, tamanhos 10/20/50/100 e PDF client-side.
- Em cada tarefa: RED verificável, GREEN mínimo, teste verde e `git diff --check`; não modificar arquivos alheios.

---

### Task 1: Escrever testes de domínio para status efetivo e sequência

**Files:**
- Create: `src/test/java/org/fen/fen/servicofarmaceutico/StatusEfetivoResolverTest.java`
- Create: `src/test/java/org/fen/fen/servicofarmaceutico/AcompanhamentoDomainTest.java`

**Interfaces:**
- Produces: requisitos observáveis para `StatusEfetivoResolver#resolver(ServicoFarmaceutico)`, com `Clock` injetado.
- Produces: enum `StatusServicoFarmaceutico` com `AGUARDANDO_RETORNO`, `EXPIRADO`, `CONCLUIDO`.

- [ ] **Step 1: Escrever testes RED com relógio fixo**

```java
@Test
void retornoComPrazoAnteriorAoRelogioEhEfetivamenteExpirado() {
    ServicoFarmaceutico atendimento = aguardando(LocalDate.of(2026, 8, 20));
    assertThat(resolver.resolver(atendimento))
            .isEqualTo(StatusServicoFarmaceutico.EXPIRADO);
}

@Test
void concluidoPermaneceConcluidoMesmoComPrazoPassado() {
    ServicoFarmaceutico atendimento = concluido(LocalDate.of(2026, 8, 20));
    assertThat(resolver.resolver(atendimento))
            .isEqualTo(StatusServicoFarmaceutico.CONCLUIDO);
}
```

- [ ] **Step 2: Rodar e confirmar falha pela ausência das classes**

Run: `./gradlew test --tests '*StatusEfetivoResolverTest' --tests '*AcompanhamentoDomainTest'`

Expected: FAIL por classes de domínio ausentes, não por erro de configuração.

- [ ] **Step 3: Criar somente enum e resolvedor mínimo em memória**

```java
public StatusServicoFarmaceutico resolver(ServicoFarmaceutico atendimento) {
    if (atendimento.getStatusPersistido() == StatusServicoFarmaceutico.CONCLUIDO) {
        return StatusServicoFarmaceutico.CONCLUIDO;
    }
    return atendimento.getAcompanhamento().getProximoRetorno().isBefore(LocalDate.now(clock))
            ? StatusServicoFarmaceutico.EXPIRADO
            : StatusServicoFarmaceutico.AGUARDANDO_RETORNO;
}
```

- [ ] **Step 4: Rodar os testes e confirmar GREEN**

Run: `./gradlew test --tests '*StatusEfetivoResolverTest' --tests '*AcompanhamentoDomainTest'`

Expected: PASS com casos de aguardando, expirado, concluído e posição final.

### Task 2: Migration e entidades auditadas

**Files:**
- Create: `src/main/resources/db/changelog/changes/007-create-servico-farmaceutico.yaml`
- Create: `src/main/java/org/fen/fen/domain/StatusServicoFarmaceutico.java`
- Create: `src/main/java/org/fen/fen/domain/TipoServicoMedicamento.java`
- Create: `src/main/java/org/fen/fen/domain/ServicoFarmaceutico.java`
- Create: `src/main/java/org/fen/fen/domain/MedicamentoAtendimento.java`
- Create: `src/main/java/org/fen/fen/domain/Acompanhamento.java`
- Create: `src/main/java/org/fen/fen/domain/CuidadosFarmaceuticos.java`
- Create: `src/main/java/org/fen/fen/domain/AplicacaoInjetavel.java`
- Create: `src/main/java/org/fen/fen/domain/Inaloterapia.java`
- Create: `src/main/java/org/fen/fen/domain/DadosServicosFarmaceuticos.java`
- Create: `src/test/java/org/fen/fen/repository/ServicoFarmaceuticoEnversTest.java`

**Interfaces:**
- Produces: três entidades auditadas e quatro `@Embeddable` opcionais.
- Produces: sequence `servicofarmaceutico_codigo_seq` e constraints `uk_servicofarmaceutico_codigo`, `uk_servicofarmaceutico_acompanhamento_retorno`.

- [ ] **Step 1: Escrever RED de persistência/auditoria**

```java
@Test
void persisteMedicamentoDeAtendimentoEAuditaAsTresEntidades() {
    ServicoFarmaceutico atendimento = repository.saveAndFlush(novoAtendimentoComMedicamento());
    assertThat(atendimento.getId()).isNotNull();
    assertThat(auditReader.getRevisions(ServicoFarmaceutico.class, atendimento.getId())).hasSize(1);
    assertThat(auditReader.getRevisions(MedicamentoAtendimento.class,
            atendimento.getMedicamentosAtendimento().getFirst().getId())).hasSize(1);
}
```

- [ ] **Step 2: Rodar e verificar que a ausência de schema impede o teste**

Run: `./gradlew test --tests '*ServicoFarmaceuticoEnversTest'`

- [ ] **Step 3: Implementar YAML, entidades e relacionamentos mínimos**

```java
@Entity @Audited
@Table(name = "medicamentoatendimento")
public class MedicamentoAtendimento extends AuditableEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servicofarmaceuticoid", nullable = false)
    private ServicoFarmaceutico servicoFarmaceutico;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicamentoid", nullable = false)
    private Medicamento medicamento;
}
```

`Acompanhamento` deve declarar `@Version Long version` para detectar alterações concorrentes além do lock explícito. O YAML deve incluir as tabelas de domínio, FKs, índices de `pacienteid/dataatendimento`, `medicamentoid/lote`, sequência e as três tabelas no schema `aud` com FK para `aud.revinfo`.

- [ ] **Step 4: Rodar teste Envers e boot H2**

Run: `./gradlew test --tests '*ServicoFarmaceuticoEnversTest' --tests '*FenApplicationTests'`

Expected: PASS e Liquibase cria todas as tabelas sem ddl-auto.

### Task 3: Repositories e consultas eficientes

**Files:**
- Create: `src/main/java/org/fen/fen/repository/ServicoFarmaceuticoRepository.java`
- Create: `src/main/java/org/fen/fen/repository/MedicamentoAtendimentoRepository.java`
- Create: `src/main/java/org/fen/fen/repository/AcompanhamentoRepository.java`
- Create: `src/main/java/org/fen/fen/repository/ServicoFarmaceuticoResumoProjection.java`
- Create: `src/test/java/org/fen/fen/repository/ServicoFarmaceuticoRepositoryTest.java`
- Create: `src/test/resources/datasets/servicofarmaceutico-repository.sql`

**Interfaces:**
- Produces: `buscarDetalhe(UUID)`, `listarResumo(...)`, `buscarAvancado(...)`, `historico(UUID)` e `buscarParaRetornoComLock(UUID)`.

- [ ] **Step 1: Escrever RED para filtro avançado no mesmo item**

```java
@Test
void medicamentoELotePrecisamPertencerAoMesmoItem() {
    Page<ServicoFarmaceutico> encontrados = repository.buscarAvancado(
            "", dipironaId, "LOTE-B", null, PageRequest.of(0, 10));
    assertThat(encontrados.getContent()).doesNotContain(atendimentoComDipironaLoteAIIbuprofenoLoteB);
}
```

- [ ] **Step 2: Executar repository test e confirmar falha de método/query**

Run: `./gradlew test --tests '*ServicoFarmaceuticoRepositoryTest'`

- [ ] **Step 3: Implementar queries com fetch controlado e lock**

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select a from Acompanhamento a join fetch a.atendimentoAtual where a.id = :id")
Optional<Acompanhamento> buscarParaRetornoComLock(@Param("id") UUID id);
```

`buscarAvancado` deve usar um único alias `ma` para `medicamentoId` e `lote`; detalhe usa entity graph para paciente, itens/medicamento e acompanhamento; a página usa projection/resumo.

- [ ] **Step 4: Rodar testes de repository**

Run: `./gradlew test --tests '*ServicoFarmaceuticoRepositoryTest' --tests '*ServicoFarmaceuticoEnversTest'`

Expected: PASS para detalhes, paginação, filtros, histórico, lote e relação medicamento.

### Task 4: DTOs e mapeador explícito

**Files:**
- Create: `src/main/java/org/fen/fen/servicofarmaceutico/dto/ServicoFarmaceuticoRequest.java`
- Create: `src/main/java/org/fen/fen/servicofarmaceutico/dto/ServicoFarmaceuticoResponse.java`
- Create: `src/main/java/org/fen/fen/servicofarmaceutico/dto/ServicoFarmaceuticoResumoResponse.java`
- Create: `src/main/java/org/fen/fen/servicofarmaceutico/dto/MedicamentoAtendimentoRequest.java`
- Create: `src/main/java/org/fen/fen/servicofarmaceutico/dto/ContinuacaoResponse.java`
- Create: `src/main/java/org/fen/fen/servicofarmaceutico/dto/AcompanhamentoResponse.java`
- Create: `src/main/java/org/fen/fen/mapper/ServicoFarmaceuticoMapper.java`
- Create: `src/test/java/org/fen/fen/mapper/ServicoFarmaceuticoMapperTest.java`

**Interfaces:**
- Produces: requests somente com UUIDs e dados de passos ativos; responses agrupadas compatíveis com `clinical-records.ts`.

- [ ] **Step 1: Escrever RED de resposta detalhada**

```java
@Test
void detalheIncluiPacienteMedicamentoFormatoEHistoricoSemEntidadesJpa() {
    ServicoFarmaceuticoResponse response = mapper.toResponse(atendimento, status);
    assertThat(response.patient().id()).isEqualTo(pacienteId);
    assertThat(response.injectable().medications().getFirst().medicationId()).isEqualTo(medicamentoId);
    assertThat(response.injectable().medications().getFirst().medicationConcentration())
            .isEqualTo("Dipirona — 500 mg");
}
```

- [ ] **Step 2: Rodar e confirmar ausência de contratos/mapeador**

Run: `./gradlew test --tests '*ServicoFarmaceuticoMapperTest'`

- [ ] **Step 3: Implementar records validados e mapper**

```java
public record MedicamentoAtendimentoRequest(
        UUID id, @NotNull UUID medicationId, @NotBlank String batch,
        @NotNull LocalDate expirationDate, @NotBlank String dosage) {}
```

O request exige medicamentos somente nos passos ativos. `followUp` é aceito apenas em criação normal, não em retorno. Responses carregam `canContinue`, `nextReturnNumber`, `returnCount`, histórico e `editAllowed`.

- [ ] **Step 4: Rodar testes de mapper**

Run: `./gradlew test --tests '*ServicoFarmaceuticoMapperTest'`

### Task 5: Serviço de criação, edição e sincronização

**Files:**
- Create: `src/main/java/org/fen/fen/service/ServicoFarmaceuticoService.java`
- Create: `src/main/java/org/fen/fen/service/StatusEfetivoResolver.java`
- Create: `src/test/java/org/fen/fen/servicofarmaceutico/ServicoFarmaceuticoServiceTest.java`

**Interfaces:**
- Produces: `criar`, `buscar`, `listar`, `atualizar`, `buscarContinuacao`, `criarRetorno` e `encerrar`.

- [ ] **Step 1: Escrever RED para criação simples, paciente existente e passos opcionais**

```java
@Test
void criaSemAcompanhamentoComoConcluidoEComDoisMedicamentos() {
    ServicoFarmaceuticoResponse result = service.criar(requestComPacienteExistenteEDoisItens());
    assertThat(result.status()).isEqualTo(CONCLUIDO);
    assertThat(result.injectable().medications()).hasSize(2);
}

@Test
void rejeitaPacienteNovoQuandoCpfJaExiste() {
    assertThatThrownBy(() -> service.criar(requestNovoComCpfExistente()))
            .isInstanceOf(ConflictException.class);
}
```

- [ ] **Step 2: Rodar e confirmar falhas do service ausente**

Run: `./gradlew test --tests '*ServicoFarmaceuticoServiceTest'`

- [ ] **Step 3: Implementar transação de criação e sincronização incremental**

```java
@Transactional
public ServicoFarmaceuticoResponse atualizar(UUID id, ServicoFarmaceuticoRequest request) {
    ServicoFarmaceutico entity = encontrarDetalhe(id);
    validarPacienteImutavel(entity, request.patientId());
    sincronizarMedicamentos(entity, request);
    preencherPassosAtivos(entity, request);
    return mapper.toResponse(repository.saveAndFlush(entity), statusResolver.resolver(entity));
}
```

`sincronizarMedicamentos` deve localizar por `id`, alterar somente itens presentes, adicionar itens sem id e remover somente itens não enviados. Um update do acompanhamento inicial após retorno existente deve lançar `BusinessRuleException`.

- [ ] **Step 4: Rodar criação/edição e confirmar GREEN**

Run: `./gradlew test --tests '*ServicoFarmaceuticoServiceTest'`

Expected: PASS para adicionar, remover e atualizar medicamento; passo inativo não valida campos; passo ativo inválido retorna erro.

### Task 6: Continuação, expiração e encerramento concorrentes

**Files:**
- Modify: `src/main/java/org/fen/fen/service/ServicoFarmaceuticoService.java`
- Create: `src/test/java/org/fen/fen/servicofarmaceutico/AcompanhamentoServiceTest.java`

**Interfaces:**
- Produces: continuação sem mutação, criação de retorno com lock e fechamento manual.

- [ ] **Step 1: Escrever RED de cadeia e relógio controlado**

```java
@Test
void terceiroRetornoConcluiECriaQuartoRetornoFalha() {
    ServicoFarmaceuticoResponse terceiro = service.criarRetorno(segundoId, requestRetorno());
    assertThat(terceiro.status()).isEqualTo(CONCLUIDO);
    assertThatThrownBy(() -> service.criarRetorno(terceiro.id(), requestRetorno()))
            .isInstanceOf(ConflictException.class);
}

@Test
void retornoExpiradoPodeProsseguirMasSomenteOAtualPodeCriarFilho() { /* Clock fixo */ }
```

- [ ] **Step 2: Rodar e confirmar falha pelo fluxo não implementado**

Run: `./gradlew test --tests '*AcompanhamentoServiceTest'`

- [ ] **Step 3: Implementar revalidação sob lock**

```java
@Transactional
public ServicoFarmaceuticoResponse criarRetorno(UUID anteriorId, ServicoFarmaceuticoRequest request) {
    Acompanhamento acompanhamento = acompanhamentoRepository.buscarParaRetornoComLockDoAtendimento(anteriorId)
            .orElseThrow(() -> new ConflictException("Atendimento não pode prosseguir"));
    validarAtualEPendente(acompanhamento, anteriorId);
    return criarProximoNaMesmaTransacao(acompanhamento, request);
}
```

O método chama a mesma validação de `buscarContinuacao`, aceita status efetivo expirado, conclui o anterior, atualiza atendimento atual/próximo prazo e traduz violação da constraint única em `409`.

- [ ] **Step 4: Rodar todos os testes de acompanhamento**

Run: `./gradlew test --tests '*AcompanhamentoServiceTest' --tests '*StatusEfetivoResolverTest'`

Expected: PASS para um retorno, intermediários, último, expiração, fechamento e tentativa duplicada.

### Task 7: Controller, autorização e integração HTTP

**Files:**
- Create: `src/main/java/org/fen/fen/controller/ServicoFarmaceuticoController.java`
- Create: `src/test/java/org/fen/fen/servicofarmaceutico/ServicoFarmaceuticoControllerTest.java`
- Create: `src/test/java/org/fen/fen/servicofarmaceutico/ServicoFarmaceuticoResourceITTest.java`

**Interfaces:**
- Produces: endpoints definidos na specification com status 201/200/400/401/404/409.

- [ ] **Step 1: Escrever RED MockMvc**

```java
mockMvc.perform(post("/api/servicos-farmaceuticos")
        .header(HttpHeaders.AUTHORIZATION, bearer(token))
        .contentType(MediaType.APPLICATION_JSON).content(payload))
    .andExpect(status().isCreated())
    .andExpect(jsonPath("$.codigo").isNumber())
    .andExpect(jsonPath("$.status").value("CONCLUIDO"));
```

- [ ] **Step 2: Rodar controller/IT e confirmar falha de rota**

Run: `./gradlew test --tests '*ServicoFarmaceuticoControllerTest' --tests '*ServicoFarmaceuticoResourceITTest'`

- [ ] **Step 3: Implementar controller fino**

```java
@PostMapping @ResponseStatus(HttpStatus.CREATED)
public ServicoFarmaceuticoResponse criar(@Valid @RequestBody ServicoFarmaceuticoRequest request) {
    return service.criar(request);
}
```

Usar `@PageableDefault(size = 10)`, delegar filtros ao service e não acessar repository. O IT deve autenticar, criar paciente/medicamento, criar atendimento, listar, detalhar, completar três retornos e verificar quarta tentativa `409`.

- [ ] **Step 4: Rodar testes HTTP**

Run: `./gradlew test --tests '*ServicoFarmaceuticoControllerTest' --tests '*ServicoFarmaceuticoResourceITTest'`

### Task 8: Modelos Angular e service HTTP

**Files:**
- Modify: `../fen-frontend/src/app/domain/clinical-records.ts`
- Create: `../fen-frontend/src/app/domain/servico-farmaceutico.service.ts`
- Create: `../fen-frontend/src/app/domain/servico-farmaceutico.service.spec.ts`

**Interfaces:**
- Produces: `ServicoFarmaceuticoService#create`, `update`, `get`, `list`, `advancedSearch`, `continuation`, `createReturn`, `close`.

- [ ] **Step 1: Escrever RED HttpTestingController**

```typescript
it('lists the requested backend page and status filter', () => {
  service.list('maria', 'AGUARDANDO_RETORNO', 1, 20).subscribe();
  const request = http.expectOne((r) => r.url.endsWith('/api/servicos-farmaceuticos'));
  expect(request.request.params.get('page')).toBe('1');
  expect(request.request.params.get('status')).toBe('AGUARDANDO_RETORNO');
});
```

- [ ] **Step 2: Rodar e confirmar falha do service inexistente**

Run: `yarn test --runInBand src/app/domain/servico-farmaceutico.service.spec.ts`

- [ ] **Step 3: Criar tipos de request/response e client HTTP**

```typescript
continuation(id: string): Observable<ContinuationContext> {
  return this.http.get<ContinuationContext>(`${this.resourceUrl}/${id}/continuacao`);
}
createReturn(id: string, request: CreatePharmaceuticalServiceAttendanceRequest) {
  return this.http.post<PharmaceuticalServiceAttendance>(`${this.resourceUrl}/${id}/retornos`, request);
}
```

Os IDs permanecem `string`; o serviço centraliza conversão de `MedicamentoAtendimento` para as três seções existentes e não gera UUID, código ou status.

- [ ] **Step 4: Rodar teste do service**

Run: `yarn test --runInBand src/app/domain/servico-farmaceutico.service.spec.ts`

### Task 9: Formulário para criar, editar e continuar

**Files:**
- Modify: `../fen-frontend/src/app/pages/servicos-farmaceuticos-page/servicos-farmaceuticos-page.ts`
- Modify: `../fen-frontend/src/app/pages/servicos-farmaceuticos-page/servicos-farmaceuticos-page.html`
- Modify: `../fen-frontend/src/app/app.routes.ts`
- Create: `../fen-frontend/src/app/pages/servicos-farmaceuticos-page/servicos-farmaceuticos-page.spec.ts`

**Interfaces:**
- Produces: rotas `atendimentos/novo`, `atendimentos/:id/editar` e `atendimentos/:id/continuar` com submit HTTP único.

- [ ] **Step 1: Escrever RED dos fluxos de formulário**

```typescript
it('uses validated continuation context and only creates return on submit', () => {
  continuationSubject.next(context);
  fixture.componentInstance.submit();
  expect(service.createReturn).toHaveBeenCalledWith('previous-id', expect.anything());
  expect(service.continuation).toHaveBeenCalledWith('previous-id');
});
```

- [ ] **Step 2: Executar teste e confirmar falha pela store ainda ser usada**

Run: `yarn test --runInBand src/app/pages/servicos-farmaceuticos-page/servicos-farmaceuticos-page.spec.ts`

- [ ] **Step 3: Substituir dependência da store por service e carregar modos**

```typescript
if (mode === 'edit') this.service.get(id).subscribe((attendance) => this.fillForm(attendance));
if (mode === 'continuation') this.service.continuation(id).subscribe((context) => this.fillContinuation(context));
```

Manter validações visuais e warnings. Desabilitar submit até concluir request, exibir erro de API e liberar em `finalize`. Ocultar acompanhamento em retorno; preencher paciente do contexto; não copiar os demais passos.

- [ ] **Step 4: Rodar testes do formulário**

Run: `yarn test --runInBand src/app/pages/servicos-farmaceuticos-page/servicos-farmaceuticos-page.spec.ts`

### Task 10: Listagem, detalhe, busca e impressão reais

**Files:**
- Modify: `../fen-frontend/src/app/pages/atendimentos-page/atendimentos-page.ts`
- Modify: `../fen-frontend/src/app/pages/atendimentos-page/atendimentos-page.html`
- Modify: `../fen-frontend/src/app/pages/visualizar-atendimento-page/visualizar-atendimento-page.ts`
- Modify: `../fen-frontend/src/app/pages/busca-avancada-atendimentos-page/busca-avancada-atendimentos-page.ts`
- Modify: `../fen-frontend/src/app/domain/atendimento-print-data.ts`
- Create: `../fen-frontend/src/app/pages/atendimentos-page/atendimentos-page.spec.ts`
- Create: `../fen-frontend/src/app/pages/visualizar-atendimento-page/visualizar-atendimento-page.spec.ts`

**Interfaces:**
- Produces: paginação/filtros server-side, ações compatíveis com contexto e impressão do detalhe HTTP.

- [ ] **Step 1: Escrever RED de paginação e ação de continuidade**

```typescript
it('requests page zero after changing status and shows server-provided continuation label', () => {
  component.updateStatusFilter('EXPIRADO');
  expect(service.list).toHaveBeenCalledWith('', 'EXPIRADO', 0, 10);
});
```

- [ ] **Step 2: Rodar e confirmar que a tela ainda pagina a store**

Run: `yarn test --runInBand src/app/pages/atendimentos-page/atendimentos-page.spec.ts`

- [ ] **Step 3: Implementar carregamento reativo e ações HTTP**

```typescript
this.service.list(this.searchTerm(), this.statusFilter(), this.currentPage() - 1, this.pageSize())
  .pipe(finalize(() => this.isLoading.set(false)))
  .subscribe({ next: (page) => this.page.set(page), error: () => this.errorMessage.set(MESSAGE) });
```

A lista usa `canContinue` e `nextReturnNumber` recebidos; a ação chama `continuation` antes de navegar; encerramento chama `close`; detalhe faz `get`; busca avançada chama `advancedSearch`; PDF recebe o detalhe retornado.

- [ ] **Step 4: Rodar testes das três páginas e build**

Run: `yarn test --runInBand src/app/pages/atendimentos-page/atendimentos-page.spec.ts src/app/pages/visualizar-atendimento-page/visualizar-atendimento-page.spec.ts && yarn build`

### Task 11: Remover fonte local substituída e ajustar testes

**Files:**
- Delete: `../fen-frontend/src/app/domain/temporary-pharmaceutical-service-store.ts`
- Delete: `../fen-frontend/src/app/domain/temporary-pharmaceutical-service-store.spec.ts`
- Modify: `../fen-frontend/src/app/domain/atendimento-print-data.spec.ts`
- Modify: imports que ainda apontem para a store removida.

**Interfaces:**
- Produces: uma única fonte de verdade HTTP para atendimento.

- [ ] **Step 1: Escrever/ajustar RED da impressão para o response real**

```typescript
it('uses backend attendance code for the PDF filename', () => {
  expect(buildAtendimentoPrintData(backendAttendance).fileName).toBe('atendimento-1042.pdf');
});
```

- [ ] **Step 2: Confirmar referências restantes antes de remover**

Run: `rg -n 'TemporaryPharmaceuticalServiceStore|fen-temporary-pharmaceutical-services' src`

Expected: apenas os arquivos planejados e seus testes referenciam a store.

- [ ] **Step 3: Remover a store e atualizar imports/tipos**

```bash
rg -n 'TemporaryPharmaceuticalServiceStore|TemporaryClinicalRecordsStore' src/app
```

Remover somente referências de atendimento; não remover `TemporaryClinicalRecordsStore` se ainda pertencer a outro fluxo.

- [ ] **Step 4: Rodar testes e type-check do frontend**

Run: `yarn test --runInBand && yarn build`

### Task 12: Verificação integrada final

**Files:**
- Modify only if a verification failure exposes uma falha diretamente relacionada ao módulo.

- [ ] **Step 1: Executar toda a suíte backend**

Run: `./gradlew test`

Expected: PASS, incluindo migrations H2, Envers, repository, service, controller e `ServicoFarmaceuticoResourceITTest`.

- [ ] **Step 2: Executar toda a suíte e build frontend**

Run: `yarn test --runInBand && yarn build`

Expected: PASS, sem dependência de `localStorage` para atendimentos.

- [ ] **Step 3: Conferir requisitos críticos contra o spec**

Run: `rg -n 'nextBusinessCode|createId|fen-temporary-pharmaceutical-services|TemporaryPharmaceuticalServiceStore' ../fen-frontend/src/app`

Expected: nenhuma ocorrência ativa na implementação do módulo; a ausência confirma que IDs, códigos, status e retornos não são mais calculados no cliente.

- [ ] **Step 4: Conferir mudanças e registrar resultado verificável**

Run: `git diff --check && git status --short && git diff --stat`

Expected: somente arquivos do módulo e documentação; relatar comandos executados e resultados reais, sem alegar sucesso sem esta evidência.
