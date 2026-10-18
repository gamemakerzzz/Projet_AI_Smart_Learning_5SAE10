# Document flashcards (PDF, PPTX, DOCX)

Start at `http://localhost:4200/#/login`. After SmartLearn login, administrators land on the dashboard and learners on their personal home. Both provide **Ouvrir les flashcards**, and the authenticated sidebar includes Flashcards documents. Direct visits to `/#/flashcards` redirect logged-out visitors to login. Flashcard and video APIs require an authenticated SmartLearn account; administration remains restricted by role. Only the GET OpenAI OAuth callback remains public because it returns on the loopback callback host and validates its own one-time state. POST, PUT, PATCH and DELETE requests retain CSRF protection. The UI obtains `/api/auth/csrf` before mutations.

## Connect Luna

Every signed-in SmartLearn user can click **Continue with ChatGPT**, without an administrator role. This starts OpenAI's dynamic registration and OAuth/PKCE flow for locally hosted apps. The user signs in and authorizes eligible ChatGPT-plan requests on OpenAI's page. Availability depends on OpenAI's preview eligibility and the user's plan, workspace policy, permissions, and model access. The application cannot guarantee that every OpenAI account is eligible for inference.

The callback is `http://127.0.0.1:8081/api/flashcards/openai/callback`. The backend binds a random, single-use, five-minute state and PKCE verifier to the original visitor's session. It verifies the ID token signature, issuer, expiry, audience and nonce, and requires the inference scopes before saving credentials. OAuth tokens remain server-side in that visitor's temporary session, not in browser storage or shared configuration. The sign-in browser returns to `http://localhost:4200/#/flashcards`; keep the frontend on `localhost` for session continuity. If serving the frontend from a different hostname, update the return URL first.

Personal connections use streaming Responses requests with `store: false`, array input and no unsupported output-token limit. Access tokens refresh as needed. Disconnecting discards the local connection and pending authorization; it does not revoke the app in ChatGPT settings. Session expiration or backend restart requires a fresh connection. No existing Codex credentials are read or reused. The host identifier is retained in `.local/openai-host-id.txt`; tokens are not written to disk. An active session reuses its issued client ID for reauthorization.

This dynamic registration path is a local loopback integration, not a production web OAuth registration. A hosted website needs the appropriate OpenAI client registration, registered callbacks and deployment configuration. Do not expose the local callback flow unchanged as a public hosted service.

The backend calls the OpenAI Responses API using `gpt-6-luna` and structured output. A separate server API key remains an optional fallback for anonymous generation when no personal connection is selected. Personal requests always use their own authorized connection.

For this local installation, save only the API key in `.local/openai-api-key.txt` at the project root. The running backend reads that file for each request, so no restart is needed when the file is added. Refresh the flashcard page afterward. This file is ignored by Git. Do not put credentials in frontend files.

Alternatively set `OPENAI_API_KEY` on the backend process. `OPENAI_MODEL` overrides the model, and `OPENAI_API_KEY_FILE` overrides the file path. Relative file paths are resolved from the backend process working directory. The `.local/start-backend.ps1` launcher uses the project root. A root `.env` file is not automatically loaded by a manual Java/Maven launch.

