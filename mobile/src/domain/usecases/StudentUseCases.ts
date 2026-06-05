import { StudentRepository } from "@domain/repositories/StudentRepository";

export class StudentUseCases {
  constructor(private readonly studentRepository: StudentRepository) {}

  listStudents() {
    return this.studentRepository.listStudents();
  }

  getMyDashboard() {
    return this.studentRepository.getMyDashboard();
  }

  getMyProfile() {
    return this.studentRepository.getMyProfile();
  }

  updateMyProfile(profile: { email: string; dateOfBirth: string; gender: string }) {
    return this.studentRepository.updateMyProfile(profile);
  }

  getMyGrades() {
    return this.studentRepository.getMyGrades();
  }

  listClasses() {
    return this.studentRepository.listClasses();
  }

  getMyRegisteredClasses() {
    return this.studentRepository.getMyRegisteredClasses();
  }

  getMyHistory() {
    return this.studentRepository.getMyHistory();
  }

  getMySessions() {
    return this.studentRepository.getMySessions();
  }
}
