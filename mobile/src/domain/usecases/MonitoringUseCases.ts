import { MonitoringRepository } from "@domain/repositories/MonitoringRepository";

export class MonitoringUseCases {
  constructor(private readonly monitoringRepository: MonitoringRepository) {}

  setMaintenance(active: boolean) {
    return this.monitoringRepository.setMaintenance(active);
  }

  changeClassStatus(classId: string, status: string) {
    return this.monitoringRepository.changeClassStatus(classId, status);
  }

  addStudentHold(studentId: string, holdCode: string) {
    return this.monitoringRepository.addStudentHold(studentId, holdCode);
  }

  recheckActiveSessions() {
    return this.monitoringRepository.recheckActiveSessions();
  }
}
