import { AxiosInstance } from "axios";
import { AuthSession, LoginCommand } from "@domain/entities/AuthSession";
import { ClassSection } from "@domain/entities/ClassSection";
import { ApiDecisionResponse, DropCommand, RegisterCommand } from "@domain/entities/Decision";
import { MonitoringResult } from "@domain/entities/Monitoring";
import { EnvironmentState } from "@domain/entities/EnvironmentState";
import { PapPolicy, PapSummary } from "@domain/entities/Policy";
import { Student, StudentGrade } from "@domain/entities/Student";

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

  async getMyGrades(): Promise<StudentGrade[]> {
    const response = await this.http.get<StudentGrade[]>("/students/me/grades");
    return response.data;
  }

  async listClasses(): Promise<ClassSection[]> {
    const response = await this.http.get<ClassSection[]>("/classes");
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
}
