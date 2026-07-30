# BuildTrace Atoms Demo v2 Product Specification

Status: **locked**. Product decision authority: **AI-authorized by the user's 2026-07-30 `授权` message**.

## Propose Extraction

| Source | Propose | Explicit fact | Inference | Decision/result |
|---|---|---|---|---|
| Assessment | Deliver an interactive, persistent, usable online Atoms demo | Public URL and real functionality are mandatory | The whole primary flow must work for a fresh reviewer | REQ-001..REQ-010 |
| Reviewer feedback | Fix auth, stability, incomplete history and single HTML management | v1 failed these areas | Cosmetic changes cannot pass | PDEC-001..PDEC-006 |
| User authorization | Make the replacement version to a higher bar | Last resubmission and AI may decide | Public runtime evidence is the completion gate | VER-001..VER-009 |

## Users And Scenarios

| User | Goal | Current pain | Desired outcome |
|---|---|---|---|
| Evaluator | Quickly verify a real AI app builder | v1 looks plausible but breaks under repeated use | Complete the flow without setup or hidden knowledge |
| Returning user | Continue prior work | Guest identity and partial history are unreliable | Login and recover projects, chat, runs and versions |
| Builder | Iterate on generated code | One HTML blob is hard to inspect and modify | Browse/edit multiple files and see a live preview |

## Product Object Model

| Object | Meaning | Key properties | Lifecycle/state |
|---|---|---|---|
| Account | Durable user identity | email, password credential, created time | registered / authenticated / signed out |
| Project | User-owned app workspace | name, current version, timestamps | empty / has version |
| Message | Durable conversation item | role, content, run, timestamp | stored; never silently removed |
| Generation run | One auditable AI attempt | prompt, model, status, attempt, error, duration | queued -> generating -> validating -> repairing -> succeeded/failed |
| Version | Immutable project snapshot | number, source, prompt, summary, files | created; may be copied by restore |
| File | Complete source unit inside a version | absolute path, content | read/edit in candidate; immutable after save |

## Requirements

| ID | Requirement |
|---|---|
| REQ-001 | A user can register, log in, log out and recover the same projects by logging in again. |
| REQ-002 | Every project/read/write is scoped by server-authenticated account identity. |
| REQ-003 | A user can create and reopen projects. |
| REQ-004 | AI produces a runnable multi-file React project from a natural-language request. |
| REQ-005 | Subsequent requests modify the current project through structured file operations. |
| REQ-006 | Every run, user message, success/failure assistant message, status and error survives refresh. |
| REQ-007 | Invalid output receives at most one automatic repair; a failed candidate never changes the current version. |
| REQ-008 | All files are browsable and editable without truncation; manual save creates an immutable version. |
| REQ-009 | The app compiles/runs in an embedded preview and exposes preview/build failure visibly. |
| REQ-010 | Users can compare changed files and restore an old snapshot as a new version. |

## Configuration

| Config | Meaning | Default | Validation | Visible where |
|---|---|---|---|---|
| Email | Account identifier | none | normalized, valid format, <=254, unique | auth form/account menu |
| Password | Account credential | none | 8..72 characters | auth form only |
| Project name | Workspace label | derived from first request | nonblank, <=160 | create flow/sidebar |
| Generation prompt | Requested app/change | none | nonblank, <=4000 | chat composer |

## State Matrix

| State | Meaning | Entered when | User action | Terminal? |
|---|---|---|---|---:|
| queued | Run is durably accepted | message/run transaction commits | wait | no |
| generating | Model request is active | worker starts model call | observe | no |
| validating | Candidate is parsed and checked | first model call returns | observe | no |
| repairing | One corrective model call is active | parse/validation fails | observe | no |
| succeeded | Valid snapshot committed | transaction updates project pointer | preview/edit/continue | yes |
| failed | No valid snapshot was committed | model/repair/validation fails | retry | yes |

## Error And Degradation Matrix

| Scenario | Product behavior | User-visible result | Recovery |
|---|---|---|---|
| Duplicate email | Registration rejected | `该邮箱已注册` | Log in or use another email |
| Invalid/expired token | API returns 401 | Return to login; no stale data | Log in again |
| Concurrent generation | New run rejected | `当前项目已有生成任务` | Wait for terminal state |
| Model timeout/HTTP error | Run and failure message persist | Failed status with concise reason | Retry same prompt |
| Invalid structured output | Repair once | `正在自动修复` phase | Automatic, then retry if terminal failure |
| Preview build/runtime error | Source remains available | Preview error surface | Edit or ask AI to repair |
| Stream disconnect | Server run continues to terminal state | Reload shows durable run | Refresh project |

## Permission Matrix

| Action/view | Permission | API denial | UI behavior |
|---|---|---|---|
| Auth endpoints | public | validation/401 | auth screen |
| List/create projects | authenticated | 401 | login screen |
| Read/generate/save/restore project | authenticated owner | 404 for other owner | no foreign project entry |

## Acceptance Scenarios

### SCN-001 Registration and recovery
Given a fresh email, when the evaluator registers, logs out and logs in, then the same account and project list are restored.

### SCN-002 Isolation
Given two accounts, when account B requests account A's project ID, then the API returns 404 and no data.

### SCN-003 Initial generation
Given an empty project, when a valid request is generated, then a durable succeeded run creates a React snapshot with multiple files and an interactive preview.

