# BroCode

An AI-powered coding assistant that streams answers in real time. Backed by Google Gemini, built on Spring Boot and React 19.

---

## What it does

- Multi-turn chat with a Gemini LLM that acts as a senior dev mentor
- Streaming responses token-by-token via SSE
- Persistent session history — conversations survive page refreshes and server restarts
- Scoped strictly to software engineering topics (hard rejects off-topic questions)

---

## Tech stack

| Layer | Tech |
|---|---|
| Frontend | React 19, TypeScript, Vite 6, Zustand 5, TailwindCSS 4, React Router 7 |
| Backend | Java 21, Spring Boot 3.5, Spring Security, Project Reactor (Flux) |
| LLM | Google Gemini via LangChain4j 1.11 |
| Auth | JWT in HttpOnly cookie (JJWT 0.13) |
| Database | MongoDB Atlas |
| Session cache | Redis (prod) / JVM in-memory (local dev) |
| Rate limiting | Bucket4j 8.10 (per-IP on auth, per-user on chat) |

---

## Running locally

**Backend** — port 1107:
```bash
cd server
bash gradlew bootRun
```

Create `server/src/main/resources/application-local.properties` with:
```properties
MONGODB_URI=mongodb+srv://...
JWT_SECRET=<openssl rand -base64 32>
GEMINI_API_KEY=...
```

**Frontend** — port 5173:
```bash
cd client
npm install
npm run dev
```

Vite proxies `/api` → `http://localhost:1107`.

---

## Running tests

```bash
# Backend — 36 tests (unit + integration with embedded MongoDB)
cd server && bash gradlew test

# Frontend — 16 tests (Vitest + jsdom)
cd client && npm test
```

---

## Environment variables (production)

| Variable | Notes |
|---|---|
| `MONGODB_URI` | Atlas connection string |
| `JWT_SECRET` | Min 32-char random key |
| `GEMINI_API_KEY` | Google AI Studio key |
| `ALLOWED_ORIGINS` | Comma-separated frontend origins |
| `REDIS_URL` | Auto-injected by Railway |
| `SPRING_PROFILES_ACTIVE` | Set to `prod` to enable Redis sessions |
| `COOKIE_SECURE` | `true` for HTTPS |
| `COOKIE_SAME_SITE` | `None` for cross-origin deployments |

---

## API

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/user/register` | Public | Create account |
| POST | `/api/user/login` | Public | Authenticate; sets HttpOnly cookie |
| POST | `/api/user/logout` | Public | Clears cookie |
| GET | `/api/user/profile` | Required | Get username |
| PATCH | `/api/user/profile` | Required | Update username |
| POST | `/api/bro/broCode` | Required | SSE streaming chat |
| GET | `/api/bro/sessions` | Required | List sessions |
| DELETE | `/api/bro/session/{id}` | Required | Delete session |
| GET | `/actuator/health` | Public | Health probe |

---

## Architecture

See [`design/`](design/) for detailed docs:

- [`HLD.md`](design/HLD.md) — system architecture diagram
- [`LLD.md`](design/LLD.md) — class diagrams, filter chain, data models
- [`FLOW.md`](design/FLOW.md) — sequence diagrams for every user journey
- [`INFO.md`](design/INFO.md) — full tech stack, design decisions, env vars
