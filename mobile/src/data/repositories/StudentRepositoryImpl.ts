import { normalizeError } from "@core/errors/normalizeError";
import { UconApiDataSource } from "@data/datasources/UconApiDataSource";
import { ClassSection } from "@domain/entities/ClassSection";
import { Student, StudentGrade } from "@domain/entities/Student";
import { StudentRepository } from "@domain/repositories/StudentRepository";

export class StudentRepositoryImpl implements StudentRepository {
  constructor(private readonly api: UconApiDataSource) {}

  async listStudents(): Promise<Student[]> {
    try {
      return await this.api.listStudents();
    } catch (error) {
      throw normalizeError(error);
    }
  }

  async getMyProfile(): Promise<Student> {
    try {
      return await this.api.getMyProfile();
    } catch (error) {
      throw normalizeError(error);
    }
  }

  async getMyGrades(): Promise<StudentGrade[]> {
    try {
      return await this.api.getMyGrades();
    } catch (error) {
      throw normalizeError(error);
    }
  }

  async listClasses(): Promise<ClassSection[]> {
    try {
      return await this.api.listClasses();
    } catch (error) {
      throw normalizeError(error);
    }
  }
}
