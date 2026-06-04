import { StudentRepository } from "@domain/repositories/StudentRepository";

export class StudentUseCases {
  constructor(private readonly studentRepository: StudentRepository) {}

  listStudents() {
    return this.studentRepository.listStudents();
  }

  getMyProfile() {
    return this.studentRepository.getMyProfile();
  }

  getMyGrades() {
    return this.studentRepository.getMyGrades();
  }

  listClasses() {
    return this.studentRepository.listClasses();
  }
}
