# Frontend Browser Verification Matrix

| User task | Action | Browser steps | Network assertions | DOM assertions | Screenshot/trace | Negative assertions | Fixture refs | Blocks done |
|---|---|---|---|---|---|---|---|---|---:|
| UI-TASK-001 | UI-ACT-001..003 | register, logout, login | auth payload exact keys; Bearer on project APIs | account email then auth form then recovered workspace | desktop/mobile screenshots | no guest header/workspace leak | live public API | yes |
| UI-TASK-002 | UI-ACT-004..006 | create and perform 5-10 prompts | every run reaches terminal response/status | ordered chat and current version advance | trace/screenshot | no duplicate submit or endless spinner | live LLM | yes |
| UI-TASK-003 | UI-ACT-007/008 | select each file, edit, save, interact with preview | save body has files/summary only | full editor, dirty state, working preview | screenshot | no single HTML/truncation | live snapshot | yes |
| UI-TASK-004 | UI-ACT-009/010 | compare old version and restore | detail GET then bodyless restore POST | changed files; version count increments | screenshot | current is not destructively replaced | live history | yes |

## Experience Rubric

| User task | Task clarity | Form ergonomics | State completeness | Error readability | Mode separation | Route/action closure | Design consistency | Responsive sanity | Follow-up |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---|
| UI-TASK-001 | 2 | 2 | 2 | 2 | 2 | 2 | 2 | 2 | none |
| UI-TASK-002 | 2 | 2 | 2 | 2 | 2 | 2 | 2 | 2 | verify public LLM latency |
| UI-TASK-003 | 2 | 2 | 2 | 2 | 2 | 2 | 2 | 2 | verify Sandpack mobile framing |
| UI-TASK-004 | 2 | 2 | 2 | 2 | 2 | 2 | 2 | 2 | none |

### Frontend Verification Local Audit Report

All mutation flows require public browser network + DOM proof; build/lint alone cannot close them. Runtime bundle freshness is proved by deployed commit/version and visible v2 auth/workspace markers.
