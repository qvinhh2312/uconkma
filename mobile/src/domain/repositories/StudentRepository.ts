import { ClassSection } from "@domain/entities/ClassSection";
import { RegisteredClass, StudentDashboard, StudentRequestHistory, StudentSession } from "@domain/entities/StudentDashboard";
import { Student, StudentGrade } from "@domain/entities/Student";

export interface StudentRepository {
  listStudents(): Promise<Student[]>;
  getMyDashboard(): Promise<StudentDashboard>;
  getMyProfile(): Promise<Student>;
  getMyGrades(): Promise<StudentGrade[]>;
  listClasses(): Promise<ClassSection[]>;
  getMyRegisteredClasses(): Promise<RegisteredClass[]>;
  getMyHistory(): Promise<StudentRequestHistory[]>;
  getMySessions(): Promise<StudentSession[]>;
}
