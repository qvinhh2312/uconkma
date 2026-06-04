import { UconApiDataSource } from "@data/datasources/UconApiDataSource";
import { normalizeError } from "@core/errors/normalizeError";
import { AdminRepository } from "@domain/repositories/AdminRepository";

export class AdminRepositoryImpl implements AdminRepository {
  constructor(private readonly dataSource: UconApiDataSource) {}

  async getDashboard() {
    try {
      return await this.dataSource.getAdminDashboard();
    } catch (error) {
      throw normalizeError(error);
    }
  }

  async listPolicies(filters?: Record<string, string>) {
    try {
      return await this.dataSource.listAdminPolicies(filters);
    } catch (error) {
      throw normalizeError(error);
    }
  }

  async getPolicySummary() {
    try {
      return await this.dataSource.getAdminPolicySummary();
    } catch (error) {
      throw normalizeError(error);
    }
  }

  async transitionPolicy(policyId: string, targetStatus: string) {
    try {
      return await this.dataSource.transitionAdminPolicy(policyId, targetStatus);
    } catch (error) {
      throw normalizeError(error);
    }
  }

  async reloadPolicies() {
    try {
      return await this.dataSource.reloadAdminPolicies();
    } catch (error) {
      throw normalizeError(error);
    }
  }

  async getMonitorSummary() {
    try {
      return await this.dataSource.getAdminMonitorSummary();
    } catch (error) {
      throw normalizeError(error);
    }
  }

  async setMaintenance(active: boolean) {
    try {
      return await this.dataSource.setAdminMaintenance(active);
    } catch (error) {
      throw normalizeError(error);
    }
  }

  async changeClassStatus(classId: string, status: string) {
    try {
      return await this.dataSource.changeAdminClassStatus(classId, status);
    } catch (error) {
      throw normalizeError(error);
    }
  }

  async addStudentHold(studentId: string, holdCode: string) {
    try {
      return await this.dataSource.addAdminStudentHold(studentId, holdCode);
    } catch (error) {
      throw normalizeError(error);
    }
  }

  async removeStudentHold(studentId: string, holdCode: string) {
    try {
      return await this.dataSource.removeAdminStudentHold(studentId, holdCode);
    } catch (error) {
      throw normalizeError(error);
    }
  }

  async recheck(trigger?: string) {
    try {
      return await this.dataSource.recheckAdminSessions(trigger);
    } catch (error) {
      throw normalizeError(error);
    }
  }

  async listSessions(filters?: Record<string, string>) {
    try {
      return await this.dataSource.listAdminSessions(filters);
    } catch (error) {
      throw normalizeError(error);
    }
  }

  async listStudents() {
    try {
      return await this.dataSource.listAdminStudents();
    } catch (error) {
      throw normalizeError(error);
    }
  }

  async getStudentDetail(studentId: string) {
    try {
      return await this.dataSource.getAdminStudentDetail(studentId);
    } catch (error) {
      throw normalizeError(error);
    }
  }

  async updateStudentState(studentId: string, state: Record<string, unknown>) {
    try {
      return await this.dataSource.updateAdminStudentState(studentId, state);
    } catch (error) {
      throw normalizeError(error);
    }
  }

  async listClasses() {
    try {
      return await this.dataSource.listAdminClasses();
    } catch (error) {
      throw normalizeError(error);
    }
  }

  async getClassDetail(classId: string) {
    try {
      return await this.dataSource.getAdminClassDetail(classId);
    } catch (error) {
      throw normalizeError(error);
    }
  }

  async updateClassState(classId: string, state: Record<string, unknown>) {
    try {
      return await this.dataSource.updateAdminClassState(classId, state);
    } catch (error) {
      throw normalizeError(error);
    }
  }

  async getValidationReport() {
    try {
      return await this.dataSource.getAdminValidationReport();
    } catch (error) {
      throw normalizeError(error);
    }
  }

  async getAnalyzerReport() {
    try {
      return await this.dataSource.getAdminAnalyzerReport();
    } catch (error) {
      throw normalizeError(error);
    }
  }

  async getBenchmarkReport() {
    try {
      return await this.dataSource.getAdminBenchmarkReport();
    } catch (error) {
      throw normalizeError(error);
    }
  }

  async listAuditLogs(filters?: Record<string, string>) {
    try {
      return await this.dataSource.listAdminAuditLogs(filters);
    } catch (error) {
      throw normalizeError(error);
    }
  }
}
