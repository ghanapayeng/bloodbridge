# BloodBridge backend

The backend exposes JSON APIs under `/api` and keeps all web pages public. API calls require an authenticated session, except for registration and login. Passwords are BCrypt hashes; the API never returns a password hash. State-changing endpoints require a CSRF token: first call `GET /api/auth/csrf`, then send its token in the returned header name (normally `X-XSRF-TOKEN`).

## MySQL setup

Create the database and least-privilege application account once, then let Flyway run the versioned schema migration on application startup:

```sql
CREATE DATABASE blood_bridge CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'bloodbridge_app'@'localhost' IDENTIFIED BY 'replace-with-a-long-random-password';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
    ON blood_bridge.* TO 'bloodbridge_app'@'localhost';
FLUSH PRIVILEGES;
```

Set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` before starting the app. Do not use MySQL `root` from the application.

The schema is in `src/main/resources/db/migration/V1__create_bloodbridge_schema.sql`.

| Table | Purpose |
| --- | --- |
| `app_users` | Account identity, normalized email, BCrypt password hash, timestamps. |
| `donor_profiles` | One optional donor profile per user: blood group, contact number, city, availability, last donation date. |
| `blood_requests` | Blood requests with requester, needed blood group, hospital, urgency, lifecycle status, and optional note. |
| `blood_request_interests` | A donor's response to a request. The unique constraint prevents duplicate responses. |
| `donations` | A donor's completed donation history, optionally linked to a BloodBridge request. |

## API surface

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/api/auth/register` | Create an account. |
| `POST` | `/api/auth/login` | Start a session using email and password. |
| `GET` | `/api/auth/csrf` | Issue the CSRF token required by authenticated writes. |
| `GET` | `/api/auth/me` | Read the signed-in account. |
| `POST` | `/api/auth/logout` | End the current session. |
| `GET`, `PUT` | `/api/donor-profiles/me` | Read or create/update the signed-in user's donor profile. |
| `GET` | `/api/donors?bloodGroup=&city=` | Find available, blood-compatible donors without exposing contact details. |
| `POST`, `GET` | `/api/blood-requests`, `/api/blood-requests/me` | Create a request or list open/owned requests. |
| `POST` | `/api/blood-requests/{id}/interests` | Register compatible donor interest. |
| `GET` | `/api/blood-requests/{id}/interests` | Request owner views donor contact details and responses. |
| `PATCH` | `/api/blood-requests/{id}/status` | Request owner changes request status. |
| `PATCH` | `/api/blood-requests/{id}/interests/{interestId}` | Request owner accepts or declines an interest. |
| `POST`, `GET` | `/api/donations`, `/api/donations/me` | Record or view the signed-in donor's history. |

Blood compatibility is enforced for red-cell donation matching. It is a matching aid only; clinical eligibility and compatibility checks remain mandatory.
