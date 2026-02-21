import { spawn } from 'node:child_process';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const BACKEND_URL = process.env.SPELLFAIRE_BACKEND_URL ?? 'http://localhost:8080/api/auth/me';
const HEALTHCHECK_ATTEMPTS = 90;
const HEALTHCHECK_DELAY_MS = 2000;
const MYSQL_HEALTHCHECK_ATTEMPTS = 60;
const MYSQL_HEALTHCHECK_DELAY_MS = 2000;

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const REPO_ROOT = resolve(__dirname, '..');

const backendRunEnv = {
  ...process.env,
  PORT: process.env.PORT ?? '8080',
  MYSQL_URL:
    process.env.MYSQL_URL ??
    'jdbc:mysql://localhost:3307/spellfaire?useSSL=false&allowPublicKeyRetrieval=true',
  MYSQL_USER: process.env.MYSQL_USER ?? 'root',
  MYSQL_PASSWORD: process.env.MYSQL_PASSWORD ?? 'spellfaire',
  CORS_ALLOWED_ORIGINS: process.env.CORS_ALLOWED_ORIGINS ?? 'http://localhost:4200',
  JWT_SECRET: process.env.JWT_SECRET ?? 'e2e-local-secret',
  REFRESH_COOKIE_SECURE: process.env.REFRESH_COOKIE_SECURE ?? 'false',
  SPELLFAIRE_DB_RESET_ON_STARTUP: process.env.SPELLFAIRE_DB_RESET_ON_STARTUP ?? 'false',
  SPELLFAIRE_DATA_INIT_ENABLED: process.env.SPELLFAIRE_DATA_INIT_ENABLED ?? 'false',
  SPELLFAIRE_DATA_INIT_DROP_EXISTING: process.env.SPELLFAIRE_DATA_INIT_DROP_EXISTING ?? 'false'
};

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

function run(command, args) {
  return new Promise((resolve, reject) => {
    const child = process.platform === 'win32'
      ? spawn('cmd.exe', ['/d', '/s', '/c', command, ...args], { stdio: 'inherit' })
      : spawn(command, args, { stdio: 'inherit' });

    child.on('error', reject);
    child.on('exit', code => {
      if (code === 0) {
        resolve();
        return;
      }
      reject(new Error(`Command failed: ${command} ${args.join(' ')} (exit ${code})`));
    });
  });
}

function runWithExitCode(command, args) {
  return new Promise(resolve => {
    const child = process.platform === 'win32'
      ? spawn('cmd.exe', ['/d', '/s', '/c', command, ...args], { stdio: 'ignore' })
      : spawn(command, args, { stdio: 'ignore' });

    child.on('error', () => resolve(1));
    child.on('exit', code => resolve(code ?? 1));
  });
}

function startBackground(command, args, options = {}) {
  if (process.platform === 'win32') {
    return spawn('cmd.exe', ['/d', '/s', '/c', command, ...args], {
      stdio: 'inherit',
      cwd: options.cwd,
      env: options.env
    });
  }

  return spawn(command, args, {
    stdio: 'inherit',
    cwd: options.cwd,
    env: options.env
  });
}

async function isBackendReachable() {
  for (let attempt = 1; attempt <= HEALTHCHECK_ATTEMPTS; attempt += 1) {
    try {
      const response = await fetch(BACKEND_URL, { method: 'GET' });
      if (response.status > 0) {
        return true;
      }
    } catch {
      // Ignore and retry.
    }

    if (attempt < HEALTHCHECK_ATTEMPTS) {
      await sleep(HEALTHCHECK_DELAY_MS);
    }
  }

  return false;
}

function stopProcess(child, label) {
  if (!child || child.exitCode !== null) {
    return;
  }

  console.log(`Stopping ${label}...`);
  child.kill('SIGTERM');
}

async function forceReleaseBackendPort() {
  const backendPort = backendRunEnv.PORT ?? '8080';

  if (process.platform === 'win32') {
    await runWithExitCode('powershell', [
      '-NoProfile',
      '-Command',
      `$listener = Get-NetTCPConnection -LocalPort ${backendPort} -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1; if ($listener) { Stop-Process -Id $listener.OwningProcess -Force }`
    ]);
    return;
  }

  await runWithExitCode('sh', ['-c', `lsof -ti tcp:${backendPort} | xargs -r kill -9`]);
}

async function startLocalMySql() {
  console.log('Starting MySQL container for real e2e...');
  await run('docker', ['compose', '-f', 'infra/docker-compose.yml', 'up', '-d', 'mysql']);
}

async function resetLocalMySql() {
  console.log('Resetting MySQL container and volume for real e2e...');
  await run('docker', ['compose', '-f', 'infra/docker-compose.yml', 'down', '-v']);
  await run('docker', ['compose', '-f', 'infra/docker-compose.yml', 'up', '-d', 'mysql']);
}

async function waitForMySqlReady() {
  console.log('Waiting for MySQL readiness...');

  for (let attempt = 1; attempt <= MYSQL_HEALTHCHECK_ATTEMPTS; attempt += 1) {
    const exitCode = await runWithExitCode('docker', [
      'compose',
      '-f',
      'infra/docker-compose.yml',
      'exec',
      '-T',
      'mysql',
      'mysqladmin',
      'ping',
      '-h',
      'localhost',
      '-uroot',
      '-pspellfaire'
    ]);

    if (exitCode === 0) {
      console.log('MySQL is ready.');
      return;
    }

    if (attempt < MYSQL_HEALTHCHECK_ATTEMPTS) {
      await sleep(MYSQL_HEALTHCHECK_DELAY_MS);
    }
  }

  throw new Error('MySQL did not become ready in time.');
}

function startBackend() {
  console.log('Starting backend for real e2e...');
  const backendCommand = process.platform === 'win32' ? '.\\backend\\mvnw.cmd' : './backend/mvnw';

  return startBackground(backendCommand, ['-f', 'backend/pom.xml', 'spring-boot:run'], {
    cwd: REPO_ROOT,
    env: backendRunEnv
  });
}

async function main() {
  const shouldAutostart = process.argv.includes('--autostart');
  const shouldResetDb = process.argv.includes('--reset-db');
  const npmCommand = 'npm';
  let backendProcess = null;

  console.log('Running mocked e2e suite...');
  await run(npmCommand, ['run', 'test:e2e']);

  let reachable = await isBackendReachable();

  if (!reachable && shouldAutostart) {
    if (shouldResetDb) {
      await resetLocalMySql();
    } else {
      await startLocalMySql();
    }

    await waitForMySqlReady();

    backendProcess = startBackend();
    reachable = await isBackendReachable();

    if (!reachable) {
      throw new Error(`Backend did not become reachable at ${BACKEND_URL} after autostart.`);
    }
  }

  if (!reachable) {
    console.log(`Skipping real-backend e2e suite (backend not reachable at ${BACKEND_URL}).`);
    return;
  }

  try {
    console.log('Backend is reachable. Running real-backend e2e suite...');
    await run(npmCommand, ['run', 'test:e2e:real']);
  } finally {
    stopProcess(backendProcess, 'backend process');
    if (backendProcess) {
      await sleep(1000);
      await forceReleaseBackendPort();
    }
  }
}

main().catch(error => {
  console.error(error.message);
  process.exit(1);
});
