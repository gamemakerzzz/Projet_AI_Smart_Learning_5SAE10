# Forum responsiveness fixes

The forum used plain component fields for asynchronous HTTP results without scheduling an Angular view refresh. A completed request could leave an old list, a loading label, or a saving indicator visible until another interaction. Before the fix, the list API returned in 6–9 ms while the open page still showed zero discussions instead of the saved discussion.

All forum HTTP completions now schedule a view refresh. Saving displays the returned discussion immediately, avoids fetching the same detail again through the route subscription, and refreshes the list in the background. A list refresh no longer keeps the save button busy. Regular forum requests time out after 10 seconds; AI requests have a 55-second browser timeout and a 45-second inference timeout on the server.

Forum requests explicitly select `gpt-6-luna` with `reasoning.effort: none`, independently of the default model configured for other features. The existing authenticated transport, structured output schemas, citations, owner checks, CSRF protection and validation remain in place. Flashcards retain their existing request settings. The official [Luna model documentation](https://developers.openai.com/api/docs/models/gpt-6-luna) documents this model and effort setting; provider latency and account access still depend on the connected account.

The forum now shows the active model and AI connection status near the top. If disconnected, use **Connect with ChatGPT**, complete sign-in on Flashcards, then return to Forum and refresh the connection status. A backend restart expires the temporary personal AI connection. A model setting does not create credentials or account access.

## Verification

- Backend: 75 tests, 73 passed, two optional renderer tests skipped. A local HTTP provider fixture verifies the actual forum request payload selects Luna with no reasoning even when another module uses a different model.
- Frontend: 30 tests passed, including rendering a completed response without another click and ending saving without waiting for an unrelated list refresh or fetching the saved detail twice.
- Production frontend build and backend package passed.
- Live browser: a synthetic question immediately displayed the saved discussion; disconnected AI immediately displayed an actionable error. The synthetic question was removed, and the user's existing discussion was preserved.
- Three save/detail/list samples through `http://localhost:4200` measured, in milliseconds: save `230, 16, 16`; detail `15, 12, 11`; list `47, 13, 11`. These are HTTP timings on this computer, not guaranteed provider or end-user timings. Raw measurements: `.local/forum-latency-measurements.json`.
- Real AI generation requires the user's ChatGPT connection and is not established by mocked-provider tests.

Logs are `.local/forum-latency-backend-tests.log`, `.local/forum-latency-frontend-tests.log`, `.local/forum-latency-frontend-build.log`, and `.local/forum-latency-package.log`.
