package vn.edu.kma.ucon.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import vn.edu.kma.ucon.engine.pep.ApiDecisionResponse;
import vn.edu.kma.ucon.engine.pep.RegistrationController;
import vn.edu.kma.ucon.engine.pep.UconRequest;
import vn.edu.kma.ucon.engine.pdp.AuthDecision;
import vn.edu.kma.ucon.engine.pdp.AttributeSchema;
import vn.edu.kma.ucon.engine.pdp.DecisionTrace;
import vn.edu.kma.ucon.engine.pdp.Environment;
import vn.edu.kma.ucon.engine.pdp.MaintenanceFlag;
import vn.edu.kma.ucon.engine.pdp.PolicyAnalysisReport;
import vn.edu.kma.ucon.engine.pdp.PolicyAnalyzer;
import vn.edu.kma.ucon.engine.pdp.PolicyDecisionPoint;
import vn.edu.kma.ucon.engine.pdp.PolicyEngine;
import vn.edu.kma.ucon.engine.pdp.PolicyFunctionRegistry;
import vn.edu.kma.ucon.engine.pdp.PolicyModelSemanticValidator;
import vn.edu.kma.ucon.engine.pdp.PolicyValidator;
import vn.edu.kma.ucon.engine.pip.entity.ClassSection;
import vn.edu.kma.ucon.engine.pip.entity.Course;
import vn.edu.kma.ucon.engine.pip.entity.Registration;
import vn.edu.kma.ucon.engine.pip.entity.Student;
import vn.edu.kma.ucon.engine.pip.repository.AuditLogRepository;
import vn.edu.kma.ucon.engine.pip.repository.ClassSectionRepository;
import vn.edu.kma.ucon.engine.pip.repository.CourseRepository;
import vn.edu.kma.ucon.engine.pip.repository.RegistrationRepository;
import vn.edu.kma.ucon.engine.pip.repository.StudentRepository;

@SpringBootTest
class UconEngineApplicationTests {

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
    @Autowired
    PolicyDecisionPoint policyDecisionPoint;
    @Autowired
    PolicyModelSemanticValidator semanticValidator;
    @Autowired
    PolicyFunctionRegistry functionRegistry;
    @Autowired
    PolicyValidator policyValidator;
    @Autowired
    PolicyAnalyzer policyAnalyzer;
    @Autowired
    AttributeSchema attributeSchema;

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
        cs102Class.setReservedSeats(0);
        cs102Class.setStatus("OPEN");
        cs102Class.setScheduleSlots("T3_1-3,T5_4-6");
        classRepo.save(cs102Class);

        ClassSection cs101Class = new ClassSection();
        cs101Class.setClassId("CS101_01");
        cs101Class.setCourse(courseRepo.findById("CS101").orElseThrow());
        cs101Class.setCapacity(30);
        cs101Class.setEnrolled(10);
        cs101Class.setReservedSeats(0);
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
    @DisplayName("Register succeeds with full state, billing, transaction, and audit updates")
    void test01_RegisterSuccess_UpdatesStateBillingTransactionAndAudit() {
        UconRequest req = new UconRequest();
        req.setStudentId("SV001");
        req.setClassId("CS102_01");

        ResponseEntity<ApiDecisionResponse> response = regController.register(req);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("Successfully enrolled.", response.getBody().getMessage());
        assertNotNull(response.getBody().getDecisionTrace());
        assertEquals(3, response.getBody().getDecisionTrace().phases().size());

        Student s = studentRepo.findById("SV001").orElseThrow();
        assertEquals(4, s.getCurrentCredits());
        assertEquals(4000000, s.getTuitionDebt());
        assertEquals(1, s.getRegisterAttemptCount());
        assertTrue(s.getRegisteredClassIds().contains("CS102_01"));
        assertTrue(s.getRegisteredScheduleSlots().contains("T3_1-3"));

        ClassSection cls = classRepo.findById("CS102_01").orElseThrow();
        assertEquals(5, cls.getEnrolled());
        assertEquals(0, cls.getReservedSeats());

        assertEquals(1, registrationRepo.count());
        Registration registration = registrationRepo.findAll().get(0);
        assertEquals("SV001", registration.getStudentId());
        assertEquals("CS102_01", registration.getClassId());

        assertEquals(1, auditRepo.count());
        assertEquals("ALLOW", auditRepo.findAll().get(0).getDecision());
    }

