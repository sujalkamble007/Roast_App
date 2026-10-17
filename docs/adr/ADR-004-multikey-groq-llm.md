# ADR-004: Multi-Key Load Balancing and Failover for Groq LLM

## Status
Accepted

## Context
High-concurrency LLM inference can hit Groq per-key rate limits (TPM / RPM) or temporary service outages.

## Decision
1. `GroqLlmClient` accepts comma-separated `GROQ_API_KEYS`.
2. Requests are distributed round-robin across healthy API keys using atomic counters.
3. If a 429 (Rate Limit) or 5xx error occurs on one key, the client automatically retries on subsequent keys before failing.

## Consequences
- Multiplies inference throughput proportionally to the number of configured keys.
- Eliminates single-key rate limit bottlenecks.
