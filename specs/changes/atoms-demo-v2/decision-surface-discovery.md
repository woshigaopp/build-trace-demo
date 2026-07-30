# Decision Surface Discovery

## Decision Surface Inventory

| ID | Surface | Current evidence | Locked decision | Owner stage | Verification |
|---|---|---|---|---|---|
| DS-001 | Identity and ownership | Caller-controlled guest header | Registration/login with JWT; owner comes only from authenticated principal | PRD | Cross-account API denial test |
| DS-002 | Generation lifecycle | Ephemeral SSE phase | Durable run state machine with terminal error and timing | PRD/design | Refresh during/after terminal run |
| DS-003 | Generated artifact | One HTML column | Immutable multi-file React snapshots | PRD/design | File tree displays full files after relogin |
| DS-004 | Incremental mutation | Full HTML rewrite | Structured write/delete operations applied to a candidate | PRD/design | 5-10 sequential modifications |
| DS-005 | Failure recovery | Terminal SSE error only | One automatic repair attempt; invalid candidate never changes current version | PRD/design | Forced invalid output test |
| DS-006 | Edit and preview | `<pre>` + `srcDoc` | Editable file workspace + Sandpack preview; manual save creates version | PRD/frontend | Edit, save, preview and refresh |
| DS-007 | Version behavior | HTML restore creates new version | Snapshot restore creates a new immutable version; show changed-file diff | PRD/frontend | Restore and compare test |
| DS-008 | Legacy data | v1 guest rows in production DB | Do not expose or destructively migrate anonymous v1 rows | PRD/compatibility | New accounts list only owned rows |

## Capability Support Matrix

| Capability | v2 behavior | Backend behavior | Frontend behavior | Failure behavior |
|---|---|---|---|---|
| Register/login/logout | supported | BCrypt password, JWT, `/me` | Dedicated auth screen and account menu | Field-level message; expired token returns to login |
| Create/open/list project | supported | Principal-scoped queries | Sidebar and empty state | Explicit retry/error |
| Initial AI generation | supported | Durable run + multi-file snapshot | Live phases then workspace | Failure stored, retry enabled |
| Incremental generation | supported | Candidate operations + validate + repair | Same conversation flow | Old version remains active |
| Multi-file browse/edit | supported | Full snapshot read/save | File tree + editor | Invalid/empty files rejected |
| Preview | supported | Files returned as source of truth | Sandpack compile/runtime | Runtime error shown without data loss |
| Versions/diff/restore | supported | Immutable snapshots | Version list, changed-file compare, restore | Restore failure leaves current unchanged |
| Generate Java/backend apps | unavailable | API accepts React project operations only | Product copy says React app | No misleading deployment claim |
| Deploy generated app independently | unavailable | No publish endpoint | No fake Publish control | Explicit non-goal |

## Frontend Action Surface Graph

| Action | Source | API/route | State constraint | Expected result | Negative assertion |
|---|---|---|---|---|---|
| Register | auth form | `POST /api/auth/register` | signed out | token + authenticated workspace | No guest fallback |
| Login | auth form | `POST /api/auth/login` | signed out | recover owned projects | Wrong password never reveals account existence details |
| Logout | account menu | local token removal | signed in | return to auth screen | No stale project remains visible |
| Generate | chat composer | `POST /api/projects/{id}/generate` | no active run | durable run advances to terminal state | Double-submit is blocked |
| Retry | failed run/message | same generate endpoint with same prompt | prior run failed | new run, old version intact | Failed candidate is never previewed |
| Select file | workspace file tree | local Sandpack action | version loaded | full content visible/editable | Long content is not truncated |
| Save edit | workspace toolbar | `POST /api/projects/{id}/versions` | local files dirty | new manual version | Save is not UI-only |
| Restore | version panel | `POST .../restore` | terminal generation | restored snapshot becomes new version | History is not rewritten |

## Post-Create Consumer Audit

| Object | Consumer | Required state | Decision | Verification |
|---|---|---|---|---|
| Account | `/me`, project list, every project mutation | valid JWT subject | Server derives owner from principal | auth integration tests |
| Project | list, detail, generate, save, restore | owned by subject | Return 404 for non-owner to avoid enumeration | cross-user tests |
| Generation run | SSE, detail refresh, conversation | durable ordered state | Run remains inspectable after stream disconnect | integration/browser refresh test |
| Version snapshot | workspace, diff, restore | complete validated file map | Immutable; current pointer changes atomically | service tests |

## Operation Mutability Matrix

| Object/field | Operation | Decision | UI expression | Verification |
|---|---|---|---|---|
| User email | update | unsupported | no edit control | no API route |
| Project name | update | supported | rename action | ownership + validation test |
| Version files | update | immutable | edit creates a new version | old version equality assertion |
| Current version | restore | new-version copy | restore confirmation | version count increments |
| Active run | cancel | not implemented in v2 | no cancel control | no misleading action |

All decision surfaces are locked. There are no `needs-decision` rows.
