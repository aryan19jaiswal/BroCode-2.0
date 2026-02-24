# 🏗️ Architecture Overview

The application follows a **monolithic client-server architecture** with clear separation of concerns.

---

## 🖥️ Client Layer (`/client`)

- Single Page Application (SPA) built with React + Vite
- Organized by:
  - Route-level components
  - Reusable UI blocks
  - Application state management
- Handles authentication using JWT tokens stored client-side
- Communicates with backend via:
  - Authentication APIs
  - Streaming AI responses
  - General REST calls

---

## 🛠️ Server Layer (`/server`)

- Spring Boot REST API serving as the backend core
- Exposes secured endpoints
- Implements stateless authentication using JWT
- Contains AI orchestration layer

### 🔐 Security Layer

- Stateless JWT-based authentication

---

### 🤖 AI Engine

Capabilities:
- Manages chat sessions
- Maintains conversational context
- Executes tools when required
- Integrates with external LLM providers

---

### 🗄️ Data Persistence

- Uses Spring Data JPA
- Backed by MongoDB and In-Memory Database

---

# 🔄 High-Level Flow

1. User authenticates → JWT issued
2. Client stores token
3. Protected routes become accessible
4. User sends chat prompt
5. Backend processes via AI Agent layer
6. Response is streamed back to client
7. Chat session persisted in database

---

# 🧩 Design Characteristics

- Clean separation of frontend and backend
- Stateless authentication for scalability
- Real-time AI response streaming
- Modular AI agent architecture
- Repository-based persistence layer
- Production-ready structure

---

# 📌 Summary

BroCode is a modern full-stack AI application built using:

- ⚛️ React + TypeScript (Frontend)
- ☕ Spring Boot + Spring Security (Backend)
- 🧠 Custom AI Agent Layer
- 🗄️ JPA-based Persistence
- 📡 Streaming AI Responses

Designed for scalability, maintainability, and real-time AI-powered coding assistance.
