import { expect, test } from "@playwright/test";

test("saved theme applies before the application JavaScript loads", async ({
  page,
}) => {
  await page.goto("/");
  await page.getByRole("button", { name: "Switch to dark mode" }).click();

  // Blocking the bundles reproduces a slow reload: effects cannot set the theme.
  await page.route("**/_next/**/*.js*", (route) => route.abort());
  for (const path of ["/", "/user", "/admin"]) {
    await page.goto(path);
    expect(await page.locator("html").getAttribute("data-theme")).toBe("dark");
    expect(
      await page
        .locator("html")
        .evaluate((element) => getComputedStyle(element).colorScheme),
    ).toBe("dark");
  }

  await page.unroute("**/_next/**/*.js*");
  await page.goto("/");
  await page.getByRole("button", { name: "Switch to light mode" }).click();
  await page.route("**/_next/**/*.js*", (route) => route.abort());
  await page.reload();
  expect(await page.locator("html").getAttribute("data-theme")).toBe("light");
});
