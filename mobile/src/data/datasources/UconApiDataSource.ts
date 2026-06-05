import { AxiosInstance } from "axios";
import { AuthSession, LoginCommand } from "@domain/entities/AuthSession";
import { ClassSection } from "@domain/entities/ClassSection";
import { ApiDecisionResponse, DropCommand, RegisterCommand } from "@domain/entities/Decision";
import { MonitoringResult } from "@domain/entities/Monitoring";
import { EnvironmentState } from "@domain/entities/EnvironmentState";
import { PapPolicy, PapSummary } from "@domain/entities/Policy";
import { Student, StudentGrade } from "@domain/entities/Student";
import { RegisteredClass, StudentDashboard, StudentRequestHistory, StudentSession } from "@domain/entities/StudentDashboard";
import {
  AdminClass,
  AdminDashboard,
  AdminMonitoringResult,
  AdminPolicy,
  AdminSession,
  AdminStudent,
} from "@domain/entities/AdminPortal";

export class UconApiDataSource {
  constructor(private readonly http: AxiosInstance) {}

  async login(command: LoginCommand): Promise<AuthSession> {
    const response = await this.http.post<AuthSession>("/auth/login", command);
    return response.data;
  }

  async logout(): Promise<void> {
    await this.http.post("/auth/logout");
  }

  async listStudents(): Promise<Student[]> {
    const response = await this.http.get<Student[]>("/students");
    return response.data;
  }

  async getMyProfile(): Promise<Student> {
    const response = await this.http.get<Student>("/students/me");
    return response.data;
  }

  async updateMyProfile(profile: { email: string; dateOfBirth: string; gender: string }): Promise<Student> {
    const response = await this.http.patch<Student>("/students/me/profile", profile);
    return response.data;
  }

  async getMyDashboard(): Promise<StudentDashboard> {
    const response = await this.http.get<StudentDashboard>("/students/me/dashboard");
    return response.data;
  }

  async getMyGrades(): Promise<StudentGrade[]> {
    const response = await this.http.get<StudentGrade[]>("/students/me/grades");
    return response.data;
  }

  async listClasses(): Promise<ClassSection[]> {
    const response = await this.http.get<ClassSection[]>("/classes");
    return response.data;
  }

  async getMyRegisteredClasses(): Promise<RegisteredClass[]> {
    const response = await this.http.get<RegisteredClass[]>("/students/me/registered-classes");
    return response.data;
  }

  async getMyHistory(): Promise<StudentRequestHistory[]> {
    const response = await this.http.get<StudentRequestHistory[]>("/students/me/history");
    return response.data;
  }

  async getMySessions(): Promise<StudentSession[]> {
    const response = await this.http.get<StudentSession[]>("/students/me/sessions");
    return response.data;
  }

  async register(command: RegisterCommand): Promise<ApiDecisionResponse> {
    const response = await this.http.post<ApiDecisionResponse>("/register", command);
    return response.data;
  }

  async drop(command: DropCommand): Promise<ApiDecisionResponse> {
    const response = await this.http.post<ApiDecisionResponse>("/drop", command);
    return response.data;
  }

  async setMaintenance(active: boolean): Promise<MonitoringResult> {
    const response = await this.http.post<MonitoringResult>("/demo/monitor/maintenance", null, {
      params: { active },
    });
    return response.data;
  }

  async changeClassStatus(classId: string, status: string): Promise<MonitoringResult> {
    const response = await this.http.post<MonitoringResult>("/demo/monitor/class-status", null, {
      params: { classId, status },
    });
    return response.data;
  }

  async addStudentHold(studentId: string, holdCode: string): Promise<MonitoringResult> {
    const response = await this.http.post<MonitoringResult>("/demo/monitor/student-hold", null, {
      params: { studentId, holdCode },
    });
    return response.data;
  }

  async recheckActiveSessions(): Promise<MonitoringResult> {
    const response = await this.http.post<MonitoringResult>("/demo/monitor/recheck");
    return response.data;
  }

  async getEnvironmentState(): Promise<EnvironmentState> {
    const response = await this.http.get<EnvironmentState>("/demo/environment/state");
    return response.data;
  }

  async openRegistrationWindow(): Promise<EnvironmentState> {
    const response = await this.http.post<EnvironmentState>("/demo/environment/open-registration");
    return response.data;
  }

  async closeRegistrationWindow(): Promise<EnvironmentState> {
    const response = await this.http.post<EnvironmentState>("/demo/environment/close-registration");
    return response.data;
  }

  async listPolicies(): Promise<PapPolicy[]> {
    const response = await this.http.get<PapPolicy[]>("/pap/policies");
    return response.data;
  }

  async getPapSummary(): Promise<PapSummary> {
    const response = await this.http.get<PapSummary>("/pap/summary");
    return response.data;
  }

