# Community forum with AI assistance

## What this module does

The English-language forum lets authenticated learners create, edit and delete questions, write replies and accept an answer. Administrators can moderate content. Questions can be linked to an existing course or posted as general discussions. The page is available at `http://localhost:4200/#/forum` and in the sidebar.

The forum has four AI workflows: semantic duplicate detection, cited discussion summaries, automatic editable topic tags, and course/resource suggestions with real links. It does not publish generated answers or change accepted-answer state. AI assistance is optional: an outage never prevents ordinary forum participation. See [the feature guide](forum-ai-features.md) for the new workflows and owner-run checks.

## Start the merged platform

From the mergee project root, run `./Start-SmartLearn.ps1`. Stop with `./Stop-SmartLearn.ps1`. Backend changes require a rebuild before restarting. This starts the same merged platform, including flashcards, video and the branch modules.

Open `http://localhost:4200/#/forum` after signing in. Existing mergee accounts and passwords remain unchanged. The local administrator is `admin@smartlearn.local / admin123`; seeded learners are `ahmed@example.com` and `sarah@example.com`, both with `password123`.

Forum discussions and replies use mergee's persistent `.local/data/smartlearn` H2 database. The FORum database and its unrelated modules were not imported. The forum starts empty; create your own discussion. The optional source demo-data seeder is inactive in the local profile.

## Connect the AI

The forum reuses `FlashcardAiClient`, which supports a server-side API key and the existing session-specific ChatGPT connection. Credentials are never included in forum API responses or frontend source.

Before starting the backend, configure the key and a model your account can access:

```powershell
$env:OPENAI_API_KEY = '<your API key>'
$env:OPENAI_MODEL = '<accessible model supporting structured outputs>'
```

Alternatively, set `OPENAI_API_KEY_FILE` to a local key file. The main launcher resolves the default to the existing project-root `.local/openai-api-key.txt` and uses that file from the project-root working directory. A root `.env` is not automatically loaded. The model default is inherited from the existing project (`gpt-6-luna`); this default alone is not evidence that a particular account can access it. A configured status means credentials exist, not that a live request has succeeded.

If you use the project's existing ChatGPT connection, open Flashcards in another tab, finish sign-in, return to the forum and refresh the connection status. The existing callback returns to Flashcards. Account access and quota must still be checked with a real request.

