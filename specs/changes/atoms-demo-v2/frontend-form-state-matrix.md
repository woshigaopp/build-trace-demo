# Frontend Form State Matrix

| Form/step | Mode/state | Active fields | Inactive/hidden fields | Default/reset rule | Validation trigger | Submit participation | Error location |
|---|---|---|---|---|---|---|---|
| Auth | register | email, password | none | preserve on failure; clear on success | submit | both required | below form |
| Auth | login | email, password | none | preserve on failure; clear on success | submit | both required | below form |
| New project | signed-in | initial prompt | none | clear after accepted create | submit | prompt derives name and first generation | workspace banner |
| Chat | owned project/idle | prompt | none | clear only after run accepted | submit | prompt only | composer/banner and durable failed message |
| Chat | active run | none | prompt disabled | retain draft | no submit | excluded | phase surface |
| Manual save | dirty files | complete files map | no current version means hidden | reset to server files after success | save click | complete map | workspace banner |

### Submit Flow Local Audit Report

Each mutation has a visible control, exact API in the payload matrix, success transition and failure location. Hidden fields do not exist; disabled generation input cannot submit.
