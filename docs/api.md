# EduSpark Agent API Contracts

This document mirrors the source contracts in `contracts/`.

## Task Lifecycle

`PENDING -> PLANNING -> EXECUTING -> REVIEWING -> COMPLETED`

Any non-terminal state may transition to `FAILED`.

## MVP Endpoints

All user-scoped endpoints require `Authorization: Bearer <jwt>`. The `{userId}` path segment must match the JWT subject, otherwise the API returns `403`.

- `POST /api/auth/register`
  - Request: `{ "username": "string", "password": "string" }`
  - Response: `{ "userId": "string", "username": "string", "token": "jwt" }`
- `POST /api/auth/login`
  - Request: `{ "username": "string", "password": "string" }`
  - Response: `{ "userId": "string", "username": "string", "token": "jwt" }`
- `POST /api/users/{userId}/documents`
  - Request: `multipart/form-data` with `file`.
  - Response: uploaded document detail with `parseStatus`, optional `parseError`, `indexedAt`, and `textPreview`.
- `GET /api/users/{userId}/documents`
  - Response: current user's documents, newest first.
- `GET /api/users/{userId}/documents/{documentId}`
  - Response: uploaded document detail, including extracted text and optional bound `taskId`.
- `DELETE /api/users/{userId}/documents/{documentId}`
  - Response: `204 No Content`.
- `POST /api/users/{userId}/documents/{documentId}/reindex`
  - Response: updated document detail after vector re-indexing attempt.
- `POST /api/users/{userId}/tasks`
  - Request: `{ "instruction": "string", "documentIds": ["string"] }`
  - Response: `{ "taskId": "string", "status": "PENDING" }`
- `GET /api/users/{userId}/tasks?limit=20`
  - Response: recent user-scoped tasks, newest first.
- `GET /api/users/{userId}/tasks/{taskId}`
  - Response: task detail.
- `GET /api/users/{userId}/tasks/{taskId}/logs`
  - Response: ordered task logs.
- `GET /api/users/{userId}/tasks/{taskId}/artifacts`
  - Response: ordered tool artifacts with `artifactType` and `contentJson`.
- `GET /api/users/{userId}/tasks/{taskId}/stream`
  - Response: Server-Sent Events with `task.status.changed`, `task.log.appended`, `task.completed`, or `task.failed`.
  - Browser EventSource clients may pass `?token=<jwt>` because native EventSource cannot set custom headers.
- `POST /api/users/{userId}/worksheets`
  - Request:
    ```json
    {
      "title": "分数加减法练习",
      "documentIds": ["document-id"],
      "questionCount": 10,
      "gradeLevel": "五年级",
      "difficulty": "中等",
      "questionTypes": ["选择题", "判断题", "简答题"],
      "includeExplanation": true
    }
    ```
  - Response: `{ "worksheetId": "string", "taskId": "string", "status": "PENDING" }`
  - Behavior: creates a worksheet task, retrieves user-scoped document chunks, generates worksheet questions, and stores generated questions under the same `userId`.
- `GET /api/users/{userId}/worksheets/{worksheetId}`
  - Response:
    ```json
    {
      "id": "string",
      "userId": "string",
      "taskId": "string",
      "title": "string",
      "documentIds": ["string"],
      "config": {
        "title": "string",
        "documentIds": ["string"],
        "questionCount": 10,
        "gradeLevel": "string",
        "difficulty": "string",
        "questionTypes": ["string"],
        "includeExplanation": true
      },
      "generationRationale": {
        "summary": "根据上传材料生成五年级分数加减法练习。",
        "keyPoints": ["同分母分数加减法", "约分", "应用题语境"],
        "difficultyPlan": "以中等难度为主，包含基础判断和综合选择。",
        "typePlan": "按选择题、判断题、简答题的偏好分布生成。",
        "deviationFromPreference": null
      },
      "questions": [
        {
          "id": "string",
          "type": "选择题",
          "stem": "string",
          "options": ["A. ...", "B. ..."],
          "answer": "A",
          "explanation": "string",
          "difficulty": "中等",
          "sourceChunkIds": ["chunk-id"]
        }
      ],
      "status": "COMPLETED",
      "createdAt": "ISO-8601 datetime",
      "updatedAt": "ISO-8601 datetime"
    }
    ```
- `GET /api/users/{userId}/worksheets/{worksheetId}/export.docx`
  - Response: Word document bytes.
  - Content-Type: `application/vnd.openxmlformats-officedocument.wordprocessingml.document`
  - Content-Disposition: attachment with a `.docx` filename derived from the worksheet title.
- `POST /api/users/{userId}/worksheets/{worksheetId}/attempts`
  - Request: `{ "answers": [{ "questionId": "string", "answer": "A" }] }`
  - Response: `{ "attemptId": "string", "worksheetId": "string", "score": 100, "items": [{ "questionId": "string", "submittedAnswer": "A", "correctAnswer": "A", "correct": true }], "weaknessSummary": "string", "remediationSuggestion": "string", "createdAt": "ISO-8601 datetime" }`
- `GET /api/users/{userId}/worksheets/{worksheetId}/attempts`
  - Response: recent grading attempts for this worksheet.
- `GET /api/users/{userId}/worksheets/{worksheetId}/wrong-questions`
  - Response: wrong-question records created from worksheet attempts.
- `GET /api/users/{userId}/wrong-questions`
  - Response: current user's unresolved wrong questions.
- `DELETE /api/users/{userId}/wrong-questions/{wrongQuestionId}`
  - Response: `204 No Content`, marks the wrong question as resolved.
- `POST /api/users/{userId}/wrong-questions/retry`
  - Request: `{ "wrongQuestionIds": ["string"], "title": "错题再练" }`
  - Response: `{ "worksheetId": "string", "taskId": "string", "status": "PENDING" }`
- `GET /api/users/{userId}/knowledge-graph?limit=12`
  - Response: `{ "nodes": [{ "id": "doc:...", "label": "string", "type": "document", "weight": 1 }], "edges": [{ "source": "doc:...", "target": "term:...", "label": "contains", "weight": 2 }] }`
- `POST /api/users/{userId}/knowledge-graphs`
  - Request: `{ "title": "string", "documentIds": ["string"] }`
  - Response: `{ "graphId": "string", "taskId": "string", "status": "PENDING" }`
- `GET /api/users/{userId}/knowledge-graphs`
  - Response: knowledge graph records for the current user, newest first.
- `GET /api/users/{userId}/knowledge-graphs/{graphId}`
  - Response:
    ```json
    {
      "id": "string",
      "userId": "string",
      "title": "string",
      "documentIds": ["string"],
      "graphJson": {
        "nodes": [
          { "id": "doc:...", "label": "string", "type": "document", "weight": 1 }
        ],
        "edges": [
          { "source": "doc:...", "target": "term:...", "label": "contains", "weight": 2 }
        ]
      },
      "status": "COMPLETED",
      "taskId": "string",
      "createdAt": "ISO-8601 datetime",
      "updatedAt": "ISO-8601 datetime"
    }
    ```
- `DELETE /api/users/{userId}/knowledge-graphs/{graphId}`
  - Response: `204 No Content`.

## DashScope Setup

To use DashScope-backed worksheet generation, set `DASHSCOPE_API_KEY` in the backend runtime environment and run with the Spring AI/DashScope planner profile expected by the deployment configuration. Without this key, local/test runs should use the mock generation mode.

## Error Semantics

- `401`: missing, invalid, or expired JWT.
- `403`: JWT user id does not match the `{userId}` path segment.
- `404`: requested task, document, or worksheet is not visible within the current user scope.
- `409`: registration conflict, such as duplicate username.
