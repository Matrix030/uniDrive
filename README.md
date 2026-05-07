# University Drive

University Drive is a folder-based assignment sync system for an Intro to Java course project. It replaces the manual Brightspace zip-upload workflow with a local client that watches submission files, uploads them in the background, and syncs assignments and feedback through a Spring Boot server.

This README is the final project report. It includes setup instructions, demo credentials, verification steps, and the advanced topics used in the project.

## API Key Requirement

No external API key is required for this project. Authentication is implemented as a mocked SSO-style login module for the course project, so the first screen asks for an email and password instead of an API key.

Use one of these demo accounts:

| Role | Email | Password | App user ID |
|---|---|---|---|
| Student | `student@nyu.edu` | `password123` | `rvg9395` |
| Student | `rvg9395@nyu.edu` | `password123` | `rvg9395` |
| Instructor | `instructor@nyu.edu` | `password123` | `instructor_rvg0000` |
| Instructor | `rvg0000@nyu.edu` | `password123` | `instructor_rvg0000` |

## Modules

- `unidrive-common`: shared DTOs, hash utility, sync status enum
- `unidrive-server`: Spring Boot REST API + SQLite metadata
- `unidrive-client`: JavaFX client + local SQLite state + background sync

## Advanced Topics Used

### 1. Client-server REST architecture

The project is split into a JavaFX desktop client and a Spring Boot REST server. The client talks to server endpoints through `RestAuthApiClient`, `RestAssignmentApiClient`, `RestSubmissionApiClient`, and `RestFeedbackApiClient`. Shared request/response contracts live in `unidrive-common` DTOs.

### 2. Persistent storage with SQLite

The server stores users, assignments, submissions, and feedback metadata in SQLite. The client also uses local SQLite databases for sync state and received-file tracking, so it can restore dashboard status across app launches.

### 3. Background file synchronization

The client uses Java `WatchService`, polling services, and background sync loops to detect local file changes, upload submissions/published assignments, and download assignments, submissions, and feedback without blocking the UI.

### 4. JavaFX desktop UI

The project includes a role-aware JavaFX interface with login, folder selection, student dashboard, instructor dashboard, status counts, file tables, and a switch-user flow.

### 5. File integrity verification

Uploads include SHA-256 hashes. The server recomputes file hashes and rejects mismatches with a `422` response, preventing corrupted or unexpected file uploads from being accepted.

### 6. Automated smoke testing

The repository includes `scripts/smoke-test.sh`, which builds the project, starts an isolated server, exercises the main API flows, checks expected error cases, verifies downloaded file contents, and runs selected client sync/session tests.

## Implemented Workflow

### Student

1. Client bootstraps a local workspace with:
   - term folders such as `fall2026/`
   - course folders such as `fall2026/daa/`
   - assignment folders created by sync, each with `files/` and `submission/`
   - `sync-state.db`
2. Files added to an assignment's `submission/` folder are detected by `WatchService`.
3. The client records them as `PENDING`, computes SHA-256, uploads them, and transitions through:
   - `UPLOADING`
   - `SYNCED` on success
   - `FAILED` on failure
4. The client polls the server for:
   - assignment files into assignment `files/` folders
   - feedback returned by the instructor

### Instructor

1. Publish an assignment through the server API.
2. Review synced submissions through the submissions API.
3. Return feedback through the server API.

## Quick Start

Requires Java 21+. The project includes Maven Wrapper, so Maven does not need to be installed separately.

From the repository root, first install fresh module artifacts:

```bash
./mvnw clean install
```

This step is important because the client and server both depend on `unidrive-common`.

### Terminal 1: Server

Run the Spring Boot server on port `8080`:

```bash
./mvnw -pl unidrive-server spring-boot:run
```

Wait until you see `Started UniDriveServerApplication`.

### Terminal 2: Client

Run the JavaFX client:

```bash
./mvnw -pl unidrive-client javafx:run
```

To override the server URL:

```bash
./mvnw -pl unidrive-client javafx:run -Dunidrive.serverBaseUrl=http://localhost:8080
```

### First launch flow

1. **Login screen**: enter one of the demo email/password pairs from the API Key Requirement section.
2. **Folder picker**: click Browse and pick a folder for the local workspace.
3. **Dashboard**: the client creates the role-specific subfolders and starts background sync.

### Folders created per role

