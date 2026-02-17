# SpellfAIre

Monorepo:
- `backend/`: Spring Boot (Spring Security) + MongoDB
- `frontend/`: Angular SPA
- `infra/`: local development infrastructure (MongoDB)

## Prereqs
- Java 21
- Maven 3.9+
- Node.js (latest LTS recommended) + npm
- Docker Desktop (for MongoDB via compose)

## Local development

### 1) Start MongoDB
From repo root:

```bash
docker compose -f infra/docker-compose.yml up -d
```

Mongo will be available at `mongodb://localhost:27017`.

### 2) Start backend

```bash
./backend/mvnw -f backend/pom.xml spring-boot:run
```

On Windows PowerShell:

```powershell
backend\mvnw.cmd -f backend\pom.xml spring-boot:run
```

Backend runs on `http://localhost:8080`.

### 3) Start frontend

```bash
cd frontend
npm install
npm start
```

Frontend runs on `http://localhost:4200`.

## MVP scope
- Register + login only.
- Everything else comes later (single-player vs AI first, multiplayer later).
