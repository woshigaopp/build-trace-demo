# Frontend Fixture Need Matrix

| Page/action | Fixture needed | State variant | Real source | Mock owner | Browser assertion | Negative assertion | Owner issue |
|---|---|---|---|---|---|---|---|
| Auth | two unique accounts | success/duplicate/wrong password | auth API contract | backend integration tests | state switches correctly | no workspace before auth | T-FE-AUTH |
| Generate | deterministic fake operations | success/repair/failure | generation contract | backend test fake client | states terminate | failed files never preview | T-FE-WORKSPACE |
| Code | multi-file snapshot | clean/dirty/save error | version contract | service fixtures | all files visible | no truncation | T-FE-CODE |
| Versions | 3 snapshots | compare/restore | version contract | service fixtures | changed files/restore | old snapshot unchanged | T-FE-VERSION |