| Role | Folders |
|---|---|
| Student | `<term>/<course>/<assignment>/files/` for received assignment files, `<term>/<course>/<assignment>/submission/` for student uploads |
| Instructor | `<term>/<course>/<assignment>/publish/` for assignment files, `<term>/<course>/<assignment>/submissions/student_<id>/` for received submissions |

### Subsequent launches

Session is restored from `~/.unidrive/config.properties`, so login and folder picker are skipped on later launches. Delete that file to start over:

```bash
rm -f ~/.unidrive/config.properties
```

To reset the server database:

```bash
rm -f unidrive-server/target/*.db
```

### End-to-end demo

1. Start the server.
2. Start an instructor client and log in with `instructor@nyu.edu` / `password123`.
3. Choose an instructor workspace folder.
4. In the instructor dashboard, create an assignment slot or use an existing publish folder.
5. Drop an assignment file into the assignment's `publish/` folder.
6. Start a student client and log in with `student@nyu.edu` / `password123`.
7. Choose a student workspace folder.
8. Confirm the assignment appears in the student's assignment/received view after sync.
9. Drop a solution file into the student's submission folder.
10. Confirm the student dashboard shows the upload moving toward `SYNCED`.
11. Confirm the instructor receives the student's submission under `Submissions/<studentId>/`.
12. Drop a feedback file into the instructor feedback folder for that student.
13. Confirm the student receives the feedback file after sync.

To run two clients at the same time without sharing saved sessions, use separate fake home directories:

```bash
./mvnw -pl unidrive-client javafx:run -Dunidrive.userHome=target/demo-home/instructor
./mvnw -pl unidrive-client javafx:run -Dunidrive.userHome=target/demo-home/student
```

## Build & Test

```bash
./mvnw test           # run all unit and integration tests
./mvnw clean package  # build all module jars
```

## Smoke Test

Run the automated smoke test before a manual demo:

```bash
./scripts/smoke-test.sh
```

If port `18080` is busy, run it on another port:

```bash
UNIDRIVE_SMOKE_PORT=18081 ./scripts/smoke-test.sh
```

The smoke test installs fresh module artifacts, runs selected client workspace/session/sync checks, starts an isolated Spring Boot server, writes all temporary files under `target/smoke/`, and stops the server automatically when finished.

It verifies:

- student and instructor mock login success
- login errors for wrong password, unknown email, blank email, and blank password
- assignment publish, list, download, delete, and missing-assignment errors
- submission upload, list, filtered list, download, delete, SHA-256 mismatch, and missing-submission errors
- feedback upload, list, download, missing-submission, and missing-feedback errors
- downloaded assignment, submission, and feedback contents match the uploaded files
- selected client workspace, session, dashboard snapshot, and sync-service tests still pass

Useful options:

```bash
UNIDRIVE_SMOKE_SKIP_BUILD=1 ./scripts/smoke-test.sh          # reuse existing build artifacts
UNIDRIVE_SMOKE_CLEAN_BUILD=1 ./scripts/smoke-test.sh         # force clean install first
UNIDRIVE_SMOKE_SKIP_CLIENT_TESTS=1 ./scripts/smoke-test.sh   # only run server/API smoke checks
```

## Key API Endpoints

### Auth

- `POST /api/v1/auth/login`

### Submissions

- `POST /api/v1/submissions/{term}/{course}/{assignmentId}`
- `GET /api/v1/submissions?term=...&course=...&assignmentId=...`
- `GET /api/v1/submissions?term=...&course=...&assignmentId=...&studentId=...`
- `GET /api/v1/submissions/{submissionId}/download`
- `DELETE /api/v1/submissions/{submissionId}`

### Assignments

- `POST /api/v1/instructor/assignments/{term}/{course}/{assignmentId}`
- `GET /api/v1/assignments?term=...&course=...`
- `GET /api/v1/assignments/{assignmentId}/download?fileName=...`
- `DELETE /api/v1/instructor/assignments/{assignmentId}?fileName=...`

### Feedback

- `POST /api/v1/instructor/feedback/{submissionId}`
- `GET /api/v1/feedback?studentId=...`
- `GET /api/v1/feedback/{feedbackId}/download`

## Testing

The project uses TDD and currently verifies:

- hashing
- shared DTO contracts
- server upload/list/download flows
- assignment publish/list/download flows
- feedback upload/list/download flows
- client sync-state persistence
- workspace bootstrap
- submission watcher behavior
- local sync-state transitions
- upload client and upload transitions
- background sync loop
- assignment and feedback polling/download services
- client runtime startup lifecycle

Run all tests with:

```bash
./mvnw test
```
