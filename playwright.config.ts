import { defineConfig, devices } from "@playwright/test";

const storefrontUrl = process.env.SHOPPEW_STOREFRONT_URL ?? "http://localhost:3000";
const sellerUrl = process.env.SHOPPEW_SELLER_URL ?? "http://localhost:3001";
const adminUrl = process.env.SHOPPEW_ADMIN_URL ?? "http://localhost:3002";
const browserExecutablePath = process.env.PLAYWRIGHT_EXECUTABLE_PATH;

export default defineConfig({
  testDir: "./tests/e2e",
  globalSetup: "./tests/e2e/global-setup.ts",
  globalTeardown: "./tests/e2e/global-teardown.ts",
  outputDir: "test-results",
  fullyParallel: false,
  workers: 1,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI
    ? [["line"], ["html", { open: "never" }]]
    : [["list"], ["html", { open: "never" }]],
  timeout: 120_000,
  expect: { timeout: 30_000 },
  use: {
    baseURL: storefrontUrl,
    locale: "vi-VN",
    timezoneId: "Asia/Ho_Chi_Minh",
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
    video: "retain-on-failure",
  },
  projects: [
    {
      name: "desktop-chrome",
      use: {
        ...devices["Desktop Chrome"],
        // Use the workstation/runner Chrome installation. This deliberately
        // avoids downloading a Playwright-managed browser binary.
        channel: browserExecutablePath ? undefined : (process.env.PLAYWRIGHT_CHANNEL ?? "chrome"),
        launchOptions: browserExecutablePath ? { executablePath: browserExecutablePath } : undefined,
      },
    },
  ],
  webServer: [
    {
      command: "pnpm --filter @shoppew/storefront dev",
      url: storefrontUrl,
      reuseExistingServer: !process.env.CI,
      timeout: 180_000,
    },
    {
      command: "pnpm --filter @shoppew/seller dev",
      url: sellerUrl,
      reuseExistingServer: !process.env.CI,
      timeout: 120_000,
    },
    {
      command: "pnpm --filter @shoppew/admin dev",
      url: adminUrl,
      reuseExistingServer: !process.env.CI,
      timeout: 120_000,
    },
  ],
});
