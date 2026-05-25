package vn.edu.kma.ucon.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import vn.edu.kma.ucon.engine.pep.ApiDecisionResponse;
import vn.edu.kma.ucon.engine.pep.RegistrationController;
import vn.edu.kma.ucon.engine.pep.UconRequest;
import vn.edu.kma.ucon.engine.pdp.AuthDecision;
import vn.edu.kma.ucon.engine.pdp.Environment;
import vn.edu.kma.ucon.engine.pdp.MaintenanceFlag;
import vn.edu.kma.ucon.engine.pdp.PolicyEngine;
import vn.edu.kma.ucon.engine.pip.entity.ClassSection;
import vn.edu.kma.ucon.engine.pip.entity.Course;
import vn.edu.kma.ucon.engine.pip.entity.Student;
import vn.edu.kma.ucon.engine.pip.repository.AuditLogRepository;
import vn.edu.kma.ucon.engine.pip.repository.ClassSectionRepository;
import vn.edu.kma.ucon.engine.pip.repository.CourseRepository;
import vn.edu.kma.ucon.engine.pip.repository.RegistrationRepository;
import vn.edu.kma.ucon.engine.pip.repository.StudentRepository;

@SpringBootTest
@TestMethodOrder(MethodOrderer.MethodName.class)
class UconInternshipDemoTests {

    @Autowired
    RegistrationController regController;
    @Autowired
    StudentRepository studentRepo;
    @Autowired
    ClassSectionRepository classRepo;
    @Autowired
    CourseRepository courseRepo;
    @Autowired
    RegistrationRepository registrationRepo;
    @Autowired
    AuditLogRepository auditRepo;
    @Autowired
    PolicyEngine policyEngine;
    @Autowired
    MaintenanceFlag maintenanceFlag;

    @BeforeEach
    void setUp() {
        maintenanceFlag.setActive(false);
        auditRepo.deleteAll();
        registrationRepo.deleteAll();
        studentRepo.deleteAll();
        classRepo.deleteAll();
        courseRepo.deleteAll();

        Course cs101 = new Course();
        cs101.setCourseId("CS101");
        cs101.setCredits(3);
        cs101.setPrerequisites("");
        cs101.setTuitionFee(3000000);
        courseRepo.save(cs101);

        Course cs102 = new Course();
        cs102.setCourseId("CS102");
        cs102.setCredits(4);
        cs102.setPrerequisites("CS101");
        cs102.setTuitionFee(4000000);
        courseRepo.save(cs102);

        ClassSection cs102Class = new ClassSection();
        cs102Class.setClassId("CS102_01");
        cs102Class.setCourse(courseRepo.findById("CS102").orElseThrow());
        cs102Class.setCapacity(5);
        cs102Class.setEnrolled(4);
        cs102Class.setStatus("OPEN");
        cs102Class.setScheduleSlots("T3_1-3,T5_4-6");
        classRepo.save(cs102Class);

        Student sv001 = new Student();
        sv001.setStudentId("SV001");
        sv001.setTuitionPaid(true);
        sv001.setCurrentCredits(0);
        sv001.setMaxCreditsEffective(15);
        sv001.setCompletedCourses("CS101");
        sv001.setRegisteredClassIds("");
        sv001.setRegisteredScheduleSlots("");
        sv001.setHolds("");
        studentRepo.save(sv001);
    }

