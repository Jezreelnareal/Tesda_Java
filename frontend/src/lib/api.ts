export type Role = "guest" | "user" | "admin";
export type Session = {
  role: Role;
  identity: string;
  userAttempts: number;
  adminAttempts: number;
};
export type User = { fullName: string; mobileNumber: string; balance: string };
export type Transaction = {
  id: number;
  type: string;
  amount: string;
  details: string;
  dateTime: string;
  sender?: string;
  receiver?: string;
  admin?: string;
  direction: "in" | "out" | "neutral";
};
export type UserDashboard = { user: User; transactions: Transaction[] };
export type AdminDashboard = {
  users: User[];
  userCount: number;
  combinedBalance: string;
  totals: Record<string, { count: number; amount: string }>;
  transactions: Transaction[];
};
export type Receipt = {
  user: User;
  amount: string;
  previousBalance: string;
  message: string;
};

export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
  ) {
    super(message);
  }
}

export async function api<T>(
  path: string,
  body?: Record<string, string>,
): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`/api/${path}`, {
      method: body ? "POST" : "GET",
      credentials: "same-origin",
      cache: "no-store",
      headers: body
        ? { "Content-Type": "application/json", "X-JCash-Request": "1" }
        : undefined,
      body: body ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(
      "Connection lost. Check your connection before trying again.",
      0,
    );
  }
  const data = await response.json();
  if (!response.ok)
    throw new ApiError(data.error ?? "Request failed.", response.status);
  return data as T;
}

export const money = (amount: string) =>
  new Intl.NumberFormat("en-PH", {
    style: "currency",
    currency: "PHP",
    minimumFractionDigits: 2,
  }).format(Number(amount));
export const typeLabel = (type: string) =>
  ({
    CASH_IN: "Cash in",
    WITHDRAWAL: "Withdrawal",
    TRANSFER: "Transfer",
    ADMIN_CREDIT: "Admin credit",
    ADMIN_DEBIT: "Admin debit",
  })[type] ?? type;
export const dateLabel = (value: string) =>
  new Intl.DateTimeFormat("en-PH", {
    month: "short",
    day: "numeric",
    year: "numeric",
    hour: "numeric",
    minute: "2-digit",
  }).format(new Date(value));
export const errorText = (error: unknown) =>
  error instanceof Error ? error.message : "Something went wrong.";
