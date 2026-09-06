import { spawn } from "node:child_process";
import { readFileSync } from "node:fs";
import { delimiter, resolve } from "node:path";

if (!process.env.JCASH_DB_URL?.includes("/jcash_web_test_")) {
  throw new Error(
    "Browser tests require an isolated database. Run scripts/test-web.ps1 from the repository root.",
  );
}
const classpath = [
  resolve("../target/classes"),
  readFileSync("../target/web-classpath.txt", "utf8").trim(),
].join(delimiter);
const java = spawn("java", ["-cp", classpath, "Main"], {
  stdio: "inherit",
  env: { ...process.env, JCASH_API_PORT: "8181" },
  windowsHide: true,
});
java.on("exit", (code) => process.exit(code ?? 1));
for (const signal of ["SIGINT", "SIGTERM"])
  process.on(signal, () => java.kill());
