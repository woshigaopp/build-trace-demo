# Frontend Fixture Need Matrix

| Page/action | Fixture needed | State variant | Real contract source | Mock owner | Browser assertion | Negative assertion | Owner issue |
|---|---|---|---|---|---|---|---|
| Trace | deterministic generation results | success, repair success, failure | Java generation service | integration test | complete timeline/details | failed run has no delivered version | T3-TRACE |
| Publish | owned project versions | never/current/stale | Java project service | integration test | stable link and pinned version | unsaved files absent | T3-PUBLISH |
| Public page | published payload | success/not-found | public controller DTO | integration test | preview compiles and interacts | no auth or owner UI | T3-PUBLIC |