    @Test
    @DisplayName("Register is denied when tuition has not been paid")
    void test02_RegisterDenied_WhenTuitionNotPaid() {
        Student sv002 = new Student();
        sv002.setStudentId("SV002");
        sv002.setTuitionPaid(false);
        sv002.setMaxCreditsEffective(15);
        sv002.setCompletedCourses("CS101");
        sv002.setHolds("");
        sv002.setRegisteredClassIds("");
        sv002.setRegisteredScheduleSlots("");
        studentRepo.save(sv002);

        UconRequest req = new UconRequest();
        req.setStudentId("SV002");
        req.setClassId("CS102_01");

        ResponseEntity<ApiDecisionResponse> response = regController.register(req);
        assertEquals(403, response.getStatusCode().value());
        assertEquals("TUITION_NOT_PAID", response.getBody().getDenyReason());
        assertEquals("P01_TuitionPaid_PreA0", response.getBody().getFailedPolicy());
        assertEquals("DENY", auditRepo.findAll().get(0).getDecision());
        assertEquals(0, registrationRepo.count());
    }

    @Test
    @DisplayName("Register is denied outside the allowed transaction window")
    void test03_RegisterDenied_OutsideTransactionWindow() {
        Environment env = new Environment("ADJUSTMENT", "2025-01-01");
        env.setOpenTime("2026-01-01");
        env.setCloseTime("2026-12-31");
        env.setSemester("2026_FALL");

        Student student = studentRepo.findById("SV001").orElseThrow();
        ClassSection cls = classRepo.findById("CS102_01").orElseThrow();

        UconRequest req = new UconRequest();
        req.setRequestId(UUID.randomUUID().toString());
        req.setActionType("REGISTER");
        req.setStudentId("SV001");
        req.setClassId("CS102_01");

        req.setConfirmedRegistrationRule(true);
        req.setAdminOverride(false);
        AuthDecision decision = policyEngine.evaluatePhase("PRE", student, cls, env, req);
        assertFalse(decision.isPermit());
        assertEquals("OUTSIDE_TRANSACTION_WINDOW", decision.getFailedCode());
    }

    @Test
    @DisplayName("Class locked between phases is denied at the ongoing check")
    void test04_RegisterDenied_WhenClassLocksBetweenPhases() {
        Student student = studentRepo.findById("SV001").orElseThrow();
        ClassSection openCls = classRepo.findById("CS102_01").orElseThrow();

        Environment env = defaultEnv(false);
        UconRequest req = registerRequest();

        AuthDecision preDecision = policyEngine.evaluatePhase("PRE", student, openCls, env, req);
        assertTrue(preDecision.isPermit());

        openCls.setStatus("LOCKED");
        classRepo.save(openCls);

        ClassSection lockedCls = classRepo.findById("CS102_01").orElseThrow();
        AuthDecision ongoingDecision = policyEngine.evaluatePhase("ONGOING", student, lockedCls, env, req);
        assertFalse(ongoingDecision.isPermit());
        assertEquals("CLASS_STATUS_CHANGED", ongoingDecision.getFailedCode());
    }

    @Test
    @DisplayName("Duplicate registration is denied based on repository data")
    void test05_RegisterDenied_WhenRepositoryShowsExistingRegistration() {
        registrationRepo.save(new Registration("SV001", "CS102_01", "2026_FALL", "REGISTER"));

        Student student = studentRepo.findById("SV001").orElseThrow();
        student.setRegisteredClassIds("");
        studentRepo.save(student);

        UconRequest req = registerRequest();
        ResponseEntity<ApiDecisionResponse> response = regController.register(req);

        assertEquals(403, response.getStatusCode().value());
        assertEquals("ALREADY_REGISTERED", response.getBody().getDenyReason());
    }

