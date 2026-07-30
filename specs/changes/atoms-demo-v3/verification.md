# Verification Matrix

## Semantic Consumption Matrix

| Source | Derived verification | Proves | Status |
|---|---|---|---|
| REQ3-001/SCN3-001 | VER3-001 | registration seeds an editable multi-file template once | ready |
| REQ3-002/003, SCN3-002/003 | VER3-002/003 | trace persistence, live phase, repair/failure and version atomicity | ready |
| REQ3-004/005, SCN3-004/005 | VER3-004/005 | stable anonymous publication pins immutable versions | ready |
| REQ3-006 | VER3-006 | prompt contract and browser output quality | ready |

## Verification Feasibility Gate

| Verification | Environment / fixture | Available | Command/owner | Fallback | Blocks done | Risk |
|---|---|---:|---|---|---:|---|
| Backend composition | H2/PostgreSQL-compatible Spring tests | yes | `cd backend && mvn test` | none | yes | none |
| Frontend static | installed npm dependencies | yes | lint/build/audit | none | yes | none |
| Browser builder/public | local then Vercel/Railway/Neon | yes | real browser flows | none | yes | deployment freshness |
| New real Qwen generation | limited reviewer quota | intentionally no | prior real call plus deterministic protocol tests | reviewer performs final call | no | model quality remains a recorded boundary |

## Verification Matrix

| ID | Source | Behavior / contract | Type | Command/manual step | Expected result | Proves | Required |
|---|---|---|---|---|---|---|---:|
| VER3-001 | REQ3-001 | registration seed | integration/browser | register, list/get/interact | exactly one labelled template with valid files and working localStorage UI | showcase readiness | yes |
| VER3-002 | REQ3-002/003 | successful trace | integration/browser | deterministic generation then reload | understanding/plan/events/files/checks/model/duration/version persist and render | trace closure | yes |
| VER3-003 | SCN3-003 | repair/failure trace | integration | invalid first/second model results | repair events persist; failure has no new version | truthful failure semantics | yes |
| VER3-004 | REQ3-004 | anonymous publication | api-route/browser | publish then signed-out GET/open | same token, pinned files compile, no auth required | deliverable flow | yes |
| VER3-005 | REQ3-005 | immutable pin/republish | integration/browser | publish v1, create v2, read, republish | public remains v1 until explicit republish, then stable URL serves v2 | no silent mutation | yes |
| VER3-006 | REQ3-006 | quality contract | unit/docs/browser | parser/prompt tests and sample inspection | structured metadata accepted and sample meets interaction/responsive/persistence bar | generation quality boundary | yes |
| VER3-007 | all | frontend integrity | frontend | `npm run lint`, `npm run build`, audit | all pass; zero high vulnerabilities | bundle readiness | yes |
| VER3-008 | all | public product acceptance | runtime/browser | desktop and 390x844 end-to-end | no overlap, critical console error or stale bundle; share route works | submission readiness | yes |

### Verification Local Audit Report

| Source | Claimed proof | Auditor finding | Missing composition path | Required verification/backflow | Blocks done |
|---|---|---|---|---|---:|
| REQ3-001..006 | VER3-001..008 | Each backend mutation has state/API proof and each user task has browser proof. | none | none | no |

## Not Run Risk

| Source | Severity | Reason | Owner/approval | Blocks done |
|---|---|---|---|---:|
| Production Qwen call after prompt change | P2 | Preserve limited quota for evaluator; deterministic tests prove protocol, not subjective model output quality. | user-authorized quota constraint | no |

## Executed Evidence

| Verification | Result | Evidence |
|---|---|---|
| Backend composition | passed | `cd backend && mvn test -q`; auth, showcase, trace, repair/failure, publication pinning/republish and anonymous controller tests all passed |
| Frontend static | passed | `npm run lint`, `npm run build`; Vite emitted only the known Sandpack bundle-size warning |
| Dependency audit | passed | `npm audit --audit-level=high`: 0 vulnerabilities |
| Diff integrity | passed | `git diff --check` returned no errors |
| Fresh registration/showcase | passed | new account received exactly one labelled LaunchBoard; latest 180px narrow-board template compiled in Sandpack |
| Stateful showcase | passed | candidate stage advancement survived preview refresh through localStorage |
| Trace persistence | passed | fallback generation created v2; understanding, plan, two paths, ordered events, checks and delivered version rendered and survived reload |
| Public delivery | passed locally | pinned app compiled and remained interactive without auth; desktop and 390x844 public DOM were nonblank |
| Mobile builder | passed | at 390x844 the builder and Trace collapsed to a usable single-column surface with no toolbar overlap |

Production evidence is added to `acceptance/product-acceptance-review.md` after the merged commit is deployed. No additional Qwen request was made.
