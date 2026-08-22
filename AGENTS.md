# AGENTS.md

## Objetivo do projeto

Este projeto (`fen`) é o backend da aplicação desenvolvida para o TCC relacionado à **Farmácia Ensino da Universidade Estadual de Maringá (FEN/UEM)**.

O backend será responsável por disponibilizar a API utilizada pelo frontend Angular localizado no projeto irmão:

```text
../fen-frontend
```

O frontend já possui grande parte das funcionalidades e atualmente contém implementações locais que serão gradualmente substituídas por integração HTTP com este backend.

---

## Stack

Stack principal esperada:

- Java;
- Spring Boot;
- Spring Web;
- Spring Data JPA;
- Bean Validation;
- banco de dados relacional;
- testes automatizados.

Antes de adicionar uma dependência, verifique as dependências já existentes no projeto.

---

## Documentação obrigatória

Consulte, quando aplicável:

```text
`docs/architecture.md`
`docs/backend-infrastructure.md`
```

Além disso, durante tarefas de integração, consulte a documentação disponível no frontend:

```text
../fen-frontend/AGENTS.md
../fen-frontend/docs/
```

Se existir um mapa de integração gerado a partir do frontend, trate-o como referência de levantamento, mas valide as informações contra o código atual.

---

## Regra principal

O frontend existente é uma **fonte de requisitos de integração**, não um molde automático da arquitetura interna do backend.

Ao implementar uma funcionalidade:

1. identifique o comportamento esperado pelo frontend;
2. identifique os modelos e operações envolvidas;
3. modele o domínio adequadamente no backend;
4. defina DTOs;
5. implemente regras no Service;
6. implemente persistência no Repository;
7. exponha o contrato necessário no Controller;
8. escreva testes;
9. somente depois integre o frontend.

---

# Arquitetura

Siga, salvo motivo concreto em contrário, a separação:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Domain / Entity
```

Utilize DTOs nas fronteiras HTTP.

Estrutura conceitual:

```text
HTTP Request
    ↓
Controller
    ↓
Request DTO
    ↓
Service
    ↓
Domain / Entity
    ↓
Repository
    ↓
Database

Database / Domain
    ↓
Service
    ↓
Response DTO
    ↓
Controller
    ↓
HTTP Response
```

---

## Controllers

Controllers devem ser responsáveis principalmente por:

- mapear endpoints;
- receber parâmetros;
- receber DTOs;
- acionar validação de entrada;
- delegar para Services;
- transformar o resultado na resposta HTTP adequada.

Evite colocar regras de negócio em Controllers.

Não acesse Repositories diretamente a partir de Controllers.

---

## Services

Services concentram regras de aplicação e negócio.

São responsáveis, quando aplicável, por:

- validações que dependem de estado persistido;
- coordenação entre repositories;
- criação e atualização de entidades;
- transações;
- regras de status;
- regras de acompanhamento;
- regras de autorização que não sejam exclusivamente responsabilidade da infraestrutura de segurança;
- operações especiais além de CRUD.

Não transforme o Service em um simples proxy para o Repository quando existir regra relevante.

---

## Repositories

Repositories devem ser responsáveis por persistência e consultas.

Utilize Spring Data JPA quando apropriado.

Repositories podem conter:

- métodos derivados;
- JPQL;
- queries específicas;
- Specifications ou outra estratégia de consulta, quando justificadas.

Não coloque regras de negócio no Repository.

---

## Domain / Entities

Entidades devem representar o modelo persistente e suas relações.

Não transforme automaticamente cada interface TypeScript do frontend em uma entidade JPA.

Antes de criar uma classe Java, classifique o conceito:

- Entity;
- Value Object;
- Request DTO;
- Response DTO;
- Search Criteria;
- Projection;
- Enum;
- objeto exclusivo do frontend.

Somente conceitos persistentes devem se tornar entidades por padrão.

---

## DTOs

Não exponha entidades JPA diretamente como contrato HTTP.

Utilize DTOs para:

- criação;
- atualização;
- resposta;
- consultas específicas;
- busca avançada;
- paginação, quando necessário.

Evite criar DTOs idênticos sem finalidade clara.

Quando create e update possuírem exatamente os mesmos campos e regras, avalie reutilização. Quando as regras forem diferentes, mantenha contratos separados.

---

## Mapeamento

Mantenha o mapeamento entre entidades e DTOs explícito e previsível.

Não espalhe conversões por Controllers.

Prefira uma estratégia consistente no projeto, seja:

- métodos de fábrica;
- mapper dedicado;
- converter;
- outra abstração já adotada.

Evite adicionar uma biblioteca de mapeamento apenas por conveniência se o projeto não necessitar dela.

---

# Integração com o frontend

O frontend Angular está em:

```text
../fen-frontend
```

Em tarefas de integração, analise os Angular Services correspondentes antes de definir o contrato.

Procure preservar, quando razoável, a interface pública já utilizada pelos Components.

Exemplo conceitual:

```text
ANTES

