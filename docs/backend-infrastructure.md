# Backend Infrastructure Guidelines

## Objetivo

Este documento define as convenções de infraestrutura, persistência, auditoria, autenticação, banco de dados e testes do backend da aplicação da Farmácia Ensino da UEM.

Estas regras complementam:

```text
AGENTS.md
docs/architecture.md
```

Ao implementar novas entidades, migrations, repositories, services, controllers, autenticação ou testes, consulte este documento antes de alterar o código.

As convenções aqui descritas devem ser tratadas como padrão do projeto.

---

# Arquitetura base

O backend segue a arquitetura clássica do Spring Boot:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Domain / Entity
    ↓
Database
```

## Controller

Responsável por:

- expor endpoints HTTP;
- receber parâmetros e DTOs;
- acionar validações de entrada;
- delegar operações para Services;
- converter resultados em respostas HTTP.

Controllers não devem:

- acessar Repositories diretamente;
- conter regras de negócio;
- executar consultas de persistência;
- concentrar transações.

---

## Service

Responsável por:

- regras de negócio;
- coordenação entre entidades e repositories;
- validações que dependam de estado persistido;
- transações;
- criação, atualização e exclusão de entidades;
- regras de autorização de negócio quando aplicável.

O Service representa a principal camada de aplicação.

---

## Repository

Responsável por:

- persistência;
- consultas;
- filtros;
- acesso ao banco de dados.

Utilize Spring Data JPA conforme os padrões existentes.

Repositories não devem conter regras de negócio.

---

# Classes de domínio

As classes persistentes do domínio devem seguir as convenções descritas nesta seção.

---

## Identificador sintético

Toda classe de domínio deve possuir identificador sintético do tipo:

```java
UUID
```

Não utilize identificadores sequenciais como chave primária das entidades de domínio, salvo exceção explicitamente documentada.

Conceitualmente:

```java
@Id
private UUID id;
```

A estratégia concreta de geração do UUID deve seguir o padrão já utilizado no projeto.

Antes de introduzir uma estratégia diferente, procure por entidades existentes e reutilize a mesma convenção.

---

# Auditoria

Toda classe de domínio deve possuir informações de auditoria.

Campos obrigatórios:

```text
createdAt
updatedAt
createdBy
updatedBy
```

Esses campos devem seguir a infraestrutura de auditoria existente no projeto.

Não implemente auditoria manualmente em cada entidade caso já exista uma abstração compartilhada.

Quando existir uma classe base ou listener de auditoria, reutilize-os.

---

## Hibernate Envers

As entidades de domínio devem ser auditadas utilizando:

```text
Hibernate Envers
```

As alterações relevantes nas entidades devem gerar histórico no schema de auditoria.

Ao criar uma nova entidade, verifique:

- se a entidade está corretamente configurada para auditoria;
- se os relacionamentos auditáveis estão consistentes;
- se a migration correspondente contempla a tabela de auditoria;
- se a entidade reutiliza a estrutura comum de auditoria do projeto.

Não considere uma entidade persistente concluída sem avaliar sua participação no Envers.

---

# Nomenclatura de tabelas

As tabelas das entidades de domínio devem utilizar o nome da classe em minúsculas e sem separação por `_`.

Exemplos:

```text
Paciente
→ paciente

ServicoFarmaceutico
→ servicofarmaceutico

SolicitacaoRecuperacaoSenha
→ solicitacaorecuperacaosenha

MedicamentoAtendimento
→ medicamentoatendimento
```

Evite:

```text
servico_farmaceutico
solicitacao_recuperacao_senha
medicamento_atendimento
```

A nomenclatura do banco deve seguir o padrão:

```text
nomedaclasse
```

tudo junto e em minúsculas.

---

# Nomenclatura de colunas

As colunas das tabelas de domínio devem seguir o mesmo princípio.

Utilize nomes em minúsculas e sem `_` para separar palavras.

Exemplos:

```text
createdAt
→ createdat

dataNascimento
→ datanascimento

responsavelTecnico
→ responsaveltecnico

