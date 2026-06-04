import { normalizeError } from "@core/errors/normalizeError";
import { UconApiDataSource } from "@data/datasources/UconApiDataSource";
import { MonitoringResult } from "@domain/entities/Monitoring";
import { EnvironmentState } from "@domain/entities/EnvironmentState";
import { MonitoringRepository } from "@domain/repositories/MonitoringRepository";

export class MonitoringRepositoryImpl implements MonitoringRepository {
  constructor(private readonly api: UconApiDataSource) {}

  async setMaintenance(active: boolean): Promise<MonitoringResult> {
    try {
      return await this.api.setMaintenance(active);
    } catch (error) {
      throw normalizeError(error);
    }
  }

  async changeClassStatus(classId: string, status: string): Promise<MonitoringResult> {
    try {
      return await this.api.changeClassStatus(classId, status);
    } catch (error) {
      throw normalizeError(error);
    }
  }

  async addStudentHold(studentId: string, holdCode: string): Promise<MonitoringResult> {
    try {
      return await this.api.addStudentHold(studentId, holdCode);
    } catch (error) {
      throw normalizeError(error);
    }
  }

  async recheckActiveSessions(): Promise<MonitoringResult> {
    try {
      return await this.api.recheckActiveSessions();
    } catch (error) {
      throw normalizeError(error);
    }
  }

  async getEnvironmentState(): Promise<EnvironmentState> {
    try {
      return await this.api.getEnvironmentState();
    } catch (error) {
      throw normalizeError(error);
    }
  }

  async openRegistrationWindow(): Promise<EnvironmentState> {
    try {
      return await this.api.openRegistrationWindow();
    } catch (error) {
      throw normalizeError(error);
    }
  }

  async closeRegistrationWindow(): Promise<EnvironmentState> {
    try {
      return await this.api.closeRegistrationWindow();
    } catch (error) {
      throw normalizeError(error);
    }
  }
}
