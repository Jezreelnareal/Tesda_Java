import { NextRequest, NextResponse } from "next/server";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

async function proxy(
  request: NextRequest,
  context: { params: Promise<{ path: string[] }> },
) {
  const { path } = await context.params;
  if (path.some((segment) => !/^[a-z-]+$/.test(segment))) {
    return NextResponse.json({ error: "Endpoint not found." }, { status: 404 });
  }
  if (request.method !== "GET") {
    // A cross-site form cannot supply this header; cross-site fetch also fails
    // the exact Origin check. The API is never exposed by a wildcard CORS rule.
    const origin = request.headers.get("origin");
    // NextURL normalizes loopback hostnames; the browser's Origin uses the
    // actual Host header (localhost and 127.0.0.1 are distinct origins).
    const expectedOrigin = `${request.nextUrl.protocol}//${request.headers.get("host")}`;
    if (
      request.headers.get("x-jcash-request") !== "1" ||
      origin !== expectedOrigin
    ) {
      return NextResponse.json(
        { error: "Request verification failed." },
        { status: 403 },
      );
    }
  }
  const headers = new Headers({
    "Content-Type": "application/json",
    "X-JCash-Request": "1",
  });
  const cookie = request.cookies.get("jcash_session");
  if (cookie) headers.set("Cookie", `jcash_session=${cookie.value}`);
  try {
    let body: string | undefined;
    if (request.method !== "GET") {
      // Bound the stream before buffering it, even when Content-Length is absent.
      const reader = request.body?.getReader();
      const chunks: Uint8Array[] = [];
      let size = 0;
      if (reader)
        while (true) {
          const { done, value } = await reader.read();
          if (done) break;
          size += value.byteLength;
          if (size > 8192) {
            await reader.cancel();
            return NextResponse.json(
              { error: "Request is too large." },
              { status: 413 },
            );
          }
          chunks.push(value);
        }
      body = Buffer.concat(chunks).toString("utf8");
    }
    const base = process.env.JCASH_API_URL ?? "http://127.0.0.1:8080";
    const upstream = await fetch(`${base}/api/${path.join("/")}`, {
      method: request.method,
      headers,
      body,
      cache: "no-store",
      redirect: "error",
    });
    const response = new NextResponse(await upstream.text(), {
      status: upstream.status,
      headers: {
        "Content-Type": "application/json",
        "Cache-Control": "no-store",
      },
    });
    const setCookie = upstream.headers.get("set-cookie");
    if (setCookie)
      response.headers.set(
        "Set-Cookie",
        setCookie + (request.nextUrl.protocol === "https:" ? "; Secure" : ""),
      );
    return response;
  } catch {
    return NextResponse.json(
      { error: "Cannot reach the Java API. Start the backend and try again." },
      { status: 503 },
    );
  }
}

export { proxy as GET, proxy as POST };
