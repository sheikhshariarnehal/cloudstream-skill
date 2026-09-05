# Project Setup, Build Files, and Distribution

Table of contents:
1. Forking and configuring the template repo
2. Root `build.gradle.kts`
3. Per-plugin `build.gradle.kts` and the `cloudstream { }` block
4. The plugin entry point
5. Distribution: `repo.json` and `plugins.json`
6. Local testing
7. Gotchas checklist

---

## 1. Forking and configuring the template repo

Start from https://github.com/recloudstream/TestPlugins rather than from scratch:

1. Fork it.
2. `Settings → Actions → General → Allow all actions and reusable workflows`.
3. Same page → `Read and write permissions` for workflows (CI needs to push built `.cs3` files).
4. **Manually create a `builds` branch from `master`** before the first push — the CI workflow
   checks this branch out on every run, so it must already exist.
5. On the very first run, the workflow's cleanup step (`rm $GITHUB_WORKSPACE/builds/*.cs3`) fails
   because no `.cs3` exists yet. Edit `.github/workflows/build.yml` and append `|| true` to that
   line until after the first successful build.

## 2. Root `build.gradle.kts`

This file (at the repo root, shared by all plugin modules via `subprojects { }`) wires up the
Android/Kotlin/CloudStream gradle plugins and the shared dependency versions. The two version
pins most likely to need bumping to whatever's currently resolvable are the CloudStream gradle
plugin itself and the Kotlin gradle plugin — the template sometimes ships a `-SNAPSHOT` plugin
coordinate that no longer resolves on JitPack, and a Kotlin version older than what current
CloudStream stubs expect:

```kotlin
buildscript {
    repositories {
        google(); mavenCentral()
        maven("https://jitpack.io")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.7.3")
        // Pin to a resolvable commit rather than "-SNAPSHOT" if the default fails to resolve:
        classpath("com.github.recloudstream:gradle:-SNAPSHOT")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.0")
    }
}

subprojects {
    apply(plugin = "com.android.library")
    apply(plugin = "kotlin-android")
    apply(plugin = "com.lagradost.cloudstream3.gradle")

    cloudstream {
        setRepo(System.getenv("GITHUB_REPOSITORY") ?: "user/repo")
    }

    android {
        namespace = "com.example"
        defaultConfig {
            minSdk = 21
            compileSdkVersion(35)
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }

        tasks.withType<KotlinJvmCompile> {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_1_8)
                freeCompilerArgs.addAll(
                    "-Xno-call-assertions",
                    "-Xno-param-assertions",
                    "-Xno-receiver-assertions"
                )
            }
        }
    }

    dependencies {
        val cloudstream by configurations
        cloudstream("com.lagradost:cloudstream3:pre-release") // stubs for all CloudStream classes

        implementation(kotlin("stdlib"))
        implementation("com.github.Blatzar:NiceHttp:0.4.16")   // HTTP client with session/cookie support
        implementation("org.jsoup:jsoup:1.22.1")                // HTML parser
        // Jackson: pin to 2.13.1 (or up to 2.20.1 if using current pre-release)
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.1")
        // Optional production libraries:
        implementation("org.mozilla:rhino:1.9.0")               // In-app JS engine for unpacking eval scripts
        implementation("me.xdrop:fuzzywuzzy:1.4.0")             // Fuzzy search string ranking
        implementation("org.bouncycastle:bcpkix-jdk15on:1.70")  // AES/DES/cipher cryptography
    }
}
```

## 3. Per-plugin `build.gradle.kts`

Each plugin folder gets its own tiny `build.gradle.kts` — mostly metadata that ends up in the
generated `plugins.json` and in CloudStream's extension browser UI:

```kotlin
version = 1

cloudstream {
    description = "Movies and TV shows from MySite"
    authors = listOf("yourname")
    status = 1                                   // 1 = working, 0 = down/broken
    tvTypes = listOf("Movie", "TvSeries")
    iconUrl = "https://www.google.com/s2/favicons?domain=mysite.example&sz=%size%"
    language = "en"
}
```

