# Frontend Action Inventory

| Action ID | Action | User intent | Reachable from | Side effect/API called | Success behavior | Failure behavior | Owner issue |
|---|---|---|---|---|---|---|---|
| UI-ACT-001 | Register | create identity | auth/register | `POST /api/auth/register` | store token, load workspace | inline error, keep values | T-FE-AUTH |
| UI-ACT-002 | Login | recover identity | auth/login | `POST /api/auth/login` | store token, load projects | inline error | T-FE-AUTH |
| UI-ACT-003 | Logout | leave account | account menu | clear token/client state | auth screen | N/A local action | T-FE-AUTH |
| UI-ACT-004 | Create project | start workspace | sidebar/empty state | `POST /api/projects` | select empty project | banner error | T-FE-WORKSPACE |
| UI-ACT-005 | Generate | build/modify app | chat composer | `POST /api/projects/{id}/generate` SSE | refresh detail/current snapshot | durable failed run + retry | T-FE-WORKSPACE |
| UI-ACT-006 | Retry | repeat failed intent | failed message/run | same generate API, same prompt | new run | new durable failure | T-FE-WORKSPACE |
| UI-ACT-007 | Select file | inspect full source | Code file tree | Sandpack `setActiveFile` | editor shows full source | missing file selects first valid file | T-FE-CODE |
| UI-ACT-008 | Save files | persist manual edit | Code toolbar | `POST /api/projects/{id}/versions` | new current version, dirty clears | error banner, edits retained | T-FE-CODE |
| UI-ACT-009 | Compare | understand change | Versions list | `GET /api/projects/{id}/versions/{versionId}` | changed-file panel | inline error | T-FE-VERSION |
| UI-ACT-010 | Restore | recover old state | Versions list | `POST .../versions/{versionId}/restore` | new current version | error, current remains | T-FE-VERSION |
