package vn.edu.kma.ucon.engine.pep;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.persistence.EntityManager;
import vn.edu.kma.ucon.engine.pdp.AuthDecision;
import vn.edu.kma.ucon.engine.pdp.AuthorizationEvaluator;
import vn.edu.kma.ucon.engine.pdp.ConditionEvaluator;
import vn.edu.kma.ucon.engine.pdp.DecisionTrace;
import vn.edu.kma.ucon.engine.pdp.DomainInvariantChecker;
import vn.edu.kma.ucon.engine.pdp.Environment;
import vn.edu.kma.ucon.engine.pdp.MaintenanceFlag;
import vn.edu.kma.ucon.engine.pdp.ObligationEvaluator;
import vn.edu.kma.ucon.engine.pdp.Phase;
import vn.edu.kma.ucon.engine.pdp.PhaseEvaluationResult;
import vn.edu.kma.ucon.engine.pdp.PhaseTrace;
import vn.edu.kma.ucon.engine.pdp.PolicyEngine;
import vn.edu.kma.ucon.engine.pdp.PredicateType;
import vn.edu.kma.ucon.engine.pip.entity.ClassSection;
import vn.edu.kma.ucon.engine.pip.entity.Student;
import vn.edu.kma.ucon.engine.pip.repository.ClassSectionRepository;
import vn.edu.kma.ucon.engine.pip.repository.StudentRepository;
import vn.edu.kma.ucon.engine.session.SessionStatus;
import vn.edu.kma.ucon.engine.session.UsageSession;
import vn.edu.kma.ucon.engine.session.UsageSessionService;
import vn.edu.kma.ucon.engine.update.UpdateManager;
import vn.edu.kma.ucon.engine.update.UpdatePlan;
import vn.edu.kma.ucon.engine.update.RollbackManager;

@RestController
@RequestMapping("/api")
public class RegistrationController {

    private static final Logger log = LoggerFactory.getLogger(RegistrationController.class);

    private final StudentRepository studentRepo;
    private final ClassSectionRepository classRepo;
    private final PolicyEngine policyEngine;
    private final ConditionEvaluator conditionEvaluator;
    private final AuthorizationEvaluator authorizationEvaluator;
    private final ObligationEvaluator obligationEvaluator;
    private final UpdateManager updateManager;
    private final RollbackManager rollbackManager;
    private final UsageSessionService usageSessionService;
    private final DomainInvariantChecker invariantChecker;
    private final EntityManager entityManager;
    private final MaintenanceFlag maintenanceFlag;

    public RegistrationController(StudentRepository stRepo,
                                  ClassSectionRepository clRepo,
                                  PolicyEngine policyEngine,
                                  ConditionEvaluator conditionEvaluator,
                                  AuthorizationEvaluator authorizationEvaluator,
                                  ObligationEvaluator obligationEvaluator,
                                  UpdateManager updateManager,
                                  RollbackManager rollbackManager,
                                  UsageSessionService usageSessionService,
                                  DomainInvariantChecker invariantChecker,
                                  EntityManager em,
                                  MaintenanceFlag mf) {
        this.studentRepo = stRepo;
        this.classRepo = clRepo;
        this.policyEngine = policyEngine;
        this.conditionEvaluator = conditionEvaluator;
        this.authorizationEvaluator = authorizationEvaluator;
        this.obligationEvaluator = obligationEvaluator;
        this.updateManager = updateManager;
        this.rollbackManager = rollbackManager;
        this.usageSessionService = usageSessionService;
        this.invariantChecker = invariantChecker;
        this.entityManager = em;
        this.maintenanceFlag = mf;
    }

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<ApiDecisionResponse> register(@RequestBody UconRequest req) {
        return handleAction(req, "REGISTER", "Successfully enrolled.",
                "Request da vuot qua PRE, ONGOING va da thuc thi day du update/obligation cua UCON.");
    }

    @PostMapping("/drop")
    @Transactional
    public ResponseEntity<ApiDecisionResponse> drop(@RequestBody UconRequest req) {
        return handleAction(req, "DROP", "Successfully dropped.",
                "Request da vuot qua PRE, ONGOING va da hoan tac state qua POST update cua UCON.");
    }

