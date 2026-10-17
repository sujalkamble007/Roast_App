# ADR-006: Spring Cloud Gateway Security and Anti-Spoofing

## Status
Accepted

## Context
Downstream microservices need user identity without repeatedly decrypting JWT cookies or handling CORS. Direct client access to internal endpoints must also be strictly prevented.

## Decision
1. Spring Cloud Gateway serves as the single public reverse proxy on port 8080.
2. `AuthenticationFilter`:
   - Strips any incoming `X-User-Id` headers from external clients to prevent identity spoofing.
   - Extracts and verifies the HMAC-SHA256 signature of the `roast_token` cookie.
   - Injects a trusted `X-User-Id` downstream header only upon valid verification.
   - Blocks external requests targeting `/internal/**` with HTTP 403 Forbidden.
3. CORS configured strictly for trusted frontend origins with credential sharing enabled.

## Consequences
- Downstream services safely consume `X-User-Id` without duplicated authentication logic.
- Complete protection against header injection and unauthorized internal calls.
