# ADR-001: Multi-Module Microservices Architecture

## Status
Accepted

## Context
Roast App requires independent scaling and isolated concerns between OAuth token ingress, digital footprint aggregation, LLM inference, social share card generation, and public gateway routing.

## Decision
We organize the project as a Maven multi-module monorepo:
1. `common`: Shared DTOs, RFC 7807 ProblemDetail error handlers, and CorrelationId tracing filter.
2. `gateway`: Spring Cloud Gateway handling public entry, CORS, and JWT cookie extraction.
3. `identity-service`: OAuth2 logins (GitHub, Spotify, Google), user records, and AES-256-GCM token encryption.
4. `connector-service`: Parallel fetch coordinator for Spotify, GitHub, and YouTube statistics.
5. `roast-service`: Async LLM roast generation with Groq multi-key failover and quota management.
6. `share-service`: Java2D 1200x630 card rendering and Cloudflare R2 storage.
7. `web`: Vite + React + TypeScript Single Page Application.

## Consequences
- Clean separation of concerns with isolated databases and Flyway schema histories.
- Each service can be containerized independently and scaled horizontally.
