# Verification Matrix

| VER | Requirement/scenario | Automated evidence | Runtime evidence | Pass condition |
|---|---|---|---|---|
| VER-001 | REQ-001 / SCN-001 | auth integration test | public register/logout/login | same project list recovered |
| VER-002 | REQ-002 / SCN-002 | cross-user controller/service test | direct foreign project request | 404, no payload leakage |
| VER-003 | REQ-004 / SCN-003 | parser/candidate/service test | public initial generation | multi-file interactive preview |
| VER-004 | REQ-005 / SCN-004 | five-operation sequential fake-model integration | one real-model generation plus browser iteration | every automated run reaches terminal state; real stream closes |
| VER-005 | REQ-007 / SCN-005 | invalid-output + invalid-repair test | controlled failure if safely triggerable | repair once; current version unchanged on failure |
| VER-006 | REQ-006 / SCN-006 | run/message persistence integration | refresh and relogin | complete ordered history |
| VER-007 | REQ-008/009 / SCN-007 | frontend build | inspect/edit/save/preview | full file content and working interaction |
| VER-008 | REQ-010 / SCN-008 | restore immutability integration | compare and restore | new version created; old unchanged |
| VER-009 | SCN-009 | clean build and health checks | public desktop/mobile browser | reviewer needs only URL and credentials they create |

No row may be marked passed until its command or browser evidence has actually been executed.

## Executed Evidence

| VER | Result | Evidence |
|---|---|---|
| VER-001/002 | passed | `AuthControllerIntegrationTest`; local browser register/logout/login/recovery |
| VER-003 | passed | structured parser/candidate tests; real `qwen-plus` returned five files; Sandpack preview interaction |
| VER-004/005 | passed | `GenerationLifecycleIntegrationTest`: five serial successes, repair success, repair failure atomicity |
| VER-006 | passed | service integration plus relogin recovered complete message/run history |
| VER-007/008 | passed | browser edit/save/compare/restore; v3-to-v4 diff contained only `/App.jsx` |
| VER-009 | passed | Vercel/Railway/Neon public desktop and 390x844 browser evidence; health UP; detail 1.32s |
