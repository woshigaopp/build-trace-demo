# Frontend Mode Field Display Matrix

| Surface | Mode/state | Data source | Must show | Must hide | Label/i18n | Empty/error state | Fixture ref | Assertion | Owner issue |
|---|---|---|---|---|---|---|---|---|---|
| Trace tab | live run | SSE phase plus persisted run | current phase, safe refresh, received progress | raw JSON and fake passed checks | Chinese product copy | queued/live state | generation integration | phase visible, raw output absent | T3-TRACE |
| Trace tab | terminal run | `ProjectDetail.runs` | understanding, plan, timeline, changed files, actual checks, model/duration/version | secrets and owner data | Chinese product copy | explicit empty/failed state | persisted run | reload retains all trace sections | T3-TRACE |
| Preview toolbar | current publication | `ProjectDetail.publication` | publish status, pinned version, copy/open | owner token before publish | Chinese product copy | API error banner | publication integration | stale/current state is distinguishable | T3-PUBLISH |
| Public route | published | public DTO | name, pinned version, full preview | sidebar, chat, trace, source, owner email | Chinese product copy | loading/not-found/error | public API | anonymous runtime only | T3-PUBLIC |
