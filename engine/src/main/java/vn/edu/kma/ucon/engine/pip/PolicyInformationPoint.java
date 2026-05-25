package vn.edu.kma.ucon.engine.pip;

import org.springframework.stereotype.Service;

import vn.edu.kma.ucon.engine.pdp.Environment;
import vn.edu.kma.ucon.engine.pdp.MaintenanceFlag;
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
    private final MaintenanceFlag maintenanceFlag;

    public PolicyInformationPoint(StudentRepository studentRepository,
                                  ClassSectionRepository classSectionRepository,
                                  MaintenanceFlag maintenanceFlag) {
        this.studentRepository = studentRepository;
        this.classSectionRepository = classSectionRepository;
        this.maintenanceFlag = maintenanceFlag;
    }

    public Student findStudent(String studentId) {
        return studentRepository.findById(studentId).orElse(null);
    }

    public ClassSection findClassSection(String classId) {
        return classSectionRepository.findById(classId).orElse(null);
    }

    public Environment buildEnvironment() {
        Environment environment = new Environment("NORMAL", "2026-03-27");
        environment.setOpenTime("2026-01-01");
        environment.setCloseTime("2026-12-31");
        environment.setSemester("2026_FALL");
        environment.setIsMaintenance(maintenanceFlag.isActive());
        environment.setMaxRegisterAttempts(5);
        environment.setMaxDropTimes(2);
        return environment;
    }
}
