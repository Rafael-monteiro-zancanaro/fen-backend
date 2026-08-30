# Integração de Serviços Farmacêuticos e Atendimentos

## Objetivo

Substituir a persistência transitória de atendimentos, medicamentos utilizados e acompanhamentos no Angular por uma API Spring Boot autenticada, transacional, auditada e reproduzível por Liquibase em PostgreSQL e H2.

O atendimento é o agregado central. Paciente, Medicamento, Comorbidade e Interação já existem e serão reutilizados; não serão recriados nem duplicados. A impressão permanece client-side e os avisos de referência clínica e interação continuam informativos no frontend.

## Levantamento e compatibilidade

O frontend atual usa `TemporaryPharmaceuticalServiceStore`, com `localStorage`, geração local de UUID/código, paginação em memória e criação local de retornos. As telas existentes são criação/continuação, listagem, visualização, busca avançada e impressão. Não há edição; esta entrega acrescentará a rota e ação de edição reutilizando o formulário.

Os dados efetivamente coletados são:

- paciente já cadastrado ou criado após consulta de CPF;
- cuidados farmacêuticos: glicemia, pressão sistólica, pressão diastólica e temperatura;
- aplicação de injetáveis: medicamentos, via de administração, prescritor e CRM/CRO;
- inaloterapia: medicamentos, prescritor e CRM/CRO;
- serviços farmacêuticos: três indicadores, sinais/sintomas, medicamentos, número de ficha e data;
- acompanhamento: intervalo em dias e quantidade de retornos.

O cadastro de `Medicamento` possui somente `nome`, `unidadeMedida` e `viaAdministracao`. A UI chama a apresentação de `medicationConcentration`, mas ela é formada por `name + measurementUnit`; não existe concentração específica do item usado. Assim, `MedicamentoAtendimento` referencia o medicamento por UUID e não duplica uma concentração inexistente. A resposta detalhada continuará fornecendo a apresentação formatada para manter a UI e o PDF.

Paciente atualmente não possui campo de número de endereço. Nenhum campo será inventado nesta entrega.

## Modelo persistente

### ServicoFarmaceutico

`ServicoFarmaceutico` estende `AuditableEntity`, usa UUID e é auditado por Envers. Ele possui:

- `codigo: long`, identificador de negócio único;
- `paciente: ManyToOne` obrigatório;
- `dataAtendimento: LocalDate` e auditoria temporal herdada;
- `statusPersistido: StatusServicoFarmaceutico`, com `AGUARDANDO_RETORNO` ou `CONCLUIDO`;
- `acompanhamento: ManyToOne` opcional;
- `atendimentoAnterior: ManyToOne` opcional;
- `numeroRetorno: int`, zero no atendimento inicial;
- componentes opcionais estruturados para cuidados, aplicação, inaloterapia e serviços farmacêuticos;
- coleção `medicamentosAtendimento` com `orphanRemoval` controlado.

Os componentes estruturados são `@Embeddable`, não entidades independentes: só existem como parte do atendimento e não introduzem tabelas de passos inativos. A presença do componente define que o serviço foi realizado. O request não contém uma lista artificial de passos ativos.

### MedicamentoAtendimento

`MedicamentoAtendimento` é entidade auditada com UUID e auditoria herdada. Ela contém `servicoFarmaceutico`, `medicamento`, `tipoServico`, `lote`, `validade`, `posologia` e, para aplicação de injetáveis, a via já escolhida para o serviço. `tipoServico` é o enum fechado `APLICACAO_INJETAVEIS`, `INALOTERAPIA` ou `SERVICOS_FARMACEUTICOS`.

O vínculo preserva o medicamento cadastrado real e permite busca por medicamento e lote no mesmo item. Não há limite artificial de itens por serviço.

### Acompanhamento e retornos

`Acompanhamento` é entidade auditada com UUID e auditoria herdada. Guarda atendimento inicial, atendimento atual, `intervaloRetornoDias`, `quantidadeRetornos`, `proximoRetorno`, `encerradoEm` e versão otimista. Todo retorno é outro `ServicoFarmaceutico`, associado ao mesmo acompanhamento, com código próprio, `numeroRetorno` crescente e referência ao atendimento anterior.

A migration cria a constraint única `(acompanhamentoid, numeroretorno)`. Na criação de retorno, o service bloqueia pessimisticamente o acompanhamento e revalida que o atendimento da URL ainda é o atual. Isso impede dois retornos para a mesma posição mesmo que duas validações de continuação tenham ocorrido antes.

Para `quantidadeRetornos = 3`, o inicial usa posição zero e os novos atendimentos usam 1, 2 e 3. O resumo do atual retorna `nextReturnNumber = numeroRetorno + 1`, enquanto ele for menor que três. Portanto, a ação mostra `1 de 3`, `2 de 3`, `3 de 3` e some após o último retorno.

## Status persistido e status efetivo

O enum exposto é `AGUARDANDO_RETORNO`, `EXPIRADO` e `CONCLUIDO`. O banco só persiste `AGUARDANDO_RETORNO` e `CONCLUIDO`; `EXPIRADO` é estado efetivo derivado e nunca é gravado apenas porque uma consulta foi feita.

`StatusEfetivoResolver`, dependente de `Clock`, é a única definição utilizada em detalhe, resumo, busca, filtros, continuação, criação de retorno e encerramento:

1. `CONCLUIDO` persistido é sempre efetivamente concluído;
2. `AGUARDANDO_RETORNO` cujo `proximoRetorno` é anterior a `Clock.now()` é efetivamente expirado;
3. os demais aguardam retorno.

