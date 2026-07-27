# BuildTrace

BuildTrace is a focused AI app builder demo for the DeepWisdom Atoms assessment. A visitor describes a web application, the Java backend streams generation progress from an OpenAI-compatible model, and the React workspace runs the resulting single-file HTML application in a sandboxed iframe. Every successful generation is persisted as a version and can be restored without deleting history.

## What works

- Guest workspaces isolated by a locally generated `X-Guest-Id`.
- Project creation, project switching, messages, and refresh recovery.
- OpenAI-compatible streaming generation over SSE.
- Complete HTML extraction and validation before a version is committed.
- Interactive iframe preview, code view, mobile preview, and a new-window preview.
- Multi-turn modification using the current complete HTML as context.
- Immutable version history: restoring an old version creates a new version.
- Explicit offline fallback when no model API key is configured.

The offline fallback exists so reviewers can still inspect the product workflow when a model provider is unavailable. It is visibly labeled and is not presented as a real model response.

## Architecture

```text
React + TypeScript + Vite (Vercel)
              |
              | JSON + SSE
              v
Spring Boot 3 / Java 21 (Railway)
       |                         |
       v                         v
OpenAI-compatible API      Neon PostgreSQL
```

The generated artifact is one self-contained HTML document with inline CSS and JavaScript. This deliberately avoids a WebContainer and arbitrary package installation: the assessment's core loop is generation, execution, iteration, persistence, and recovery.

## Run locally

Prerequisites: Java 21, Maven 3.9+, and Node.js 20+.

Start the backend:

```bash
cd backend
mvn spring-boot:run
```

Start the frontend in a second terminal:

```bash
cd frontend
npm ci
npm run dev
```

Open `http://localhost:5173`. By default, the backend stores data in `backend/data/buildtrace.mv.db` and uses the labeled fallback generator. To exercise the real model path, set `APP_AI_API_KEY` and, if needed, `APP_AI_BASE_URL` and `APP_AI_MODEL` before starting Spring Boot. See [`.env.example`](.env.example) for the complete environment inventory.

## Verification

```bash
cd backend && mvn test
cd frontend && npm run lint
cd frontend && npm run build
cd frontend && npm audit --audit-level=high
```

The browser acceptance path is:

1. Create a todo application and wait for v1.
2. Add, complete, and delete a todo inside the iframe.
3. Request a second modification and wait for v2.
4. Restore v1 and confirm that a new v3 is created.
5. Refresh and confirm that messages, current HTML, and all three versions remain.

## Deployment

No AWS account or self-managed VM is required. The intended deployment uses three managed services and an OpenAI-compatible model provider.

### 1. Neon

Create a PostgreSQL database and retain its host, database, user, and password. Railway needs a JDBC URL such as:

```text
jdbc:postgresql://HOST/DATABASE?sslmode=require
```

### 2. Railway backend

Create a service from this repository and set its root directory to `backend`. Railway will use `backend/Dockerfile` and `backend/railway.toml`. Configure:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://HOST/DATABASE?sslmode=require
SPRING_DATASOURCE_USERNAME=...
SPRING_DATASOURCE_PASSWORD=...
APP_AI_BASE_URL=...
APP_AI_API_KEY=...
APP_AI_MODEL=...
APP_CORS_ALLOWED_ORIGINS=https://YOUR-VERCEL-DOMAIN
```

After deployment, verify `https://YOUR-RAILWAY-DOMAIN/actuator/health` returns `UP`.

### 3. Vercel frontend

Create a project from the same repository, set its root directory to `frontend`, and configure:

```text
VITE_API_BASE_URL=https://YOUR-RAILWAY-DOMAIN
```

Deploy the frontend, then update `APP_CORS_ALLOWED_ORIGINS` on Railway to the exact Vercel origin and redeploy the backend if the final Vercel domain differs.

Do not commit or paste API keys, database passwords, or platform tokens into issues, documentation, or chat. Put them only in each platform's encrypted environment-variable settings.

## Security boundary

The iframe uses `sandbox="allow-scripts allow-forms"`. `allow-forms` is required for ordinary generated applications whose interaction is implemented with a standard form submit event. `allow-same-origin` is intentionally absent, so generated code cannot access the parent application's origin or storage.

This is demo-level containment, not a production arbitrary-code sandbox. A production design should serve generated applications from a separate origin and enforce CSP and outbound-network isolation. It should also add authentication, rate limits, quotas, database migrations, concurrency control, model usage limits, and retention policies.

## Scope and trade-offs

- SSE was chosen because generation is a one-way server-to-browser stream; WebSocket would add lifecycle complexity without product value here.
- Each modification sends the current complete HTML to the model. This is simple and reliable for a time-boxed demo, but production should add token limits, summarization, and structured patches.
- H2 is only the local default. Production must use persistent PostgreSQL because Railway's local filesystem is not durable application storage.
- The fallback demonstrates availability and interaction, but the submitted online demo must be configured with a real model key to satisfy the assessment.
- Complex multi-agent orchestration, WebContainer, package installation, OAuth, payments, and publishing generated apps as independent services are explicit non-goals.

The factual AI-assisted development record is in [`docs/ai-development-report.md`](docs/ai-development-report.md).
