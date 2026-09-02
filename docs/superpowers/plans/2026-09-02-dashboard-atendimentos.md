# Dashboard de Atendimentos Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Execute inline in the primary session; do not use subagents.

**Goal:** Expor um resumo histórico autenticado de atendimentos e renderizá-lo no dashboard Angular, com filtros de navegação consistentes.

**Architecture:** `GET /api/dashboard` usa `DashboardService` e consultas agregadas de `ServicoFarmaceuticoRepository`; status dependentes de tempo recebem a data derivada do `Clock`. Angular consome uma resposta por `DashboardService`, enquanto a listagem mantém o estado de filtro na URL e reutiliza seus labels existentes.

**Tech Stack:** Java 25, Spring Boot 4.1, Spring Data JPA, H2, JWT, JUnit 6, Angular 22, RxJS, Vitest, Chart.js e ng2-charts.

**Spec:** `docs/superpowers/specs/2026-09-02-dashboard-atendimentos-design.md`

## Global Constraints

- Não criar status `ABERTO` nem persistir `EXPIRADO` durante leitura.
- Toda data vem de `Clock`; testes temporais usam `Clock.fixed`.
- O dashboard faz uma única chamada agregada e não carrega atendimentos para calcular métricas no Angular.
- Usar o endpoint dedicado `GET /api/dashboard`, protegido pelo JWT existente.
- Tipos exibidos são cuidados farmacêuticos, aplicação de injetáveis, inaloterapia e serviços farmacêuticos; assistência domiciliar não é categoria.
- Usar Chart.js/ng2-charts, os labels existentes e `fen-frontend/src/styles.css`.
- Cards navegam para `/atendimentos` usando query params, inclusive `retornoHoje=true`.
- Não usar subagents ou dependências novas.

---

### Task 1: Definir o contrato e os testes de agregação do backend

**Files:**
- Create: `src/main/java/org/fen/fen/dashboard/dto/DashboardResponse.java`
- Create: `src/test/java/org/fen/fen/dashboard/DashboardServiceTest.java`
- Modify: `src/test/java/org/fen/fen/repository/ServicoFarmaceuticoRepositoryTest.java`

**Interfaces:**
- Produces `DashboardResponse`, whose nested records are `Indicators(long awaitingReturn, long returnsToday, long totalAttendances, long expired)`, `ServiceTypeCount(String type, long count)` and `StatusCount(StatusServicoFarmaceutico status, long count)`.
- Produces critérios de teste para contagens por status, tipo, data e atendimento atual.

- [ ] **Step 1: Escrever os testes RED do service com `Clock.fixed`**

```java
assertThat(response.indicators().returnsToday()).isEqualTo(1);
assertThat(response.indicators().awaitingReturn()).isEqualTo(1);
assertThat(response.indicators().expired()).isEqualTo(1);
```

Inclua prazos ontem, hoje e amanhã, mais atendimentos concluídos e combinações de cuidados, injetáveis, inaloterapia e serviços farmacêuticos.

- [ ] **Step 2: Executar os testes para confirmar RED**

Run: `./gradlew test --tests '*DashboardServiceTest'`

Expected: FAIL porque `DashboardResponse` e `DashboardService` ainda não existem.

- [ ] **Step 3: Escrever testes RED de repository para filtros/contagens**

```java
Page<ServicoFarmaceutico> page = repository.listar("", "", true, LocalDate.of(2026, 9, 2), PageRequest.of(0, 10));
assertThat(page.getTotalElements()).isEqualTo(1);
```

Valide que somente o atendimento atual aguardando com prazo igual ao dia é retornado e que um atendimento expirado não é contado como aguardando.

- [ ] **Step 4: Executar os testes de repository para confirmar RED**

Run: `./gradlew test --tests '*ServicoFarmaceuticoRepositoryTest'`

Expected: FAIL por assinatura e consultas inexistentes.

- [ ] **Step 5: Commit**