References: [Luna](https://developers.openai.com/api/docs/models/gpt-6-luna), [structured outputs](https://developers.openai.com/api/docs/guides/structured-outputs).

Account sign-in: [registration](https://developers.openai.com/siwc/token-sharing-open-source/sign-in), [inference requirements](https://developers.openai.com/siwc/token-sharing-open-source/preview-limitations).

## Behaviour

- Upload a PDF, PowerPoint `.pptx`, or Word `.docx` up to 10 MiB. The content is validated server-side, not just by filename. Legacy `.ppt` and `.doc` files are unsupported. Office archives are limited to 24 MiB expanded and 2,000 entries.
- Select up to 300 source units with at most 600,000 extracted characters: PDF pages, PowerPoint slides, or Word logical text sections. Word sections contain approximately 3,000 characters and are not rendered page numbers. PowerPoint extracts slide text, grouped text and tables. Word extracts main-body paragraphs and tables, including nested tables. Speaker notes, image text, embedded documents, and layout-dependent elements are not included. Password-protected, damaged, image-only and insufficient-text documents receive explicit errors. OCR is not included.
- Generate a target of 5–20 cards in French, English or Arabic at the selected difficulty. The API accepts 3–20. The model can return fewer cards if the source is insufficient.
- Every retained card has a page number and supporting quote checked against extracted text. This verifies quote provenance, not the correctness of every generated statement. Learners should still check the original PDF.
- Reveal answers, mark cards as remembered or to review, edit questions/answers, and request an expanded explanation with an illustrative example and takeaway. Study marks are saved in MySQL; they are not spaced-repetition scheduling.
- Explanations use the stored source page. Editing invalidates the cached explanation. Repeating an unchanged explanation request uses the cache.
- Generated decks, cards, extracted source text, cached explanations and study marks are saved in MySQL. Use **+ Nouveau paquet** to start another document without deleting the selected deck. The upload form also stays available while studying; each generation adds a separate deck. The library displays the 100 most recently updated decks in a wrapping grid; select a deck to reopen its cards, progress and associated video without reconnecting OpenAI. New explanations still need an AI connection unless already cached. Editing a card resets its remembered mark and deletes its cached explanation. Deleting a deck removes only that deck and its cards, explanations, video and extracted source text. The original uploaded file is not retained.
- A private, random, HttpOnly `SMARTLEARN_FLASHCARDS` cookie identifies this browser's library for one year. Only its SHA-256 hash is stored in the database. Owner checks apply to listing, loading, editing, study marks, explanations and deletion. The cookie survives backend restarts and ordinary session expiry. This is a browser library, not a cross-device SmartLearn-account library. Clearing cookies or using another browser loses access to these decks; export JSON for a portable copy. JSON import is not included. OpenAI credentials remain temporary session data, outside MySQL.

## Database tables

Spring/Hibernate creates the following tables using the existing `ddl-auto=update` setting:

| Table | Contents |
|---|---|
| `flashcard_decks` | Owner hash, title, filename, language, difficulty, source label, extracted source JSON and timestamps |
| `flashcards` | Ordered questions/answers, source number and quote, remembered mark, deck foreign key |
| `flashcard_explanations` | One cached explanation, example and takeaway per card, with a card foreign key |

These records survive application restarts. Older decks that existed only in the previous in-memory implementation cannot be reconstructed after that process stops; generate them again to save them in MySQL.
- Selected text is sent to OpenAI with `store: false`; this does not itself promise zero provider-side retention. Original PDFs are not uploaded to the provider.
- A process-local guard permits two simultaneous AI operations, five generation attempts and forty total AI calls per remote address per hour, and one hundred total calls per hour. Cached explanations do not consume an AI call. These limits reset when the process restarts. Put shared rate limiting and a spending limit in place before a multi-instance internet deployment. Forwarded headers are not trusted for this guard.
- For local testing, set `AI_RATE_LIMIT_ENABLED=false` on the backend process. This skips hourly limits but retains the two-operation concurrency limit. The local `.local/start-backend.ps1` launcher currently sets this testing flag. Other launch methods default to enabled.

## API

| Method | URL | Purpose |
|---|---|---|
| GET | `/api/flashcards/status` | Configuration status and model; no credential values |
| POST | `/api/flashcards/openai/connect` | Start personal OAuth; returns an OpenAI authorization URL |
| GET | `/api/flashcards/openai/callback` | Validate the authorization and redirect back to flashcards |
| DELETE | `/api/flashcards/openai` | Disconnect only this visitor's OpenAI session |
| POST | `/api/flashcards` | Multipart `file`, `count`, `language`, `difficulty`, `firstPage`, optional `lastPage` |
| GET | `/api/flashcards` | Most recently updated deck owned by this browser |
| GET | `/api/flashcards/decks` | Saved-deck library for this browser |
| GET | `/api/flashcards/decks/{id}` | Reopen an owned deck |
| DELETE | `/api/flashcards/decks/{id}` | Delete an owned deck and its dependent records |
| PUT | `/api/flashcards/cards/{id}` | Update an owned card and invalidate its explanation |
| PATCH | `/api/flashcards/cards/{id}/review` | Save the remembered mark |
| POST | `/api/flashcards/cards/{id}/explain` | Return or generate and persist an explanation for an owned card |
| DELETE | `/api/flashcards` | Delete this browser's most recently updated deck (legacy endpoint) |

Generation is one bounded request, with an AI timeout of 180 seconds. The interface reports a pending state and does not fabricate incremental progress. Configure reverse-proxy timeouts accordingly when deploying. When the API key, quota or provider is unavailable, the UI displays an actionable error; no placeholder cards are presented as AI output.
# Narrated document videos

Open a saved deck in Flashcards and select **Générer la leçon vidéo**. The existing Luna connection writes a short overview of the same selected document text, in the deck's language and difficulty. Each of 3–6 slides includes concise points, a labelled illustrative example, narration, and a verified supporting quote and page/slide/section number. This is an overview of selected concepts rather than an exhaustive reading of a long document.

The server supplies up to 150 verified excerpts sampled across the selected document, prioritizing existing verified card sources. Luna selects excerpt IDs; the server attaches the original quote and unit number instead of asking the model to copy them exactly. The response schema constrains slide counts and text lengths. Invalid output receives one automatic correction attempt; an unknown source ID is still rejected. This verifies that cited excerpts exist, while factual alignment of the narration remains dependent on the model. Logs contain validation constraint names, never private excerpts or generated scripts.

The default narration engine is **Piper**, a free local neural speech model, with FFmpeg producing a 720p H.264/AAC MP4. French uses **Siwis** (`fr_FR-siwis-medium`), English **Lessac** (`en_US-lessac-high`), and Arabic **Kareem** (`ar_JO-kareem-medium`). Narration runs on the server without an API key, Azure account, usage fee, or external speech request. Luna still prepares the lesson script using the existing OpenAI connection. The audio is synthetic. Existing saved videos retain their original audio until regenerated.

Run `pwsh -File scripts/setup-local-speech.ps1 -Python PATH_TO_PYTHON` to install the pinned Piper runtime and download the models and model cards into ignored `.local/` folders. The default `SPEECH_PROVIDER=local` uses `LOCAL_SPEECH_PYTHON=.local/voice-runtime/Scripts/python.exe` and `LOCAL_SPEECH_MODELS=.local/voices`. These paths can be overridden for other deployment environments. Source and attribution: [Piper engine (GPL-3.0)](https://github.com/OHF-Voice/piper1-gpl), [French Siwis model and dataset attribution](https://huggingface.co/rhasspy/piper-voices/blob/main/fr/fr_FR/siwis/medium/MODEL_CARD), [English Lessac model](https://huggingface.co/rhasspy/piper-voices/blob/main/en/en_US/lessac/high/MODEL_CARD), [Arabic Kareem model](https://huggingface.co/rhasspy/piper-voices/blob/main/ar/ar_JO/kareem/medium/MODEL_CARD). Preserve each downloaded model card and its applicable license/attribution when redistributing the voice models.

Azure is an optional alternative, disabled by default. To select it explicitly, set `SPEECH_PROVIDER=azure` and configure a real Azure Speech resource using `AZURE_SPEECH_KEY` and `AZURE_SPEECH_REGION`, or fill in the ignored project file `.local/azure-speech.json`:

```json
{"key":"YOUR_AZURE_SPEECH_RESOURCE_KEY","region":"YOUR_RESOURCE_REGION"}
```

Use the region code from that resource (for example `westeurope`), not a display name or URL. The key must belong to a Speech resource in that region. These settings are separate from the OpenAI subscription. The file is read dynamically, so filling it in does not require a restart; refresh the video's status in the interface. Never put the key in frontend code, chat, or Git. Voices and pricing depend on the Azure resource and region. The server checks for configuration before spending an OpenAI call, reports authentication/quota/voice errors without exposing Azure response bodies, and does not silently fall back to Windows speech.

Use **Régénérer la vidéo** on a saved video to reuse its stored lesson script without another OpenAI request and replace its narration with the selected engine. Failed narration jobs with a stored script also reuse it when retried. Generated lesson JSON records the chosen narrator. The API keeps owner checks and CSRF protection for regeneration. No Azure account, resource, or billable subscription is needed for local narration.

Install the bundled renderer from the project root with `python -m pip install --target .local/video-tools imageio-ffmpeg==0.6.0`, or configure `VIDEO_FFMPEG` with an absolute FFmpeg executable path. The default bundled executable is discovered relative to the project root. `VIDEO_WORK_DIR` defaults to `.local/video-work`. No installed Windows voice is needed for Azure narration. The rest of Flashcards continues to work without these dependencies.

`flashcard_videos` stores one generated video per deck: job state, lesson JSON, narration transcript, MP4 bytes, WebVTT captions, and duration. MP4s and captions survive backend restarts and follow the same browser ownership rules as the cards. Deleting a deck cascades to its video. Temporary audio, slide images, and working files are removed after rendering. A backend restart marks unfinished jobs as failed so they can be retried. Completed videos play without reconnecting OpenAI.

Generation first writes the script on the requesting visitor's thread, then renders in a bounded local worker. The worker receives the lesson text, never OAuth credentials. Only one video is generated at a time on this local server. Failed generation can be retried, and reopening a deck resumes viewing its job state. Captions are burned into the MP4 and also available as WebVTT; timing is approximate per text group, not word-level alignment. The player supports seeking, download, and a transcript with source quotes.

- `GET /api/flashcards/video/capabilities`: available narration languages.
- `GET /api/flashcards/decks/{id}/video`: private saved status and lesson.
- `POST /api/flashcards/decks/{id}/video`: generate or reuse a completed/in-progress lesson; `?regenerate=true` renders a saved script again using the selected narration engine; requires CSRF.
- `GET /api/flashcards/decks/{id}/video/media`: private MP4 with byte-range seeking; `?download=true` downloads it.
- `GET /api/flashcards/decks/{id}/video/captions`: private WebVTT.

Azure client tests use a local HTTP stub to check authentication headers, the selected French voice, escaped SSML, valid WAV output, dynamic ignored-file configuration, and provider error handling. They make no paid API calls. Backend renderer integration verification is opt-in on Windows: run Maven tests with `-Dsmartlearn.test.video-renderer=true` from the project root after installing bundled FFmpeg. That legacy renderer fixture produces `.local/video-render-preview.mp4` with a local Windows voice; it does not verify live Azure quality. Live Azure audio verification requires a valid Speech resource key and region.

To verify actual free neural narration after installation, run Maven tests with `-Dsmartlearn.test.local-speech=true`. The integration test synthesizes real French, English and Arabic WAV files and renders a French MP4 with captions without any cloud credentials. Samples are saved as `.local/piper-fr-sample.wav`, `.local/piper-en-sample.wav`, `.local/piper-ar-sample.wav` and `.local/piper-video-preview.mp4`.