### SCN-004 Repeated incremental changes
Given a valid current version, when 5-10 modifications are submitted serially, then each run terminates and successful runs preserve prior requested behavior.

### SCN-005 Automatic repair and atomic failure
Given invalid model output, when validation fails, then one repair is attempted; if repair fails, the run is failed and current version ID/files are byte-identical to before.

### SCN-006 Complete recovery
Given successful and failed runs, when the browser refreshes or the user relogs, then all messages, run statuses, errors and versions are present in order.

### SCN-007 File edit and preview
Given a generated snapshot, when a file is selected and edited, then full content is shown; save creates a version and preview reflects the saved files.

### SCN-008 Version compare and restore
Given at least two versions, when an older version is compared and restored, then changed files are identified and restore creates a new current version without deleting history.

### SCN-009 Public delivery
Given only the README and public URL, when a fresh evaluator follows the documented flow, then no local setup or privileged account is required.

## Product Decisions

| ID | Decision key | Locked decision | Rejected alternative | Reason |
|---|---|---|---|---|
| PDEC-001 | identity.mode | Email/password + JWT | Guest UUID | Reviewer explicitly rejected missing registration/login. |
| PDEC-002 | generated.scope | Multi-file frontend React apps | Arbitrary full-stack apps | Delivers inspectable code and real preview without claiming unsupported backend deployment. |
| PDEC-003 | preview.runtime | Sandpack | Keep iframe `srcDoc`; build WebContainer | Mature multi-file browser runtime with much lower infrastructure risk. |
| PDEC-004 | generation.contract | Structured file operations | Full-project HTML rewrite | Smaller bounded changes and deterministic application/validation. |
| PDEC-005 | failure.policy | One automatic repair, atomic commit | Infinite retry; partial apply | Bounded latency/cost and no poisoned current version. |
| PDEC-006 | history.policy | Persist every run and terminal conversation outcome | Persist successes only | Directly closes incomplete conversation feedback. |

## Source Trace

| Original source | Statement | Normalized section | Interpretation | Gap/decision |
|---|---|---|---|---|
| Assessment | real interaction and persistence | REQ-001..REQ-010 | Persistence covers all user-visible workflow state | locked |
| Reviewer | no register/login | REQ-001/002 | A real server-authenticated account is required | PDEC-001 |
| Reviewer | incremental development sometimes never succeeds | REQ-005/007 | Runs must terminate, repair is bounded and failures are durable | PDEC-004/005 |
| Reviewer | some project conversations missing | REQ-006 | Failure records are first-class persistent objects | PDEC-006 |
| Reviewer | only single HTML, incomplete display | REQ-004/008/009 | Multi-file snapshots and a complete editor/preview are P0 | PDEC-002/003 |

## Research Evidence

| ID | Source | Fact | Applies to | Confidence |
|---|---|---|---|---|
| E-001 | https://sandpack.codesandbox.io/docs/ | Sandpack exposes provider, code editor, preview, active file and file APIs for editable browser sandboxes. | PDEC-003 | high |
| E-002 | npm `@codesandbox/sandpack-react` metadata, 2026-07-30 | Latest stable observed version is 2.20.0. | implementation dependency | high |
| E-003 | current codebase | Spring Boot/Postgres/Vercel/Railway deployment topology already works. | compatibility | high |

## PRD Completeness Gate

| Dimension | Complete? | Evidence | Open decision | Blocks next stage |
|---|---:|---|---|---:|
| Propose/source trace | yes | propose and source trace tables | N/A | no |
| Current product/code | yes | `code-scope-discovery.md` | N/A | no |
| Users/object/scope | yes | this spec and proposal | N/A | no |
| Config/state/error | yes | matrices above | N/A | no |
| Permission/compatibility | yes | permission matrix and PDEC rows | N/A | no |
| Acceptance scenarios | yes | SCN-001..009 | N/A | no |
| Decisions locked | yes | PDEC-001..006; AI authorization SRC-007 | N/A | no |

## PRD Local Audit Report

| Audit scope | Finding | Severity | Evidence | Required backflow | Blocks PRD lock |
|---|---|---|---|---|---:|
| Source coverage | All behavior-affecting conversation, handoff, feedback, code and external runtime sources are registered. | none | source ledger | none | no |
| Current behavior | Every required area has concrete file evidence and a stop condition. | none | code scope | none | no |
| Decision authority | User explicitly authorized AI decisions after the final-resubmission instruction. | none | SRC-007 | none | no |
| Unsupported promise | Generated backend deployment is explicitly excluded. | none | PDEC-002/non-goals | none | no |

## Semantic Consumption Seed

| Upstream object | Required downstream | Copied semantics | Verification | Status |
|---|---|---|---|---|
| REQ-001/002, PDEC-001 | auth design/tasks | principal-derived owner, no guest fallback | auth/cross-user tests | ready |
| REQ-004/005/007, PDEC-002..005 | generation design/tasks | operations, candidate, bounded repair, atomic commit | parser/service/continuous tests | ready |
| REQ-006, PDEC-006 | persistence design/tasks | terminal messages and runs durable | refresh/relogin tests | ready |
| REQ-008..010 | frontend/version tasks | full files, Sandpack, diff/restore | browser acceptance | ready |
