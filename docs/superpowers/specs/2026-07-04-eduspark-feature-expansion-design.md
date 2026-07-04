# EduSpark Feature Expansion Design

## Scope

This design expands EduSpark Agent according to `产品办理清单.xls`, excluding production Docker deployment for now.

The expansion focuses on four product lines:

1. Document Center
2. Practice and Grading Loop
3. Agent Process Replay and History Center
4. Knowledge Graph

Out of scope for this round:

- Production Docker Compose deployment
- Large file chunked upload
- OCR for scanned PDFs
- Full performance专项工程

## Current Baseline

The project already has:

- React frontend with login/register, JWT session, document upload, task console, worksheet generation, Word export.
- Spring Boot backend with user-scoped APIs.
- JWT authentication and userId path isolation.
- Document persistence, PDF/TXT extraction baseline, vector chunk storage, worksheet generation, SSE task stream.
- Qwen-compatible Spring AI configuration through DashScope/OpenAI-compatible API.

## Product Line 1: Document Center

### Goal

Turn document upload from a hidden supporting step into a visible learning-material management workflow.

### Features

- Support common learning files:
  - TXT
  - MD
  - PDF
  - DOCX
  - CSV
- Show upload progress on frontend.
- Show parse status: uploaded, parsing, indexed, failed.
- Show extracted text preview after upload.
- Add document list in workspace or personal center.
- Allow document deletion.
- Allow manual re-index when parsing/indexing fails.

### Backend Design

Extend `edu_document` with parse/index metadata:

- `parse_status`
- `parse_error`
- `indexed_at`
- `text_preview`

Add document APIs:

- `GET /api/users/{userId}/documents`
- `DELETE /api/users/{userId}/documents/{documentId}`
- `POST /api/users/{userId}/documents/{documentId}/reindex`

Keep upload contract user-scoped.

### Frontend Design

Add a `DocumentCenterPanel`:

- Upload area
- Current upload progress
- Parsed text preview
- Document list
- Delete/reindex actions

## Product Line 2: Practice and Grading Loop

### Goal

Complete the learning loop from generated worksheet to student answer, grading, feedback, and retry practice.

### Features

- Generated worksheet view has two tabs:
  - Practice
  - Explanation
- Practice tab hides answers.
- Each question has answer input:
  - Choice: option selection
  - True/False: option selection
  - Short answer: multiline text
- Submit answers.
- Backend returns:
  - total score
  - per-question correctness
  - correct answer
  - explanation
  - weakness summary
  - remediation suggestion
- Incorrect questions can generate similar practice.

### Backend Design

Add worksheet attempt model:

- `edu_worksheet_attempt`
  - `id`
  - `user_id`
  - `worksheet_id`
  - `answers_json`
  - `grading_result_json`
  - `score`
  - `created_at`

Add APIs:

- `POST /api/users/{userId}/worksheets/{worksheetId}/attempts`
- `GET /api/users/{userId}/worksheets/{worksheetId}/attempts`
- `POST /api/users/{userId}/worksheets/{worksheetId}/retry-mistakes`

For choice and true/false questions, grading is deterministic.

For short-answer questions, first version uses simple keyword/LLM-assisted grading behind a service boundary:

- `AnswerGradingService`
- mock implementation for tests
- Qwen implementation when AI mode is enabled

### Frontend Design

Refactor `WorksheetPanel` into:

- `WorksheetTabs`
- `PracticeTab`
- `ExplanationTab`
- `AttemptResultPanel`

## Product Line 3: Agent Process Replay and History Center

### Goal

Make Agent execution understandable and replayable.

### Features

- Agent timeline supports collapse/expand.
- Tool input/output rendered as formatted JSON.
- Completed tasks can be reopened.
- Personal center shows:
  - recent tasks
  - uploaded documents
  - worksheet records
  - knowledge graph records
- Delete records from history.

### Backend Design

Add list/delete APIs:

- `GET /api/users/{userId}/tasks`
- `DELETE /api/users/{userId}/tasks/{taskId}`
- `GET /api/users/{userId}/worksheets`
- `DELETE /api/users/{userId}/worksheets/{worksheetId}`

Deletion must be user-scoped and should clean dependent logs/artifacts where appropriate.

### Frontend Design

Add workspace navigation:

- Console
- Documents
- History
- Knowledge Graph

History page can stay lightweight:

- Search/filter by type and date
- Click item to open detail
- Delete item

## Product Line 4: Knowledge Graph

### Goal

Generate an interactive concept map from uploaded materials or generated worksheet content.

### Features

- Generate graph from selected documents.
- Store graph JSON.
- Render graph in browser.
- Drag, zoom, pan.
- Click node to view:
  - concept name
  - explanation
  - related source snippets
  - linked concepts
- Export graph JSON.
- Screenshot export can be added after graph is stable.

### Backend Design

Add graph model:

- `edu_knowledge_graph`
  - `id`
  - `user_id`
  - `title`
  - `document_ids_json`
  - `graph_json`
  - `status`
  - `created_at`
  - `updated_at`

Graph JSON contract:

```json
{
  "nodes": [
    {
      "id": "concept-id",
      "label": "Concept",
      "description": "Brief explanation",
      "sourceChunkIds": ["chunk-id"]
    }
  ],
  "edges": [
    {
      "source": "concept-id-a",
      "target": "concept-id-b",
      "relation": "prerequisite"
    }
  ]
}
```

Add APIs:

- `POST /api/users/{userId}/knowledge-graphs`
- `GET /api/users/{userId}/knowledge-graphs`
- `GET /api/users/{userId}/knowledge-graphs/{graphId}`
- `DELETE /api/users/{userId}/knowledge-graphs/{graphId}`

### Frontend Design

Use SVG or Canvas without adding a heavy graph library initially.

Components:

- `KnowledgeGraphPage`
- `KnowledgeGraphCanvas`
- `ConceptDetailDrawer`

## Error Handling

Document errors:

- Unsupported type: `400`
- No readable text: `400`
- Indexing failed: document remains uploaded, status shows failed indexing

Worksheet errors:

- No document selected: frontend validation
- AI output invalid: backend fallback where possible
- Grading unavailable: show retryable error

History errors:

- Missing record: `404`
- Cross-user access: `403`

## Contracts First

Before implementation, update:

- `contracts/task.contract.ts`
- `docs/api.md`
- backend DTOs
- frontend API types

Every new user-facing API must use `/api/users/{userId}/...`.

## Suggested Implementation Phases

### Phase 1: Document Center

Add document list, delete, reindex, text preview, and parse/index status.

### Phase 2: Practice and Grading

Add worksheet attempts, answer submission, result display, and practice/explanation tabs.

### Phase 3: History Center and Replay

Add list/delete APIs and frontend history views for tasks, documents, and worksheets.

### Phase 4: Knowledge Graph

Add graph generation, graph storage, graph API, and interactive graph view.

### Phase 5: Polish

Improve loading states, empty states, error messages, responsiveness, and frontend performance.

## Testing Strategy

Backend:

- Migration tests for new tables/columns
- User-scope integration tests for every API
- Document parsing tests for TXT/PDF/DOCX/CSV
- Worksheet attempt grading tests
- Knowledge graph parser/generator tests

Frontend:

- Document center behavior
- Worksheet practice/explanation tabs
- Attempt submission and grading display
- History navigation and deletion
- Knowledge graph render basics

Full verification:

- Backend `mvn package`
- Frontend `vitest run`
- Frontend `tsc -b`
- Frontend `vite build`

