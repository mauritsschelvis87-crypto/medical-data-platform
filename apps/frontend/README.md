# Frontend

Angular frontend for the medical data platform.

## What the UI currently shows

- patient search with live backend results
- patient detail page
- latest vital signs
- risk summary area
- timeline area
- consult notes area
- medications area

For the current imported full dataset, the most important working screens are:

- patient search
- patient detail
- vital signs display

## Local URL

- `http://localhost:4200`

## Start locally

From `apps/frontend`:

```powershell
npm install
npm start
```

Build:

```powershell
npm run build
```

## Backend dependency

The frontend expects the backend at:

- `http://localhost:8081/api`

Before starting the frontend, make sure:

1. PostgreSQL is running.
2. The backend is running.
3. The full dataset has been generated and imported.

## Expected local flow

1. Run the notebook pipeline in `apps/ai-service`.
2. Start the Spring Boot backend.
3. Import the generated dataset into the backend.
4. Start the Angular frontend.
5. Search for a patient.

## Search examples

Known working example after full import:

- `Thomas`

You can also search by:

- patient number
- first name
- last name
- full name
- birth date if supported by the backend search input

## Data expectations

The frontend is aligned with the current backend contract:

- UUID patient ids
- patient address nested under `address`
- vital signs stored one measurement per row in the backend and aggregated in the UI for display

## Current empty states

The current imported dataset does not currently preload:

- predictions
- timeline events
- consult notes
- medication records

So those panels can still render empty-state content even though the pages themselves work correctly.
