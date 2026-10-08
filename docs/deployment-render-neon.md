# Deploy no Render com PostgreSQL Neon

Este documento prepara o backend `fen` para um deploy manual. Ele não cria o
serviço no Render, não cria banco no Neon e não inclui credenciais no
repositório.

## Configuração do serviço no Render

Crie um serviço com estas opções:

| Campo | Valor |
| --- | --- |
| Service type | **Web Service** |
| Runtime/Language | **Docker** |
| Root Directory (monorepo) | `fen` |
| Dockerfile | `Dockerfile` dentro do Root Directory |

Se o repositório remoto contiver somente o projeto `fen`, deixe o **Root
Directory** vazio; o mesmo `Dockerfile` continuará sendo encontrado na raiz.
Não é necessário criar `render.yaml`: o Dockerfile e as variáveis de runtime
são suficientes nesta etapa.

O Dockerfile usa o Gradle Wrapper 9.5.1 e Java 25 para produzir o Spring Boot
fat JAR. O estágio final executa somente `java -jar /app/app.jar` como usuário
não-root. O build não roda testes e não recebe secrets por `ARG` ou `ENV`.

## Profile e porta HTTP

Cadastre `SPRING_PROFILES_ACTIVE=prod`. Isso carrega
`application-prod.properties`, onde datasource e JWT não possuem fallback
local.

O backend usa `server.port=${PORT:8080}`. O Render fornece `PORT` em runtime;
normalmente não cadastre essa variável manualmente. `EXPOSE 8080` no Dockerfile
é apenas documentação da imagem e não fixa a porta real. Não há
`server.address=localhost` nem outra configuração que restrinja o bind externo.

Os logs padrão do Spring Boot são emitidos no console, compatível com os logs
de stdout/stderr do Render. O projeto não possui Actuator nem endpoint público
de health nesta etapa; não configure um health check HTTP customizado até que
exista uma rota pública própria para isso.

## Valores vindos do Neon

No painel do Neon, obtenha os parâmetros de conexão e preencha-os em variáveis
separadas no Render. O driver continua sendo o PostgreSQL JDBC; não há driver
específico do Neon.

Quando o Neon fornecer uma URI como:

```text
postgresql://<usuario>:<senha>@<host>/<banco>?sslmode=require
```

use estes valores:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://<host>/<banco>?sslmode=require
SPRING_DATASOURCE_USERNAME=<usuario>
SPRING_DATASOURCE_PASSWORD=<senha>
```

Preserve todos os parâmetros de query fornecidos pelo Neon, em especial
`sslmode=require`. Não adicione `sslmode=disable` nem substitua a URL por uma
connection string contendo a senha.

## Primeiro startup e banco vazio

Com o profile `prod`, a sequência de startup é:

```text
Spring conecta ao Neon
→ Liquibase usa o contexto production
→ changesets pendentes criam/atualizam o schema
→ Hibernate valida o schema (ddl-auto=validate)
→ aplicação inicia
```

Liquibase permanece ativo com `spring.liquibase.contexts=production` e é a
única ferramenta de evolução do schema; Hibernate não cria, atualiza nem
remove tabelas.

### Carga demonstrativa fictícia

O changeset `seed-production-demo-data` contém dados estritamente fictícios
para a produção demonstrativa do TCC: medicamentos, comorbidades, interações,
pacientes e seus vínculos. Ele pertence ao contexto Liquibase `production` e,
portanto, será aplicado automaticamente no próximo deploy com o profile
`prod`. Não cria usuários, funcionários ou atendimentos.

Em um banco Neon que já possui o schema, basta redeployar o backend. O
Liquibase encontrará o novo changeset, executará a carga uma única vez e o
registrará em `DATABASECHANGELOG`, sem duplicar registros nos próximos
deploys. Antes de usar o mesmo banco como produção com dados reais, planeje
uma migration separada para retirar ou substituir a carga demonstrativa.

### Limitação operacional: ADMIN inicial

O changeset atual de ADMIN é intencionalmente restrito aos contexts `dev` e
`test`. Portanto, em um Neon vazio:

```text
Liquibase cria o schema
→ aplicação inicia
→ nenhum usuário ADMIN existe
```

O deploy funciona tecnicamente, mas o ambiente não estará operacional para
administração/efetivação de usuários até que seja definida uma estratégia
segura para provisionar o primeiro ADMIN. Não reutilize o seed de `dev/test` e
não introduza credenciais administrativas fixas em migration de produção.

## JWT, CORS e anexos

`JWT_SECRET` é obrigatório em produção e deve ter ao menos 32 bytes UTF-8.
`JWT_EXPIRATION_SECONDS` é opcional e mantém o default de `28800` segundos
(8 horas). Nunca salve esses valores em arquivos versionados.

`FEN_CORS_ALLOWED_ORIGINS` aceita origens separadas por vírgula. Enquanto o
frontend estiver local, use `http://localhost:4200`; depois inclua a origem
HTTPS real do frontend implantado. Não use `*`, pois a API usa credenciais/JWT.

`FEN_ATTACHMENTS_STORAGE_PATH` continua configurável. O filesystem normal do
container Render é efêmero e não deve ser tratado como armazenamento
permanente de anexos. Configure Persistent Disk ou object storage antes de
usar anexos em produção de forma persistente; esta tarefa não altera a
arquitetura de storage.

## Validação local recomendada

Execute fora do Docker antes de deploy:

```bash
./gradlew --no-daemon test
./gradlew --no-daemon bootJar
```

Em uma máquina com Docker disponível, a partir de `fen`:

```bash
docker build -t fen-backend .
```

O container precisa receber as mesmas variáveis de produção em runtime. Não
use credenciais reais do Neon para uma validação local desnecessária. O arquivo
`.env.example` é somente uma referência de nomes; Spring Boot não carrega
arquivos `.env` automaticamente.

## Variáveis para cadastrar no Render

| Variável | Valor no Render | Obrigatória |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `prod` | Sim |
| `SPRING_DATASOURCE_URL` | URL JDBC Neon, com SSL preservado | Sim |
| `SPRING_DATASOURCE_USERNAME` | usuário Neon | Sim |
| `SPRING_DATASOURCE_PASSWORD` | senha Neon | Sim (secret) |
| `JWT_SECRET` | segredo aleatório com pelo menos 32 bytes | Sim (secret) |
| `JWT_EXPIRATION_SECONDS` | `28800` ou duração desejada em segundos | Não |
| `FEN_CORS_ALLOWED_ORIGINS` | `http://localhost:4200` inicialmente; depois a URL HTTPS do frontend | Sim para produção do frontend |
| `FEN_ATTACHMENTS_STORAGE_PATH` | caminho no filesystem/volume configurado | Recomendado; obrigatório para persistência de anexos |
| `PORT` | fornecida automaticamente pelo Render | Não cadastrar manualmente |
