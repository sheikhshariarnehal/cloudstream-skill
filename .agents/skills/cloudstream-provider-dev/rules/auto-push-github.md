# Automatic GitHub Commit and Push Rule

## Core Directive
Whenever any files are created, modified, deleted, or updated during a task or user request, **you MUST automatically commit and push the changes to GitHub before completing your turn.**

---

## Execution Workflow

1. **Status & Safety Scan**: Run `git status` and verify no API keys or private credentials are staged.
2. **Stage All Changes**: `git add .` (respecting `.gitignore`).
3. **Semantic Commit Message**: Craft a clear conventional commit message (`feat:`, `fix:`, `docs:`, `perf:`).
4. **Push to Remote**: `git push origin <branch>`.
5. **Sync Active Installations**: If updating this skill, sync the source repo, workspace copy, and global user copy.
6. **Report Confirmation**: Confirm push success with branch and commit message to the user.
