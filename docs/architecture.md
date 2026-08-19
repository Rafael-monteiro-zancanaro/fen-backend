# Arquitetura do Backend

## Objetivo

Este documento descreve a arquitetura base esperada para o backend da aplicação da Farmácia Ensino da UEM.

O backend será desenvolvido em Java/Spring Boot e integrado gradualmente ao frontend Angular já existente em:

```text
../fen-frontend
```

A arquitetura deve suportar evolução incremental, permitindo substituir uma funcionalidade local do frontend por vez.

---

# Visão geral

Fluxo principal:

```text
Angular
    ↓ HTTP
Controller
    ↓
Service
    ↓
Repository
    ↓
Database
```

Com DTOs nas fronteiras:

```text
Angular
    ↓
Request DTO
    ↓
Controller
    ↓
Service
    ↓
Domain / Entity
    ↓
Repository
    ↓
Database

Database
    ↓
Repository
    ↓
Service
    ↓
Response DTO
    ↓
Controller
    ↓
Angular
```

---

# Camadas

## Controller

Responsabilidade:

- definir endpoints;
- receber parâmetros;
- receber requests;
- acionar Bean Validation;
- delegar ao Service;
- devolver respostas HTTP.

Controller não deve concentrar regra de negócio.

Exemplo conceitual:

```java
@RestController
@RequestMapping("/pacientes")
class PacienteController {
    private final PacienteService service;
}
```

---

## Service

Responsabilidade:

- regras de negócio;
- coordenação de operações;
- validações dependentes de persistência;
- transações;
- conversão/orquestração necessária entre domínio e contratos;
- operações especiais.

Exemplo conceitual:

```java
@Service
class PacienteService {
    private final PacienteRepository repository;
}
```

---

## Repository

Responsabilidade:

- persistência;
- consultas;
- filtros;
- operações de banco.

Exemplo conceitual:

```java
interface PacienteRepository extends JpaRepository<Paciente, Long> {
}
```

Queries específicas devem existir quando representarem necessidade real da aplicação.

---

## Domain / Entity

Entidades representam conceitos persistentes do domínio.

Uma interface TypeScript não é automaticamente uma entidade Java.

Antes de criar uma entity, classifique o conceito.

Possibilidades:

```text
Entity
Value Object
Enum
Request DTO
Response DTO
Search Criteria
Projection
Frontend-only model
```

---

## DTO

DTOs definem os contratos de entrada e saída da API.

Evite expor Entity diretamente.

Tipos comuns:

```text
CreateRequest
UpdateRequest
Response
SummaryResponse
SearchCriteria
```

Não crie variantes sem necessidade.

---

# Estrutura de pacotes

A estrutura deve seguir o padrão já existente no projeto.

Se o backend ainda não possuir convenção definida, prefira organização por domínio/feature, mantendo as camadas próximas.

Exemplo:

```text
br...fen/
├── paciente/
│   ├── Paciente.java
│   ├── PacienteController.java
│   ├── PacienteService.java
│   ├── PacienteRepository.java
│   └── dto/
│       ├── PacienteCreateRequest.java
│       ├── PacienteUpdateRequest.java
│       └── PacienteResponse.java
│
├── medicamento/
│   └── ...
│
├── comorbidade/
│   └── ...
│
└── atendimento/
    └── ...
```

Evite uma estrutura global gigantesca como:

```text
controller/
service/
repository/
entity/
dto/
```

se isso fizer módulos não relacionados ficarem excessivamente espalhados.

Se o projeto já tiver escolhido essa estrutura, preserve-a em vez de reorganizar sem solicitação.

---

# Domínios principais

O levantamento definitivo deve ser feito a partir do frontend e dos requisitos, mas os conceitos esperados incluem:

```text
Usuario / Funcionario
Farmaceutico
Estagiario
Role

Paciente
Endereco
Comorbidade

Medicamento
Interacao

ServicoFarmaceutico
MedicamentoAtendimento
Acompanhamento

SolicitacaoRecuperacaoSenha
```

Os nomes reais devem seguir a nomenclatura adotada pelo código.

---

# Relacionamentos importantes

## Paciente e atendimento

Conceitualmente:

```text
Paciente 1 ---- N ServicoFarmaceutico
```

Um atendimento pertence a um paciente.

---

## Paciente e comorbidade

Conceitualmente:

