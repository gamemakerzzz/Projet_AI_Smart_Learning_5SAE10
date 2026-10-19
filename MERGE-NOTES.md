# Merged SmartLearn

## Selected branch update (2026-10-19)

Imported only the requested module updates from `nesrine-mon-travail` commit `f3b3901` (including `6005dfa`): exams, sessions, notification read controls and automatic reminders, chapters to review, planning, learner analysis and AI planning. Statistics had no upstream source changes; its endpoints were checked. Added authenticated `/planification-ia` and `/analyse-ia` pages without replacing the existing navigation or login fix. Admin-only exam editing and learner ownership checks follow the updated branch.

The analysis/planning service stays on port 8001. The recommendation service stays on port 8000, with only session scoring added; the upstream duplicate profile class that would break recommendations was not imported. The session Random Forest was rebuilt locally using the branch's synthetic 5,000-row training script. Scores are estimates from synthetic data, not validated predictions of real learner outcomes. AI planning previews revisions before exams; the existing Planificateur persists sessions.

Verification: 88 backend tests (86 passed, two optional renderer tests skipped), 36 frontend tests passed, production build passed with existing size warnings. Live HTTP checks passed for course filtering, revisions before exams, real model scoring through the backend, recommendations, module reads and flashcard/tutor status. Added integration tests for admin exam permissions, private notifications, read-all, planning ownership/date validation, and reminder deduplication. All 63 protected backend/frontend Flashcards, Forum and Tutor source files retain their pre-update hashes. Their credentials and data were retained. A backend restart expires browser sessions/personal ChatGPT connections.

Pre-update source copies, database backup, protected hashes and exact scope are under `.local/branch-update-backup`; logs and HTTP results are `.local/branch-*`. No changes pushed to GitHub.

## AI Tutor and Mind Map integration

The full tutor module from `main` commit `0c42021` is now included at `/#/tutor`: Chat with course context, Explain, Summarize, Mind Map, private saved conversations and a sidebar link. Only tutor sources and its required integration changes were imported. Groq has a dedicated server-side key file `.local/groq-api-key.txt`; it does not reuse the forum/flashcard OpenAI credentials. See [tutor setup and verification](docs/tutor.md). The user does not have a Groq key yet, so all generation checks used a synthetic provider and live Groq calls remain unverified. Existing modules and stored data are retained.

## Forum integration

Forum saving/loading responsiveness and explicit Luna request settings were subsequently fixed. See [performance verification](docs/forum-performance.md) for the cause, changes, timings, and current test results.

The community forum from `C:/Users/azizb/Desktop/software/FORum` is now included at `http://localhost:4200/#/forum`, with a sidebar entry for both learners and administrators. Only forum sources, resources, tests and guides were imported. The original FORum folder was not modified (179 source/support file hashes verified unchanged). Existing mergee modules, accounts and database stay in place; the forum adds `forum_discussions` and `forum_replies` tables to the same database. No source forum posts or unrelated modules were imported.

The forum supports questions, replies, course filters, tags, search, accepted answers, owner editing and administrator moderation. All four AI features are integrated: duplicate-question detection, cited discussion summaries, editable topic tags and course/resource suggestions. They reuse the existing Flashcards AI connection. Open Flashcards, complete **Continue with ChatGPT**, then return to Forum and refresh the connection status. A backend restart requires signing in and reconnecting again. There is no additional Python service to configure.

Verification after forum integration: production frontend build passed; 28 frontend tests passed. Final backend reports contain 74 tests, 72 passed and two optional renderer tests skipped. The earlier actual Piper rendering verification remains valid; renderer code was not replaced. New insight tests cover generated/editable/saved tags, real summary citations, rejection of invented citations/resource IDs, and real course links. All AI-provider output in automated tests is mocked; live model quality and new generation remain unverified without an AI connection.

Live HTTP checks passed for learner login, question creation, replies, accepted answers and tag search; temporary verification content was removed afterward. The forum returns an actionable HTTP 503 when AI is disconnected, while ordinary posting still works. Existing video capabilities remain available for French, English and Arabic. Logs are `.local/forum-frontend-build.log`, `.local/forum-frontend-tests.log`, `.local/forum-backend-tests.log` (full suite), and `.local/forum-insights-tests.log` (corrected test fixture and successful package). See [forum guide](docs/forum.md) and [AI features](docs/forum-ai-features.md).