MedicamentoComponent
    ↓
MedicamentoService
    ↓
dados locais

DEPOIS

MedicamentoComponent
    ↓
MedicamentoService
    ↓
HttpClient
    ↓
MedicamentoController
```

O objetivo é reduzir alterações desnecessárias na camada de apresentação.

---

## Não copiar models TypeScript cegamente

Um model TypeScript pode representar:

- entidade;
- DTO;
- view model;
- estado de formulário;
- resultado agregado;
- critérios de busca;
- dados derivados para apresentação.

Analise seu propósito antes de criar o equivalente Java.

Exemplo:

```text
AtendimentoViewModel
```

não deve se tornar automaticamente:

```java
@Entity
class AtendimentoViewModel
```

---

# Responsabilidades que devem migrar

Quando encontradas atualmente no frontend, responsabilidades de integridade devem ser implementadas no backend.

Exemplos:

- persistência;
- geração de IDs;
- geração sequencial do código do atendimento;
- unicidade de CPF;
- paginação server-side;
- busca avançada;
- validação definitiva de roles;
- expiração de acompanhamento;
- determinação de status;
- prevenção de retornos duplicados;
- consistência da sequência de atendimentos;
- aprovação de recuperação de senha;
- alteração segura de credenciais.

O frontend pode duplicar algumas validações para UX, mas o backend continua sendo a fonte de verdade.

---

# Responsabilidades client-side

Não mova para o backend sem necessidade funcionalidades puramente de interface, como:

- máscara visual de CPF;
- máscara de telefone;
- máscara de CEP;
- layout;
- estados de componentes;
- collapse de seções;
- previews;
- geração client-side da via em PDF.

---

# Domínios principais

Ao analisar ou implementar funcionalidades, considere os principais conceitos já presentes no frontend, quando confirmados pelo código:

- Usuário;
- Funcionário;
- Farmacêutico;
- Estagiário;
- Role;
- Paciente;
- Endereço;
- Medicamento;
- Comorbidade;
- Interação;
- Serviço Farmacêutico / Atendimento;
- medicamentos utilizados no atendimento;
- acompanhamento;
- retornos;
- solicitação de recuperação de senha.

Utilize os nomes reais já adotados no backend e mantenha consistência com a terminologia do projeto.

---

# Pacientes

O backend deve ser a fonte de verdade para pacientes.

Regras e operações podem incluir, conforme os requisitos e frontend confirmarem:

- criação;
- atualização;
- busca por ID;
- busca por CPF;
- listagem paginada;
- associação a comorbidades.

CPF deve ser validado e protegido contra duplicidade no backend quando for tratado como identificador único.

Não dependa apenas da validação Angular.

---

# Medicamentos

O cadastro de medicamentos não representa estoque.

Não introduza sem requisito:

- inventário;
- quantidade;
- entrada/saída;
- compras;
- fornecedores;
- vendas.

Medicamentos existem como referência de domínio para atendimentos e interações.

---

# Comorbidades e interações

Preserve a distinção:

```text
Paciente
    ↓ possui
Comorbidade

Medicamento
    ↓ relaciona-se através de interação
Comorbidade
```

Evite hardcode de regras clínicas.

Interações devem ser determinadas pelos dados cadastrados.

---

# Serviços Farmacêuticos

Serviços Farmacêuticos / Atendimentos são centrais para o sistema.

O backend deverá garantir, conforme os requisitos:

- persistência;
- código de negócio;
- paciente associado;
- serviços realizados;
- medicamentos utilizados;
- status;
- acompanhamento;
- sequência de retornos;
- encerramento;
- busca;
- histórico.

Regras críticas não devem depender apenas do frontend.

---

# Código do atendimento

O código exibido do atendimento é diferente do identificador técnico.

Conceitualmente:

```text
id
→ identificador técnico

