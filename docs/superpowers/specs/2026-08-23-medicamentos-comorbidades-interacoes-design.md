# Integração de Medicamentos, Comorbidades e Interações

## Objetivo

Substituir a persistência local de medicamentos, comorbidades e suas interações no Angular por uma API Spring Boot autenticada, persistida no PostgreSQL e integralmente reproduzível no H2 de testes.

O módulo não representa estoque e não adiciona atributos clínicos à interação. A interação indica somente que um medicamento cadastrado está relacionado a uma comorbidade cadastrada.

## Estado atual e decisões de compatibilidade

O backend contém uma implementação provisória ainda não implantada, composta por `Medication`, `Comorbity`, um `ManyToMany`, controllers genéricos que expõem entidades e o changeset `004-create-medication-and-comorbity.yaml`. Como o changeset foi usado somente no H2, ele será substituído pela modelagem definitiva sem migration de dados.

O frontend utiliza os nomes `Medication`, `Comorbidity`, `name`, `measurementUnit`, `administrationRoute` e `medicationInteractionIds`. Esses nomes são adequados como contrato de integração e serão preservados para reduzir alterações nos componentes. A nomenclatura interna e física do backend seguirá o domínio em português: `Medicamento`, `Comorbidade`, `Interacao`, `medicamento`, `comorbidade` e `interacao`.

As alterações locais existentes no backend serão tratadas como trabalho do usuário: o que estiver correto será aproveitado, e somente os arquivos diretamente relacionados serão substituídos ou ajustados.

## Modelo persistente

### Medicamento

`Medicamento` estende a abstração de auditoria existente e possui:

- `id: UUID`;
- `nome`;
- `unidadeMedida`;
- `viaAdministracao`;
- `createdAt`, `updatedAt`, `createdBy`, `updatedBy` herdados.

A entidade é auditada com Envers e mapeada para `medicamento`, com colunas `nome`, `unidademedida`, `viaadministracao`, `createdat`, `updatedat`, `createdby` e `updatedby`.

### Comorbidade

`Comorbidade` estende a mesma abstração e possui:

- `id: UUID`;
- `nome`;
- coleção lazy de interações;
- campos de auditoria herdados.

A entidade é auditada com Envers e mapeada para `comorbidade`.

### Interacao

`Interacao` é uma entidade associativa explícita, e não um `ManyToMany`. Ela possui:

- `id: UUID`;
- `comorbidade: ManyToOne`;
- `medicamento: ManyToOne`;
- campos de auditoria herdados.

Ela é mapeada para `interacao`, auditada com Envers e protegida por uma constraint única sobre `(comorbidadeid, medicamentoid)`. A entidade explícita preserva identidade e histórico individual da associação, permite sincronização incremental e mantém espaço arquitetural para evolução sem adicionar atributos clínicos agora.

`Comorbidade` será o agregado usado para coordenar interações. Não haverá cascade de remoção de `Medicamento` ou `Comorbidade`. A remoção de uma interação pertencente à comorbidade poderá usar orphan removal controlado, desde que os testes confirmem a geração correta do histórico Envers.

## Migrations

O changeset provisório `004` será reescrito para criar, em ordem:

1. `medicamento` e índices de pesquisa por nome;
2. `comorbidade` e índice por nome;
3. `interacao`, suas foreign keys e a constraint única;
4. `aud.medicamento_aud`;
5. `aud.comorbidade_aud`;
6. `aud.interacao_aud`.

Todas as entidades usarão UUID. As tabelas de auditoria terão chave `(id, rev)` e foreign key para `aud.revinfo`. A configuração Java e as migrations serão validadas tanto no PostgreSQL quanto no modo H2 já usado pelos testes.

## Contratos HTTP

Todos os endpoints ficam sob `/api`, herdam a exigência JWT global e podem ser usados por `ADMIN`, `FARMACEUTICO` e `ESTAGIARIO`. Não haverá endpoint CRUD exclusivo para `Interacao`.

### Medicamentos

- `POST /api/medicamentos` cria e retorna `201`;
- `GET /api/medicamentos/{id}` retorna o detalhe;
- `GET /api/medicamentos?query=&page=0&size=10` lista e filtra no servidor;
- `PUT /api/medicamentos/{id}` atualiza e retorna `200`;
- `DELETE /api/medicamentos/{id}` exclui quando não houver referência impeditiva e retorna `204`;
- `GET /api/medicamentos/autocomplete?query=&limit=8` fornece a busca reutilizável.

Requests e responses preservam os campos Angular:

```json
{
  "name": "Dipirona",
  "measurementUnit": "mg",
  "administrationRoute": "Oral"
}
```

O autocomplete retorna uma lista pequena, ordenada por nome, contendo `id`, `name`, `measurementUnit` e `administrationRoute`. O limite será validado e limitado no backend para impedir consultas excessivas.

### Comorbidades

- `POST /api/comorbidades` cria a comorbidade e suas interações em uma transação;
- `GET /api/comorbidades/{id}` retorna o detalhe com medicamentos associados;
- `GET /api/comorbidades?query=&page=0&size=10` lista e filtra no servidor;
- `PUT /api/comorbidades/{id}` sincroniza nome e interações;
- `DELETE /api/comorbidades/{id}` exclui quando não houver referência impeditiva.

O request usa somente IDs persistentes:

```json
{
  "name": "Hipertensão",
  "medicationInteractionIds": ["uuid-1", "uuid-2"]
}
```

A resposta detalhada inclui `medicationInteractionIds` para compatibilidade e `interactionMedications` com summaries completos o bastante para edição e visualização sem consultas adicionais.

