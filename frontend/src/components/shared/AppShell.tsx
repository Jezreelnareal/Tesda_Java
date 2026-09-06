"use client";

import { useEffect, useState, type ReactNode } from "react";
import { useRouter } from "next/navigation";
import {
  ArrowUpRight,
  LayoutDashboard,
  List,
  LogOut,
  Moon,
  ShieldCheck,
  Sun,
  Users,
  Wallet,
  ChartNoAxesCombined,
} from "lucide-react";
import { api, errorText, type Session } from "@/lib/api";
import { useSession } from "./SessionProvider";

export function Brand() {
  return (
    <span className="brand">
      <span className="brand-mark">
        <Wallet size={23} strokeWidth={2.5} />
      </span>
      JCash<span className="brand-dot">.</span>
    </span>
  );
}
export function ThemeToggle() {
  const [dark, setDark] = useState(false);
  useEffect(() => {
    const value = localStorage.getItem("jcash-theme") === "dark";
    setDark(value);
    document.documentElement.dataset.theme = value ? "dark" : "light";
  }, []);
  return (
    <button
      className="icon-button theme-toggle"
      aria-label={dark ? "Switch to light mode" : "Switch to dark mode"}
      onClick={() => {
        const next = !dark;
        setDark(next);
        document.documentElement.dataset.theme = next ? "dark" : "light";
        localStorage.setItem("jcash-theme", next ? "dark" : "light");
      }}
    >
      {dark ? <Sun size={19} /> : <Moon size={19} />}
    </button>
  );
}
export function AppShell({
  role,
  tab,
  onTab,
  name,
  children,
}: {
  role: "user" | "admin";
  tab: string;
  onTab: (tab: string) => void;
  name: string;
  children: ReactNode;
}) {
  const router = useRouter();
  const { session, loading, setSession } = useSession();
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  useEffect(() => {
    if (!loading && session.role !== role)
      router.replace(session.role === "guest" ? "/" : `/${session.role}`);
  }, [loading, session.role, role, router]);
  const tabs =
    role === "user"
      ? [
          { id: "overview", label: "Overview", icon: LayoutDashboard },
          { id: "activity", label: "Activity", icon: List },
        ]
      : [
          { id: "overview", label: "Overview", icon: LayoutDashboard },
          { id: "accounts", label: "Accounts", icon: Users },
          { id: "reports", label: "Reports", icon: ChartNoAxesCombined },
        ];
  async function logout() {
    setBusy(true);
    try {
      setSession(await api<Session>("logout", {}));
      router.replace("/");
    } catch (error) {
      setError(errorText(error));
    } finally {
      setBusy(false);
    }
  }
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <Brand />
        <div className="workspace-label">
          {role === "admin" ? "ADMIN WORKSPACE" : "YOUR WALLET"}
        </div>
        <nav aria-label="Main navigation">
          {tabs.map((item) => (
            <button
              key={item.id}
              className={`nav-item ${tab === item.id ? "active" : ""}`}
              aria-label={item.label}
              aria-current={tab === item.id ? "page" : undefined}
              onClick={() => onTab(item.id)}
            >
              <item.icon size={19} />
              <span>{item.label}</span>
              {tab === item.id && <span className="nav-dot" />}
            </button>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <div className="sidebar-note">
            <ShieldCheck size={21} />
            <strong>A little more peace of mind.</strong>
            <p>
              Your account. Your money.
              <br />
              All in one place.
            </p>
            <ArrowUpRight size={18} />
          </div>
          <button
            className="nav-item logout"
            aria-label="Sign out"
            onClick={() => void logout()}
            disabled={busy}
          >
            <LogOut size={19} />
            <span>{busy ? "Signing out…" : "Sign out"}</span>
          </button>
        </div>
      </aside>
      <div className="workspace">
        <header className="topbar">
          <span className="breadcrumb">
            {role === "admin" ? "Administration" : "Personal account"}
            <span>/</span>
            <strong>{tabs.find((t) => t.id === tab)?.label}</strong>
          </span>
          <div className="topbar-right">
            <ThemeToggle />
            <span className="topbar-divider" />
            <span className="avatar">
              {(name || "J").slice(0, 1).toUpperCase()}
            </span>
            <div className="topbar-profile">
              <strong>{name || "JCash"}</strong>
              <small>
                {role === "admin" ? "Administrator" : "Personal wallet"}
              </small>
            </div>
          </div>
        </header>
        <main className="main-content">
          {error && (
            <p className="notice error" role="alert">
              {error}
            </p>
          )}
          {children}
        </main>
        <footer className="footer">
          <span>JCash · A simpler way to manage money</span>
          <span>Made for your everyday.</span>
        </footer>
      </div>
    </div>
  );
}
