# Docker Deployment

This project is designed for a two-container deployment:

- `backend`: Spring Boot API on port `8080`
- `frontend`: Nginx serving the SPA on port `80` and proxying `/api` to the backend

## Required files

- `backend/Dockerfile`
- `frontend/Dockerfile`
- `frontend/nginx.conf`
- `infra/docker-compose.prod.yml`
- `infra/.env`

## Environment variables

Copy `infra/.env.example` to `infra/.env` and fill in:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `AUTH_JWT_SECRET`
- `DASHSCOPE_API_KEY`

Recommended values:

- `SPRING_AI_OPENAI_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode`
- `SPRING_AI_OPENAI_CHAT_MODEL=qwen-plus`
- `SPRING_AI_OPENAI_EMBEDDING_MODEL=text-embedding-v4`
- `AGENT_PLANNER_MODE=spring-ai`
- `VITE_API_BASE_URL=/api` during frontend build

## Run

```bash
docker compose -f infra/docker-compose.prod.yml up -d --build
```

## Notes

- The backend container uses the host-mounted volume `eduspark-uploads` for uploaded documents.
- The frontend talks to the backend through the same origin `/api`, so browser CORS issues are minimized.
- Open port `80` on the server security group. Keep backend `8080` internal unless you explicitly want direct access.
