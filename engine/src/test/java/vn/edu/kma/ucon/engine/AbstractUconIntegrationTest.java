package vn.edu.kma.ucon.engine;

import java.io.File;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import vn.edu.kma.ucon.engine.pdp.MaintenanceFlag;
import vn.edu.kma.ucon.engine.pep.RegistrationController;
import vn.edu.kma.ucon.engine.pep.UconRequest;
import vn.edu.kma.ucon.engine.pip.entity.ClassSection;
import vn.edu.kma.ucon.engine.pip.entity.Course;
import vn.edu.kma.ucon.engine.pip.entity.Student;
import vn.edu.kma.ucon.engine.pip.repository.AuditLogRepository;
import vn.edu.kma.ucon.engine.pip.repository.ClassSectionRepository;
import vn.edu.kma.ucon.engine.pip.repository.CourseRepository;
import vn.edu.kma.ucon.engine.pip.repository.RegistrationRepository;
import vn.edu.kma.ucon.engine.pip.repository.StudentRepository;
import vn.edu.kma.ucon.engine.session.UsageSessionRepository;

/**
 * Shared Spring fixture for focused UCON integration tests.
 */
@SpringBootTest
abstract class AbstractUconIntegrationTest {

    @Autowired
    protected RegistrationController registrationController;
    @Autowired
    protected StudentRepository studentRepository;
    @Autowired
    protected ClassSectionRepository classSectionRepository;
    @Autowired
    protected CourseRepository courseRepository;
    @Autowired
    protected RegistrationRepository registrationRepository;
    @Autowired
    protected AuditLogRepository auditLogRepository;
    @Autowired
    protected UsageSessionRepository usageSessionRepository;
    @Autowired
    protected MaintenanceFlag maintenanceFlag;

    @BeforeEach
    void resetDomainState() {
        maintenanceFlag.setActive(false);
        auditLogRepository.deleteAll();
        registrationRepository.deleteAll();
        usageSessionRepository.deleteAll();
        studentRepository.deleteAll();
        classSectionRepository.deleteAll();
        courseRepository.deleteAll();

        Course cs101 = new Course();
        cs101.setCourseId("CS101");
        cs101.setCredits(3);
        cs101.setPrerequisites("");
        cs101.setTuitionFee(3000000);
        courseRepository.save(cs101);

        Course cs102 = new Course();
        cs102.setCourseId("CS102");
        cs102.setCredits(4);
        cs102.setPrerequisites("CS101");
        cs102.setTuitionFee(4000000);
        courseRepository.save(cs102);

        ClassSection cs102Class = new ClassSection();
        cs102Class.setClassId("CS102_01");
        cs102Class.setCourse(courseRepository.findById("CS102").orElseThrow());
        cs102Class.setCapacity(5);
        cs102Class.setEnrolled(4);
        cs102Class.setReservedSeats(0);
        cs102Class.setStatus("OPEN");
        cs102Class.setScheduleSlots("T3_1-3,T5_4-6");
        classSectionRepository.save(cs102Class);

        Student sv001 = new Student();
        sv001.setStudentId("SV001");
        sv001.setTuitionPaid(true);
        sv001.setCurrentCredits(0);
        sv001.setMaxCreditsEffective(15);
        sv001.setCompletedCourses("CS101");
        sv001.setRegisteredClassIds("");
        sv001.setRegisteredScheduleSlots("");
        sv001.setHolds("");
        sv001.setRegisterAttemptCount(0);
        sv001.setDropCountForSemester(0);
        studentRepository.save(sv001);
    }

    protected UconRequest registerRequest() {
        UconRequest request = new UconRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setStudentId("SV001");
        request.setClassId("CS102_01");
        request.setConfirmedRegistrationRule(true);
        request.setAdminOverride(false);
        request.setSessionLeaseValid(true);
        return request;
    }

    protected UconRequest dropRequest() {
        UconRequest request = registerRequest();
        request.setActionType("DROP");
        return request;
    }

    protected File resolveExistingFile(String fromModule, String fromRoot) {
        File file = new File(fromModule);
        if (file.exists()) {
            return file;
        }
        file = new File(fromRoot);
        if (file.exists()) {
            return file;
        }
        throw new IllegalStateException("Cannot resolve file: " + fromModule + " or " + fromRoot);
    }
}