viaAdministracao
→ viaadministracao
```

Não utilize, salvo necessidade específica:

```text
created_at
data_nascimento
responsavel_tecnico
via_administracao
```

Ao adicionar uma nova coluna, mantenha consistência entre:

- annotation JPA;
- Liquibase;
- schema PostgreSQL;
- schema H2 de testes.

---

# Schema de auditoria

As tabelas auditadas pelo Envers devem existir no schema:

```text
aud
```

Para cada tabela de domínio auditada, deve existir uma tabela correspondente de auditoria.

O padrão de nomenclatura é:

```text
nomedaclasse_aud
```

Exemplos:

```text
paciente
→ aud.paciente_aud

servicofarmaceutico
→ aud.servicofarmaceutico_aud

medicamento
→ aud.medicamento_aud
```

Apenas o sufixo:

```text
_aud
```

utiliza `_`.

O nome da entidade continua sendo escrito sem separação entre palavras.

---

# Colunas das tabelas de auditoria

As colunas que representam atributos da entidade nas tabelas de auditoria devem manter o mesmo nome utilizado na tabela de domínio.

Exemplo:

```text
paciente.datanascimento
→ aud.paciente_aud.datanascimento
```

Não crie uma convenção diferente para as colunas do schema `aud`.

Além das colunas da entidade, preserve as colunas técnicas exigidas pela infraestrutura do Envers.

---

# Relacionamentos

Ao criar relacionamentos JPA:

- defina explicitamente as cardinalidades;
- avalie o lado proprietário;
- evite `CascadeType.ALL` como escolha automática;
- evite `FetchType.EAGER` sem necessidade;
- analise impacto de serialização e consultas;
- preserve compatibilidade com auditoria Envers;
- crie as foreign keys por Liquibase.

Não modele relacionamentos apenas para reproduzir diretamente uma interface TypeScript.

A estrutura deve representar corretamente o domínio persistente.

---

# Autenticação

O backend deve possuir autenticação baseada em:

```text
JWT
```

O backend é a fonte de verdade para autenticação e autorização.

Não considere verificações de role realizadas apenas no frontend como mecanismo de segurança.

---

# Roles

O sistema possui inicialmente três roles:

```text
ADMIN
FARMACEUTICO
ESTAGIARIO
```

## ADMIN

Possui acesso administrativo completo, respeitando as regras específicas de cada funcionalidade.

## FARMACEUTICO

Possui acesso às funcionalidades relacionadas ao atendimento farmacêutico, exceto operações administrativas restritas.

## ESTAGIARIO

Possui inicialmente permissões funcionais semelhantes às de `FARMACEUTICO`, salvo regras específicas definidas posteriormente.

---

# Autorização

Endpoints protegidos devem validar autorização no backend.

Quando possível, centralize regras de segurança utilizando os mecanismos do Spring Security.

Evite espalhar verificações manuais como:

```java
if (usuario.getRole() == Role.ADMIN) {
    ...
}
```

em diversos Controllers.

Prefira mecanismos declarativos e centralizados quando aplicáveis.

---

# JWT

A implementação JWT deve manter responsabilidades separadas.

Conceitualmente:

```text
Credenciais
    ↓
Autenticação
    ↓
Token JWT
    ↓
Request autenticada
    ↓
Filtro / Security Context
    ↓
