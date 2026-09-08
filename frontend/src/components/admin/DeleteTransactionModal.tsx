"use client";

import { useRef, useState, type FormEvent } from "react";
import { Trash2 } from "lucide-react";
import {
  api,
  ApiError,
  dateLabel,
  errorText,
  money,
  typeLabel,
  type Transaction,
} from "@/lib/api";
import { Field, Modal } from "@/components/shared/Forms";
import { useSession } from "@/components/shared/SessionProvider";
import styles from "./DeleteTransactionModal.module.css";

export function DeleteTransactionModal({
  transaction,
  onClose,
  onDeleted,
}: {
  transaction: Transaction;
  onClose: () => void;
  onDeleted: (message: string) => Promise<void>;
}) {
  const { refresh } = useSession();
  const [confirmation, setConfirmation] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [missing, setMissing] = useState(false);
  const submitting = useRef(false);
  const expected = `DELETE ${transaction.id}`;

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting.current || missing || confirmation !== expected) return;
    submitting.current = true;
    setBusy(true);
    setError("");
    try {
      const result = await api<{ deletedId: number; message: string }>(
        "admin/transactions/delete",
        {
          transactionId: String(transaction.id),
          confirmation,
        },
      );
      await onDeleted(result.message);
    } catch (error) {
      setError(errorText(error));
      if (error instanceof ApiError && error.status === 404) setMissing(true);
      if (error instanceof ApiError && error.status === 401) await refresh();
    } finally {
      submitting.current = false;
      setBusy(false);
    }
  }

  return (
    <Modal title="Delete transaction log?" onClose={onClose} busy={busy}>
      <p className="modal-description">
        Review record #{transaction.id} before deleting it.
      </p>
      <dl className={styles.record}>
        <div>
          <dt>Transaction</dt>
          <dd>{typeLabel(transaction.type)}</dd>
        </div>
        <div>
          <dt>Amount</dt>
          <dd>{money(transaction.amount)}</dd>
        </div>
        <div>
          <dt>Date &amp; time</dt>
          <dd>{dateLabel(transaction.dateTime)}</dd>
        </div>
        {transaction.sender && (
          <div>
            <dt>From</dt>
            <dd>{transaction.sender}</dd>
          </div>
        )}
        {transaction.receiver && (
          <div>
            <dt>To</dt>
            <dd>{transaction.receiver}</dd>
          </div>
        )}
        {transaction.admin && (
          <div>
            <dt>Administrator</dt>
            <dd>{transaction.admin}</dd>
          </div>
        )}
        <div>
          <dt>Details</dt>
          <dd>{transaction.details}</dd>
        </div>
      </dl>
      <div className={styles.warning}>
        <strong>Permanent deletion · Test data only</strong>
        <p>
          This removes the log from customer history and report totals. It does
          not undo the transaction or change any wallet balance.
        </p>
      
      </div>
      <form onSubmit={submit}>
        <fieldset disabled={busy || missing}>
          <Field
            label={`Type ${expected} to confirm`}
            name="confirmation"
            value={confirmation}
            onChange={(event) => setConfirmation(event.target.value)}
            autoComplete="off"
            spellCheck={false}
            required
          />
        </fieldset>
        {error && (
          <p className="notice error" role="alert">
            {error}
          </p>
        )}
        <div className={styles.actions}>
          <button
            type="button"
            className="button secondary"
            onClick={onClose}
            disabled={busy}
          >
            Cancel
          </button>
          <button
            type="submit"
            className={`button ${styles.deleteButton}`}
            disabled={busy || missing || confirmation !== expected}
          >
            <Trash2 size={16} /> {busy ? "Deleting…" : "Delete permanently"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
