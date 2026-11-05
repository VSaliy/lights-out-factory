# Frontend

Angular 21 standalone application. See the repository-root `README.md` for the local workflow and API instructions.

From this directory:

```sh
npm ci
npm test -- --watch=false
npm run build
npm start
```

The development server proxies `/api` and `/actuator` to `http://localhost:8080`. No browser end-to-end runner is configured yet.
