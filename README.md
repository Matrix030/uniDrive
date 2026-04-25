# University Drive

University Drive is a folder-based assignment sync system for an Intro to Java course project. It replaces the manual Brightspace zip-upload workflow with a local client that watches submission files, uploads them in the background, and syncs assignments and feedback through a Spring Boot server.

## Modules

- `unidrive-common`: shared DTOs, hash utility, sync status enum
- `unidrive-server`: Spring Boot REST API + SQLite metadata
- `unidrive-client`: JavaFX client + local SQLite state + background sync

## Implemented Workflow

### Student

1. Client bootstraps a local workspace with:
   - `Assignments/`
   - `Submissions/`
   - `Feedback/`
   - `sync-state.db`
2. Files added to `Submissions/` are detected by `WatchService`.
3. The client records them as `PENDING`, computes SHA-256, uploads them, and transitions through:
   - `UPLOADING`
   - `SYNCED` on success
   - `FAILED` on failure
4. The client polls the server for:
   - assignments into `Assignments/`
   - feedback into `Feedback/`

### Instructor

1. Publish an assignment through the server API.
2. Review synced submissions through the submissions API.
3. Return feedback through the server API.

## Build

```bash
./mvnw test
```

## Run Server

```bash
java -jar unidrive-server/target/unidrive-server.jar
```

If you have not packaged the jars yet, you can build first with:

```bash
./mvnw package
```

## Run Client

The client currently uses system properties for a simple course-project startup path:

```bash
java \
  -Dunidrive.root=demo-workspace/student-rvg9395 \
  -Dunidrive.studentId=rvg9395 \
  -Dunidrive.assignmentId=assignment-1 \
  -Dunidrive.serverBaseUrl=http://localhost:8080 \
  -jar unidrive-client/target/unidrive-client.jar
```

## Key API Endpoints

### Submissions

- `POST /api/v1/submissions/{assignmentId}`
- `GET /api/v1/submissions?assignmentId=...`
- `GET /api/v1/submissions?assignmentId=...&studentId=...`
- `GET /api/v1/submissions/{submissionId}/download`

### Assignments

- `POST /api/v1/instructor/assignments`
- `GET /api/v1/assignments`
- `GET /api/v1/assignments/{assignmentId}/download`

### Feedback

- `POST /api/v1/instructor/feedback/{submissionId}`
- `GET /api/v1/feedback?studentId=...`
- `GET /api/v1/feedback/{feedbackId}/download`

## Demo Flow

1. Start the server.
2. Start the client.
3. Publish an assignment using the assignment API.
4. Confirm the file appears in the student's `Assignments/` folder.
5. Drop a solution file into `Submissions/`.
6. Confirm the JavaFX client shows the file moving to `SYNCED`.
7. Confirm the server lists the submission.
8. Upload feedback using the feedback API.
9. Confirm the file appears in the student's `Feedback/` folder.

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
