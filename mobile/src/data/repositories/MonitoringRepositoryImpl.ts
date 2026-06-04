import { normalizeError } from "@core/errors/normalizeError";
import { UconApiDataSource } from "@data/datasources/UconApiDataSource";
import { MonitoringResult } from "@domain/entities/Monitoring";
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
}
