# Code Scope Discovery

## Discovery Seeds

| Seed | Source | Search terms / paths | Why relevant |
|---|---|---|---|
| Authentication | reviewer feedback | `X-Guest-Id`, `guestId`, security dependencies | Determine whether identity is real and enforced server-side. |
| Generation stability | reviewer feedback | `generate`, `stream`, `HtmlExtractor`, error handling | Trace input, model output, validation and failure persistence. |
| Persistence completeness | reviewer feedback | entities, repositories, `ProjectDetail` | Determine which user-visible objects survive refresh. |
| Code management | reviewer feedback | `currentHtml`, PreviewPanel, VersionEntity | Determine artifact shape, display and restore behavior. |

## Search Coverage

| Area | Required? | Evidence path / command | Finding | Stop condition met? |
|---|---:|---|---|---:|
| UI route/page | yes | `frontend/src/App.tsx`, `components/*.tsx` | One workspace page, no auth route; code is rendered as one `<pre>`. | yes |
| API route/client | yes | `frontend/src/api.ts`, `ProjectController.java` | Every request sends browser-generated `X-Guest-Id`; generate uses POST + SSE. | yes |
| Config/schema | yes | `application.yml`, JPA entities | H2/Postgres with `ddl-auto=update`; project/message/version persist only HTML-oriented state. | yes |
| State/status/error | yes | `GenerationService.java`, `ApiExceptionHandler.java` | Phase is ephemeral SSE; failures are not stored; old current HTML survives by accident. | yes |
| Permission/visibility | yes | `rg guestId`, no Spring Security dependency | Ownership is a caller-controlled header, not authentication. | yes |
| Runtime/task | yes | `AiGenerationClient.java`, `HtmlExtractor.java` | Entire HTML is rewritten; only complete HTML accepted; no repair or durable run. | yes |
| Tests/fixtures | yes | `backend/src/test/**`; no frontend tests | Service/extractor tests exist; no auth, run lifecycle, multi-file or browser tests. | yes |
| Docs/current specs | yes | `README.md`, `docs/ai-development-report.md` | v1 trade-offs accurately describe single-file scope but no longer meet feedback. | yes |

## Current Product/Code Understanding

| Area | Current behavior | Evidence path / command | Product implication | Gap / decision |
|---|---|---|---|---|
| Identity | Local browser creates a UUID and can send any UUID. | `frontend/src/api.ts`, `ProjectController.java` | No account recovery or trustworthy isolation. | Replace with email/password + JWT and server-derived owner. |
| Conversation | User message is stored before model call; success assistant message is stored. | `ProjectService.addUserMessage/completeGeneration` | Refresh recovers only successful portions. | Persist run and terminal assistant message for both success and failure. |
| Generation | Full current HTML is appended to every modification prompt. | `AiGenerationClient.buildUserPrompt` | Context and output grow on every edit; timeout/truncation risk increases. | Return bounded structured file operations. |
| Validation | Extractor only checks for complete `<html>`. | `HtmlExtractor.extract` | No repair and no project-level invariant checks. | Validate candidate files, retry repair once, atomically commit only valid candidate. |
| Code artifact | Project/current version stores one HTML string. | `ProjectEntity`, `VersionEntity` | Cannot browse/edit a normal project. | Store a map of paths to complete file contents per immutable version. |
| Preview | iframe `srcDoc` executes one HTML document. | `PreviewPanel.tsx` | Supports interaction but not React multi-file compilation. | Use Sandpack React runtime and surface build status/errors. |
| Versions | Restore copies HTML into a new version. | `ProjectService.restore` | Immutable restore behavior is sound and should remain. | Restore a complete file snapshot as a new version. |
| Deployment | Vercel + Railway + Neon are already connected. | repo config and prior production runtime | Keep operational topology and migrate in place. | New schema must tolerate existing v1 rows. |