    @Test
    @DisplayName("Register is denied when the credit limit is exceeded")
    void test06_RegisterDenied_WhenCreditLimitExceeded() {
        Student s = studentRepo.findById("SV001").orElseThrow();
        s.setCurrentCredits(12);
        studentRepo.save(s);

        ResponseEntity<ApiDecisionResponse> response = regController.register(registerRequest());
        assertEquals(403, response.getStatusCode().value());
        assertEquals("CREDIT_LIMIT_EXCEEDED", response.getBody().getDenyReason());
    }

    @Test
    @DisplayName("Register is denied when a prerequisite is missing")
    void test07_RegisterDenied_WhenPrerequisiteMissing() {
        Student s = studentRepo.findById("SV001").orElseThrow();
        s.setCompletedCourses("");
        studentRepo.save(s);

        ResponseEntity<ApiDecisionResponse> response = regController.register(registerRequest());
        assertEquals(403, response.getStatusCode().value());
        assertEquals("PREREQUISITE_NOT_MET", response.getBody().getDenyReason());
    }

    @Test
    @DisplayName("Register is denied when the schedule overlaps existing classes")
    void test08_RegisterDenied_WhenScheduleConflicts() {
        Student s = studentRepo.findById("SV001").orElseThrow();
        s.setRegisteredScheduleSlots("T3_1-3");
        studentRepo.save(s);

        ResponseEntity<ApiDecisionResponse> response = regController.register(registerRequest());
        assertEquals(403, response.getStatusCode().value());
        assertEquals("SCHEDULE_CONFLICT", response.getBody().getDenyReason());
    }

    @Test
    @DisplayName("Register is denied when the student is on hold")
    void test09_RegisterDenied_WhenStudentOnHold() {
        Student s = studentRepo.findById("SV001").orElseThrow();
        s.setHolds("DISCIPLINARY_HOLD");
        studentRepo.save(s);

        ResponseEntity<ApiDecisionResponse> response = regController.register(registerRequest());
        assertEquals(403, response.getStatusCode().value());
        assertEquals("STUDENT_ON_HOLD", response.getBody().getDenyReason());
        assertEquals(0, registrationRepo.count());
    }

    @Test
    @DisplayName("Only one student can claim the last remaining seat")
    void test10_OnlyOneStudentCanClaimLastSeat() throws InterruptedException {
        Student sv002 = new Student();
        sv002.setStudentId("SV002");
        sv002.setTuitionPaid(true);
        sv002.setMaxCreditsEffective(15);
        sv002.setCompletedCourses("CS101");
        sv002.setHolds("");
        sv002.setRegisteredClassIds("");
        sv002.setRegisteredScheduleSlots("");
        studentRepo.save(sv002);

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        executor.submit(() -> runConcurrentRegister("SV001", successCount, failCount, startLatch, doneLatch));
        executor.submit(() -> runConcurrentRegister("SV002", successCount, failCount, startLatch, doneLatch));

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertEquals(1, successCount.get());
        assertEquals(1, failCount.get());
        assertEquals(5, classRepo.findById("CS102_01").orElseThrow().getEnrolled());
        assertEquals(1, registrationRepo.count());
    }

    @Test
    @DisplayName("Maintenance blocks the request and leaves state unchanged")
    void test11_MaintenanceBlocksRequest_AndPreservesState() {
        Student student = studentRepo.findById("SV001").orElseThrow();
        ClassSection cls = classRepo.findById("CS102_01").orElseThrow();

        UconRequest req = registerRequest();

        AuthDecision preDecision = policyEngine.evaluatePhase("PRE", student, cls, defaultEnv(false), req);
        assertTrue(preDecision.isPermit());

        AuthDecision ongoingDecision = policyEngine.evaluatePhase("ONGOING", student, cls, defaultEnv(true), req);
        assertFalse(ongoingDecision.isPermit());
        assertEquals("SYSTEM_UNDER_MAINTENANCE", ongoingDecision.getFailedCode());

        maintenanceFlag.setActive(true);
        ResponseEntity<ApiDecisionResponse> response = regController.register(registerRequest());
        assertEquals(403, response.getStatusCode().value());
        assertEquals("SYSTEM_UNDER_MAINTENANCE", response.getBody().getDenyReason());

        Student unchanged = studentRepo.findById("SV001").orElseThrow();
        assertEquals(0, unchanged.getCurrentCredits());
        assertFalse(unchanged.getRegisteredClassIds().contains("CS102_01"));
        assertEquals(4, classRepo.findById("CS102_01").orElseThrow().getEnrolled());
        assertEquals(0, registrationRepo.count());
        assertEquals(1, auditRepo.count());
        assertEquals("DENY", auditRepo.findAll().get(0).getDecision());
    }

