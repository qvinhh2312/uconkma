import { ClassSection } from "@domain/entities/ClassSection";
import { Student, StudentGrade } from "@domain/entities/Student";

export interface StudentRepository {
  listStudents(): Promise<Student[]>;
  getMyProfile(): Promise<Student>;
  getMyGrades(): Promise<StudentGrade[]>;
  listClasses(): Promise<ClassSection[]>;
}
