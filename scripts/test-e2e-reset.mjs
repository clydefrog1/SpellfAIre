import { spawn } from 'node:child_process';

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

async function main() {
  console.log('Resetting e2e MySQL container and volume...');

  await run('docker', ['compose', '-f', 'infra/docker-compose.yml', 'down', '-v']);
  await run('docker', ['compose', '-f', 'infra/docker-compose.yml', 'up', '-d', 'mysql']);

  console.log('E2E MySQL container reset complete.');
}

main().catch(error => {
  console.error(error.message);
  process.exit(1);
});
