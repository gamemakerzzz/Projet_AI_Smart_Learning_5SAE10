# Forum AI: summaries, topic tags and learning links

The forum now has four AI workflows: duplicate detection plus the three features described here. All use the existing server-side AI transport and the same session-specific **Continue with ChatGPT** connection or configured API key. There is no second AI account setup, new service or new dependency to install.

This forum module was integrated into mergee with automated tests for its four AI workflows. The procedures below describe manual checks. Live provider verification requires a working AI connection; see ../MERGE-NOTES.md for recorded results.

## Start and connect

Restart the platform after these backend changes:

```powershell
./Stop-SmartLearn.ps1
./Start-SmartLearn.ps1 -Build
```

If you started the backend manually or through your IDE, stop that instance from its original terminal/IDE first. Existing healthy dependencies do not need reinstalling for these features. Use the established dependency-repair options only if your previous installation was incomplete.

Open `http://localhost:4200/#/forum` after SmartLearn sign-in. To use your ChatGPT account, open the Flashcards page, complete **Continue with ChatGPT**, then return to Forum and refresh its connection status. Both modules use the same browser session. The existing callback still returns to Flashcards. Model access, quota and a working provider request are still required.

## 1. Automatic topic tagging

While creating or editing a question, fill the title and details, then click **Auto-tag question**.

- The model suggests specific topic labels, such as `angular`, `authentication` and `route-guards`.
- The tags field is filled automatically. You can edit or remove any suggestion before publishing.
- Publishing or saving the question persists the tags in the active database.
- Tags appear on the question and discussion-list cards. The existing search field also searches tag text.
- The maximum is six tags, each 1–28 characters. Tags are lowercase; spaces become hyphens. Letters, digits, hyphens, dots, plus and # are supported.
- A changing draft invalidates an in-progress suggestion so the old result cannot overwrite the new draft.

Tagging sends the question title and body to the AI provider when you click the button. It does not publish or silently save the question. Existing discussions start with no tags until edited and saved. The `forum_discussions.tags` column stores the labels; the default Hibernate schema-update setting creates this column on backend startup.

### Manual check

1. Ask: `How do I protect an Angular dashboard route?`
2. Add details about redirecting anonymous visitors to login.
3. Click **Auto-tag question** and inspect whether its tags actually describe the question.
4. Edit one tag, publish, reopen and refresh the page. Confirm your edited tags remain.
5. Search for a saved tag. Confirm that discussion is found.
6. Change the draft while tagging is pending. Confirm the previous result does not replace the new input.

## 2. Discussion summarizer

Open a saved discussion, scroll to **Discussion insights**, and click **Summarize discussion**.

The summary displays:

- An overview with references to the source posts.
- Key points from the conversation.
- Proposed solutions, attributed to what participants said.
- Supported unresolved questions, when present.
- A separate accepted-answer summary only when an answer was actually accepted by the author/administrator.

Click **Question**, **Reply #N** or **View accepted reply** to scroll to that source post. The server validates every cited ID against the content sent to the model. This proves a reference points to an included post; it does not prove the AI interpreted the post correctly. Read the cited text when evaluating quality.

The summarizer includes the question and up to 20 recent replies. It preserves the accepted reply in that selection when available, even if that reply is older. Included replies are presented to the model in chronological order. Each reply is limited to 1,500 characters; the question retains its normal 6,000-character limit. The panel reports included/total reply counts and whether content was omitted. It never claims a partial context is the whole thread.

Summary generation does not add a reply, change the original posts, accept an answer or mark the discussion resolved. Results are cached temporarily rather than stored as a new discussion entity.

### Manual check

1. Add at least two replies proposing different approaches to your question.
2. Leave the question unresolved and generate a summary. Confirm no accepted answer is invented.
3. Click its source references and compare every claim with the original text.
4. Accept one reply, then summarize again. Confirm only the actual accepted reply receives the accepted-answer label.
5. Edit or add a reply. Local mutations clear old insights; generate again and confirm the summary reflects the changed thread.
6. Repeat an unchanged request to check the cached-result label.
7. On a thread with no replies, confirm the model summarizes the question and does not invent a proposed solution.

If another user changes the thread during analysis, the backend rejects the result as stale. If the browser's loaded version is older than the analyzed version, the UI asks you to refresh the discussion. Switching to another discussion also discards late results.

## 3. Course and resource suggestions with links

On a saved discussion, click **Suggest courses & resources** in the **Keep learning** panel.

The model chooses up to four courses and four external resources and explains why each may help with the discussion. This is based on the question and included replies; it does not use the learner's private profile or replace the separate profile-based TF-IDF recommendation module.

### Platform courses

Candidates are up to 30 most recently added courses, with the discussion's linked course included when available. The AI receives bounded title, description, level and keyword metadata. It selects supplied course IDs; the server builds actual links to `/#/formations/{id}` and rejects invented/duplicate IDs. The current course catalogue is checked before returning links. Older courses outside this shortlist may be missed.

