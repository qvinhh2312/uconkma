import { createContext, ReactNode, useContext, useMemo, useState } from "react";
import { ApiDecisionResponse } from "@domain/entities/Decision";

type DecisionContextValue = {
  latestDecision: ApiDecisionResponse | null;
  setLatestDecision(decision: ApiDecisionResponse): void;
};

const DecisionContext = createContext<DecisionContextValue | undefined>(undefined);

export function DecisionProvider({ children }: { children: ReactNode }) {
  const [latestDecision, setLatestDecision] = useState<ApiDecisionResponse | null>(null);
  const value = useMemo(() => ({ latestDecision, setLatestDecision }), [latestDecision]);
  return <DecisionContext.Provider value={value}>{children}</DecisionContext.Provider>;
}

export function useDecisionHistory() {
  const value = useContext(DecisionContext);
  if (!value) {
    throw new Error("useDecisionHistory must be used inside DecisionProvider");
  }
  return value;
}
