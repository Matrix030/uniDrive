# University Drive — CLAUDE.md

## Project Overview

**University Drive** is a multithreaded Java file synchronization system for NYU Tandon CS6103 (Intro to Java). It replaces the manual Brightspace submission workflow with a native folder-based system where files dropped into local directories are automatically hashed, validated, and synced to a central server.

**Team:** Rishikesh Gharat (rvg9395), Omkar Waikar (ow2130)  
**Course:** CS6103 — Introduction to Java, NYU Tandon  
**Deadline:** May 10, 2026

---

## Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Database | SQLite via JDBC |
| GUI | JavaFX |
| Networking | Spring Boot (REST API) |
| Concurrency | Java WatchService API + Threads |
| File Integrity | SHA-256 hashing |
| Build | Maven |
| Testing | JUnit 5 |

---

## Architecture

**Client-Server.** Two separate runnable modules in one Maven multi-module project.

```
unidrive/
├── unidrive-server/        # Spring Boot REST API + SQLite
├── unidrive-client/        # JavaFX GUI + WatchService sync
└── unidrive-common/        # Shared DTOs, hash utils, constants
```

### Server responsibilities
- Store files and metadata (assignment, submission, feedback) in SQLite
- REST endpoints for upload, download, list, and status
- Validate SHA-256 hash on every received file before marking upload complete (atomic submission)
- Organize submissions per student per assignment

### Client responsibilities
- On first launch: prompt user to choose a local root folder; create `/Assignments`, `/Submissions`, `/Feedback` subdirectories
- Background `SyncService` thread using `WatchService` monitors all three folders
- On file drop into `/Submissions`: compute SHA-256 → upload → confirm hash with server → mark synced
- Poll server for new items in `/Assignments` and `/Feedback`; download and place locally
- JavaFX UI thread shows sync status, recent activity log, connection indicator

---

## Roles

### Student
- `/Assignments` — read-only; populated by server when professor publishes
- `/Submissions` — drop files here; client auto-hashes, validates, and uploads
- `/Feedback` — read-only; populated when professor returns graded work

### Professor / TA
- `/Publish` — drop assignment files here; server fans out to all enrolled students
- `/Submissions/<student_id>/` — synced locally for grading with existing tools
- `/Feedback/<student_id>/` — drop graded file here; server syncs back to that student

---

## Key Implementation Requirements

### Concurrency (mandatory demo criterion)
- `SyncService` runs in a dedicated daemon thread, never on the JavaFX Application Thread
- `WatchService` loop is non-blocking with a short poll timeout so the thread stays interruptible
- File uploads run in a thread pool (`ExecutorService`) so multiple files upload concurrently
- Use `ReentrantLock` or `synchronized` blocks around shared state (upload queue, status map)
- JavaFX UI updates always go through `Platform.runLater(...)`

### Hashing / Integrity
- SHA-256 computed client-side before upload; sent as a request header or body field
- Server recomputes SHA-256 on received bytes and rejects mismatches with HTTP 422
- Only after server confirms hash match is the file marked `SYNCED` in local DB/state
- Hash stored in SQLite on both sides for deduplication and change detection

### Atomic Submission
- A submission is only "submitted" when the server returns 200 with a confirmed hash
- Partial uploads (network drop mid-transfer) are deleted server-side and retried by client
- Client keeps a local SQLite table (`sync_state`) tracking each file's status: `PENDING | UPLOADING | SYNCED | FAILED`

### Network / REST Conventions
- Base URL configured in `application.properties` (client side), default `http://localhost:8080`
- Endpoints follow `/api/v1/{role}/{action}` pattern
- Multipart upload: `POST /api/v1/submissions/{assignmentId}` with file + SHA-256 header
- JSON responses use a consistent envelope: `{ "status": "ok"|"error", "data": {...}, "message": "" }`

---

## Database Schema (SQLite)

### Server-side (`unidrive-server`)
```sql
CREATE TABLE users (id TEXT PRIMARY KEY, name TEXT, role TEXT); -- role: STUDENT | INSTRUCTOR
CREATE TABLE assignments (id TEXT PRIMARY KEY, title TEXT, published_at INTEGER, file_path TEXT, hash TEXT);
CREATE TABLE submissions (id TEXT PRIMARY KEY, assignment_id TEXT, student_id TEXT, file_path TEXT, hash TEXT, submitted_at INTEGER, status TEXT);
CREATE TABLE feedback (id TEXT PRIMARY KEY, submission_id TEXT, file_path TEXT, hash TEXT, returned_at INTEGER);
```

### Client-side (`unidrive-client`)
```sql
CREATE TABLE sync_state (local_path TEXT PRIMARY KEY, remote_id TEXT, sha256 TEXT, status TEXT, last_synced INTEGER);
```

---

## Project Milestones

| Phase | Tasks | Dates |
|---|---|---|
| Initialization | Architecture decisions, Maven setup, skeleton modules | Apr 3–12 |
| Initial Approach | WatchService + thread pool, file locking, SHA-256 hashing | Apr 13–26 |
| Execution | JavaFX UI, REST API integration, end-to-end testing | Apr 27–May 4 |
| Finalization | Bug fixes, refactor, documentation | May 5–10 |

**Current date: Apr 16** — in the Initial Approach phase.

---

## Testing Strategy

- **Unit tests (JUnit 5):** hash utility, sync state machine, REST request builders
- **Mock Mode flag:** `--mock` CLI arg or env var `UNIDRIVE_MOCK=true` makes `SyncService` simulate network latency (random 200–2000 ms delay) and random failure (10% drop rate) without a real server
- **Functional test:** drop a file in `/Submissions`; assert it appears in professor's synced folder within a configurable timeout
- **UI responsiveness test:** upload a 50 MB file; assert JavaFX UI remains interactive (no ANR)

---

## Build & Run

```bash
# Build all modules
mvn clean package

# Run server (default port 8080)
java -jar unidrive-server/target/unidrive-server.jar

# Run client (student mode)
java -jar unidrive-client/target/unidrive-client.jar --role=student --user=rvg9395

# Run client in mock mode (no server needed)
java -jar unidrive-client/target/unidrive-client.jar --role=student --user=rvg9395 --mock
```

---

## Conventions

- Package root: `edu.nyu.unidrive`
- Sub-packages: `server`, `client`, `common`
- Class names: `SyncService`, `FileHasher`, `WatchServiceMonitor`, `SubmissionController`, `AssignmentController`
- No Lombok — keep it plain Java for a course project
- All file I/O through `java.nio.file` (not `java.io.File`)
- Log with `java.util.logging` (no external logging framework)
- No external HTTP client libraries — use Spring's `RestTemplate` on the client side (already a Spring dependency)
