# Automatic GitHub Commit & Push Rule

## Core Directive
Whenever any files are created, modified, refactored, optimized, deleted, or updated during any task or user request, **you MUST automatically commit and push all changes to GitHub before concluding your turn.**

Never leave pending, uncommitted, or unpushed modifications in the repository when you finish your work.

---

## Trigger Conditions
Execute this workflow whenever:
1. A new CloudStream provider or plugin module is scaffolded or written (e.g. via `/provider`).
2. An existing provider or repository is audited, fixed, or optimized (e.g. via `/optimize`).
3. Any bug fix, feature addition, refactoring, or documentation update is completed.
4. Changes are made to this skill repository, its workflows, references, or rules.

---

## Step-by-Step Execution Workflow

### 1. Status & Secret Safety Scan
- Run `git status` in the root of the active repository to inspect all modified and untracked files.
- Verify that no sensitive credentials, `.env` secrets, private API keys, or temporary files are accidentally included.
- Ensure `.gitignore` properly ignores build artifacts (e.g. `build/`, `.gradle/`, `.idea/`, `*.iml`, local caches).

### 2. Stage Changes
```bash
git add .
```
*(Or specify exact modified directories/files if working in a monorepo).*

### 3. Generate Semantic Conventional Commit
Craft a descriptive, conventional commit message:
- `feat:` for new providers, extractors, slash commands, or features (e.g. `feat: add NetMirror and CastleTv providers`)
- `fix:` for bug fixes, crash resolutions, or parser patches (e.g. `fix: resolve NPE in episode list parsing`)
- `perf:` for performance optimizations and thread-safety updates (e.g. `perf: migrate apmap to non-blocking amap`)
- `docs:` for documentation, guide, or cheat sheet updates (e.g. `docs: update scraping guide for iframe extraction`)
- `refactor:` for code restructuring and cleanups

```bash
git commit -m "<type>(<scope>): <clear description of changes>"
```

### 4. Push to Remote Repository
- Push the commit to the active tracking branch on GitHub:
```bash
git push origin <current-branch>
```
- If working in an environment with GitHub MCP tools, use the corresponding MCP push tool when local git commands are restricted.

### 5. Multi-Installation Sync (Skill Updates)
If modifications were made to `cloudstream-skill`, synchronize the updated files across all active installations:
1. Source Repository: `d:\Poject\CloudStream\cloudstream-skill`
2. Local Workspace Copy: `d:\Poject\CloudStream\.agents\skills\cloudstream-provider-dev`
3. Global User Copy: `~/.gemini/config/skills/cloudstream-provider-dev` (and `~/.gemini/config/rules/`)

### 6. User Confirmation
In your final response to the user, include:
- The commit message.
- Target branch and repository.
- Push confirmation status.
