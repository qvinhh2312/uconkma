export type AccountRole = "ADMIN" | "STUDENT";

export type AuthSession = {
  token: string;
  username: string;
  displayName: string;
  role: AccountRole;
  studentId?: string | null;
};

export type LoginCommand = {
  username: string;
  password: string;
};
