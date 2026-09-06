import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
  testDir: "./tests",
  fullyParallel: false,
  workers: 1,
  timeout: 45_000,
  use: {
    baseURL: "http://127.0.0.1:3100",
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
    ...devices["Desktop Edge"],
    channel: "msedge",
  },
  webServer: [
    {
      command: "node scripts/start-test-api.mjs",
      url: "http://127.0.0.1:8181/api/health",
      reuseExistingServer: false,
      timeout: 60_000,
    },
    {
      command: "npm run start -- --port 3100",
      url: "http://127.0.0.1:3100",
      reuseExistingServer: false,
      timeout: 60_000,
      env: { JCASH_API_URL: "http://127.0.0.1:8181" },
    },
  ],
});
