import { MonitoringResult } from "@domain/entities/Monitoring";

export interface MonitoringRepository {
  setMaintenance(active: boolean): Promise<MonitoringResult>;
  changeClassStatus(classId: string, status: string): Promise<MonitoringResult>;
  addStudentHold(studentId: string, holdCode: string): Promise<MonitoringResult>;
  recheckActiveSessions(): Promise<MonitoringResult>;
}