codigo
→ identificador de negócio sequencial
```

A geração sequencial deve ser garantida pelo backend/banco de dados de forma segura para concorrência.

Não dependa de `max(codigo) + 1` sem proteção adequada.

---

# Acompanhamentos

O backend deve ser a fonte de verdade para a sequência do acompanhamento.

Quando o domínio confirmar esse fluxo, deve garantir:

- quantidade prevista de retornos;
- intervalo;
- retorno atual;
- vínculo entre atendimentos;
- impossibilidade de criar retorno além do permitido;
- prevenção de duplicidade;
- estado aguardando retorno;
- estado expirado;
- conclusão.

Um retorno deve ser tratado como um novo atendimento vinculado ao acompanhamento, e não como simples sobrescrita do atendimento anterior, se essa for a estrutura confirmada pelos requisitos.

---

# Busca avançada

Buscas capazes de percorrer grande volume de atendimentos devem ser server-side.

Evite exigir que o frontend carregue toda a base para depois filtrar.

Critérios podem incluir, conforme contrato confirmado:

- CPF;
- medicamento;
- lote;
- data;
- código.

Quando medicamento e lote forem combinados, preserve a correspondência do mesmo item de medicamento utilizado no atendimento.

---

# Paginação

Listagens que podem crescer devem ser paginadas no backend.

Não invente um modelo de paginação incompatível com o frontend sem necessidade.

Analise primeiro a abstração atual do Angular.

A resposta deve conter informações suficientes para:

- conteúdo;
- página atual;
- tamanho;
- total de elementos;
- total de páginas,

ou equivalente coerente com o contrato adotado.

---

# Autenticação e autorização

Roles previstas:

```text
ADMIN
FARMACEUTICO
ESTAGIARIO
```

Não considere ocultação de botão no frontend como autorização real.

O backend deve validar acesso às operações protegidas.

Quando a infraestrutura de segurança for implementada, centralize a autenticação e evite verificações manuais de role espalhadas pelos Controllers.

Não invente JWT ou outra estratégia antes de analisar os requisitos e a configuração do projeto.

---

# Recuperação de senha

A alteração real de senha deve ocorrer exclusivamente no backend.

Nunca retorne a nova senha solicitada para telas administrativas.

O administrador autoriza/rejeita a solicitação; não precisa conhecer a credencial escolhida.

Persistência, hashing e alteração de senha devem seguir práticas seguras.

---

# Validação

Utilize Bean Validation para validações estruturais de entrada quando apropriado.

Exemplos:

- obrigatório;
- formato;
- tamanho;
- valores positivos.

Validações dependentes de persistência ou regras de negócio devem permanecer no Service.

Não confie apenas em validações client-side.

---

# Exceções

Utilize uma estratégia consistente de tratamento de exceções.

Prefira um handler global para transformar exceções da aplicação em respostas HTTP previsíveis.

Não espalhe blocos `try/catch` repetitivos em Controllers sem necessidade.

Diferencie adequadamente situações como:

- recurso não encontrado;
- conflito;
- validação;
- acesso negado;
- erro interno.

---

# Transações

Use transações nos limites adequados do Service.

Operações que alteram múltiplas entidades ou relações devem ser atômicas quando necessário.

Exemplos potenciais:

- criar atendimento e relacionamentos;
- prosseguir acompanhamento;
- encerrar atendimento;
- aprovar recuperação de senha.

---

# Testes

Ao implementar um módulo:

1. teste regras do Service;
2. teste consultas importantes de Repository;
3. teste contratos relevantes de Controller;
4. teste cenários de erro;
5. teste regras críticas de integridade.

Não escreva testes apenas de caminho feliz para regras importantes.

---

# Banco e migrations

Se o projeto utilizar ferramenta de migration, preserve-a.

Não dependa de criação automática destrutiva de schema como estratégia definitiva.

Alterações de schema devem ser reproduzíveis.

---

# Nomenclatura

Não utilize prefixos/sufixos transitórios como arquitetura futura:

```text
Mock
Temporary
Temp
Fake
```

Prefira nomes de responsabilidade estáveis.

Se algum arquivo existente realmente possuir esses termos, documente a situação, mas não os propague automaticamente.

---

# Escopo das alterações

Faça alterações pequenas e orientadas ao módulo atual.

Evite:

- implementar vários domínios não solicitados;
- refatorar áreas não relacionadas;
- criar frameworks internos;
- generalizações prematuras;
- abstrações sem uso concreto.

---

# Ordem de trabalho recomendada

Para cada módulo:

1. analisar frontend relacionado;
2. identificar modelos e operações esperados;
3. modelar domínio no backend;
4. criar migrations/schema quando aplicável;
5. criar Repository;
6. criar Service;
7. criar DTOs/mapeamento;
8. criar Controller;
9. criar testes;
10. validar API;
11. integrar Angular Service;
12. validar fluxo ponta a ponta;
13. remover somente a infraestrutura local que foi substituída.

---

# Princípio final

O backend não existe apenas para replicar métodos do frontend.

Ele deve transformar os fluxos já existentes em uma API consistente e garantir:

- persistência;
- integridade;
- segurança;
- regras de negócio;
- concorrência;
- transações;
- consultas eficientes.

Use o frontend para descobrir **o que precisa ser suportado**.

Use a arquitetura do backend para decidir **como isso será garantido**.
