# DataSentry Repository Guide

This guide provides instructions for AI agents working in this repository.

## 1. Project Overview

**DataSentry** is an enterprise-grade intelligent data analysis agent based on Spring AI Alibaba.
- **Backend**: Java 17 + Spring Boot 3.4.8 + Spring AI 1.1.0 (`datasentry-management`)
- **Frontend**: Vue 3 + Vite + TypeScript + Element Plus (`datasentry-frontend`)

## 2. Backend (Java)

**Directory**: `datasentry-management/`

### 2.1 Build & Test
- **Build**: `mvn clean package -DskipTests` (fastest) or `make build`
- **Run Tests**: `mvn test` or `make test`
- **Run Single Test**: `mvn test -Dtest=ClassName#methodName`
- **Frameworks**: JUnit 5 (`junit-jupiter`), Testcontainers, Mockito.

### 2.2 Code Style & Linting
**Strict adherence required.** The CI pipeline will fail if these are not met.
- **Formatter**: Spring Java Format + Spotless.
- **Linter**: Checkstyle.

**Commands:**
- **Fix Formatting**: `mvn spring-javaformat:apply` (Fixes indentation/style)
- **Fix Imports**: `mvn spotless:apply` (Removes unused imports, adds headers)
- **Check Lint**: `mvn checkstyle:check` (Reports violations)

**Guidelines:**
- **Java Version**: 17.
- **Imports**: No unused imports. Grouping handled by Spotless.
- **Naming**: standard Java camelCase.
- **Annotations**: Use Lombok (`@Data`, `@Slf4j`) to reduce boilerplate.
- **Logging**: Use `@Slf4j` and `log.info()`, `log.error()`.
- **API**: Follow Spring Boot REST conventions.
- **MyBatis-Plus**: Prefer MyBatis-Plus CRUD and wrappers; avoid handwritten SQL unless absolutely necessary.

## 3. Frontend (Vue/TS)

**Directory**: `datasentry-frontend/`

### 3.1 Build & Run
- **Install**: `npm install`
- **Dev Server**: `npm run dev`
- **Build**: `npm run build`

### 3.2 Code Style
- **Linter**: ESLint + Vue Plugin.
- **Formatter**: Prettier.
- **Unused Code**: Knip.

**Commands:**
- **Fix All (Recommended)**: `npm run lint && npm run format`
- **Check Only**: `npm run lint:check && npm run format:check`
- **Check Unused**: `npm run unused`

**Rules:**
- **Quotes**: Single quotes (`'`).
- **Semi**: Semicolons at end of line.
- **Indent**: 2 spaces.
- **Trailing Comma**: Yes.
- **Components**: PascalCase (e.g., `HeaderComponent.vue`).
- **API**: Use `fetch` or `axios` (configured in `vite.config.js` proxy).

## 4. General & CI

### 4.1 Makefile (Root)
The project uses a `Makefile` in the root which delegates to `CI/make/`.
- `make build`: Build backend.
- `make test`: Run backend tests.
- `make format-fix`: Format Java code.
- `make lint`: Run miscellaneous linters (yaml, markdown, codespell).
- `make licenses-check`: Check file headers.

### 4.3 File Headers (New Files)
- **New files must NOT include the Apache license header block**. Keep existing headers in legacy files unchanged.

### 4.2 Git Commit Messages
**Format**: `type(module): message`
- **Types**: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`.
- **Example**: `feat(backend): add new model configuration endpoint`
- **Example**: `fix(frontend): resolve layout issue in settings page`

## 5. Development Workflow for Agents

1.  **Exploration**:
    - Use `ls -F` to navigate.
    - Read `README.md` in subdirectories for specific context.

2.  **Implementation**:
    - **Backend**:
        - Write code in `datasentry-management/src/main/java`.
        - Add tests in `datasentry-management/src/test/java`.
        - **ALWAYS** run `mvn spotless:apply && mvn spring-javaformat:apply` before verifying.
        - Verify with `mvn test -Dtest=YourTest`.
    - **Frontend**:
        - Write code in `datasentry-frontend/src`.
        - **ALWAYS** run `npm run lint && npm run format` before verifying.

3.  **Verification**:
    - Ensure build passes: `mvn clean package -DskipTests` (backend) or `npm run build` (frontend).
    - Ensure lints pass.

4.  **Commit**:
    - Use the specified conventional commit format.
