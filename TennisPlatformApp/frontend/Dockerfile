# syntax=docker/dockerfile:1

# ---- build stage -------------------------------------------------------------
FROM node:24-alpine AS build
WORKDIR /app

# Dependencies in their own layer: a source-only change does not reinstall them.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
# rewrites() in next.config.ts is evaluated at build time, not at start: the backend address
# is baked into the image here. Left out, every /api call would go to localhost:8081.
ARG BACKEND_URL=http://backend:8080
ENV BACKEND_URL=$BACKEND_URL \
    NEXT_TELEMETRY_DISABLED=1
# public/ is optional (it arrives with the PWA); the runtime stage copies it either way.
RUN mkdir -p public && npm run build

# ---- runtime stage -----------------------------------------------------------
FROM node:24-alpine AS runtime

# Never run the application as root.
RUN addgroup -S app && adduser -S -G app app

WORKDIR /app
ENV NODE_ENV=production \
    PORT=3000 \
    HOSTNAME=0.0.0.0 \
    NEXT_TELEMETRY_DISABLED=1

COPY --from=build --chown=app:app /app/.next/standalone ./
COPY --from=build --chown=app:app /app/.next/static ./.next/static
COPY --from=build --chown=app:app /app/public ./public
USER app

EXPOSE 3000
CMD ["node", "server.js"]
