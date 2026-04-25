# University Drive Implementation Plan

## Goal

Build University Drive as an Intro to Java course project using Test Driven Development (TDD), with a scoped client-server architecture that demonstrates:

- Java 21 fundamentals
- multithreading and concurrency
- file I/O with `java.nio.file`
- REST communication
- SQLite persistence
- JavaFX UI responsiveness
- unit and integration testing with JUnit 5

## Project Scope

This project should stay course-appropriate and avoid unnecessary production complexity.

### In scope

- Maven multi-module project
- `unidrive-common`, `unidrive-server`, `unidrive-client`
- SHA-256 file hashing
- atomic submission flow
- local folder watching with `WatchService`
- background upload with thread pool
- SQLite metadata/state storage
- JavaFX status UI
- polling for assignments and feedback
- JUnit 5 tests
- mock mode for unreliable network simulation

### Out of scope unless later required

- real authentication/login
- WebSocket/live push
- conflict resolution across simultaneous edits
- advanced distributed sync logic
- complex permissions model
- production deployment concerns

## Architecture

### Modules

- `unidrive-common`
  - shared DTOs
  - response envelope
  - enums
  - hash utilities
  - constants
- `unidrive-server`
  - Spring Boot REST API
  - SQLite metadata storage
  - file storage management
  - assignment/submission/feedback endpoints
- `unidrive-client`
  - JavaFX app
  - local folder setup
  - `WatchService` monitoring
  - upload/download sync logic
  - local SQLite sync state

## TDD Development Rule

Every functional change should follow Red -> Green -> Refactor.

1. Write or update a failing test first.
2. Implement the smallest code change needed to pass.
3. Refactor only after tests pass.
4. Keep production code simple and course-appropriate.
5. Do not add speculative abstractions before tests justify them.

## Delivery Strategy

Build the system in vertical slices so each milestone produces a working demo.

### Slice 1: Project skeleton

- create Maven parent project
- create all three modules
- make tests run successfully in each module
- verify packaging/build works

### Slice 2: Shared utilities

- implement `FileHasher`
- implement shared enums and DTOs
- implement standard API response envelope

### Slice 3: Server submission MVP

- SQLite schema initialization
- file storage directories
- upload submission endpoint
- server-side SHA-256 validation
- metadata persistence
- partial upload cleanup

### Slice 4: Client submission MVP

- JavaFX application shell
- local root folder selection
- create `/Assignments`, `/Submissions`, `/Feedback`
- local `sync_state` SQLite table
- `WatchService` monitoring `/Submissions`
- upload executor pool
- update UI with sync status

### Slice 5: Download flow

- server list/download endpoints for assignments and feedback
- client polling service
- local file placement
- activity log updates

### Slice 6: Instructor workflow

- publish assignment flow
- list/download student submissions
- upload feedback flow

### Slice 7: Reliability and polish

- retries for failed uploads
- clearer status transitions
- improved logging
- demo-ready UI cleanup
- end-to-end verification

## TDD Plan By Module

### `unidrive-common`

Write tests first for:

- SHA-256 output correctness
- hash consistency across repeated reads
- DTO serialization basics if needed
- enum/state behavior

Then implement:

- `FileHasher`
- `ApiResponse<T>`
- shared constants
- status enums

### `unidrive-server`

Write tests first for:

- hash mismatch rejection
- successful submission acceptance
- schema/repository behavior
- partial upload cleanup behavior
- list/download endpoint behavior

Then implement:

- controllers
- services
- repositories
- storage manager

### `unidrive-client`

Write tests first for:

- sync state transitions
- folder bootstrap logic
- watched file event filtering
- upload request building
- mock mode timing/failure behavior

Then implement:

- local DB layer
- sync service
- watch service wrapper
- API client
- JavaFX bindings

## Suggested Package Structure

### Common

- `edu.nyu.unidrive.common.dto`
- `edu.nyu.unidrive.common.model`
- `edu.nyu.unidrive.common.util`

### Server

- `edu.nyu.unidrive.server.controller`
- `edu.nyu.unidrive.server.service`
- `edu.nyu.unidrive.server.repository`
- `edu.nyu.unidrive.server.storage`
- `edu.nyu.unidrive.server.config`

### Client

- `edu.nyu.unidrive.client.ui`
- `edu.nyu.unidrive.client.sync`
- `edu.nyu.unidrive.client.storage`
- `edu.nyu.unidrive.client.net`
- `edu.nyu.unidrive.client.model`

## Initial Database Plan

### Server

- `users`
- `assignments`
- `submissions`
- `feedback`

### Client

- `sync_state`

## Initial API Plan

### Submission flow

- `POST /api/v1/submissions/{assignmentId}`
- `GET /api/v1/submissions`
- `GET /api/v1/submissions/{id}/download`

### Assignment flow

- `POST /api/v1/instructor/assignments`
- `GET /api/v1/assignments`
- `GET /api/v1/assignments/{id}/download`

### Feedback flow

- `POST /api/v1/instructor/feedback/{submissionId}`
- `GET /api/v1/feedback`
- `GET /api/v1/feedback/{id}/download`

All JSON responses should use:

- `status`
- `data`
- `message`

## Milestone Order

1. Maven structure and test harness
2. `FileHasher` and shared DTOs
3. server upload path with tests
4. client folder setup and sync state tests
5. `WatchService` and upload execution
6. JavaFX status UI
7. assignment/feedback polling
8. instructor workflow
9. end-to-end tests and demo prep

## Testing Strategy

### Unit tests

- hashing
- sync state transitions
- repository methods
- request/response models

### Integration tests

- REST upload/download behavior
- SQLite persistence behavior
- file storage behavior

### Functional tests

- drop file into `/Submissions`
- confirm upload reaches server
- confirm hash validation
- confirm UI stays responsive

### Mock mode tests

- random latency
- simulated failure rate
- retry handling

## Course-Oriented Design Principles

- prefer plain Java over heavy frameworks unless already required
- keep classes focused and understandable
- favor readability over clever abstractions
- demonstrate concurrency clearly
- keep the UI simple but functional
- implement a reliable MVP before adding extra features

## First Implementation Target

The first complete demo should be:

1. Start the server.
2. Start the student client.
3. Choose a root folder.
4. Drop a file into `/Submissions`.
5. Client hashes file and uploads in the background.
6. Server verifies SHA-256 and stores metadata.
7. Client marks file as `SYNCED`.
8. UI remains responsive during the process.

That slice already demonstrates the core project requirements and should be the first goal.
