package vn.edu.kma.ucon.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import vn.edu.kma.ucon.engine.pep.RegistrationController;
import vn.edu.kma.ucon.engine.pep.UconRequest;
import vn.edu.kma.ucon.engine.pdp.AuthDecision;
import vn.edu.kma.ucon.engine.pdp.Environment;
import vn.edu.kma.ucon.engine.pdp.PolicyEngine;
import vn.edu.kma.ucon.engine.pip.entity.ClassSection;
import vn.edu.kma.ucon.engine.pip.entity.Course;
import vn.edu.kma.ucon.engine.pip.entity.Student;
import vn.edu.kma.ucon.engine.pip.repository.AuditLogRepository;
import vn.edu.kma.ucon.engine.pip.repository.ClassSectionRepository;
import vn.edu.kma.ucon.engine.pip.repository.CourseRepository;
import vn.edu.kma.ucon.engine.pip.repository.RegistrationRepository;
import vn.edu.kma.ucon.engine.pip.repository.StudentRepository;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UconEngineApplicationTests {

    @Autowired RegistrationController regController;
    @Autowired StudentRepository studentRepo;
    @Autowired ClassSectionRepository classRepo;
    @Autowired CourseRepository courseRepo;
    @Autowired RegistrationRepository registrationRepo;
    @Autowired AuditLogRepository auditRepo;
    @Autowired PolicyEngine policyEngine;

    @BeforeEach
    void setUp() {
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

        ClassSection cs101Class = new ClassSection();
        cs101Class.setClassId("CS101_01");
        cs101Class.setCourse(courseRepo.findById("CS101").orElseThrow());
        cs101Class.setCapacity(30);
        cs101Class.setEnrolled(10);
        cs101Class.setStatus("OPEN");
        cs101Class.setScheduleSlots("T2_1-3");
        classRepo.save(cs101Class);

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
    @DisplayName("[P11+P12] Happy Path — Dang ky thanh cong, state mutation + audit log")
    void test01_HappyPath_SuccessfulRegistration() {
        System.out.println("  TEST 01 — P11 + P12 | POST_UPDATE");

        UconRequest req = new UconRequest();
        req.setStudentId("SV001");
        req.setClassId("CS102_01");

        ResponseEntity<String> response = regController.register(req);
        assertEquals(200, response.getStatusCode().value());

        Student s = studentRepo.findById("SV001").orElseThrow();
        assertEquals(4, s.getCurrentCredits());
        assertTrue(s.getRegisteredClassIds().contains("CS102_01"));
        assertTrue(s.getRegisteredScheduleSlots().contains("T3_1-3"));

        ClassSection cls = classRepo.findById("CS102_01").orElseThrow();
        assertEquals(5, cls.getEnrolled());
        assertEquals(1, registrationRepo.count());
        assertEquals(1, auditRepo.count());
        assertEquals("ALLOW", auditRepo.findAll().get(0).getDecision());

        System.out.println("  \u2705 TEST 01 PASSED");
    }

    @Test
    @DisplayName("[P01] TuitionPaid — SV chua dong hoc phi bi DENY")
    void test02_P01_TuitionNotPaid_ShouldDeny() {
        System.out.println("  TEST 02 — P01 | PRE_AUTHORIZATION");

        Student sv002 = new Student();
        sv002.setStudentId("SV002");
        sv002.setTuitionPaid(false);
        sv002.setMaxCreditsEffective(15);
        sv002.setCompletedCourses("CS101");
        sv002.setHolds(""); sv002.setRegisteredClassIds(""); sv002.setRegisteredScheduleSlots("");
        studentRepo.save(sv002);

        UconRequest req = new UconRequest();
        req.setStudentId("SV002");
        req.setClassId("CS102_01");

        ResponseEntity<String> response = regController.register(req);
        assertEquals(403, response.getStatusCode().value());
        assertTrue(response.getBody().contains("TUITION_NOT_PAID"));
        assertEquals("DENY", auditRepo.findAll().get(0).getDecision());
        assertEquals(0, registrationRepo.count());

        Student s = studentRepo.findById("SV002").orElseThrow();
        assertEquals(0, s.getCurrentCredits());

        System.out.println("  \u2705 TEST 02 PASSED");
    }

    @Test
    @DisplayName("[P03] ClassStatusOpen — Lop bi LOCKED phai DENY")
    void test03_P03_ClassNotOpen_ShouldDeny() {
        System.out.println("  TEST 03 — P03 | PRE_AUTHORIZATION");

        ClassSection cls = classRepo.findById("CS102_01").orElseThrow();
        cls.setStatus("LOCKED");
        classRepo.save(cls);

        UconRequest req = new UconRequest();
        req.setStudentId("SV001");
        req.setClassId("CS102_01");

        ResponseEntity<String> response = regController.register(req);
        assertEquals(403, response.getStatusCode().value());
        assertTrue(response.getBody().contains("CLASS_NOT_OPEN"));

        System.out.println("  \u2705 TEST 03 PASSED");
    }

    @Test
    @DisplayName("[P04] NotAlreadyRegistered — Khong cho dang ky trung lop")
    void test04_P04_AlreadyRegistered_ShouldDeny() {
        System.out.println("  TEST 04 — P04 | PRE_AUTHORIZATION");

        UconRequest req1 = new UconRequest();
        req1.setStudentId("SV001"); req1.setClassId("CS102_01");
        assertEquals(200, regController.register(req1).getStatusCode().value());

        UconRequest req2 = new UconRequest();
        req2.setStudentId("SV001"); req2.setClassId("CS102_01");
        ResponseEntity<String> second = regController.register(req2);
        assertEquals(403, second.getStatusCode().value());
        assertTrue(second.getBody().contains("ALREADY_REGISTERED"));
        assertEquals(1, registrationRepo.count());

        System.out.println("  \u2705 TEST 04 PASSED");
    }

    @Test
    @DisplayName("[P05] CreditLimit — Vuot han muc tin chi bi DENY")
    void test05_P05_MaxCreditLimit_ShouldDeny() {
        System.out.println("  TEST 05 — P05 | PRE_AUTHORIZATION");

        Student s = studentRepo.findById("SV001").orElseThrow();
        s.setCurrentCredits(12);
        studentRepo.save(s);

        UconRequest req = new UconRequest();
        req.setStudentId("SV001"); req.setClassId("CS102_01");

        ResponseEntity<String> response = regController.register(req);
        assertEquals(403, response.getStatusCode().value());
        assertTrue(response.getBody().contains("CREDIT_LIMIT_EXCEEDED"));

        System.out.println("  \u2705 TEST 05 PASSED");
    }

    @Test
    @DisplayName("[P06] Prerequisite — Thieu mon tien quyet bi DENY")
    void test06_P06_Prerequisite_ShouldDeny() {
        System.out.println("  TEST 06 — P06 | PRE_AUTHORIZATION");

        Student s = studentRepo.findById("SV001").orElseThrow();
        s.setCompletedCourses("");
        studentRepo.save(s);

        UconRequest req = new UconRequest();
        req.setStudentId("SV001"); req.setClassId("CS102_01");

        ResponseEntity<String> response = regController.register(req);
        assertEquals(403, response.getStatusCode().value());
        assertTrue(response.getBody().contains("PREREQUISITE_NOT_MET"));

        System.out.println("  \u2705 TEST 06 PASSED");
    }

    @Test
    @DisplayName("[P07] ScheduleConflict — Trung lich hoc bi DENY")
    void test07_P07_ScheduleConflict_ShouldDeny() {
        System.out.println("  TEST 07 — P07 | PRE_AUTHORIZATION");

        Student s = studentRepo.findById("SV001").orElseThrow();
        s.setRegisteredScheduleSlots("T3_1-3");
        studentRepo.save(s);

        UconRequest req = new UconRequest();
        req.setStudentId("SV001"); req.setClassId("CS102_01");

        ResponseEntity<String> response = regController.register(req);
        assertEquals(403, response.getStatusCode().value());
        assertTrue(response.getBody().contains("SCHEDULE_CONFLICT"));

        System.out.println("  \u2705 TEST 07 PASSED");
    }

    @Test
    @DisplayName("[P10] StudentHold — SV bi cam thi/ky luat bi DENY o ONGOING")
    void test08_P10_StudentOnHold_ShouldDeny() {
        System.out.println("  TEST 08 — P10 | ONGOING_AUTHORIZATION");

        Student s = studentRepo.findById("SV001").orElseThrow();
        s.setHolds("DISCIPLINARY_HOLD");
        studentRepo.save(s);

        UconRequest req = new UconRequest();
        req.setStudentId("SV001"); req.setClassId("CS102_01");

        ResponseEntity<String> response = regController.register(req);
        assertEquals(403, response.getStatusCode().value());
        assertTrue(response.getBody().contains("STUDENT_ON_HOLD"));
        assertEquals(0, registrationRepo.count());

        Student unchanged = studentRepo.findById("SV001").orElseThrow();
        assertEquals(0, unchanged.getCurrentCredits());

        System.out.println("  \u2705 TEST 08 PASSED");
    }

    @Test
    @DisplayName("[P08] CapacityRecheck — Race condition: 2 SV tranh suat cuoi, chi 1 thang")
    void test09_P08_RaceCondition_OptimisticLocking() throws InterruptedException {
        System.out.println("  TEST 09 — P08 | ONGOING_AUTHORIZATION + OptimisticLock");

        Student sv002 = new Student();
        sv002.setStudentId("SV002");
        sv002.setTuitionPaid(true);
        sv002.setMaxCreditsEffective(15);
        sv002.setCompletedCourses("CS101");
        sv002.setHolds(""); sv002.setRegisteredClassIds(""); sv002.setRegisteredScheduleSlots("");
        studentRepo.save(sv002);

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        Runnable registerSV001 = () -> {
            try {
                startLatch.await();
                UconRequest req = new UconRequest();
                req.setStudentId("SV001"); req.setClassId("CS102_01");
                if (regController.register(req).getStatusCode().value() == 200) successCount.incrementAndGet();
                else failCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        };

        Runnable registerSV002 = () -> {
            try {
                startLatch.await();
                UconRequest req = new UconRequest();
                req.setStudentId("SV002"); req.setClassId("CS102_01");
                if (regController.register(req).getStatusCode().value() == 200) successCount.incrementAndGet();
                else failCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        };

        executor.submit(registerSV001);
        executor.submit(registerSV002);
        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertEquals(1, successCount.get());
        assertEquals(1, failCount.get());

        ClassSection cls = classRepo.findById("CS102_01").orElseThrow();
        assertEquals(5, cls.getEnrolled());
        assertEquals(1, registrationRepo.count());

        System.out.println("  \u2705 TEST 09 PASSED");
    }

    @Test
    @DisplayName("[P02] RegistrationWindow — Ngoai dot dang ky bi DENY")
    void test10_P02_OutsideRegistrationWindow_ShouldDeny() {
        System.out.println("  TEST 10 — P02 | PRE_AUTHORIZATION");

        Environment env = new Environment("ADJUSTMENT", "2025-01-01");
        env.setOpenTime("2026-01-01");
        env.setCloseTime("2026-12-31");
        env.setSemester("2026_FALL");

        Student student = studentRepo.findById("SV001").orElseThrow();
        ClassSection cls = classRepo.findById("CS102_01").orElseThrow();

        UconRequest req = new UconRequest();
        req.setRequestId(UUID.randomUUID().toString());
        req.setActionType("REGISTER");
        req.setStudentId("SV001"); req.setClassId("CS102_01");

        AuthDecision decision = policyEngine.evaluatePhase("PRE_AUTHORIZATION", student, cls, env, req);
        assertFalse(decision.isPermit());
        assertEquals("OUTSIDE_REGISTRATION_WINDOW", decision.getFailedCode());

        System.out.println("  \u2705 TEST 10 PASSED");
    }

    @Test
    @DisplayName("[P09] ClassStatusRecheck — Admin khoa lop giua PRE va ONGOING bi DENY")
    void test11_P09_ClassStatusChangedOngoing_ShouldDeny() {
        System.out.println("  TEST 11 — P09 | ONGOING_AUTHORIZATION");

        ClassSection cls = classRepo.findById("CS102_01").orElseThrow();
        cls.setStatus("LOCKED");
        classRepo.save(cls);

        Student student = studentRepo.findById("SV001").orElseThrow();
        ClassSection refreshedCls = classRepo.findById("CS102_01").orElseThrow();

        Environment env = new Environment("NORMAL", "2026-03-27");
        env.setOpenTime("2026-01-01");
        env.setCloseTime("2026-12-31");
        env.setSemester("2026_FALL");

        UconRequest req = new UconRequest();
        req.setRequestId(UUID.randomUUID().toString());
        req.setActionType("REGISTER");
        req.setStudentId("SV001"); req.setClassId("CS102_01");

        AuthDecision decision = policyEngine.evaluatePhase("ONGOING_AUTHORIZATION", student, refreshedCls, env, req);
        assertFalse(decision.isPermit());
        assertEquals("CLASS_STATUS_CHANGED", decision.getFailedCode());

        System.out.println("  \u2705 TEST 11 PASSED");
    }

    @Test
    @DisplayName("[P13] EmergencyMaintenance — Bat bao tri giua ONGOING, request bi chan")
    void test12_P13_EmergencyMaintenance_ShouldDeny() {
        System.out.println("  TEST 12 — P13 | ONGOING_AUTHORIZATION");

        Student student = studentRepo.findById("SV001").orElseThrow();
        ClassSection cls = classRepo.findById("CS102_01").orElseThrow();

        Environment env = new Environment("NORMAL", "2026-03-27");
        env.setOpenTime("2026-01-01");
        env.setCloseTime("2026-12-31");
        env.setSemester("2026_FALL");
        env.setIsMaintenance(true);

        UconRequest req = new UconRequest();
        req.setRequestId(UUID.randomUUID().toString());
        req.setActionType("REGISTER");
        req.setStudentId("SV001"); req.setClassId("CS102_01");

        AuthDecision decision = policyEngine.evaluatePhase("ONGOING_AUTHORIZATION", student, cls, env, req);
        assertFalse(decision.isPermit());
        assertEquals("SYSTEM_UNDER_MAINTENANCE", decision.getFailedCode());

        System.out.println("  \u2705 TEST 12 PASSED");
    }

    @Test
    @DisplayName("[P14] DropStateRevert — DROP thanh cong: enrolled--, credits-=, Registration deleted")
    void test13_P14_DropStateRevert_Success() {
        System.out.println("  TEST 13 — P14 | POST_UPDATE (DROP)");

        UconRequest regReq = new UconRequest();
        regReq.setStudentId("SV001"); regReq.setClassId("CS102_01");
        assertEquals(200, regController.register(regReq).getStatusCode().value());

        Student afterReg = studentRepo.findById("SV001").orElseThrow();
        assertEquals(4, afterReg.getCurrentCredits());
        assertEquals(1, registrationRepo.count());

        UconRequest dropReq = new UconRequest();
        dropReq.setStudentId("SV001"); dropReq.setClassId("CS102_01");
        assertEquals(200, regController.drop(dropReq).getStatusCode().value());

        Student afterDrop = studentRepo.findById("SV001").orElseThrow();
        ClassSection afterDropCls = classRepo.findById("CS102_01").orElseThrow();

        assertEquals(0, afterDrop.getCurrentCredits());
        assertEquals(4, afterDropCls.getEnrolled());
        assertFalse(afterDrop.getRegisteredClassIds().contains("CS102_01"));
        assertFalse(afterDrop.getRegisteredScheduleSlots().contains("T3_1-3"), "slots must be removed");
        assertEquals(0, registrationRepo.count());

        System.out.println("  \u2705 TEST 13 PASSED");
    }

    @Test
    @DisplayName("[P15a] RegisterBilling — REGISTER: tuitionDebt += tuitionFee")
    void test14_P15a_RegisterBilling() {
        System.out.println("  TEST 14 — P15a | POST_UPDATE (REGISTER)");

        assertEquals(0, studentRepo.findById("SV001").orElseThrow().getTuitionDebt());

        UconRequest req = new UconRequest();
        req.setStudentId("SV001"); req.setClassId("CS102_01");
        assertEquals(200, regController.register(req).getStatusCode().value());

        assertEquals(4000000, studentRepo.findById("SV001").orElseThrow().getTuitionDebt());

        System.out.println("  \u2705 TEST 14 PASSED");
    }

    @Test
    @DisplayName("[P15b] DropRefund — DROP: tuitionDebt -= tuitionFee")
    void test15_P15b_DropRefund() {
        System.out.println("  TEST 15 — P15b | POST_UPDATE (DROP)");

        UconRequest regReq = new UconRequest();
        regReq.setStudentId("SV001"); regReq.setClassId("CS102_01");
        assertEquals(200, regController.register(regReq).getStatusCode().value());
        assertEquals(4000000, studentRepo.findById("SV001").orElseThrow().getTuitionDebt());

        UconRequest dropReq = new UconRequest();
        dropReq.setStudentId("SV001"); dropReq.setClassId("CS102_01");
        assertEquals(200, regController.drop(dropReq).getStatusCode().value());
        assertEquals(0, studentRepo.findById("SV001").orElseThrow().getTuitionDebt());

        System.out.println("  \u2705 TEST 15 PASSED");
    }
}
