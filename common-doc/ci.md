# Continuous integration

The frontend and backend test workflows run for open, non-draft pull requests targeting `master`. They run when a PR is opened, updated with new commits, reopened, marked ready for review, or edited (including retargeting to `master`). Pushes to other branches and manual dispatches do not trigger CI. Both suites cover every change, including shared code and build configuration.

Pushes to `master` first check whether the pushed tip is a merged PR's merge commit. Each suite skips its tests only when its workflow has a successful PR run for that PR's final head revision, associated with the PR and the `master` base, with a successful `test` job. Skipped draft jobs do not count. This supports merge, squash, and rebase merges when GitHub supplies the matching merge commit and run association.

Direct commits, untested merges, failed or pending PR tests, and missing verification data run the tests on `master`. API failures also fall back to testing. The small verification job still runs after a tested PR is merged, but the expensive test suite is skipped. No additional PR workflow is triggered by closing or merging the PR.

New PR runs cancel older runs for that PR. Master runs are not cancelled by newer commits. AI review and triage workflows have been removed.

The shared decision helper is `.github/scripts/ci-should-run.cjs`. Verify its behavior with `node --test .github/scripts/ci-should-run.test.cjs` and validate the workflows with `actionlint`.

Backend tests override the database configuration with `DB_URL`, `DB_USER`, and `DB_PASSWORD`. Hoplite 3 uses a single underscore for nested property paths (for example, `DB_URL` binds to `db.url`); the old double-underscore names no longer override the YAML defaults. Use these names for runtime database overrides as well.
