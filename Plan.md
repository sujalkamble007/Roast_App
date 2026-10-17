# Roast App: Build Guide v1

Instant one-time roast from a user's Spotify, GitHub or YouTube activity. Users can pick any one of these platforms and don't have to connect all of them at once. Spring Boot microservices, React, Neon Postgres.

## 1. How to use this guide

Work top to bottom. Every step has four parts: **Goal**, **Do**, **How**, **Done when**. Do not start the next step until the current Done when is true. Commit at the end of every step, one branch per step.

## 2. Product in one page

- **Promise:** connect, roast, share. One instant roast, no ongoing sync.
- **Sign in:** Login with Spotify, GitHub or Google. No passwords.
- **Limit:** 3 roasts per user per day.
- **Sharing:** public link with an auto-generated preview image.
- **Out of scope for v1:** friends and battles, history storage, scheduled sync, mobile app.

## 3. Architecture

```
React SPA -> Edge (Cloudflare, Caddy) -> gateway
                                            |
      +------------------+------------------+------------------+
 identity-service   connector-service    roast-service     share-service
      |                   |                   |                 |
 Neon + Redis        Provider APIs        Neon + LLM        Neon + R2
```

| Service | Port | Owns | Storage |
| --- | --- | --- | --- |
| gateway | 8080 | Routing, JWT check, rate limit, CORS | none |
| identity-service | 8081 | Users, OAuth login, JWT, token handoff | Neon (identity), Redis |
| connector-service | 8082 | Fetch and normalize provider data | none (stateless) |
| roast-service | 8083 | Prompt, LLM call, roasts, daily quota | Neon (roast), Redis |
| share-service | 8084 | Public pages, OG cards, share links | Neon (share), R2 |

**Roast flow:** POST /roasts, check quota, save PENDING, return 202. Then fetch stats from connector-service, call the LLM, save READY. The client polls GET /roasts/{id} every 1.5 seconds.

## 4. Decision log

| # | Decision |
| --- | --- |
| D1 | v1 is an instant one-time roast |
| D2 | True microservices from day one (5 deployables) |
| D3 | OAuth-only sign in (Spotify, GitHub, Google) |
| D4 | Free, 3 roasts per day per user |
| D5 | Public share link with OG preview card |
| D6 | Docker Compose on one VPS |

**Still open:** LLM provider, domain name, VPS size (8 GB recommended). Step 2 hides the provider behind an interface, so this does not block you.

## 5. Ground rules (what keeps it maintainable)

1. **One job per service.** If you cannot describe a service in one sentence, split it.
2. **Database per service.** Never read another service's tables. Call its API.
3. **Contract first.** Write the OpenAPI file in `contracts/` before the controller.
4. **Layered packages** in every service: `api` (controllers, DTOs), `service`, `domain`, `repo`, `client`. Controllers never touch repositories.
5. **DTOs are not entities.** Never return a JPA entity from an API.
6. **Schema changes only through Flyway.** Never edit a merged migration, add a new one. Set `ddl-auto: validate`.
7. **No secrets in git.** Config comes from environment variables. Commit `.env.example` only.
8. **One error format** everywhere: RFC 7807 problem+json (Spring `ProblemDetail`).
9. **Every external call has a timeout** and a clear failure path.
10. **Write a short ADR** in `docs/adr/` for every decision that is hard to reverse.
11. **Small commits** using conventional messages: `feat:`, `fix:`, `docs:`, `chore:`.
12. **Tests:** JUnit 5, Testcontainers for Postgres and Redis, WireMock for provider and LLM APIs.

## 6. Repository layout

```
roast-app/
  pom.xml                 parent POM (versions pinned once)
  common/                 shared DTOs, error format, correlation id filter
  gateway/
  identity-service/
  connector-service/
  roast-service/
  share-service/
  web/                    React app (Vite + TypeScript)
  contracts/              OpenAPI files, one per service
  deploy/                 docker-compose.yml, Caddyfile, .env.example
  docs/                   this guide, adr/
  .github/workflows/      CI and deploy
```

## 7. Build steps

### Step 0: Tools and accounts

**Goal:** everything installed and every external account ready. No code yet.

**Do**

1. Install JDK 21 (LTS), Maven 3.9+, Docker Desktop, Node 20+, Git and IntelliJ IDEA.
2. Create: a Neon project, a Spotify developer app, a GitHub OAuth app, a Google Cloud OAuth client (enable the YouTube Data API), one LLM provider API key, a Cloudflare account.
3. Set each OAuth redirect URI to `http://localhost:8080/api/v1/auth/{provider}/callback`.
4. Put all secrets in a local `.env` file that is gitignored.

**How:** in Neon, copy the pooled connection string and create a branch named `dev`.

**Learn first:** what an OAuth client id, client secret and redirect URI are.

**Done when:** `java -version`, `mvn -v`, `docker version` and `node -v` all work, and you hold credentials for all three providers, the LLM and Neon.

### Step 1: Monorepo skeleton and shared library

**Goal:** one repo that builds every module with the same conventions.

**Do**