Controller
```

Não coloque lógica de geração ou parsing de JWT nos Controllers.

A estrutura concreta deve seguir o padrão de segurança adotado pelo projeto.

---

# Senhas

Senhas:

- nunca devem ser armazenadas em texto puro;
- nunca devem ser retornadas em DTOs;
- nunca devem aparecer em logs;
- devem utilizar algoritmo de hashing adequado;
- devem ser tratadas exclusivamente pelo backend para persistência.

Fluxos como recuperação de senha devem respeitar essas mesmas regras.

---

# Bancos de dados

O projeto utiliza dois bancos em contextos diferentes.

---

## PostgreSQL

O banco principal da aplicação é:

```text
PostgreSQL
```

Esse banco deve ser utilizado nos ambientes normais da aplicação.

A estrutura do schema deve ser controlada exclusivamente pelas migrations do projeto.

Não dependa de criação automática do Hibernate como mecanismo definitivo de evolução do schema.

---

## H2

O projeto utiliza:

```text
H2
```

exclusivamente para testes automatizados que precisem de persistência.

O H2 de testes deve permanecer isolado do PostgreSQL.

Não configure testes para depender do banco PostgreSQL local do desenvolvedor.

---

# Liquibase

A criação e alteração do banco deve ser controlada por:

```text
Liquibase
```

As migrations devem utilizar arquivos:

```text
.yml
```

ou:

```text
.yaml
```

conforme o padrão já existente no projeto.

Não introduza migrations SQL avulsas ou XML se o projeto padronizou Liquibase em YAML.

---

# Regras para migrations

Toda mudança persistente deve possuir migration correspondente, incluindo:

- criação de tabela;
- criação de tabela de auditoria;
- nova coluna;
- alteração de coluna;
- foreign key;
- índice;
- constraint;
- dados estruturais obrigatórios quando aplicável.

Não altere manualmente o PostgreSQL e considere a tarefa concluída.

O schema deve poder ser reconstruído exclusivamente pelas migrations.

---

# Ordem de criação

Ao criar uma nova entidade auditada, considere no Liquibase:

```text
1. tabela de domínio;
2. constraints;
3. foreign keys;
4. índices necessários;
5. tabela de auditoria no schema aud;
6. estrutura exigida pelo Envers.
```

Adapte a ordem conforme dependências reais.

---

# Compatibilidade PostgreSQL e H2

Toda migration utilizada nos testes deve ser compatível com a infraestrutura de H2 adotada pelo projeto ou possuir tratamento específico previsto pelo projeto.

Ao adicionar:

- tipos;
- constraints;
- funções;
- valores default;

verifique se os testes em H2 continuam funcionando.

Não utilize uma funcionalidade específica do PostgreSQL sem avaliar o impacto no ambiente de testes.

---

# Testes

Cada camada do backend deve possuir testes adequados e isolados.

A existência de testes de integração não substitui os testes das camadas.

---

# Repository Tests

Repositories devem possuir testes próprios.

Esses testes devem:

- utilizar o banco H2 exclusivo de testes;
- validar queries customizadas;
- validar relacionamentos relevantes;
- validar constraints quando aplicável;
- evitar dependência de PostgreSQL externo.

Os dados necessários para testes de Repository devem ser inseridos através de scripts localizados nos resources de teste.

Estrutura conceitual:

```text
src/test/resources/
```

Utilize o mecanismo já adotado no projeto para executar esses scripts.

Não crie dados manualmente em todos os testes se um dataset reutilizável for mais adequado.

---

# Dados de teste

Scripts de dados do H2 devem:

- utilizar valores determinísticos;
- evitar depender da ordem de execução de outros testes;
- possuir IDs conhecidos quando necessários para assertions;
- permanecer pequenos e focados no cenário testado.

Não utilize dados pessoais reais.

---

# Service Tests

Services devem possuir testes unitários próprios.

Sempre que possível:

- isole o Service;
- mocke suas dependências;
- teste regras de negócio;
- teste cenários de sucesso;
- teste cenários inválidos;
- teste recursos inexistentes;
- teste conflitos;
- teste mudanças de estado.

Não utilize o banco apenas para testar uma regra que pode ser validada isoladamente no Service.

---

# Controller Tests

Controllers devem possuir testes próprios.

Eles devem validar, conforme a infraestrutura adotada:

- endpoint;
- método HTTP;
- status code;
- validação de request;
- serialização;
- autorização;
- resposta;
- comportamento de erro.

Isole a camada Controller quando o objetivo do teste for apenas seu contrato HTTP.

---

# Testes de integração

Testes de integração podem ser adicionados quando fizerem sentido.

A nomenclatura obrigatória deve terminar em:

```text
ResourceITTest
```

Exemplo:

```text
ServicoFarmaceuticoResourceITTest
PacienteResourceITTest
MedicamentoResourceITTest
```

Não utilize outra convenção para testes de integração que cubram o fluxo HTTP completo, salvo padrão existente explicitamente diferente.

---

# Diferença entre testes

Utilize cada tipo de teste para sua responsabilidade.

```text
RepositoryTest
→ persistência e queries

ServiceTest
→ regra de negócio

ControllerTest
→ contrato HTTP da camada Controller

