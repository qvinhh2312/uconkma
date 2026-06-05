import { normalizeError } from "@core/errors/normalizeError";
import { UconApiDataSource } from "@data/datasources/UconApiDataSource";
import { ClassSection } from "@domain/entities/ClassSection";
import { Student, StudentGrade } from "@domain/entities/Student";
import { RegisteredClass, StudentDashboard, StudentRequestHistory, StudentSession } from "@domain/entities/StudentDashboard";
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

  async getMyDashboard(): Promise<StudentDashboard> {
    try {
      return await this.api.getMyDashboard();
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

  async updateMyProfile(profile: { email: string; dateOfBirth: string; gender: string }): Promise<Student> {
    try {
      return await this.api.updateMyProfile(profile);
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

  async getMyRegisteredClasses(): Promise<RegisteredClass[]> {
    try {
      return await this.api.getMyRegisteredClasses();
    } catch (error) {
      throw normalizeError(error);
    }
  }

  async getMyHistory(): Promise<StudentRequestHistory[]> {
    try {
      return await this.api.getMyHistory();
    } catch (error) {
      throw normalizeError(error);
    }
  }

  async getMySessions(): Promise<StudentSession[]> {
    try {
      return await this.api.getMySessions();
    } catch (error) {
      throw normalizeError(error);
    }
  }
}
