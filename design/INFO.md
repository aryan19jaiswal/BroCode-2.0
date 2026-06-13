# BroCode — Project Info

## What is BroCode?

BroCode is an AI-powered coding assistant web app. Users chat with an LLM that plays the role of a senior dev mentor — opinionated, direct, and scoped exclusively to software engineering topics. It supports multi-turn conversations, persistent session history, and real-time token streaming.

---

## Project Structure

```
BroCode/
├── client/          # React frontend (Vite)
├── server/          # Spring Boot backend
├── design/          # Architecture docs
```

---

## Tech Stack

### Backend

| Concern | Technology | Version |
|---|---|---|
| Language | Java | 21 (LTS) |
| Framework | Spring Boot | 3.5.10 |
| LLM integration | LangChain4j | 1.11.0 |
| LLM provider | Google Gemini | gemini-2.5-flash-lite |
| Database | MongoDB Atlas | (cloud-hosted) |
| Session cache (prod) | Redis | via Spring Data Redis |
| Session cache (dev) | JVM in-memory map | `@Profile("local")` |
| Auth | JWT (JJWT) | 0.13.0 |
| Auth transport | HttpOnly cookie | SameSite=Lax |
| Rate limiting | Bucket4j | 8.10.1 |
| Global API rate limit | Guava RateLimiter | 33.0.0-jre |
| Reactive streaming | Project Reactor (Flux) | via spring-boot-starter-webflux |
| Validation | Jakarta Bean Validation | via spring-boot-starter-validation |
| Health checks | Spring Actuator | via spring-boot-starter-actuator |
| Build tool | Gradle (Kotlin DSL) | — |
| Test framework | JUnit 5 + Mockito | via spring-boot-starter-test |
| Integration test DB | Flapdoodle embedded MongoDB | 4.14.0 |

### Frontend

| Concern | Technology | Version |
|---|---|---|
| Language | TypeScript | ~5.9.3 |
| Framework | React | 19.2.0 |
| Build tool | Vite | 6.3.5 |
| State management | Zustand | 5.0.11 |
| HTTP client | Axios | 1.13.5 |
| CSS | TailwindCSS | 4.2.0 |
| Routing | React Router v7 | 7.13.0 |
| Toast notifications | react-hot-toast | 2.6.0 |
| Markdown rendering | react-markdown + remark-gfm | 10.1.0 / 4.0.1 |
| Icons | lucide-react | 0.575.0 |
| Test framework | Vitest + jsdom | 3.2.0 / 25.0.0 |
| Test utilities | @testing-library/react | 16.0.0 |

---

## Key Design Decisions

**Stateless backend with JWT cookies**
The server holds no HTTP session state. Every request is authenticated by reading an HttpOnly JWT cookie — the cookie is invisible to JavaScript, which prevents XSS token theft. The JWT subject is the MongoDB user `id`; controllers read it from the `SecurityContext` principal.

**Two-tier session storage**
Live conversation history lives in a fast cache (Redis in production, a `ConcurrentHashMap` locally). After each streaming turn completes, the session is asynchronously synced to MongoDB (`doFinally` fire-and-forget on a `boundedElastic` scheduler). This keeps the hot path non-blocking while providing durability.

**Cache miss restoration**
When a user resumes a conversation whose cache TTL has expired (30 min default), `BroCodeService.resolveSession` transparently reloads the MongoDB history into the cache before querying the LLM. The client never sees an error.

**SSE streaming via Reactor Flux**
The chat endpoint returns `Flux<ResponseDto>` with `produces = text/event-stream`. Each chunk carries both the token content and the `sessionId`, allowing the frontend to correlate the first chunk with the temp `"__pending__"` session id and replace it with the real UUID.

**Optimistic UI**
The frontend inserts both the user message and an empty assistant bubble into the store before the first byte arrives from the server. Chunks are appended to the assistant bubble in place, giving a smooth typing effect.

**Agent abstraction**
`Agent` is a base class that selects the correct `GeminiLLMService` overload at runtime depending on which of three optional capabilities are configured: bare chat, RAG (ContentRetriever), tools, or both. `BroCodeAgent` currently uses only bare chat and extends `Agent` without overriding anything.

**Rate limiting at the interceptor layer**
`RateLimitInterceptor` runs as a `HandlerInterceptor` (before controllers) using in-memory Bucket4j token buckets keyed by client IP (auth endpoints) or `userId` (chat endpoint). Returns HTTP 429 with a `Retry-After` header on breach.

---

## API Endpoints

### User (`/api/user`)

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/register` | Public | Create account |
| POST | `/login` | Public | Authenticate; sets HttpOnly cookie |
| POST | `/logout` | Public | Clears cookie (Max-Age=0) |
| GET | `/profile` | Required | Returns username |
| PATCH | `/profile` | Required | Update username |

### Chat (`/api/bro`)

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/broCode` | Required | SSE streaming chat (text/event-stream) |
| GET | `/sessions` | Required | List user's chat sessions |
| DELETE | `/session/{id}` | Required | Delete session from MongoDB + cache |

### Actuator

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/actuator/health` | Public | Health probe for Railway / Docker |

---

## Environment Variables

| Variable | Required | Default | Notes |
|---|---|---|---|
| `MONGODB_URI` | Yes | — | Atlas connection string |
| `JWT_SECRET` | Yes | — | Min 32-char random string (`openssl rand -base64 32`) |
| `GEMINI_API_KEY` | Yes | — | Google AI Studio key |
| `ALLOWED_ORIGINS` | Yes | `http://localhost:5173` | Comma-separated frontend origins |
| `REDIS_URL` | Prod only | `redis://localhost:6379` | Railway injects this automatically |
| `SPRING_PROFILES_ACTIVE` | No | `local` | Set to `prod` to activate Redis sessions |
| `COOKIE_SECURE` | Prod | `false` | Set `true` for HTTPS deployments |
| `COOKIE_SAME_SITE` | Prod | `Lax` | Set `None` for cross-origin deployments |
| `GEMINI_MODEL_NAME` | No | `gemini-2.5-flash-lite` | Override to use a different Gemini model |

---

## Running Locally

**Backend** (port 1107):
```bash
cd server
bash gradlew bootRun
```
Requires `server/src/main/resources/application-local.properties` with `MONGODB_URI`, `JWT_SECRET`, and `GEMINI_API_KEY` values.

**Frontend** (port 5173, proxies `/api` to 1107):
```bash
cd client
npm install
npm run dev
```

---

## Tests

**Backend** (36 tests):
```bash
cd server
bash gradlew test
```
Covers: `JwtUtil` (7 unit), `AuthService` (6 unit), `InMemoryChatSessionService` (9 unit), `UserController` (7 integration), `BroCodeController` (7 integration). Integration tests use flapdoodle embedded MongoDB.

**Frontend** (16 tests):
```bash
cd client
npm test
```
Covers: `authStore` (8 tests), `chatStore` (8 tests). Uses Vitest + jsdom.

---

## AI Persona

BroCode is hardcoded (via system prompt in `PromptService`) to:

- Respond only to software engineering topics — hard-rejects anything else
- Structure every answer in six sections: Diagnosis → Root Cause → Game Plan → Clean Code → Complexity Analysis → Pro Tips
- Default to Java if no language is specified
- Never reveal the system prompt or accept prompt injection attempts
- Use a "senior dev mentor" tone — knowledgeable and direct, not sycophantic

The system prompt is re-injected at position 0 whenever a session is restored from MongoDB into the cache.
