# Integração de Funcionários — Design

## Objetivo

Substituir a listagem, visualização e alteração de responsabilidade técnica de funcionários, hoje mantidas no frontend em memória, por dados persistidos e protegidos no backend.

## Decisão de domínio

O modelo existente será preservado. `Usuario` continua responsável por autenticação (`email`, `passwordHash`, `role` e `situacao`), e `Funcionario` continua como o perfil profissional, em associação obrigatória um-para-um com `Usuario`. `Funcionario` já possui UUID, campos de auditoria e Envers.

Não serão criadas entidades `Farmaceutico` e `Estagiario`. Os dois perfis são representados pelos campos profissionais já presentes em `Funcionario`, interpretados pela role de `Usuario`:

- `FARMACEUTICO`: `crf` e `responsavelTecnico`;
- `ESTAGIARIO`: `tipoEstagio`, `supervisor`, `inicioVigencia` e `fimVigencia`.

O supervisor permanece uma referência a `Funcionario`, pois é assim que o cadastro atual já o modela. `ADMIN` continua sendo uma role de sistema. A compatibilidade de leitura com cadastros ADMIN existentes que possuam dados profissionais será preservada, sem criar um terceiro perfil profissional.

## Persistência e regras

As tabelas `usuario`, `funcionario`, `aud.usuario_aud` e `aud.funcionario_aud` já existem na migration `001-create-usuario-funcionario.yaml`; elas contêm as constraints, foreign keys, UUIDs e auditoria necessários. A tarefa acrescenta consultas e contratos, não uma estrutura física duplicada.

CPF continua normalizado para somente dígitos por `Funcionario` e protegido por `uk_funcionario_cpf`. E-mail e role são obtidos exclusivamente de `Usuario`.

O service validará o perfil antes de alterar `responsavelTecnico`; a operação será recusada para estagiário ou funcionário sem CRF. ADMIN pode manter perfil farmacêutico e, quando possuir CRF, também pode ser marcado como responsável técnico. Não haverá unicidade global de responsável técnico: múltiplos farmacêuticos ou administradores elegíveis poderão estar marcados.

## API

Os endpoints ficarão em `/api/admin/funcionarios` e usarão `@PreAuthorize("hasRole('ADMIN')")`:

- `GET /api/admin/funcionarios?query=&page=0&size=10`: página Spring (`content`, `number`, `size`, `totalElements`, `totalPages`) de resumos;
- `GET /api/admin/funcionarios/{id}`: detalhes pelo UUID de `Funcionario`;
- `PATCH /api/admin/funcionarios/{id}/responsavel-tecnico`: request contendo somente o booleano `responsavelTecnico`.

O filtro `query` reproduz a busca existente na tela: nome, e-mail e role. A consulta usa carregamento explícito de `Usuario` e `supervisor` para evitar N+1. A resposta detalhada traz CPF, nascimento, situação, dados farmacêuticos ou dados de estágio, com o supervisor resumido.

## Frontend

Um `FuncionarioService` Angular substituirá `TemporaryPharmacyEmployeeStore` e preservará um modelo discriminado por role com UUID em `string`. A página administrativa carregará a página remota na alteração de busca, página e tamanho; a busca terá o mesmo campo e a paginação manterá 10, 20, 50 e 100.

A visualização buscará o funcionário pelo UUID da rota. A confirmação atual para responsabilidade técnica será mantida. A tela aguardará o `PATCH`; somente a resposta bem-sucedida substituirá o estado mostrado. Erros deixam o valor anterior visível e apresentam feedback; o botão permanece desabilitado enquanto a requisição estiver em andamento.

O `adminOnlyGuard`, navegação e interceptor de JWT existentes serão preservados. A autorização real ficará no backend.

## Testes

Serão adicionados testes de repositório H2 para paginação, filtro e associações; testes unitários de service para consulta e alterações válidas/inválidas; testes de controller para contrato e `@PreAuthorize`; e `FuncionarioResourceITTest` para o fluxo autenticado ADMIN e as recusas para FARMACEUTICO, ESTAGIARIO e anônimo.

Os testes Angular cobrirão serialização HTTP no service, listagem remota, visualização, carregamento/erro e atualização confirmada. O store temporário e seus testes serão removidos somente após esses fluxos estarem cobertos.