    @Test
    @DisplayName("Drop restores state, removes the transaction, and refunds tuition debt")
    void test12_DropRestoresState_RemovesTransaction_AndRefundsDebt() {
        ResponseEntity<ApiDecisionResponse> regResponse = regController.register(registerRequest());
        assertEquals(200, regResponse.getStatusCode().value());
        assertEquals(1, registrationRepo.count());
        assertEquals(4000000, studentRepo.findById("SV001").orElseThrow().getTuitionDebt());

        Student student = studentRepo.findById("SV001").orElseThrow();
        student.setTuitionPaid(false);
        studentRepo.save(student);

        UconRequest dropReq = dropRequest();
        ResponseEntity<ApiDecisionResponse> dropResponse = regController.drop(dropReq);
        assertEquals(200, dropResponse.getStatusCode().value());
        assertEquals("Successfully dropped.", dropResponse.getBody().getMessage());

        Student afterDrop = studentRepo.findById("SV001").orElseThrow();
        ClassSection afterDropCls = classRepo.findById("CS102_01").orElseThrow();

        assertEquals(0, registrationRepo.count());
        assertEquals(0, afterDrop.getCurrentCredits());
        assertEquals(0, afterDrop.getTuitionDebt());
        assertEquals(4, afterDropCls.getEnrolled());
        assertFalse(afterDrop.getRegisteredClassIds().contains("CS102_01"));
        assertFalse(afterDrop.getRegisteredScheduleSlots().contains("T3_1-3"));
    }

    @Test
    @DisplayName("Register is denied when the student has not confirmed the registration rule")
    void test13_RegisterDenied_WhenRegulationNotConfirmed() {
        UconRequest req = registerRequest();
        req.setConfirmedRegistrationRule(false);

        ResponseEntity<ApiDecisionResponse> response = regController.register(req);
        assertEquals(403, response.getStatusCode().value());
        assertEquals("REGULATION_NOT_CONFIRMED", response.getBody().getDenyReason());
        assertEquals("P17_AgreeRegistrationRule_PreB0", response.getBody().getFailedPolicy());
    }

    @Test
    @DisplayName("Register is denied when override is requested without a reason")
    void test14_RegisterDenied_WhenOverrideReasonMissing() {
        UconRequest req = registerRequest();
        req.setAdminOverride(true);
        req.setOverrideReason("");

        ResponseEntity<ApiDecisionResponse> response = regController.register(req);
        assertEquals(403, response.getStatusCode().value());
        assertEquals("OVERRIDE_REASON_REQUIRED", response.getBody().getDenyReason());
        assertEquals("P18_AdminOverrideReason_PreB0", response.getBody().getFailedPolicy());
    }

