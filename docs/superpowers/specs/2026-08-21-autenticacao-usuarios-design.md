# Autenticação, usuários e aprovação de cadastro

## Objetivo

Implementar o primeiro fluxo integrado entre `fen-frontend` e `fen`: cadastro público de profissionais sujeito à aprovação administrativa, autenticação por e-mail e senha, autorização por role e sessão stateless com JWT válido por oito horas.

O backend será a fonte de verdade para identidade, situação do cadastro e autorização. O frontend manterá guards e controles visuais apenas para navegação e experiência do usuário.

## Escopo

Esta entrega inclui:

- persistência de usuários e funcionários;
- cadastro público de farmacêuticos e estagiários;
- aprovação ou rejeição administrativa do cadastro;
- login de usuários ativos;
- emissão e validação de JWT;
- restauração da sessão no Angular;
- interceptor HTTP e guards de autenticação/role;
- usuário administrador inicial criado por Liquibase;
- testes unitários, de persistência, de controller, de integração e frontend.

Não inclui recuperação ou alteração de senha, refresh token, sessão persistida no banco nem integração completa da administração de funcionários existente.

## Modelagem de domínio

### Usuario

`Usuario` representa identidade e credenciais. Possui:

- `id: UUID`;
- `email` normalizado em minúsculas e único;
- `passwordHash`, nunca exposto por DTO;
- `role: Role` persistida como texto (`ADMIN`, `FARMACEUTICO`, `ESTAGIARIO`);
- `situacao: SituacaoUsuario` persistida como texto (`PENDENTE`, `ATIVO`);
- `createdAt`, `updatedAt`, `createdBy`, `updatedBy` conforme a infraestrutura compartilhada de auditoria.

`Usuario` será auditado pelo Envers. Somente usuários `ATIVO` poderão autenticar. Cadastros públicos nunca aceitarão `ADMIN`.

### Funcionario

`Funcionario` representa o perfil profissional e possui associação 1:1 obrigatória com o usuário criado pelo cadastro público. Possui:

- `id: UUID`;
- `usuario`;
- nome completo;
- CPF;
- data de nascimento opcional, conforme o formulário atual;
- CRF e indicação de responsável técnico para farmacêuticos;
- tipo de estágio, início e fim da vigência para estagiários;
- supervisor persistente por UUID, associado a outro `Funcionario` cujo usuário esteja ativo e tenha role `FARMACEUTICO` ou `ADMIN`;
- campos compartilhados de auditoria.

Campos profissionais específicos serão mantidos nesta entidade, com nulabilidade física compatível com os dois perfis e invariantes validadas no service. Esta solução evita misturar credenciais com dados profissionais sem introduzir herança JPA e tabelas adicionais antes de o domínio exigir essa complexidade.

O administrador inicial não terá `Funcionario`, pois os requisitos fornecem apenas suas credenciais. Não serão inventados CPF, CRF ou outros dados profissionais.

### Aprovação e rejeição

O cadastro cria `Usuario(PENDENTE)` e `Funcionario` na mesma transação. A aprovação altera a situação para `ATIVO`. A rejeição remove ambos transacionalmente e libera e-mail e CPF para novo cadastro. Não haverá histórico funcional de rejeições; apenas as auditorias que existirem antes da exclusão conforme o comportamento do Envers.

## Contratos HTTP

### Operações públicas

#### `POST /api/auth/register`

Recebe um DTO específico com credenciais, role profissional e os campos de funcionário aplicáveis. A confirmação de senha permanece uma regra de formulário do frontend e não é enviada como campo persistente. O backend valida formato, obrigatoriedade, role permitida, consistência dos campos profissionais, supervisor e unicidade.

Cria o cadastro pendente e retorna `201 Created` com identificadores e situação, sem senha, hash ou token.

#### `POST /api/auth/login`

Recebe e-mail e senha. A validação passa pelo `AuthenticationManager`, `UserDetailsService` e `PasswordEncoder`. Credenciais inválidas, usuário inexistente e usuário pendente produzem resposta uniforme `401`, evitando enumeração de contas.

Retorna:

- token JWT;
- instante de expiração;
- usuário resumido com `id`, `email` e `role`.

#### `GET /api/public/supervisores`

Retorna somente `id` e nome dos funcionários elegíveis para supervisão. A exposição pública é deliberadamente mínima e existe para preservar o seletor do cadastro atual.

### Operações protegidas

#### `GET /api/auth/me`

Retorna o usuário resumido correspondente ao JWT válido. É usado para confirmar/restaurar a sessão e como endpoint protegido básico.

#### `GET /api/admin/usuarios/pendentes`

Exige role `ADMIN` e lista dados necessários à análise de cadastros pendentes, incluindo o perfil profissional, sem credenciais.

#### `POST /api/admin/usuarios/{id}/aprovar`

Exige role `ADMIN`, aceita apenas usuário pendente e o altera para ativo.

#### `DELETE /api/admin/usuarios/{id}/rejeitar`

Exige role `ADMIN`, aceita apenas usuário pendente, remove seu funcionário e usuário e retorna `204 No Content`.

## Segurança backend

Spring Security operará sem sessão de servidor. A configuração terá:

- `SecurityFilterChain` com negação por padrão e lista explícita de endpoints públicos;
- `AuthenticationManager` e provider baseado em DAO;
- `UserDetailsService` que carrega usuário por e-mail e considera sua situação;
- `BCryptPasswordEncoder`;
- serviço JWT responsável por emissão, leitura e validação;
- filtro `OncePerRequestFilter` que cria o contexto com authorities no formato `ROLE_<ROLE>`;
- handlers consistentes para `401` e `403`;
- CORS centralizado com origens configuráveis.

