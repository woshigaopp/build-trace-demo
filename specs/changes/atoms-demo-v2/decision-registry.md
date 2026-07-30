# Decision Registry

| ID | Decision key | Layer | Decision | Status | Source | Verification |
|---|---|---|---|---|---|---|
| PDEC-001 | identity.mode | product | Email/password + JWT | locked | reviewer feedback + AI authorization | auth and isolation tests |
| PDEC-002 | generated.scope | product | Multi-file React apps only | locked | reviewer feedback + AI authorization | file/runtime acceptance |
| PDEC-003 | preview.runtime | product | Sandpack editor/preview | locked | official evidence + AI authorization | browser build/interaction |
| PDEC-004 | generation.contract | product | Structured file operations | locked | failure analysis + AI authorization | parser and sequence tests |
| PDEC-005 | generation.failure-policy | product | One repair, atomic commit | locked | failure analysis + AI authorization | forced-failure test |
| PDEC-006 | history.persistence | product | Persist every run and terminal outcome | locked | reviewer feedback + AI authorization | refresh/relogin test |
| ADEC-001 | auth.token | engineering | HMAC JWT with BCrypt password hashes | locked | Spring deployment fit | security integration tests |
| ADEC-002 | snapshot.storage | engineering | Version-level JSON file map; retain legacy HTML columns for migration compatibility | locked | existing production schema | JPA integration tests |
| ADEC-003 | run.execution | engineering | Virtual-thread worker + POST SSE, durable state before async work | locked | current service topology | disconnect/reload test |
| ADEC-004 | preview.integration | engineering | Sandpack 2.20.0 React provider/editor/preview | locked | E-001/E-002 | frontend build/browser test |
| FDEC-001 | frontend.navigation | frontend | State-driven auth/workspace SPA | locked | frontend contract | auth browser flow |
| FDEC-002 | frontend.workspace | frontend | Sandpack runtime plus BuildTrace persistence controls | locked | PDEC-003 | file browser flow |
| FDEC-003 | frontend.manual-save | frontend | Explicit save creates versions | locked | REQ-008 | save/refresh test |
| FDEC-004 | frontend.failure | frontend | Durable failure row plus retry | locked | REQ-006/007 | fail/refresh test |

## Consistency Matrix

| Decision key | Active decisions | Conflict? | Result |
|---|---|---:|---|
| identity.mode | PDEC-001, ADEC-001 | no | Engineering mechanism implements product identity. |
| generated.scope | PDEC-002, ADEC-002/ADEC-004 | no | Storage and runtime both implement React file snapshots. |
| generation.failure-policy | PDEC-005, ADEC-003 | no | Durable run owns bounded execution and atomic commit. |
