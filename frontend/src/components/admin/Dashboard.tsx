"use client";

import {
  useCallback,
  useEffect,
  useRef,
  useState,
  type FormEvent,
} from "react";
import {
  ArrowRight,
  ArrowUpRight,
  ArrowDownLeft,
  ChartNoAxesCombined,
  Plus,
  RefreshCw,
  Search,
  Users,
  Wallet,
} from "lucide-react";
import {
  api,
  ApiError,
  errorText,
  money,
  typeLabel,
  type AdminDashboard,
  type Receipt,
  type User,
} from "@/lib/api";
import { useSession } from "@/components/shared/SessionProvider";
import { AppShell } from "@/components/shared/AppShell";
import {
  AccountFields,
  Field,
  formValues,
  Modal,
} from "@/components/shared/Forms";
import { TransactionTable } from "@/components/shared/TransactionTable";

export default function Dashboard() {
  const { session, refresh } = useSession();
  const [data, setData] = useState<AdminDashboard | null>(null);
  const [tab, setTab] = useState("overview");
  const [query, setQuery] = useState("");
  const [action, setAction] = useState<"create" | "credit" | "debit" | null>(
    null,
  );
  const [selected, setSelected] = useState<User | null>(null);
  const [busy, setBusy] = useState(false);
  const submitting = useRef(false);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState("");
  const [formError, setFormError] = useState("");
  const [success, setSuccess] = useState("");
  const load = useCallback(async () => {
    setRefreshing(true);
    try {
      setData(await api<AdminDashboard>("admin/dashboard"));
      setError("");
    } catch (error) {
      setError(errorText(error));
      if (error instanceof ApiError && error.status === 401) await refresh();
    } finally {
      setRefreshing(false);
    }
  }, [refresh]);
  useEffect(() => {
    if (session.role === "admin") void load();
  }, [session.role, load]);
  function open(next: "create" | "credit" | "debit", user: User | null = null) {
    setAction(next);
    setSelected(user);
    setFormError("");
  }
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting.current || !action) return;
    submitting.current = true;
    setBusy(true);
    setFormError("");
    try {
      const values = formValues(event);
      if (action === "create") {
        await api("admin/accounts", values);
        setSuccess(
          `Account created for ${values.fullName} with a zero balance.`,
        );
      } else {
        const result = await api<Receipt>(`admin/${action}`, {
          ...values,
          mobileNumber: selected!.mobileNumber,
        });
        setSuccess(
          `${result.message} ${result.user.fullName}: ${money(result.previousBalance)} → ${money(result.user.balance)}.`,
        );
      }
      setAction(null);
      await load();
    } catch (error) {
      setFormError(errorText(error));
      if (error instanceof ApiError && error.status === 401) await refresh();
    } finally {
      submitting.current = false;
      setBusy(false);
    }
  }
  const users =
    data?.users.filter((user) =>
      `${user.fullName} ${user.mobileNumber}`
        .toLowerCase()
        .includes(query.toLowerCase()),
    ) ?? [];
  const totalCount = data
    ? Object.values(data.totals).reduce((sum, total) => sum + total.count, 0)
    : 0;
  return (
    <AppShell role="admin" tab={tab} onTab={setTab} name={session.identity}>
      <div className="page-heading">
        <div>
          <span className="eyebrow">A CLEAR VIEW OF THE BIG PICTURE</span>
          <h1>
            {tab === "accounts"
              ? "Customer accounts"
              : tab === "reports"
                ? "System reports"
                : "Welcome back, admin."}
          </h1>
          <p>
            {tab === "accounts"
              ? "A helping hand for every account."
              : tab === "reports"
                ? "Understand the activity across JCash."
                : "Here’s what’s happening across JCash today."}
          </p>
        </div>
        <div className="heading-actions">
          <button
            className="button secondary"
            onClick={() => void load()}
            disabled={refreshing}
          >
            <RefreshCw size={16} className={refreshing ? "spin" : ""} />
            Refresh
          </button>
          <button className="button primary" onClick={() => open("create")}>
            <Plus size={17} />
            New account
          </button>
        </div>
      </div>
      {error && (
        <p className="notice error" role="alert">
          {error}
        </p>
      )}
      {success && (
        <div className="notice success" role="status">
          {success}
          <button className="text-button" onClick={() => setSuccess("")}>
            Dismiss
          </button>
        </div>
      )}
      {!data ? (
        <div className="loading-card">
          {error
            ? "Dashboard unavailable. Restore the connection and refresh."
            : "Loading your workspace…"}
        </div>
      ) : (
        <>
          {tab !== "accounts" && (
            <div className="metric-grid">
              <section className="metric-card">
                <span className="metric-icon">
                  <Users size={21} />
                </span>
                <span>Total customers</span>
                <strong>{data.userCount}</strong>
                <small>Registered JCash accounts</small>
              </section>
              <section className="metric-card featured">
                <span className="metric-icon">
                  <Wallet size={21} />
                </span>
                <span>Combined wallet balance</span>
                <strong>{money(data.combinedBalance)}</strong>
                <small>Across all customer accounts</small>
              </section>
              <section className="metric-card">
                <span className="metric-icon">
                  <ChartNoAxesCombined size={21} />
                </span>
                <span>Total transactions</span>
                <strong>{totalCount}</strong>
                <small>All completed money movements</small>
              </section>
            </div>
          )}
          {tab === "accounts" && (
            <section className="panel">
              <div className="panel-heading">
                <div>
                  <h2>All accounts</h2>
                  <p>
                    Find a customer to view their balance or make an adjustment.
                  </p>
                </div>
                <span className="count-badge">{data.userCount} customers</span>
              </div>
              <div className="table-toolbar">
                <label className="search-field">
                  <Search size={17} />
                  <input
                    aria-label="Search accounts"
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    placeholder="Search name or mobile number…"
                  />
                </label>
              </div>
              {users.length ? (
                <div className="table-scroll">
                  <table>
                    <thead>
                      <tr>
                        <th>Customer</th>
                        <th>Mobile number</th>
                        <th>Balance</th>
                        <th className="align-right">Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {users.map((user) => (
                        <tr key={user.mobileNumber}>
                          <td>
                            <div className="transaction-cell">
                              <span className="avatar">{user.fullName[0]}</span>
                              <strong>{user.fullName}</strong>
                            </div>
                          </td>
                          <td className="mono">{user.mobileNumber}</td>
                          <td className="amount">{money(user.balance)}</td>
                          <td>
                            <div className="row-actions">
                              <button
                                className="button small secondary"
                                onClick={() => open("credit", user)}
                              >
                                <Plus size={14} />
                                Credit
                              </button>
                              <button
                                className="button small secondary"
                                onClick={() => open("debit", user)}
                              >
                                <ArrowDownLeft size={14} />
                                Debit
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <div className="empty-state">
                  <Users size={30} />
                  <h3>No accounts found</h3>
                  <p>Try another search or create an account.</p>
                </div>
              )}
              <div className="table-footer">
                {users.length} matching accounts
              </div>
            </section>
          )}
          {tab === "overview" && (
            <div className="insight-strip">
              <span className="insight-icon">
                <Users size={22} />
              </span>
              <div>
                <strong>Good service starts with a clear view.</strong>
                <p>
                  Look up customers, create accounts, and manage balance
                  adjustments.
                </p>
              </div>
              <button
                className="text-button"
                onClick={() => setTab("accounts")}
              >
                Manage accounts <ArrowUpRight size={17} />
              </button>
            </div>
          )}
          {tab === "reports" && (
            <section className="panel report-panel">
              <div className="panel-heading">
                <div>
                  <h2>Transactions by type</h2>
                  <p>
                    Lifetime totals across all accounts. Transfer amounts are
                    counted once.
                  </p>
                </div>
              </div>
              <div className="table-scroll">
                <table>
                  <thead>
                    <tr>
                      <th>Transaction type</th>
                      <th>Count</th>
                      <th className="align-right">Total amount</th>
                    </tr>
                  </thead>
                  <tbody>
                    {[
                      "CASH_IN",
                      "WITHDRAWAL",
                      "TRANSFER",
                      "ADMIN_CREDIT",
                      "ADMIN_DEBIT",
                    ].map((type) => (
                      <tr key={type}>
                        <td>
                          <strong>{typeLabel(type)}</strong>
                        </td>
                        <td>{data.totals[type]?.count ?? 0}</td>
                        <td className="align-right amount">
                          {money(data.totals[type]?.amount ?? "0")}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </section>
          )}
          {tab !== "accounts" && (
            <section className="panel">
              <div className="panel-heading">
                <div>
                  <h2>
                    {tab === "overview"
                      ? "Latest activity"
                      : "Recent transactions"}
                  </h2>
                  <p>The latest transactions across the JCash community.</p>
                </div>
                {tab === "overview" && (
                  <button
                    className="text-button"
                    onClick={() => setTab("reports")}
                  >
                    View reports <ArrowRight size={16} />
                  </button>
                )}
              </div>
              <TransactionTable
                transactions={data.transactions}
                compact={tab === "overview"}
                admin
              />
            </section>
          )}
        </>
      )}
      {action && (
        <Modal
          title={
            action === "create"
              ? "Create a customer account"
              : `${action === "credit" ? "Credit" : "Debit"} account`
          }
          onClose={() => setAction(null)}
          busy={busy}
        >
          <p className="modal-description">
            {action === "create"
              ? "New accounts start with a PHP 0.00 balance."
              : `${selected?.fullName} · ${selected?.mobileNumber}`}
          </p>
          <form onSubmit={submit}>
            <fieldset disabled={busy}>
              {action === "create" ? (
                <AccountFields />
              ) : (
                <>
                  <Field
                    label="Amount (PHP)"
                    name="amount"
                    inputMode="decimal"
                    pattern="[0-9]+(\.[0-9]{1,2})?"
                    maxLength={16}
                    placeholder="0.00"
                    required
                  />
                  <p className="input-hint">
                    Current balance: {money(selected!.balance)}
                  </p>
                </>
              )}
              {formError && (
                <p className="notice error" role="alert">
                  {formError}
                </p>
              )}
              <button className="button primary full-width" type="submit">
                {busy
                  ? "Processing…"
                  : action === "create"
                    ? "Create account"
                    : "Confirm adjustment"}
                <ArrowRight size={17} />
              </button>
            </fieldset>
          </form>
        </Modal>
      )}
    </AppShell>
  );
}