  async transitionPolicy(policyId: string, targetStatus: string): Promise<unknown> {
    const response = await this.http.post("/pap/transition", null, {
      params: { policyId, targetStatus },
    });
    return response.data;
  }

  async reloadPolicies(): Promise<unknown> {
    const response = await this.http.post("/pap/reload");
    return response.data;
  }

  async getAdminDashboard(): Promise<AdminDashboard> {
    const response = await this.http.get<AdminDashboard>("/admin/dashboard");
    return response.data;
  }

  async listAdminPolicies(filters?: Record<string, string>): Promise<AdminPolicy[]> {
    const response = await this.http.get<AdminPolicy[]>("/admin/policies", { params: filters });
    return response.data;
  }

  async getAdminPolicySummary(): Promise<Record<string, number>> {
    const response = await this.http.get<Record<string, number>>("/admin/policies/summary");
    return response.data;
  }

  async transitionAdminPolicy(policyId: string, targetStatus: string): Promise<Record<string, unknown>> {
    const response = await this.http.post<Record<string, unknown>>(`/admin/policies/${policyId}/transition`, {
      targetStatus,
    });
    return response.data;
  }

  async reloadAdminPolicies(): Promise<Record<string, unknown>> {
    const response = await this.http.post<Record<string, unknown>>("/admin/policies/reload");
    return response.data;
  }

  async getAdminMonitorSummary(): Promise<Record<string, unknown>> {
    const response = await this.http.get<Record<string, unknown>>("/admin/monitor/summary");
    return response.data;
  }

  async setAdminMaintenance(active: boolean): Promise<AdminMonitoringResult> {
    const response = await this.http.post<AdminMonitoringResult>("/admin/monitor/maintenance", { active });
    return response.data;
  }

  async changeAdminClassStatus(classId: string, status: string): Promise<AdminMonitoringResult> {
    const response = await this.http.post<AdminMonitoringResult>("/admin/monitor/class-status", { classId, status });
    return response.data;
  }

  async addAdminStudentHold(studentId: string, holdCode: string): Promise<AdminMonitoringResult> {
    const response = await this.http.post<AdminMonitoringResult>("/admin/monitor/student-hold", { studentId, holdCode });
    return response.data;
  }

  async removeAdminStudentHold(studentId: string, holdCode: string): Promise<Record<string, unknown>> {
    const response = await this.http.delete<Record<string, unknown>>("/admin/monitor/student-hold", {
      data: { studentId, holdCode },
    });
    return response.data;
  }

  async recheckAdminSessions(trigger?: string): Promise<AdminMonitoringResult> {
    const response = await this.http.post<AdminMonitoringResult>("/admin/monitor/recheck", { trigger });
    return response.data;
  }

  async listAdminSessions(filters?: Record<string, string>): Promise<AdminSession[]> {
    const response = await this.http.get<AdminSession[]>("/admin/sessions", { params: filters });
    return response.data;
  }

  async listAdminStudents(): Promise<AdminStudent[]> {
    const response = await this.http.get<AdminStudent[]>("/admin/students");
    return response.data;
  }

  async getAdminStudentDetail(studentId: string): Promise<Record<string, unknown>> {
    const response = await this.http.get<Record<string, unknown>>(`/admin/students/${studentId}`);
    return response.data;
  }

  async updateAdminStudentState(studentId: string, state: Record<string, unknown>): Promise<Record<string, unknown>> {
    const response = await this.http.patch<Record<string, unknown>>(`/admin/students/${studentId}/demo-state`, state);
    return response.data;
  }

  async listAdminClasses(): Promise<AdminClass[]> {
    const response = await this.http.get<AdminClass[]>("/admin/classes");
    return response.data;
  }

  async getAdminClassDetail(classId: string): Promise<Record<string, unknown>> {
    const response = await this.http.get<Record<string, unknown>>(`/admin/classes/${classId}`);
    return response.data;
  }

  async updateAdminClassState(classId: string, state: Record<string, unknown>): Promise<Record<string, unknown>> {
    const response = await this.http.patch<Record<string, unknown>>(`/admin/classes/${classId}/demo-state`, state);
    return response.data;
  }

  async getAdminValidationReport(): Promise<Record<string, unknown>> {
    const response = await this.http.get<Record<string, unknown>>("/admin/validation");
    return response.data;
  }

  async getAdminAnalyzerReport(): Promise<Record<string, unknown>> {
    const response = await this.http.get<Record<string, unknown>>("/admin/analyzer");
    return response.data;
  }

  async getAdminBenchmarkReport(): Promise<Record<string, unknown>> {
    const response = await this.http.get<Record<string, unknown>>("/admin/benchmark");
    return response.data;
  }

  async listAdminAuditLogs(filters?: Record<string, string>): Promise<Record<string, unknown>[]> {
    const response = await this.http.get<Record<string, unknown>[]>("/admin/audit-logs", { params: filters });
    return response.data;
  }
}
