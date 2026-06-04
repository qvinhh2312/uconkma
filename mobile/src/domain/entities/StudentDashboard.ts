import { ClassSection } from "./ClassSection";
import { Student } from "./Student";

export type RegisteredClass = ClassSection & {
  semester: string;
  registrationStatus: string;
  registeredAt?: string;
};

export type StudentRequestHistory = {
  id?: number;
  requestId: string;
  action: string;
  classId: string;
  decision: string;
  failedPolicy?: string;
  denyReason?: string;
  createdAt?: string;
  sessionStatus?: string;
};

export type StudentSession = {
  sessionId: string;
  requestId: string;
  action: string;
  classId: string;
  status: "ACTIVE" | "COMMITTED" | "FAILED" | "REVOKED" | string;
  startedAt?: string;
  lastCheckedAt?: string;
  revokeReason?: string;
};

export type StudentDashboard = {
  profile: Student;
  registeredClasses: RegisteredClass[];
  availableClasses: ClassSection[];
  recentHistory: StudentRequestHistory[];
  sessions: StudentSession[];
};
