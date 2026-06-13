# Application Flow — BroCode

---

## 1. User Registration

```mermaid
sequenceDiagram
    actor User
    participant React
    participant authStore
    participant authService
    participant UserController
    participant AuthService
    participant MongoDB

    User->>React: fills RegisterPage form
    React->>authStore: register(email, username, password)
    authStore->>authService: register(...)
    authService->>UserController: POST /api/user/register
    UserController->>AuthService: register(RegisterRequest)
    AuthService->>AuthService: BCrypt.encode(password)
    AuthService->>MongoDB: userRepository.save(User)
    MongoDB-->>AuthService: saved (or DuplicateKeyException)
    alt duplicate email/username
        AuthService--xUserController: DuplicateEmailException / DuplicateUsernameException
        UserController--xReact: 409 Conflict
        React-->>User: toast error
    else success
        AuthService-->>UserController: void
        UserController-->>React: 201 Created { success: true }
        React-->>User: toast success → navigate /login
    end
```

---

## 2. User Login

```mermaid
sequenceDiagram
    actor User
    participant React
    participant authStore
    participant authService
    participant UserController
    participant AuthService
    participant JwtUtil
    participant MongoDB

    User->>React: fills LoginPage form
    React->>authStore: login(identifier, password)
    authStore->>authService: login(...)
    authService->>UserController: POST /api/user/login
    UserController->>AuthService: login(AuthRequest)
    AuthService->>MongoDB: findByEmail or findByUsername
    MongoDB-->>AuthService: User | empty
    alt user not found
        AuthService--xUserController: InvalidCredentialsException
        UserController-->>React: 401
        React-->>User: toast error
    else wrong password
        AuthService->>AuthService: passwordEncoder.matches → false
        AuthService--xUserController: InvalidCredentialsException
        UserController-->>React: 401
        React-->>User: toast error
    else valid
        AuthService->>JwtUtil: generateToken(userId)
        JwtUtil-->>AuthService: JWT string
        AuthService-->>UserController: AuthResponse(username, token)
        UserController->>React: 200 + Set-Cookie: token=<JWT>; HttpOnly; SameSite=Strict
        React->>authStore: setState(isAuthenticated=true, username)
        authStore->>localStorage: setItem("isAuthenticated","true")
        authStore->>localStorage: setItem("username", username)
        React-->>User: navigate /chat
    end
```

---

## 3. App Load / Session Verification

On every page load the app reads `isAuthenticated` from localStorage. If true, it fires two parallel calls to validate the JWT server-side and hydrate sessions.

```mermaid
sequenceDiagram
    participant App
    participant authStore
    participant chatStore
    participant Server

    App->>authStore: read isAuthenticated from localStorage
    alt isAuthenticated = true
        par
            App->>authStore: fetchProfile()
            authStore->>Server: GET /api/user/profile (cookie)
            alt JWT valid
                Server-->>authStore: 200 { username }
                authStore->>authStore: update username
            else JWT expired/missing
                Server-->>authStore: 401
                authStore->>authStore: clearAuth() → isAuthenticated=false
            end
        and
            App->>chatStore: loadSessions()
            chatStore->>Server: GET /api/bro/sessions (cookie)
            Server-->>chatStore: 200 ChatSessionResponse[]
            chatStore->>chatStore: hydrate sessions (filter system messages)
        end
    end
```

---

## 4. Sending a Chat Message (SSE Streaming)

This is the core flow. The frontend optimistically adds the user + assistant messages, then streams tokens from the server into the assistant bubble.

```mermaid
sequenceDiagram
    actor User
    participant ChatInput
    participant chatStore
    participant stream.ts
    participant BroCodeController
    participant BroCodeService
    participant ChatSessionService
    participant BroCodeAgent
    participant GeminiLLMService
    participant Gemini
    participant ChatPersistenceService
    participant MongoDB

    User->>ChatInput: types question, hits Send
    ChatInput->>chatStore: sendMessage(question)

    alt new conversation (activeSessionId = null)
        chatStore->>chatStore: insert temp session "__pending__"
    else existing session
        chatStore->>chatStore: append userMessage + empty assistantMessage
    end

    chatStore->>stream.ts: streamChat({question, sessionId}, onChunk, onDone, onError, signal)
    stream.ts->>BroCodeController: POST /api/bro/broCode (SSE)

    BroCodeController->>BroCodeController: read userId from SecurityContext

    alt sessionId null or blank
        BroCodeController->>BroCodeService: createNewChatSession(userId, title)
        BroCodeService->>ChatSessionService: startNewChatSession(systemPrompt)
        ChatSessionService-->>BroCodeService: sessionId (UUID)
        BroCodeService->>ChatPersistenceService: createSession(sessionId, userId, title)
        ChatPersistenceService->>MongoDB: save ChatSessionDocument (empty messages)
    else sessionId provided
        BroCodeController->>BroCodeService: resolveSession(sessionId, userId)
        BroCodeService->>ChatSessionService: validateSessionId(sessionId)
        alt cache hit
            ChatSessionService-->>BroCodeService: ok
        else cache miss (TTL expired)
            BroCodeService->>ChatPersistenceService: restoreToCache(sessionId, userId, chatSessionService)
            ChatPersistenceService->>MongoDB: findByIdAndUserId
            MongoDB-->>ChatPersistenceService: ChatSessionDocument
            ChatPersistenceService->>ChatSessionService: restoreSession(sessionId, [systemPrompt]+messages)
        end
    end

    BroCodeController->>BroCodeService: getBroCodeAgentResponse(sessionId, question)
    BroCodeService->>BroCodeAgent: chatStream(sessionId, question)
    BroCodeAgent->>GeminiLLMService: chatStream(sessionId, question)
    GeminiLLMService->>Gemini: streaming chat request (LangChain4j)

    loop SSE tokens
        Gemini-->>GeminiLLMService: token chunk
        GeminiLLMService-->>BroCodeController: Flux onNext(chunk)
        BroCodeController-->>stream.ts: data: {"content":"...","sessionId":"..."}
        stream.ts->>chatStore: onChunk(content, sessionId)
        chatStore->>chatStore: append chunk to assistant message
        chatStore->>chatStore: replace "__pending__" id → real sessionId (first chunk only)
    end

    Gemini-->>GeminiLLMService: stream complete
    GeminiLLMService-->>BroCodeController: Flux complete
    BroCodeController->>BroCodeController: doFinally → fire-and-forget syncSessionToMongo
    BroCodeController-->>stream.ts: SSE stream closed
    stream.ts->>chatStore: onDone()
    chatStore->>chatStore: isStreaming=false

    Note over BroCodeService,MongoDB: async (boundedElastic scheduler)
    BroCodeService->>ChatSessionService: getMessages(sessionId)
    BroCodeService->>ChatPersistenceService: syncMessages(sessionId, messages)
    ChatPersistenceService->>MongoDB: update ChatSessionDocument.messages (exclude system)
```

