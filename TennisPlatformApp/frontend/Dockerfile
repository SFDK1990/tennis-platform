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
# robots.txt and sitemap.xml are generated here too, with the public address in them. Left
# out, they would point search engines at localhost.
ARG SITE_URL
ENV BACKEND_URL=$BACKEND_URL \
    SITE_URL=$SITE_URL \
    NEXT_TELEMETRY_DISABLED=1
RUN npm run build

# ---- runtime stage -----------------------------------------------------------
FROM node:24-alpine AS runtime

# The server runs with `node server.js` and never needs a package manager, but the base image
# ships npm and yarn with their own dependency trees, and that is where the image's CVEs live
# (undici, brace-expansion, http-cache-semantics in October 2026), out of reach of npm audit
# and of our lockfile. The upgrade does for Alpine what the backend image does: a fix published
# before the base image is rebuilt with it.
RUN apk upgrade --no-cache \
    && rm -rf /usr/local/lib/node_modules/npm /usr/local/lib/node_modules/corepack \
        /usr/local/bin/npm /usr/local/bin/npx /usr/local/bin/corepack \
        /opt/yarn-* /usr/local/bin/yarn /usr/local/bin/yarnpkg

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
