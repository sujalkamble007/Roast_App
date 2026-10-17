# ADR-003: Ephemeral OAuth Token Encryption with AES-256-GCM

## Status
Accepted

## Context
Third-party OAuth tokens (GitHub, Spotify, YouTube) allow access to user profiles. Storing raw tokens in plaintext or indefinitely in relational tables poses serious security and compliance risks.

## Decision
1. Provider tokens are never stored in Postgres.
2. Tokens are stored in Redis under key `tok:{userId}:{provider}` with a strict 15-minute Time-To-Live (TTL).
3. Tokens are encrypted at rest using AES-256-GCM with authenticated tags before storing in Redis.
4. Once the connector service fetches profile stats, tokens expire naturally or can be revoked by user deletion (`DELETE /me`).

## Consequences
- No database breach can leak OAuth credentials.
- Total privacy compliance: tokens automatically vanish after 15 minutes.