1. Create the repo with the layout from section 6.
2. Write the parent `pom.xml` (packaging `pom`) listing all modules. Pin the Java and Spring Boot versions here only.
3. Generate each service at start.spring.io (Maven, Java 21, Spring Web, Validation, Actuator, Data JPA, PostgreSQL Driver, Flyway) and move it into the repo as a module. Use the same Spring Boot version everywhere.
4. In `common`: correlation-id filter, a shared error helper built on `ProblemDetail`, shared DTO records.
5. Expose `/actuator/health` on every service.

**Done when:** `mvn clean verify` at the root builds all modules and each service starts with `/actuator/health` returning UP.

### Step 2: roast-service with fake stats

**Goal:** POST /roasts produces a real roast from hard-coded stats. This step teaches Spring Boot, JPA, Flyway and REST end to end.

**Do**

1. Write migration `V1__create_roasts.sql` using the roasts table from the data model.
2. Create the packages `api`, `service`, `domain`, `repo`, `client`.
3. Define `LlmClient` with one method `String generate(String prompt)` and implement it for your chosen provider with Spring `RestClient`.
4. Build prompts in a `PromptBuilder` class: system rules (tone, no attacks on protected traits, max length), the stats JSON, the tone. Keep a `PROMPT_VERSION` constant.
5. Implement POST /roasts (returns 202), GET /roasts/{id}, GET /roasts, DELETE /roasts/{id}. Generate asynchronously with a small executor. Status goes PENDING then READY or FAILED.
6. Add a timeout and one retry (Resilience4j) around the LLM call.
7. Write tests: unit test for `PromptBuilder`, integration test with Testcontainers Postgres, WireMock standing in for the LLM.

