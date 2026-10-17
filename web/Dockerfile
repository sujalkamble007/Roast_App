# Multi-stage Dockerfile for web frontend
FROM node:22-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . ./
RUN npm run build

FROM alpine:latest
WORKDIR /srv
COPY --from=builder /app/dist ./
CMD ["echo", "Web assets built"]
