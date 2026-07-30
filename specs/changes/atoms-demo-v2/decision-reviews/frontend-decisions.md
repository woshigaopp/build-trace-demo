# Frontend Decisions

| ID | Decision | Rejected alternative | Reason | Affected files | Verification | Status |
|---|---|---|---|---|---|---|
| FDEC-001 | One state-driven SPA with auth and workspace states | Add routing solely for two states | Keeps flow fast without route complexity; no deep-link requirement | `App.tsx`, `AuthScreen.tsx` | auth browser flow | locked |
| FDEC-002 | Sandpack owns editing/preview; BuildTrace owns file tree/save/version controls | Custom textarea + iframe only | Mature compiler/runtime while retaining product-specific persistence | `WorkspacePanel.tsx` | multi-file browser flow | locked |
| FDEC-003 | Persist only explicit manual Save, not every keystroke | Autosave each edit | Prevents version spam and accidental LLM context changes | `WorkspacePanel.tsx`, API | save/refresh test | locked |
| FDEC-004 | Failed run is a durable chat/run row with retry | Ephemeral toast | Closes missing conversation and recoverability feedback | chat/run components | fail/refresh test | locked |

User-visible semantics are inherited from locked PRD; these decisions only select concrete interaction mechanisms.
