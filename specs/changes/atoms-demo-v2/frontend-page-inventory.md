# Frontend Page Inventory

## User Task Contract

| User task ID | User goal | Entry points | Page/route | Visible controls | Required data | Primary action | Loading/empty/error states | Success next state | Failure feedback | Owner issue |
|---|---|---|---|---|---|---|---|---|---|---|
| UI-TASK-001 | Enter a durable account | initial load/logout/401 | single-page auth state | email, password, login/register segmented control, submit | auth mode | register/login | submitting, field/server error | workspace | inline form error | T-FE-AUTH |
| UI-TASK-002 | Create and iterate on an app | authenticated workspace | single-page workspace | new project, chat input, generate/retry | projects, detail, active run | generate | list/detail/generation empty/loading/error | current version workspace | persisted failed run + banner | T-FE-WORKSPACE |
| UI-TASK-003 | Inspect and edit code | generated project | workspace Code tab | file tree, editor, save | current files/version | save files | no-version, dirty, saving, save error | new version + preview | workspace error | T-FE-CODE |
| UI-TASK-004 | Compare or restore history | generated project | workspace Versions tab | version rows, compare, restore | versions and selected snapshot | restore | empty/loading/error | new current version | inline error | T-FE-VERSION |

## Page Structure

| Page/route | Purpose | Reference | Layout pattern |
|---|---|---|---|
| App auth state | authenticate before any project access | current `frontend/src/App.tsx` app shell styling | centered compact auth surface, no marketing landing |
| App workspace state | project/chat/code/preview/history | existing `App.tsx`, `ProjectSidebar`, `ChatPanel`, `PreviewPanel` | project rail + conversation + primary work area; responsive stacked mobile |

### Frontend Source Local Audit Report

| Audit scope | Finding | Evidence | Required backflow | Blocks frontend contract |
|---|---|---|---|---:|
| Reference | Existing workspace is a concrete source; retain its quiet product-tool composition. | listed component paths | none | no |
| Action trace | Every v2 visible action is mapped in route and action matrices. | companion artifacts | none | no |
