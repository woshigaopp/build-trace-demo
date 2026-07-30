# Frontend Form State Matrix

No new form is introduced. Existing auth and prompt form contracts remain unchanged.

| Form/step | Mode/state | Active fields | Inactive/hidden fields | Default/reset rule | Validation trigger | Submit participation | Error location |
|---|---|---|---|---|---|---|---|
| Publish command | current stored version | none | editor buffer, version selector | backend derives current version | currentVersionId required | bodyless POST | workspace error banner |

### Submit Flow Local Audit Report

Publish is the only new mutation. Its action, bodyless request, derived version, success state and non-destructive failure are fully specified.
