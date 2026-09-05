# CloudStream Provider Development Skill

[![Antigravity Skill](https://img.shields.io/badge/Antigravity-Skill-blueviolet.svg)](https://github.com/recloudstream/cloudstream)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-blue.svg)](https://kotlinlang.org/)
[![CloudStream](https://img.shields.io/badge/CloudStream-3.0-orange.svg)](https://github.com/recloudstream)

An expert pair-programming skill for [Google Antigravity](https://antigravity.google) tailored for building, reverse engineering, debugging, and maintaining **CloudStream** (`recloudstream`) plugins and providers in Kotlin.

---

## 🎯 What This Skill Does

When activated, your Antigravity agent is equipped with deep domain knowledge of the CloudStream plugin architecture, including:
- Implementing the 4 core provider methods (`search`, `getMainPage`, `load`, `loadLinks`).
- Writing custom `ExtractorApi` implementations for complex or encrypted video hosts.
- Inspecting network traffic, sniffing obfuscated `.m3u8`/`.mp4` stream links, and bypassing common devtools/crawler guards.
- Correct Gradle configuration (`TestPlugins` template, JitPack stubs, metadata blocks, Jackson version pinning).
- Automated CI/CD workflow troubleshooting (`builds` branch setup, `repo.json`, and `plugins.json` manifest generation).

---

## 📂 Repository Structure

```
├── SKILL.md                          # Main skill definition, skeleton provider, and core rules
├── README.md                         # Project documentation and installation guide
├── LICENSE                           # MIT License
├── .gitattributes                    # Line ending normalization
├── workflows/
│   └── add-provider.md               # Step-by-step workflow triggered by /provider <url>
└── references/
    ├── project-setup.md              # Template fork, Gradle files, repo.json/plugins.json & CI
    ├── provider-cookbook.md          # Real-world implementations of all 4 provider methods
    └── scraping-guide.md             # Reverse-engineering video hosts, selectors, regex & network sniffing
```

### Reference Documentation & Workflows

| Document | Description |
|---|---|
| [`SKILL.md`](SKILL.md) | The core instructions and progressive disclosure entry point loaded by Antigravity. |
| [`workflows/add-provider.md`](workflows/add-provider.md) | **Automated workflow for `/provider <url>`**: executes site reconnaissance, CloudStream architecture mapping, provider scaffolding, and compilation checks. |
| [`references/project-setup.md`](references/project-setup.md) | Complete guide to configuring `TestPlugins`, root/plugin `build.gradle.kts`, distribution manifests (`repo.json` / `plugins.json`), and CI gotchas. |
| [`references/provider-cookbook.md`](references/provider-cookbook.md) | Step-by-step cookbook patterns for Movies, TV Series, pagination, JSON payload caching, and custom extractors. |
| [`references/scraping-guide.md`](references/scraping-guide.md) | Practical guide to extracting playable streams: hunting iframes, reversing obfuscation (Base64/AES), and handling headers. |

---

## 🚀 Installation & Usage

### Option 1: Workspace Skill (Project-Level)
Clone or copy this repository into your workspace's `.agents/skills/` directory:

```bash
mkdir -p .agents/skills/cloudstream-provider-dev
# Copy the files into .agents/skills/cloudstream-provider-dev/
```

### Option 2: Global Skill (User-Level)
Make the skill available across all your Antigravity projects:

**Windows (PowerShell):**
```powershell
$target = "$env:USERPROFILE\.gemini\config\skills\cloudstream-provider-dev"
git clone https://github.com/sheikhshariarnehal/cloudstream-skill.git $target
```

**Linux / macOS:**
```bash
git clone https://github.com/sheikhshariarnehal/cloudstream-skill.git ~/.gemini/config/skills/cloudstream-provider-dev
```

---

## 💡 Triggering the Skill

### ⚡ Quick Slash Command: `/provider <url>`
Simply provide a streaming website URL and trigger `/provider`:
```
/provider https://example-movies.to
```
or
```
Here is the site https://example-movies.to /provider add this new provider
```
**What Antigravity automatically does:**
1. **Understands the Site**: Scrapes HTML, tests search query endpoints, identifies catalog categories, checks detail metadata, and inspects video players/iframes for playable `.m3u8`/`.mp4` stream links.
2. **Maps CloudStream Architecture**: Determines provider name, package structure, supported `TvType`s, and built-in or custom extractor requirements.
3. **Implements the Provider**: Scaffolds `<SiteName>Provider/`, `build.gradle.kts`, `Provider.kt`, and `Plugin.kt`.
4. **Validates & Builds**: Checks compilation and guides local deployment.

### Other Triggers
Antigravity also automatically invokes this skill whenever you ask questions or issue instructions related to:
- Creating or fixing a CloudStream provider or plugin.
- Scraping a streaming website for movie/TV links in Kotlin.
- Writing a `MainAPI` or `ExtractorApi` class.
- Configuring `build.gradle.kts` for a CloudStream `.cs3` plugin.
- Setting up or troubleshooting `repo.json` and `plugins.json`.

---

## 📄 License

This skill is distributed under the [MIT License](LICENSE).