Notes:
- `language` is **required** — CloudStream filters the extension list by language, and a
  missing/wrong value means the plugin simply never shows up, with no error.
- `%size%` in `iconUrl` is a template placeholder CloudStream substitutes with the actual icon
  size it needs at render time — leave it literal.
- **Don't add `apiVersion`** to this block; it isn't a recognized property and will fail the
  build.
- Bump `version` (a plain integer) on every change you want users' installed copies to update to.

## 4. The plugin entry point

Somewhere in the same module, one class wires the provider(s) into CloudStream's plugin loader:

```kotlin
@CloudstreamPlugin
class MySitePlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(MySiteProvider())
        // registerExtractorAPI(MyHostExtractor()) // if you wrote a custom extractor
    }
}
```

## 5. Distribution: `repo.json` and `plugins.json`

CloudStream adds a whole *repository* at once (via `Extensions → Add Repository`), pointed at a
`repo.json`:

```json
{
  "name": "My Plugin Repo",
  "description": "A collection of CloudStream plugins",
  "manifestVersion": 1,
  "pluginLists": [
    "https://raw.githubusercontent.com/<user>/<repo>/builds/plugins.json"
  ]
}
```

- `manifestVersion` is currently unused (reserved for future compatibility handling).
- `pluginLists` is a list of URLs, each pointing at a `plugins.json` — every one gets fetched.
- **You write `repo.json` by hand once**, in the `builds` branch. It is *not* generated for you.
- `plugins.json`, by contrast, **is auto-generated** by the template's CI on every push to
  `master` and lives alongside `repo.json` in the `builds` branch. It lists every plugin's
  metadata plus its `.cs3` download URL, file hash, and size, which CloudStream uses to verify
  the download. If you're not using the template's CI, you can generate it manually with
  `./gradlew makePluginsJson`.
- **Known mismatch**: newer versions of the CloudStream gradle plugin add extra fields
  (`jarUrl`, `jarFileSize`, `jarHash`) to `plugins.json` that older CloudStream app builds don't
  expect. Symptom: the repo shows up in CloudStream, but no plugins from it appear in the
  extension list. Fix: manually strip those extra fields from `plugins.json` in the `builds`
  branch, or make sure users are on a current-enough CloudStream build.

## 6. Local testing

- Build/deploy a single module directly: `./gradlew MySiteProvider:make` or
  `./gradlew MySiteProvider:deployWithAdb` (Windows: `.\gradlew.bat ...`).
- On Android 11+, CloudStream needs "All files access" granted for local plugin loading to work:
  `adb shell appops set --uid <package> MANAGE_EXTERNAL_STORAGE allow`, where `<package>` is
  `com.lagradost.cloudstream3` (stable), `com.lagradost.cloudstream3.prerelease` (prerelease), or
  `com.lagradost.cloudstream3.prerelease.debug` (debug) depending on which build you're running.
  This can also be granted manually in Android's Settings → Apps → (CloudStream) → Special app
  access → All files access.

## 7. Gotchas checklist

- [ ] `builds` branch exists *before* the first CI push.
- [ ] CI's first-run clean step has `|| true` appended (no `.cs3` exists yet).
- [ ] CloudStream gradle plugin coordinate actually resolves (avoid stale `-SNAPSHOT` refs).
- [ ] Kotlin gradle plugin version matches what current CloudStream stubs expect.
- [ ] `language` is set correctly in every plugin's `cloudstream { }` block.
- [ ] No `apiVersion` property anywhere in a `cloudstream { }` block.
- [ ] Jackson pinned at `2.13.1`, not newer.
- [ ] `newExtractorLink(...)`'s optional fields are set inside its trailing lambda, not passed
      positionally.
- [ ] If a plugin's repo shows up but the plugin itself doesn't: check the content-type filter in
      CloudStream's settings (e.g. TvSeries disabled hides anything with `TvSeries` in
      `tvTypes`), then check for the `jarUrl`/`jarFileSize`/`jarHash` field mismatch above, then
      as a last resort remove the repo, force-stop CloudStream, clear its app cache, and re-add.