O JWT conterá somente subject igual ao UUID do usuário, role, emissão e expiração. A duração padrão será `8h`/`28.800s`, configurável. O secret não ficará no código: será obtido de `FEN_JWT_SECRET` ou propriedade equivalente. O profile de testes fornecerá secret próprio e expirações reduzidas quando necessário.

O auditor usará o e-mail do usuário autenticado. Ações públicas e carga inicial utilizarão o identificador de sistema já adotado.

## Persistência e Liquibase

Liquibase YAML criará:

- tabela `usuario`;
- unique constraint de e-mail;
- tabela `funcionario`;
- unique constraints e foreign keys necessárias, incluindo usuário 1:1 e supervisor;
- `aud.usuario_aud`;
- `aud.funcionario_aud`;
- registro ADMIN ativo com UUID fixo documentado, e-mail `admin@fen.br`, role `ADMIN` e hash BCrypt compatível com a senha inicial `admin123`.

Tabelas e colunas seguirão a convenção sem underscores, com exceção do sufixo `_aud`. Os campos de auditoria serão `createdat`, `updatedat`, `createdby` e `updatedby`; a abstração existente será ajustada para refletir os nomes exigidos pela documentação. As migrations deverão executar tanto em PostgreSQL quanto em H2 com modo PostgreSQL.

## Tratamento de erros

A API adotará uma resposta consistente, sem stack traces ou detalhes internos:

- `400` para Bean Validation e invariantes inválidas;
- `401` para credenciais inválidas, usuário não ativo e token ausente/inválido/expirado em recurso protegido;
- `403` para usuário autenticado sem role suficiente;
- `404` para identificador administrativo inexistente;
- `409` para e-mail, CPF ou outro identificador único já cadastrado.

Erros de login não diferenciarão e-mail, senha ou situação. Senhas, hashes e tokens completos não serão registrados em logs.

## Integração Angular

### Infraestrutura de sessão

Um `AuthService` será a única fonte do estado autenticado. Ele oferecerá operações de cadastro, login, logout, restauração e consulta do usuário atual. O acesso ao armazenamento browser-side será encapsulado em um serviço próprio ou internamente no `AuthService`, sem chamadas espalhadas em componentes.

O token e a resposta mínima do usuário serão persistidos para suportar reload. Na inicialização, o frontend verificará a expiração local do JWT, descartará tokens vencidos e confirmará tokens ainda válidos por meio de `/api/auth/me`.

Um interceptor funcional adicionará `Authorization: Bearer <token>` às chamadas apropriadas. Em `401`, removerá a sessão e redirecionará ao login quando a requisição exigir autenticação. Em `403`, manterá a sessão e redirecionará para uma rota segura com feedback conforme o padrão visual existente.

A URL da API virá da configuração de environment do Angular.

### Páginas e guards

- login passará a usar e-mail e senha reais, removerá o seletor temporário de role e apresentará falhas uniformes;
- cadastro será convertido para Reactive Forms, enviará o contrato real e exibirá confirmação de que aguarda aprovação;
- o seletor de supervisor consumirá o endpoint público e persistirá o UUID escolhido;
- será criada uma tela administrativa de cadastros pendentes, com ações de aprovar e rejeitar;
- um guard autenticado protegerá todas as rotas de aplicação, exceto login, cadastro e recuperação de senha;
- um guard de role protegerá rotas administrativas usando a role real;
- `TemporaryAccessControl` será removido;
- `TemporaryPharmacyEmployeeStore` permanecerá exclusivamente nas telas de funcionários ainda não integradas e deixará de influenciar autenticação/autorização;
- stores e telas de recuperação de senha permanecerão, pois sua integração está fora do escopo.

## Testes

### Backend

- `UsuarioRepositoryTest` e `FuncionarioRepositoryTest` com H2/datasets: persistência, consulta por e-mail, unicidade, associação e supervisão;
- services unitários: cadastro, normalização, BCrypt, duplicidade, validações por role, aprovação e rejeição;
- controllers: requests válidos/inválidos, status e DTOs, login válido/inválido e proteção administrativa;
- serviço JWT: token válido, assinatura inválida e expiração configurável sem espera real;
- `AuthenticationResourceITTest`: cadastro, aprovação, login, token, `/me`, `401` sem autenticação e `403` com role incorreta;
- execução da suíte completa e validação do contexto Liquibase/Envers.

### Frontend

- `AuthService`: login, persistência, restauração, expiração e logout;
- interceptor: inclusão de bearer token e comportamentos distintos de `401`/`403`;
- guards: usuário ausente, role permitida e role insuficiente;
- login e cadastro: payloads, sucesso, falha e pendência;
- tela administrativa: listagem, aprovação e rejeição;
- build e suíte completa do Angular.

## Sequência de implementação

1. Corrigir e validar a infraestrutura compartilhada de auditoria.
2. Implementar migrations e domínio de usuário/funcionário com testes de repository.
3. Implementar cadastro e aprovação com testes de service/controller.
4. Implementar Spring Security e JWT com testes unitários e integrados.
5. Validar integralmente o backend.
6. Implementar configuração HTTP e infraestrutura de autenticação Angular.
7. Integrar login e cadastro.
8. Criar a tela de aprovação e adaptar guards/rotas.
9. Remover somente a fonte temporária de autenticação substituída.
10. Executar testes e builds completos dos dois projetos.

## Decisões adiadas

Recuperação de senha continuará local e sem integração nesta tarefa. O modelo de usuário e as roles deixam espaço para o futuro endpoint administrativo, mas não serão criados solicitação, aprovação de senha, token de recuperação ou alteração de credencial agora.

Também ficam adiados refresh token, rotação de token, revogação persistente, histórico funcional de rejeições e integração completa das telas de gerenciamento de funcionários.
