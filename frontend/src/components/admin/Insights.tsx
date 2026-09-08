"use client";

import { useState } from "react";
import {
  ArrowDownLeft,
  ArrowLeftRight,
  ArrowRight,
  ArrowUpRight,
  ChartNoAxesCombined,
  ReceiptText,
  Users,
  Wallet,
} from "lucide-react";
import { money, typeLabel, type AdminDashboard } from "@/lib/api";
import styles from "./Insights.module.css";

const categories = [
  { type: "CASH_IN", icon: ArrowDownLeft, note: "Customer deposits" },
  { type: "WITHDRAWAL", icon: ArrowUpRight, note: "Customer withdrawals" },
  { type: "TRANSFER", icon: ArrowLeftRight, note: "Between JCash wallets" },
  {
    type: "ADMIN_CREDIT",
    icon: ArrowDownLeft,
    note: "Credits by an administrator",
  },
  {
    type: "ADMIN_DEBIT",
    icon: ArrowUpRight,
    note: "Debits by an administrator",
  },
];

// Keep aggregate arithmetic in centavos, matching the API's decimal strings.
function cents(amount: string) {
  const [whole, fraction = ""] = amount.split(".");
  return BigInt(whole) * 100n + BigInt(fraction.padEnd(2, "0"));
}

function displayAmount(amount: bigint) {
  return money(
    `${amount / 100n}.${(amount % 100n).toString().padStart(2, "0")}`,
  );
}

function summarize(data: AdminDashboard) {
  const rows = categories.map((category) => ({
    ...category,
    count: data.totals[category.type]?.count ?? 0,
    amount: cents(data.totals[category.type]?.amount ?? "0"),
  }));
  const count = rows.reduce((sum, row) => sum + row.count, 0);
  const amount = rows.reduce((sum, row) => sum + row.amount, 0n);
  return { rows, count, amount };
}

function average(amount: bigint, count: number) {
  return count
    ? displayAmount((amount + BigInt(Math.floor(count / 2))) / BigInt(count))
    : money("0");
}

const countLabel = (count: number) => count.toLocaleString("en-PH");