    private ResponseEntity<ApiDecisionResponse> handleAction(UconRequest req, String actionType, String successMessage, String successExplanation) {
        if (req == null) {
            return badRequest(actionType, null, null, null, "Request body is required.");
        }
        initializeRequest(req, actionType);
        log.info("[REQUEST] action={} requestId={} studentId={} classId={}",
                req.getActionType(), req.getRequestId(), req.getStudentId(), req.getClassId());

        if (!hasText(req.getStudentId()) || !hasText(req.getClassId())) {
            return badRequest(req.getActionType(), req.getRequestId(), req.getStudentId(), req.getClassId(),
                    "studentId and classId are required.");
        }

        Student student = studentRepo.findById(req.getStudentId()).orElse(null);
        ClassSection cls = classRepo.findById(req.getClassId()).orElse(null);
        if (student == null || cls == null) {
            return badRequest(req.getActionType(), req.getRequestId(), req.getStudentId(), req.getClassId(),
                    "Student or ClassSection not found.");
        }

        Environment preEnv = buildEnvironment();
        log.info("[STATE BEFORE] student={} class={}", studentSnapshot(student), classSnapshot(cls));
        log.info("[ENV PRE] {}", environmentSnapshot(preEnv));

        List<PhaseTrace> traces = new ArrayList<>();

        PhaseEvaluationResult preCondition = conditionEvaluator.evaluate(Phase.PRE, student, cls, preEnv, req);
        traces.add(preCondition.trace());
        if (!preCondition.decision().isPermit()) {
            return denyBeforeSession("PRE", req, student, cls, preEnv, preCondition.decision(), traces);
        }

        PhaseEvaluationResult preAuthorization = authorizationEvaluator.evaluate(Phase.PRE, student, cls, preEnv, req);
        traces.add(preAuthorization.trace());
        if (!preAuthorization.decision().isPermit()) {
            return denyBeforeSession("PRE", req, student, cls, preEnv, preAuthorization.decision(), traces);
        }

        PhaseEvaluationResult preObligation = obligationEvaluator.evaluate(Phase.PRE, student, cls, preEnv, req);
        traces.add(preObligation.trace());
        if (!preObligation.decision().isPermit()) {
            return denyBeforeSession("PRE", req, student, cls, preEnv, preObligation.decision(), traces);
        }

        UpdatePlan prePlan = updateManager.buildPlan(Phase.PRE, student, cls, preEnv, req);
        List<String> preApplied = updateManager.apply(prePlan, student, cls, preEnv, req);
        applyPlanUpdatesToTraces(traces, prePlan, preApplied);
        studentRepo.save(student);
        classRepo.save(cls);
        entityManager.flush();
        invariantChecker.assertValid(student, cls);

        entityManager.refresh(student);
        entityManager.refresh(cls);
        log.info("[STATE REFRESHED] student={} class={}", studentSnapshot(student), classSnapshot(cls));

        UsageSession session = usageSessionService.createActive(req);
        Environment ongoingEnv = buildEnvironment();
        log.info("[ENV ONGOING] {}", environmentSnapshot(ongoingEnv));

        PhaseEvaluationResult ongoingCondition = conditionEvaluator.evaluate(Phase.ONGOING, student, cls, ongoingEnv, req);
        traces.add(ongoingCondition.trace());
        if (!ongoingCondition.decision().isPermit()) {
            usageSessionService.markRevoked(session, ongoingCondition.decision().getFailedCode());
            return denyAfterSession("ONGOING", req, student, cls, ongoingEnv, ongoingCondition.decision(), session, traces);
        }

        PhaseEvaluationResult ongoingAuthorization = authorizationEvaluator.evaluate(Phase.ONGOING, student, cls, ongoingEnv, req);
        traces.add(ongoingAuthorization.trace());
        if (!ongoingAuthorization.decision().isPermit()) {
            SessionStatus status = isRevocation(ongoingAuthorization.decision()) ? SessionStatus.REVOKED : SessionStatus.FAILED;
            updateSessionByDecision(session, status, ongoingAuthorization.decision().getFailedCode());
            return denyAfterSession("ONGOING", req, student, cls, ongoingEnv, ongoingAuthorization.decision(), session, traces);
        }

        PhaseEvaluationResult ongoingObligation = obligationEvaluator.evaluate(Phase.ONGOING, student, cls, ongoingEnv, req);
        traces.add(ongoingObligation.trace());
        if (!ongoingObligation.decision().isPermit()) {
            usageSessionService.markRevoked(session, ongoingObligation.decision().getFailedCode());
            return denyAfterSession("ONGOING", req, student, cls, ongoingEnv, ongoingObligation.decision(), session, traces);
        }

        UpdatePlan ongoingPlan = updateManager.buildPlan(Phase.ONGOING, student, cls, ongoingEnv, req);
        UpdatePlan rollbackPlan = rollbackManager.buildPlan(Phase.ONGOING, student, cls, ongoingEnv, req);
        UpdatePlan postPlan = null;
        List<String> ongoingApplied = List.of();
        List<String> rollbackApplied = List.of();
        List<String> postApplied = List.of();

        try {
            ongoingApplied = updateManager.apply(ongoingPlan, student, cls, ongoingEnv, req);
            applyPlanUpdatesToTraces(traces, ongoingPlan, ongoingApplied);

            req.setDecision("ALLOW");
            req.setFailedPolicyCodes("NONE");
            postPlan = updateManager.buildPlan(Phase.POST, student, cls, ongoingEnv, req);
            postApplied = updateManager.apply(postPlan, student, cls, ongoingEnv, req);

            classRepo.save(cls);
            studentRepo.save(student);
            entityManager.flush();
            invariantChecker.assertValid(student, cls);
            usageSessionService.markCommitted(session);
        } catch (RuntimeException ex) {
            rollbackApplied = rollbackManager.apply(rollbackPlan, student, cls, ongoingEnv, req);
            applyPlanRollbacksToTraces(traces, rollbackPlan, rollbackApplied);
            usageSessionService.markFailed(session, ex.getMessage());
            throw ex;
        }

        appendPostPlanTraces(traces, postPlan, postApplied, req);
        log.info("[STATE AFTER] student={} class={}", studentSnapshot(student), classSnapshot(cls));

        DecisionTrace trace = traceFor(req, session, traces);
        ApiDecisionResponse response = successResponse("POST", req, successMessage, successExplanation, trace);
        log.info("[REQUEST SUCCESS] action={} requestId={} decision={} response=\"{}\" sessionId={} sessionStatus={}",
                req.getActionType(), req.getRequestId(), req.getDecision(), response.getMessage(),
                session.getSessionId(), SessionStatus.COMMITTED.name());
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

    private ResponseEntity<ApiDecisionResponse> denyBeforeSession(String phase,
                                                                 UconRequest req,
                                                                 Student student,
                                                                 ClassSection cls,
                                                                 Environment env,
                                                                 AuthDecision decision,
                                                                 List<PhaseTrace> traces) {
        req.setDecision("DENY");
        req.setFailedPolicyCodes(decision.getFailedCode());
        UpdatePlan auditPlan = updateManager.buildAuditOnlyPlan(Phase.POST, student, cls, env, req);
        List<String> auditApplied = updateManager.apply(auditPlan, student, cls, env, req);
        appendPostPlanTraces(traces, auditPlan, auditApplied, req);
        log.warn("[REQUEST DENIED] action={} phase={} requestId={} failedCode={} failedPolicy={}",
                req.getActionType(), phase, req.getRequestId(), decision.getFailedCode(), decision.getFailedPolicy());
        return denyResponse(phase, req, decision, traceFor(req, null, traces));
    }

    private ResponseEntity<ApiDecisionResponse> denyAfterSession(String phase,
                                                                UconRequest req,
                                                                Student student,
                                                                ClassSection cls,
                                                                Environment env,
                                                                AuthDecision decision,
                                                                UsageSession session,
                                                                List<PhaseTrace> traces) {
        req.setDecision("DENY");
        req.setFailedPolicyCodes(decision.getFailedCode());
        UpdatePlan auditPlan = updateManager.buildAuditOnlyPlan(Phase.POST, student, cls, env, req);
        List<String> auditApplied = updateManager.apply(auditPlan, student, cls, env, req);
        appendPostPlanTraces(traces, auditPlan, auditApplied, req);
        log.warn("[REQUEST DENIED] action={} phase={} requestId={} failedCode={} failedPolicy={} sessionId={} sessionStatus={}",
                req.getActionType(), phase, req.getRequestId(), decision.getFailedCode(), decision.getFailedPolicy(),
                session.getSessionId(), session.getStatus().name());
        return denyResponse(phase, req, decision, traceFor(req, session, traces));
    }

    private void applyPlanUpdatesToTraces(List<PhaseTrace> traces, UpdatePlan plan, List<String> appliedPolicyIds) {
        if (plan == null || plan.plannedPolicies().isEmpty()) {
            return;
        }
        for (int i = 0; i < traces.size(); i++) {
            PhaseTrace trace = traces.get(i);
            if (!plan.phase().name().equals(trace.phase())) {
                continue;
            }
            List<String> updatesForPredicate = plan.plannedPolicies().stream()
                    .filter(pp -> pp.predicate().equals(trace.predicate()) && appliedPolicyIds.contains(pp.policyId()))
                    .map(pp -> pp.policyId())
                    .toList();
            if (!updatesForPredicate.isEmpty()) {
                traces.set(i, new PhaseTrace(
                        trace.phase(),
                        trace.predicate(),
                        trace.action(),
                        trace.decision(),
                        trace.failedPolicy(),
                        trace.failedReason(),
                        trace.policies(),
                        updatesForPredicate,
                        trace.rollbackApplied()));
            }
        }
    }

    private void applyPlanRollbacksToTraces(List<PhaseTrace> traces, UpdatePlan plan, List<String> rollbackPolicyIds) {
        if (plan == null || plan.plannedPolicies().isEmpty()) {
            return;
        }
        for (int i = 0; i < traces.size(); i++) {
            PhaseTrace trace = traces.get(i);
            if (!plan.phase().name().equals(trace.phase())) {
                continue;
            }
            List<String> rollbacksForPredicate = plan.plannedPolicies().stream()
                    .filter(pp -> pp.predicate().equals(trace.predicate()) && rollbackPolicyIds.contains(pp.policyId()))
                    .map(pp -> pp.policyId())
                    .toList();
            if (!rollbacksForPredicate.isEmpty()) {
                traces.set(i, new PhaseTrace(
                        trace.phase(),
                        trace.predicate(),
                        trace.action(),
                        trace.decision(),
                        trace.failedPolicy(),
                        trace.failedReason(),
                        trace.policies(),
                        trace.updatesApplied(),
                        rollbacksForPredicate));
            }
        }
    }

    private void appendPostPlanTraces(List<PhaseTrace> traces, UpdatePlan postPlan, List<String> appliedPolicyIds, UconRequest req) {
        if (postPlan == null || postPlan.plannedPolicies().isEmpty()) {
            return;
        }
        for (PredicateType predicate : List.of(PredicateType.AUTHORIZATION, PredicateType.OBLIGATION)) {
            List<String> predicatePolicies = postPlan.plannedPolicies().stream()
                    .filter(pp -> predicate.name().equals(pp.predicate()) && appliedPolicyIds.contains(pp.policyId()))
                    .map(pp -> pp.policyId())
                    .toList();
            if (!predicatePolicies.isEmpty()) {
                traces.add(new PhaseTrace(
                        Phase.POST.name(),
                        predicate.name(),
                        req.getActionType(),
                        req.getDecision(),
                        null,
                        null,
                        List.of(),
                        predicatePolicies,
                        List.of()));
            }
        }
    }

    private void updateSessionByDecision(UsageSession session, SessionStatus status, String reason) {
        if (status == SessionStatus.REVOKED) {
            usageSessionService.markRevoked(session, reason);
        } else {
            usageSessionService.markFailed(session, reason);
        }
    }

    private boolean isRevocation(AuthDecision decision) {
        return "SYSTEM_UNDER_MAINTENANCE".equals(decision.getFailedCode())
                || "CLASS_STATUS_CHANGED".equals(decision.getFailedCode());
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
        env.setMaxRegisterAttempts(5);
        env.setMaxDropTimes(2);
        return env;
    }

    private String studentSnapshot(Student student) {
        return String.format(
                "{id=%s,currentCredits=%d,tuitionPaid=%s,tuitionDebt=%d,registerAttemptCount=%d,dropCountForSemester=%d,holds=%s,registeredClassIds=%s,registeredScheduleSlots=%s}",
                student.getStudentId(),
                student.getCurrentCredits(),
                student.isTuitionPaid(),
                student.getTuitionDebt(),
                student.getRegisterAttemptCount(),
                student.getDropCountForSemester(),
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
                "{phase=%s,currentDateTime=%s,openTime=%s,closeTime=%s,semester=%s,isMaintenance=%s,maxRegisterAttempts=%d,maxDropTimes=%d}",
                safe(env.getRegistrationPhase()),
                safe(env.getCurrentDateTime()),
                safe(env.getOpenTime()),
                safe(env.getCloseTime()),
                safe(env.getSemester()),
                env.getIsMaintenance(),
                env.getMaxRegisterAttempts(),
                env.getMaxDropTimes());
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "<empty>" : value;
    }

    private DecisionTrace traceFor(UconRequest req, UsageSession session, List<PhaseTrace> phases) {
        return new DecisionTrace(
                req.getRequestId(),
                req.getActionType(),
                req.getDecision(),
                req.getStudentId(),
                req.getClassId(),
                session != null ? session.getSessionId() : null,
                session != null && session.getStatus() != null ? session.getStatus().name() : null,
                List.copyOf(phases));
    }
}