```text
Paciente N ---- N Comorbidade
```

A modelagem física deve ser decidida com base nas necessidades de persistência e nos requisitos.

---

## Medicamento e comorbidade

A interação representa a relação relevante entre medicamento e comorbidade.

Conceitualmente:

```text
Medicamento
    ↓
Interacao
    ↓
Comorbidade
```

Se `Interacao` possuir atributos próprios, deve ser modelada como entidade associativa em vez de um simples `ManyToMany`.

---

## Atendimento e medicamentos

Um atendimento pode registrar múltiplos medicamentos.

Como o vínculo possui informações próprias, como:

- lote;
- validade;
- posologia;
- eventualmente concentração/via conforme o modelo;

o relacionamento deve provavelmente ser representado por uma entidade própria, por exemplo conceitualmente:

```text
ServicoFarmaceutico
    ↓ 1:N
MedicamentoAtendimento
    ↓ N:1
Medicamento
```

O nome definitivo deve vir do modelo real.

---

## Acompanhamento e sequência de atendimentos

O fluxo de retorno não deve ser representado apenas como alteração do atendimento original.

Conceitualmente:

```text
Acompanhamento
├── Atendimento inicial
├── Retorno 1
├── Retorno 2
└── Retorno N
```

Cada retorno é um atendimento próprio.

O backend deve garantir a integridade da sequência.

---

# Identificadores

## ID técnico

Entidades persistentes podem possuir identificador técnico:

```text
id: Long
```

## Código de negócio do atendimento

Serviço Farmacêutico possui também código de negócio exibido ao usuário.

```text
id
→ identificador técnico

codigo
→ identificador sequencial pesquisável
```

Não use o código de negócio como substituto automático da chave primária.

A geração deve ser segura para concorrência.

---

# Paginação

Listagens de entidades devem ser preparadas para paginação server-side.

O frontend trabalha com tamanhos como:

```text
10
20
50
100
```

O backend não deve carregar toda a tabela para depois paginar em memória.

Utilize os recursos de paginação do Spring Data quando apropriado.

---

# Busca

Buscas simples podem utilizar métodos específicos de Repository.

Buscas complexas devem ser modeladas de maneira sustentável.

A busca avançada de atendimentos pode combinar:

- CPF;
- medicamento;
- lote;
- data;
- futuramente código.

Evite criar uma combinação explosiva de métodos:

```text
findByCpf
findByCpfAndMedicamento
findByCpfAndMedicamentoAndLote
...
```

quando os filtros forem opcionais e combináveis.

Considere Specifications, Criteria API ou outra abordagem equivalente quando a complexidade justificar.

---

# Rastreabilidade de medicamento/lote

Ao buscar por medicamento e lote, os dois filtros devem corresponder ao **mesmo registro de medicamento utilizado no atendimento**.

Exemplo:

```text
Atendimento:
- Dipirona / lote A
- Ibuprofeno / lote B
```

A busca:

```text
Dipirona + lote B
```

não deve encontrar esse atendimento.

Essa regra deve ser preservada na consulta server-side.

---

# Acompanhamentos

O backend deve garantir regras como:

- quantidade de retornos;
- intervalo;
- número do próximo retorno;
- vínculo com o acompanhamento;
- último retorno;
- conclusão;
- expiração;
- encerramento;
- prevenção de duplicidade.

O frontend pode esconder/desabilitar ações, mas a API deve impedir operações inválidas.

---

# Estados

Status devem ser representados de forma consistente, preferencialmente por enums quando forem conjuntos fechados.

Exemplos conceituais já presentes no fluxo:

```text
AGUARDANDO_RETORNO
EXPIRADO
CONCLUIDO
```

Utilize os nomes reais definidos pelo domínio.

Evite strings livres espalhadas pelo código.

---

# Transações

Casos que provavelmente precisam de transação:

- criação de atendimento com dependências;
- prosseguimento de atendimento;
- atualização do acompanhamento;
- encerramento;
- associação de relacionamentos;
- aprovação de recuperação de senha.

Mantenha a fronteira transacional no Service.

---

# Autenticação e autorização

Roles previstas:

```text
ADMIN
FARMACEUTICO
ESTAGIARIO
```

A estratégia de autenticação será definida conforme requisitos e configuração futura.

Independentemente da estratégia:

