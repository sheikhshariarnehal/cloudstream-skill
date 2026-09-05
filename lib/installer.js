const fs = require('fs');
const path = require('path');
const os = require('os');

const SKILL_NAME = 'cloudstream-provider-dev';

const PROVIDERS = {
  antigravity: {
    name: 'Antigravity IDE / Gemini CLI',
    projectDir: '.agents/skills',
    globalDir: path.join('.gemini', 'config', 'skills')
  },
  claude: {
    name: 'Claude Code',
    projectDir: '.claude/skills',
    globalDir: path.join('.claude', 'skills')
  },
  cursor: {
    name: 'Cursor',
    projectDir: '.cursor/skills',
    globalDir: path.join('.cursor', 'skills')
  },
  codex: {
    name: 'Codex CLI',
    projectDir: '.codex/skills',
    globalDir: path.join('.codex', 'skills')
  },
  grok: {
    name: 'Grok Build',
    projectDir: '.grok/skills',
    globalDir: path.join('.grok', 'skills')
  }
};

/**
 * Detect installed AI agent harness folders in project root or home directory.
 */
function detectHarnesses(projectDir = process.cwd(), homeDir = os.homedir()) {
  const detected = [];

  for (const [id, provider] of Object.entries(PROVIDERS)) {
    const projectPath = path.join(projectDir, provider.projectDir.split('/')[0]);
    const globalPath = path.join(homeDir, provider.globalDir.split(path.sep)[0]);

    const projectExists = fs.existsSync(projectPath);
    const globalExists = fs.existsSync(globalPath);

    if (projectExists || globalExists || id === 'antigravity') {
      detected.push({
        id,
        name: provider.name,
        hasProject: projectExists,
        hasGlobal: globalExists
      });
    }
  }

  return detected;
}

/**
 * Copy directory recursively.
 */
function copyDirRecursive(src, dest) {
  if (!fs.existsSync(dest)) {
    fs.mkdirSync(dest, { recursive: true });
  }

  const entries = fs.readdirSync(src, { withFileTypes: true });

  for (const entry of entries) {
    const srcPath = path.join(src, entry.name);
    const destPath = path.join(dest, entry.name);

    if (entry.isDirectory()) {
      copyDirRecursive(srcPath, destPath);
    } else {
      fs.copyFileSync(srcPath, destPath);
    }
  }
}

/**
 * Copy a skill payload into a target destination folder.
 */
function copySkillPayload(packageRoot, targetSkillDir) {
  if (!fs.existsSync(targetSkillDir)) {
    fs.mkdirSync(targetSkillDir, { recursive: true });
  }

  // Folders and files belonging to the skill
  const skillItems = ['SKILL.md', 'references', 'workflows', 'rules', 'README.md', 'LICENSE', '.gitattributes'];

  for (const item of skillItems) {
    const srcPath = path.join(packageRoot, item);
    const destPath = path.join(targetSkillDir, item);

    if (fs.existsSync(srcPath)) {
      const stat = fs.statSync(srcPath);
      if (stat.isDirectory()) {
        copyDirRecursive(srcPath, destPath);
      } else {
        fs.copyFileSync(srcPath, destPath);
      }
    }
  }
}

/**
 * Execute skill installation or update across selected providers and scope.
 */
function installSkills(options = {}) {
  const {
    selectedProviders = ['antigravity'],
    scope = 'project',
    projectDir = process.cwd(),
    homeDir = os.homedir(),
    packageRoot = path.join(__dirname, '..'),
    isUpdate = false
  } = options;

  const results = [];

  for (const providerId of selectedProviders) {
    const provider = PROVIDERS[providerId];
    if (!provider) continue;

    const targetScopes = scope === 'both' ? ['project', 'global'] : [scope];

    for (const currentScope of targetScopes) {
      const baseDir = currentScope === 'project'
        ? path.join(projectDir, provider.projectDir)
        : path.join(homeDir, provider.globalDir);

      const targetSkillDir = path.join(baseDir, SKILL_NAME);

      copySkillPayload(packageRoot, targetSkillDir);

      results.push({
        provider: provider.name,
        providerId,
        scope: currentScope,
        skill: SKILL_NAME,
        targetPath: targetSkillDir
      });
    }
  }

  return {
    success: true,
    isUpdate,
    installedSkills: [SKILL_NAME],
    results
  };
}

module.exports = {
  SKILL_NAME,
  PROVIDERS,
  detectHarnesses,
  installSkills
};
