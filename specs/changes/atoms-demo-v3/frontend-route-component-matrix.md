# Frontend Route Component Matrix

| Action ID | Visible action | Source component | Permission guard | Handler | Final route/API | Router definition | Landing component/file | Mode branch | Forbidden inherited UI/API | Verification | Owner issue |
|---|---|---|---|---|---|---|---|---|---|---|---|
| UI3-ACT-001/002 | Build Trace/run row | `WorkspacePanel.tsx` | owned project | tab/run state | local state | pathname branch in `App.tsx` | `TracePanel.tsx` | builder | raw model JSON/token dump | DOM/browser trace flow | T3-TRACE |
| UI3-ACT-003 | Publish/Update publication | `WorkspacePanel.tsx` | current stored version | publish handler | `POST /api/projects/{id}/publish` | state-driven builder | `WorkspacePanel.tsx` | builder | publish unsaved editor buffer | integration/browser publish | T3-PUBLISH |
| UI3-ACT-004/005 | Copy/Open | `WorkspacePanel.tsx` | publication exists | clipboard/window open | `/p/{token}` | pathname branch in `App.tsx` | `PublishedApp.tsx` | public | auth/project sidebar/chat/run data | browser public flow | T3-PUBLIC |
| UI3-ACT-006 | Shared URL load | `PublishedApp.tsx` | public token only | effect/API client | `GET /api/public/projects/{token}` | pathname branch in `App.tsx` | `PublishedApp.tsx` | public | Bearer requirement and owner metadata | API/browser public flow | T3-PUBLIC |

### Action Landing Local Audit Report

All visible actions have a source, exact API or route, landing file, permission rule and negative surface assertion. No route library is introduced.
