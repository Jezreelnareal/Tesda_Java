"use client";

import {
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
  type ReactNode,
} from "react";
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
  PanelLeftClose,
  PanelLeftOpen,
  Plus,
  Send,
  ArrowDownLeft,
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
  useLayoutEffect(() => {
    let value = document.documentElement.dataset.theme === "dark";
    try {
      value = localStorage.getItem("jcash-theme") === "dark";
    } catch {
      // Keep the current theme when browser storage is unavailable.
    }
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
        try {
          localStorage.setItem("jcash-theme", next ? "dark" : "light");
        } catch {
          // The toggle still works when the preference cannot be saved.
        }
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
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [mobile, setMobile] = useState(false);
  const sidebarRef = useRef<HTMLElement>(null);
  const toggleRef = useRef<HTMLButtonElement>(null);
  const drawerOpen = mobile && !sidebarCollapsed;
  useEffect(() => {
    const media = window.matchMedia("(max-width: 850px)");
    const update = () => {
      setMobile(media.matches);
      if (media.matches) setSidebarCollapsed(true);
    };
    update();
    try {
      setSidebarCollapsed(
        media.matches ||
          localStorage.getItem("jcash-sidebar-hidden") === "true",
      );
    } catch {
      // Navigation still works when browser storage is unavailable.
    }
    media.addEventListener("change", update);
    return () => media.removeEventListener("change", update);
  }, []);
  useEffect(() => {
    if (!drawerOpen) return;
    const previous = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    toggleRef.current?.focus();
    return () => {
      document.body.style.overflow = previous;
    };
  }, [drawerOpen]);
  function closeDrawer() {
    setSidebarCollapsed(true);
    toggleRef.current?.focus();
  }
  function toggleSidebar() {
    const next = !sidebarCollapsed;
    setSidebarCollapsed(next);
    try {
      localStorage.setItem("jcash-sidebar-hidden", String(next));
    } catch {
      // Keep the preference for this page even if it cannot be saved.
    }
  }
  useEffect(() => {
    if (!loading && session.role !== role)
      router.replace(session.role === "guest" ? "/" : `/${session.role}`);
  }, [loading, session.role, role, router]);
  const tabs =
    role === "user"
      ? [
          { id: "overview", label: "Overview", icon: LayoutDashboard },
          { id: "cash-in", label: "Cash in", icon: Plus },
          { id: "transfer", label: "Send money", icon: Send },
          { id: "withdraw", label: "Withdraw", icon: ArrowDownLeft },
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
    <div className={`app-shell${sidebarCollapsed ? " sidebar-collapsed" : ""}`}>
      {drawerOpen && (
        <button
          className="sidebar-backdrop"
          tabIndex={-1}
          aria-label="Close navigation"
          onClick={closeDrawer}
        />
      )}
      <aside
        ref={sidebarRef}
        className="sidebar"
        id="navigation-panel"
        role={drawerOpen ? "dialog" : undefined}
        aria-modal={drawerOpen ? true : undefined}
        aria-label={drawerOpen ? "Navigation" : undefined}
        onKeyDown={(event) => {
          if (!drawerOpen) return;
          if (event.key === "Escape") {
            event.preventDefault();
            closeDrawer();
          }
          if (event.key === "Tab") {
            const buttons =
              sidebarRef.current?.querySelectorAll<HTMLButtonElement>(
                "button:not(:disabled)",
              );
            if (!buttons?.length) return;
            const first = buttons[0],
              last = buttons[buttons.length - 1];
            if (event.shiftKey && document.activeElement === first) {
              event.preventDefault();
              last.focus();
            } else if (!event.shiftKey && document.activeElement === last) {
              event.preventDefault();
              first.focus();
            }
          }
        }}
      >
        <button
          ref={toggleRef}
          type="button"
          className="icon-button navigation-toggle sidebar-panel-toggle"
          aria-label={
            sidebarCollapsed
              ? "Expand navigation panel"
              : "Collapse navigation panel"
          }
          title={
            sidebarCollapsed
              ? "Expand navigation panel"
              : "Collapse navigation panel"
          }
          aria-controls="navigation-panel"
          aria-expanded={!sidebarCollapsed}
          onClick={toggleSidebar}
        >
          {sidebarCollapsed ? (
            <PanelLeftOpen size={19} />
          ) : (
            <PanelLeftClose size={19} />
          )}
        </button>
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
              title={item.label}
              aria-current={tab === item.id ? "page" : undefined}
              onClick={() => {
                onTab(item.id);
                if (mobile) closeDrawer();
              }}
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
            title="Sign out"
            onClick={() => void logout()}
            disabled={busy}
          >
            <LogOut size={19} />
            <span>{busy ? "Signing out…" : "Sign out"}</span>
          </button>
        </div>
      </aside>
      <div className="workspace" inert={drawerOpen}>
        <header className="topbar">
          <div className="topbar-start">
            <span className="breadcrumb">
              {role === "admin" ? "Administration" : "Personal account"}
              <span>/</span>
              <strong>{tabs.find((t) => t.id === tab)?.label}</strong>
            </span>
          </div>
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
