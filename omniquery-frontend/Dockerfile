# syntax=docker/dockerfile:1.7
FROM node:22-alpine AS build
WORKDIR /workspace

COPY omniquery-frontend/package*.json ./
RUN --mount=type=cache,target=/root/.npm npm ci

COPY omniquery-frontend ./
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /workspace/dist /usr/share/nginx/html
EXPOSE 80
