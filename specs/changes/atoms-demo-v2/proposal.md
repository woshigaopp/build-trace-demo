# BuildTrace Atoms Demo v2 Proposal

## Why

The first submission proved that the model, persistence and deployment topology work, but reviewer evidence shows that it is not yet a complete product: identity is anonymous, incremental generation can fail indefinitely, failed conversations disappear, and code management is a single incomplete HTML view. The final resubmission must fix these system properties rather than add decorative features.

## In Scope

- Email/password registration, login, logout and account recovery by login.
- Account-scoped projects and all project mutations.
- Multi-file React generation and incremental modification.
- Durable messages, generation runs, statuses, errors, versions and files.
- Candidate validation, one automatic repair attempt and atomic commit.
- Editable file workspace, browser preview, versions, changed-file diff and restore.
- Public deployment plus continuous end-to-end acceptance.

## Non-Goals

- OAuth, password reset email, organizations, sharing or collaboration.
- Arbitrary Java/backend application generation or deployment of generated apps.
- A full WebContainer, shell access, package installation chosen by the user or server-side builds.
- Multi-agent theater, Race mode, billing, custom domains or generated-app publishing.

## Success Definition

The work is complete only when a new evaluator can register on the public URL, create a multi-file app, perform at least five consecutive modifications, observe a recoverable forced failure, refresh/relogin without losing the conversation, inspect and edit complete files, restore a version, and use the generated preview.

## Decision Document Index

- [PRD decisions](decision-reviews/prd-decisions.md)
- [Product specification](spec.md)
- [Code scope](code-scope-discovery.md)
- [Decision surfaces](decision-surface-discovery.md)
