# Render and Neon Deployment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `fen` deployable as a Docker-based Render Web Service connected to Neon PostgreSQL, with production configuration sourced entirely from runtime environment variables.

**Architecture:** Keep the current base/local and test configurations intact where possible, and add an explicit `prod` profile that requires Neon datasource and JWT inputs. Package the existing Spring Boot fat JAR using a Java 25 two-stage container; runtime secrets remain external to the image.

**Tech Stack:** Java 25, Spring Boot 4.1.0, Gradle Wrapper 9.5.1, Liquibase, PostgreSQL JDBC, H2 tests, Docker/Eclipse Temurin 25.

**Spec:** `docs/superpowers/specs/2026-09-29-deploy-render-neon-design.md`

## Global Constraints

- Do not change domain code, migrations, frontend code, or create `render.yaml`.
- `prod` must require `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, and `JWT_SECRET` without local fallbacks.
- Keep Liquibase enabled with context `production` and Hibernate `ddl-auto=validate` in `prod`.
- Preserve H2 test isolation and the existing `dev/test`-only ADMIN seed.
- Do not version real credentials, tokens, JWT secrets, or complete credential-bearing connection URLs.
- Docker uses Java 25 and Gradle Wrapper 9.5.1; it builds `bootJar` without running tests.
- No Docker execution claim is valid unless a Docker CLI becomes available.

## Review Focus

- Missing production datasource variable must fail configuration rather than silently connect to localhost.
- A Neon JDBC URL with `sslmode=require` must pass through unchanged.
- `PORT` must override 8080, while an unset `PORT` remains locally runnable on 8080.
- CORS must preserve explicit comma-separated origins with credentials and never use `*`.
- A fresh production schema must not execute the `dev/test` ADMIN seed; documentation must identify the resulting lack of ADMIN.

### Task 1: Separate runtime configuration safely

**Files:**
- Modify: `src/main/resources/application.properties`
- Create: `src/main/resources/application-prod.properties`
- Modify: `src/main/resources/application-test.properties`

**Interfaces:**
- Consumes: Render `PORT`, `SPRING_PROFILES_ACTIVE`, standard Spring datasource variables, `JWT_SECRET`, `JWT_EXPIRATION_SECONDS`, current `FEN_CORS_ALLOWED_ORIGINS`, and `FEN_ATTACHMENTS_STORAGE_PATH`.
- Produces: a `prod` profile with required datasource/JWT values and base/test behavior independent of production Neon credentials.

- [ ] **Step 1: Record the configuration assertions to verify**

Verify `application-prod.properties` contains direct unresolved placeholders for all three datasource variables and `JWT_SECRET`, `spring.liquibase.contexts=production`, and `spring.jpa.hibernate.ddl-auto=validate`; verify the base configuration uses `server.port=${PORT:8080}` and `JWT_EXPIRATION_SECONDS` with an eight-hour default.

- [ ] **Step 2: Remove the JWT literal and externalize runtime settings**

In `application.properties`, replace the hardcoded JWT secret with `JWT_SECRET`, express the existing eight-hour default as `JWT_EXPIRATION_SECONDS=28800`, preserve the current CORS/storage variables, and set the Render-compatible port fallback.

- [ ] **Step 3: Add the strict production profile**

Create `application-prod.properties` with unresolved placeholders for the three standard datasource variables and JWT secret, PostgreSQL driver/dialect, production Liquibase context, and Hibernate validation. Keep SSL controlled by the provided JDBC URL rather than adding a disabling parameter.

- [ ] **Step 4: Keep test credentials isolated from environment**

Set a non-production test-only JWT value in `application-test.properties`, so H2 tests do not require `JWT_SECRET` from the shell or Render.

- [ ] **Step 5: Verify configuration and compile/package**

Run: `GRADLE_USER_HOME=/tmp/fen-gradle ./gradlew --no-daemon bootJar`

Expected: successful Java 25 compilation and exactly one executable JAR under `build/libs`.

### Task 2: Create reproducible container and environment hygiene

**Files:**
- Create: `Dockerfile`
- Create: `.dockerignore`
- Modify: `.gitignore`
- Create: `.env.example`

**Interfaces:**
- Consumes: `gradlew`, `gradle/wrapper/`, `build.gradle`, `settings.gradle`, and `src/` from the `fen` Docker context.
- Produces: an image that runs `java -jar /app/app.jar` as non-root and an environment template containing only names/defaults safe to publish.

- [ ] **Step 1: Create the two-stage Java 25 Dockerfile**

Use the verified Eclipse Temurin Java 25 official image family, invoke `./gradlew --no-daemon bootJar` in the build stage, and copy the sole Spring Boot JAR into the runtime stage. Create a non-root runtime user, use `EXPOSE 8080`, and avoid `ARG` or secret-bearing `ENV` declarations.

- [ ] **Step 2: Limit the Docker build context**

Add `.dockerignore` entries for version-control metadata, Gradle/build output, IDE folders, logs, real `.env` variants, local attachment directories, and other unnecessary local files while retaining Gradle Wrapper, sources, build files, and `.env.example`.

- [ ] **Step 3: Protect local environment and attachment files from Git**

Add patterns for `.env`, `.env.local`, `.env.prod`, and local variants to `.gitignore`, explicitly unignore `.env.example`, and retain/extend local attachment storage exclusions without ignoring application code or Liquibase files.

- [ ] **Step 4: Add the safe environment template**

Create `.env.example` with `SPRING_PROFILES_ACTIVE=prod`, blank production datasource/JWT values, `JWT_EXPIRATION_SECONDS=28800`, localhost CORS default, and a blank attachment-path value. Include no real connection string or credential.

- [ ] **Step 5: Inspect artifacts statically**

Run: `git diff --check && rg -n '(JWT_SECRET=.{32,}|SPRING_DATASOURCE_PASSWORD=.+|ARG .*SECRET|ENV .*SECRET)' Dockerfile .env.example src/main/resources`

Expected: no whitespace errors and no committed secret values; only variable references/placeholders appear.

### Task 3: Document Render, Neon, operational limits, and verification

**Files:**
- Create: `docs/deployment-render-neon.md`

**Interfaces:**
- Consumes: final environment variable names, Dockerfile path, explicit `prod` profile, and current Liquibase/seed behavior.
- Produces: operator-facing deployment instructions for a Render Web Service without external provisioning automation.

- [ ] **Step 1: Document service creation and paths**

Describe Render Web Service with Docker runtime; set Root Directory to `fen` for the monorepo and leave it empty only when the repository root itself is `fen`. State that no `render.yaml` is required.

- [ ] **Step 2: Document Neon inputs and startup sequence**

List conversion from a Neon `postgresql://` URI to JDBC `jdbc:postgresql://` when needed, preserving query parameters such as `sslmode=require`. Explain Liquibase production execution followed by Hibernate validation.

