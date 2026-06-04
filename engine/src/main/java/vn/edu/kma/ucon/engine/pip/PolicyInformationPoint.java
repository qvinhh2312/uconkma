package vn.edu.kma.ucon.engine.pip;

import org.springframework.stereotype.Service;

import vn.edu.kma.ucon.engine.pdp.Environment;
import vn.edu.kma.ucon.engine.pip.entity.ClassSection;
import vn.edu.kma.ucon.engine.pip.entity.Student;
import vn.edu.kma.ucon.engine.pip.repository.ClassSectionRepository;
import vn.edu.kma.ucon.engine.pip.repository.StudentRepository;

/**
 * Policy information point (PIP) facade that supplies domain entities and
 * environment attributes to the UCON pipeline.
 */
@Service
public class PolicyInformationPoint {

    private final StudentRepository studentRepository;
    private final ClassSectionRepository classSectionRepository;
    private final EnvironmentStateService environmentStateService;

    public PolicyInformationPoint(StudentRepository studentRepository,
                                  ClassSectionRepository classSectionRepository,
                                  EnvironmentStateService environmentStateService) {
        this.studentRepository = studentRepository;
        this.classSectionRepository = classSectionRepository;
        this.environmentStateService = environmentStateService;
    }

    public Student findStudent(String studentId) {
        return studentRepository.findById(studentId).orElse(null);
    }

    public ClassSection findClassSection(String classId) {
        return classSectionRepository.findById(classId).orElse(null);
    }

    public Environment buildEnvironment() {
        return environmentStateService.buildEnvironment();
    }
}
