export type DecisionTracePolicy = {
  policyId?: string;
  predicate?: string;
  phase?: string;
  updateTiming?: string;
  uconVariant?: string;
  source?: string;
  version?: string;
  policyStatus?: string;
  result?: string;
  conditionResult?: boolean;
  denyReason?: string;
};

export type DecisionTracePhase = {
  phase?: string;
  predicate?: string;
  decision?: string;
  policies?: DecisionTracePolicy[];
  updatesApplied?: unknown[];
  rollbackApplied?: unknown[];
};

export type DecisionTrace = {
  requestId?: string;
  action?: string;
  decision?: string;
  sessionId?: string;
  sessionStatus?: string;
  snapshotBefore?: Record<string, unknown>;
  snapshotAfter?: Record<string, unknown>;
  phases?: DecisionTracePhase[];
  phaseTraces?: DecisionTracePhase[];
  [key: string]: unknown;
};

export type ApiDecisionResponse = {
  requestId: string;
  action: "REGISTER" | "DROP" | string;
  decision: "ALLOW" | "DENY" | "PERMIT" | "FAILED" | string;
  phase?: string;
  predicate?: string;
  studentId?: string;
  classId?: string;
  failedPolicy?: string;
  denyReason?: string;
  sessionStatus?: string;
  explanation?: string;
  message?: string;
  decisionTrace?: DecisionTrace;
};

export type RegisterCommand = {
  requestId: string;
  studentId: string;
  classId: string;
  confirmedRegistrationRule: boolean;
  adminOverride: boolean;
  overrideReason: string;
  sessionLeaseValid: boolean;
};

export type DropCommand = {
  requestId: string;
  studentId: string;
  classId: string;
  sessionLeaseValid: boolean;
};