    @Test
    @DisplayName("Validator rejects updates to request-managed fields")
    void test15_ValidatorRejectsRequestManagedFieldUpdates() {
        EObject copiedRoot = EcoreUtil.copy(policyDecisionPoint.getPolicyModelRoot());
        EObject target = firstUpdateTarget(copiedRoot, "P11_RegisterStateUpdate_PostA3");
        target.eSet(target.eClass().getEStructuralFeature("entity"), enumLiteral(target, "EntityScope", "REQUEST"));
        target.eSet(target.eClass().getEStructuralFeature("path"), "decision");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> semanticValidator.validate(copiedRoot));
        assertTrue(ex.getMessage().contains("updates REQUEST path"));
    }

    @Test
    @DisplayName("Validator rejects malformed audit log statements")
    void test16_ValidatorRejectsMalformedAuditLogStatements() {
        EObject copiedRoot = EcoreUtil.copy(policyDecisionPoint.getPolicyModelRoot());
        EObject auditStmt = findPolicyById(copiedRoot, "P12_AuditAndTrace_PostB3")
                .eContents()
                .stream()
                .filter(e -> "AuditLogStatement".equals(e.eClass().getName()))
                .findFirst()
                .orElseThrow();

        @SuppressWarnings("unchecked")
        List<EObject> arguments = (List<EObject>) auditStmt.eGet(auditStmt.eClass().getEStructuralFeature("arguments"));
        arguments.remove(arguments.size() - 1);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> semanticValidator.validate(copiedRoot));
        assertTrue(ex.getMessage().contains("create AuditLog(...) must have exactly 5 arguments"));
    }

    @Test
    @DisplayName("Policy decision point startup fails when semantic validation fails")
    void test17_PolicyDecisionPointFailsFast_WhenValidationFails() {
        PolicyValidator failingValidator = new PolicyValidator(new PolicyModelSemanticValidator(functionRegistry) {
            @Override
            public void validate(EObject policyModelRoot) {
                throw new IllegalStateException("forced semantic failure");
            }
        }, attributeSchema);
        PolicyDecisionPoint pdp = new PolicyDecisionPoint(failingValidator, new PolicyAnalyzer());

        IllegalStateException ex = assertThrows(IllegalStateException.class, pdp::init);
        assertTrue(ex.getMessage().contains("PDP startup failed"));
        assertNotNull(ex.getCause());
        assertTrue(ex.getCause().getMessage().contains("forced semantic failure"));
    }

    @Test
    @DisplayName("Validator rejects invalid condition paths, function arity, and function phase")
    void test18_ValidatorRejectsInvalidPath_Arity_AndPhase() {
        EObject invalidPathRoot = EcoreUtil.copy(policyDecisionPoint.getPolicyModelRoot());
        EObject badPath = firstVariableAccessInCondition(invalidPathRoot, "P05_CreditLimit_PreA0");
        badPath.eSet(badPath.eClass().getEStructuralFeature("path"), "maxCreditEffecitve");
        IllegalStateException pathEx = assertThrows(IllegalStateException.class, () -> semanticValidator.validate(invalidPathRoot));
        assertTrue(pathEx.getMessage().contains("unknown path SUBJECT.maxCreditEffecitve"));

        EObject invalidArityRoot = EcoreUtil.copy(policyDecisionPoint.getPolicyModelRoot());
        EObject functionCall = firstFunctionCallInCondition(invalidArityRoot, "P10_StudentHoldRecheck_OnA0");
        @SuppressWarnings("unchecked")
        List<EObject> args = (List<EObject>) functionCall.eGet(functionCall.eClass().getEStructuralFeature("arguments"));
        args.add(EcoreUtil.copy(args.get(0)));
        IllegalStateException arityEx = assertThrows(IllegalStateException.class, () -> semanticValidator.validate(invalidArityRoot));
        assertTrue(arityEx.getMessage().contains("expects 1 arguments"));

        EObject invalidPhaseRoot = EcoreUtil.copy(policyDecisionPoint.getPolicyModelRoot());
        EObject postPolicy = findPolicyById(invalidPhaseRoot, "P11_RegisterStateUpdate_PostA3");
        EObject replacement = createFunctionCall(invalidPhaseRoot, "checkExistsRegistration", List.of(
                createVariableAccess(invalidPhaseRoot, "SUBJECT", "studentId"),
                createVariableAccess(invalidPhaseRoot, "OBJECT", "classId"),
                createVariableAccess(invalidPhaseRoot, "ENVIRONMENT", "semester")
        ));
        postPolicy.eSet(postPolicy.eClass().getEStructuralFeature("condition"), replacement);
        IllegalStateException phaseEx = assertThrows(IllegalStateException.class, () -> semanticValidator.validate(invalidPhaseRoot));
        assertTrue(phaseEx.getMessage().contains("disallowed phase POST"));

        EObject overlappingPriorityRoot = EcoreUtil.copy(policyDecisionPoint.getPolicyModelRoot());
        EObject registerOnlyPolicy = findPolicyById(overlappingPriorityRoot, "P01_TuitionPaid_PreA0");
        registerOnlyPolicy.eSet(registerOnlyPolicy.eClass().getEStructuralFeature("priority"), 95);
        IllegalStateException priorityEx = assertThrows(IllegalStateException.class,
                () -> semanticValidator.validate(overlappingPriorityRoot));
        assertTrue(priorityEx.getMessage().contains("overlapping priority 95"));
    }

    @Test
    @DisplayName("PolicyValidator rejects immutable ENVIRONMENT updates via attribute schema")
    void test19_PolicyValidatorRejectsEnvironmentImmutableUpdate() {
        EObject copiedRoot = EcoreUtil.copy(policyDecisionPoint.getPolicyModelRoot());
        EObject target = firstUpdateTarget(copiedRoot, "P11_RegisterStateUpdate_PostA3");
        target.eSet(target.eClass().getEStructuralFeature("entity"), enumLiteral(target, "EntityScope", "ENVIRONMENT"));
        target.eSet(target.eClass().getEStructuralFeature("path"), "isMaintenance");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> policyValidator.validate(copiedRoot));
        assertTrue(ex.getMessage().contains("environment must stay immutable")
                || ex.getMessage().contains("immutable path ENVIRONMENT.isMaintenance"));
    }

    @Test
    @DisplayName("PolicyAnalyzer warns when audit trace policy is missing")
    void test20_PolicyAnalyzerWarnsWhenAuditTraceMissing() {
        EObject copiedRoot = EcoreUtil.copy(policyDecisionPoint.getPolicyModelRoot());
        EObject auditPolicy = findPolicyById(copiedRoot, "P12_AuditAndTrace_PostB3");
        @SuppressWarnings("unchecked")
        List<EObject> policies = (List<EObject>) copiedRoot.eGet(copiedRoot.eClass().getEStructuralFeature("policies"));
        policies.remove(auditPolicy);

        PolicyAnalysisReport report = policyAnalyzer.analyze(copiedRoot);
        assertTrue(report.warnings().stream().anyMatch(w -> "MISSING_AUDIT".equals(w.type())));
    }

    @Test
    @DisplayName("Deny responses include decision trace with phase and failed policy")
    void test21_DenyResponseContainsDecisionTrace() {
        Student sv002 = new Student();
        sv002.setStudentId("SV002");
        sv002.setTuitionPaid(false);
        sv002.setMaxCreditsEffective(15);
        sv002.setCompletedCourses("CS101");
        sv002.setHolds("");
        sv002.setRegisteredClassIds("");
        sv002.setRegisteredScheduleSlots("");
        studentRepo.save(sv002);

        UconRequest req = new UconRequest();
        req.setStudentId("SV002");
        req.setClassId("CS102_01");

        ResponseEntity<ApiDecisionResponse> response = regController.register(req);
        DecisionTrace trace = response.getBody().getDecisionTrace();

        assertNotNull(trace);
        assertEquals("DENY", trace.decision());
        assertEquals(1, trace.phases().size());
        assertEquals("PRE", trace.phases().get(0).phase());
        assertEquals("P01_TuitionPaid_PreA0", trace.phases().get(0).failedPolicy());
    }

    private void runConcurrentRegister(String studentId,
                                       AtomicInteger successCount,
                                       AtomicInteger failCount,
                                       CountDownLatch startLatch,
                                       CountDownLatch doneLatch) {
        try {
            startLatch.await();
            UconRequest req = registerRequest();
            req.setStudentId(studentId);
            if (regController.register(req).getStatusCode().value() == 200) {
                successCount.incrementAndGet();
            } else {
                failCount.incrementAndGet();
            }
        } catch (Exception e) {
            failCount.incrementAndGet();
        } finally {
            doneLatch.countDown();
        }
    }

    private UconRequest registerRequest() {
        UconRequest req = new UconRequest();
        req.setRequestId(UUID.randomUUID().toString());
        req.setActionType("REGISTER");
        req.setStudentId("SV001");
        req.setClassId("CS102_01");
        req.setConfirmedRegistrationRule(true);
        req.setAdminOverride(false);
        return req;
    }

    private UconRequest dropRequest() {
        UconRequest req = new UconRequest();
        req.setRequestId(UUID.randomUUID().toString());
        req.setActionType("DROP");
        req.setStudentId("SV001");
        req.setClassId("CS102_01");
        req.setConfirmedRegistrationRule(true);
        req.setAdminOverride(false);
        return req;
    }

    private Environment defaultEnv(boolean maintenance) {
        Environment env = new Environment("NORMAL", "2026-03-27");
        env.setOpenTime("2026-01-01");
        env.setCloseTime("2026-12-31");
        env.setSemester("2026_FALL");
        env.setIsMaintenance(maintenance);
        return env;
    }

    private EObject findPolicyById(EObject root, String policyId) {
        @SuppressWarnings("unchecked")
        List<EObject> policies = (List<EObject>) root.eGet(root.eClass().getEStructuralFeature("policies"));
        return policies.stream()
                .filter(p -> policyId.equals(p.eGet(p.eClass().getEStructuralFeature("policyId"))))
                .findFirst()
                .orElseThrow();
    }

    private EObject firstUpdateTarget(EObject root, String policyId) {
        EObject policy = findPolicyById(root, policyId);
        @SuppressWarnings("unchecked")
        List<EObject> postUpdates = (List<EObject>) policy.eGet(policy.eClass().getEStructuralFeature("postUpdates"));
        EObject firstUpdate = postUpdates.stream()
                .filter(s -> "UpdateStatement".equals(s.eClass().getName()))
                .findFirst()
                .orElseThrow();
        return (EObject) firstUpdate.eGet(firstUpdate.eClass().getEStructuralFeature("target"));
    }

    private EObject firstVariableAccessInCondition(EObject root, String policyId) {
        EObject policy = findPolicyById(root, policyId);
        EObject condition = (EObject) policy.eGet(policy.eClass().getEStructuralFeature("condition"));
        return depthFirst(condition).stream()
                .filter(node -> "VariableAccess".equals(node.eClass().getName()))
                .findFirst()
                .orElseThrow();
    }

    private EObject firstFunctionCallInCondition(EObject root, String policyId) {
        EObject policy = findPolicyById(root, policyId);
        EObject condition = (EObject) policy.eGet(policy.eClass().getEStructuralFeature("condition"));
        return depthFirst(condition).stream()
                .filter(node -> "FunctionCall".equals(node.eClass().getName()))
                .findFirst()
                .orElseThrow();
    }

    private List<EObject> depthFirst(EObject root) {
        List<EObject> nodes = new java.util.ArrayList<>();
        nodes.add(root);
        for (EObject child : root.eContents()) {
            nodes.addAll(depthFirst(child));
        }
        return nodes;
    }

    private EObject createFunctionCall(EObject root, String functionName, List<EObject> arguments) {
        EObject functionCall = policyDecisionPoint.getUconPackage().getEFactoryInstance()
                .create((org.eclipse.emf.ecore.EClass) policyDecisionPoint.getUconPackage().getEClassifier("FunctionCall"));
        functionCall.eSet(functionCall.eClass().getEStructuralFeature("functionName"), functionName);
        @SuppressWarnings("unchecked")
        List<EObject> argList = (List<EObject>) functionCall.eGet(functionCall.eClass().getEStructuralFeature("arguments"));
        argList.addAll(arguments);
        return functionCall;
    }

    private EObject createVariableAccess(EObject root, String entityLiteral, String path) {
        EObject variableAccess = policyDecisionPoint.getUconPackage().getEFactoryInstance()
                .create((org.eclipse.emf.ecore.EClass) policyDecisionPoint.getUconPackage().getEClassifier("VariableAccess"));
        variableAccess.eSet(variableAccess.eClass().getEStructuralFeature("entity"),
                enumLiteral(variableAccess, "EntityScope", entityLiteral));
        variableAccess.eSet(variableAccess.eClass().getEStructuralFeature("path"), path);
        return variableAccess;
    }

    private EEnumLiteral enumLiteral(EObject context, String enumName, String literalName) {
        EEnum eEnum = (EEnum) policyDecisionPoint.getUconPackage().getEClassifier(enumName);
        return eEnum.getEEnumLiteral(literalName);
    }
}
