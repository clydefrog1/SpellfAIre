import { defineConfig, devices } from '@playwright/test';

const isCI = !!process.env.CI;

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: isCI,
  retries: isCI ? 2 : 0,
  workers: isCI ? 2 : undefined,
  reporter: [['html', { open: 'never' }], ['list']],
  use: {
    baseURL: 'http://localhost:4200',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure'
  },
  webServer: {
    command: 'npm run start -- --host localhost --port 4200',
    url: 'http://localhost:4200',
    reuseExistingServer: !isCI,
    timeout: 120000
  },
  projects: [
    {
      name: 'real-backend',
      testMatch: /.*\.real\.spec\.ts$/,
      use: {
        ...devices['Desktop Chrome']
      }
    },
    {
      name: 'mocked-api',
      testMatch: /.*\.mock\.spec\.ts$/,
      use: {
        ...devices['Desktop Chrome']
      }
    }
  ]
});
