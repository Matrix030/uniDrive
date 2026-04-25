# University Drive Progress

## Current State

- Project type: Intro to Java course project
- Development style: Test Driven Development
- Build command: `./mvnw test`
- Current test status: passing
- Current branch: `main`
- Current local head: `7262e34` `Add assignment and feedback client synchronization`

## Progress Chart

| Area | Status | Notes |
|---|---|---|
| Maven multi-module scaffold | Done | Root project, `unidrive-common`, `unidrive-client`, `unidrive-server`, Maven wrapper |
| Shared hashing utility | Done | `FileHasher` with tests |
| Shared API envelope | Done | `ApiResponse<T>` with tests |
| Shared sync status enum | Done | `PENDING`, `UPLOADING`, `SYNCED`, `FAILED` |
| Server upload endpoint | Done | `POST /api/v1/submissions/{assignmentId}` |
| Server hash validation | Done | Returns `422` on mismatch |
| Server SQLite schema | Done | `schema.sql` initializes core tables |
| Server submission persistence | Done | Verified uploads stored in `submissions` table |
| Server submission listing | Done | `GET /api/v1/submissions?assignmentId=...` |
| Server student filter | Done | Optional `studentId` filter supported |
| Server submission download | Done | `GET /api/v1/submissions/{submissionId}/download` |
| Missing submission download handling | Done | Returns `404` |
| Client sync-state SQLite repository | Done | Save/load/update local `sync_state` |
| Client workspace bootstrap | Done | Creates `Assignments`, `Submissions`, `Feedback`, `sync-state.db` |
| Client submissions watcher | Done | Watches `/Submissions`, ignores directories, coalesces file events |
| Client local pending-state transitions | Done | File events recorded as `PENDING` in local `sync_state` |
| Client HTTP upload service | Done | Multipart upload via client API client with SHA-256 header |
| Client upload success/failure transitions | Done | Upload service sets `UPLOADING`, then `SYNCED` or `FAILED` |
| Background sync service | Done | Interruptible loop polls watcher, records `PENDING`, schedules uploads |
| JavaFX app wiring to real services | Done | App bootstraps workspace, starts sync loop, and shuts it down on exit |
| JavaFX sync status and activity view | Done | UI refreshes local sync state counts and tracked file rows |
| Assignment download flow | Done | Server publish/list/download endpoints plus client polling/download service |
| Feedback sync flow | Done | Server upload/list/download endpoints plus client polling/download service |
| Instructor workflow | Done | Instructor can publish assignments and return feedback through server APIs |
| README / run guide | Done | Project setup, run commands, and demo flow documented |

## Completed Slices

1. Project scaffold and TDD foundation
2. Shared common utilities and DTOs
3. Server-side upload with SHA-256 validation
4. Server-side SQLite persistence
5. Server-side submission retrieval and download
6. Client-side local workspace and sync-state persistence
7. Client-side file watching and pending-state recording
8. Client-side HTTP upload and upload-state transitions
9. Client-side background sync loop
10. JavaFX startup wiring to real client services
11. JavaFX sync status/activity display
12. Assignment and feedback remote synchronization
13. Final run/demo documentation

## Latest Verified Behavior

### Server

- Upload a submission with multipart file, `studentId`, and `X-File-Sha256`
- Reject mismatched hashes with `422`
- Persist valid submission metadata to SQLite
- Publish assignments through `/api/v1/instructor/assignments`
- List and download assignments through `/api/v1/assignments`
- List submissions by assignment
- List submissions by assignment plus student
- Download a stored submission by ID
- Upload feedback through `/api/v1/instructor/feedback/{submissionId}`
- List feedback by student and download returned feedback files
- Return `404` for unknown submission download

### Client

- Bootstrap local folders and local SQLite state database
- Persist local sync-state rows
- Watch `/Submissions` for file create/modify events
- Record watched files as `PENDING` in local `sync_state`
- Compute SHA-256 for a local submission file before upload
- Upload a submission to the server with multipart + `X-File-Sha256`
- Mark uploaded files as `SYNCED` on success
- Mark uploaded files as `FAILED` on client/API failure
- Poll submission events in a background sync loop
- Schedule uploads from the sync loop without blocking the watcher loop
- Start the real client runtime from JavaFX startup
- Stop the running sync loop during JavaFX shutdown
- Display the workspace root, student, assignment, and server in the JavaFX window
- Refresh sync-state counts in the JavaFX UI
- Display tracked file rows with status, remote ID, hash, and last synced time
- Poll and download new assignments into `/Assignments`
- Poll and download feedback into `/Feedback`

## Important Uncommitted Work

As of this file creation, the worktree contains uncommitted changes.

### Intended project changes

- `PROJECT_PROGRESS.md`
- `README.md`

These documentation updates summarize the now-working full project flow.

### Unrelated or decision-needed files

- `.gitignore`
- `AGENTS.md`
- `university_drive_project_proposal.md`

Do not commit those unless intentionally desired.

## Recommended Next Step

The core project implementation is complete. The remaining work is optional polish.

### Goal

If you continue, focus on polish rather than missing architecture.

### Suggested order

1. improve JavaFX layout and activity messaging
2. add stronger end-to-end demo tests if desired
3. add optional configuration UI instead of startup system properties
4. add packaging/demo script polish

### Minimal classes likely needed next

- optional JavaFX view models or settings UI
- optional demo helpers or packaging scripts

## Handoff Notes For Other Agents

- Prefer small vertical slices
- Keep changes course-appropriate and plain Java where possible
- Continue using Red -> Green -> Refactor
- Verify with `./mvnw test` after each slice
- The current best pickup point is polish, not missing core workflow
