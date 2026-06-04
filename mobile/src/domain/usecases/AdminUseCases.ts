import { AdminRepository } from "@domain/repositories/AdminRepository";

export class AdminUseCases {
  constructor(private readonly repository: AdminRepository) {}

  getDashboard() {
    return this.repository.getDashboard();
  }

  listPolicies(filters?: Record<string, string>) {
    return this.repository.listPolicies(filters);
  }

  getPolicySummary() {
    return this.repository.getPolicySummary();
  }

  transitionPolicy(policyId: string, targetStatus: string) {
    return this.repository.transitionPolicy(policyId, targetStatus);
  }

  reloadPolicies() {
    return this.repository.reloadPolicies();
  }

  getMonitorSummary() {
    return this.repository.getMonitorSummary();
  }

  setMaintenance(active: boolean) {
    return this.repository.setMaintenance(active);
  }

  changeClassStatus(classId: string, status: string) {
    return this.repository.changeClassStatus(classId, status);
  }

  addStudentHold(studentId: string, holdCode: string) {
    return this.repository.addStudentHold(studentId, holdCode);
  }

  removeStudentHold(studentId: string, holdCode: string) {
    return this.repository.removeStudentHold(studentId, holdCode);
  }

  recheck(trigger?: string) {
    return this.repository.recheck(trigger);
  }

  listSessions(filters?: Record<string, string>) {
    return this.repository.listSessions(filters);
  }

  listStudents() {
    return this.repository.listStudents();
  }

  getStudentDetail(studentId: string) {
    return this.repository.getStudentDetail(studentId);
  }

  updateStudentState(studentId: string, state: Record<string, unknown>) {
    return this.repository.updateStudentState(studentId, state);
  }

  listClasses() {
    return this.repository.listClasses();
  }

  getClassDetail(classId: string) {
    return this.repository.getClassDetail(classId);
  }

  updateClassState(classId: string, state: Record<string, unknown>) {
    return this.repository.updateClassState(classId, state);
  }

  getValidationReport() {
    return this.repository.getValidationReport();
  }

  getAnalyzerReport() {
    return this.repository.getAnalyzerReport();
  }

  getBenchmarkReport() {
    return this.repository.getBenchmarkReport();
  }

  listAuditLogs(filters?: Record<string, string>) {
    return this.repository.listAuditLogs(filters);
  }
}