Only relevant matches should appear. If no checked course fits, the UI says so rather than filling the panel with unrelated items. Course metadata is not proof that the course solves the exact problem.

### External resources

External links come from `backend/src/main/resources/forum-resources.json`. The initial catalogue contains ten official documentation pages, checked during implementation on October 18, 2026:

| ID | Resource |
|---|---|
| angular-guards | [Angular route guards](https://angular.dev/guide/routing/route-guards) |
| angular-forms | [Angular reactive forms](https://angular.dev/guide/forms/reactive-forms) |
| spring-authorization | [Spring Security HTTP authorization](https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html) |
| java-collections | [Java 17 collections](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Collection.html) |
| java-null | [Java 17 NullPointerException](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/NullPointerException.html) |
| mysql-transactions | [MySQL transactions, COMMIT and ROLLBACK](https://dev.mysql.com/doc/refman/8.4/en/commit.html) |
| css-layout | [MDN CSS layout](https://developer.mozilla.org/en-US/docs/Learn_web_development/Core/CSS_layout) |
| python-tutorial | [Python tutorial](https://docs.python.org/3/tutorial/) |
| javascript-guide | [MDN JavaScript guide](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide) |
| typescript-types | [TypeScript everyday types](https://www.typescriptlang.org/docs/handbook/2/everyday-types.html) |

The AI receives resource IDs, titles and descriptions, not permission to invent URLs. It returns selected IDs; the server supplies the stored HTTPS links. These are catalogue suggestions, not live web-search results. The application does not fetch arbitrary posted URLs, scrape websites or check availability on each request. External pages and versions may change. The initial catalogue mainly covers programming/web topics; unrelated subjects may correctly produce no external match.

To expand coverage, review a real official resource, add its unique ID/title/description/HTTPS URL to the JSON catalogue and restart the backend. No new service is needed.

### Manual check

1. Use a discussion about Angular route guards.
2. Ensure the platform has a course whose saved title/description actually covers Angular or authentication; create one as an administrator if needed.
3. Click **Suggest courses & resources**.
4. Open each suggested platform course and confirm its title/ID correspond to a real saved course.
5. Open external links and check their actual relevance. An Angular guard discussion should plausibly select the Angular guard resource; this is an expectation to evaluate, not a guaranteed model output.
6. Try an unrelated question. Confirm weak matches are not forced into the result.
7. Change the discussion or course metadata, refresh and regenerate rather than treating an old suggestion as current.

## APIs

All routes require the existing authenticated SmartLearn session. POST requests retain CSRF protection.

| Method / endpoint | Input / behavior |
|---|---|
| POST /api/forum/ai/tags | Valid question draft; returns editable tags and an explanation |
| POST /api/forum/discussions/{id}/ai/summary | Uses saved question/replies; returns cited summary sections |
| POST /api/forum/discussions/{id}/ai/learning | Uses saved discussion and catalogue candidates; returns trusted links |
| POST /api/forum/discussions | Draft accepts an optional `tags` array |
| PUT /api/forum/discussions/{id} | Saves edited question/tags under existing author/admin rules |
| GET /api/forum/ai/status | Includes the available feature names and insights prompt version |

Each new AI result is wrapped as `{ result, meta }`. Metadata contains actual request duration, whether a cached result was used, model and `forum-insights-v1` prompt version. The earlier duplicate detector continues using `forum-v2`.

## Reliability, cost controls and privacy

- The new features share a per-user in-flight guard, a two-second uncached-request cooldown and a maximum of four simultaneous insight requests per backend instance. Existing duplicate-detection controls remain separate.
- Results are cached for five minutes, with at most 60 entries, partitioned by user and keyed by operation, content, model and prompt version. Changes to included discussion content, resolution state or supplied course metadata produce another key.
- Failed/invalid provider output is not cached as a successful result. A stale context is not displayed as current.
- Schema-constrained output and server checks reject unknown source/course/resource IDs, duplicate selections, invalid labels, excessive sections and malformed tags. The model cannot change posts or supply a clickable destination.
- Names, emails and profile data are not sent as dedicated fields. Post bodies may themselves contain personal information; choose non-sensitive examples for evaluation. The shared transport uses `store: false`, which is not a promise about all provider-side retention.
- Prompt instructions treat posts and catalogue metadata as untrusted content. These instructions reduce a failure mode but do not establish perfect prompt-injection resistance.
- Suggestions and summaries can still be inaccurate, incomplete or irrelevant. Limits and context coverage are shown so they can be assessed.

Keep actual screenshots, inputs, source-reference checks, incorrect suggestions and timings for the report. The existing duplicate-classifier evaluation script does not evaluate these new workflows; assess them using the manual scenarios above. No new live-test result or quality score has been invented.

