# Frontend Route Component Matrix

The app intentionally uses one state-driven Vite entry instead of a route library.

| Action ID | Visible action | Source component | Permission guard | Handler | Final route/API | Router definition | Landing component/file | Mode branch | Forbidden inherited UI/API | Verification | Owner issue |
|---|---|---|---|---|---|---|---|---|---|---|---|
| UI-ACT-001/002/003 | 注册/登录/退出 | `frontend/src/App.tsx`, `AuthScreen.tsx` | auth state | auth handlers | `/api/auth/*` or local clear | state-driven in `App.tsx` | `App.tsx` | none | `X-Guest-Id` | auth browser flow | T-FE-AUTH |
| UI-ACT-004/005/006 | 新建/生成/重试 | `ProjectSidebar.tsx`, `ChatPanel.tsx` | authenticated | workspace handlers | `/api/projects*` | state-driven in `App.tsx` | `App.tsx` | none | local-only message success | generation browser flow | T-FE-WORKSPACE |
| UI-ACT-007/008 | 文件/保存 | `WorkspacePanel.tsx` | owned project | Sandpack/save handlers | local active-file or versions POST | state-driven tabs | `WorkspacePanel.tsx` | none | single `<pre>`, `currentHtml` | file browser flow | T-FE-CODE |
| UI-ACT-009/010 | 比较/恢复 | `VersionPanel.tsx` | owned project | compare/restore handlers | version GET/restore POST | state-driven tabs | `VersionPanel.tsx` | none | destructive history rewrite | version browser flow | T-FE-VERSION |

### Action Landing Local Audit Report

All rows specify source, final API/local action, landing file, forbidden v1 behavior and verification. No router or mode branch exists; this is a locked single-mode SPA decision.
