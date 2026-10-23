# Provider Healing & Maintenance Workflow (`/heal` or `/update-provider`)

This workflow executes automatically when the user types `/heal <ProviderName>`, `/update-provider <ProviderName>`, asks to "fix a broken provider", or reports that an existing CloudStream extension stopped working.

---

## State Machine

```
[TARGET IDENTIFIED] ──> [PHASE 1: DIAGNOSE] ──> [PHASE 2: REPAIR] ──> [PHASE 3: VERSION BUMP] ──> [PHASE 4: COMPILE] ──> [PHASE 5: PUSH & LEARN]
```

---

## Step-by-Step Execution Protocol

### Phase 1 — Diagnostic Reconnaissance
1. **Locate Provider Module**: Find the provider directory in the active repository (e.g. `<Repo>/<ProviderName>/` or `nehal-CloudStream/<ProviderName>/`).
2. **Inspect Existing Code**:
   - Check `mainUrl` in `<ProviderName>Provider.kt`.
   - Identify stream extractors used in `loadLinks()`.
   - Review selectors in `search()`, `getMainPage()`, and `load()`.
3. **Probe Target Site Live**:
   - Fetch the current site URL using `read_url_content` or `chrome-devtools-mcp`.
   - Verify if domain was redirected (e.g. `.to` ➔ `.tv` ➔ `.lat`).
   - Check if HTML selectors for movie cards or episode lists changed.
   - Inspect the player embed iframe to see what video hoster is now serving streams.
4. **Initial Compile Check**:
   - Run `./gradlew <ProviderName>:make` to check for Kotlin deprecation or dependency errors.

---

### Phase 2 — Targeted Repair & Modernization
1. **Update Domain & Selectors**:
   - Update `mainUrl` if the site migrated to a new domain.
   - Refresh CSS selectors for titles, poster images, descriptions, and episode links.
2. **Upgrade Stream Extractors**:
   - Check `references/extractors/` in `cloudstream-skill` for matching hoster extractors (e.g. StreamWish, VidHide, VolcEngine, Abyss, Upns, GDMirror).
   - If the site uses a new hoster, reverse-engineer it using `references/scraping-guide.md` and implement the clean `ExtractorApi`.
   - Ensure multi-quality playback and valid headers (`getVideoInterceptor`).
3. **Apply 6-Pillar Optimizations**:
   - Eliminate blocking `apmap` in favor of `amap`.
   - Wrap fragile JSON in `parsedSafe<T>()` and integers in `toIntOrNull()`.
   - Add `cacheTime = 60` and `timeout = 30`.

---

### Phase 3 — Mandatory Version Increment
Every fix or update to a provider **must** trigger an update for end users inside the CloudStream Android app:
1. Open `<ProviderName>/build.gradle.kts`.
2. Locate `version = <number>`.
3. Increment the integer by 1 (e.g., `version = 2` ➔ `version = 3`).
4. Update `changelog` or commit message to reflect the fix.

---

### Phase 4 — Two-Tier Verification
1. **Stream Verification**: Verify that the newly resolved stream URL (`.m3u8` or `.mp4`) returns HTTP 200 with media content.
2. **Gradle Compilation**:
   ```bash
   ./gradlew <ProviderName>:make
   ```
   Confirm that `<ProviderName>.cs3` is generated successfully in the provider's `build/` directory without warnings treated as errors.

---

### Phase 5 — Auto-Push & Autonomous Skill Learning
1. **Commit & Push Extension**:
   - Commit the fixed provider with semantic conventional commit:
     ```bash
     git add <ProviderName>/
     git commit -m "fix(<ProviderName>): resolve stream extraction & update selectors (v<NewVersion>)"
     git push origin <branch>
     ```
2. **Trigger Autonomous Self-Learning Protocol**:
   - If a new extractor, decryptor, or bypass pattern was developed during the fix, immediately execute `rules/self-learning-loop.md` to advance the skill by 1 step.
   - Update `LEARNING_LOG.md` in `cloudstream-skill`.
   - Synchronize across harnesses and push to `origin/main`.
