import * as SecureStore from "expo-secure-store";

const SESSION_KEY = "uconkma.session";

export type StoredSession = {
  token: string;
  role: "ADMIN" | "STUDENT";
  username: string;
  displayName: string;
  studentId?: string | null;
};

export interface TokenStorage {
  getSession(): Promise<StoredSession | null>;
  saveSession(session: StoredSession): Promise<void>;
  clearSession(): Promise<void>;
}

export class SecureTokenStorage implements TokenStorage {
  async getSession() {
    const raw = await SecureStore.getItemAsync(SESSION_KEY);
    return raw ? (JSON.parse(raw) as StoredSession) : null;
  }

  async saveSession(session: StoredSession) {
    await SecureStore.setItemAsync(SESSION_KEY, JSON.stringify(session));
  }

  async clearSession() {
    await SecureStore.deleteItemAsync(SESSION_KEY);
  }
}