```bash
git add src/test/java/org/fen/fen/dashboard/DashboardServiceTest.java src/test/java/org/fen/fen/repository/ServicoFarmaceuticoRepositoryTest.java src/main/java/org/fen/fen/dashboard/dto/DashboardResponse.java
git commit -m "test: define dashboard aggregation contract"
```

### Task 2: Implementar consultas, serviço e endpoint de dashboard

**Files:**
- Create: `src/main/java/org/fen/fen/service/DashboardService.java`
- Create: `src/main/java/org/fen/fen/controller/DashboardController.java`
- Modify: `src/main/java/org/fen/fen/repository/ServicoFarmaceuticoRepository.java`
- Modify: `src/main/java/org/fen/fen/service/ServicoFarmaceuticoService.java`
- Modify: `src/main/java/org/fen/fen/controller/ServicoFarmaceuticoController.java`

**Interfaces:**
- Consumes `Clock`, `ServicoFarmaceuticoRepository` e `DashboardResponse`.
- Produces `GET /api/dashboard` and `GET /api/servicos-farmaceuticos?retornoHoje=true`.

- [ ] **Step 1: Implementar as projections e queries agregadas mínimas**

```java
long contarReconsultasHoje(LocalDate today);
long contarAguardandoRetorno(LocalDate today);
long contarExpirados(LocalDate today);
List<ServicoTipoCountProjection> contarPorTipoServicoMedicamento();
```

Todas as queries temporais exigem `a.atendimentoAtual = s`, `a.encerradoEm is null` e `s.statusPersistido = AGUARDANDO_RETORNO`; aguardando usa `>=`, expirado usa `<` e hoje usa `=`.

- [ ] **Step 2: Estender a mesma query de listagem**

```java
Page<ServicoFarmaceutico> listar(String query, String digits, String status,
        boolean retornoHoje, LocalDate today, Pageable pageable);
```

Quando `retornoHoje` é verdadeiro, aplicar a mesma condição da consulta de reconsultas hoje. Os filtros de status usam a condição efetiva validada pelo resolver.

- [ ] **Step 3: Implementar o `DashboardService`**

```java
@Transactional(readOnly = true)
public DashboardResponse buscarResumo() {
    LocalDate today = LocalDate.now(clock);
    // Invocar somente as agregações do repository e preencher zeros para categorias ausentes.
}
```

O método retorna todas as quatro categorias e todos os três status, inclusive quando seu total é zero.

- [ ] **Step 4: Expor o controller e propagar `retornoHoje` pela listagem**

```java
@GetMapping("/api/dashboard")
public DashboardResponse resumo() { return service.buscarResumo(); }
```

`ServicoFarmaceuticoController#listar` recebe `@RequestParam(defaultValue = "false") boolean retornoHoje` e passa o valor ao service.

- [ ] **Step 5: Executar os testes GREEN do backend**

Run: `./gradlew test --tests '*DashboardServiceTest' --tests '*ServicoFarmaceuticoRepositoryTest'`

