# Frontend Page Inventory

## User Task Contract

| User task ID | User goal | Entry points | Page/route | Visible controls | Required data | Primary action | Loading/empty/error states | Success next state | Failure feedback | Owner issue |
|---|---|---|---|---|---|---|---|---|---|---|
| UI3-TASK-001 | Inspect and continue from a credible product | registration or project list | builder workspace | sample project, preview, prompt, tabs | seeded project detail | interact or submit an increment | preview loading/runtime error | trace then updated preview | durable failed run | T3-SHOWCASE |
| UI3-TASK-002 | Understand exactly what AI did | generation or Trace tab | builder Trace tab | run list, timeline, plan, files, checks | project runs and live phase | select a run | empty/live/succeeded/failed | linked current version | failure reason and retry in chat | T3-TRACE |
| UI3-TASK-003 | Deliver a generated app | Preview toolbar | builder then `/p/{token}` | publish, copy, open | current version and publication | publish current version | publishing/stale/error | stable public URL | workspace error, old publication retained | T3-PUBLISH |
| UI3-TASK-004 | Use a published app without an account | shared URL | `/p/{token}` | generated application controls | anonymous published payload | interact with app | loading/not-found/runtime error | persistent browser interaction | explicit unavailable state | T3-PUBLIC |

## Page Structure

| Page/route | Purpose | Reference | Layout pattern |
|---|---|---|---|
| builder workspace | conversation, trace, code, preview and versions | existing `WorkspacePanel.tsx` tool shell | dense three-column operational workspace |
| `/p/{token}` | run one pinned generated snapshot | existing Sandpack preview runtime | restrained top bar plus full remaining viewport preview, no nested card |

### Frontend Source Local Audit Report

| Audit scope | Finding | Evidence | Required backflow | Blocks frontend contract |
|---|---|---|---|---:|
| Reference | Existing builder, version and Sandpack patterns are concrete sources. | `App.tsx`, `WorkspacePanel.tsx`, `styles.css` | none | no |
| Action trace | All new actions and routes are listed in companion matrices. | action/route matrices | none | no |
