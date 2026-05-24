package vn.edu.kma.ucon.engine.pep;

import java.util.UUID;
import java.util.Map;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.EntityManager;
import vn.edu.kma.ucon.engine.pdp.AuthDecision;
import vn.edu.kma.ucon.engine.pdp.DecisionTrace;
import vn.edu.kma.ucon.engine.pdp.DomainInvariantChecker;
import vn.edu.kma.ucon.engine.pdp.Environment;
import vn.edu.kma.ucon.engine.pdp.MaintenanceFlag;
import vn.edu.kma.ucon.engine.pdp.PhaseEvaluationResult;
import vn.edu.kma.ucon.engine.pdp.PhaseTrace;
import vn.edu.kma.ucon.engine.pdp.PolicyEngine;
import vn.edu.kma.ucon.engine.pip.entity.ClassSection;
import vn.edu.kma.ucon.engine.pip.entity.Student;
import vn.edu.kma.ucon.engine.pip.repository.ClassSectionRepository;
import vn.edu.kma.ucon.engine.pip.repository.StudentRepository;

@RestController
@RequestMapping("/api")
public class RegistrationController {

    private static final Logger log = LoggerFactory.getLogger(RegistrationController.class);

    private final StudentRepository studentRepo;
    private final ClassSectionRepository classRepo;
    private final PolicyEngine policyEngine;
    private final DomainInvariantChecker invariantChecker;
    private final EntityManager entityManager;
    private final MaintenanceFlag maintenanceFlag;