**How:** Neon connection config, secrets from the environment:

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 4
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
```

**Done when:** a curl POST returns 202, polling shows READY with roast text, tests pass, and Swagger UI loads.

### Step 3: connector-service

**Goal:** return normalized stats, GitHub first, then Spotify, then YouTube.

**Do**

1. Define the plug-in interface and the stats model:

```java
public interface Connector {
    Provider provider();
    ProviderStats fetch(String accessToken);
}
```

2. `GitHubConnector`: user, repos, events. Compute top languages, commit-hour histogram, abandoned repo count.
3. `SpotifyConnector`: top artists and tracks, recently played. Compute top genres and late-night ratio.
4. `YouTubeConnector`: liked videos and subscriptions. Compute top channels and categories. Watch history is not available through the API.
5. `GET /internal/v1/stats?userId=&providers=`: run connectors in parallel and return partial results if one provider fails.
6. While developing, pass a personal access token in a header. Redis comes in Step 4.
7. Save real sample responses as WireMock fixtures and test against them.

**Done when:** calling the endpoint with your own token returns clean stats for each provider, and roast-service now calls connector-service (5 second timeout) instead of using fake stats.

### Step 4: identity-service

**Goal:** Login with Spotify, GitHub or Google, a JWT cookie, and token handoff to the connector.

**Do**

1. Add `spring-boot-starter-oauth2-client` and configure three registrations from environment variables.
2. On login success: find or create the user and linked account (Flyway migration), store the provider access token in Redis encrypted with AES-GCM (TTL 15 minutes, key `tok:{userId}:{provider}`), then issue the JWT.
3. JWT: short expiry (about 60 minutes), signing key from the environment, sent in an httpOnly, Secure, SameSite=Lax cookie.
4. Endpoints: GET /me, POST /auth/logout, DELETE /me (deletes the account and asks roast-service and share-service to delete their data).
5. Change connector-service to read the token from Redis.

**Done when:** browser login sets the cookie, GET /me returns the profile, and a roast request fetches real stats with no manual token.

### Step 5: gateway

**Goal:** one public entry point for all API traffic.

**Do**

1. Use Spring Cloud Gateway (match the release train to your Spring Boot version).
2. Routes: `/api/v1/auth/**` and `/api/v1/me` go to identity, `/api/v1/roasts/**` to roast, `/s/**` and `/api/v1/public/**` to share.
3. JWT filter: read the cookie, verify the signature, add `X-User-Id`, and strip any `X-User-Id` sent by the client. Skip public routes.
4. Block `/internal/**`, enable CORS for the React origin only, add a request id.

**Done when:** every call goes through port 8080, and in Compose only the gateway side is reachable from outside.

### Step 6: quota, cache and resilience

**Goal:** protect the LLM budget.

**Do**

1. Add Redis to roast-service. Daily quota: counter key `quota:{userId}:{yyyymmdd}` with a TTL to midnight UTC. Expose GET /roasts/quota.
2. Cache: look up by `stats_hash + tone + prompt_version` before calling the LLM. A cache hit does not use quota.
3. Global daily spend counter. When it is exceeded, trip a circuit breaker and return a friendly message.
4. Support the `Idempotency-Key` header on POST /roasts.
5. Return 429 with `Retry-After` when the limit is hit.

**Done when:** the 4th roast of the day returns 429, identical stats skip the LLM, and a broken LLM URL ends in a FAILED roast with no crash.

### Step 7: share-service and OG cards

**Goal:** public links that unfurl with a preview image.

**Do**

1. Migration for the `shares` table.
2. `POST /internal/v1/shares`, called by roast-service on POST /roasts/{id}/share. Generate a 12-character nanoid.
3. Render a 1200x630 PNG with Java2D (`BufferedImage`), bundle a font, wrap the roast text.
4. Upload the PNG to Cloudflare R2 using the AWS SDK v2 (S3 compatible).
5. `GET /s/{id}` returns small server-rendered HTML with `og:title`, `og:description`, `og:image`, `twitter:card`, then redirects the browser to the SPA route `/r/{id}`.
6. `GET /api/v1/public/shares/{id}` returns JSON for the SPA. DELETE sets `revoked_at` and the link then returns 404.
7. Send `Cache-Control` headers so the CDN can cache.

**Done when:** pasting a link into WhatsApp, LinkedIn or X shows the card, and a revoked link stops working. Test with the platforms' link debuggers on a real domain.

### Step 8: React frontend

**Goal:** the full user journey in the browser.

**Do**

1. Vite + React + TypeScript in `web/`.
2. Pages: `/` landing with three login buttons, `/roast` (pick sources and tone, loading state), `/r/:id` public roast view, `/history`.
3. API client using `fetch` with `credentials: 'include'`. Use TanStack Query and poll GET /roasts/{id} every 1.5 seconds.
4. Handle 401 (send to login) and 429 (show when the limit resets).
5. Share button copies the public URL.
6. Dev proxy to the gateway in `vite.config.ts`.

**Done when:** login, roast and share all work locally in the browser, on a phone-sized screen too.

### Step 9: Docker and Compose

**Goal:** the whole system starts with one command.

**Do**

1. One multi-stage Dockerfile per service: Maven build stage, then a slim Java 21 JRE image. Set `JAVA_TOOL_OPTIONS=-Xmx256m`.
2. `deploy/docker-compose.yml` with all services, Redis and Caddy. Use healthchecks (install curl in the image), `restart: unless-stopped`, one shared network.
3. Only Caddy publishes ports 80 and 443. Everything else is internal.
4. Caddy serves the built React files and proxies `/api` and `/s` to the gateway.

```yaml
services:
  gateway:
    build: ./gateway
    env_file: .env
    restart: unless-stopped
  roast-service:
    build: ./roast-service
    env_file: .env
    restart: unless-stopped
  redis:
    image: redis:7-alpine
    restart: unless-stopped
  caddy:
    image: caddy:2
    ports:
      - 80:80
      - 443:443
    volumes:
      - ./Caddyfile:/etc/caddy/Caddyfile
```

**Done when:** `docker compose up --build` starts everything and the app works at `http://localhost`.

### Step 10: observability

**Goal:** find problems without guessing.

**Do**

1. JSON structured logs with the correlation id on every line.
2. Micrometer metrics, expose `/actuator/prometheus` internally only.
3. Add an external uptime monitor on the public URL and `/actuator/health`.
4. Add Prometheus and Grafana later, once there is traffic.

**Done when:** you can follow one request across all services using its correlation id.

### Step 11: CI/CD and VPS deploy

**Goal:** merging to main deploys automatically.

**Do**

1. GitHub Actions on pull request: `mvn verify`, then `npm ci && npm run build`.
2. On merge to main: build images, push to GHCR, SSH to the VPS, run `docker compose pull && docker compose up -d`.
3. VPS: Ubuntu LTS with 8 GB RAM, firewall allowing only 22, 80, 443, non-root user, SSH keys only, fail2ban, Docker installed.
4. DNS on Cloudflare with proxy on. Use a Neon `prod` branch for production.
5. Secrets live in GitHub Actions secrets and in a `chmod 600` `.env` on the server.

**Done when:** a merge to main updates the live site over HTTPS with no manual steps.

### Step 12: hardening and launch

**Security**

- Treat provider data as untrusted text inside the prompt so it cannot change the instructions. Filter roast output before saving.
- Per-IP rate limit at the gateway, strict CORS, input validation, Dependabot enabled.

**Privacy**

- Privacy policy page, working delete-my-data, tokens kept at most 15 minutes, sharing only on explicit user action.

**Provider rules (verify the current terms before launch)**

- Spotify development mode limits who can log in, so plan an extended-access application.
- Google needs OAuth app verification for sensitive scopes such as YouTube once you go beyond test users.

**Operations**

- Confirm Neon point-in-time restore works, reboot-test the VPS, load test (for example 50 concurrent roasts with k6), set an LLM spend alert.

**Quality**

- A README per service, published OpenAPI files, ADRs for D1 to D6.

## 8. Glossary

| Term | Meaning |
| --- | --- |
| Microservice | Small separate program with one job |
| Gateway | Single front door that routes requests |
| OAuth | Login through another site without sharing a password |
| JWT | Signed ID card carrying the user id and an expiry |
| Flyway | Tool that applies database changes in versioned scripts |
| Redis | Fast short-term store for counters and temporary data |
| OG card | Preview image and text shown when a link is shared |
| ADR | Short note recording why a decision was made |
