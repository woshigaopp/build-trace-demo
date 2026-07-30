# Frontend Browser Verification Matrix

| User task ID | Action ID | Browser steps | Network assertions | DOM assertions | Screenshot/trace | Negative assertions | Fixture refs | Blocks done |
|---|---|---|---|---|---|---|---|---:|
| UI3-TASK-001 | existing registration | register fresh account, interact with sample, refresh preview | auth then owned project detail | labelled LaunchBoard, rich controls, persisted interaction | desktop/mobile screenshots | no acceptance-test copy | seeded template | yes |
| UI3-TASK-002 | UI3-ACT-001/002 | generate, observe live trace, reload, inspect completed run | phase SSE then detail run metadata | plan/timeline/files/checks/model/duration/version | trace screenshot | no raw JSON or invented compiler pass | deterministic and public prior Qwen path | yes |
| UI3-TASK-003 | UI3-ACT-003/004/005 | publish, copy/open, modify current, republish | bodyless POST; stable token; public GET | current/stale publication state | builder/public screenshots | unsaved/current newer version does not leak before republish | publication integration | yes |
| UI3-TASK-004 | UI3-ACT-006 | open shared URL signed out and interact | anonymous GET 200 | no builder chrome; preview works | public screenshot | no owner/chat/source/run data | public payload | yes |

## Experience Rubric

| User task ID | Task clarity | Form ergonomics | State completeness | Error readability | Mode separation | Route/action closure | Design consistency | Responsive sanity | Required follow-up |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---|
| UI3-TASK-001 | 2 | 2 | 2 | 2 | 2 | 2 | 2 | 2 | none |
| UI3-TASK-002 | 2 | 2 | 2 | 2 | 2 | 2 | 2 | 2 | public browser proof |
| UI3-TASK-003 | 2 | 2 | 2 | 2 | 2 | 2 | 2 | 2 | public browser proof |
| UI3-TASK-004 | 2 | 2 | 2 | 2 | 2 | 2 | 2 | 2 | mobile public proof |

### Frontend Verification Local Audit Report

Every new mutation or route has API/integration and real browser proof. Build/lint alone cannot close any task.
