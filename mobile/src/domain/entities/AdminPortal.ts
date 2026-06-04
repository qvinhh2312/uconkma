export type AdminDashboard = {
  policySummary: Record<string, number>;
  uconCoverage: {
    authorization: number;
    obligation: number;
    condition: number;
    variants: string[];
  };
  runtimeSummary: Record<string, number>;
  domainSummary: Record<string, number>;
  environment: Record<string, unknown>;
  lastRecheck: Record<string, unknown>;
};

export type AdminPolicy = Record<string, unknown> & {
  policyId: string;
  predicate?: string;
  phase?: string;
  updateTiming?: string;
  targetAction?: string;
  effect?: string;
  uconVariant?: string;
  policyStatus?: string;
  status?: string;
  source?: string;
  version?: string;
  description?: string;
};

export type AdminMonitoringResult = Record<string, unknown> & {
  checkedSessions?: number;
  revokedSessions?: number;
  message?: string;
  trigger?: string;
};

export type AdminStudent = Record<string, unknown> & {
  studentId: string;
  fullName: string;
  currentCredits?: number;
  tuitionDebt?: number;
  holds?: string[] | string;
  registeredClassCount?: number;
};

export type AdminClass = Record<string, unknown> & {
  classId: string;
  courseName?: string;
  courseCode?: string;
  capacity?: number;
  enrolled?: number;
  reservedSeats?: number;
  availableSeats?: number;
  status?: string;
};

export type AdminSession = Record<string, unknown> & {
  sessionId: string;
  requestId?: string;
  studentId?: string;
  classId?: string;
  action?: string;
  status?: string;
  revokeReason?: string;
};
