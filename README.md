# StuHub

StuHub is a student materials platform. Students upload PDF notes, search and bookmark materials shared by others, rate them, generate AI summaries and flashcards, and ask or answer questions in a community Q&A board called **AskHub**.

The backend is a Spring Boot application that also serves the single-page frontend, so one process runs the whole app.

## Features

- **Accounts** – register and log in with email and password; the API uses JWT bearer tokens (1 hour lifetime).
- **Materials** – upload PDFs with title, subject, course code and tags; search, sort by rating, downloads or date; download files.
- **Bookmarks and courses** – bookmark materials and course codes; a course page shows your bookmarked materials plus recommended ones for that course.
- **Ratings and reviews** – rate materials 1–5 with an optional comment; average ratings are kept on each material.
- **AI tools** – generate a summary, flashcards, or both for a bookmarked material using the OpenAI Assistants API (`gpt-4o` with file search).
- **Coins** – uploading a material earns 1 coin; each AI generation costs 1 coin.
- **AskHub** – post questions (with priority and an optional image), answer them, and browse or search recent questions.

## Tech stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.6 (Web, Security, Data JPA, Validation, Actuator) |
| Auth | JWT (jjwt 0.12.6), BCrypt password hashing |
| Database | PostgreSQL in production, H2 in-memory for local development |
| Migrations | Flyway (`src/main/resources/db/migration`) |
| AI | OpenAI Assistants API over `java.net.http` |
| Frontend | Single HTML/CSS/JavaScript page in `src/main/resources/static/index.html` |
| Build | Maven (wrapper included) |

## Project structure

```
.
├── pom.xml
├── mvnw, mvnw.cmd, .mvn/          Maven wrapper
└── src
    ├── main
    │   ├── java/com/ffenf/app
    │   │   ├── BackendApplication.java
    │   │   ├── admin/             Upload-folder cleanup endpoint
    │   │   ├── ai/                AI generation controller and OpenAI services
    │   │   ├── askhub/            Q&A questions, answers and images
    │   │   ├── auth/              Register/login, JWT service and filter
    │   │   ├── config/            Security, CORS, OpenAI and request logging
    │   │   ├── domain/            JPA entities
    │   │   ├── health/            /health endpoint
    │   │   ├── materials/         Upload, search, details and download
    │   │   ├── profile/           Profile, stats, bookmarks and courses
    │   │   ├── repo/              Spring Data repositories
    │   │   ├── reviews/           Ratings and reviews
    │   │   └── storage/           Local file storage
    │   └── resources
    │       ├── application.properties
    │       ├── db/migration/      Flyway SQL migrations (V1–V6)
    │       └── static/index.html  Frontend
    └── test/java/...              Spring context test
```

## Getting started

### Prerequisites

- Java 21 or newer
- PostgreSQL 12+ (optional; H2 is used if no database is configured)
- An OpenAI API key (optional; only needed for AI features)

### Run locally

```bash
git clone https://github.com/gopikadasari/stuhub.git
cd stuhub
./mvnw spring-boot:run
```

Open <http://localhost:8080>. Without a `DB_URL` the app uses an in-memory H2 database, so data is lost when it stops.

### Build a JAR

```bash
./mvnw clean package -DskipTests
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

## Configuration

All settings are read from environment variables (see `src/main/resources/application.properties`).

| Variable | Purpose | Default |
|---|---|---|
| `DB_URL` | JDBC URL, e.g. `jdbc:postgresql://localhost:5432/stuhub` | H2 in-memory |
| `DB_USERNAME` | Database user | `sa` |
| `DB_PASSWORD` | Database password | empty |
| `DB_POOL_MAX` | Max connection pool size | `10` |
| `APP_JWT_SECRET` | Base64 key used to sign JWTs (at least 256 bits) | built-in development key |
| `OPENAI_API_KEY` | OpenAI key for summaries and flashcards | empty (AI disabled) |
| `STORAGE_PATH` | Folder for uploaded files | `/tmp/uploads` |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins | all origins |
| `PORT` | HTTP port | `8080` |
| `JPA_SHOW_SQL` | Log SQL statements | `false` |

**Always set `APP_JWT_SECRET` outside local development.** Generate one with:

```bash
openssl rand -base64 32
```

Example using PostgreSQL:

```bash
createdb stuhub
export DB_URL=jdbc:postgresql://localhost:5432/stuhub
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export APP_JWT_SECRET=$(openssl rand -base64 32)
export OPENAI_API_KEY=sk-...
./mvnw spring-boot:run
```

> **Note:** migration `V6__cleanup_all_data.sql` empties all data tables the first time it runs against a database. It has no effect on a fresh database, but review it before pointing the app at a database that already holds data.

## API overview

Authenticated requests send `Authorization: Bearer <token>`.

### Auth
| Method | Path | Description |
|---|---|---|
| POST | `/auth/register` | Body `{ "email", "name", "password" }` → `{ "token" }` |
| POST | `/auth/login` | Body `{ "email", "password" }` → `{ "token" }` |

### Materials
| Method | Path | Description |
|---|---|---|
| POST | `/materials/upload-new` | Multipart: `file` (PDF), `title`, `subject`, `courseCode`, `tags` |
| GET | `/materials/search` | Query: `q`, `page`, `size`, `sortBy` (`createdAt`, `avgRating`, `downloadsCount`, …), `sortDir` |
| GET | `/materials/{id}` | Material details, summary and flashcards |
| POST | `/materials/{id}/download` | Increments download count and returns the file URL |
| GET | `/materials/{id}/file` | Downloads the PDF |

### Reviews
| Method | Path | Description |
|---|---|---|
| POST | `/reviews/{materialId}` | Body `{ "rating": 1-5, "comment" }`; creates or updates your review |
| GET | `/reviews/{materialId}` | All reviews for a material |
| GET | `/reviews/{materialId}/my` | Your review for a material |

### AI
| Method | Path | Description |
|---|---|---|
| GET | `/ai/generate/{materialId}?type=summary\|flashcards\|both` | Generates content (costs 1 coin) |
| GET | `/ai/job/{jobId}` | Status of a generation job |
| GET | `/ai/material/{materialId}` | Stored summary and flashcards |

### Profile, bookmarks and courses
| Method | Path | Description |
|---|---|---|
| GET | `/profile/me` | Profile with coins, upload and review counts |
| GET | `/profile/stats` | Coins, uploads, reviews and total downloads |
| GET | `/profile/my-uploads` | Your uploaded materials (paged) |
| GET | `/profile/my-reviews` | Reviews you have written |
| GET | `/profile/me/materials` | Bookmarked materials |
| POST | `/profile/me/materials/{id}/bookmark` | Bookmark a material |
| POST | `/profile/me/materials/{id}/unbookmark` | Remove a material bookmark |
| GET | `/profile/me/courses` | Bookmarked courses with material counts |
| POST | `/profile/me/courses/bookmark?courseCode=` | Bookmark a course |
| POST | `/profile/me/courses/unbookmark?courseCode=` | Remove a course bookmark |
| GET | `/profile/me/courses/{courseCode}/materials` | Bookmarked and recommended materials for a course |

### AskHub
| Method | Path | Description |
|---|---|---|
| GET | `/askhub/questions` | Questions (paged; `page`, `size`, `sortBy`, `sortDir`) |
| POST | `/askhub/questions` | Multipart: `title`, `description`, `courseCode`, `subject`, `tags`, `priority`, `image` |
| GET | `/askhub/questions/{id}` | Question with its answers |
| GET | `/askhub/questions/search?query=` | Search questions, optionally by `courseCode` |
| GET | `/askhub/questions/unanswered` | Questions with no answers |
| POST | `/askhub/questions/{id}/answers` | Multipart: `content`, `displayName`, `image` |
| GET | `/askhub/images/{path}` | Serves an uploaded question or answer image |

### Health
| Method | Path | Description |
|---|---|---|
| GET | `/health` | Application and storage status |

## Database

Flyway creates and updates the schema on startup. Tables: `users`, `materials`, `reviews`, `coin_transactions`, `ai_jobs`, `course_bookmarks`, `material_bookmarks`, `questions`, `answers`.

## Testing

```bash
./mvnw test
```

## Known limitations

- Several API routes are currently open without authentication in `SecurityConfig`; tighten these before exposing the app publicly.
- Uploaded files are stored on the local disk at `STORAGE_PATH`.
- AI generation runs inside the HTTP request and can take a few minutes for large PDFs.
- Accepting and voting on AskHub answers are not working yet.
