# Implementation Plan

1. Extend generation output and durable run state with understanding, plan, trace events, changed files, checks and delivered version.
2. Add a deterministic, explicitly labelled LaunchBoard showcase snapshot and seed it transactionally after account registration.
3. Add owner publish and anonymous public read APIs that pin immutable versions behind a stable token.
4. Add Trace workspace, live phase handoff, publish controls and a public Sandpack route.
5. Improve the model contract and starter quality rules without spending another production model request during development.
6. Prove persistence, isolation, stable publication, trace/repair behavior, frontend build quality and real browser flows.

Risk order: persistence and public access contracts land before UI consumers. Publication never reads mutable editor state; it only pins a validated stored version.
