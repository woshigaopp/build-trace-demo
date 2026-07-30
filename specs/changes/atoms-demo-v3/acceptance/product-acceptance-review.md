# Product Acceptance Review

Status: accepted locally and in production.

## PAR Readiness Local Audit Report

| Input/environment | Status | Finding | Required backflow | Blocks PAR |
|---|---|---|---|---:|
| Product spec and decisions | ready | `spec.md` and `decision-registry.md` lock the product thesis, trace truthfulness, showcase and publication semantics. | none | no |
| Frontend contract | ready | Page, action, route, payload, mode, fixture and browser matrices exist. | none | no |
| Verification matrix | ready | `verification.md` maps REQ3-001..006 to integration, frontend and browser proof. | none | no |
| Runtime | ready | Local Vite `5173`, Spring Boot `8080`, isolated H2 DB and in-app browser are available. | none | no |
| Real-model boundary | accepted Not Run | No additional production Qwen call, preserving the user's limited evaluator quota. | reviewer performs the final subjective generation | no |

## Acceptance Context

| Item | Value |
|---|---|
| Environment | local full stack plus Railway/Neon/Vercel production; no new production model request |
| URL | `http://127.0.0.1:5173`, `https://buildtrace-atoms-demo.vercel.app` |
| Branch/image/commit | merged GitHub commit `27cf3c0`; Railway `b253c4fe`; Vercel `dpl_BBXHqQP93wMaqxBLA4MncBHMVnM1` |
| Browser used | yes, desktop and explicit `390x844` viewport |
| Login/user role | freshly registered local and production project owners, then anonymous public visitor |
| Feature object under review | seeded LaunchBoard, generated v2 trace and pinned public version |

## Product Semantic Matrix

| Area | Expected product semantics | Actual behavior | Conflict? | Severity | Evidence |
|---|---|---|---|---|---|
| Registration | Account owns durable projects and receives one labelled sample. | Fresh registration created exactly one `LaunchBoard 招聘工作台 · 示例`, source `template`. | no | - | browser DOM plus auth integration test |
| Showcase | Demonstrate a credible product without pretending it was model-generated. | Seven-file React recruitment workspace compiled; search/filter/add/delete/advance/theme work; localStorage retained a stage change after refresh. | no | - | browser interaction and screenshot |
| Generation | A request has explicit progress and a recoverable terminal result. | Local fallback created v2 and durable run; completed toast, messages and version were coherent. | no | - | browser generation flow and integration tests |
| Build Trace | Show accountable facts, not raw model output or invented checks. | Understanding, three-step plan, two actual changed paths, ordered events, model/duration, four server checks and delivered v2 rendered; raw JSON absent. | no | - | Trace DOM and mobile screenshot |
| Publication | Publish current stored version, expose stable state and never publish mutable editor buffers. | Publish action derives current version server-side; current/stale states and explicit republish are distinguishable. | no | - | publication integration test and browser toolbar |
| Public runtime | Anonymous user receives only a pinned runnable product. | `/p/{token}` compiled LaunchBoard without builder chrome; public client sends no JWT; public DTO omits owner/chat/run data. | no | - | anonymous API test, frontend source and browser DOM |
| Responsive behavior | Core flows remain usable at 390x844. | Public application and builder Trace use narrow layouts; toolbar controls fit and content scrolls without overlap. | no | - | explicit viewport screenshots |

## State Consistency Checks

| State source | Observed value | Expected relation | Consistent? | Evidence |
|---|---|---|---:|---|
| Project list | sample has a runnable version | agrees with project detail current v1/v2 | yes | browser DOM |
| Project detail | messages, current files, versions, run and publication load together | durable aggregate is recoverable after reload | yes | reload browser flow and integration tests |
| Trace | terminal succeeded and delivered v2 | version list contains v2 and currentVersionId points to it | yes | Trace/version DOM |
| Publication | pinned v1 while current is v2 shows stale | public API remains v1 until explicit republish | yes | integration test and toolbar state |
| Public runtime | header shows pinned version | files equal the pinned immutable snapshot | yes | public API and Sandpack runtime |

## Runtime Capability Checks

