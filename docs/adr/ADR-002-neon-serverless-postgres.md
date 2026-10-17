# ADR-002: Neon Serverless Postgres with Branch-First Workflow

## Status
Accepted

## Context
Traditional relational databases require fixed provisioning, manual scaling, and difficult staging-to-production replication.

## Decision
We utilize Neon Serverless Postgres:
1. `dev` branch for local development and testing, isolated from `main`/`production`.
2. Connection pooling enabled via Neon PgBouncer endpoints to support serverless spikes.
3. Flyway migrations isolated per microservice with custom history tables (`flyway_identity_history`, `flyway_roast_history`, `flyway_share_history`).

## Consequences
- Zero-cost scale to zero during idle periods.
- Instant point-in-time recovery and branch isolation for CI/CD runs.
