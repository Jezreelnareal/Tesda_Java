"use client";

import { useState } from "react";
import {
  ArrowDownLeft,
  ArrowUpRight,
  Search,
  ArrowLeftRight,
  ReceiptText,
} from "lucide-react";
import { dateLabel, money, typeLabel, type Transaction } from "@/lib/api";

export function TransactionTable({
  transactions,
  compact = false,
  admin = false,
}: {
  transactions: Transaction[];
  compact?: boolean;
  admin?: boolean;
}) {
  const [query, setQuery] = useState("");
  const [filter, setFilter] = useState("ALL");
  const filtered = transactions.filter(
    (t) =>
      (filter === "ALL" || t.type === filter) &&
      `${t.details} ${t.sender ?? ""} ${t.receiver ?? ""} ${typeLabel(t.type)}`
        .toLowerCase()
        .includes(query.toLowerCase()),
  );
  const visible = compact ? filtered.slice(0, 5) : filtered;
  return (
    <>
      {!compact && (
        <div className="table-toolbar">
          <label className="search-field">
            <Search size={17} />
            <input
              aria-label="Search transactions"
              placeholder="Search transactions…"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
            />
          </label>
          <select
            aria-label="Transaction type"
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
          >
            <option value="ALL">All transactions</option>
            {[
              "CASH_IN",
              "WITHDRAWAL",
              "TRANSFER",
              "ADMIN_CREDIT",
              "ADMIN_DEBIT",
            ].map((type) => (
              <option key={type} value={type}>
                {typeLabel(type)}
              </option>
            ))}
          </select>
        </div>
      )}
      {visible.length === 0 ? (
        <div className="empty-state">
          <ReceiptText size={30} />
          <h3>
            {transactions.length
              ? "No matching transactions"
              : "Your story starts here"}
          </h3>
          <p>
            {transactions.length
              ? "Try another search or transaction type."
              : "Your completed transactions will appear here."}
          </p>
        </div>
      ) : (
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Transaction</th>
                <th>Date & time</th>
                {admin && <th>Account</th>}
                <th>Status</th>
                <th className="align-right">Amount</th>
              </tr>
            </thead>
            <tbody>
              {visible.map((t) => (
                <tr key={t.id}>
                  <td>
                    <div className="transaction-cell">
                      <span className={`transaction-icon ${t.direction}`}>
                        {t.direction === "in" ? (
                          <ArrowDownLeft size={19} />
                        ) : t.direction === "out" ? (
                          <ArrowUpRight size={19} />
                        ) : (
                          <ArrowLeftRight size={18} />
                        )}
                      </span>
                      <div>
                        <strong>{typeLabel(t.type)}</strong>
                        <small>{t.details}</small>
                      </div>
                    </div>
                  </td>
                  <td className="date-cell">{dateLabel(t.dateTime)}</td>
                  {admin && <td className="mono">{t.sender ?? t.receiver}</td>}
                  <td>
                    <span className="status-badge">Completed</span>
                  </td>
                  <td className={`align-right amount ${t.direction}`}>
                    {t.direction === "in"
                      ? "+"
                      : t.direction === "out"
                        ? "−"
                        : ""}
                    {money(t.amount)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {!compact && (
        <div className="table-footer">
          {filtered.length} transaction{filtered.length !== 1 ? "s" : ""}
          {admin ? " · Most recent 100 records" : ""}
        </div>
      )}
    </>
  );
}