| Capability | Expected semantics | Runtime action/evidence | UI/API evidence | Pass? | Issue if failed |
|---|---|---|---|---:|---|
| Generated interaction | controls change domain state | advanced candidate and refreshed preview | candidate remained in interview stage | yes | - |
| Trace recovery | reload preserves accountable execution facts | generated fallback v2, reloaded project | plan/files/checks/timeline/version persisted | yes | - |
| Publish | bodyless owner mutation pins current version | publish v1, create v2, read public, republish | stable token and explicit version advance | yes | - |
| Anonymous delivery | direct route works without account | opened `/p/{token}` and interacted | no auth surface; compiled React runtime | yes | - |

## Action Landing Checks

| Action | Source page/object state | Expected route/component | Actual route/component | Expected visible surface | Forbidden inherited surface | Pass? | Evidence |
|---|---|---|---|---|---|---:|---|
| Open Trace | builder with completed run | local Trace tab / `TracePanel` | Trace tab / `TracePanel` | build history and execution facts | raw model JSON | yes | DOM and screenshot |
| Publish | builder with current version | bodyless publish API, same builder | publication toolbar state | pinned version and share controls | editor buffer claim | yes | API test and browser |
| Open public app | published project | `/p/{token}` / `PublishedApp` | `/p/{token}` / `PublishedApp` | name, pinned version, runtime | sidebar/chat/trace/code controls | yes | anonymous browser DOM |

## Product Semantic Local Audit Report

| Lane | User path inspected | Expected | Actual | Evidence | Finding | Root stage candidate | Blocks acceptance |
|---|---|---|---|---|---|---|---:|
| Action landing | Trace, publish, public route | each command closes on its specified surface | all three landed correctly | browser DOM/API tests | none | - | no |
| Runtime capability | interact, refresh, generate, publish | user-visible behavior and durable state | all required capabilities executed | browser/integration | none | - | no |
| State consistency | list/detail/trace/version/public | one coherent version model | current, delivered and published versions remained distinct and correct | browser/integration | none | - | no |

## Issue Triage And Backflow

| Issue ID | Symptom | Severity | Root stage | Required backflow | Artifact updated | Blocks acceptance? |
|---|---|---|---|---|---|---:|
| PAR3-001 | A public request reused authenticated request headers when a JWT existed. | P2 | implementation-bug | use a dedicated anonymous request and rerun frontend/public acceptance | `frontend/src/api.ts`, this report | no after fix |
| PAR3-002 | Sandpack can remain visually blank for several seconds while compiling at a newly applied mobile viewport. | P3 | deployment/runtime-data-gap | retain explicit loading state and wait for runtime DOM before judging | verification evidence | no |

## Backflow Classification Local Audit Report

| Issue ID | Auditor root-stage finding | Missing invalidation | Verification rerun needed | Required artifact update | Blocks acceptance |
|---|---|---|---|---|---:|
| PAR3-001 | implementation used a broader helper than the public contract required | none; contract already forbade auth context | lint/build and anonymous browser load | frontend implementation and acceptance report | no after rerun |
| PAR3-002 | runtime startup latency, not a semantic or layout defect | none | wait for compiled iframe DOM at both viewports | final verification evidence | no |

## Final PAR Exit Audit

Acceptance has real browser evidence, state/API composition proof and no unresolved P0-P2 issue.

Production evidence on 2026-07-30:

- Railway deployment `b253c4fe-33ec-47ff-9de3-e23305857921` reached `SUCCESS`; `/actuator/health` returned `UP`, and the anonymous missing-publication probe returned contract-level 404 rather than auth 401.
- Vercel deployment `dpl_BBXHqQP93wMaqxBLA4MncBHMVnM1` reached `Ready`; the production alias and `/p/{token}` rewrite both returned 200.
- Fresh account `prod-v3-1785402635319@example.com` registered successfully against production PostgreSQL and received exactly one labelled nine-file LaunchBoard template.
- LaunchBoard compiled in Sandpack. Advancing `林知夏` changed stage counts from `2/2/1` to `1/3/1`; preview refresh retained the state through localStorage.
- Publishing v1 produced stable route `/p/3715b9dd061441a3b0dad7f8983de246`. A no-auth HTTP request returned exactly `name`, `versionNumber`, `files`, `publishedAt`, with nine files and no extra keys.
- The published runtime compiled at desktop and `390x844`; controls and two board columns remained visible without overlap. Builder and public browser error logs were both empty.

The Sandpack runtime can show its terminal after the first compile at a newly applied viewport and may require the provided refresh control once. This remains the recorded P3 runtime-latency limitation; it does not corrupt versions or block interaction after refresh.
