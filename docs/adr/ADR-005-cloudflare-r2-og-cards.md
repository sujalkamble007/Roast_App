# ADR-005: Java2D Dynamic Card Rendering and Cloudflare R2 Storage

## Status
Accepted

## Context
When users share roasts on WhatsApp, Twitter/X, and LinkedIn, social crawlers require standard 1200x630 OpenGraph images to generate rich link unfurls.

## Decision
1. Standard headless browsers (Puppeteer/Playwright) consume excessive memory (500MB+ per instance) in microservice architectures.
2. We render pixel-perfect 1200x630 PNG images directly in Java using Java2D (`BufferedImage`, `Graphics2D`, antialiased font metrics) in sub-100ms with negligible RAM overhead.
3. Cards are uploaded to Cloudflare R2 using AWS SDK v2 S3-compatible APIs for fast global edge CDN distribution with zero egress fees.

## Consequences
- Fast sub-100ms card generation.
- Zero egress bandwidth costs via Cloudflare R2.
