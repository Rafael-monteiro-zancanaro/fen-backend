# Dashboard de Atendimentos

## Objetivo

Substituir o conteúdo estático de `inicio-page` por um dashboard histórico da Farmácia Ensino, alimentado por uma única resposta agregada autenticada do backend. O dashboard apresenta quatro indicadores navegáveis e dois gráficos, sem filtros de período.

## Levantamento confirmado

A aplicação possui `ServicoFarmaceutico` como atendimento, `Acompanhamento` como cadeia de retornos e o enum `StatusServicoFarmaceutico` com `AGUARDANDO_RETORNO`, `EXPIRADO` e `CONCLUIDO`. `EXPIRADO` não é persistido durante leituras: `StatusEfetivoResolver`, com `Clock`, o deriva de um atendimento aguardando cujo `proximoRetorno` é anterior a hoje.

Os serviços efetivamente identificáveis são:

- Cuidados farmacêuticos, pela presença de dados de cuidados;
- Aplicação de injetáveis, por ao menos um `MedicamentoAtendimento` do tipo `APLICACAO_INJETAVEIS`;
- Inaloterapia, por ao menos um `MedicamentoAtendimento` do tipo `INALOTERAPIA`;
- Serviços farmacêuticos, pela presença de `DadosServicosFarmaceuticos` ou medicamento do tipo `SERVICOS_FARMACEUTICOS`.

`assistenciaDomiciliar` é um atributo interno de Serviços farmacêuticos e não uma categoria independente. O frontend já centraliza labels em `attendance-labels.ts` e já possui `Chart.js` com `ng2-charts`; nenhuma dependência nova será instalada. O Design System efetivo fica em `fen-frontend/src/styles.css`.

## Contrato HTTP

Será criado `GET /api/dashboard`, protegido pela autenticação JWT existente e acessível a qualquer usuário autenticado. A resposta não expõe entidades JPA:

```json
{
  "indicators": {
    "awaitingReturn": 0,
    "returnsToday": 0,
    "totalAttendances": 0,
    "expired": 0
  },
  "serviceTypes": [
    { "type": "cuidados-farmaceuticos", "count": 0 }
  ],
  "statuses": [
    { "status": "CONCLUIDO", "count": 0 }
  ]
}
```

O frontend usará um `DashboardService` dedicado e uma única requisição para preencher cards e gráficos.

## Status efetivo e filtros consistentes

Em todas as consultas abaixo, `hoje` é `LocalDate.now(clock)`.

- **Concluído:** `statusPersistido == CONCLUIDO`.
- **Aguardando retorno:** somente o atendimento atual de um acompanhamento ativo, com `statusPersistido == AGUARDANDO_RETORNO` e `proximoRetorno >= hoje`.
- **Expirado:** somente o atendimento atual de um acompanhamento ativo, com `statusPersistido == AGUARDANDO_RETORNO` e `proximoRetorno < hoje`.
- **Reconsulta hoje:** somente o atendimento atual de um acompanhamento ativo, com `statusPersistido == AGUARDANDO_RETORNO` e `proximoRetorno == hoje`.

O backend continuará usando `StatusEfetivoResolver` para respostas de entidades. As consultas de listagem e agregação reproduzem sua condição relacional no banco para não carregar a coleção inteira em memória. A mesma definição será aplicada à listagem e ao dashboard, coberta por testes de compatibilidade entre o indicador e `totalElements`.

`GET /api/servicos-farmaceuticos` receberá `retornoHoje=false|true`. Quando verdadeiro, o critério de reconsulta hoje se soma à busca textual e impede combinação com um status incompatível. A URL do dashboard será `/atendimentos?retornoHoje=true`; os outros cards usarão `status=AGUARDANDO_RETORNO`, `status=EXPIRADO` ou nenhum status.

## Persistência e agregações

Nenhuma entidade ou migration é necessária. `ServicoFarmaceuticoRepository` receberá consultas JPQL agregadas para total, status efetivo, reconsultas de hoje e tipos de serviço. As categorias que dependem de `MedicamentoAtendimento` contam `distinct servicoFarmaceutico.id`, para que múltiplos medicamentos do mesmo atendimento contribuam apenas uma vez por tipo. Um atendimento pode contribuir para mais de uma categoria.

## Interface Angular

`InicioPage` trocará valores, seletor de período e séries mensais estáticas por signals de carregamento, sucesso, erro e dados. Os cards serão botões semânticos, com ícones Bootstrap já disponíveis e navegação por `Router` para a rota existente de atendimentos. A página usará `card`, `skeleton`, `alert`, `empty` e Tailwind apenas para grid e responsividade.

Os gráficos serão de barras, lado a lado em desktop e empilhados em telas menores. `ng2-charts` fornecerá tooltips padrão com rótulo e quantidade. Dados vazios exibem a estrutura do gráfico com estado vazio, sem canvas quebrado. Labels vêm de `ATTENDANCE_STATUS_LABELS` e `PHARMACEUTICAL_SERVICE_LABELS`.

`AtendimentosPage` passará a sincronizar termo, status e `retornoHoje` com query params, para suportar refresh, compartilhamento e voltar/avançar. O critério de hoje será visível por chip removível; os filtros rápidos existentes refletirão o status da URL.

## Testes

No backend, testes de repository validam as consultas agregadas e o filtro `retornoHoje` com H2/dataset. Testes de service usam `Clock.fixed` para ontem, hoje e amanhã, contagem de tipos e status efetivo. Controller e integração validam contrato, JWT e resposta agregada.

No frontend, testes de service validam o endpoint e tipos. Testes da página validam loading, erro, zero, labels, estado vazio, transformação dos dados e navegação. Os testes de rota cobrem os quatro cards e a listagem reconhece `status`/`retornoHoje` da URL.
