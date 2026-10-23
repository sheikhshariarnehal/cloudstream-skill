# Autonomous Self-Learning & Skill Evolution Rule

## Core Directive
Every single time you create, fix, audit, optimize, or reverse-engineer a CloudStream provider or extractor, **you MUST advance the skill by 1 step before concluding your turn.**

The skill must train itself dynamically from your conversation context, capture newly solved patterns into reusable libraries, bump its version, log the advancement, sync across all local and global harnesses, and push updates to GitHub.

---

## The 6-Step Self-Training Protocol

```
[TASK COMPLETED]
       │
       ▼
[1. CONTEXT MINING] ────► Identify new extractors, anti-bot bypasses, CMS archetypes, or bug resolutions
       │
       ▼
[2. CODE HARVEST] ──────► Save reusable Kotlin extractors to references/extractors/
                          Save platform patterns to references/archetypes/
       │
       ▼
[3. LOG ADVANCE] ───────► Append Step N entry to LEARNING_LOG.md with technical findings
       │
       ▼
[4. VERSION BUMP] ──────► Increment patch version in package.json and SKILL.md
       │
       ▼
[5. TRIPLE SYNC] ───────► Copy updated skill files to:
                          1. Source: D:\Poject\CloudStream\cloudstream-skill
                          2. Workspace: .agents/skills/cloudstream-provider-dev
                          3. Global: ~/.gemini/config/skills/cloudstream-provider-dev
       │
       ▼
[6. AUTO-PUSH] ─────────► Commit & push to GitHub origin/main (feat(learn): Step N - <summary>)
```

---

## Detailed Step Actions

### 1. Context Mining
Scan what was accomplished in the current turn:
- **Video Hosters**: Did we unpack, decrypt, or scrape a new video server (e.g. StreamWish, VidHide, VolcEngine, Abyss, Upns, GDMirror)?
- **De-Obfuscation**: Did we solve AES-GCM/CBC, Base64, packer `eval(p,a,c,k,e,d)`, or API signature generation?
- **Site Layout / CMS**: Did we interact with WordPress DooPlay, an OTT/Short-drama REST API, an FTP index, or an anime multi-server setup?
- **Bug Fixes**: Did we resolve a Gradle dependency clash, Jackson serialization failure, or coroutine thread lock?

### 2. Code Harvesting & Modular Storage
- **Extractors**: Create or update clean Kotlin classes in `references/extractors/<Name>Extractor.kt`. Must extend `ExtractorApi()`, implement `getUrl()`, and be self-contained so any future provider can directly reuse it.
- **Archetypes**: Create or update reference markdown in `references/archetypes/<Name>.md` outlining endpoints, selectors, payload formats, and pagination.
- **Cookbook & Scraping Guide**: If general wisdom was gained, update `references/provider-cookbook.md` or `references/scraping-guide.md`.

### 3. Record in `LEARNING_LOG.md`
Append an entry using this exact schema:
```markdown
### Step <N> — <YYYY-MM-DD>
- **Target / Provider**: `<ProviderName>` (`<domain>`)
- **Category**: [Extractor | Anti-Bot | CMS Archetype | Bug Resolution | Architecture]
- **Learned Pattern**: <Summary of the technical discovery>
- **Harvested Files**: [`references/extractors/<File>.kt`](references/extractors/<File>.kt)
- **Impact**: Enables all future CloudStream providers to instantly resolve streams from `<domain/host>` without re-investigating.
```

### 4. Version Increment
- Open `package.json` and increment `version` (e.g. `1.1.0` ➔ `1.1.1`).
- Check `SKILL.md` frontmatter/header and keep it aligned.

### 5. Triple-Location Synchronization
Mirror the updated skill directory across all active harnesses:
1. **Source Repo**: `D:\Poject\CloudStream\cloudstream-skill`
2. **Local Workspace**: `D:\Poject\CloudStream\.agents\skills\cloudstream-provider-dev`
3. **Global Config**:
   - Windows: `$env:USERPROFILE\.gemini\config\skills\cloudstream-provider-dev`
   - Linux/Mac: `~/.gemini/config/skills/cloudstream-provider-dev`

### 6. Automatic Commit & Push
Commit the skill repository:
```bash
git add .
git commit -m "feat(learn): step <N> - <short summary of learned feature>"
git push origin main
```
*Note: This operates in conjunction with `rules/auto-push-github.md` to ensure no changes are left unpushed.*
