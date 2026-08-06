# brain

Knowledge corpus for the JU Housing (제이유 하우징) HQ multi-agent "brain". All content lives as Markdown under `knowledge/` and is consumed by an external local AI system ("Control Tower"), which is not part of this repository.

## Cursor Cloud specific instructions

This repository is **content-only**: 21 Markdown files under `knowledge/` (`core/`, `estimate/`, `field/`, `marketing/`, `shorts/`, `shared/`). There is no application code, package manifest, build step, test suite, or runnable service in the repo. Keep that in mind before looking for a server to start — there isn't one.

- Lint: `markdownlint-cli2 "knowledge/**/*.md"`. The linter is installed globally via the update script (user-local npm prefix `~/.npm-global`, already on `PATH` via `~/.bashrc`). The existing content currently reports style findings (e.g. `MD022`/`MD032` blank-line rules, `MD060` table spacing); do not mass-reformat the corpus unless a task explicitly asks for it.
- Tests / build: none exist. Do not invent a test or build command.
- Preview the corpus: render the Markdown to a browsable HTML site and serve it. Any Markdown renderer works; a quick throwaway approach is a Node script using `marked` plus `python3 -m http.server`. This is a preview only — the real "runtime" (the Control Tower agent stack) lives outside this repo.
- The content is primarily Korean. Preserve the domain vocabulary and the explicit "금지 표현 / 금지 건축 스타일" (banned-terms) rules in `knowledge/shared/` when editing.
