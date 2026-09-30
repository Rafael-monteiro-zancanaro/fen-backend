# Deploy Render + Neon — Design

## Objetivo

Preparar o backend Spring Boot `fen` para execução como um Render Web Service
baseado em Docker, conectado a um PostgreSQL do Neon. A preparação não cria
recursos externos nem inclui credenciais reais no repositório.

## Contexto confirmado

- O projeto usa Java 25, Gradle Wrapper 9.5.1, Spring Boot, Liquibase e o
  driver JDBC PostgreSQL.
- Os testes usam o profile `test` com H2 isolado.
- O changelog Liquibase possui os contexts `production`, `dev` e `test`.
- O changeset de administrador inicial é restrito a `dev/test` e não deve ser
  promovido para produção nesta tarefa.
- CORS e o caminho de anexos já usam, respectivamente,
  `FEN_CORS_ALLOWED_ORIGINS` e `FEN_ATTACHMENTS_STORAGE_PATH`.

## Configuração de ambientes

A configuração local existente permanece no `application.properties`. Um novo
`application-prod.properties` será ativado exclusivamente por
`SPRING_PROFILES_ACTIVE=prod` e conterá apenas referências a variáveis de
ambiente, nunca valores de produção.

No profile `prod`, estas variáveis serão obrigatórias e não terão fallback
local:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`

O datasource continuará usando o driver PostgreSQL. A URL JDBC entregue pelo
Neon será usada sem removê-la ou sobrescrever parâmetros SSL, portanto
`sslmode=require` continuará efetivo quando informado pelo Neon.

O profile de produção definirá `spring.jpa.hibernate.ddl-auto=validate` e
`spring.liquibase.contexts=production`. Assim, no primeiro startup de um
banco Neon vazio, Liquibase criará o schema e aplicará changesets pendentes;
em seguida Hibernate somente validará o resultado.

`server.port` aceitará `${PORT:8080}` para atender ao Render e manter a porta
local. O servidor padrão do Spring Boot já escuta em interfaces de rede
compatíveis com o serviço, sem fixação em `localhost`.

JWT será obtido por `JWT_SECRET`; a expiração permanecerá configurável por
`JWT_EXPIRATION_SECONDS`, com default de `28800` segundos (8 horas). CORS
continuará aceitando a lista separada por vírgulas em
`FEN_CORS_ALLOWED_ORIGINS`, sem origem curinga com credenciais. O caminho dos
anexos continuará vindo de `FEN_ATTACHMENTS_STORAGE_PATH`.

## Container

O Dockerfile terá dois estágios compatíveis com Java 25:

1. uma imagem JDK compila o fat JAR pelo Gradle Wrapper 9.5.1;
2. uma imagem JRE enxuta recebe somente o JAR executável e o executa como
   usuário não-root.

O build usará `bootJar` e não executará testes. Ele não depende de Neon, de
secrets de runtime ou de Maven/Gradle instalado no Render. `EXPOSE 8080` será
apenas descritivo; a porta real vem de `PORT`.

`.dockerignore` excluirá Git, build local, arquivos de ambiente, logs,
configurações de IDE e armazenamento físico de anexos. `.gitignore` ignorará
`.env` e variantes locais, preservando `.env.example` versionado.

## Operação e documentação

`.env.example` documentará os nomes das variáveis, sem valores secretos.
`docs/deployment-render-neon.md` explicará a criação do Web Service Docker,
uso de `fen` como Root Directory no monorepo, configuração das variáveis,
conversão da string Neon para URL JDBC quando necessário, e a natureza não
persistente do filesystem do container.

Não será criado `render.yaml`, não será incluído Actuator e o frontend não
será alterado. Como não existe endpoint público de health nem Actuator, a
documentação não indicará um health check HTTP customizado.

## Segurança e limitações conhecidas

Nenhuma senha, URL com credencial, segredo JWT ou token será adicionado ao
Dockerfile, propriedades de produção, `.env.example` ou documentação.

Um Neon novo receberá o schema via Liquibase, mas não receberá o administrador
inicial: o seed atual se aplica somente a `dev/test`. A estratégia segura de
provisionamento de ADMIN permanece uma pendência explícita e fora deste
escopo; não haverá credenciais administrativas fixas em migration de
produção.

Os logs continuarão no console padrão do Spring Boot, adequado a stdout/stderr
do Render. O pool Hikari manterá os defaults, sem tuning prematuro.

## Verificação

A suíte H2 será executada separadamente do build Docker. A tentativa de
baseline neste ambiente iniciou, mas não alcançou resultado final verificável.
Após a alteração, a suíte Gradle será novamente executada e o resultado real
será reportado. Este ambiente não possui Docker CLI, portanto `docker build`
e `docker run` não poderão ser executados aqui; a validade estática do
Dockerfile e o `bootJar` serão verificados na medida disponível.
