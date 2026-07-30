# Atoms Demo v2 Source Intake Ledger

## Source Inventory

| Source ID | Source | Type | Status | Read method | Behavior impact |
|---|---|---|---|---|---|
| SRC-001 | DeepWisdom assessment requirements: interactive demo, persistence, primary flow, extension, public URL | user-provided assessment | read | conversation and handoff | Defines the minimum delivery contract. |
| SRC-002 | `docs/deepwisdom-atoms-assessment-handoff.md` in the AutoMQBox worktree | user-provided handoff | read | `sed` | Records company context, original scope, public-work research and v1 rationale. |
| SRC-003 | Reviewer feedback after the first submission | user-provided runtime evidence | read | conversation | Authoritative evidence that guest identity, unstable incremental generation, partial conversations and single-HTML code management fail acceptance. |
| SRC-004 | Current `origin/main` source | codebase | read | `rg`, `sed`, Maven/frontend manifests | Establishes v1 behavior and migration constraints. |
| SRC-005 | Current public v1 deployment | runtime | read | prior browser/API acceptance in this task | Establishes that the LLM and deployments work, but does not override reviewer failures. |
| SRC-006 | User statement `允许补交...这是最后一个机会` | user direction | read | conversation | Raises the completion bar from code-complete to public continuous-scenario acceptance. |
| SRC-007 | User statement `授权` | decision authority | read | conversation | Authorizes AI to lock product and engineering choices for v2. |
| SRC-008 | Sandpack official docs and npm metadata | official third-party evidence | read | official site, GitHub README, `npm view` | Confirms browser-based editable multi-file React sandboxes; latest stable version observed is 2.20.0. |

## Source To Semantic Object Map

| Source | Semantic objects |
|---|---|
| SRC-001 | REQ-001..REQ-010, SCN-001..SCN-009 |
| SRC-002 | Product context, v1 non-goals, PDEC-001 alternatives |
| SRC-003 | REQ-001, REQ-004..REQ-010, PDEC-001..PDEC-006 |
| SRC-004 | Current Product/Code Understanding, migration constraints |
| SRC-005 | Deployment baseline and regression constraint |
| SRC-006 | VER-001..VER-009 public acceptance requirement |
| SRC-007 | Product decision authority for all active PDEC rows |
| SRC-008 | PDEC-003 and ADEC-004 external runtime choice |

## Source Conflict Matrix

| Conflict | Earlier source | Later evidence | Resolution | Status |
|---|---|---|---|---|
| Guest identity versus registration | SRC-002 accepted guest mode for the time box | SRC-003 explicitly rejects missing registration/login | Real account registration/login is required; guest mode is removed | locked |
| Single HTML versus code workspace | SRC-002 explicitly excluded arbitrary multi-file projects | SRC-003 rejects single-HTML and incomplete code display | Multi-file React project becomes P0; arbitrary backend generation remains out of scope | locked |
| Full rewrite simplicity versus incremental stability | SRC-002 chose full HTML rewrite | SRC-003 reports requests that never succeed | Structured file operations, candidate validation and one repair attempt replace full rewrite | locked |

No behavior-affecting source remains unread or conflicted.
