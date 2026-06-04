import {
  AdminClass,
  AdminDashboard,
  AdminMonitoringResult,
  AdminPolicy,
  AdminSession,
  AdminStudent,
} from "@domain/entities/AdminPortal";

export interface AdminRepository {
  getDashboard(): Promise<AdminDashboard>;
  listPolicies(filters?: Record<string, string>): Promise<AdminPolicy[]>;
  getPolicySummary(): Promise<Record<string, number>>;
  transitionPolicy(policyId: string, targetStatus: string): Promise<Record<string, unknown>>;
  reloadPolicies(): Promise<Record<string, unknown>>;
  getMonitorSummary(): Promise<Record<string, unknown>>;
  setMaintenance(active: boolean): Promise<AdminMonitoringResult>;
  changeClassStatus(classId: string, status: string): Promise<AdminMonitoringResult>;
  addStudentHold(studentId: string, holdCode: string): Promise<AdminMonitoringResult>;
  removeStudentHold(studentId: string, holdCode: string): Promise<Record<string, unknown>>;
  recheck(trigger?: string): Promise<AdminMonitoringResult>;
  listSessions(filters?: Record<string, string>): Promise<AdminSession[]>;
  listStudents(): Promise<AdminStudent[]>;
  getStudentDetail(studentId: string): Promise<Record<string, unknown>>;
  updateStudentState(studentId: string, state: Record<string, unknown>): Promise<Record<string, unknown>>;
  listClasses(): Promise<AdminClass[]>;
  getClassDetail(classId: string): Promise<Record<string, unknown>>;
  updateClassState(classId: string, state: Record<string, unknown>): Promise<Record<string, unknown>>;
  getValidationReport(): Promise<Record<string, unknown>>;
  getAnalyzerReport(): Promise<Record<string, unknown>>;
  getBenchmarkReport(): Promise<Record<string, unknown>>;
  listAuditLogs(filters?: Record<string, string>): Promise<Record<string, unknown>[]>;
}
