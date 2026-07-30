# Frontend Action Inventory

| Action ID | Action | User intent | Reachable from | Side effect/API called | Success behavior | Failure behavior | Owner issue |
|---|---|---|---|---|---|---|---|
| UI3-ACT-001 | Open Trace | inspect AI work | builder tabs | local tab state | latest run selected | empty trace explanation | T3-TRACE |
| UI3-ACT-002 | Select run | inspect history | Trace run list | local selection | selected trace details render | selection falls back to latest valid run | T3-TRACE |
| UI3-ACT-003 | Publish | deliver current version | Preview toolbar, owned project | `POST /api/projects/{id}/publish` with no body | publication state and stable URL update | error banner; prior publication retained | T3-PUBLISH |
| UI3-ACT-004 | Copy link | share deliverable | published toolbar state | clipboard write | copied confirmation | copy error is visible | T3-PUBLISH |
| UI3-ACT-005 | Open published app | inspect deliverable | published toolbar state | navigate/open `/p/{token}` | anonymous runtime loads | public error surface | T3-PUBLIC |
| UI3-ACT-006 | Load public app | use shared result | direct `/p/{token}` | `GET /api/public/projects/{token}` | pinned Sandpack files run | 404/error without auth redirect | T3-PUBLIC |
