# Acompanhamento Farmacoterapêutico como Step Implementation Plan

> **For agentic workers:** Execute inline in the main session; this task explicitly forbids subagents.

**Goal:** Separar Acompanhamento farmacoterapêutico de Serviços farmacêuticos, persistindo e exibindo medicamentos por origem própria sem alterar o acompanhamento de retornos.

**Architecture:** `ServicoFarmaceutico` passa a possuir a flag clínica `acompanhamentoFarmacoterapeutico`, mapeada na coluna existente. O request/response adiciona `pharmacotherapeuticFollowUp` no nível raiz, e os medicamentos desse bloco recebem a origem `ACOMPANHAMENTO_FARMACOTERAPEUTICO`. O Angular mantém estado independente para cada Step e reutiliza o mesmo template interno para o formulário de medicamento sem prescritor.

**Tech Stack:** Spring Boot 4, JPA/Hibernate Envers, Liquibase, Angular 22, Vitest.

**Spec:** `fen/docs/superpowers/specs/2026-08-28-servicos-farmaceuticos-design.md`

## Global Constraints

- Não alterar a lógica de retornos (`Acompanhamento`, `FOLLOW_UP_RETURN`, extensão, expiração ou status).
- Reutilizar a coluna e a coluna de auditoria existentes; não criar migration só para mudar o mapeamento Java.
- Não reclassificar nem duplicar medicamentos históricos de origem `SERVICOS_FARMACEUTICOS`.
- Novos medicamentos de Acompanhamento farmacoterapêutico usam exclusivamente `ACOMPANHAMENTO_FARMACOTERAPEUTICO`.
- Executar toda a implementação na sessão principal, sem subagents.

---

### Task 1: Separar o contrato e o agregado do backend

**Files:**

- Modify: `fen/src/main/java/org/fen/fen/domain/DadosServicosFarmaceuticos.java`
- Modify: `fen/src/main/java/org/fen/fen/domain/ServicoFarmaceutico.java`
- Modify: `fen/src/main/java/org/fen/fen/domain/TipoServicoMedicamento.java`
- Modify: `fen/src/main/java/org/fen/fen/servicofarmaceutico/dto/ServicoFarmaceuticoRequest.java`
- Modify: `fen/src/main/java/org/fen/fen/servicofarmaceutico/dto/ServicoFarmaceuticoResponse.java`
- Modify: `fen/src/main/java/org/fen/fen/mapper/ServicoFarmaceuticoMapper.java`
- Modify: `fen/src/main/java/org/fen/fen/service/ServicoFarmaceuticoService.java`
- Test: `fen/src/test/java/org/fen/fen/servicofarmaceutico/ServicoFarmaceuticoResourceITTest.java`

**Interfaces:**

- Consumes: `ComplementaryServices` e `MedicamentoAtendimentoRequest` existentes.
- Produces: `pharmacotherapeuticFollowUp: { medications: Medication[] }` separado de `complementaryServices`.

- [ ] **Step 1: Confirmar o teste de contrato vermelho**

Run: `devenv shell -- ./gradlew test --tests org.fen.fen.servicofarmaceutico.ServicoFarmaceuticoResourceITTest --tests org.fen.fen.repository.DashboardRepositoryTest`

Expected: compilation fails porque ainda faltam a flag clínica e a contagem separada.

- [ ] **Step 2: Implementar o menor modelo separado**

```java
@Column(name = "acompanhamentofarmacoterapeutico")
private Boolean acompanhamentoFarmacoterapeutico;

public record PharmacotherapeuticFollowUp(
        @NotNull List<@Valid MedicamentoAtendimentoRequest> medications
) {}
```

- [ ] **Step 3: Sincronizar e responder medicamentos por origem**

```java
adicionarSolicitados(solicitados,
    request.pharmacotherapeuticFollowUp() == null ? List.of()
        : request.pharmacotherapeuticFollowUp().medications(),
    TipoServicoMedicamento.ACOMPANHAMENTO_FARMACOTERAPEUTICO);
```

- [ ] **Step 4: Executar o teste de contrato verde**

Run: `devenv shell -- ./gradlew test --tests org.fen.fen.servicofarmaceutico.ServicoFarmaceuticoResourceITTest`

Expected: o detalhe retorna listas distintas por origem e não expõe o boolean dentro de `complementaryServices`.

### Task 2: Contar a categoria clínica no dashboard

**Files:**

- Modify: `fen/src/main/java/org/fen/fen/repository/ServicoFarmaceuticoRepository.java`
- Modify: `fen/src/main/java/org/fen/fen/service/DashboardService.java`
- Test: `fen/src/test/java/org/fen/fen/repository/DashboardRepositoryTest.java`
- Test: `fen/src/test/java/org/fen/fen/dashboard/DashboardServiceTest.java`
- Test: `fen/src/test/java/org/fen/fen/dashboard/DashboardResourceITTest.java`

**Interfaces:**

- Produces: quinta entrada `acompanhamento-farmacoterapeutico` em `DashboardResponse.serviceTypes`.

- [ ] **Step 1: Escrever as expectativas de categoria separada**

```java
assertThat(repository.contarServicosFarmaceuticos()).isEqualTo(0L);
assertThat(repository.contarAcompanhamentosFarmacoterapeuticos()).isEqualTo(1L);
```

- [ ] **Step 2: Restringir a consulta de Serviços farmacêuticos**

```jpql
where s.dadosServicosFarmaceuticos.assistenciaDomiciliar is not null
   or s.dadosServicosFarmaceuticos.indicacaoTranstornosMenores is not null
   or s.dadosServicosFarmaceuticos.sinaisESintomas is not null
   or exists (... tipoServico = SERVICOS_FARMACEUTICOS)
```

