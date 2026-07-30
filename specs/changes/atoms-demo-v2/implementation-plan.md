# Implementation Plan

1. Add Spring Security authentication and principal-scoped project access.
2. Introduce durable generation runs and multi-file immutable version snapshots while retaining legacy database columns.
3. Replace HTML extraction with structured operation parsing, candidate validation and one repair attempt.
4. Replace guest frontend boot with auth state and token-aware API handling.
5. Replace single HTML code/preview with a complete multi-file Sandpack workspace, manual save, compare and restore.
6. Add backend integration tests, frontend build/lint tests and public browser acceptance.

Risk order is intentional: identity and storage contracts are implemented before generation/UI consumers so no later layer invents ownership or snapshot semantics.
