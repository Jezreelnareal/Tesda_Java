import type { Metadata } from "next";
import { SessionProvider } from "@/components/shared/SessionProvider";
import "./globals.css";

export const metadata: Metadata = {
  title: "JCash — Your everyday wallet",
  description:
    "Manage your JCash wallet, transfer money, and keep track of your transactions.",
};
export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <head>
        {/* Apply the saved theme while parsing HTML, before React or first paint. */}
        <script
          dangerouslySetInnerHTML={{
            __html: `try { document.documentElement.dataset.theme = localStorage.getItem("jcash-theme") === "dark" ? "dark" : "light"; } catch { document.documentElement.dataset.theme = "light"; }`,
          }}
        />
      </head>
      <body>
        <SessionProvider>{children}</SessionProvider>
      </body>
    </html>
  );
}
