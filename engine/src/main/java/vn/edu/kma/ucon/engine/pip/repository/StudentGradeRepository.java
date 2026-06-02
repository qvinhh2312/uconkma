package vn.edu.kma.ucon.engine.pip.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.edu.kma.ucon.engine.pip.entity.StudentGrade;

public interface StudentGradeRepository extends JpaRepository<StudentGrade, Long> {
    List<StudentGrade> findByStudentIdOrderBySemesterDescCourseIdAsc(String studentId);
    boolean existsByStudentIdAndCourseIdAndSemester(String studentId, String courseId, String semester);
}