As respostas paginadas usam o contrato `Page` do Spring: `content`, `number`, `size`, `totalElements` e `totalPages`. A API usa página baseada em zero; o controle visual Angular continua baseado em um.

### Consulta de interações para atendimentos

O módulo disponibilizará uma consulta em lote que receba IDs de medicamentos e comorbidades e retorne os pares cadastrados necessários aos warnings. O contrato não dependerá das futuras entidades de paciente ou atendimento e não fará uma chamada por combinação.

Uma forma compatível é:

```text
GET /api/interacoes?medicamentoIds=id1,id2&comorbidadeIds=id3,id4
```

A resposta contém summaries de medicamento e comorbidade. Esse endpoint é uma consulta de domínio, não um CRUD de interação.

## Regras de aplicação

Os Services executam:

- normalização por `trim` sem alterar arbitrariamente caixa ou acentuação;
- validação de existência de IDs;
- rejeição de IDs repetidos no request de interação com `400`;
- proteção definitiva de duplicidade no banco, traduzida para `409` quando houver concorrência;
- sincronização incremental de interações na edição;
- paginação e filtro por texto ignorando filtro vazio;
- ordenação previsível por nome;
- tradução de recurso inexistente para `404`;
- tradução de exclusão impedida por referência para `409` com mensagem específica.

Na sincronização, interações já existentes são mantidas com o mesmo UUID. Somente associações ausentes são inseridas e somente associações removidas da seleção são excluídas. Toda a alteração ocorre na mesma transação.

Auditoria de usuário depende exclusivamente do `AuditorAware` e do principal JWT existente. Requests não aceitam `createdBy` ou `updatedBy`.

## Integração Angular

Serão introduzidos `MedicationService` e `ComorbidityService` HTTP, seguindo o padrão de `environment.apiUrl` e o interceptor JWT existente.

Os componentes de listagem passarão a reagir a página, tamanho e filtro, solicitarão somente a página atual e manterão os tamanhos `10`, `20`, `50`, `100`. Mudanças de filtro ou tamanho retornam à primeira página. Loading, erro e exclusão em andamento serão representados explicitamente para evitar double submit.

Os formulários de cadastro/edição e páginas de visualização carregarão os registros por ID. Edição de comorbidade preencherá a seleção com `interactionMedications` da resposta detalhada.

O autocomplete será assíncrono e reutilizável. Ele aplicará debounce e `switchMap` para cancelar a busca anterior, limitará os resultados pelo endpoint e continuará emitindo o objeto `Medication` selecionado.

O `TemporaryClinicalRecordsStore` não manterá mais medicamentos e comorbidades como fonte de verdade, não gerará seus IDs e não os persistirá no `localStorage`. Os dados temporários de pacientes e serviços farmacêuticos permanecerão até a integração dos respectivos módulos.

Para não quebrar os serviços farmacêuticos, a lista local de medicamentos será substituída pelo autocomplete HTTP. A verificação de warning passará a utilizar a consulta em lote de interações quando houver IDs persistentes de medicamento e comorbidade. Enquanto pacientes continuarem temporários, seus `comorbidityIds` permanecem no frontend; nenhuma regra clínica será codificada.

## Erros e segurança

Controllers serão finos e usarão Bean Validation. O handler existente será ajustado para mensagens de integridade específicas, sem reutilizar a mensagem atual de e-mail/CPF para qualquer constraint.

Status esperados:

- `201` criação;
- `200` consulta e atualização;
- `204` exclusão;
- `400` estrutura inválida ou IDs duplicados no request;
- `401` ausência/token inválido;
- `404` entidade inexistente;
- `409` duplicidade concorrente ou exclusão impedida.

Não serão adicionadas restrições de role além da autenticação já configurada.

## Estratégia de testes

A implementação seguirá ciclos TDD por comportamento.

Backend:

- repository tests com datasets determinísticos para persistência, filtro, autocomplete, relações, carregamento e constraint única;
- service tests para CRUD, inexistência, conflitos, exclusão e todos os cenários de sincronização;
- controller tests com MockMvc para validação, serialização, paginação, filtro, autenticação e erros;
- `MedicamentoResourceITTest` e `ComorbidadeResourceITTest` para o fluxo HTTP completo autenticado;
- fluxo integrado com dois medicamentos, criação de comorbidade, consulta, remoção de uma interação e nova consulta;
- teste Envers das três entidades e da remoção de interação;
- execução da suíte completa com Gradle.

Frontend:

- testes dos services HTTP com `HttpTestingController`;
- testes do debounce/cancelamento do autocomplete;
- adaptação dos testes de páginas para operações assíncronas, loading, erros, paginação e prevenção de double submit;
- testes de compatibilidade dos warnings de atendimento;
- execução de `yarn test` e `yarn build`.

## Limites desta entrega

Pacientes e serviços farmacêuticos não serão persistidos no backend nesta tarefa. Permanecem temporariamente no frontend:

- cadastro e associação de pacientes;
- atendimentos e acompanhamentos;
- estado visual e dados transitórios dos formulários;
- geração client-side de PDF;
- warnings informativos, alimentados pelos dados reais de interação.

Não serão adicionados estoque, severidade, contraindicação, dose, risco, recomendação terapêutica ou protocolo clínico.

## Critérios de conclusão

A entrega estará concluída quando migrations reconstruírem os schemas no H2 e PostgreSQL, as três entidades forem auditadas, CRUDs e consultas funcionarem sob JWT, paginação e autocomplete forem server-side, interações forem sincronizadas transacionalmente, as telas Angular consumirem a API sem fonte local paralela e as suítes backend/frontend passarem.