    @Test
    @DisplayName("Demo 1 - UCON pre-authorization blocks invalid register requests")
    void demo01_UconPreAuthorization_BlocksInvalidRegisterRequests() {
        System.out.println("\n DEMO 1: UCON PRE-AUTHORIZATION");
        System.out.println(" Muc tieu: chung minh UCON xay dung chinh sach an ninh truoc khi hanh dong REGISTER xay ra");

        Student unpaidStudent = createStudent("SV002", false, 0, "CS101", "");
        ResponseEntity<ApiDecisionResponse> unpaidResponse = regController.register(registerRequest("SV002", "CS102_01"));
        assertEquals(403, unpaidResponse.getStatusCode().value());
        assertEquals("TUITION_NOT_PAID", unpaidResponse.getBody().getDenyReason());
        System.out.println("[CHECK] P01 - Chua dong hoc phi -> DENY: TUITION_NOT_PAID");

        Student eligibleStudent = studentRepo.findById("SV001").orElseThrow();
        ClassSection classSection = classRepo.findById("CS102_01").orElseThrow();
        AuthDecision outsideWindow = policyEngine.evaluatePhase(
                "PRE",
                eligibleStudent,
                classSection,
                outsideWindowEnv(),
                registerRequest("SV001", "CS102_01")
        );
        assertFalse(outsideWindow.isPermit());
        assertEquals("OUTSIDE_TRANSACTION_WINDOW", outsideWindow.getFailedCode());
        System.out.println("[CHECK] P02 - Ngoai khung thoi gian giao dich -> DENY: OUTSIDE_TRANSACTION_WINDOW");

        classSection.setStatus("LOCKED");
        classRepo.save(classSection);
        ResponseEntity<ApiDecisionResponse> lockedClassResponse = regController.register(registerRequest("SV001", "CS102_01"));
        assertEquals(403, lockedClassResponse.getStatusCode().value());
        assertEquals("CLASS_NOT_OPEN", lockedClassResponse.getBody().getDenyReason());
        System.out.println("[CHECK] P03 - Lop khong mo -> DENY: CLASS_NOT_OPEN");

        setUp();
        Student creditLimitedStudent = studentRepo.findById("SV001").orElseThrow();
        creditLimitedStudent.setCurrentCredits(12);
        studentRepo.save(creditLimitedStudent);
        ResponseEntity<ApiDecisionResponse> creditLimitResponse = regController.register(registerRequest("SV001", "CS102_01"));
        assertEquals(403, creditLimitResponse.getStatusCode().value());
        assertEquals("CREDIT_LIMIT_EXCEEDED", creditLimitResponse.getBody().getDenyReason());
        System.out.println("[CHECK] P05 - Vuot tran tin chi -> DENY: CREDIT_LIMIT_EXCEEDED");

        setUp();
        Student missingPrereqStudent = studentRepo.findById("SV001").orElseThrow();
        missingPrereqStudent.setCompletedCourses("");
        studentRepo.save(missingPrereqStudent);
        ResponseEntity<ApiDecisionResponse> prerequisiteResponse = regController.register(registerRequest("SV001", "CS102_01"));
        assertEquals(403, prerequisiteResponse.getStatusCode().value());
        assertEquals("PREREQUISITE_NOT_MET", prerequisiteResponse.getBody().getDenyReason());
        System.out.println("[CHECK] P06 - Thieu mon tien quyet -> DENY: PREREQUISITE_NOT_MET");
        System.out.println("[RESULT] Demo 1 thanh cong: UCON PRE authorization chan request khong hop le ngay truoc hanh dong");
    }

    @Test
    @DisplayName("Demo 2 - UCON ongoing-authorization rechecks state before commit")
    void demo02_UconOngoingAuthorization_RechecksStateBeforeCommit() {
        System.out.println("\n DEMO 2: UCON ONGOING-AUTHORIZATION");
        System.out.println(" Muc tieu: chung minh request co the pass PRE nhung bi chan o ONGOING khi trang thai thay doi");

        Student student = studentRepo.findById("SV001").orElseThrow();
        ClassSection openClass = classRepo.findById("CS102_01").orElseThrow();
        UconRequest request = registerRequest("SV001", "CS102_01");

        AuthDecision preDecision = policyEngine.evaluatePhase(
                "PRE",
                student,
                openClass,
                defaultEnv(false),
                request
        );
        assertTrue(preDecision.isPermit());
        System.out.println("[CHECK] PRE pass: request du dieu kien o thoi diem bat dau");

        AuthDecision ongoingDecision = policyEngine.evaluatePhase(
                "ONGOING",
                student,
                openClass,
                defaultEnv(true),
                request
        );
        assertFalse(ongoingDecision.isPermit());
        assertEquals("SYSTEM_UNDER_MAINTENANCE", ongoingDecision.getFailedCode());
        System.out.println("[CHECK] ONGOING fail: maintenance bat giua chung -> DENY: SYSTEM_UNDER_MAINTENANCE");

        assertEquals(0, registrationRepo.count());
        assertEquals(0, studentRepo.findById("SV001").orElseThrow().getCurrentCredits());
        assertEquals(4, classRepo.findById("CS102_01").orElseThrow().getEnrolled());
        System.out.println("[CHECK] Khong co post-update nao duoc thuc thi, state duoc giu nguyen");
        System.out.println("[RESULT] Demo 2 thanh cong: UCON the hien continuity bang ongoing re-check gan commit");
    }

