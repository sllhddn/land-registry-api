# land-registry-api: How to use

This guide covers running the API, calling every endpoint, and running the tests.

## 1. Run with Docker (easiest)

Requires Docker with the Compose plugin.

```bash
git clone https://github.com/sllhddn/land-registry-api.git
cd land-registry-api
docker compose up --build
```

This starts PostgreSQL 16 and the API on port 8080. On first start Flyway creates the schema, then Hibernate checks
that every entity matches it.

| URL | Purpose |
|---|---|
| http://localhost:8080/swagger-ui.html | Interactive API documentation |
| http://localhost:8080/actuator/health | Health check (`{"status":"UP"}`) |

Stop with `Ctrl+C`, then `docker compose down` (add `-v` to delete the database volume).

## 2. Run locally with Maven

Requires Java 17+, Maven 3.9+ and a PostgreSQL database.

```bash
createdb landregistry                                   # or create it in pgAdmin / DBeaver
export DB_URL=jdbc:postgresql://localhost:5432/landregistry
export DB_USER=postgres
export DB_PASSWORD=yourpassword
mvn spring-boot:run
```

| Variable | Default |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/landregistry` |
| `DB_USER` | `landregistry` |
| `DB_PASSWORD` | `landregistry` |

## 3. Walk through the workflow

The examples use `curl`. The same calls are in `requests.http` for the IntelliJ or VS Code REST Client.

**Register a parcel**

```bash
curl -s -X POST localhost:8080/api/parcels -H 'Content-Type: application/json' \
  -d '{"lotNo":"lot-1024","district":"District A","areaSqm":1250.5,"tenure":"FREEHOLD"}'
```

Response `201 Created` with the new parcel. The lot number is stored in upper case (`LOT-1024`).

**Submit an application for it** (use the parcel `id` from the response)

```bash
curl -s -X POST localhost:8080/api/applications -H 'Content-Type: application/json' \
  -d '{"parcelId":1,"type":"TRANSFER","applicantName":"Siti Aminah","feeAmount":150.00}'
```

Response `201 Created`, status `SUBMITTED`, with a reference like `APP/2026/9F3A1C2B`.

**Move it through the workflow** (use the application `id`)

```bash
curl -s -X POST localhost:8080/api/applications/1/transitions -H 'Content-Type: application/json' \
  -d '{"status":"UNDER_REVIEW","remarks":"documents received"}'
curl -s -X POST localhost:8080/api/applications/1/transitions -H 'Content-Type: application/json' \
  -d '{"status":"APPROVED","remarks":"all checks passed"}'
```

**List and summarise**

```bash
curl -s 'localhost:8080/api/applications?status=UNDER_REVIEW&page=0&size=20'
curl -s localhost:8080/api/applications/summary
```

## 4. Workflow rules

| From | Allowed next steps |
|---|---|
| SUBMITTED | UNDER_REVIEW, WITHDRAWN |
| UNDER_REVIEW | APPROVED, REJECTED, WITHDRAWN |
| APPROVED | REGISTERED |
| REJECTED, REGISTERED, WITHDRAWN | none (final) |

A parcel can have only one open application (SUBMITTED, UNDER_REVIEW or APPROVED) of each type at a time.

## 5. Endpoints

| Method | Path | Success | Errors |
|---|---|---|---|
| POST | `/api/parcels` | 201 | 400 invalid input, 409 duplicate lot number |
| GET | `/api/parcels?page=&size=&sort=` | 200 paged | |
| GET | `/api/parcels/{id}` | 200 | 404 |
| POST | `/api/applications` | 201 | 400, 404 parcel not found, 409 open application exists |
| GET | `/api/applications?status=&page=&size=` | 200 paged | |
| GET | `/api/applications/{id}` | 200 | 404 |
| POST | `/api/applications/{id}/transitions` | 200 | 400, 404, 409 transition not allowed |
| GET | `/api/applications/summary` | 200 counts per status | |

Paged responses look like `{"content":[…],"page":0,"size":20,"totalElements":3,"totalPages":1}`.

## 6. Error responses

All errors follow RFC 7807 (`application/problem+json`):

```json
{
  "type": "about:blank",
  "title": "Validation failed",
  "status": 400,
  "detail": "One or more fields are invalid",
  "errors": { "lotNo": "must not be blank", "tenure": "must match \"FREEHOLD|LEASEHOLD\"" }
}
```

| Status | When |
|---|---|
| 400 | A field breaks validation (blank, too long, too many digits, unknown enum) |
| 404 | Parcel or application id does not exist |
| 409 | Duplicate lot number, second open application, or a transition the workflow forbids |

## 7. Run the tests

```bash
mvn verify
```

Tests use an in-memory H2 database in PostgreSQL mode, so no database is needed. Flyway runs the real migration and
Hibernate validates the entities against it, so a mismatch fails the build.

## 8. Change the database schema

1. Add a new file, e.g. `src/main/resources/db/migration/V2__add_survey_date.sql`. Never edit `V1__init.sql`.
2. Update the matching entity field.
3. Run `mvn verify`. If the entity and migration disagree, the context fails to start and the tests fail.

Sequence rule: any new sequence must use `INCREMENT BY 50` to match `allocationSize = 50` in `@SequenceGenerator`.

## 9. Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `Schema-validation: missing column` at start-up | Entity has a field with no column | Add a Flyway migration for it |
| `Schema-validation: wrong column type` | Entity type and column type differ | Align them in a new migration |
| `Connection refused` on start | PostgreSQL not running or wrong `DB_URL` | Check the database and environment variables |
| 409 on a transition you expected to work | The current status does not allow it | Check the workflow table above |