    public RegistrationController(StudentRepository stRepo,
                                  ClassSectionRepository clRepo,
                                  PolicyEngine pe,
                                  DomainInvariantChecker invariantChecker,
                                  EntityManager em,
                                  MaintenanceFlag mf) {
        this.studentRepo = stRepo;
        this.classRepo = clRepo;
        this.policyEngine = pe;
        this.invariantChecker = invariantChecker;
        this.entityManager = em;
        this.maintenanceFlag = mf;
    }

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<ApiDecisionResponse> register(@RequestBody UconRequest req) {
        if (req == null) {
            return badRequest("REGISTER", null, null, null, "Request body is required.");
        }
        initializeRequest(req, "REGISTER");
        log.info("[REQUEST] action={} requestId={} studentId={} classId={}",
                req.getActionType(), req.getRequestId(), req.getStudentId(), req.getClassId());

        if (!hasText(req.getStudentId()) || !hasText(req.getClassId())) {
            return badRequest(req.getActionType(), req.getRequestId(), req.getStudentId(), req.getClassId(),
                    "studentId and classId are required.");
        }

        Environment preEnv = buildEnvironment();
        Student student = studentRepo.findById(req.getStudentId()).orElse(null);
        ClassSection cls = classRepo.findById(req.getClassId()).orElse(null);

        if (student == null || cls == null) {
            return badRequest(req.getActionType(), req.getRequestId(), req.getStudentId(), req.getClassId(),
                    "Student or ClassSection not found.");
        }
        log.info("[STATE BEFORE] student={} class={}", studentSnapshot(student), classSnapshot(cls));
        log.info("[ENV PRE] {}", environmentSnapshot(preEnv));

        PhaseEvaluationResult preEvaluation = policyEngine.evaluatePhaseWithTrace("PRE", student, cls, preEnv, req);
        AuthDecision preDecision = preEvaluation.decision();
        log.info("[PHASE RESULT] phase=PRE permit={} failedCode={}",
                preDecision.isPermit(), preDecision.getFailedCode());
        if (!preDecision.isPermit()) {
            req.setDecision("DENY");
            req.setFailedPolicyCodes(preDecision.getFailedCode());
            PhaseTrace auditTrace = appendUpdates(preEvaluation.trace(), policyEngine.executeAuditLogOnly(student, cls, preEnv, req), List.of());
            log.warn("[REQUEST DENIED] action={} phase=PRE requestId={} failedCode={} failedPolicy={}",
                    req.getActionType(), req.getRequestId(), preDecision.getFailedCode(), preDecision.getFailedPolicy());
            return denyResponse("PRE", req, preDecision, traceFor(req, auditTrace));
        }
        List<String> preUpdates = policyEngine.executeUpdatesForPhase("PRE", student, cls, preEnv, req);
        studentRepo.save(student);
        classRepo.save(cls);
        entityManager.flush();
        invariantChecker.assertValid(student, cls);

        entityManager.refresh(student);
        entityManager.refresh(cls);
        log.info("[STATE REFRESHED] student={} class={}", studentSnapshot(student), classSnapshot(cls));

        Environment ongoingEnv = buildEnvironment();
        log.info("[ENV ONGOING] {}", environmentSnapshot(ongoingEnv));
        PhaseEvaluationResult ongoingEvaluation = policyEngine.evaluatePhaseWithTrace("ONGOING", student, cls, ongoingEnv, req);
        AuthDecision ongoingDecision = ongoingEvaluation.decision();
        log.info("[PHASE RESULT] phase=ONGOING permit={} failedCode={}",
                ongoingDecision.isPermit(), ongoingDecision.getFailedCode());
        if (!ongoingDecision.isPermit()) {
            req.setDecision("DENY");
            req.setFailedPolicyCodes(ongoingDecision.getFailedCode());
            PhaseTrace auditTrace = appendUpdates(ongoingEvaluation.trace(), policyEngine.executeAuditLogOnly(student, cls, ongoingEnv, req), List.of());
            log.warn("[REQUEST DENIED] action={} phase=ONGOING requestId={} failedCode={} failedPolicy={}",
                    req.getActionType(), req.getRequestId(), ongoingDecision.getFailedCode(), ongoingDecision.getFailedPolicy());
            return denyResponse("ONGOING", req, ongoingDecision, traceFor(req, appendUpdates(preEvaluation.trace(), preUpdates, List.of()), auditTrace));
        }

        List<String> ongoingUpdates = List.of();
        List<String> rollbackUpdates = List.of();
        List<String> postUpdates = List.of();
        try {
            ongoingUpdates = policyEngine.executeUpdatesForPhase("ONGOING", student, cls, ongoingEnv, req);
            req.setDecision("ALLOW");
            req.setFailedPolicyCodes("NONE");
            postUpdates = policyEngine.executeUpdatesForPhase("POST", student, cls, ongoingEnv, req);

            classRepo.save(cls);
            studentRepo.save(student);
            entityManager.flush();
            invariantChecker.assertValid(student, cls);
        } catch (RuntimeException ex) {
            rollbackUpdates = policyEngine.executeRollbackUpdatesForPhase("ONGOING", student, cls, ongoingEnv, req);
            throw ex;
        }
        log.info("[STATE AFTER] student={} class={}", studentSnapshot(student), classSnapshot(cls));
        DecisionTrace trace = traceFor(
                req,
                appendUpdates(preEvaluation.trace(), preUpdates, List.of()),
                appendUpdates(ongoingEvaluation.trace(), ongoingUpdates, rollbackUpdates),
                new PhaseTrace("POST", req.getActionType(), "ALLOW", null, null, List.of(), postUpdates, List.of()));
        ApiDecisionResponse response = successResponse(
                "POST",
                req,
                "Successfully enrolled.",
                "Request da vuot qua PRE, ONGOING va da thuc thi POST updates thanh cong.",
                trace);
        log.info("[REQUEST SUCCESS] action={} requestId={} decision={} response=\"{}\"",
                req.getActionType(), req.getRequestId(), req.getDecision(), response.getMessage());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/drop")
    @Transactional
    public ResponseEntity<ApiDecisionResponse> drop(@RequestBody UconRequest req) {
        if (req == null) {
            return badRequest("DROP", null, null, null, "Request body is required.");
        }
        initializeRequest(req, "DROP");
        log.info("[REQUEST] action={} requestId={} studentId={} classId={}",
                req.getActionType(), req.getRequestId(), req.getStudentId(), req.getClassId());

        if (!hasText(req.getStudentId()) || !hasText(req.getClassId())) {
            return badRequest(req.getActionType(), req.getRequestId(), req.getStudentId(), req.getClassId(),
                    "studentId and classId are required.");
        }

        Environment preEnv = buildEnvironment();
        Student student = studentRepo.findById(req.getStudentId()).orElse(null);
        ClassSection cls = classRepo.findById(req.getClassId()).orElse(null);

        if (student == null || cls == null) {
            return badRequest(req.getActionType(), req.getRequestId(), req.getStudentId(), req.getClassId(),
                    "Student or ClassSection not found.");
        }
        log.info("[STATE BEFORE] student={} class={}", studentSnapshot(student), classSnapshot(cls));
        log.info("[ENV PRE] {}", environmentSnapshot(preEnv));

        PhaseEvaluationResult preEvaluation = policyEngine.evaluatePhaseWithTrace("PRE", student, cls, preEnv, req);
        AuthDecision preDecision = preEvaluation.decision();
        log.info("[PHASE RESULT] phase=PRE permit={} failedCode={}",
                preDecision.isPermit(), preDecision.getFailedCode());
        if (!preDecision.isPermit()) {
            req.setDecision("DENY");
            req.setFailedPolicyCodes(preDecision.getFailedCode());
            PhaseTrace auditTrace = appendUpdates(preEvaluation.trace(), policyEngine.executeAuditLogOnly(student, cls, preEnv, req), List.of());
            log.warn("[REQUEST DENIED] action={} phase=PRE requestId={} failedCode={} failedPolicy={}",
                    req.getActionType(), req.getRequestId(), preDecision.getFailedCode(), preDecision.getFailedPolicy());
            return denyResponse("PRE", req, preDecision, traceFor(req, auditTrace));
        }
        List<String> preUpdates = policyEngine.executeUpdatesForPhase("PRE", student, cls, preEnv, req);
        studentRepo.save(student);
        classRepo.save(cls);
        entityManager.flush();
        invariantChecker.assertValid(student, cls);

        entityManager.refresh(student);
        entityManager.refresh(cls);
        log.info("[STATE REFRESHED] student={} class={}", studentSnapshot(student), classSnapshot(cls));

        Environment ongoingEnv = buildEnvironment();
        log.info("[ENV ONGOING] {}", environmentSnapshot(ongoingEnv));
        PhaseEvaluationResult ongoingEvaluation = policyEngine.evaluatePhaseWithTrace("ONGOING", student, cls, ongoingEnv, req);
        AuthDecision ongoingDecision = ongoingEvaluation.decision();
        log.info("[PHASE RESULT] phase=ONGOING permit={} failedCode={}",
                ongoingDecision.isPermit(), ongoingDecision.getFailedCode());
        if (!ongoingDecision.isPermit()) {
            req.setDecision("DENY");
            req.setFailedPolicyCodes(ongoingDecision.getFailedCode());
            PhaseTrace auditTrace = appendUpdates(ongoingEvaluation.trace(), policyEngine.executeAuditLogOnly(student, cls, ongoingEnv, req), List.of());
            log.warn("[REQUEST DENIED] action={} phase=ONGOING requestId={} failedCode={} failedPolicy={}",
                    req.getActionType(), req.getRequestId(), ongoingDecision.getFailedCode(), ongoingDecision.getFailedPolicy());
            return denyResponse("ONGOING", req, ongoingDecision, traceFor(req, appendUpdates(preEvaluation.trace(), preUpdates, List.of()), auditTrace));
        }

        List<String> ongoingUpdates = List.of();
        List<String> rollbackUpdates = List.of();
        List<String> postUpdates = List.of();
        try {
            ongoingUpdates = policyEngine.executeUpdatesForPhase("ONGOING", student, cls, ongoingEnv, req);
            req.setDecision("ALLOW");
            req.setFailedPolicyCodes("NONE");
            postUpdates = policyEngine.executeUpdatesForPhase("POST", student, cls, ongoingEnv, req);

            classRepo.save(cls);
            studentRepo.save(student);
            entityManager.flush();
            invariantChecker.assertValid(student, cls);
        } catch (RuntimeException ex) {
            rollbackUpdates = policyEngine.executeRollbackUpdatesForPhase("ONGOING", student, cls, ongoingEnv, req);
            throw ex;
        }
        log.info("[STATE AFTER] student={} class={}", studentSnapshot(student), classSnapshot(cls));
        DecisionTrace trace = traceFor(
                req,
                appendUpdates(preEvaluation.trace(), preUpdates, List.of()),
                appendUpdates(ongoingEvaluation.trace(), ongoingUpdates, rollbackUpdates),
                new PhaseTrace("POST", req.getActionType(), "ALLOW", null, null, List.of(), postUpdates, List.of()));
        ApiDecisionResponse response = successResponse(
                "POST",
                req,
                "Successfully dropped.",
                "Request da vuot qua PRE, ONGOING va da hoan tat POST updates de hoan tac state.",
                trace);
        log.info("[REQUEST SUCCESS] action={} requestId={} decision={} response=\"{}\"",
                req.getActionType(), req.getRequestId(), req.getDecision(), response.getMessage());

        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiDecisionResponse> handleOptimisticLockException(ObjectOptimisticLockingFailureException ex) {
        return ResponseEntity.status(409).body(new ApiDecisionResponse(
                null,
                null,
                "DENY",
                "COMMIT",
                null,
                null,
                null,
                "RACE_CONDITION",
                "Co xung dot optimistic locking o buoc commit, nghia la state da thay doi do request dong thoi khac.",
                "DENIED_RACE_CONDITION: concurrent enrollment update was detected.",
                null));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiDecisionResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return ResponseEntity.status(409).body(new ApiDecisionResponse(
                null,
                null,
                "DENY",
                "COMMIT",
                null,
                null,
                null,
                "DUPLICATE_REGISTRATION",
                "Database phat hien ban ghi dang ky trung lap tai buoc commit nen giao dich bi tu choi.",
                "DENIED_DUPLICATE_REGISTRATION: active registration already exists.",
                null));
    }

    private ResponseEntity<ApiDecisionResponse> badRequest(String action,
                                                           String requestId,
                                                           String studentId,
                                                           String classId,
                                                           String message) {
        return ResponseEntity.badRequest().body(new ApiDecisionResponse(
                requestId,
                action,
                "DENY",
                "VALIDATION",
                studentId,
                classId,
                null,
                "BAD_REQUEST",
                message,
                message,
                null));
    }

    private ResponseEntity<ApiDecisionResponse> denyResponse(String phase, UconRequest req, AuthDecision decision, DecisionTrace trace) {
        return ResponseEntity.status(403).body(new ApiDecisionResponse(
                req.getRequestId(),
                req.getActionType(),
                req.getDecision(),
                phase,
                req.getStudentId(),
                req.getClassId(),
                decision.getFailedPolicy(),
                decision.getFailedCode(),
                denyExplanation(decision.getFailedCode()),
                messageForPhase(phase, decision.getFailedCode()),
                trace));
    }

    private ApiDecisionResponse successResponse(String phase, UconRequest req, String message, String explanation, DecisionTrace trace) {
        return new ApiDecisionResponse(
                req.getRequestId(),
                req.getActionType(),
                req.getDecision(),
                phase,
                req.getStudentId(),
                req.getClassId(),
                null,
                null,
                explanation,
                message,
                trace);
    }

    private String messageForPhase(String phase, String failedCode) {
        if ("ONGOING".equals(phase)) {
            return "DENIED_ONGOING: " + failedCode;
        }
        return "DENIED_PREAUTH: " + failedCode;
    }

    private String denyExplanation(String failedCode) {
        Map<String, String> explanations = Map.ofEntries(
                Map.entry("TUITION_NOT_PAID", "Sinh vien chua hoan tat hoc phi nen request bi chan truoc khi dang ky xay ra."),
                Map.entry("OUTSIDE_TRANSACTION_WINDOW", "Thoi diem hien tai nam ngoai khung giao dich hop le cua dot dang ky."),
                Map.entry("CLASS_NOT_OPEN", "Lop hoc phan khong o trang thai OPEN nen khong the dang ky."),
                Map.entry("ALREADY_REGISTERED", "Sinh vien da co giao dich dang ky hop le cho lop hoc phan nay."),
                Map.entry("CREDIT_LIMIT_EXCEEDED", "Tong so tin chi sau khi dang ky vuot qua gioi han tin chi hieu luc."),
                Map.entry("PREREQUISITE_NOT_MET", "Sinh vien chua hoan thanh day du mon tien quyet cua hoc phan."),
                Map.entry("REGULATION_NOT_CONFIRMED", "Sinh vien chua xac nhan da doc quy che dang ky nen khong duoc tiep tuc request."),
                Map.entry("OVERRIDE_REASON_REQUIRED", "Request co su dung override hoc vu nhung khong cung cap ly do hop le."),
                Map.entry("SCHEDULE_CONFLICT", "Lich hoc cua lop moi bi trung voi lich hoc da dang ky."),
                Map.entry("CLASS_FULL_ON_COMMIT", "Tai thoi diem gan commit, lop da het cho nen request bi tu choi."),
                Map.entry("NO_SEAT_TO_RESERVE", "Khong the giu tam cho o pha ongoing vi so cho trong khong con du."),
                Map.entry("CLASS_STATUS_CHANGED", "Trang thai lop da thay doi giua PRE va ONGOING nen request khong con hop le."),
                Map.entry("STUDENT_ON_HOLD", "Sinh vien dang co hold hoc vu/ky luat nen khong duoc thuc hien giao dich."),
                Map.entry("SYSTEM_UNDER_MAINTENANCE", "He thong da chuyen sang trang thai maintenance trong luc giao dich dang duoc xu ly."),
                Map.entry("NOT_REGISTERED", "Khong ton tai giao dich dang ky hop le de thuc hien thao tac DROP."));
        return explanations.getOrDefault(failedCode, "Policy da tu choi request o pha hien tai.");
    }

    private void initializeRequest(UconRequest req, String actionType) {
        req.setActionType(actionType);
        req.setStudentId(trimToNull(req.getStudentId()));
        req.setClassId(trimToNull(req.getClassId()));
        req.setRequestId(normalizeRequestId(req.getRequestId()));
        if (req.getConfirmedRegistrationRule() == null) {
            req.setConfirmedRegistrationRule(Boolean.TRUE);
        }
        if (req.getAdminOverride() == null) {
            req.setAdminOverride(Boolean.FALSE);
        }
        req.setOverrideReason(trimToNull(req.getOverrideReason()));
    }

    private String normalizeRequestId(String requestId) {
        String normalized = trimToNull(requestId);
        return normalized != null ? normalized : UUID.randomUUID().toString();
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Environment buildEnvironment() {
        Environment env = new Environment("NORMAL", "2026-03-27");
        env.setOpenTime("2026-01-01");
        env.setCloseTime("2026-12-31");
        env.setSemester("2026_FALL");
        env.setIsMaintenance(maintenanceFlag.isActive());
        return env;
    }

    private String studentSnapshot(Student student) {
        return String.format(
                "{id=%s,currentCredits=%d,tuitionPaid=%s,tuitionDebt=%d,registerAttemptCount=%d,holds=%s,registeredClassIds=%s,registeredScheduleSlots=%s}",
                student.getStudentId(),
                student.getCurrentCredits(),
                student.isTuitionPaid(),
                student.getTuitionDebt(),
                student.getRegisterAttemptCount(),
                safe(student.getHolds()),
                safe(student.getRegisteredClassIds()),
                safe(student.getRegisteredScheduleSlots()));
    }

    private String classSnapshot(ClassSection cls) {
        return String.format(
                "{id=%s,status=%s,enrolled=%d,reservedSeats=%d,capacity=%d,scheduleSlots=%s,courseId=%s}",
                cls.getClassId(),
                safe(cls.getStatus()),
                cls.getEnrolled(),
                cls.getReservedSeats(),
                cls.getCapacity(),
                safe(cls.getScheduleSlots()),
                cls.getCourse() != null ? safe(cls.getCourse().getCourseId()) : "null");
    }

    private String environmentSnapshot(Environment env) {
        return String.format(
                "{phase=%s,currentDateTime=%s,openTime=%s,closeTime=%s,semester=%s,isMaintenance=%s}",
                safe(env.getRegistrationPhase()),
                safe(env.getCurrentDateTime()),
                safe(env.getOpenTime()),
                safe(env.getCloseTime()),
                safe(env.getSemester()),
                env.getIsMaintenance());
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "<empty>" : value;
    }

    private PhaseTrace appendUpdates(PhaseTrace original, List<String> updatesApplied, List<String> rollbackApplied) {
        return new PhaseTrace(
                original.phase(),
                original.action(),
                original.decision(),
                original.failedPolicy(),
                original.failedReason(),
                original.policies(),
                updatesApplied,
                rollbackApplied);
    }

    private DecisionTrace traceFor(UconRequest req, PhaseTrace... phases) {
        return new DecisionTrace(
                req.getRequestId(),
                req.getActionType(),
                req.getDecision(),
                req.getStudentId(),
                req.getClassId(),
                List.of(phases));
    }
}
