package vn.edu.kma.ucon.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
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
import vn.edu.kma.ucon.engine.pdp.PolicyAdministrationPoint;
import vn.edu.kma.ucon.engine.pdp.PolicyAnalyzer;
import vn.edu.kma.ucon.engine.pdp.PolicyDecisionPoint;
import vn.edu.kma.ucon.engine.pdp.PolicyEngine;
import vn.edu.kma.ucon.engine.pdp.PolicyFunctionRegistry;
import vn.edu.kma.ucon.engine.pdp.PolicyLifecycleService;
import vn.edu.kma.ucon.engine.pdp.PolicyModelSemanticValidator;
import vn.edu.kma.ucon.engine.pdp.PolicyValidator;
import vn.edu.kma.ucon.engine.pdp.Phase;
import vn.edu.kma.ucon.engine.pdp.PredicateType;
import vn.edu.kma.ucon.engine.pip.entity.ClassSection;
import vn.edu.kma.ucon.engine.pip.entity.Course;
import vn.edu.kma.ucon.engine.pip.entity.Registration;
import vn.edu.kma.ucon.engine.pip.entity.Student;
import vn.edu.kma.ucon.engine.pip.repository.AuditLogRepository;
import vn.edu.kma.ucon.engine.pip.repository.ClassSectionRepository;
import vn.edu.kma.ucon.engine.pip.repository.CourseRepository;
import vn.edu.kma.ucon.engine.pip.repository.RegistrationRepository;
import vn.edu.kma.ucon.engine.pip.repository.StudentRepository;
import vn.edu.kma.ucon.engine.session.SessionStatus;
import vn.edu.kma.ucon.engine.session.UsageSession;
import vn.edu.kma.ucon.engine.session.UsageSessionRepository;
import vn.edu.kma.ucon.engine.session.UsageSessionService;
import vn.edu.kma.ucon.engine.session.monitor.ClassStatusChangedEvent;
import vn.edu.kma.ucon.engine.session.monitor.MaintenanceEnabledEvent;
import vn.edu.kma.ucon.engine.session.monitor.StudentHoldAddedEvent;
import vn.edu.kma.ucon.engine.update.RollbackManager;
import vn.edu.kma.ucon.engine.update.UpdateManager;
import vn.edu.kma.ucon.engine.update.UpdatePlan;

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
    PolicyAdministrationPoint policyAdministrationPoint;
    @Autowired
    PolicyLifecycleService policyLifecycleService;
    @Autowired
    AttributeSchema attributeSchema;
    @Autowired
    UsageSessionRepository usageSessionRepository;
    @Autowired
    UsageSessionService usageSessionService;
    @Autowired
    ApplicationEventPublisher eventPublisher;
    @Autowired
    UpdateManager updateManager;
    @Autowired
    RollbackManager rollbackManager;

    @BeforeEach
    void setUp() {
        maintenanceFlag.setActive(false);
        auditRepo.deleteAll();
        registrationRepo.deleteAll();
        usageSessionRepository.deleteAll();
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
        sv001.setRegisterAttemptCount(0);
        sv001.setDropCountForSemester(0);
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
        assertEquals(8, response.getBody().getDecisionTrace().phases().size());
        assertEquals("COMMITTED", response.getBody().getDecisionTrace().sessionStatus());
        assertEquals("PRE", response.getBody().getDecisionTrace().phases().get(0).phase());
        assertEquals("CONDITION", response.getBody().getDecisionTrace().phases().get(0).predicate());
        assertEquals("AUTHORIZATION", response.getBody().getDecisionTrace().phases().get(1).predicate());
        assertEquals("OBLIGATION", response.getBody().getDecisionTrace().phases().get(2).predicate());
        assertEquals("ONGOING", response.getBody().getDecisionTrace().phases().get(3).phase());
        assertEquals("POST", response.getBody().getDecisionTrace().phases().get(6).phase());

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
        assertEquals(1, usageSessionRepository.count());
        assertEquals(SessionStatus.COMMITTED, usageSessionRepository.findAll().get(0).getStatus());
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
        PolicyDecisionPoint pdp = new PolicyDecisionPoint(
                failingValidator,
                new PolicyAnalyzer(),
                new PolicyAdministrationPoint());

        IllegalStateException ex = assertThrows(IllegalStateException.class, pdp::init);
        assertTrue(ex.getMessage().contains("failed to apply")
                || ex.getMessage().contains("PDP startup failed"));
        assertNotNull(ex.getCause());
        Throwable rootCause = ex;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }
        assertTrue(rootCause.getMessage().contains("forced semantic failure"));
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
        assertEquals(3, trace.phases().size());
        assertEquals("PRE", trace.phases().get(0).phase());
        assertEquals("CONDITION", trace.phases().get(0).predicate());
        assertEquals("AUTHORIZATION", trace.phases().get(1).predicate());
        assertEquals("P01_TuitionPaid_PreA0", trace.phases().get(1).failedPolicy());
        assertNull(trace.sessionStatus());
    }

    @Test
    @DisplayName("Direct ongoing maintenance denial marks an active usage session as revoked")
    void test22_DirectOngoingMaintenanceMarksSessionRevoked() {
        Student student = studentRepo.findById("SV001").orElseThrow();
        ClassSection cls = classRepo.findById("CS102_01").orElseThrow();
        UconRequest req = registerRequest();

        UsageSession session = usageSessionService.createActive(req);
        AuthDecision ongoingDecision = policyEngine.evaluate(Phase.ONGOING, PredicateType.CONDITION, student, cls, defaultEnv(true), req);
        assertFalse(ongoingDecision.isPermit());
        assertEquals("SYSTEM_UNDER_MAINTENANCE", ongoingDecision.getFailedCode());

        usageSessionService.markRevoked(session, ongoingDecision.getFailedCode());
        assertEquals(1, usageSessionRepository.count());
        assertEquals(SessionStatus.REVOKED, usageSessionRepository.findAll().get(0).getStatus());
    }

    @Test
    @DisplayName("Ten students competing for three slots preserve capacity invariants")
    void test23_Race_10Students_3Slots_PreservesInvariants() throws InterruptedException {
        ClassSection cls = classRepo.findById("CS102_01").orElseThrow();
        cls.setCapacity(7);
        cls.setEnrolled(4);
        cls.setReservedSeats(0);
        classRepo.save(cls);

        for (int i = 2; i <= 10; i++) {
            Student student = new Student();
            student.setStudentId("SV%03d".formatted(i));
            student.setTuitionPaid(true);
            student.setMaxCreditsEffective(15);
            student.setCompletedCourses("CS101");
            student.setHolds("");
            student.setRegisteredClassIds("");
            student.setRegisteredScheduleSlots("");
            studentRepo.save(student);
        }

        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 1; i <= 10; i++) {
            String studentId = "SV%03d".formatted(i);
            executor.submit(() -> runConcurrentRegister(studentId, successCount, failCount, startLatch, doneLatch));
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        ClassSection after = classRepo.findById("CS102_01").orElseThrow();
        assertEquals(threads, successCount.get() + failCount.get());
        assertTrue(successCount.get() <= 3);
        assertTrue(failCount.get() >= 7);
        assertTrue(after.getEnrolled() >= 4);
        assertTrue(after.getEnrolled() <= 7);
        assertEquals(0, after.getReservedSeats());
        assertTrue(after.getEnrolled() <= after.getCapacity());
        assertEquals(after.getEnrolled() - 4, registrationRepo.count());
    }

    @Test
    @DisplayName("Ongoing reserve-seat update rolls back cleanly")
    void test24_ReserveSeatRollback_RestoresReservedSeats() {
        Student student = studentRepo.findById("SV001").orElseThrow();
        ClassSection cls = classRepo.findById("CS102_01").orElseThrow();
        Environment env = defaultEnv(false);
        UconRequest req = registerRequest();

        UpdatePlan ongoingPlan = updateManager.buildPlan(Phase.ONGOING, student, cls, env, req);
        UpdatePlan rollbackPlan = rollbackManager.buildPlan(Phase.ONGOING, student, cls, env, req);

        List<String> ongoingApplied = updateManager.apply(ongoingPlan, student, cls, env, req);
        assertTrue(ongoingApplied.contains("P20_ReserveSeat_OnA2"));
        assertEquals(1, cls.getReservedSeats());

        List<String> rollbackApplied = rollbackManager.apply(rollbackPlan, student, cls, env, req);
        assertTrue(rollbackApplied.contains("P20_ReserveSeat_OnA2"));
        assertEquals(0, cls.getReservedSeats());
    }

    @Test
    @DisplayName("Register is denied when the maximum register-attempt limit is reached")
    void test25_RegisterDenied_WhenMaxRegisterAttemptsReached() {
        Student student = studentRepo.findById("SV001").orElseThrow();
        student.setRegisterAttemptCount(5);
        studentRepo.save(student);

        ResponseEntity<ApiDecisionResponse> response = regController.register(registerRequest());
        assertEquals(403, response.getStatusCode().value());
        assertEquals("MAX_REGISTER_ATTEMPTS_EXCEEDED", response.getBody().getDenyReason());
        assertEquals("P25_MaxRegisterAttempts_PreA0", response.getBody().getFailedPolicy());
    }

    @Test
    @DisplayName("Drop is denied when the maximum drop count for the semester is reached")
    void test26_DropDenied_WhenMaxDropTimesReached() {
        ResponseEntity<ApiDecisionResponse> regResponse = regController.register(registerRequest());
        assertEquals(200, regResponse.getStatusCode().value());

        Student student = studentRepo.findById("SV001").orElseThrow();
        student.setDropCountForSemester(2);
        studentRepo.save(student);

        ResponseEntity<ApiDecisionResponse> response = regController.drop(dropRequest());
        assertEquals(403, response.getStatusCode().value());
        assertEquals("MAX_DROP_TIMES_EXCEEDED", response.getBody().getDenyReason());
        assertEquals("P26_MaxDropTimes_PreA0", response.getBody().getFailedPolicy());
    }

    @Test
    @DisplayName("Register is revoked in ONGOING obligation when the usage session lease expires")
    void test27_RegisterDenied_WhenSessionLeaseExpiresDuringOngoing() {
        UconRequest req = registerRequest();
        req.setSessionLeaseValid(false);

        ResponseEntity<ApiDecisionResponse> response = regController.register(req);
        assertEquals(403, response.getStatusCode().value());
        assertEquals("USAGE_SESSION_EXPIRED", response.getBody().getDenyReason());
        assertEquals("P27_SessionLease_OnB0", response.getBody().getFailedPolicy());
        assertNotNull(response.getBody().getDecisionTrace());
        assertEquals("DENY", response.getBody().getDecisionTrace().decision());
        assertEquals("REVOKED", response.getBody().getDecisionTrace().sessionStatus());
        assertTrue(response.getBody().getDecisionTrace().phases().stream()
                .anyMatch(phase -> "ONGOING".equals(phase.phase())
                        && "OBLIGATION".equals(phase.predicate())
                        && "P27_SessionLease_OnB0".equals(phase.failedPolicy())));
        assertEquals(0, registrationRepo.count());
        assertEquals(1, usageSessionRepository.count());
        assertEquals(SessionStatus.REVOKED, usageSessionRepository.findAll().get(0).getStatus());
    }

    @Test
    @DisplayName("PolicyAnalyzer warns when equivalent policies shadow one another by priority")
    void test28_PolicyAnalyzerWarnsWhenEquivalentPoliciesShadowEachOther() {
        EObject copiedRoot = EcoreUtil.copy(policyDecisionPoint.getPolicyModelRoot());
        @SuppressWarnings("unchecked")
        List<EObject> policies = (List<EObject>) copiedRoot.eGet(copiedRoot.eClass().getEStructuralFeature("policies"));

        EObject duplicate = EcoreUtil.copy(findPolicyById(copiedRoot, "P03_ClassStatusOpen_PreA0"));
        duplicate.eSet(duplicate.eClass().getEStructuralFeature("policyId"), "P03_ClassStatusOpen_PreA0_Copy");
        duplicate.eSet(duplicate.eClass().getEStructuralFeature("priority"), 79);
        policies.add(duplicate);

        PolicyAnalysisReport report = policyAnalyzer.analyze(copiedRoot);
        assertTrue(report.warnings().stream().anyMatch(w -> "SHADOWING".equals(w.type())));
    }

    @Test
    @DisplayName("PolicyAnalyzer warns when policies share the same priority but disagree on effect")
    void test29_PolicyAnalyzerWarnsWhenPoliciesConflictOnEffect() {
        EObject copiedRoot = EcoreUtil.copy(policyDecisionPoint.getPolicyModelRoot());
        @SuppressWarnings("unchecked")
        List<EObject> policies = (List<EObject>) copiedRoot.eGet(copiedRoot.eClass().getEStructuralFeature("policies"));

        EObject duplicate = EcoreUtil.copy(findPolicyById(copiedRoot, "P01_TuitionPaid_PreA0"));
        duplicate.eSet(duplicate.eClass().getEStructuralFeature("policyId"), "P01_TuitionPaid_PreA0_DenyTwin");
        duplicate.eSet(duplicate.eClass().getEStructuralFeature("effect"),
                enumLiteral(duplicate, "PolicyEffect", "DENY"));
        policies.add(duplicate);

        PolicyAnalysisReport report = policyAnalyzer.analyze(copiedRoot);
        assertTrue(report.warnings().stream().anyMatch(w -> "CONFLICTING_PRIORITY".equals(w.type())));
    }

    @Test
    @DisplayName("PolicyAdministrationPoint keeps only ACTIVE policies in the runtime model")
    void test30_PolicyAdministrationPointKeepsOnlyActivePolicies() {
        EObject copiedRoot = EcoreUtil.copy(policyDecisionPoint.getPolicyModelRoot());
        EObject targetPolicy = findPolicyById(copiedRoot, "P01_TuitionPaid_PreA0");
        targetPolicy.eSet(targetPolicy.eClass().getEStructuralFeature("policyStatus"),
                enumLiteral(targetPolicy, "PolicyStatus", "DEPRECATED"));

        EObject filteredRoot = policyAdministrationPoint.activateValidatedPolicies(copiedRoot);

        @SuppressWarnings("unchecked")
        List<EObject> filteredPolicies = (List<EObject>) filteredRoot.eGet(filteredRoot.eClass().getEStructuralFeature("policies"));
        assertTrue(filteredPolicies.stream().noneMatch(p -> "P01_TuitionPaid_PreA0".equals(
                p.eGet(p.eClass().getEStructuralFeature("policyId")))));

        @SuppressWarnings("unchecked")
        List<EObject> policySets = (List<EObject>) filteredRoot.eGet(filteredRoot.eClass().getEStructuralFeature("policySets"));
        @SuppressWarnings("unchecked")
        List<String> policyIds = (List<String>) policySets.get(0).eGet(policySets.get(0).eClass().getEStructuralFeature("policyIds"));
        assertFalse(policyIds.contains("P01_TuitionPaid_PreA0"));
    }

    @Test
    @DisplayName("Ongoing monitor revokes an ACTIVE session when class status changes")
    void test31_OngoingMonitorRevokesActiveSession_WhenClassStatusChanges() {
        UsageSession session = createActiveUsageSession("SV001", "CS102_01", "REGISTER");
        ClassSection cls = classRepo.findById("CS102_01").orElseThrow();
        cls.setStatus("LOCKED");
        classRepo.save(cls);

        eventPublisher.publishEvent(new ClassStatusChangedEvent("CS102_01", "LOCKED"));

        UsageSession reloaded = usageSessionRepository.findById(session.getSessionId()).orElseThrow();
        assertEquals(SessionStatus.REVOKED, reloaded.getStatus());
        assertEquals("CLASS_STATUS_CHANGED", reloaded.getRevokeReason());
        assertEquals("DENY", auditRepo.findTopByRequestIdOrderByIdDesc(session.getRequestId()).orElseThrow().getDecision());
    }

    @Test
    @DisplayName("Ongoing monitor revokes an ACTIVE session when maintenance is enabled")
    void test32_OngoingMonitorRevokesActiveSession_WhenMaintenanceEnabled() {
        UsageSession session = createActiveUsageSession("SV001", "CS102_01", "REGISTER");
        maintenanceFlag.setActive(true);

        eventPublisher.publishEvent(new MaintenanceEnabledEvent(true));

        UsageSession reloaded = usageSessionRepository.findById(session.getSessionId()).orElseThrow();
        assertEquals(SessionStatus.REVOKED, reloaded.getStatus());
        assertEquals("SYSTEM_UNDER_MAINTENANCE", reloaded.getRevokeReason());
        assertEquals("DENY", auditRepo.findTopByRequestIdOrderByIdDesc(session.getRequestId()).orElseThrow().getDecision());
    }

    @Test
    @DisplayName("Ongoing monitor revokes an ACTIVE session when a student hold is added")
    void test33_OngoingMonitorRevokesActiveSession_WhenStudentHoldAdded() {
        UsageSession session = createActiveUsageSession("SV001", "CS102_01", "REGISTER");
        Student student = studentRepo.findById("SV001").orElseThrow();
        student.setHolds("DISCIPLINARY_HOLD");
        studentRepo.save(student);

        eventPublisher.publishEvent(new StudentHoldAddedEvent("SV001", "DISCIPLINARY_HOLD"));

        UsageSession reloaded = usageSessionRepository.findById(session.getSessionId()).orElseThrow();
        assertEquals(SessionStatus.REVOKED, reloaded.getStatus());
        assertEquals("STUDENT_ON_HOLD", reloaded.getRevokeReason());
        assertEquals("DENY", auditRepo.findTopByRequestIdOrderByIdDesc(session.getRequestId()).orElseThrow().getDecision());
    }

    @Test
    @DisplayName("Policy lifecycle service supports DRAFT -> VALIDATED -> ACTIVE -> DEPRECATED -> ARCHIVED")
    void test34_PolicyLifecycleServiceSupportsFullTransitionChain() {
        EObject originalRoot = EcoreUtil.copy(policyDecisionPoint.getAuthoringPolicyModelRoot());
        try {
            EObject workingRoot = EcoreUtil.copy(originalRoot);
            @SuppressWarnings("unchecked")
            List<EObject> policies = (List<EObject>) workingRoot.eGet(workingRoot.eClass().getEStructuralFeature("policies"));

            EObject draftCopy = EcoreUtil.copy(findPolicyById(workingRoot, "P01_TuitionPaid_PreA0"));
            draftCopy.eSet(draftCopy.eClass().getEStructuralFeature("policyId"), "P01_TuitionPaid_LifecycleDemo");
            draftCopy.eSet(draftCopy.eClass().getEStructuralFeature("priority"), 101);
            draftCopy.eSet(draftCopy.eClass().getEStructuralFeature("policyStatus"),
                    enumLiteral(draftCopy, "PolicyStatus", "DRAFT"));
            policies.add(draftCopy);
            policyDecisionPoint.replacePolicyModel(workingRoot);

            assertEquals("DRAFT", policyLifecycleService.transitionPolicy("P01_TuitionPaid_LifecycleDemo", "DRAFT").status());
            assertEquals("VALIDATED", policyLifecycleService.transitionPolicy("P01_TuitionPaid_LifecycleDemo", "VALIDATED").status());
            assertEquals("ACTIVE", policyLifecycleService.transitionPolicy("P01_TuitionPaid_LifecycleDemo", "ACTIVE").status());
            assertTrue(policyLifecycleService.listRuntimePolicyIds().contains("P01_TuitionPaid_LifecycleDemo"));
            assertEquals("DEPRECATED", policyLifecycleService.transitionPolicy("P01_TuitionPaid_LifecycleDemo", "DEPRECATED").status());
            assertFalse(policyLifecycleService.listRuntimePolicyIds().contains("P01_TuitionPaid_LifecycleDemo"));
            assertEquals("ARCHIVED", policyLifecycleService.transitionPolicy("P01_TuitionPaid_LifecycleDemo", "ARCHIVED").status());
        } finally {
            policyDecisionPoint.replacePolicyModel(originalRoot);
        }
    }

    @Test
    @DisplayName("Policy lifecycle service rejects invalid transition")
    void test35_PolicyLifecycleServiceRejectsInvalidTransition() {
        EObject originalRoot = EcoreUtil.copy(policyDecisionPoint.getAuthoringPolicyModelRoot());
        try {
            EObject workingRoot = EcoreUtil.copy(originalRoot);
            @SuppressWarnings("unchecked")
            List<EObject> policies = (List<EObject>) workingRoot.eGet(workingRoot.eClass().getEStructuralFeature("policies"));

            EObject draftCopy = EcoreUtil.copy(findPolicyById(workingRoot, "P01_TuitionPaid_PreA0"));
            draftCopy.eSet(draftCopy.eClass().getEStructuralFeature("policyId"), "P01_TuitionPaid_InvalidLifecycle");
            draftCopy.eSet(draftCopy.eClass().getEStructuralFeature("priority"), 101);
            draftCopy.eSet(draftCopy.eClass().getEStructuralFeature("policyStatus"),
                    enumLiteral(draftCopy, "PolicyStatus", "DRAFT"));
            policies.add(draftCopy);
            policyDecisionPoint.replacePolicyModel(workingRoot);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> policyLifecycleService.transitionPolicy("P01_TuitionPaid_InvalidLifecycle", "ACTIVE"));
            assertTrue(ex.getMessage().contains("Invalid lifecycle transition"));
        } finally {
            policyDecisionPoint.replacePolicyModel(originalRoot);
        }
    }

    @Test
    @DisplayName("XMI policy model conforms to Ecore metamodel and semantic validation rules")
    void test36_XmiPolicyModelConformsToEcoreAndSemanticRules() {
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());

        ResourceSet resourceSet = new ResourceSetImpl();
        File ecoreFile = resolveExistingFile("../metamodel/ucon.ecore", "metamodel/ucon.ecore");
        Resource ecoreResource = resourceSet.getResource(URI.createFileURI(ecoreFile.getAbsolutePath()), true);
        assertTrue(ecoreResource.getErrors().isEmpty());

        EPackage ePackage = (EPackage) ecoreResource.getContents().get(0);
        EPackage.Registry.INSTANCE.put(ePackage.getNsURI(), ePackage);

        File xmiFile = resolveExistingFile("../xmi/ucon_policy.xmi", "xmi/ucon_policy.xmi");
        Resource xmiResource = resourceSet.getResource(URI.createFileURI(xmiFile.getAbsolutePath()), true);
        assertTrue(xmiResource.getErrors().isEmpty());
        assertEquals(1, xmiResource.getContents().size());

        EObject xmiRoot = xmiResource.getContents().get(0);
        semanticValidator.validate(xmiRoot);
        policyValidator.validate(xmiRoot);

        assertNotNull(findPolicyById(xmiRoot, "P27_SessionLease_OnB0"));
        assertNotNull(findPolicyById(xmiRoot, "P12_AuditAndTrace_PostB3"));
    }

    @Test
    @DisplayName("Controller register runtime uses canonical PRE/ONGOING/POST phases")
    void test37_ControllerRegisterUsesCanonicalUconPhasesAndReturnsTrace() {
        ResponseEntity<ApiDecisionResponse> response = regController.register(registerRequest());

        assertEquals(200, response.getStatusCode().value());
        ApiDecisionResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("ALLOW", body.getDecision());
        assertEquals("POST", body.getPhase());
        assertNotNull(body.getDecisionTrace());
        assertEquals("COMMITTED", body.getDecisionTrace().sessionStatus());

        List<String> phases = body.getDecisionTrace().phases().stream()
                .map(phaseTrace -> phaseTrace.phase())
                .toList();
        assertTrue(phases.contains("PRE"));
        assertTrue(phases.contains("ONGOING"));
        assertTrue(phases.contains("POST"));
        assertFalse(phases.contains("PRE" + "_AUTHORIZATION"));
        assertFalse(phases.contains("ONGOING" + "_AUTHORIZATION"));
        assertFalse(phases.contains("POST" + "_UPDATE"));
    }

    @Test
    @DisplayName("Drop not registered is denied by P16 policy through the UCON pipeline")
    void test38_DropNotRegisteredDeniedByP16PolicyThroughPipeline() {
        ResponseEntity<ApiDecisionResponse> response = regController.drop(dropRequest());

        assertEquals(403, response.getStatusCode().value());
        ApiDecisionResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("DENY", body.getDecision());
        assertEquals("PRE", body.getPhase());
        assertEquals("P16_DropOnlyIfRegistered_PreA0", body.getFailedPolicy());
        assertEquals("NOT_REGISTERED", body.getDenyReason());
        assertNotNull(body.getDecisionTrace());
        assertTrue(body.getDecisionTrace().phases().stream()
                .anyMatch(phase -> "PRE".equals(phase.phase())
                        && "AUTHORIZATION".equals(phase.predicate())
                        && "P16_DropOnlyIfRegistered_PreA0".equals(phase.failedPolicy())));
        assertEquals(0, registrationRepo.count());
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
        req.setSessionLeaseValid(true);
        return req;
    }

    private UsageSession createActiveUsageSession(String studentId, String classId, String actionType) {
        UconRequest request = new UconRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setStudentId(studentId);
        request.setClassId(classId);
        request.setActionType(actionType);
        request.setConfirmedRegistrationRule(true);
        request.setAdminOverride(false);
        request.setSessionLeaseValid(true);
        return usageSessionService.createActive(request);
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
        env.setMaxRegisterAttempts(5);
        env.setMaxDropTimes(2);
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

    private File resolveExistingFile(String... candidates) {
        for (String candidate : candidates) {
            File file = new File(candidate);
            if (file.exists()) {
                return file;
            }
            file = new File(System.getProperty("user.dir"), candidate);
            if (file.exists()) {
                return file;
            }
        }
        throw new IllegalStateException("Unable to resolve required file from candidates: " + String.join(", ", candidates));
    }

    private EEnumLiteral enumLiteral(EObject context, String enumName, String literalName) {
        EEnum eEnum = (EEnum) policyDecisionPoint.getUconPackage().getEClassifier(enumName);
        return eEnum.getEEnumLiteral(literalName);
    }
}
