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
        courseRepo.save(cs101);

        Course cs102 = new Course();
        cs102.setCourseId("CS102");
        cs102.setCredits(4);
        cs102.setPrerequisites("CS101");
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
    @DisplayName("[P11+P12] Happy Path — Đăng ký thành công, state mutation + audit log")
    void test01_HappyPath_SuccessfulRegistration() {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println(  "║  TEST 01 — P11 + P12 │ Phase: POST_UPDATE                   ║");
        System.out.println(  "║  Kịch bản : SV001 đăng ký CS102_01 (đủ điều kiện)           ║");
        System.out.println(  "║  Kỳ vọng  : HTTP 200, enrolled++, credits+=4, AuditLog=ALLOW ║");
        System.out.println(  "╚══════════════════════════════════════════════════════════════╝");

        UconRequest req = new UconRequest();
        req.setStudentId("SV001");
        req.setClassId("CS102_01");

        ResponseEntity<String> response = regController.register(req);
        assertEquals(200, response.getStatusCode().value(), "Expected 200 OK");
        System.out.println("  → HTTP Status : " + response.getStatusCode().value() + " ✅");
        System.out.println("  → Body        : " + response.getBody());

        Student updatedStudent = studentRepo.findById("SV001").orElseThrow();
        assertEquals(4, updatedStudent.getCurrentCredits(), "Credits must increment by 4");
        assertTrue(updatedStudent.getRegisteredClassIds().contains("CS102_01"), "registeredClassIds must contain CS102_01");
        assertTrue(updatedStudent.getRegisteredScheduleSlots().contains("T3_1-3"), "Slots must be appended");
        System.out.println("  → currentCredits    : " + updatedStudent.getCurrentCredits() + " (0 → 4) ✅");
        System.out.println("  → registeredClassIds: " + updatedStudent.getRegisteredClassIds() + " ✅");

        ClassSection updatedClass = classRepo.findById("CS102_01").orElseThrow();
        assertEquals(5, updatedClass.getEnrolled(), "Enrolled must increment to 5");
        System.out.println("  → enrolled          : " + updatedClass.getEnrolled() + " (4 → 5) ✅");

        assertEquals(1, registrationRepo.count(), "P11 must create 1 Registration record");
        System.out.println("  → Registration rows : " + registrationRepo.count() + " ✅");

        assertEquals(1, auditRepo.count(), "P12 must create 1 AuditLog record");
        assertEquals("ALLOW", auditRepo.findAll().get(0).getDecision(), "AuditLog decision must be ALLOW");
        System.out.println("  → AuditLog decision : " + auditRepo.findAll().get(0).getDecision() + " ✅");
        System.out.println("  ✅ TEST 01 PASSED\n");
    }

    @Test
    @DisplayName("[P01] TuitionPaid — Sinh viên chưa đóng học phí bị DENY")
    void test02_P01_TuitionNotPaid_ShouldDeny() {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println(  "║  TEST 02 — P01 │ Phase: PRE_AUTHORIZATION                    ║");
        System.out.println(  "║  Kịch bản : SV002 chưa đóng học phí (tuitionPaid=false)      ║");
        System.out.println(  "║  Kỳ vọng  : HTTP 403, TUITION_NOT_PAID, không mutate state   ║");
        System.out.println(  "╚══════════════════════════════════════════════════════════════╝");

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
        System.out.println("  → HTTP Status : " + response.getStatusCode().value() + " ✅");
        System.out.println("  → Body        : " + body02);

        assertEquals(1, auditRepo.count(), "P12 must write AuditLog even on DENY");
        assertEquals("DENY", auditRepo.findAll().get(0).getDecision());
        System.out.println("  → AuditLog written (DENY isolation): decision=" + auditRepo.findAll().get(0).getDecision() + " ✅");

        assertEquals(0, registrationRepo.count(), "P11 must NOT create Registration on DENY");
        Student unchanged = studentRepo.findById("SV002").orElseThrow();
        assertEquals(0, unchanged.getCurrentCredits(), "Credits must NOT change on DENY");
        assertEquals("", unchanged.getRegisteredClassIds(), "registeredClassIds must NOT change on DENY");
        ClassSection unchangedClass = classRepo.findById("CS102_01").orElseThrow();
        assertEquals(4, unchangedClass.getEnrolled(), "Enrolled must NOT change on DENY");
        System.out.println("  → State NOT mutated: credits=0, enrolled=4 ✅");
        System.out.println("  ✅ TEST 02 PASSED\n");
    }

    @Test
    @DisplayName("[P03] ClassStatusOpen — Lớp bị LOCKED phải DENY")
    void test03_P03_ClassNotOpen_ShouldDeny() {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println(  "║  TEST 03 — P03 │ Phase: PRE_AUTHORIZATION                    ║");
        System.out.println(  "║  Kịch bản : CS102_01 bị Admin khóa (status=LOCKED)           ║");
        System.out.println(  "║  Kỳ vọng  : HTTP 403, CLASS_NOT_OPEN                         ║");
        System.out.println(  "╚══════════════════════════════════════════════════════════════╝");

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
        System.out.println("  → HTTP Status : " + response.getStatusCode().value() + " ✅");
        System.out.println("  → Body        : " + body03);
        System.out.println("  ✅ TEST 03 PASSED\n");
    }

    @Test
    @DisplayName("[P04] NotAlreadyRegistered — Không cho đăng ký trùng lớp")
    void test04_P04_AlreadyRegistered_ShouldDeny() {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println(  "║  TEST 04 — P04 │ Phase: PRE_AUTHORIZATION                    ║");
        System.out.println(  "║  Kịch bản : SV001 đăng ký CS102_01 lần 2 (đã đăng ký rồi)  ║");
        System.out.println(  "║  Kỳ vọng  : Lần 1 → 200, Lần 2 → 403 ALREADY_REGISTERED     ║");
        System.out.println(  "╚══════════════════════════════════════════════════════════════╝");

        UconRequest req1 = new UconRequest();
        req1.setStudentId("SV001");
        req1.setClassId("CS102_01");
        ResponseEntity<String> first = regController.register(req1);
        assertEquals(200, first.getStatusCode().value(), "First registration must succeed");
        System.out.println("  → Lần 1: HTTP " + first.getStatusCode().value() + " ✅");

        UconRequest req2 = new UconRequest();
        req2.setStudentId("SV001");
        req2.setClassId("CS102_01");
        ResponseEntity<String> second = regController.register(req2);
        assertEquals(403, second.getStatusCode().value());
        String body04 = second.getBody();
        assertNotNull(body04, "Response body must not be null");
        assertTrue(body04.contains("ALREADY_REGISTERED"), "Must deny with ALREADY_REGISTERED");
        System.out.println("  → Lần 2: HTTP " + second.getStatusCode().value() + " ✅");
        System.out.println("  → Body  : " + body04);

        assertEquals(1, registrationRepo.count(), "Must have exactly 1 Registration record");
        System.out.println("  → Registration rows: 1 (không bị duplicate) ✅");
        System.out.println("  ✅ TEST 04 PASSED\n");
    }

    @Test
    @DisplayName("[P05] CreditLimit — Vượt hạn mức tín chỉ bị DENY")
    void test05_P05_MaxCreditLimit_ShouldDeny() {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println(  "║  TEST 05 — P05 │ Phase: PRE_AUTHORIZATION                    ║");
        System.out.println(  "║  Kịch bản : currentCredits=12, CS102=4TC → 12+4=16 > 15 max ║");
        System.out.println(  "║  Kỳ vọng  : HTTP 403, CREDIT_LIMIT_EXCEEDED                  ║");
        System.out.println(  "╚══════════════════════════════════════════════════════════════╝");

        Student heavyStudent = studentRepo.findById("SV001").orElseThrow();
        heavyStudent.setCurrentCredits(12);
        studentRepo.save(heavyStudent);
        System.out.println("  → currentCredits set to 12 (12 + 4 = 16 > maxCreditsEffective=15)");

        UconRequest req = new UconRequest();
        req.setStudentId("SV001");
        req.setClassId("CS102_01");

        ResponseEntity<String> response = regController.register(req);
        assertEquals(403, response.getStatusCode().value());
        String body05 = response.getBody();
        assertNotNull(body05, "Response body must not be null");
        assertTrue(body05.contains("CREDIT_LIMIT_EXCEEDED"));
        System.out.println("  → HTTP Status : " + response.getStatusCode().value() + " ✅");
        System.out.println("  → Body        : " + body05);
        System.out.println("  ✅ TEST 05 PASSED\n");
    }

    @Test
    @DisplayName("[P06] Prerequisite — Thiếu môn tiên quyết bị DENY")
    void test06_P06_Prerequisite_ShouldDeny() {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println(  "║  TEST 06 — P06 │ Phase: PRE_AUTHORIZATION                    ║");
        System.out.println(  "║  Kịch bản : CS102 yêu cầu CS101, SV001 chưa học CS101        ║");
        System.out.println(  "║  Kỳ vọng  : HTTP 403, PREREQUISITE_NOT_MET                   ║");
        System.out.println(  "╚══════════════════════════════════════════════════════════════╝");

        Student newbie = studentRepo.findById("SV001").orElseThrow();
        newbie.setCompletedCourses("");
        studentRepo.save(newbie);
        System.out.println("  → completedCourses set to '' (xóa CS101 khỏi danh sách)");

        UconRequest req = new UconRequest();
        req.setStudentId("SV001");
        req.setClassId("CS102_01");

        ResponseEntity<String> response = regController.register(req);
        assertEquals(403, response.getStatusCode().value());
        String body06 = response.getBody();
        assertNotNull(body06, "Response body must not be null");
        assertTrue(body06.contains("PREREQUISITE_NOT_MET"));
        System.out.println("  → HTTP Status : " + response.getStatusCode().value() + " ✅");
        System.out.println("  → Body        : " + body06);
        System.out.println("  ✅ TEST 06 PASSED\n");
    }

    @Test
    @DisplayName("[P07] ScheduleConflict — Trùng lịch học bị DENY")
    void test07_P07_ScheduleConflict_ShouldDeny() {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println(  "║  TEST 07 — P07 │ Phase: PRE_AUTHORIZATION                    ║");
        System.out.println(  "║  Kịch bản : SV001 đã có slot T3_1-3, CS102_01 cũng dạy T3   ║");
        System.out.println(  "║  Kỳ vọng  : HTTP 403, SCHEDULE_CONFLICT                      ║");
        System.out.println(  "╚══════════════════════════════════════════════════════════════╝");

        Student busyStudent = studentRepo.findById("SV001").orElseThrow();
        busyStudent.setRegisteredScheduleSlots("T3_1-3");
        studentRepo.save(busyStudent);
        System.out.println("  → registeredScheduleSlots: T3_1-3 (trùng với CS102_01: T3_1-3,T5_4-6)");

        UconRequest req = new UconRequest();
        req.setStudentId("SV001");
        req.setClassId("CS102_01");

        ResponseEntity<String> response = regController.register(req);
        assertEquals(403, response.getStatusCode().value());
        String body07 = response.getBody();
        assertNotNull(body07, "Response body must not be null");
        assertTrue(body07.contains("SCHEDULE_CONFLICT"));
        System.out.println("  → HTTP Status : " + response.getStatusCode().value() + " ✅");
        System.out.println("  → Body        : " + body07);
        System.out.println("  ✅ TEST 07 PASSED\n");
    }

    @Test
    @DisplayName("[P10] StudentHold — Sinh viên bị cấm thi/kỷ luật bị DENY ở ONGOING")
    void test08_P10_StudentOnHold_ShouldDeny() {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println(  "║  TEST 08 — P10 │ Phase: ONGOING_AUTHORIZATION                ║");
        System.out.println(  "║  Kịch bản : SV001 bị thêm DISCIPLINARY_HOLD sau khi qua PRE ║");
        System.out.println(  "║  Kỳ vọng  : HTTP 403, STUDENT_ON_HOLD, state không thay đổi ║");
        System.out.println(  "╚══════════════════════════════════════════════════════════════╝");

        Student heldStudent = studentRepo.findById("SV001").orElseThrow();
        heldStudent.setHolds("DISCIPLINARY_HOLD");
        studentRepo.save(heldStudent);
        System.out.println("  → holds: DISCIPLINARY_HOLD (bị ban)");

        UconRequest req = new UconRequest();
        req.setStudentId("SV001");
        req.setClassId("CS102_01");

        ResponseEntity<String> response = regController.register(req);
        assertEquals(403, response.getStatusCode().value());
        String body08 = response.getBody();
        assertNotNull(body08, "Response body must not be null");
        assertTrue(body08.contains("STUDENT_ON_HOLD"), "Must deny with STUDENT_ON_HOLD");
        System.out.println("  → HTTP Status : " + response.getStatusCode().value() + " ✅");
        System.out.println("  → Body        : " + body08);

        assertEquals(0, registrationRepo.count(), "P11 must NOT run on ONGOING DENY");
        Student unchanged = studentRepo.findById("SV001").orElseThrow();
        assertEquals(0, unchanged.getCurrentCredits(), "Credits must not change on ONGOING DENY");
        System.out.println("  → State NOT mutated: credits=0, registration=0 ✅");
        System.out.println("  ✅ TEST 08 PASSED\n");
    }

    @Test
    @DisplayName("[P08] CapacityRecheck — Race condition: 2 thread tranh suất cuối, chỉ 1 thắng")
    void test09_P08_RaceCondition_OptimisticLocking() throws InterruptedException {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println(  "║  TEST 09 — P08 │ Phase: ONGOING_AUTHORIZATION + OptimisticLock║");
        System.out.println(  "║  Kịch bản : enrolled=4/capacity=5, 2 SV đăng ký đồng thời   ║");
        System.out.println(  "║  Kỳ vọng  : 1 thành công (200), 1 thất bại (403/409)         ║");
        System.out.println(  "╚══════════════════════════════════════════════════════════════╝");

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

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertEquals(1, successCount.get(), "Exactly 1 thread should claim the last seat");
        assertEquals(1, failCount.get(), "Exactly 1 thread must be denied");
        System.out.println("  → Thread thành công: " + successCount.get() + " ✅");
        System.out.println("  → Thread bị từ chối: " + failCount.get() + " ✅");

        ClassSection c2 = classRepo.findById("CS102_01").orElseThrow();
        assertEquals(5, c2.getEnrolled(), "Enrolled must not exceed capacity (5)");
        System.out.println("  → enrolled sau race: " + c2.getEnrolled() + " (không vượt capacity=5) ✅");

        assertEquals(1, registrationRepo.count(), "Only 1 Registration record must be created");
        System.out.println("  → Registration rows: 1 (không duplicate) ✅");
        System.out.println("  ✅ TEST 09 PASSED\n");
    }

    @Test
    @DisplayName("[P02] RegistrationWindow — Ngoài đợt đăng ký bị DENY")
    void test10_P02_OutsideRegistrationWindow_ShouldDeny() {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println(  "║  TEST 10 — P02 │ Phase: PRE_AUTHORIZATION                    ║");
        System.out.println(  "║  Kịch bản : phase=ADJUSTMENT, date=2025-01-01 (ngoài window) ║");
        System.out.println(  "║  Kỳ vọng  : DENY, failedCode=OUTSIDE_REGISTRATION_WINDOW     ║");
        System.out.println(  "╚══════════════════════════════════════════════════════════════╝");

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

        vn.edu.kma.ucon.engine.pdp.AuthDecision decision =
                policyEngine.evaluatePhase("PRE_AUTHORIZATION", student, cls, closedEnv, req);

        assertFalse(decision.isPermit(), "P02 must DENY when outside registration window");
        assertEquals("OUTSIDE_REGISTRATION_WINDOW", decision.getFailedCode(),
                "FailedCode must be OUTSIDE_REGISTRATION_WINDOW");
        System.out.println("  → permit        : " + decision.isPermit() + " ✅");
        System.out.println("  → failedCode    : " + decision.getFailedCode() + " ✅");
        System.out.println("  ✅ TEST 10 PASSED\n");
    }

    @Test
    @DisplayName("[P09] ClassStatusRecheck — Admin khóa lớp giữa PRE và ONGOING bị DENY")
    void test11_P09_ClassStatusChangedOngoing_ShouldDeny() {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println(  "║  TEST 11 — P09 │ Phase: ONGOING_AUTHORIZATION                ║");
        System.out.println(  "║  Kịch bản : Admin khóa lớp sau khi request đã qua PRE phase  ║");
        System.out.println(  "║  Kỳ vọng  : DENY, failedCode=CLASS_STATUS_CHANGED             ║");
        System.out.println(  "╚══════════════════════════════════════════════════════════════╝");

        ClassSection lockedCls = classRepo.findById("CS102_01").orElseThrow();
        lockedCls.setStatus("LOCKED");
        classRepo.save(lockedCls);
        System.out.println("  → CS102_01 status changed to LOCKED (bởi Admin)");

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

        vn.edu.kma.ucon.engine.pdp.AuthDecision decision =
                policyEngine.evaluatePhase("ONGOING_AUTHORIZATION", student, refreshedCls, env, req);

        assertFalse(decision.isPermit(), "P09 must DENY when class is LOCKED in ONGOING phase");
        assertEquals("CLASS_STATUS_CHANGED", decision.getFailedCode(),
                "FailedCode must be CLASS_STATUS_CHANGED");
        System.out.println("  → permit     : " + decision.isPermit() + " ✅");
        System.out.println("  → failedCode : " + decision.getFailedCode() + " ✅");
        System.out.println("  ✅ TEST 11 PASSED\n");
    }
}
