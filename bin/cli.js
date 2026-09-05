#!/usr/bin/env node

const readline = require('readline');
const path = require('path');
const { PROVIDERS, detectHarnesses, installSkills, SKILL_NAME } = require('../lib/installer');
const pkg = require('../package.json');

const args = process.argv.slice(2);
const command = args[0] || 'install';

if (args.includes('--help') || args.includes('-h') || command === 'help') {
  printHelp();
  process.exit(0);
}

if (args.includes('--version') || args.includes('-v') || command === 'version') {
  console.log(`cloudstream-skill v${pkg.version}`);
  process.exit(0);
}

function parseFlags() {
  const flags = {
    providers: null,
    scope: null
  };

  for (const arg of args) {
    if (arg.startsWith('--providers=')) {
      const value = arg.split('=')[1];
      flags.providers = value === 'all'
        ? Object.keys(PROVIDERS)
        : value.split(',').map(p => p.trim());
    } else if (arg.startsWith('--scope=')) {
      flags.scope = arg.split('=')[1].trim();
    }
  }

  return flags;
}

function printHelp() {
  console.log(`
🚀 cloudstream-skill CLI Installer v${pkg.version}

Usage:
  npx github:sheikhshariarnehal/cloudstream-skill install [options]
  npx github:sheikhshariarnehal/cloudstream-skill update  [options]

Options:
  --providers=<p1,p2>  Providers to install into (antigravity, claude, cursor, codex, grok, all)
  --scope=<scope>      Installation scope (project, global, both)
  -h, --help           Show help
  -v, --version        Show version

Examples:
  npx github:sheikhshariarnehal/cloudstream-skill install
  npx github:sheikhshariarnehal/cloudstream-skill install --scope=global
  npx github:sheikhshariarnehal/cloudstream-skill install --providers=antigravity,claude --scope=project
  npx github:sheikhshariarnehal/cloudstream-skill update  --scope=global
`);
}

async function runInteractive(commandName) {
  const isUpdate = commandName === 'update';
  console.log(`\n📦 cloudstream-skill AI Agent Skill ${isUpdate ? 'Updater' : 'Installer'} v${pkg.version}\n`);

  const detected = detectHarnesses();
  console.log('🔍 Detected AI Agent Harnesses:');
  for (const item of detected) {
    const status = [];
    if (item.hasProject) status.push('project');
    if (item.hasGlobal) status.push('global');
    const badge = status.length > 0 ? `[✓ ${status.join('/')}]` : '[available]';
    console.log(`  • ${item.name} (${item.id}) ${badge}`);
  }
  console.log('');

  const flags = parseFlags();

  let selectedProviders = flags.providers;
  let scope = flags.scope;

  // Non-interactive fallback when run without a TTY
  if (!process.stdin.isTTY && (!selectedProviders || !scope)) {
    selectedProviders = selectedProviders || ['antigravity'];
    scope = scope || 'global';
  }

  if (!selectedProviders || !scope) {
    const rl = readline.createInterface({
      input: process.stdin,
      output: process.stdout
    });

    const ask = (query) => new Promise(resolve => rl.question(query, resolve));

    if (!selectedProviders) {
      const defaultProviders = detected.map(d => d.id).join(',') || 'antigravity';
      const ans = await ask(`Select providers to install into [default: ${defaultProviders}]: `);
      selectedProviders = ans.trim()
        ? (ans.trim() === 'all' ? Object.keys(PROVIDERS) : ans.split(',').map(s => s.trim()))
        : (detected.length > 0 ? detected.map(d => d.id) : ['antigravity']);
    }

    if (!scope) {
      console.log('\nSelect installation scope:');
      console.log('  1. Current Project (.agents/skills, .claude/skills, etc.)');
      console.log('  2. Global (~/.gemini/config/skills, ~/.claude/skills, etc.)');
      console.log('  3. Both');
      const ans = await ask('Choose scope (1-3) [default: 2]: ');
      const choice = ans.trim();
      scope = choice === '1' ? 'project' : (choice === '3' ? 'both' : 'global');
    }

    rl.close();
  }

  console.log(`\n⏳ ${isUpdate ? 'Updating' : 'Installing'} skill '${SKILL_NAME}' for: ${selectedProviders.join(', ')} (Scope: ${scope})...\n`);

  const outcome = installSkills({
    selectedProviders,
    scope,
    isUpdate
  });

  console.log(`✅ ${isUpdate ? 'Update' : 'Installation'} Complete!\n`);
  console.log('Installed Skill Locations:');
  for (const item of outcome.results) {
    console.log(`  ✓ [${item.provider}] (${item.scope}): ${item.skill} → ${item.targetPath}`);
  }
  console.log('\n🎉 You can now trigger `/provider <url>` or `/optimize` in your AI coding assistant!\n');
}

runInteractive(command).catch(err => {
  console.error('\n❌ Installation failed:', err.message);
  process.exit(1);
});
