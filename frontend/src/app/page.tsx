"use client";

import { useEffect, useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import {
  ArrowRight,
  ArrowUpRight,
  Check,
  ShieldCheck,
  Wallet,
} from "lucide-react";
import { api, errorText, type Session } from "@/lib/api";
import { useSession } from "@/components/shared/SessionProvider";
import { Brand, ThemeToggle } from "@/components/shared/AppShell";
import {
  AccountFields,
  Field,
  PinField,
  formValues,
} from "@/components/shared/Forms";

export default function Welcome() {
  const router = useRouter();
  const {
    session,
    loading,
    error: connectionError,
    setSession,
    refresh,
  } = useSession();
  const [mode, setMode] = useState<"user" | "admin" | "register">("user");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [connected, setConnected] = useState(false);
  useEffect(() => {
    if (!loading && session.role !== "guest")
      router.replace(`/${session.role}`);
  }, [session.role, loading, router]);
  useEffect(() => {
    api("health")
      .then(() => setConnected(true))
      .catch(() => setConnected(false));
  }, []);
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    const values = formValues(event);
    setBusy(true);
    setError("");
    setSuccess("");
    try {
      if (mode === "register") {
        await api("register", values);
        setMode("user");
        setSuccess("Account created. Sign in with your mobile number and PIN.");
      } else {
        const next = await api<Session>("login", { ...values, role: mode });
        setSession(next);
        router.replace(`/${next.role}`);
      }
    } catch (error) {
      setError(errorText(error));
      await refresh();
    } finally {
      setBusy(false);
    }
  }
  const remaining =
    mode === "admin" ? session.adminAttempts : session.userAttempts;
  return (
    <div className="welcome">
      <header className="welcome-header">
        <Brand />
        <div className="welcome-meta">
          <span>YOUR EVERYDAY WALLET</span>
          <ThemeToggle />
        </div>
      </header>
      <main className="welcome-main">
        <section className="welcome-story">
          <span className="eyebrow">
            <span className="tiny-dot" /> MONEY, MADE SIMPLE
          </span>
          <h1>
            A little less hassle.
            <br />
            <span>A lot more living.</span>
          </h1>
          <p className="welcome-description">
            Send, save, and stay on top of your money.
            <br />
            Your everyday essentials, together in JCash.
          </p>
          <div className="wallet-art" aria-hidden="true">
            <div className="art-orbit" />
            <div className="art-card">
              <div className="art-card-top">
                <Wallet size={24} />
                <strong>JCash.</strong>
                <span>PERSONAL</span>
              </div>
              <small>Your next chapter starts here</small>
              <div className="art-phrase">
                Make room
                <br />
                for what matters.
              </div>
              <div className="art-card-bottom">
                <span>EVERYDAY POSSIBILITIES</span>
                <ArrowUpRight size={30} />
              </div>
            </div>
            <div className="art-note">
              <span className="art-check">
                <Check size={17} />
              </span>
              <div>
                <strong>More clarity. Less worry.</strong>
                <small>Every transaction, in one place.</small>
              </div>
            </div>
          </div>
          <div className="welcome-benefits">
            <span>
              <Check size={16} /> Easy transfers
            </span>
            <span>
              <Check size={16} /> Clear transaction history
            </span>
          </div>
        </section>
        <section className="auth-card">
          <span className="eyebrow">LET’S GET YOU STARTED</span>
          <h2>
            {mode === "register" ? "Your wallet awaits." : "Welcome back."}
          </h2>
          <p>
            {mode === "register"
              ? "Create an account and make yourself at home."
              : "Sign in to pick up where you left off."}
          </p>
          {mode !== "register" && (
            <div className="role-tabs" aria-label="Account role">
              <button
                className={mode === "user" ? "selected" : ""}
                onClick={() => {
                  setMode("user");
                  setError("");
                }}
                disabled={busy}
              >
                Personal account
              </button>
              <button
                className={mode === "admin" ? "selected" : ""}
                onClick={() => {
                  setMode("admin");
                  setError("");
                }}
                disabled={busy}
              >
                Administrator
              </button>
            </div>
          )}
          {(error || connectionError) && (
            <p className="notice error" role="alert">
              {error || connectionError}{" "}
              {connectionError && (
                <button className="text-button" onClick={() => void refresh()}>
                  Retry
                </button>
              )}
            </p>
          )}
          {success && (
            <p className="notice success" role="status">
              {success}
            </p>
          )}
          <form key={mode} onSubmit={submit}>
            <fieldset disabled={busy || loading}>
              {mode === "register" ? (
                <AccountFields />
              ) : (
                <>
                  <Field
                    label={
                      mode === "admin" ? "Admin username" : "Mobile number"
                    }
                    name="identity"
                    autoComplete="username"
                    required
                    placeholder={
                      mode === "admin" ? "Enter your username" : "09XXXXXXXXX"
                    }
                    maxLength={mode === "admin" ? 30 : 11}
                    inputMode={mode === "admin" ? "text" : "tel"}
                  />
                  <PinField />
                </>
              )}
              {mode !== "register" && (
                <p className="input-hint">
                  {remaining === 0
                    ? "This role is locked for the current session."
                    : `${remaining} sign-in attempts available this session.`}
                </p>
              )}
              <button
                className="button primary full-width"
                type="submit"
                disabled={
                  busy || loading || (mode !== "register" && remaining === 0)
                }
              >
                {busy
                  ? "Please wait…"
                  : mode === "register"
                    ? "Create account"
                    : "Sign in"}
                <ArrowRight size={18} />
              </button>
            </fieldset>
          </form>
          <div className="auth-switch">
            {mode === "register" ? "Already have an account?" : "New to JCash?"}{" "}
            <button
              className="text-button"
              disabled={busy}
              onClick={() => {
                setMode(mode === "register" ? "user" : "register");
                setError("");
                setSuccess("");
              }}
            >
              {mode === "register" ? "Sign in" : "Create an account"}
            </button>
          </div>
          <div className="auth-footnote">
            <ShieldCheck size={15} />
            <span>
              {connected
                ? "Connected and ready for your day"
                : "Start the Java API and MySQL to connect"}
            </span>
          </div>
        </section>
      </main>
      <footer className="welcome-footer">
        <span>JCash · Made for your everyday.</span>
        <span>Simple. Thoughtful. Yours.</span>
      </footer>
    </div>
  );
}
