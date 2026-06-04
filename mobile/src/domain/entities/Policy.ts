export type Policy = {
  policyId: string;
  predicate: string;
  phase: string;
  updateTiming: string;
  action: string;
  effect: string;
  variant: string;
  status: string;
  source: string;
  version: string;
};

export type PapPolicy = {
  policyId: string;
  predicate?: string;
  phase?: string;
  uconVariant?: string;
  status?: string;
  source?: string;
  version?: string;
};

export type PapSummary = Record<string, number>;
