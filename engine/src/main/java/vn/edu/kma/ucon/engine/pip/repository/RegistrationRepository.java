package vn.edu.kma.ucon.engine.pip.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.kma.ucon.engine.pip.entity.Registration;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    Optional<Registration> findByStudentIdAndClassIdAndSemester(String studentId, String classId, String semester);
    void deleteByStudentIdAndClassIdAndSemester(String studentId, String classId, String semester);
}
