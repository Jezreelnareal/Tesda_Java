"use client";

import {
  useEffect,
  useRef,
  useState,
  type FormEvent,
  type InputHTMLAttributes,
  type ReactNode,
} from "react";
import { Eye, EyeOff, X } from "lucide-react";

export function Field({
  label,
  ...props
}: InputHTMLAttributes<HTMLInputElement> & { label: string }) {
  return (
    <label className="field">
      <span>{label}</span>
      <input {...props} />
    </label>
  );
}
export function PinField({
  label = "PIN",
  name = "pin",
  confirm = false,
}: {
  label?: string;
  name?: string;
  confirm?: boolean;
}) {
  const [visible, setVisible] = useState(false);
  return (
    <label className="field">
      <span>{label}</span>
      <span className="password-field">
        <input
          name={name}
          type={visible ? "text" : "password"}
          inputMode="numeric"
          pattern="[0-9]{4}"
          maxLength={4}
          minLength={4}
          required
          autoComplete={confirm ? "new-password" : "current-password"}
          placeholder="4-digit PIN"
        />
        <button
          type="button"
          className="icon-button"
          aria-label={visible ? `Hide ${label}` : `Show ${label}`}
          onClick={() => setVisible(!visible)}
        >
          {visible ? <EyeOff size={18} /> : <Eye size={18} />}
        </button>
      </span>
    </label>
  );
}
export function AccountFields() {
  return (
    <>
      <Field
        label="Full name"
        name="fullName"
        autoComplete="name"
        maxLength={100}
        required
        placeholder="Juan Dela Cruz"
      />
      <Field
        label="Mobile number"
        name="mobileNumber"
        inputMode="tel"
        autoComplete="tel"
        pattern="09[0-9]{9}"
        maxLength={11}
        required
        placeholder="09XXXXXXXXX"
      />
      <div className="form-grid">
        <PinField />
        <PinField label="Confirm PIN" name="confirmPin" confirm />
      </div>
    </>
  );
}
export function formValues(
  event: FormEvent<HTMLFormElement>,
): Record<string, string> {
  return Object.fromEntries(
    new FormData(event.currentTarget).entries(),
  ) as Record<string, string>;
}
export function Modal({
  title,
  children,
  onClose,
  busy = false,
}: {
  title: string;
  children: ReactNode;
  onClose: () => void;
  busy?: boolean;
}) {
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    const dialog = ref.current;
    dialog?.showModal();
    return () => dialog?.close();
  }, []);
  return (
    <dialog
      ref={ref}
      className="modal"
      aria-labelledby="modal-title"
      onCancel={(event) => {
        event.preventDefault();
        if (!busy) onClose();
      }}
    >
      <div className="modal-heading">
        <h2 id="modal-title">{title}</h2>
        <button
          type="button"
          className="icon-button"
          onClick={onClose}
          disabled={busy}
          aria-label="Close dialog"
        >
          <X size={20} />
        </button>
      </div>
      {children}
    </dialog>
  );
}