- [ ] **Step 3: Document operational limitations**

State that Render supplies `PORT`, the container filesystem is not durable for attachments without a Persistent Disk/object storage, no custom health endpoint is available, and a blank Neon database contains no ADMIN after this deploy.

- [ ] **Step 4: End with the exact Render variable checklist**

List `SPRING_PROFILES_ACTIVE=prod`, three datasource values, `JWT_SECRET`, optional/defaulted JWT expiration, CORS origins, and attachment storage path. Clarify that `PORT` is supplied by Render and normally not entered manually.

### Task 4: Verify build and regression behavior within available tooling

**Files:**
- Verify: `build/libs/*.jar`
- Verify: Gradle test reports
- Verify: Docker availability

**Interfaces:**
- Consumes: the configuration, Dockerfile, and existing H2 test suite.
- Produces: an evidence-based validation report distinguishing completed checks from unavailable Docker execution and inconclusive test outcomes.

- [ ] **Step 1: Package with the Gradle Wrapper**

Run: `GRADLE_USER_HOME=/tmp/fen-gradle ./gradlew --no-daemon clean bootJar`

Expected: exit code 0 and a single executable JAR in `build/libs`.

- [ ] **Step 2: Run the full H2 suite separately**

Run: `GRADLE_USER_HOME=/tmp/fen-gradle ./gradlew --no-daemon test`

Expected: full test summary. If the process terminates before a final Gradle result, report it as inconclusive rather than passing.

- [ ] **Step 3: Check whether Docker validation is possible**

Run: `docker --version`

Expected: if Docker is unavailable, do not attempt to install it or claim Docker build/run validation. Report the limitation and the completed static/container-context checks.
