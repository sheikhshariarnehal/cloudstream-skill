# Provider Version Bump Rule

## Core Directive
Whenever any code, extractor, feature, bugfix, or refactor is made to an existing CloudStream provider:
**You MUST ALWAYS increment the integer `version` property in that provider module's `build.gradle.kts`.**

---

## Why This is Required
CloudStream compares the version integer in the user's installed plugin against the version declared in the repository's `plugins.json`:
- If `version` is unchanged (e.g. remains `version = 1`), CloudStream considers the plugin up to date and **never** fetches or prompts the user for the update.
- Incrementing `version` (e.g. `version = 1` ➔ `version = 2`) triggers the in-app update notification and replaces the old `.cs3` with the newly compiled one.

---

## Action Checklist
1. Identify the modified provider's module directory (e.g. `MyProvider/`).
2. Open its `build.gradle.kts`.
3. Locate `version = <number>`.
4. Increment the integer by 1 (e.g., `version = 2` ➔ `version = 3`).
5. Include the version bump in your commit message (e.g. `fix(MyProvider): resolve video stream decryption (v3)`).
