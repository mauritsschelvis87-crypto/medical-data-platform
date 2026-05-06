# Deployment

## Scope

This monorepo is prepared for:

- `apps/frontend`: Angular on Vercel
- `apps/backend`: Spring Boot on Render
- `apps/ai-service`: FastAPI on Render
- Supabase used only as PostgreSQL

Architecture constraints kept in place:

- Angular only calls Spring Boot
- Angular never calls the AI service directly
- Spring Boot calls the AI service through `AI_SERVICE_URL`
- Spring Boot uses Supabase only through JDBC environment variables
- no production URLs or secrets are hardcoded

## Assumptions

- The frontend is deployed as a static Angular build on Vercel. Because Vercel serves static output, `API_BASE_URL` is injected at build time.
- The backend and AI service are deployed as Docker-based Render web services.
- Supabase Auth, PostgREST, Storage, and anon keys are intentionally not used by the Angular app.
- The production medication catalog CSV path is optional. If no `MEDICATION_CATALOG_SOURCE_PATH` is set, the backend skips file-based catalog bootstrap instead of relying on a local dev path.

## Spring Boot production behavior

- Active profile: `prod`
- Port binding: `server.port=${PORT:8081}`
- JPA schema mode: `ddl-auto=validate`
- Flyway enabled with validation and `baseline-on-migrate=true`
- Prepared statements disabled at the JDBC driver level by default with `SPRING_DATASOURCE_PREPARE_THRESHOLD=0`

That `prepareThreshold=0` setting is there to keep PostgreSQL JDBC compatible with Supabase transaction pooler connections if you choose port `6543`.

## Supabase PostgreSQL

Required backend variables:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

Preferred connection-string choices:

1. For long-lived Render services: use the direct connection string or the Supabase session pooler if you need pooled IPv4 access.
2. If you explicitly choose the Supabase transaction pooler for cloud hosting, use port `6543` and keep prepared statements disabled.

Examples:

```text
jdbc:postgresql://db.<project-ref>.supabase.co:5432/postgres?sslmode=require
jdbc:postgresql://aws-0-<region>.pooler.supabase.com:5432/postgres?sslmode=require
jdbc:postgresql://aws-0-<region>.pooler.supabase.com:6543/postgres?sslmode=require
```

If you use the transaction pooler on `6543`, this repo is already configured to work with it through:

- `spring.datasource.hikari.data-source-properties.prepareThreshold=0`

SSL guidance:

- keep `sslmode=require` in the JDBC URL
- verify SSL enforcement in the Supabase dashboard if your project policy requires it

## Angular on Vercel

Root directory:

- `apps/frontend`

Build behavior:

- Vercel runs `npm run build:vercel`
- that script generates Angular environment files from `API_BASE_URL`
- output directory is `dist/frontend/browser`

Required Vercel environment variable:

- `API_BASE_URL=https://<BACKEND_ON_RENDER>/api`

## Render services

The repo root contains `render.yaml` with:

- `medical-data-platform-backend`
- `medical-data-platform-ai-service`

Backend variables:

- `SPRING_PROFILES_ACTIVE=prod`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_DATASOURCE_PREPARE_THRESHOLD=0`
- `AI_SERVICE_URL=https://<AI_SERVICE_ON_RENDER>`
- `FRONTEND_URL=https://<FRONTEND_ON_VERCEL>`
- `MEDICATION_CATALOG_SOURCE_PATH=` optional

AI-service variables:

- `MODEL_PATH=/app/models`
- `PORT` is supplied automatically by Render for web services unless you override it manually

## Manual deployment order

1. Create the Supabase project and capture the database credentials.
2. Deploy the AI service on Render and note its public URL.
3. Deploy the backend on Render using the Supabase credentials and the AI-service URL.
4. Deploy the frontend on Vercel using the backend `/api` URL.
5. Update `FRONTEND_URL` in the backend if you attach a custom Vercel domain after the first deploy.

## Health endpoints

- Spring Boot: `/api/health`
- AI service: `/health`

## Security notes

- Do not place Supabase anon keys in Angular for medical data access.
- Do not configure Angular to use Supabase JS clients for patient data.
- Current backend logging was reduced so patient identifiers are not emitted in operational warnings and generic error logs.