    @Test
    @DisplayName("Demo 3 - UCON post-update mutates and restores state")
    void demo03_UconPostUpdate_MutatesAndRestoresState() {
        System.out.println("\n DEMO 3: UCON POST-UPDATE");
        System.out.println(" Muc tieu: chung minh policy khong chi permit/deny ma con cap nhat va hoan tra state he thong");

        ResponseEntity<ApiDecisionResponse> registerResponse = regController.register(registerRequest("SV001", "CS102_01"));
        assertEquals(200, registerResponse.getStatusCode().value());

        Student afterRegister = studentRepo.findById("SV001").orElseThrow();
        ClassSection classAfterRegister = classRepo.findById("CS102_01").orElseThrow();

        assertEquals(4, afterRegister.getCurrentCredits());
        assertEquals(4000000, afterRegister.getTuitionDebt());
        assertTrue(afterRegister.getRegisteredClassIds().contains("CS102_01"));
        assertTrue(afterRegister.getRegisteredScheduleSlots().contains("T3_1-3"));
        assertEquals(5, classAfterRegister.getEnrolled());
        assertEquals(1, registrationRepo.count());
        assertEquals(1, auditRepo.count());
        System.out.println("[CHECK] REGISTER thanh cong -> tang enrolled, tang currentCredits, tao transaction, cong tuitionDebt, ghi audit");

        ResponseEntity<ApiDecisionResponse> dropResponse = regController.drop(dropRequest("SV001", "CS102_01"));
        assertEquals(200, dropResponse.getStatusCode().value());

        Student afterDrop = studentRepo.findById("SV001").orElseThrow();
        ClassSection classAfterDrop = classRepo.findById("CS102_01").orElseThrow();

        assertEquals(0, afterDrop.getCurrentCredits());
        assertEquals(0, afterDrop.getTuitionDebt());
        assertFalse(afterDrop.getRegisteredClassIds().contains("CS102_01"));
        assertFalse(afterDrop.getRegisteredScheduleSlots().contains("T3_1-3"));
        assertEquals(4, classAfterDrop.getEnrolled());
        assertEquals(0, registrationRepo.count());
        assertEquals(2, auditRepo.count());
        System.out.println("[CHECK] DROP thanh cong -> giam enrolled, giam currentCredits, xoa transaction, tru tuitionDebt, ghi audit");
        System.out.println("[RESULT] Demo 3 thanh cong: UCON POST update tac dong truc tiep len state va audit cua he thong");
    }

    private Student createStudent(String studentId,
                                  boolean tuitionPaid,
                                  int currentCredits,
                                  String completedCourses,
                                  String holds) {
        Student student = new Student();
        student.setStudentId(studentId);
        student.setTuitionPaid(tuitionPaid);
        student.setCurrentCredits(currentCredits);
        student.setMaxCreditsEffective(15);
        student.setCompletedCourses(completedCourses);
        student.setRegisteredClassIds("");
        student.setRegisteredScheduleSlots("");
        student.setHolds(holds);
        return studentRepo.save(student);
    }

    private UconRequest registerRequest(String studentId, String classId) {
        UconRequest req = new UconRequest();
        req.setRequestId(UUID.randomUUID().toString());
        req.setActionType("REGISTER");
        req.setStudentId(studentId);
        req.setClassId(classId);
        req.setConfirmedRegistrationRule(true);
        req.setAdminOverride(false);
        req.setSessionLeaseValid(true);
        return req;
    }

    private UconRequest dropRequest(String studentId, String classId) {
        UconRequest req = new UconRequest();
        req.setRequestId(UUID.randomUUID().toString());
        req.setActionType("DROP");
        req.setStudentId(studentId);
        req.setClassId(classId);
        req.setConfirmedRegistrationRule(true);
        req.setAdminOverride(false);
        req.setSessionLeaseValid(true);
        return req;
    }

    private Environment defaultEnv(boolean maintenance) {
        Environment env = new Environment("NORMAL", "2026-03-27");
        env.setOpenTime("2026-01-01");
        env.setCloseTime("2026-12-31");
        env.setSemester("2026_FALL");
        env.setIsMaintenance(maintenance);
        env.setMaxRegisterAttempts(3);
        env.setMaxDropTimes(3);
        return env;
    }

    private Environment outsideWindowEnv() {
        Environment env = new Environment("ADJUSTMENT", "2025-01-01");
        env.setOpenTime("2026-01-01");
        env.setCloseTime("2026-12-31");
        env.setSemester("2026_FALL");
        env.setIsMaintenance(false);
        env.setMaxRegisterAttempts(3);
        env.setMaxDropTimes(3);
        return env;
    }
}