export function OverviewSummary({
  data,
  onAccounts,
  onReports,
}: {
  data: AdminDashboard;
  onAccounts: () => void;
  onReports: () => void;
}) {
  const { rows, count } = summarize(data);
  const incoming = rows.filter((row) =>
    ["CASH_IN", "ADMIN_CREDIT"].includes(row.type),
  );
  const outgoing = rows.filter((row) =>
    ["WITHDRAWAL", "ADMIN_DEBIT"].includes(row.type),
  );
  const transfers = rows.find((row) => row.type === "TRANSFER")!;

  return (
    <div className={styles.content}>
      <div className={styles.overviewMetrics}>
        <section
          className={styles.walletCard}
          aria-label="Combined wallet balance"
        >
          <div className={styles.cardTop}>
            <span>
              <Wallet size={19} /> Customer wallets
            </span>
            <span className={styles.walletBadge}>PHP</span>
          </div>
          <p>Combined wallet balance</p>
          <strong className={styles.walletValue}>
            {money(data.combinedBalance)}
          </strong>
          <div className={styles.walletFoot}>
            <span>Current funds across all customer accounts</span>
            <Wallet size={30} strokeWidth={1.2} aria-hidden="true" />
          </div>
        </section>
        <section className={styles.statCard}>
          <span className={styles.statIcon}>
            <Users size={20} />
          </span>
          <p>Total customers</p>
          <strong>{countLabel(data.userCount)}</strong>
          <button className="text-button" onClick={onAccounts}>
            Manage accounts <ArrowRight size={15} />
          </button>
        </section>
        <section className={styles.statCard}>
          <span className={styles.statIcon}>
            <ReceiptText size={20} />
          </span>
          <p>Completed transactions</p>
          <strong>{countLabel(count)}</strong>
          <span className={styles.caption}>
            Across all time · Transfers counted once
          </span>
        </section>
      </div>

      <div className={styles.overviewDetails}>
        <section className={`panel ${styles.panel}`}>
          <div className="panel-heading">
            <div>
              <h2>Money movement</h2>
              <p>A lifetime view of funds moving through JCash.</p>
            </div>
            <span className="count-badge">All time</span>
          </div>
          <div className={styles.flowGrid}>
            {[
              {
                label: "Money in",
                rows: incoming,
                icon: ArrowDownLeft,
                note: "Cash in + admin credits",
              },
              {
                label: "Money out",
                rows: outgoing,
                icon: ArrowUpRight,
                note: "Withdrawals + admin debits",
              },
              {
                label: "Internal transfers",
                rows: [transfers],
                icon: ArrowLeftRight,
                note: "Moved between customer wallets",
              },
            ].map((flow) => (
              <div className={styles.flowItem} key={flow.label}>
                <span className={styles.flowLabel}>
                  <flow.icon size={17} />
                  {flow.label}
                </span>
                <strong>
                  {displayAmount(
                    flow.rows.reduce((sum, row) => sum + row.amount, 0n),
                  )}
                </strong>
                <small>{flow.note}</small>
              </div>
            ))}
          </div>
          <div className={styles.panelFoot}>
            Internal transfers do not change the combined wallet balance.
          </div>
        </section>

        <section className={`panel ${styles.panel}`}>
          <div className="panel-heading">
            <div>
              <h2>Your workspace</h2>
              <p>The next step, within reach.</p>
            </div>
          </div>
          <div className={styles.shortcuts}>
            <button onClick={onAccounts}>
              <span className={styles.statIcon}>
                <Users size={18} />
              </span>
              <span>
                <strong>Customer accounts</strong>
                <small>Find a customer or adjust a balance</small>
              </span>
              <ArrowUpRight size={17} />
            </button>
            <button onClick={onReports}>
              <span className={styles.statIcon}>
                <ChartNoAxesCombined size={18} />
              </span>
              <span>
                <strong>Explore reports</strong>
                <small>Review totals and transaction records</small>
              </span>
              <ArrowUpRight size={17} />
            </button>
          </div>
        </section>
      </div>
    </div>
  );
}

