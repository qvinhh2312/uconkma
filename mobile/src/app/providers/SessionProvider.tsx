import { createContext, ReactNode, useContext, useEffect, useMemo, useState } from "react";
import { AuthSession, LoginCommand } from "@domain/entities/AuthSession";
import { dependencies } from "@app/di";

type SessionContextValue = {
  session: AuthSession | null;
  initializing: boolean;
  login(command: LoginCommand): Promise<void>;
  logout(): Promise<void>;
};

const SessionContext = createContext<SessionContextValue | undefined>(undefined);

export function SessionProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(null);
  const [initializing, setInitializing] = useState(true);

  useEffect(() => {
    dependencies.auth
      .getStoredSession()
      .then(setSession)
      .finally(() => setInitializing(false));
  }, []);

  const value = useMemo<SessionContextValue>(
    () => ({
      session,
      initializing,
      async login(command) {
        const nextSession = await dependencies.auth.login(command);
        setSession(nextSession);
      },
      async logout() {
        await dependencies.auth.logout();
        setSession(null);
      },
    }),
    [initializing, session],
  );

  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>;
}

export function useSession() {
  const value = useContext(SessionContext);
  if (!value) {
    throw new Error("useSession must be used inside SessionProvider");
  }
  return value;
}
