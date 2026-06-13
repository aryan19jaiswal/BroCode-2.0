# High Level Design — BroCode

## System Overview

BroCode is a full-stack AI coding assistant. The frontend is a single-page React app; the backend is a stateless Spring Boot service that proxies user prompts to Google Gemini via LangChain4j. Conversation history lives in two tiers: a fast cache (Redis in production, JVM heap locally) and a durable MongoDB store.

---

## Architecture Diagram

```mermaid
graph TB
    subgraph Client["Client — React 19 + Vite (port 8080)"]
        UI[Pages / Components]
        AS[authStore\nZustand]
        CS[chatStore\nZustand]
        SS[stream.ts\nSSE Reader]
        API[api.ts\nAxios]
    end

    subgraph Server["Server — Spring Boot 3.5 / Java 21 (port 1107)"]
        SC[Spring Security\nFilter Chain]
        UC[UserController\n/api/user/*]
        BC[BroCodeController\n/api/bro/*]
        AuthSvc[AuthService]
        BroSvc[BroCodeService]
        PersistSvc[ChatPersistenceService]
        CacheSvc[ChatSessionService\ninterface]
        Agent[BroCodeAgent\n→ Agent]
        LLM[GeminiLLMService\nLangChain4j]
        JWT[JwtUtil]
        RL[RateLimitInterceptor\nBucket4j]
    end

    subgraph Cache["Session Cache"]
        IMem[InMemoryChatSessionService\nprofile: local]
        Redis[RedisChatSessionService\nprofile: !local]
    end

    subgraph Persistence["Persistence — MongoDB Atlas"]
        UserColl[(users)]
        ChatColl[(chat_sessions)]
    end

    subgraph External["External APIs"]
        Gemini[Google Gemini API\ngemini-2.0-flash]
    end

    UI --> AS
    UI --> CS
    CS --> SS
    CS --> API
    AS --> API

    API -- "REST (HTTPS + HttpOnly cookie)" --> SC
    SS  -- "SSE text/event-stream"           --> SC

    SC --> UC
    SC --> BC

    UC --> AuthSvc
    AuthSvc --> JWT
    AuthSvc --> UserColl

    BC --> BroSvc
    BC --> PersistSvc
    BroSvc --> CacheSvc
    BroSvc --> Agent
    BroSvc --> PersistSvc

    CacheSvc --> IMem
    CacheSvc --> Redis

    Agent --> LLM
    LLM --> Gemini

    PersistSvc --> ChatColl

    RL -. "preHandle" .-> UC
    RL -. "preHandle" .-> BC

    style Client fill:#1e293b,stroke:#3b82f6,color:#f8fafc
    style Server fill:#0f172a,stroke:#6366f1,color:#f8fafc
    style Cache fill:#0f172a,stroke:#10b981,color:#f8fafc
    style Persistence fill:#0f172a,stroke:#f59e0b,color:#f8fafc
    style External fill:#0f172a,stroke:#ef4444,color:#f8fafc
```

---

## Component Responsibilities

| Layer | Component | Responsibility |
|---|---|---|
| **Frontend** | `authStore` | Auth state (isAuthenticated, username), login/logout/fetchProfile actions |
| **Frontend** | `chatStore` | Session list, SSE streaming state, sendMessage / deleteSession actions |
| **Frontend** | `stream.ts` | Native `EventSource`-style SSE parser; hands chunks to chatStore |
| **Frontend** | `api.ts` | Axios instance with `withCredentials: true`; dispatches `auth:unauthorized` on 401 |
| **Backend** | `JwtFilter` | Reads `token` HttpOnly cookie, validates JWT, sets `SecurityContext` principal to `userId` |
| **Backend** | `RateLimitInterceptor` | Per-IP limit on login/register (Bucket4j); per-userId limit on `/api/bro/broCode` |
| **Backend** | `UserController` | Register, login, logout, get/update profile |
| **Backend** | `BroCodeController` | SSE streaming chat, list sessions, delete session |
| **Backend** | `BroCodeService` | Orchestrates session lifecycle: create → stream → sync |
| **Backend** | `ChatPersistenceService` | All MongoDB reads/writes for `ChatSessionDocument` |
| **Backend** | `ChatSessionService` | Interface over the live cache tier (InMemory or Redis) |
| **Backend** | `BroCodeAgent` → `Agent` | Selects the right LLM call variant (bare / tools / RAG / tools+RAG) |
| **Backend** | `GeminiLLMService` | Wraps LangChain4j `StreamingChatLanguageModel`; returns `Flux<String>` |
| **Backend** | `GlobalExceptionHandler` | Maps typed exceptions to HTTP status codes; never leaks stack traces |

---

## Communication Protocols

| Direction | Protocol | Auth |
|---|---|---|
| Browser → REST endpoints | HTTPS POST/GET/DELETE | HttpOnly JWT cookie |
| Browser → SSE stream | HTTPS POST + `text/event-stream` | HttpOnly JWT cookie |
| Server → Gemini | HTTPS (LangChain4j client) | API key (env var) |
| Server → MongoDB | MongoDB wire protocol | Atlas connection string (env var) |
| Server → Redis | Redis protocol | Password (env var, production only) |

---

## Deployment Profiles

| Profile | Session Cache | Redis required |
|---|---|---|
| `local` (default dev) | `InMemoryChatSessionService` | No |
| `staging` / `prod` | `RedisChatSessionService` | Yes |