ResourceITTest
→ fluxo integrado entre múltiplas camadas
```

Evite repetir exatamente o mesmo cenário em todas as camadas sem propósito.

---

# Isolamento dos testes

Os testes devem ser independentes.

Não dependa:

- da execução de outro teste;
- da ordem da suíte;
- de dados persistidos por teste anterior;
- de PostgreSQL externo;
- do ambiente local do desenvolvedor.

---

# Nomenclatura de testes

Siga a nomenclatura já utilizada pelo projeto para testes unitários e de Repository/Controller.

Para testes de integração, preserve obrigatoriamente:

```text
<Resource>ResourceITTest
```

Exemplo:

```text
PacienteResourceITTest
```

---

# Fluxo esperado para uma nova entidade

Ao implementar uma nova entidade de domínio, verifique pelo menos:

```text
Domain Entity
    ↓
UUID
    ↓
Auditoria
    ↓
Envers
    ↓
Liquibase - tabela domínio
    ↓
Liquibase - tabela aud
    ↓
Repository
    ↓
RepositoryTest + dataset H2
    ↓
Service
    ↓
ServiceTest
    ↓
DTOs
    ↓
Controller
    ↓
ControllerTest
    ↓
ResourceITTest, quando aplicável
```

Não considere a implementação completa apenas porque o endpoint funciona manualmente.

---

# Checklist para nova classe de domínio

Antes de concluir uma entidade, confirme:

- [ ] possui UUID como identificador sintético;
- [ ] possui `createdAt`;
- [ ] possui `updatedAt`;
- [ ] possui `createdBy`;
- [ ] possui `updatedBy`;
- [ ] está integrada ao mecanismo de auditoria;
- [ ] está auditada pelo Envers;
- [ ] tabela segue `nomedaclasse`;
- [ ] colunas seguem nomenclatura sem `_`;
- [ ] tabela de auditoria existe em `aud`;
- [ ] tabela de auditoria segue `nomedaclasse_aud`;
- [ ] colunas auditadas mantêm a nomenclatura da tabela de domínio;
- [ ] migration Liquibase YAML existe;
- [ ] relacionamentos possuem foreign keys adequadas;
- [ ] Repository existe quando necessário;
- [ ] Repository possui testes quando possui persistência relevante;
- [ ] Service possui testes;
- [ ] Controller possui testes;
- [ ] ResourceITTest existe quando o fluxo justificar integração completa.

---

# Checklist para nova migration

Antes de concluir uma alteração de banco, confirme:

- [ ] migration está em Liquibase YAML;
- [ ] tabela/coluna segue nomenclatura do projeto;
- [ ] alteração funciona no PostgreSQL;
- [ ] testes continuam funcionando no H2;
- [ ] constraints foram adicionadas;
- [ ] foreign keys foram adicionadas;
- [ ] índices relevantes foram avaliados;
- [ ] tabela de auditoria foi atualizada quando necessário;
- [ ] Envers permanece compatível com o schema.

---

# Checklist de autenticação/autorização

Ao adicionar uma funcionalidade protegida:

- [ ] endpoint exige usuário autenticado quando necessário;
- [ ] role correta é validada no backend;
- [ ] frontend não é tratado como fonte de segurança;
- [ ] senha não aparece em logs ou DTOs;
- [ ] JWT é tratado pela infraestrutura de segurança;
- [ ] Controller não implementa parsing manual de token;
- [ ] testes cobrem acesso permitido e negado.

---

# Nomenclatura estável

Não utilize nomes arquiteturais baseados no caráter provisório de implementações anteriores.

Evite:

```text
Mock
Temporary
Temp
Fake
```

Classes do backend devem possuir nomes orientados à responsabilidade real.

Exemplos:

```text
PacienteService
PacienteRepository
ServicoFarmaceuticoService
JwtService
```

e não:

```text
TemporaryPacienteService
MockServicoFarmaceuticoRepository
FakeJwtService
```

---

# Prioridade das convenções

Em caso de conflito, considere:

1. requisitos funcionais;
2. regras explícitas deste documento;
3. arquitetura documentada do backend;
4. padrões já consolidados no código;
5. convenções genéricas do Spring Boot.

Se uma implementação existente divergir deste documento, não replique a divergência automaticamente em novas funcionalidades. Avalie se é uma exceção intencional ou dívida técnica.

---

# Princípio final

Uma funcionalidade backend não está completa apenas quando responde corretamente a uma requisição HTTP.

Ela deve integrar de forma consistente:

```text
Domain
+
Persistência
+
Auditoria
+
Segurança
+
Liquibase
+
Testes
```

As convenções deste documento devem ser aplicadas de forma uniforme conforme o backend for construído.
