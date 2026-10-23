# CloudStream Provider Development & Continuous Self-Learning Skill

[![Antigravity Skill](https://img.shields.io/badge/Antigravity-Skill-blueviolet.svg)](https://github.com/recloudstream/cloudstream)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-blue.svg)](https://kotlinlang.org/)
[![CloudStream](https://img.shields.io/badge/CloudStream-3.0-orange.svg)](https://github.com/recloudstream)
[![Self-Learning](https://img.shields.io/badge/Engine-Self--Learning-brightgreen.svg)](LEARNING_LOG.md)

An expert pair-programming skill for [Google Antigravity](https://antigravity.google), Claude Code, Cursor, Codex, and Grok. Tailored for building, reverse engineering, debugging, and maintaining **CloudStream** (`recloudstream`) plugins and providers in Kotlin.

Equipped with an **Autonomous Self-Learning Engine** that mines technical discoveries from active development sessions, harvests reusable Kotlin extractors, maintains a step-by-step evolution ledger, and auto-syncs across environments.

---

## 🎯 What This Skill Does

- **Autonomous Self-Training**: Trains itself 1 step further every session, capturing new video hosters, decryption routines, and CMS archetypes into permanent libraries.
- **Provider Healing (`/heal`)**: Diagnoses broken providers, updates expired selectors, injects modular extractors, and auto-increments version in `build.gradle.kts` for CloudStream in-app updates.
- **Two-Tier Verification Gate**: Verifies live stream URLs return HTTP 200 before writing code, and tests `./gradlew :make` to guarantee valid `.cs3` compilation.
- **Core Provider Architecture**: Deep scaffolding for `search`, `getMainPage`, `load`, and `loadLinks`.
- **Custom Extractor Library**: Pre-built, drop-in Kotlin `ExtractorApi` implementations for ByteDance VolcEngine, AWSStream, Upns, GDMirror, and more.
- **Automated CI/CD & GitHub Push**: Enforces semantic git commits and automated pushes to remote tracking branches.

---

## 📂 Repository Structure

```
├── SKILL.md                          # Main skill definition, slash commands, and core rules
├── README.md                         # Project documentation and installation guide
├── LEARNING_LOG.md                   # Step-by-step master ledger of learned capabilities
├── LICENSE                           # MIT License
├── package.json                      # Version tracking and CLI manifest (v1.2.0)
├── workflows/
│   ├── add-provider.md               # Step-by-step workflow triggered by /provider <url>
│   ├── heal-provider.md              # Extension repair & version bump triggered by /heal
│   ├── optimize-provider.md          # 6-pillar repository audit triggered by /optimize
│   └── learn-step.md                 # Autonomous context-mining triggered by /learn
├── rules/
│   ├── self-learning-loop.md         # Mandatory rule to advance skill 1 step after every task
│   ├── provider-version-bump.md      # Auto-version bumping & changelog maintenance rule
│   └── auto-push-github.md           # Mandatory rule to always commit & push after finishing work
└── references/
    ├── extractors/                   # Standalone, reusable Kotlin ExtractorApi library
    │   ├── VolcEngineExtractor.kt    # ByteDance / VolcEngine short-drama VOD extractor
    │   ├── AWSStreamExtractor.kt     # AWSStream / Zephyrflick / Ravok multi-host extractor
    │   ├── UpnsPlayerExtractor.kt    # Upns / Packed JS M3U8 stream extractor
    │   ├── GDMirrorExtractor.kt      # Google Drive & fast CDN direct link extractor
    │   └── HubCloudExtractor.kt      # HubCloud / BuzzServer / PixelDrain / S3 resolver
    ├── archetypes/                   # Architectural blueprints for common streaming platforms
    │   ├── short-drama-api.md        # Vertical mobile OTT short-drama REST API pattern
    │   ├── anime-multiserver.md      # Multi-server, Dub/Sub anime aggregator pattern
    │   ├── bdix-ftp-portal.md        # High-speed local ISP/FTP direct stream pattern
    │   ├── iptv-m3u-player.md        # IPTV playlist & ClearKey DRM DASH streaming pattern
    │   └── live-sports-events.md     # Live sports matches, countdowns & AES-CBC decryptor
    ├── project-setup.md              # Template fork, Gradle files, repo.json/plugins.json & CI
    ├── provider-cookbook.md          # Real-world implementations of all provider methods
    └── scraping-guide.md             # Reverse-engineering video hosts, selectors, regex & sniffing
```

---

## ⚡ Slash Commands & Triggers

| Command | Action | Workflow |
|---|---|---|
| `/provider <url>` | Create a new provider from target website | [`workflows/add-provider.md`](workflows/add-provider.md) |
| `/heal <ProviderName>` | Diagnose and repair a broken provider, bumping version | [`workflows/heal-provider.md`](workflows/heal-provider.md) |
| `/optimize` | Run 6-pillar deep audit across provider repository | [`workflows/optimize-provider.md`](workflows/optimize-provider.md) |
| `/learn` | Manually trigger context mining & advance skill step | [`workflows/learn-step.md`](workflows/learn-step.md) |

---

## 🚀 Installation & Usage

### ⚡ Recommended: 1-Line NPX Installer
Install or update directly from GitHub with zero git path errors. Automatically detects Antigravity IDE, Claude Code, Cursor, Codex, and Grok:

```bash
npx github:sheikhshariarnehal/cloudstream-skill install
```

To install directly into global settings without prompts:
```bash
npx github:sheikhshariarnehal/cloudstream-skill install --scope=global
```

To update your installed skill anytime:
```bash
npx github:sheikhshariarnehal/cloudstream-skill update
```

---

### Alternative: Git Clone

#### Workspace Scope (Current Project):
```bash
git clone https://github.com/sheikhshariarnehal/cloudstream-skill.git .agents/skills/cloudstream-provider-dev
```

#### Global Scope (User-Level):
```bash
git clone https://github.com/sheikhshariarnehal/cloudstream-skill.git "$env:USERPROFILE\.gemini\config\skills\cloudstream-provider-dev"
```

---

## 🧠 Continuous Learning Protocol

Whenever a task completes:
1. **Mine Context**: Extract new video hosts, regex patterns, anti-bot headers, or Gradle solutions.
2. **Harvest Reusable Code**: Save Kotlin classes into `references/extractors/` and blueprints into `references/archetypes/`.
3. **Log Advance**: Increment Step N in `LEARNING_LOG.md`.
4. **Bump Version**: Update patch version in `package.json` and `SKILL.md`.
5. **Triple Sync**: Synchronize source repo, workspace `.agents`, and global user config.
6. **Push to GitHub**: Commit with conventional semantic message and push to remote.
