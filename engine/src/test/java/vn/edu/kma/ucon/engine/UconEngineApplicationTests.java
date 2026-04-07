package vn.edu.kma.ucon.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import vn.edu.kma.ucon.engine.pep.RegistrationController;
import vn.edu.kma.ucon.engine.pep.UconRequest;
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UconEngineApplicationTests {

    @Autowired private RegistrationController regController;
    @Autowired private PolicyEngine policyEngine;
    @Autowired private StudentRepository studentRepo;
    @Autowired private ClassSectionRepository classRepo;
    @Autowired private CourseRepository courseRepo;
    @Autowired private AuditLogRepository auditRepo;
    @Autowired private RegistrationRepository registrationRepo;

    @BeforeEach
    void setUp() {
        // Full cleanup before each test for complete isolation
        auditRepo.deleteAll();
        registrationRepo.deleteAll();
        studentRepo.deleteAll();
        classRepo.deleteAll();
        courseRepo.deleteAll();

        // Courses
        Course cs101 = new Course("CS101", 3, "");
        Course cs102 = new Course("CS102", 4, "CS101"); // prereq: CS101
        courseRepo.save(cs101);
        courseRepo.save(cs102);

        // Classes
        ClassSection c1 = new ClassSection();
        c1.setClassId("CS101_01");
        c1.setCourse(cs101);
        c1.setCapacity(50);
        c1.setEnrolled(0);
        c1.setStatus("OPEN");
        c1.setScheduleSlots("T2_1-3,T4_4-6");
        classRepo.save(c1);

        ClassSection c2 = new ClassSection();
        c2.setClassId("CS102_01");
        c2.setCourse(cs102);
        c2.setCapacity(5);
        c2.setEnrolled(4); // 1 slot left — for Race Condition test
        c2.setStatus("OPEN");
        c2.setScheduleSlots("T3_1-3,T5_4-6");
        classRepo.save(c2);

        // Default happy-path student (SV001)
        Student sv001 = new Student();
        sv001.setStudentId("SV001");
        sv001.setCurrentCredits(0);
        sv001.setTuitionPaid(true);
        sv001.setAcademicWarning(false);
        sv001.setMaxCreditsEffective(15);
        sv001.setCompletedCourses("CS101");
        sv001.setRegisteredScheduleSlots("");
        sv001.setRegisteredClassIds("");
        sv001.setHolds("");
        studentRepo.save(sv001);
    }

    // =========================================================================
    // TEST 01 — Happy Path: full ALLOW flow (P11 + P12 verified)
    // =========================================================================
    @Test
    void test01_HappyPath_SuccessfulRegistration() {
        UconRequest req = new UconRequest();
        req.setStudentId("SV001");
        req.setClassId("CS102_01");

        ResponseEntity<String> response = regController.register(req);
        assertEquals(200, response.getStatusCode().value(), "Expected 200 OK");

        // P11: State mutations via DSL
        Student updatedStudent = studentRepo.findById("SV001").orElseThrow();
        assertEquals(4, updatedStudent.getCurrentCredits(), "Credits must increment by 4");
        assertTrue(updatedStudent.getRegisteredClassIds().contains("CS102_01"), "registeredClassIds must contain CS102_01");
        assertTrue(updatedStudent.getRegisteredScheduleSlots().contains("T3_1-3"), "Slots must be appended");

        ClassSection updatedClass = classRepo.findById("CS102_01").orElseThrow();
        assertEquals(5, updatedClass.getEnrolled(), "Enrolled must increment to 5");

        // P11: Registration transaction persisted by DSL (not hardcoded)
        assertEquals(1, registrationRepo.count(), "DSL P11 must create 1 Registration record");

        // P12: AuditLog written by DSL (not hardcoded)
        assertEquals(1, auditRepo.count(), "DSL P12 must create 1 AuditLog record");
        assertEquals("ALLOW", auditRepo.findAll().get(0).getDecision(), "AuditLog decision must be ALLOW");
    }

    // =========================================================================
    // TEST 02 — P01: Tuition not paid → DENY (also verifies DENY audit isolation)
    // =========================================================================
    @Test
    void test02_P01_TuitionNotPaid_ShouldDeny() {
        Student badStudent = new Student();
        badStudent.setStudentId("SV002");
        badStudent.setTuitionPaid(false);
        badStudent.setMaxCreditsEffective(15);
        badStudent.setCompletedCourses("CS101");
        badStudent.setHolds(""); badStudent.setRegisteredClassIds(""); badStudent.setRegisteredScheduleSlots("");
        studentRepo.save(badStudent);

        UconRequest req = new UconRequest();
        req.setStudentId("SV002");
        req.setClassId("CS102_01");

        ResponseEntity<String> response = regController.register(req);
        assertEquals(403, response.getStatusCode().value());
        String body02 = response.getBody();
        assertNotNull(body02, "Response body must not be null");
        assertTrue(body02.contains("TUITION_NOT_PAID"), "Must deny with TUITION_NOT_PAID");

        // P12 AuditLog written on DENY
        assertEquals(1, auditRepo.count(), "P12 must write AuditLog even on DENY");
        assertEquals("DENY", auditRepo.findAll().get(0).getDecision());

        // CRITICAL: P11 must NOT run on DENY — no Registration record, no state mutation
        assertEquals(0, registrationRepo.count(), "P11 must NOT create Registration on DENY");
        Student unchanged = studentRepo.findById("SV002").orElseThrow();
        assertEquals(0, unchanged.getCurrentCredits(), "Credits must NOT change on DENY");
        assertEquals("", unchanged.getRegisteredClassIds(), "registeredClassIds must NOT change on DENY");
        ClassSection unchangedClass = classRepo.findById("CS102_01").orElseThrow();
        assertEquals(4, unchangedClass.getEnrolled(), "Enrolled must NOT change on DENY");
    }

    // =========================================================================
    // TEST 03 — P03: Class not OPEN → DENY
    // =========================================================================
    @Test
    void test03_P03_ClassNotOpen_ShouldDeny() {
        ClassSection lockedClass = classRepo.findById("CS102_01").orElseThrow();
        lockedClass.setStatus("LOCKED");
        classRepo.save(lockedClass);

        UconRequest req = new UconRequest();
        req.setStudentId("SV001");
        req.setClassId("CS102_01");

        ResponseEntity<String> response = regController.register(req);
        assertEquals(403, response.getStatusCode().value());
        String body03 = response.getBody();
        assertNotNull(body03, "Response body must not be null");
        assertTrue(body03.contains("CLASS_NOT_OPEN"), "Must deny with CLASS_NOT_OPEN");
    }

    // =========================================================================
    // TEST 04 — P04: Already registered → DENY
    // =========================================================================
    @Test
    void test04_P04_AlreadyRegistered_ShouldDeny() {
        // Register once successfully
        UconRequest req1 = new UconRequest();
        req1.setStudentId("SV001");
        req1.setClassId("CS102_01");
        ResponseEntity<String> first = regController.register(req1);
        assertEquals(200, first.getStatusCode().value(), "First registration must succeed");

        // Try to register the same class again
        UconRequest req2 = new UconRequest();
        req2.setStudentId("SV001");
        req2.setClassId("CS102_01");
        ResponseEntity<String> second = regController.register(req2);
        assertEquals(403, second.getStatusCode().value());
        String body04 = second.getBody();
        assertNotNull(body04, "Response body must not be null");
        assertTrue(body04.contains("ALREADY_REGISTERED"), "Must deny with ALREADY_REGISTERED");

        // Only 1 Registration record total (from succesful first attempt)
        assertEquals(1, registrationRepo.count(), "Must have exactly 1 Registration record");
    }

    // =========================================================================
    // TEST 05 — P05: Credit limit exceeded → DENY
    // =========================================================================
    @Test
    void test05_P05_MaxCreditLimit_ShouldDeny() {
        Student heavyStudent = studentRepo.findById("SV001").orElseThrow();
        heavyStudent.setCurrentCredits(12); // 12 + 4 (CS102) = 16 > 15
        studentRepo.save(heavyStudent);

        UconRequest req = new UconRequest();
        req.setStudentId("SV001");
        req.setClassId("CS102_01");

        ResponseEntity<String> response = regController.register(req);
        assertEquals(403, response.getStatusCode().value());
        String body05 = response.getBody();
        assertNotNull(body05, "Response body must not be null");
        assertTrue(body05.contains("CREDIT_LIMIT_EXCEEDED"));
    }

    // =========================================================================
    // TEST 06 — P06: Prerequisite not met → DENY
    // =========================================================================
    @Test
    void test06_P06_Prerequisite_ShouldDeny() {
        Student newbie = studentRepo.findById("SV001").orElseThrow();
        newbie.setCompletedCourses(""); // Missing CS101
        studentRepo.save(newbie);

        UconRequest req = new UconRequest();
        req.setStudentId("SV001");
        req.setClassId("CS102_01"); // Requires CS101

        ResponseEntity<String> response = regController.register(req);
        assertEquals(403, response.getStatusCode().value());
        String body06 = response.getBody();
        assertNotNull(body06, "Response body must not be null");
        assertTrue(body06.contains("PREREQUISITE_NOT_MET"));
    }

    // =========================================================================
    // TEST 07 — P07: Schedule conflict → DENY
    // =========================================================================
    @Test
    void test07_P07_ScheduleConflict_ShouldDeny() {
        Student busyStudent = studentRepo.findById("SV001").orElseThrow();
        busyStudent.setRegisteredScheduleSlots("T3_1-3"); // Conflicts with CS102_01 (T3_1-3,T5_4-6)
        studentRepo.save(busyStudent);

        UconRequest req = new UconRequest();
        req.setStudentId("SV001");
        req.setClassId("CS102_01");

        ResponseEntity<String> response = regController.register(req);
        assertEquals(403, response.getStatusCode().value());
        String body07 = response.getBody();
        assertNotNull(body07, "Response body must not be null");
        assertTrue(body07.contains("SCHEDULE_CONFLICT"));
    }

    // =========================================================================
    // TEST 08 — P10: Student on hold → DENY (ONGOING phase)
    // =========================================================================
    @Test
    void test08_P10_StudentOnHold_ShouldDeny() {
        Student heldStudent = studentRepo.findById("SV001").orElseThrow();
        heldStudent.setHolds("DISCIPLINARY_HOLD"); // Non-empty hold
        studentRepo.save(heldStudent);

        UconRequest req = new UconRequest();
        req.setStudentId("SV001");
        req.setClassId("CS102_01");

        ResponseEntity<String> response = regController.register(req);
        assertEquals(403, response.getStatusCode().value());
        String body08 = response.getBody();
        assertNotNull(body08, "Response body must not be null");
        assertTrue(body08.contains("STUDENT_ON_HOLD"), "Must deny with STUDENT_ON_HOLD");

        // State must be untouched on DENY
        assertEquals(0, registrationRepo.count(), "P11 must NOT run on ONGOING DENY");
        Student unchanged = studentRepo.findById("SV001").orElseThrow();
        assertEquals(0, unchanged.getCurrentCredits(), "Credits must not change on ONGOING DENY");
    }

    // =========================================================================
    // TEST 09 — P08 + Optimistic Locking: Race Condition on last seat
    // =========================================================================
    @Test
    void test09_P08_RaceCondition_OptimisticLocking() throws InterruptedException {
        // CS102_01 has enrolled=4, capacity=5 (exactly 1 slot left)
        // Two different students race to claim the last seat simultaneously.
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
        CountDownLatch doneLatch  = new CountDownLatch(threads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount    = new AtomicInteger(0);

        Runnable registerSV001 = () -> {
            try {
                startLatch.await();
                UconRequest req = new UconRequest();
                req.setStudentId("SV001");
                req.setClassId("CS102_01");
                ResponseEntity<String> res = regController.register(req);
                if (res.getStatusCode().value() == 200) successCount.incrementAndGet();
                else failCount.incrementAndGet();
            } catch (ObjectOptimisticLockingFailureException e) {
                failCount.incrementAndGet();
            } catch (Exception e) {
                // Optimistic lock may be wrapped inside a transaction rollback exception
                failCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        };

        Runnable registerSV002 = () -> {
            try {
                startLatch.await();
                UconRequest req = new UconRequest();
                req.setStudentId("SV002");
                req.setClassId("CS102_01");
                ResponseEntity<String> res = regController.register(req);
                if (res.getStatusCode().value() == 200) successCount.incrementAndGet();
                else failCount.incrementAndGet();
            } catch (ObjectOptimisticLockingFailureException e) {
                failCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        };

        executor.submit(registerSV001);
        executor.submit(registerSV002);

        startLatch.countDown(); // Release both threads simultaneously
        doneLatch.await();      // Wait for both to finish
        executor.shutdown();

        // Exactly 1 must succeed, 1 must fail
        assertEquals(1, successCount.get(), "Exactly 1 thread should claim the last seat");
        assertEquals(1, failCount.get(), "Exactly 1 thread must be denied");

        // DB must not exceed capacity
        ClassSection c2 = classRepo.findById("CS102_01").orElseThrow();
        assertEquals(5, c2.getEnrolled(), "Enrolled must not exceed capacity (5)");

        // Only 1 Registration record may exist
        assertEquals(1, registrationRepo.count(), "Only 1 Registration record must be created");
    }

    // =========================================================================
    // TEST 10 — P02: Outside registration window → DENY
    // =========================================================================
    @Test
    void test10_P02_OutsideRegistrationWindow_ShouldDeny() {
        // The RegistrationController hardcodes environment with:
        //   phase="NORMAL", currentDateTime="2026-03-27", openTime="2026-01-01", closeTime="2026-12-31"
        // To simulate OUTSIDE_REGISTRATION_WINDOW, we would need to control Environment.
        // Since RegistrationController creates Environment internally, we verify P02
        // passes in normal conditions (happy path) and document that P02 blocks
        // requests outside window at the architecture level.
        //
        // Architecture-level verification: In the happy path (test01), the Environment is
        // set to phase=NORMAL, date within [openTime, closeTime] → P02 PERMITS.
        // If phase were "ADJUSTMENT" or date were out of range → P02 would DENY.
        // This is verified by directly calling evaluatePhase on a custom environment.

        Environment closedEnv = new Environment("ADJUSTMENT", "2025-01-01");
        closedEnv.setOpenTime("2026-01-01");
        closedEnv.setCloseTime("2026-12-31");
        closedEnv.setSemester("2026_FALL");

        Student student = studentRepo.findById("SV001").orElseThrow();
        ClassSection cls  = classRepo.findById("CS102_01").orElseThrow();

        UconRequest req = new UconRequest();
        req.setRequestId(java.util.UUID.randomUUID().toString());
        req.setActionType("REGISTER");
        req.setStudentId("SV001");
        req.setClassId("CS102_01");

        // Call PolicyEngine directly to evaluate PRE with out-of-window Environment
        vn.edu.kma.ucon.engine.pdp.AuthDecision decision =
                policyEngine.evaluatePhase("PRE_AUTHORIZATION", student, cls, closedEnv, req);

        assertFalse(decision.isPermit(), "P02 must DENY when outside registration window");
        assertEquals("OUTSIDE_REGISTRATION_WINDOW", decision.getFailedCode(),
                "FailedCode must be OUTSIDE_REGISTRATION_WINDOW");
    }

    // =========================================================================
    // TEST 11 — P09: Class status changed to LOCKED during ONGOING → DENY
    // =========================================================================
    @Test
    void test11_P09_ClassStatusChangedOngoing_ShouldDeny() {
        // P09 (ONGOING) rechecks class status AFTER entityManager.refresh().
        // To test P09 independently of P03 (PRE):
        // We call PolicyEngine.evaluatePhase("ONGOING_AUTHORIZATION") directly
        // with a LOCKED class, simulating that Admin locked it between PRE and ONGOING.

        ClassSection lockedCls = classRepo.findById("CS102_01").orElseThrow();
        lockedCls.setStatus("LOCKED");
        classRepo.save(lockedCls);

        Student student = studentRepo.findById("SV001").orElseThrow();
        ClassSection refreshedCls = classRepo.findById("CS102_01").orElseThrow();

        Environment env = new Environment("NORMAL", "2026-03-27");
        env.setOpenTime("2026-01-01");
        env.setCloseTime("2026-12-31");
        env.setSemester("2026_FALL");

        UconRequest req = new UconRequest();
        req.setRequestId(java.util.UUID.randomUUID().toString());
        req.setActionType("REGISTER");
        req.setStudentId("SV001");
        req.setClassId("CS102_01");

        // Evaluate ONLY the ONGOING phase — simulates request that passed PRE
        // but class was locked by Admin between the two phases
        vn.edu.kma.ucon.engine.pdp.AuthDecision decision =
                policyEngine.evaluatePhase("ONGOING_AUTHORIZATION", student, refreshedCls, env, req);

        assertFalse(decision.isPermit(), "P09 must DENY when class is LOCKED in ONGOING phase");
        assertEquals("CLASS_STATUS_CHANGED", decision.getFailedCode(),
                "FailedCode must be CLASS_STATUS_CHANGED");
    }
}
