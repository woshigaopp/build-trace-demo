# Frontend Mode Leakage Negative Matrix

| Surface/action | Mode/state | Forbidden DOM/text | Forbidden payload fields | Forbidden route/API | Assertion method | Owner issue |
|---|---|---|---|---|---|---|
| Trace | builder | raw model response, API key, complete prompt context JSON | N/A | public project API | DOM/browser | T3-TRACE |
| Publish | builder | claim that unsaved editor state is published | files, ownerId, token, versionId | public GET as mutation | network/browser | T3-PUBLISH |
| Public page | anonymous | account email, project list, conversation, trace, code/version controls | Authorization header requirement | authenticated detail endpoint | API/browser negative assertions | T3-PUBLIC |
