import { expect, test, type Page } from "@playwright/test";

async function login(
  page: Page,
  role: "user" | "admin",
  identity: string,
  pin: string,
) {
  await page.goto("/");
  if (role === "admin")
    await page
      .getByRole("button", { name: "Administrator", exact: true })
      .click();
  await page
    .getByLabel(role === "admin" ? "Admin username" : "Mobile number", {
      exact: true,
    })
    .fill(identity);
  await page.getByLabel("PIN", { exact: true }).fill(pin);
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
}

test("customer cash-in, validation, withdrawal, transfer, history and logout", async ({
  page,
  browser,
}) => {
  const errors: string[] = [];
  page.on("pageerror", (error) => errors.push(error.message));
  await page.goto("/");
  await expect(
    page.getByRole("heading", { name: "Welcome back." }),
  ).toBeVisible();
  await page.screenshot({ path: "test-results/welcome.png", fullPage: true });
  await login(page, "user", "09171234567", "1234");
  await expect(page).toHaveURL("/user");
  await expect(page.locator(".balance-value")).toContainText("1,000.00");
  await page.getByRole("button", { name: "Cash in", exact: false }).click();
  await page.getByLabel("Amount (PHP)").fill("100.00");
  await page.getByRole("button", { name: "Confirm cash in" }).click();
  await expect(page.locator(".balance-value")).toContainText("1,100.00");
  await page.getByRole("button", { name: "Withdraw", exact: false }).click();
  await page.getByLabel("Amount (PHP)").fill("25");
  await page.getByRole("button", { name: "Confirm withdraw" }).click();
  await expect(page.locator(".balance-value")).toContainText("1,075.00");
  await page.getByRole("button", { name: "Send money", exact: false }).click();
  await page.getByLabel("Recipient mobile number").fill("09999999999");
  await page.getByLabel("Amount (PHP)").fill("50");
  await page.getByRole("button", { name: "Confirm send money" }).click();
  await expect(page.getByRole("dialog").getByRole("alert")).toBeVisible();
  await page.getByLabel("Recipient mobile number").fill("09181234567");
  await page.getByLabel("Amount (PHP)").fill("9999");
  await page.getByRole("button", { name: "Confirm send money" }).click();
  await expect(page.getByRole("dialog").getByRole("alert")).toContainText(
    "Insufficient",
  );
  await page.getByLabel("Amount (PHP)").fill("50");
  await page.getByRole("button", { name: "Confirm send money" }).click();
  await expect(page.locator(".balance-value")).toContainText("1,025.00");
  await expect(page.getByRole("dialog")).toHaveCount(0);
  await page.screenshot({
    path: "test-results/user-dashboard.png",
    fullPage: true,
  });
  await page.getByRole("button", { name: "Activity", exact: true }).click();
  await expect(page.locator("tbody tr")).toHaveCount(3);
  await page.getByLabel("Search transactions").fill("Cash in");
  await expect(page.locator("tbody tr")).toHaveCount(1);
  const other = await browser.newContext();
  const receiver = await other.newPage();
  await login(receiver, "user", "09181234567", "5678");
  await expect(receiver.locator(".balance-value")).toContainText("550.00");
  await expect(receiver.locator(".amount.in")).toContainText("50.00");
  await other.close();
  await page.getByRole("button", { name: "Sign out" }).click();
  await expect(page).toHaveURL("/");
  await page.goto("/user");
  await expect(page).toHaveURL("/");
  expect(errors).toEqual([]);
});

test("admin creates accounts, adjusts balances, and reads reports", async ({
  page,
}) => {
  await login(page, "admin", "admin", "1234");
  await expect(page).toHaveURL("/admin");
  await expect(
    page.getByRole("heading", { name: "Welcome back, admin." }),
  ).toBeVisible();
  await page.getByRole("button", { name: "New account" }).click();
  await page
    .getByLabel("Full name", { exact: true })
    .fill("Browser Test Customer");
  await page.getByLabel("Mobile number", { exact: true }).fill("09191234567");
  await page.getByLabel("PIN", { exact: true }).fill("2468");
  await page.getByLabel("Confirm PIN", { exact: true }).fill("2468");
  await page
    .getByRole("button", { name: "Create account", exact: true })
    .click();
  await expect(page.getByRole("dialog")).toHaveCount(0);
  await page.getByRole("button", { name: "Accounts", exact: true }).click();
  await page.getByLabel("Search accounts").fill("09191234567");
  await expect(page.locator("tbody tr")).toHaveCount(1);
  await page.getByRole("button", { name: "Credit", exact: true }).click();
  await page.getByLabel("Amount (PHP)").fill("200");
  await page.getByRole("button", { name: "Confirm adjustment" }).click();
  await expect(page.locator("tbody tr")).toContainText("200.00");
  await page.getByRole("button", { name: "Debit", exact: true }).click();
  await page.getByLabel("Amount (PHP)").fill("50");
  await page.getByRole("button", { name: "Confirm adjustment" }).click();
  await expect(page.locator("tbody tr")).toContainText("150.00");
  await page.getByRole("button", { name: "Reports", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Transactions by type" }),
  ).toBeVisible();
  await expect(page.locator(".report-panel")).toContainText("200.00");
  await page.screenshot({
    path: "test-results/admin-reports.png",
    fullPage: true,
  });
});

test("registration, mobile layout and theme toggle", async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/");
  await page
    .getByRole("button", { name: "Create an account", exact: true })
    .click();
  await page
    .getByLabel("Full name", { exact: true })
    .fill("Mobile Browser Customer");
  await page.getByLabel("Mobile number", { exact: true }).fill("09201234567");
  await page.getByLabel("PIN", { exact: true }).fill("2468");
  await page.getByLabel("Confirm PIN", { exact: true }).fill("2468");
  await page
    .getByRole("button", { name: "Create account", exact: true })
    .click();
  await expect(page.getByRole("status")).toContainText("Account created");
  await login(page, "user", "09201234567", "2468");
  await expect(page.locator(".balance-value")).toContainText("0.00");
  await page.getByRole("button", { name: "Switch to dark mode" }).click();
  await expect(page.locator("html")).toHaveAttribute("data-theme", "dark");
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBeTruthy();
  await page.screenshot({
    path: "test-results/mobile-wallet.png",
    fullPage: true,
  });
});

test("browser lockout survives reload and cross-site requests are rejected", async ({
  page,
}) => {
  await login(page, "user", "09171234567", "0000");
  await expect(page.locator(".auth-card").getByRole("alert")).toContainText(
    "2 attempt",
  );
  await page.reload();
  await page.getByLabel("Mobile number", { exact: true }).fill("09171234567");
  await page.getByLabel("PIN", { exact: true }).fill("0000");
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await expect(page.locator(".auth-card").getByRole("alert")).toContainText(
    "1 attempt",
  );
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await expect(page.locator(".auth-card").getByRole("alert")).toContainText(
    "locked",
  );
  await expect(
    page.getByRole("button", { name: "Sign in", exact: true }),
  ).toBeDisabled();
  const response = await page.request.post("/api/register", {
    data: {},
    headers: { origin: "https://example.org", "X-JCash-Request": "1" },
  });
  expect(response.status()).toBe(403);
});