- [ ] **Step 3: Adicionar a quinta categoria ao serviço**

```java
new DashboardResponse.ServiceTypeCount(
    "acompanhamento-farmacoterapeutico",
    repository.contarAcompanhamentosFarmacoterapeuticos())
```

- [ ] **Step 4: Rodar os testes de dashboard**

Run: `devenv shell -- ./gradlew test --tests org.fen.fen.repository.DashboardRepositoryTest --tests org.fen.fen.dashboard.DashboardServiceTest --tests org.fen.fen.dashboard.DashboardResourceITTest`

Expected: a nova categoria existe e um atendimento pode contribuir em ambas as categorias clínicas.

### Task 3: Atualizar modelos e formulário Angular

**Files:**

- Modify: `fen-frontend/src/app/domain/clinical-records.ts`
- Modify: `fen-frontend/src/app/domain/attendance-labels.ts`
- Modify: `fen-frontend/src/app/domain/dashboard.service.ts`
- Modify: `fen-frontend/src/app/pages/servicos-farmaceuticos-page/servicos-farmaceuticos-page.ts`
- Modify: `fen-frontend/src/app/pages/servicos-farmaceuticos-page/servicos-farmaceuticos-page.html`
- Test: `fen-frontend/src/app/app.spec.ts`

**Interfaces:**

- Produces: `PharmaceuticalServiceKey` inclui `'acompanhamento-farmacoterapeutico'`.
- Produces: formulário envia `pharmacotherapeuticFollowUp` separadamente e mantém `followUp` exclusivamente para retornos.

- [ ] **Step 1: Adicionar expectativa de formulário vermelho**

```typescript
expect(compiled.querySelector('#enableAcompanhamentoFarmacoterapeutico')).not.toBeNull();
expect(compiled.querySelector('[name="pharmacotherapeuticFollowUp"]')).toBeNull();
```

- [ ] **Step 2: Criar estado independente e Step 06**

```typescript
type MedicationSection = 'injectable' | 'inhalotherapy' | 'complementary' | 'pharmacotherapeuticFollowUp';
```

- [ ] **Step 3: Reutilizar o template interno de medicamentos sem prescritor**

```html
<ng-template #nonPrescriptionMedicationForm let-section="section" let-prefix="prefix">
  <!-- autocomplete, lote, validade, posologia e mini-listagem do section -->
</ng-template>
```

- [ ] **Step 4: Ajustar edição e validação**

```typescript
this.enabledSteps['acompanhamento-farmacoterapeutico'] = Boolean(
  attendance.pharmacotherapeuticFollowUp,
);
this.medicationItems.pharmacotherapeuticFollowUp = [
  ...(attendance.pharmacotherapeuticFollowUp?.medications ?? []),
];
```

- [ ] **Step 5: Rodar testes frontend focados**

Run: `devenv shell -- yarn test --watch=false`

Expected: o formulário compila, o Step antigo não contém checkbox interno e as listas não compartilham estado.

### Task 4: Separar detalhe, PDF e contratos de dashboard no frontend

**Files:**

- Modify: `fen-frontend/src/app/pages/visualizar-atendimento-page/visualizar-atendimento-page.html`
- Modify: `fen-frontend/src/app/domain/atendimento-print-data.ts`
- Modify: `fen-frontend/src/app/components/atendimento-print/atendimento-print.html`
- Modify: `fen-frontend/src/app/domain/atendimento-print-data.spec.ts`
- Modify: `fen-frontend/src/app/pages/inicio-page/inicio-page.spec.ts`
- Modify: `fen-frontend/src/app/app.spec.ts`

**Interfaces:**

- Produces: seção independente para Acompanhamento farmacoterapêutico no detalhe e PDF.

- [ ] **Step 1: Escrever a expectativa de PDF com duas listas distintas**

```typescript
expect(data.complementaryServices?.medications[0].batch).toBe('SVC-01');
expect(data.pharmacotherapeuticFollowUp?.medications[0].batch).toBe('AFT-01');
```

- [ ] **Step 2: Mapear a nova seção de impressão**

```typescript
pharmacotherapeuticFollowUp: attendance.pharmacotherapeuticFollowUp
  ? mapMedicationService(attendance.pharmacotherapeuticFollowUp)
  : null,
```

- [ ] **Step 3: Exibir a tabela dedicada no detalhe e no template de impressão**

```html
@if (currentAttendance.pharmacotherapeuticFollowUp) {
  <section class="card"><h2 class="card-title">Acompanhamento farmacoterapêutico</h2></section>
}
```

- [ ] **Step 4: Rodar testes de impressão e dashboard**

Run: `devenv shell -- yarn test --watch=false`

Expected: PDF, labels e gráfico reconhecem a nova categoria sem confundi-la com retornos.

### Task 5: Verificação integral

**Files:**

- Verify: `fen`
- Verify: `fen-frontend`

- [ ] **Step 1: Rodar toda a suíte backend**

Run: `devenv shell -- ./gradlew test`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Rodar build e testes frontend**

Run: `devenv shell -- yarn test --watch=false && devenv shell -- yarn build`

Expected: testes e compilação Angular aprovados.

- [ ] **Step 3: Conferir o diff final**

Run: `git -C fen diff --check && git -C fen-frontend diff --check && git -C fen status --short && git -C fen-frontend status --short`

Expected: sem whitespace errors e apenas arquivos relacionados à mudança.