A continuação aceita tanto aguardando quanto expirado se o atendimento for o atual e houver retorno pendente. O encerramento manual só aceita o estado efetivo expirado, persiste o atendimento atual como concluído e marca o acompanhamento encerrado. Ao criar retorno, o anterior passa a concluído; o novo fica aguardando se ainda houver posições, ou concluído se for o último.

## Código de negócio e migrations

`servicofarmaceutico_codigo_seq`, criada por Liquibase, gera o código por `nextval` sob responsabilidade do banco. A sequência evita concorrência e não usa `MAX(codigo) + 1`; lacunas por rollback são comportamento normal e não comprometem unicidade ou ordenação.

O changeset `007-create-servico-farmaceutico.yaml` cria, nesta ordem:

1. sequence de código;
2. `servicofarmaceutico`, FKs, constraint de código e índices de paciente/data/status;
3. `acompanhamento`, FKs e índices para atual/próximo retorno;
4. `medicamentoatendimento`, FKs e índices por medicamento/lote e atendimento;
5. constraint de posição de retorno;
6. tabelas `aud.servicofarmaceutico_aud`, `aud.acompanhamento_aud` e `aud.medicamentoatendimento_aud`, com PK e FK para `aud.revinfo`.

Todas as colunas seguem minúsculas sem `_`; somente `_aud` é usado nas tabelas Envers. A migration deve funcionar no PostgreSQL e no H2 em modo PostgreSQL.

## Contratos HTTP

Todos os endpoints exigem JWT e não impõem papel adicional aos papéis já autenticados `ADMIN`, `FARMACEUTICO` e `ESTAGIARIO`.

- `POST /api/servicos-farmaceuticos`: cria atendimento e retorna `201`;
- `GET /api/servicos-farmaceuticos/{id}`: detalhe completo;
- `PUT /api/servicos-farmaceuticos/{id}`: atualiza dados e medicamentos de modo incremental;
- `GET /api/servicos-farmaceuticos?query=&status=&page=0&size=10`: página de resumos;
- `GET /api/servicos-farmaceuticos/busca-avancada?cpfPaciente=&medicamentoId=&lote=&dataAtendimento=&page=0&size=10`: página de rastreabilidade;
- `GET /api/servicos-farmaceuticos/{id}/continuacao`: somente valida contexto e devolve paciente, progresso e atendimento anterior; não reserva nem altera nada;
- `POST /api/servicos-farmaceuticos/{id}/retornos`: revalida sob lock e cria o retorno no salvamento;
- `POST /api/servicos-farmaceuticos/{id}/encerrar`: encerra atendimento efetivamente expirado.

Para criação normal, o request usa `patientId` quando o CPF já foi localizado. Para paciente novo, usa um subrequest de paciente; o serviço verifica CPF e retorna conflito se um paciente já existir, em vez de criar ou sobrescrever automaticamente. Retornos recebem apenas o paciente associado ao contexto; o backend confirma que ele é o mesmo da cadeia.

Respostas detalhadas agrupam dados por serviço e incluem `PacienteResponse`, itens de medicamentos com `MedicamentoSummaryResponse`, acompanhamento, histórico e progresso. Resumos não carregam a árvore completa. Todas as respostas paginadas preservam o formato Spring `Page`, compatível com `ApiPage` Angular.

Edição mantém IDs dos medicamentos já existentes, atualiza itens presentes, cria novos e remove apenas itens retirados. Serviços podem ser editados sem reabrir o acompanhamento. Intervalo e quantidade só podem mudar no atendimento inicial antes de existir retorno, porque depois alterariam a sequência já realizada; essa é uma proteção de integridade da cadeia, não uma restrição baseada apenas no status.

## Consultas e eficiência

Listagem usa projection ou query de resumo com paciente e progresso necessário, sem carregar medicamentos ou histórico por linha. Detalhe usa entity graph/fetch join controlado para paciente, medicamentos e acompanhamento. O histórico é carregado em consulta ordenada pela posição.

A busca avançada combina critérios no servidor. Medicamento e lote são aplicados no mesmo alias de `MedicamentoAtendimento`, eliminando falsos positivos com medicamentos/lotes de itens diferentes. A consulta de interações existente continua em lote, sem uma chamada por medicamento e comorbidade.

## Integração Angular

`ServicoFarmaceuticoService` HTTP substituirá a store transitória, mantendo os tipos públicos atuais quando compatíveis e concentrando conversões request/response. O formulário será reutilizado para criação, edição e retorno; a rota de edição será `atendimentos/:id/editar`.

Na continuação, a listagem chama o endpoint de contexto antes da navegação. O formulário recebe paciente e progresso, oculta acompanhamento e só faz `POST /retornos` ao salvar. O submit e encerramento bloqueiam double submit e restauram a UI em erro.

Listagem e busca avançada solicitam somente a página atual, reagem a filtros/tamanho e mantêm 10, 20, 50 e 100. Visualização busca detalhe por UUID. O PDF usa o detalhe já carregado; a geração com `html2pdf.js` não muda. Máscaras, CEP e warnings de sinais vitais/interações permanecem no frontend.

Após a integração, `TemporaryPharmaceuticalServiceStore`, seu teste e a persistência local associada serão removidos. `TemporaryClinicalRecordsStore` permanece intocado se ainda for usado por módulos fora deste escopo.

## Testes e erros

Bean Validation cobre referências, datas, strings obrigatórias dos itens e positivos de acompanhamento. O service cobre integridade, transações, sincronização, status efetivo com `Clock` fixo, encerramento e concorrência. Repository tests usam H2/datasets. Controller tests cobrem JWT, 400, 404 e 409. `ServicoFarmaceuticoResourceITTest` cobre criação, listagem, detalhe, acompanhamento de três retornos e expiração sem espera real.

Não são adicionados diagnósticos, regras clínicas bloqueantes, estoque, exclusão física ou snapshot independente do paciente.
