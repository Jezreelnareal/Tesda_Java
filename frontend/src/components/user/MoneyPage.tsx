"use client";

import { useState, type FormEvent } from "react";
import { ArrowDownLeft, ArrowRight, Plus, Send, Wallet } from "lucide-react";
import { money, type UserDashboard } from "@/lib/api";
import { Field } from "@/components/shared/Forms";
import { TransactionTable } from "@/components/shared/TransactionTable";
import styles from "./MoneyPage.module.css";

export type Operation = "cash-in" | "transfer" | "withdraw";
export const operationContent = {
  "cash-in": {
    title: "Cash in",
    description:
      "Add funds to your personal wallet and see your updated balance.",
    formTitle: "Add to your wallet",
    icon: Plus,
    type: "CASH_IN",
    amountLabel: "Amount to add",
    balanceLabel: "Balance after cash in",
    helpTitle: "How cash in works",
    historyTitle: "Recent cash-ins",
    steps: [
      "Choose an amount or enter your own in Philippine pesos.",
      "Review the amount and your estimated new balance.",
      "Confirm to credit your JCash wallet and save a transaction record.",
    ],
    note: "This is a JCash simulation. Cash in adds funds to your simulated wallet; it does not charge a bank account or card.",
  },
  transfer: {
    title: "Send money",
    description: "Transfer funds to another registered JCash wallet.",
    formTitle: "Who are you sending to?",
    icon: Send,
    type: "TRANSFER",
    amountLabel: "Amount to send",
    balanceLabel: "Balance after sending",
    helpTitle: "Before you send",
    historyTitle: "Recent transfers",
    steps: [
      "Enter the recipient’s registered 11-digit mobile number.",
      "Double-check the number and amount before confirming.",
      "A successful transfer updates both wallets and appears in your activity.",
    ],
    note: "The recipient must have a JCash account. The server checks the recipient and your available balance when you confirm.",
  },
  withdraw: {
    title: "Withdraw",
    description: "Withdraw funds from your available JCash balance.",
    formTitle: "Choose your withdrawal amount",
    icon: ArrowDownLeft,
    type: "WITHDRAWAL",
    amountLabel: "Amount to withdraw",
    balanceLabel: "Balance after withdrawal",
    helpTitle: "Before you withdraw",
    historyTitle: "Recent withdrawals",
    steps: [
      "Enter an amount within your available wallet balance.",
      "Review how much will remain in your wallet.",
      "Confirm to deduct the amount and save your withdrawal record.",
    ],
    note: "This is a simulated withdrawal. It updates your JCash wallet; it does not dispense cash or send funds to an external bank.",
  },
};

function toCents(value: string) {
  const [whole, fraction = ""] = value.split(".");
  return BigInt(whole) * 100n + BigInt(fraction.padEnd(2, "0"));
}
function formatCents(value: bigint) {
  const absolute = value < 0n ? -value : value;
  return money(
    `${value < 0n ? "-" : ""}${absolute / 100n}.${String(absolute % 100n).padStart(2, "0")}`,
  );
}