Expected: PASS com prazos ontem/hoje/amanhã, cadeias ativas e contagem distinta por atendimento/tipo.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/org/fen/fen/dashboard src/main/java/org/fen/fen/service/DashboardService.java src/main/java/org/fen/fen/controller/DashboardController.java src/main/java/org/fen/fen/repository/ServicoFarmaceuticoRepository.java src/main/java/org/fen/fen/service/ServicoFarmaceuticoService.java src/main/java/org/fen/fen/controller/ServicoFarmaceuticoController.java
git commit -m "feat: add attendance dashboard API"
```

### Task 3: Cobrir o contrato HTTP e a consistência ponta a ponta

**Files:**
- Create: `src/test/java/org/fen/fen/dashboard/DashboardControllerTest.java`
- Create: `src/test/java/org/fen/fen/dashboard/DashboardResourceITTest.java`

**Interfaces:**
- Consumes `GET /api/dashboard`, JWT e listagem filtrada.
- Produces verificação autenticada do DTO e da igualdade dashboard/listagem.

- [ ] **Step 1: Escrever e executar o controller isolado**

```java
mockMvc.perform(get("/api/dashboard"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.indicators.totalAttendances").value(4));
```

Teste a delegação para `DashboardService`; a ausência de JWT é coberta pelo teste de integração.

- [ ] **Step 2: Escrever e executar o teste de integração**

```java
// Com token, obter $.indicators.expired e comparar com
// $.totalElements de GET /api/servicos-farmaceuticos?status=EXPIRADO.
```

Repita a comparação para aguardando e `retornoHoje=true`.

- [ ] **Step 3: Corrigir somente falhas de contrato expostas pelos testes**

Run: `./gradlew test --tests '*DashboardControllerTest' --tests '*DashboardResourceITTest'`

Expected: os testes já implementados nas Tasks 1–2 passam; qualquer falha deve indicar divergência de serialização, segurança ou consistência entre dashboard/listagem.

- [ ] **Step 4: Executar a verificação GREEN**

Run: `./gradlew test --tests '*DashboardControllerTest' --tests '*DashboardResourceITTest' --tests '*ServicoFarmaceuticoResourceITTest'`

Expected: PASS, incluindo `401` sem JWT e os mesmos totais para cards/listagem.

- [ ] **Step 5: Commit**

```bash
git add src/test/java/org/fen/fen/dashboard src/test/java/org/fen/fen/servicofarmaceutico/ServicoFarmaceuticoResourceITTest.java
git commit -m "test: cover dashboard endpoint and filter consistency"
```

### Task 4: Definir o contrato Angular e URL da listagem

**Files:**
- Create: `../fen-frontend/src/app/domain/dashboard.service.ts`
- Create: `../fen-frontend/src/app/domain/dashboard.service.spec.ts`
- Modify: `../fen-frontend/src/app/domain/servico-farmaceutico.service.ts`
- Modify: `../fen-frontend/src/app/pages/atendimentos-page/atendimentos-page.ts`
- Modify: `../fen-frontend/src/app/pages/atendimentos-page/atendimentos-page.html`

**Interfaces:**
- Produces `DashboardService.getSummary(): Observable<DashboardResponse>`.
- Produces `ServicoFarmaceuticoService.list(query, status, retornoHoje, page, size)`.
- Consumes `status` e `retornoHoje` de `ActivatedRoute.queryParamMap`.

- [ ] **Step 1: Escrever RED do Angular service**

```ts
service.getSummary().subscribe((summary) => expect(summary.indicators.expired).toBe(2));
const request = httpMock.expectOne(`${environment.apiUrl}/api/dashboard`);
expect(request.request.method).toBe('GET');
```

- [ ] **Step 2: Escrever RED da sincronização da listagem**

```ts
await router.navigateByUrl('/atendimentos?status=EXPIRADO');
expect(component.statusFilter()).toBe('EXPIRADO');
await router.navigateByUrl('/atendimentos?retornoHoje=true');
expect(component.returnsTodayFilter()).toBe(true);
```

Teste um chip removível que retira `retornoHoje` da URL e o parâmetro HTTP enviado pelo service.

- [ ] **Step 3: Executar RED**

Run: `yarn test --watch=false --include src/app/domain/dashboard.service.spec.ts --include src/app/app.spec.ts`

Expected: FAIL por service, assinatura e estado de URL ausentes.

- [ ] **Step 4: Implementar service e sincronização de query params**

```ts
list(query: string, status: AttendanceStatusFilter, retornoHoje: boolean, page: number, size: number)
```

Somente enviar `retornoHoje=true` quando o filtro estiver ativo. A listagem atualiza URL com `Router.navigate` e `queryParamsHandling` explícito, preservando filtros em refresh e histórico.

- [ ] **Step 5: Executar GREEN**

Run: `yarn test --watch=false --include src/app/domain/dashboard.service.spec.ts --include src/app/app.spec.ts`

Expected: PASS e a UI informa claramente que o filtro de hoje está ativo.

- [ ] **Step 6: Commit**

```bash
git add src/app/domain/dashboard.service.ts src/app/domain/dashboard.service.spec.ts src/app/domain/servico-farmaceutico.service.ts src/app/pages/atendimentos-page
git commit -m "feat: add dashboard client and attendance URL filters"
```

### Task 5: Implementar cards e gráficos do dashboard

**Files:**
- Modify: `../fen-frontend/src/app/pages/inicio-page/inicio-page.ts`
- Modify: `../fen-frontend/src/app/pages/inicio-page/inicio-page.html`
- Modify: `../fen-frontend/src/app/app.spec.ts`

**Interfaces:**
- Consumes `DashboardService`, `DashboardResponse`, labels existentes, Router e `BaseChartDirective`.
- Produces quatro cards navegáveis, dois gráficos de barras e estados de loading/erro/vazio.

- [ ] **Step 1: Escrever RED de página**

```ts
expect(compiled.querySelector('[data-dashboard-card="awaiting-return"]')?.textContent).toContain('7');
expect(compiled.querySelectorAll('[data-dashboard-card]').length).toBe(4);
expect(compiled.querySelector('[data-dashboard-empty="service-types"]')).toBeTruthy();
```

Inclua loading sem números, erro com botão de tentar novamente, zero após resposta, labels amigáveis e navegação para as quatro URLs esperadas.

- [ ] **Step 2: Executar RED**

Run: `yarn test --watch=false --include src/app/app.spec.ts`

Expected: FAIL porque a página ainda tem métricas e séries estáticas.

- [ ] **Step 3: Implementar o dashboard mínimo**

```ts
protected readonly summary = signal<DashboardResponse | null>(null);
protected readonly isLoading = signal(true);
protected readonly errorMessage = signal('');
```

Usar `button` para cards, `Router.navigate` para `/atendimentos`, classes `card`, `skeleton`, `alert`, `empty` e grid Tailwind responsivo. Criar dados `ChartData<'bar'>` para tipos e status, com tooltip que mostre rótulo e quantidade. Remover seletor de período e gráficos mensais fictícios.

- [ ] **Step 4: Executar GREEN e build**

Run: `yarn test --watch=false --include src/app/app.spec.ts && yarn build`

Expected: PASS e build de produção concluído sem nova dependência.

- [ ] **Step 5: Commit**

```bash
git add src/app/pages/inicio-page src/app/app.spec.ts
git commit -m "feat: render attendance dashboard"
```

### Task 6: Verificação integrada e revisão final

**Files:**
- Modify somente arquivos necessários pelos testes de Tasks 1–5.

- [ ] **Step 1: Executar a suíte backend**

Run: `./gradlew test`

Expected: PASS.

- [ ] **Step 2: Executar a suíte e build frontend**

Run: `yarn test --watch=false && yarn build`

Expected: PASS.

- [ ] **Step 3: Inspecionar alterações**

Run: `git diff --check && git status --short`

Expected: nenhum erro de whitespace e somente arquivos do dashboard/listagem/testes/documentação.

- [ ] **Step 4: Validar manualmente o fluxo principal**

Com backend autenticado, abrir `/inicio`, confirmar cards e gráficos; abrir cada card e conferir `totalElements` na listagem contra o respectivo indicador. Confirmar que `/atendimentos?retornoHoje=true` mostra critério visível e removível.

- [ ] **Step 5: Commit de ajustes de verificação, se necessário**

```bash
git add src/main/java/org/fen/fen/controller/DashboardController.java src/main/java/org/fen/fen/service/DashboardService.java src/main/java/org/fen/fen/repository/ServicoFarmaceuticoRepository.java src/main/java/org/fen/fen/service/ServicoFarmaceuticoService.java src/main/java/org/fen/fen/controller/ServicoFarmaceuticoController.java src/test/java/org/fen/fen/dashboard src/test/java/org/fen/fen/repository/ServicoFarmaceuticoRepositoryTest.java
git commit -m "test: verify dashboard integration"
```