export function ReportsSummary({ data }: { data: AdminDashboard }) {
  const [measure, setMeasure] = useState<"count" | "amount">("count");
  const { rows, count, amount } = summarize(data);
  const largest = rows.reduce(
    (best, row) => (row.count > best.count ? row : best),
    rows[0],
  );
  const tied = rows.filter((row) => row.count === largest.count).length > 1;
  const share = (row: (typeof rows)[number]) =>
    measure === "count"
      ? count
        ? (row.count / count) * 100
        : 0
      : amount
        ? Number((row.amount * 1000n + amount / 2n) / amount) / 10
        : 0;

  return (
    <div className={styles.content}>
      <div className={styles.reportMetrics}>
        <section className={styles.statCard}>
          <span className={styles.statIcon}>
            <ChartNoAxesCombined size={20} />
          </span>
          <p>Total transaction volume</p>
          <strong>{displayAmount(amount)}</strong>
          <span className={styles.caption}>
            All completed amounts · Transfers counted once
          </span>
        </section>
        <section className={styles.statCard}>
          <span className={styles.statIcon}>
            <ReceiptText size={20} />
          </span>
          <p>Completed transactions</p>
          <strong>{countLabel(count)}</strong>
          <span className={styles.caption}>
            Lifetime count across all five types
          </span>
        </section>
        <section className={styles.statCard}>
          <span className={styles.statIcon}>
            <Wallet size={20} />
          </span>
          <p>Average transaction</p>
          <strong>{average(amount, count)}</strong>
          <span className={styles.caption}>
            Total volume divided by transaction count
          </span>
        </section>
      </div>

      <section className={`panel ${styles.panel}`}>
        <div className="panel-heading">
          <div>
            <h2>Transaction mix</h2>
            <p>
              See how each transaction type contributes to overall activity.
            </p>
          </div>
          <div
            className={styles.segmented}
            role="group"
            aria-label="Chart measure"
          >
            <button
              aria-pressed={measure === "count"}
              onClick={() => setMeasure("count")}
            >
              By count
            </button>
            <button
              aria-pressed={measure === "amount"}
              onClick={() => setMeasure("amount")}
            >
              By amount
            </button>
          </div>
        </div>
        <div className={styles.mixLayout}>
          <div
            className={styles.chart}
            aria-label={`Transaction share by ${measure}`}
          >
            {rows.map((row, index) => (
              <div className={styles.chartRow} key={row.type}>
                <div className={styles.chartLabel}>
                  <span>{typeLabel(row.type)}</span>
                  <strong>{share(row).toFixed(1)}%</strong>
                </div>
                <div className={styles.barTrack} aria-hidden="true">
                  <div
                    className={`${styles.bar} ${styles[`bar${index}`]}`}
                    style={{ width: `${share(row)}%` }}
                  />
                </div>
                <small>
                  {measure === "count"
                    ? `${countLabel(row.count)} transaction${row.count === 1 ? "" : "s"}`
                    : displayAmount(row.amount)}
                </small>
              </div>
            ))}
          </div>
          <aside className={styles.reportNote}>
            <span className={styles.noteKicker}>ACTIVITY AT A GLANCE</span>
            <ChartNoAxesCombined
              size={28}
              strokeWidth={1.5}
              aria-hidden="true"
            />
            <h3>
              {count
                ? typeLabel(largest.type)
                : "Ready for the first transaction"}
            </h3>
            <p>
              {count
                ? `${tied ? "Tied for the most frequent transaction type" : "The most frequent transaction type"}, with ${countLabel(largest.count)} of ${countLabel(count)} completed transactions.`
                : "Once a transaction is completed, its share of activity will appear here."}
            </p>
            <div className={styles.noteDivider} />
            <strong>Reading this report</strong>
            <p>
              Totals cover all time. Volume includes deposits, withdrawals,
              transfers, and admin adjustments; it is not revenue or the current
              wallet balance.
            </p>
          </aside>
        </div>
      </section>

      <section className={`panel report-panel ${styles.breakdown}`}>
        <div className="panel-heading">
          <div>
            <h2>Transactions by type</h2>
            <p>
              Lifetime totals across all accounts. Transfer amounts are counted
              once.
            </p>
          </div>
          <span className="count-badge">All time</span>
        </div>
        <div className="table-scroll">
          <table
            className="responsive-table"
            role="table"
            aria-label="Transactions by type"
          >
            <thead>
              <tr>
                <th scope="col">Transaction type</th>
                <th scope="col" className="align-right">
                  Count
                </th>
                <th scope="col" className="align-right">
                  Average amount
                </th>
                <th scope="col" className="align-right">
                  Total amount
                </th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.type}>
                  <td className="mobile-row-heading">
                    <div className="transaction-cell">
                      <span className={styles.statIcon}>
                        <row.icon size={18} />
                      </span>
                      <div>
                        <strong>{typeLabel(row.type)}</strong>
                        <small>{row.note}</small>
                      </div>
                    </div>
                  </td>
                  <td className="align-right" data-label="Count">
                    {countLabel(row.count)}
                  </td>
                  <td
                    className="align-right amount"
                    data-label="Average amount"
                  >
                    {average(row.amount, row.count)}
                  </td>
                  <td className="align-right amount" data-label="Total amount">
                    {displayAmount(row.amount)}
                  </td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr>
                <th scope="row" className="mobile-row-heading">
                  All transactions
                </th>
                <td className="align-right" data-label="Count">
                  {countLabel(count)}
                </td>
                <td className="align-right amount" data-label="Average amount">
                  {average(amount, count)}
                </td>
                <td className="align-right amount" data-label="Total amount">
                  {displayAmount(amount)}
                </td>
              </tr>
            </tfoot>
          </table>
        </div>
      </section>
    </div>
  );
}
