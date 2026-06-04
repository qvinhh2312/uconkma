import { MonitoringResult } from "@domain/entities/Monitoring";
import { EnvironmentState } from "@domain/entities/EnvironmentState";

export interface MonitoringRepository {
  setMaintenance(active: boolean): Promise<MonitoringResult>;
  changeClassStatus(classId: string, status: string): Promise<MonitoringResult>;
  addStudentHold(studentId: string, holdCode: string): Promise<MonitoringResult>;
  recheckActiveSessions(): Promise<MonitoringResult>;
  getEnvironmentState(): Promise<EnvironmentState>;
  openRegistrationWindow(): Promise<EnvironmentState>;
  closeRegistrationWindow(): Promise<EnvironmentState>;
}
