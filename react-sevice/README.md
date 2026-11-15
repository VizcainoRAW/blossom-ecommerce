# Blossom storefront (React)

React 19 + TypeScript + Vite + Tailwind CSS single-page app for the Blossom store. See the [main README](../README.md) for the full project and [docs/architecture.md](../docs/architecture.md#frontend) for how the app is organized.

```bash
npm install
npm run dev                       # http://localhost:5173, proxies /api to http://localhost:8080
node --test scripts/*.test.ts     # unit tests for cart, product and validation logic
npm run lint                      # oxlint
npm run build                     # type-check + production build into dist/
```

In Docker, the [`Dockerfile`](Dockerfile) builds the app and serves it with nginx ([`nginx.conf`](nginx.conf)), which also proxies `/api` to the `spring` service.
