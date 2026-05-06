import { mkdir, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, '..');
const environmentsDir = path.join(projectRoot, 'src', 'environments');

const defaultDevelopmentUrl = '/api';
const defaultProductionUrl = '/api';
const resolvedProductionUrl = normalizeApiBaseUrl(process.env.API_BASE_URL || defaultProductionUrl);

await mkdir(environmentsDir, { recursive: true });

await Promise.all([
  writeEnvironmentFile(path.join(environmentsDir, 'environment.ts'), false, defaultDevelopmentUrl),
  writeEnvironmentFile(path.join(environmentsDir, 'environment.development.ts'), false, defaultDevelopmentUrl),
  writeEnvironmentFile(path.join(environmentsDir, 'environment.prod.ts'), true, resolvedProductionUrl),
  writeEnvironmentFile(path.join(environmentsDir, 'environment.production.ts'), true, resolvedProductionUrl),
]);

function normalizeApiBaseUrl(value) {
  const normalized = value.trim().replace(/\/+$/, '');
  if (normalized.startsWith('/')) {
    return normalized;
  }
  if (!/^https?:\/\//.test(normalized)) {
    throw new Error('API_BASE_URL must start with http://, https://, or /');
  }
  return normalized;
}

async function writeEnvironmentFile(filePath, production, apiBaseUrl) {
  const content = `export const environment = {
  production: ${production},
  apiUrl: '${apiBaseUrl}',
};
`;
  await writeFile(filePath, content, 'utf8');
}
