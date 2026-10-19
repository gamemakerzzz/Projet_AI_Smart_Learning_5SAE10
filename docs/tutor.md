# AI Tutor and Mind Map

This module was selectively imported from GitHub `main` commit `0c42021`. It is available at `http://localhost:4200/#/tutor` and in the authenticated sidebar. The existing forum, flashcards/video, planning, exams and other modules are retained.

## Enable Groq generation

The default provider is Groq's OpenAI-compatible Chat Completions endpoint, using the branch's `qwen/qwen3.8-27b` model. This is independent of the ChatGPT sign-in used by Forum and Flashcards. Create a key in [Groq Console](https://console.groq.com/keys) and save only the key in the ignored file:

```text
C:/Users/azizb/Desktop/software/mergee/.local/groq-api-key.txt
```

Then click **Actualiser le statut IA** in the tutor. The key file is read dynamically; no restart is required. Never put the key in frontend files or commit it. No Groq key was available during this integration, so real Groq generation and account/model access have not been verified.

Alternatively set `GROQ_API_KEY` or `TUTOR_AI_KEY` in the backend's environment before starting it. The tutor uses dedicated credentials and never sends the forum's OpenAI key to Groq. Manual launch does not automatically load `.env`.

Groq documents a [Free tier and paid upgrades](https://console.groq.com/docs/billing-faqs). Free-tier limits, model availability and eligibility depend on the account; the integration does not create an account, upgrade a plan, or configure billing. The selected Qwen model is a [preview model](https://console.groq.com/docs/model/qwen/qwen3.8-27b), so availability may change. `TUTOR_AI_MODEL` can select another compatible model available to the account.

## Four tools

- **Chat:** optionally choose a course. The tutor receives up to 8,000 characters from its stored chapter texts and the latest ten conversation messages. This is bounded course-context retrieval, not a separate embeddings/search service. Answers are model-generated; requested chapter citations are not independently verified for factual correctness.
- **Expliquer:** explanation, example, analogy and takeaway for a concept and optional source context.
- **Résumer:** summary, key points and estimated original reading time, in French, English or Arabic.
- **Mind Map:** a displayed tree with 3–6 branches and 2–4 children per branch, descriptions and an option to continue the topic in chat. It is a tree visualization, not a drag-and-drop diagram editor.

Invalid or truncated provider output is reported as an error. Results are not replaced with fabricated content. Two provider requests can run concurrently; the inference timeout is 60 seconds. Normal history requests use a 10-second browser timeout and AI requests 70 seconds. Late responses cannot overwrite a different conversation.

## Saved conversations and database

Successful chats save the user/assistant pair atomically in `tutor_conversations` and `tutor_messages`. Failed new chats do not leave empty history. History is limited to the authenticated account; another learner or an administrator cannot read someone else's tutor conversations through these endpoints. Deleting a conversation removes its messages.

The running merged copy continues using its persistent H2 database at `.local/data/smartlearn`, shared with the other modules. Immediate committed writes are enabled so recent messages survive an abrupt stop. Existing user data is retained. MySQL mappings and the existing MySQL datasource configuration are supported by the module; a real MySQL server was not exercised in this environment. No source-branch database was copied.

To use MySQL, stop the local backend and launch the packaged JAR without the `local` profile, configuring `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD` for a dedicated MySQL database. Hibernate's existing `ddl-auto=update` creates the tutor tables. The local launcher intentionally selects H2, so changing only the URL while retaining its H2 driver is insufficient.

## OpenAI-compatible configuration

The transport follows [Groq's Chat Completions compatibility](https://console.groq.com/docs/openai). For another provider, set its server-side key with `TUTOR_AI_KEY`, its full Chat Completions endpoint with `TUTOR_AI_URL`, and a supported model with `TUTOR_AI_MODEL`, then restart the backend. For OpenAI the endpoint is `https://api.openai.com/v1/chat/completions`; this configuration uses an API key, not the existing browser OAuth session. Never use a Groq key against an OpenAI endpoint or vice versa.

## Verification

- Backend suite: 85 tests reported, 83 passed, two optional renderer tests skipped. Tutor tests cover authentication, CSRF, ownership, course context, multi-turn history, saving/deletion, provider failures and validated outputs for all four tools. Provider HTTP fixtures verify request headers, history, JSON mode, dynamic key loading and handling of incomplete output.
- Frontend: 35 tests passed, including all four tabs, rendered mind-map branches, displayed messages, failed-request recovery and late-response handling. Production build passed. The component stylesheet budget was raised from 8 to 10 kB for the imported four-tool module; size warnings remain.
- `scripts/verify-tutor.py` starts an isolated test backend with a local synthetic provider and file database. Real HTTP checks cover all four tools, RAG, multi-turn messages, history surviving an abrupt restart, ownership and deletion. It makes no real AI request and does not modify the running application's database. Result: `.local/tutor-http-verification.json`.
- Tests with synthetic providers establish integration behavior, not real model quality or account access. Live Groq verification still needs the user's key.

Run `./Start-SmartLearn.ps1` normally. After source edits, stop with `./Stop-SmartLearn.ps1`, then use `./Start-SmartLearn.ps1 -Build`. Backend logs and verification output are in `.local`.
