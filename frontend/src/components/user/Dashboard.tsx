"use client";

import {
  useCallback,
  useEffect,
  useRef,
  useState,
  type FormEvent,
} from "react";
import {
  ArrowDownLeft,
  ArrowRight,
  ArrowUpRight,
  CreditCard,
  Plus,
  RefreshCw,
  Send,
  ShieldCheck,
  Wallet,
} from "lucide-react";
import {
  api,
  ApiError,
  errorText,
  money,
  type Receipt,
  type UserDashboard,
} from "@/lib/api";
import { useSession } from "@/components/shared/SessionProvider";
import { AppShell } from "@/components/shared/AppShell";
import { formValues } from "@/components/shared/Forms";
import { TransactionTable } from "@/components/shared/TransactionTable";
import { MoneyPage, operationContent, type Operation } from "./MoneyPage";

export default function Dashboard() {
  const { session, refresh } = useSession();
  const [data, setData] = useState<UserDashboard | null>(null);
  const [tab, setTab] = useState("overview");
  const operation: Operation | null =
    tab === "cash-in" || tab === "transfer" || tab === "withdraw" ? tab : null;
  const [busy, setBusy] = useState(false);
  const submitting = useRef(false);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState("");
  const [formError, setFormError] = useState("");
  const [receipt, setReceipt] = useState<Receipt | null>(null);
  const load = useCallback(async () => {
    setRefreshing(true);
    try {
      setData(await api<UserDashboard>("user/dashboard"));
      setError("");
    } catch (error) {
      setError(errorText(error));
      if (error instanceof ApiError && error.status === 401) await refresh();
    } finally {
      setRefreshing(false);
    }
  }, [refresh]);
  useEffect(() => {
    if (session.role === "user") void load();
  }, [session.role, load]);
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting.current || !operation) return false;
    submitting.current = true;
    setBusy(true);
    setFormError("");
    try {
      const result = await api<Receipt>(`user/${operation}`, formValues(event));
      setReceipt(result);
      setData((current) =>
        current ? { ...current, user: result.user } : current,
      );
      await load();
      return true;
    } catch (error) {
      setFormError(errorText(error));
      if (error instanceof ApiError && error.status === 401) await refresh();
      return false;
    } finally {
      submitting.current = false;
      setBusy(false);
    }
  }
  function navigate(next: string) {
    if (submitting.current) return;
    setFormError("");
    setTab(next);
  }
  return (
    <AppShell
      role="user"
      tab={tab}
      onTab={navigate}
      name={data?.user.fullName ?? ""}
    >
      <div className="page-heading">
        <div>
          <span className="eyebrow">
            {operation
              ? "YOUR EVERYDAY TRANSACTIONS"
              : "YOUR MONEY, AT A GLANCE"}
          </span>
          <h1>
            {tab === "activity"
              ? "Your activity"
              : operation
                ? operationContent[operation].title
                : `Hello${data ? `, ${data.user.fullName.split(" ")[0]}` : ""}.`}
          </h1>
          <p>
            {tab === "activity"
              ? "Every move, clearly in view."
              : operation
                ? operationContent[operation].description
                : "A fresh look at your everyday finances."}
          </p>
        </div>
        <button
          className="button secondary"
          onClick={() => void load()}
          disabled={refreshing || busy}
        >
          <RefreshCw size={16} className={refreshing ? "spin" : ""} />
          Refresh
        </button>
      </div>
      {error && (
        <p className="notice error" role="alert">
          {error}
        </p>
      )}
      {receipt && (
        <div className="notice success receipt-notice" role="status">
          <ShieldCheck size={22} />
          <div>
            <strong>{receipt.message}</strong>
            <p>
              {money(receipt.amount)} · Balance:{" "}
              {money(receipt.previousBalance)} → {money(receipt.user.balance)}
            </p>
          </div>
          <button className="text-button" onClick={() => setReceipt(null)}>
            Dismiss
          </button>
        </div>
      )}
      {!data ? (
        <div className="loading-card" aria-live="polite">
          {error
            ? "Your wallet will appear once the connection is restored."
            : "Loading your wallet…"}
        </div>
      ) : (
        <>
          {operation && (
            <MoneyPage
              key={operation}
              operation={operation}
              data={data}
              busy={busy}
              error={formError}
              onSubmit={submit}
              onActivity={() => navigate("activity")}
            />
          )}
          {tab === "overview" && (
            <>
              <div className="wallet-grid">
                <section className="balance-card">
                  <div className="balance-top">
                    <span>
                      <Wallet size={19} />
                      Personal wallet
                    </span>
                    <span className="balance-tag">PHP ACCOUNT</span>
                  </div>
                  <div className="balance-label">Available balance</div>
                  <div className="balance-value">
                    {money(data.user.balance)}
                  </div>
                  <div className="balance-bottom">
                    <div>
                      <small>ACCOUNT HOLDER</small>
                      <strong>{data.user.fullName}</strong>
                    </div>
                    <div className="account-number">
                      <small>MOBILE NUMBER</small>
                      <strong>{data.user.mobileNumber}</strong>
                    </div>
                    <CreditCard size={29} />
                  </div>
                </section>
                <section className="quick-card">
                  <span className="section-kicker">LET’S MAKE A MOVE</span>
                  <h2>Everyday essentials</h2>
                  <div className="quick-actions">
                    <button onClick={() => navigate("cash-in")}>
                      <span className="action-icon">
                        <Plus size={23} />
                      </span>
                      <strong>Cash in</strong>
                      <small>Add to your wallet</small>
                      <ArrowUpRight size={18} />
                    </button>
                    <button onClick={() => navigate("transfer")}>
                      <span className="action-icon">
                        <Send size={21} />
                      </span>
                      <strong>Send money</strong>
                      <small>Make someone’s day</small>
                      <ArrowUpRight size={18} />
                    </button>
                    <button onClick={() => navigate("withdraw")}>
                      <span className="action-icon">
                        <ArrowDownLeft size={23} />
                      </span>
                      <strong>Withdraw</strong>
                      <small>Take what you need</small>
                      <ArrowUpRight size={18} />
                    </button>
                  </div>
                </section>
              </div>
              <div className="insight-strip">
                <span className="insight-icon">
                  <ShieldCheck size={22} />
                </span>
                <div>
                  <strong>A clear picture of your money.</strong>
                  <p>
                    Keep track of every cash-in, transfer, and withdrawal in
                    your activity.
                  </p>
                </div>
                <button
                  className="text-button"
                  onClick={() => navigate("activity")}
                >
                  View activity <ArrowRight size={16} />
                </button>
              </div>
            </>
          )}
          {!operation && (
            <section className="panel">
              <div className="panel-heading">
                <div>
                  <h2>
                    {tab === "overview"
                      ? "Recent activity"
                      : "Transaction history"}
                  </h2>
                  <p>
                    {tab === "overview"
                      ? "The latest from your wallet."
                      : "Search and filter your completed transactions."}
                  </p>
                </div>
                {tab === "overview" && (
                  <button
                    className="text-button"
                    onClick={() => navigate("activity")}
                  >
                    View all <ArrowRight size={16} />
                  </button>
                )}
              </div>
              <TransactionTable
                key={tab}
                transactions={data.transactions}
                compact={tab === "overview"}
              />
            </section>
          )}
        </>
      )}
    </AppShell>
  );
}
