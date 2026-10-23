# Self-Learning Workflow (`/learn` or Step N Hook)

This workflow runs autonomously at the end of any provider task, or explicitly when the user triggers `/learn`. It transforms real debugging, scraping, and reversing sessions into permanent skill capabilities.

---

## Workflow Pipeline

```
[START LEARN] ──> [MINING] ──> [HARVEST CODE] ──> [UPDATE LOG] ──> [BUMP VERSION] ──> [SYNC & PUSH]
```

---

## Execution Protocol

### Step 1: Scan Session Context
Inspect the active conversation trajectory:
1. What target site or provider was analyzed, scaffolded, or repaired?
2. Were new video hosts encountered? (Check network logs, iframe URLs, `.m3u8` origins).
3. Did we write any custom extractor logic or crypto routines (e.g. AES, XOR, Base64, token signing)?
4. Were any CloudStream runtime or compile issues resolved (e.g. coroutine thread locks, Jackson models, Gradle JVM targets)?

### Step 2: Extract & Modularize
- For any new or improved extractor:
  - Extract the Kotlin `ExtractorApi` class.
  - Save it into `references/extractors/<HostName>Extractor.kt`.
  - Ensure it includes proper imports, `@JsonProperty` data models, and safe fallback logic.
- For site frameworks or CMS:
  - Document the structure in `references/archetypes/<Archetype>.md` (e.g. DooPlay, BDIX, API).

### Step 3: Update `LEARNING_LOG.md`
- Read current `LEARNING_LOG.md` to determine the latest Step number `N`.
- Append a new section for `### Step N+1`.
- Include the provider, date, technical challenge solved, and harvested file links.

### Step 4: Bump Version
- Read `package.json`.
- Increment the patch version (e.g. `1.1.0` -> `1.1.1`).
- Sync version string in `SKILL.md`.

### Step 5: Triple-Location Sync
- Copy the updated directories (`references/`, `workflows/`, `rules/`, `SKILL.md`, `LEARNING_LOG.md`, `package.json`, `README.md`) from `D:\Poject\CloudStream\cloudstream-skill` to:
  1. `D:\Poject\CloudStream\.agents\skills\cloudstream-provider-dev`
  2. `$env:USERPROFILE\.gemini\config\skills\cloudstream-provider-dev`

### Step 6: Git Commit & Remote Push
- Run `git status` in `D:\Poject\CloudStream\cloudstream-skill`.
- Stage all files: `git add .`
- Commit: `git commit -m "feat(learn): step <N+1> - <feature summary>"`
- Push: `git push origin main`
- Report the newly learned step and updated version to the user.