The request uses the [OpenAI Responses API structured-output format](https://developers.openai.com/api/docs/guides/structured-outputs). A strict JSON schema constrains output shape; the forum additionally validates IDs, classification labels, coverage, repeated IDs and explanation length. API failures and invalid output are displayed in English.

## Demonstration

1. Sign in as Ahmed and open **Community forum**.
2. Create a route-guard discussion first, then add a reply and accept it to demonstrate the workflow. This merged copy does not seed forum posts.
3. Click **Ask a question**. Choose **General discussion**.
4. Title: `How do I stop anonymous visitors from opening my Angular dashboard?`
5. Details: `Users who are not signed in should be redirected to the login page before they can access the dashboard route. How should I implement this?`
6. Click **Check similar questions**. With a working AI connection, inspect the classification and explanation for the route-guard discussion. The login-styling discussion should be related, not a duplicate. Actual results may vary and must be checked.
7. Open a suggestion in a separate tab, preserving the draft.
8. Repeat the identical check to demonstrate the short-lived cache.
9. Change the details: old AI suggestions disappear. Check again after the cooldown, or publish immediately.
10. Sign in as Sarah in another browser session, add a reply, then use Ahmed's session to accept it.

## Architecture and endpoints

Angular `ForumComponent` -> `/api/forum` -> `ForumService` / `ForumAiService` -> JPA repositories or `ForumAiClient` -> existing AI transport -> OpenAI.

| Endpoint | Purpose |
|---|---|
| GET /api/forum/discussions | Search and filter, 20 results per page |
| POST /api/forum/discussions | Create a question |
| GET, PUT, DELETE /api/forum/discussions/{id} | Read, edit or delete a question |
| POST /api/forum/discussions/{id}/replies | Add a reply |
| PUT, DELETE /api/forum/discussions/{id}/replies/{replyId} | Edit or delete a reply |
| PUT /api/forum/discussions/{id}/accepted-reply | Accept an answer, or pass null to reopen |
| GET /api/forum/ai/status | Report credential presence, configured model, prompt versions and features |
| POST /api/forum/ai/tags | Generate editable topic tags for a draft |
| POST /api/forum/discussions/{id}/ai/summary | Summarize the saved thread with source references |
| POST /api/forum/discussions/{id}/ai/learning | Select courses and reviewed resources, with real links |
| POST /api/forum/ai/check | Analyze a draft; optional excludeId omits the question being edited |

Questions also store up to six editable topic tags in `forum_discussions.tags`; the existing search includes these tags. Existing discussions with no tags remain valid. The default schema-update configuration adds the column at startup.

Question bodies have a 6,000-character limit; titles have a 180-character limit. Replies are also limited to 6,000 characters. Authors and timestamps are assigned on the server. The caller cannot select a different author. An accepted reply must belong to the discussion. Editing a question or its accepted reply reopens the discussion. Deleting a discussion removes its replies in the same transaction.

Author IDs and names are stored as snapshots, without introducing foreign-key dependencies into the existing user-deletion workflow. Course IDs are validated when saving; the course name is stored as a snapshot. Renaming or deleting a course does not retroactively rewrite historical forum posts. This is a deliberate simplicity tradeoff for this module.

## How the AI decision works

1. Validate the draft and chosen course.
2. Select up to 20 recently updated discussions, within the selected course when one is chosen. General questions compare across all courses. Exclude the question being edited.
3. Send question text, opaque discussion IDs and the draft. Candidate details are truncated to 1,200 characters. Names, email fields and replies are omitted; personal information typed inside a body is still part of that text.
4. Ask the model to classify every candidate as DUPLICATE, RELATED or UNRELATED, with a short English explanation.
5. Validate the complete response. Reject invented IDs, missing or repeated candidates, unknown labels and invalid explanations.
6. Show at most five useful suggestions, with probable duplicates first. Titles and links come from the database, never from model-generated URLs.

This section describes duplicate detection. For summary coverage, tag persistence and learning-link selection, see [forum AI features](forum-ai-features.md).

The system does not compute a probability of correctness. It labels matches as probable and leaves the publishing decision to the learner. The exact production prompt lives in `ForumAiClient.PROMPT` and is versioned as `forum-v2`.

## Optimization and limits

- At most 20 candidates and truncated candidate bodies bound each request.
- Results are cached for five minutes, with at most 100 entries. The cache key includes the draft, candidate content and update/resolution state, model and prompt version. This cache contains public-forum analysis and is shared within the application instance.
- Each learner has a 10-second cooldown on new model requests and at most one in-flight request. There are at most 10 simultaneous forum AI requests per application instance. Cached responses do not incur a new API call.
- Empty scopes do not call AI. The UI explicitly reports that no call was needed.
- Only the newest discussions are scanned: an older duplicate can be missed. The UI reports scanned and available counts. Candidate-body truncation can also remove an important constraint.
- Classification is subjective at the RELATED/UNRELATED boundary. Review labels before treating the evaluation set as ground truth.
- Prompt instructions treat post text as untrusted data, but prompt-injection resistance is not guaranteed. The model cannot modify posts, follow generated links, or accept answers.
- No numerical cost claims are made without real usage and current provider pricing.

## Owner-run tests and measured AI evaluation

```powershell
# From backend/
mvn.cmd test
# From frontend/
npm.cmd test -- --watch=false
npm.cmd run build
# From project root, with an API key configured locally
python scripts/evaluate-forum-ai.py --model <your-model> --prompt both
```

`ForumTests` covers authentication, CSRF, CRUD, ownership, moderation, accepted-answer integrity, validation and the AI endpoint using a mocked provider. `ForumAiServiceTests` covers response validation, empty scopes, caching, changed content, candidate limits and cooldown. Frontend tests cover late AI results, late discussion requests, draft changes and outages.

The separate live evaluation script reads the improved prompt directly from Java and compares it with a simple baseline on 12 synthetic labeled pairs. It saves predicted labels, explanations, latency, returned token usage, accuracy including errors, duplicate precision/recall and false-duplicate counts. HTTP failures are recorded, not replaced by fabricated classifications. Use `--limit 1 --prompt improved` for a small initial connectivity check. Full `--prompt both` can make up to 24 API calls and consumes provider quota. It evaluates the classifier with one candidate per case; separately test the application with multiple candidates and real course filters.

Use the demonstration above and [merge notes](../MERGE-NOTES.md) for end-to-end checks. Do not describe mock-provider tests as proof of model quality. Add actual live results to the final report only after running the evaluation. See `../MERGE-NOTES.md` for this integration and its validation results.