export function MoneyPage({
  operation,
  data,
  busy,
  error,
  onSubmit,
  onActivity,
}: {
  operation: Operation;
  data: UserDashboard;
  busy: boolean;
  error: string;
  onSubmit: (event: FormEvent<HTMLFormElement>) => Promise<boolean>;
  onActivity: () => void;
}) {
  const [amount, setAmount] = useState("");
  const [receiver, setReceiver] = useState("");
  const content = operationContent[operation];
  const validAmount =
    /^\d{1,13}(\.\d{1,2})?$/.test(amount) && toCents(amount) > 0n;
  const balance = toCents(data.user.balance);
  const entered = validAmount ? toCents(amount) : 0n;
  const after = balance + (operation === "cash-in" ? entered : -entered);
  const exceedsBalance = validAmount && after < 0n;

  async function submit(event: FormEvent<HTMLFormElement>) {
    if (await onSubmit(event)) {
      setAmount("");
      setReceiver("");
    }
  }

  return (
    <div className={styles.page}>
      <div className={styles.layout}>
        <section className={`panel ${styles.formPanel}`}>
          <div className={styles.formHeading}>
            <span className={styles.icon}>
              <content.icon size={23} />
            </span>
            <div>
              <h2>{content.formTitle}</h2>
              <p>All amounts are in PHP.</p>
            </div>
          </div>
          <form onSubmit={submit} aria-label={`${content.title} form`}>
            <fieldset disabled={busy}>
              {operation === "transfer" && (
                <>
                  <Field
                    label="Recipient mobile number"
                    name="receiver"
                    required
                    inputMode="tel"
                    pattern="09[0-9]{9}"
                    maxLength={11}
                    placeholder="09XXXXXXXXX"
                    value={receiver}
                    onChange={(event) => setReceiver(event.target.value)}
                    aria-describedby="recipient-help"
                  />
                  <p id="recipient-help" className={styles.fieldHint}>
                    Use the mobile number registered to your recipient’s JCash
                    account.
                  </p>
                </>
              )}
              <Field
                label="Amount (PHP)"
                name="amount"
                required
                inputMode="decimal"
                pattern="[0-9]{1,13}(\.[0-9]{1,2})?"
                maxLength={16}
                placeholder="0.00"
                value={amount}
                onChange={(event) => setAmount(event.target.value)}
                aria-describedby="amount-help"
              />
              <p id="amount-help" className={styles.fieldHint}>
                Enter an amount greater than zero, with up to two decimal
                places.
              </p>
              <div
                className={styles.presets}
                role="group"
                aria-label="Suggested amounts"
              >
                {[100, 500, 1000].map((value) => (
                  <button
                    type="button"
                    className="button secondary small"
                    key={value}
                    onClick={() => setAmount(String(value))}
                    aria-pressed={amount === String(value)}
                  >
                    {money(String(value))}
                  </button>
                ))}
              </div>
              <div
                className={styles.summary}
                aria-live="polite"
                aria-atomic="true"
              >
                <h3>Transaction summary</h3>
                {operation === "transfer" && (
                  <div>
                    <span>To JCash mobile</span>
                    <strong>{receiver || "Enter a recipient"}</strong>
                  </div>
                )}
                <div>
                  <span>{content.amountLabel}</span>
                  <strong>{validAmount ? formatCents(entered) : "—"}</strong>
                </div>
                <div>
                  <span>{content.balanceLabel}</span>
                  <strong>{validAmount ? formatCents(after) : "—"}</strong>
                </div>
                {exceedsBalance ? (
                  <p className={styles.warning}>
                    Insufficient balance. Choose an amount within your available
                    funds.
                  </p>
                ) : (
                  <p>
                    Preview only. Your balance changes after a successful
                    confirmation.
                  </p>
                )}
              </div>
              {error && (
                <p className="notice error" role="alert">
                  {error}
                </p>
              )}
              <button type="submit" className="button primary full-width">
                {busy
                  ? "Processing…"
                  : `Confirm ${content.title.toLowerCase()}`}
                <ArrowRight size={17} />
              </button>
            </fieldset>
          </form>
        </section>
        <aside className={styles.side}>
          <section className={styles.balance} aria-label="Your wallet details">
            <span>
              <Wallet size={19} /> Your available balance
            </span>
            <strong className="balance-value">
              {money(data.user.balance)}
            </strong>
            <div>
              <span>{data.user.fullName}</span>
              <span>{data.user.mobileNumber}</span>
            </div>
          </section>
          <section className={`panel ${styles.guide}`}>
            <h2>{content.helpTitle}</h2>
            <ol>
              {content.steps.map((step) => (
                <li key={step}>{step}</li>
              ))}
            </ol>
            <p>{content.note}</p>
          </section>
        </aside>
      </div>
      <section className="panel">
        <div className="panel-heading">
          <div>
            <h2>{content.historyTitle}</h2>
            <p>
              Your five latest completed{" "}
              {operation === "cash-in"
                ? "cash-ins"
                : operation === "withdraw"
                  ? "withdrawals"
                  : "sent and received transfers"}
              .
            </p>
          </div>
          <button className="text-button" onClick={onActivity}>
            View activity <ArrowRight size={16} />
          </button>
        </div>
        <TransactionTable
          key={operation}
          transactions={data.transactions.filter(
            (transaction) => transaction.type === content.type,
          )}
          compact
        />
      </section>
    </div>
  );
}
