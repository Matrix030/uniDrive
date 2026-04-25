# University Drive Progress

## Current State

- Project type: Intro to Java course project
- Development style: Test Driven Development
- Build command: `./mvnw test`
- Current test status: passing
- Current branch: `main`
- Latest pushed commit: `a47db46` `Add submission retrieval and client workspace setup`

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
| Assignment download flow | Not started | Future server/client slice |
| Feedback sync flow | Not started | Future server/client slice |
| Instructor workflow | Not started | Future server/client slice |

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

## Latest Verified Behavior

### Server

- Upload a submission with multipart file, `studentId`, and `X-File-Sha256`
- Reject mismatched hashes with `422`
- Persist valid submission metadata to SQLite
- List submissions by assignment
- List submissions by assignment plus student
- Download a stored submission by ID
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

## Important Uncommitted Work

As of this file creation, the worktree contains uncommitted changes.

### Intended project changes

- `PROJECT_PROGRESS.md`
- `unidrive-client/src/main/java/edu/nyu/unidrive/client/`
- `unidrive-client/src/main/java/edu/nyu/unidrive/client/net/`
- `unidrive-client/src/main/java/edu/nyu/unidrive/client/sync/`
- `unidrive-client/src/main/java/edu/nyu/unidrive/client/storage/`
- `unidrive-client/src/test/java/edu/nyu/unidrive/client/`
- `unidrive-client/src/test/java/edu/nyu/unidrive/client/net/`
- `unidrive-client/src/test/java/edu/nyu/unidrive/client/sync/`
- `unidrive-client/src/test/java/edu/nyu/unidrive/client/storage/`
- `unidrive-client/pom.xml`

These contain the current client upload, watcher, background sync, runtime startup wiring, status dashboard, and local transition work that already passes `./mvnw test` locally.

### Unrelated or decision-needed files

- `.gitignore`
- `AGENTS.md`
- `university_drive_project_proposal.md`

Do not commit those unless intentionally desired.

## Recommended Next Step

Start the assignment download flow.

### Goal

Let the student client receive assignments into `/Assignments` so the two-way course workflow can begin.

### Suggested order

1. Add failing server tests for listing/downloading assignments
2. Implement minimal assignment persistence and endpoints on the server
3. Add a client assignment API client and polling/downloading service
4. Save assignment files into `/Assignments`
5. Surface new assignments in the JavaFX UI

### Minimal classes likely needed next

- `unidrive-server/.../AssignmentController`
- `unidrive-client/.../net/AssignmentApiClient`
- `unidrive-client/.../sync/AssignmentSyncService`

## Handoff Notes For Other Agents

- Prefer small vertical slices
- Keep changes course-appropriate and plain Java where possible
- Continue using Red -> Green -> Refactor
- Verify with `./mvnw test` after each slice
- The current best pickup point is assignment download flow, starting server-side test-first
