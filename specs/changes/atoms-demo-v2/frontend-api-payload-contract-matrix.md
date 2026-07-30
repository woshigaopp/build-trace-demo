# Frontend API Payload Contract Matrix

| Action ID | Mode/state | Method/path | Canonical body | Allowed keys | Forbidden keys/aliases | Required/default rule | Legacy rule | Exact-key assertion | Owner issue |
|---|---|---|---|---|---|---|---|---|---|
| UI-ACT-001/002 | signed out | `POST /api/auth/register|login` | root | `email,password` | `guestId,ownerId` | both required | none | keys equal email/password | T-FE-AUTH |
| UI-ACT-004 | signed in | `POST /api/projects` | root | `name` | `ownerId,guestId` | nonblank | no guest identity | keys equal name | T-FE-WORKSPACE |
| UI-ACT-005/006 | idle project | `POST /api/projects/{id}/generate` | root | `prompt` | `files,currentHtml,ownerId` | nonblank | server reads current snapshot | keys equal prompt | T-FE-WORKSPACE |
| UI-ACT-008 | dirty snapshot | `POST /api/projects/{id}/versions` | root | `files,summary` | `html,currentHtml,versionNumber,ownerId` | complete nonempty file map | no HTML save | keys equal files/summary | T-FE-CODE |
| UI-ACT-010 | terminal | `POST /api/projects/{id}/versions/{versionId}/restore` | none | none | all body keys | no body | retains POST route | no body | T-FE-VERSION |