This folder combines `nesrine-mon-travail` at `c25756a` with the flashcard and narrated-video implementation from the local SmartLearn working copy (`c10070c`). The original SmartLearn folder is used only as a read-only source. No changes have been pushed to GitHub.

## Run on this computer

From PowerShell in this folder:

```powershell
.\Start-SmartLearn.ps1
```

Open http://localhost:4200/#/login. The local demonstration administrator is `admin@smartlearn.local` with password `admin123`. The seeded learners are `ahmed@example.com` and `sarah@example.com`, both with password `password123`. The administrator email/password can be overridden with `SMARTLEARN_ADMIN_EMAIL` and `SMARTLEARN_ADMIN_PASSWORD` before the first launch.

Stop this copy with `./Stop-SmartLearn.ps1`. Rebuild backend source changes with `./Start-SmartLearn.ps1 -Build` after stopping it. Angular reloads frontend source changes automatically.

The launcher starts recommendations on 8000, learner analysis on 8001, backend on 8081, and frontend on 4200. It refuses to reuse an unrelated application on these ports. Logs are in `.local`. Java and Maven are installed under `.local/tools`; Python services use `.venv`.

The `local` Spring profile stores a persistent H2 database in `.local/data`, entirely inside this folder. It does not connect to or change SmartLearn's MySQL database. Existing users, saved decks, videos and browser ownership cookies from the original installation are not migrated. The normal MySQL configuration remains available when launching without the local profile.

## Included features and integration fixes

- Branch modules: formations, categories, learners, profiles, recommendations, exams, sessions, notifications, planning, chapters to review, statistics and learner analysis.
- Local flashcards: PDF/PPTX/DOCX extraction, saved decks, explanations, study marks, narrated video generation, captions, seeking and downloads.
- Piper voice runtime and French, English and Arabic models, including their model cards, plus the same FFmpeg renderer as the original installation. These are ignored local assets.
- Flashcard routes, navigation, upload limits, backend dependencies, permissions and exception handling merged without removing branch ownership checks.
- Learner analysis uses its own Python service and configurable `IA_SERVICE_URL`. Learners can access their own analysis through the authenticated API.
- The chapters-to-review page imports the Angular directives required to display its contents.
- Compose indentation repaired and the missing analysis service added. Windows video dependencies are local; the Docker backend requires separate Linux speech/FFmpeg configuration. Docker is not the verified startup path for this copy.

## Flashcards and video

Sign in, open **Flashcards documents**, upload a document and generate a deck. Then select **Générer la leçon vidéo**. Narration is free and local; writing cards and lesson scripts requires the configured OpenAI connection/key. A fresh personal connection is needed if using browser sign-in rather than the server key. Existing local server key configuration was copied into this copy's ignored `.local` directory without printing its contents. Tokens and source database contents were not copied.

The copied API-key file is empty, so select **Continue with ChatGPT** to establish a fresh personal connection before generating cards or lesson scripts. The old server's temporary connection cannot be transferred. Live AI generation was not tested without that connection.

The flashcard feature guide is in [docs/flashcards.md](docs/flashcards.md). Local rendering uses `.local/voice-runtime/Scripts/python.exe`, `.local/voices`, and `.local/video-tools/imageio_ffmpeg/binaries`.

## Verification

Frontend build and tests:

```powershell
cd frontend
npm run build
npm test -- --watch=false
```

Backend tests, including actual three-language narration and MP4 generation (from the project root, with Java 17 and Maven on PATH):

```powershell
mvn -f backend/pom.xml "-Dmaven.repo.local=$PWD/.local/m2" "-Dsmartlearn.test.local-speech=true" test
```

Python recommendations: `.venv/Scripts/python.exe -m unittest discover -s mla-service -p test_main.py`.

Verified on this computer:

- Backend package: successful, 57 tests passed; one legacy Windows-system-voice test skipped. Piper's real three-language speech and MP4 integration test passed.
- Frontend production build: successful; 22 tests passed. Two size-budget warnings remain (526 kB initial bundle and 7.85 kB flashcard stylesheet).
- Python recommendation suite: 5 tests passed. Both live Python health endpoints and a live learner-analysis request passed.
- Full local launcher, administrator login, merged navigation, flashcard screen, and video capabilities (`fr`, `en`, `ar`) verified. No paid/live AI-generation request was made.
- The original 148 source and support files recorded before copying have identical SHA-256 hashes afterward.

The actual generated renderer test video is `.local/piper-video-preview.mp4`. Detailed results are recorded in `.local` logs and Maven's `backend/target/surefire-reports`.