---

## 5. Session Management

### 5a. Load session history on page load

```mermaid
flowchart TD
    A[User opens /chat] --> B[App useEffect: loadSessions]
    B --> C[GET /api/bro/sessions]
    C --> D[ChatPersistenceService.getUserSessions]
    D --> E[MongoDB: findByUserId orderByUpdatedAtDesc]
    E --> F[ChatSessionResponse list]
    F --> G[chatStore: hydrate sessions\nfilter role=system messages]
    G --> H[Sidebar renders session titles]
    H --> I[User clicks a session]
    I --> J[chatStore.setActiveSession]
    J --> K[MessageList renders messages]
```

### 5b. Delete a session

```mermaid
sequenceDiagram
    actor User
    participant Sidebar
    participant chatStore
    participant chatService
    participant BroCodeController
    participant ChatPersistenceService
    participant ChatSessionService
    participant MongoDB

    User->>Sidebar: clicks delete on session X
    Sidebar->>chatStore: deleteSession(id)
    chatStore->>chatService: deleteSession(id)
    chatService->>BroCodeController: DELETE /api/bro/session/{id}
    BroCodeController->>ChatPersistenceService: deleteSession(id, userId)
    ChatPersistenceService->>MongoDB: deleteByIdAndUserId (idempotent)
    BroCodeController->>ChatSessionService: destroyChatSession(id)
    ChatSessionService->>ChatSessionService: remove from cache (Redis DEL or map.remove)
    BroCodeController-->>chatStore: 200 { success: true }
    chatStore->>chatStore: filter sessions (remove id)\nif activeSessionId=id → null
    Sidebar->>Sidebar: re-renders without deleted session
```

---

## 6. Logout

```mermaid
sequenceDiagram
    actor User
    participant Navbar
    participant authStore
    participant authService
    participant UserController

    User->>Navbar: clicks Logout
    Navbar->>authStore: logout()
    authStore->>authService: logout()
    authService->>UserController: POST /api/user/logout
    UserController->>UserController: Set-Cookie: token=; Max-Age=0; HttpOnly
    UserController-->>authStore: 200 { success: true }
    authStore->>authStore: clearAuth()\nisAuthenticated=false, username=null
    authStore->>localStorage: removeItem("isAuthenticated")\nremoveItem("username")
    authStore-->>Navbar: state update
    Navbar->>Navbar: navigate /
```

---

## 7. Unauthorized Request Handling (401 Auto-Logout)

When any API call returns 401 (JWT expired mid-session), the Axios interceptor automatically signs the user out without requiring explicit logout.

```mermaid
flowchart TD
    A[Any API call] --> B{Response status}
    B -->|200-299| C[Normal response handling]
    B -->|401| D[Axios interceptor fires]
    D --> E[window.dispatchEvent\nauthunauthorized]
    E --> F[authStore listener]
    F --> G[clearAuth\nisAuthenticated=false\nlocalStorage cleared]
    G --> H[React re-render\nProtectedRoute redirects to /]
```

---

## 8. Cache Miss Restoration Flow

When a user resumes a session whose cache TTL has expired (default 30 min), the backend transparently restores it from MongoDB before querying the LLM.

```mermaid
flowchart TD
    A[POST /api/bro/broCode\nwith sessionId] --> B[resolveSession]
    B --> C[validateSessionId]
    C -->|key exists| D[Cache hit → proceed]
    C -->|key missing| E[Cache miss]
    E --> F[ChatPersistenceService\nrestoreToCache]
    F --> G[MongoDB findByIdAndUserId]
    G -->|not found or wrong user| H[InvalidSessionException → 400]
    G -->|found| I[Build message list:\nSystemMessage + stored messages]
    I --> J[ChatSessionService.restoreSession\nwrite back to cache with TTL]
    J --> D
    D --> K[BroCodeAgent.chatStream]
```
