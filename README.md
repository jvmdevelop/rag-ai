# rag-ai
What it does
```java
    Pulls in documents (from storage or uploads).
    Indexes content for fast lookup.
    Finds useful snippets for a query and builds context.
    Generates answers that reference that context.
    Exposes simple admin endpoints for metrics, cache control and health.
```
Public API (short)
```java
    POST /api/chat/message — send a message, get a generated reply.
    POST /api/admin/upload/txt — upload plain text for indexing.
    Admin under /api/admin/rag — metrics, doc stats, clear cache, reset metrics, health.
```
Architecture (short)
```java
    Ingest -> Index -> Retrieve -> Assemble context -> Generate.
    Different services handle ingestion, indexing/retrieval, generation, caching and metrics.
    Modular by design so parts can be swapped or extended.
```
Why this is handy
```java
    A compact pattern for building a knowledge‑aware Q&A or assistant.
    Easy to swap components (retriever, index, generator) without changing the flow.
```
