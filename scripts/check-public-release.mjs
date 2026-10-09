import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const strict = process.argv.includes('--strict');
const findings = [];
const textExtensions = new Set(['.java', '.vue', '.ts', '.mjs', '.css', '.html', '.svg',
  '.json', '.xml', '.properties', '.sql', '.md', '.ps1', '.yml', '.yaml']);
const textNames = new Set(['.gitignore', '.gitattributes', 'mvnw', 'mvnw.cmd']);
const excludedSegments = new Set(['.git', '.idea', '.run', '.vscode', 'node_modules',
  'target', 'dist', 'outputs', 'uploads', 'avatars']);
const rules = [
  ['private key', /-----BEGIN (?:RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----/],
  ['credential token', /(?:gh[pousr]_|github_pat_|sk-)[A-Za-z0-9_-]{16,}/],
  ['inline URL credentials', /https?:\/\/[^\s/]+:[^\s/]+@/],
  ['local Windows path', /[A-Za-z]:[\\/](?:Users|Projects)[\\/][^\s]+/i],
  ['local Unix home path', /\/(?:Users|home)\/[^\s/]+\//],
  ['phone number', /(?<!\d)1[3-9]\d{9}(?!\d)/],
  ['identity number', /(?<!\d)\d{17}[\dXx](?!\d)/],
  ['personal mailbox', /[\w.+-]+@(?:qq|163|126|gmail|outlook|hotmail)\.com/i],
  ['legacy default password', new RegExp('admin' + '123' + '|(?:password\\s*=\\s*|mysql[^\\n]*-p)' + '123' + '456', 'i')],
  ['database password literal', /^spring\.datasource\.password\s*=\s*(?!\$\{)\S+/m],
  ['admin password literal', /^hospital\.admin\.password\s*=\s*(?!\$\{)\S+/m],
  ['API key literal', /^ai\.qwen\.api-key\s*=\s*(?!\$\{)\S+/m],
];

let fileCount = 0;
function walk(directory) {
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    const absolute = path.join(directory, entry.name);
    const relative = path.relative(root, absolute).split(path.sep).join('/');
    const localConfig = /^config\/(?!application-local\.example\.properties$)/.test(relative)
      || /(?:^|\/)(?:\.env(?:\..*)?|application-local\.(?:properties|ya?ml))$/.test(relative);
    const generated = relative === 'src/main/resources/static/spa';
    // Git metadata is inspected separately with the history secret scan.
    if (relative === '.git') continue;
    if (entry.isSymbolicLink()) {
      findings.push([relative, 'linked artifact']);
      continue;
    }
    if (excludedSegments.has(entry.name) || localConfig || generated) {
      if (strict) findings.push([relative, 'private or generated artifact']);
      continue;
    }
    if (entry.isDirectory()) {
      walk(absolute);
      continue;
    }
    fileCount++;
    const extension = path.extname(entry.name).toLowerCase();
    if (extension === '.png' && relative === 'src/main/resources/static/images/hospital-outpatient.png') {
      const bytes = fs.readFileSync(absolute);
      if (!bytes.subarray(0, 8).equals(Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]))) {
        findings.push([relative, 'invalid artwork format']);
      }
      // Retain public C2PA provenance. Do not remove it to obscure the AI origin.
      continue;
    }
    if (!textExtensions.has(extension) && !textNames.has(entry.name)) {
      findings.push([relative, 'unreviewed file type']);
      continue;
    }
    const contents = fs.readFileSync(absolute, 'utf8');
    for (const [label, expression] of rules) {
      if (expression.test(contents)) findings.push([relative, label]);
    }
    if (extension === '.sql' && /INSERT\s+INTO\s+`?(?:user|patient|doctor|appointment|medical_record|report|ai_generation_log)`?\s*\(/i.test(contents)) {
      findings.push([relative, 'personal or clinical data insert']);
    }
  }
}

walk(root);
if (findings.length) {
  for (const [relative, label] of findings) console.error(`${relative}: ${label}`);
  process.exitCode = 1;
} else {
  console.log(`Publication file checks passed: ${fileCount} files (${strict ? 'strict distribution' : 'source'} mode).`);
}