- backend deve validar autorização;
- frontend não é fonte de segurança;
- endpoints administrativos devem ser protegidos;
- regras não devem depender apenas de menus ocultos.

---

# Recuperação de senha

Fluxo conceitual:

```text
Usuário solicita alteração
        ↓
Solicitação pendente
        ↓
ADMIN aprova ou rejeita
```

O backend deve garantir que:

- a senha não seja exposta ao administrador;
- a alteração efetiva ocorra somente após aprovação;
- armazenamento utilize hashing adequado;
- solicitação rejeitada não altere credenciais.

A modelagem definitiva deve considerar cuidadosamente a segurança desse fluxo antes da implementação.

---

# Validação

Divisão recomendada:

```text
Bean Validation
→ formato/estrutura do request

Service
→ regra dependente de banco/domínio

Database
→ constraints de integridade
```

Exemplo de CPF:

```text
Frontend
→ máscara e feedback imediato

Backend DTO
→ formato

Service/Database
→ unicidade
```

---

# Tratamento de erros

A API deve retornar erros previsíveis.

Considere uma estrutura consistente para erros com informações como:

```text
timestamp
status
code
message
fieldErrors
```

ou outra estrutura adotada pelo projeto.

Não exponha stack traces ou detalhes internos.

---

# Concorrência

Regras que dependem de estado atual precisam considerar concorrência.

Exemplos:

- geração do código sequencial;
- prosseguir acompanhamento;
- aprovar uma solicitação;
- impedir duplicidade;
- definir responsável técnico, caso existam restrições.

Não dependa exclusivamente de:

```text
buscar
→ verificar
→ salvar
```

sem considerar que outra requisição pode alterar o estado entre essas operações.

Use constraints, locking ou outra estratégia quando realmente necessário.

---

# Persistência

O backend será a fonte de verdade.

Não reproduza no backend estruturas de armazenamento em memória que existam atualmente no frontend, exceto em testes.

O objetivo da integração é substituir essas responsabilidades por persistência real.

---

# Migrations

Se Flyway, Liquibase ou ferramenta equivalente estiver presente, utilize-a.

Mudanças estruturais devem ser reproduzíveis.

Evite depender de `ddl-auto=create` como estratégia de evolução do schema.

---

# Integração incremental

Não implemente todas as entidades e endpoints em uma única etapa.

Recomendação:

```text
1. selecionar um domínio
2. analisar frontend correspondente
3. identificar contratos
4. implementar backend
5. testar
6. adaptar Angular Service
7. testar ponta a ponta
8. remover apenas a implementação local substituída
```

Isso mantém o frontend funcional durante a migração.

---

# Ordem inicial sugerida

A ordem definitiva deve ser ajustada após o levantamento, mas uma sequência plausível é:

```text
1. Usuários / Funcionários / Roles
2. Medicamentos
3. Comorbidades
4. Interações
5. Pacientes
6. Serviços Farmacêuticos
7. Acompanhamentos
8. Busca avançada
9. Recuperação de senha
```

Dependências reais do código têm prioridade sobre essa sugestão.

---

# Testes

Para cada módulo, considere:

## Repository

- queries específicas;
- constraints relevantes;
- filtros.

## Service

- regra de negócio;
- cenários inválidos;
- transações;
- estados.

## Controller

- contrato HTTP;
- validação;
- autorização;
- status codes.

Fluxos críticos devem possuir cobertura adequada antes da integração com o frontend.

---

# O que não fazer

Evite:

- entidade JPA exposta diretamente;
- Controller chamando Repository;
- regra de negócio no Controller;
- regra de negócio em Repository;
- duplicação de DTOs sem necessidade;
- uma Entity para cada interface TypeScript;
- endpoints derivados mecanicamente dos nomes de métodos Angular;
- nomes `Mock`, `Temporary`, `Temp` ou `Fake` como arquitetura definitiva;
- integração de todos os módulos em um único commit/tarefa;
- remoção prematura da implementação local do frontend.

---

# Princípio arquitetural

A integração deve converter gradualmente:

```text
simulação de comportamento no frontend
```

em:

```text
contrato HTTP
+
regra de negócio no backend
+
persistência real
```

sem exigir que a interface Angular seja reconstruída.

O backend deve ser modelado em torno do domínio e das garantias necessárias, e não como uma reprodução mecânica das estruturas internas do frontend.
