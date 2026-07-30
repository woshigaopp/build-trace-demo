# Frontend API Payload Contract Matrix

| Action ID | Mode/state | Method/path | Request body canonical path | Allowed keys | Forbidden keys / semantic aliases | Required/nullable/default/derived rule | Legacy compatibility rule | Network exact-key assertion | Owner issue |
|---|---|---|---|---|---|---|---|---|---|
| UI3-ACT-003 | owned project with current version | `POST /api/projects/{id}/publish` | none | none | `versionId`, `files`, `ownerId`, `shareToken` | backend derives current version and token | frontend must not send a body | request has no JSON body | T3-PUBLISH |
| UI3-ACT-006 | public token | `GET /api/public/projects/{token}` | none | none | Authorization requirement, owner/project internals | token comes from pathname | N/A | GET has no body and succeeds anonymously | T3-PUBLIC |
