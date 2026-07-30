# Frontend Mode Field Display Matrix

| Surface | Mode/state | Data source | Must show | Must hide | Label | Empty/error state | Fixture ref | Assertion | Owner issue |
|---|---|---|---|---|---|---|---|---|---|
| Auth | signed-out | local | active email/password mode | projects/workspace | 注册/登录 | inline auth error | auth API | workspace absent while signed out | T-FE-AUTH |
| Workspace | signed-in/no project | project list | create prompt | code/version controls | 新建应用 | actionable empty state | empty list | no disabled fake editor | T-FE-WORKSPACE |
| Workspace | active run | project detail/SSE | durable phase, run status | duplicate generate | 生成中/校验中/修复中 | failure + retry | run response | submit disabled; status visible | T-FE-WORKSPACE |
| Code/Preview | current version | files map | every path, full editor, preview | single HTML blob | 代码/预览 | build error visible | version snapshot | all paths selectable; no truncation | T-FE-CODE |
| Versions | 2+ versions | version summaries/details | source, summary, changed files, restore | destructive overwrite wording | 版本 | empty history | snapshots | restore adds version | T-FE-VERSION |

No deployment/runtime mode switch exists. Multi-file React is the only supported generated-app mode, so cross-mode matrices are locked N/A.
