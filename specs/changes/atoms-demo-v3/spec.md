# BuildTrace v3 Product Specification

Status: locked by the user's instruction to implement the higher product bar.

## Product Thesis

BuildTrace is a traceable AI software delivery workspace. It does not only return code: it makes requirement understanding, implementation planning, file changes, validation, repair and immutable delivery visible and recoverable.

## Requirements

| ID | Requirement |
|---|---|
| REQ3-001 | A newly registered account receives one clearly labelled, editable showcase project with a polished interactive React application and browser-local data persistence. |
| REQ3-002 | Every AI run durably exposes requirement understanding, implementation plan, ordered lifecycle events, actual changed files, server-side checks, model, duration and delivered version. |
| REQ3-003 | Live generation moves the workspace to the trace surface and keeps showing useful progress without exposing raw model JSON. |
| REQ3-004 | A project owner can publish the current immutable version and receive a stable anonymous read-only URL. |
| REQ3-005 | Later edits do not silently mutate the published snapshot; publishing again advances the same stable URL to the selected current version. |
| REQ3-006 | Model instructions require functional controls, realistic data, responsive product UI, empty states and localStorage persistence when the generated app owns editable domain data. |

## Product Decisions

| ID | Decision | Locked result | Reason |
|---|---|---|---|
| PDEC3-001 | Differentiator | Persisted Build Trace is a first-class workspace tab | Reliability must be visible in the product, not only README prose. |
| PDEC3-002 | Showcase | Seed one `template` version after registration | Evaluators see the quality ceiling before spending model quota. |
| PDEC3-003 | Publication | Pin an immutable version behind a random stable share token | A generated result becomes a deliverable while history remains explicit. |
| PDEC3-004 | Runtime | Public page runs the pinned files in Sandpack | Reuses the already accepted browser runtime and avoids unsupported backend claims. |
| PDEC3-005 | Verification language | Show only checks the server actually performs; browser compilation remains a separate runtime state | Avoids presenting model self-claims as engineering proof. |

## State And Failure Semantics

| Area | State | User-visible semantics | Failure/recovery |
|---|---|---|---|
| Trace | queued/generating/validating/repairing/succeeded/failed | ordered durable timeline and terminal result | failed run retains reason and retry action; current version is unchanged |
| Publication | never published/current/stale | stable URL, pinned version and published time | publish disabled without a current version; API error leaves prior publication intact |
| Public page | loading/running/not-found/error | branded loading shell then full interactive preview | explicit unavailable surface; no auth redirect |
| Showcase | template current version | clearly marked editable sample | ordinary version/generation behavior applies after edits |

## Acceptance Scenarios

| ID | Scenario | Expected result |
|---|---|---|
| SCN3-001 | Register a new account | LaunchBoard sample opens with rich multi-file UI and working localStorage interactions. |
| SCN3-002 | Generate or modify an app | Trace shows live phase, then persisted understanding, plan, timeline, changed paths, checks and delivered version after refresh. |
| SCN3-003 | Trigger validation repair | Timeline records validating, repairing, validating and terminal result without corrupting the prior version. |
| SCN3-004 | Publish current version | Stable `/p/{token}` URL opens without authentication and runs the pinned files. |
| SCN3-005 | Modify after publish | Workspace shows publication is behind current; public URL keeps the old snapshot until republished. |

## Non-Goals

- Per-generated-app Java/Python backends or databases.
- Arbitrary package installation, network access or secret handling in generated code.
- GitHub export, team collaboration or multiple model selection.
