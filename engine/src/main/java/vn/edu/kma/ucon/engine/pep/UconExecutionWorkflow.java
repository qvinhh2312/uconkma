package vn.edu.kma.ucon.engine.pep;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import vn.edu.kma.ucon.engine.pdp.AuthDecision;
import vn.edu.kma.ucon.engine.pdp.AuthorizationEvaluator;
import vn.edu.kma.ucon.engine.pdp.ConditionEvaluator;
import vn.edu.kma.ucon.engine.pdp.DecisionTrace;
import vn.edu.kma.ucon.engine.pdp.DomainInvariantChecker;
import vn.edu.kma.ucon.engine.pdp.Environment;
import vn.edu.kma.ucon.engine.pdp.ObligationEvaluator;
import vn.edu.kma.ucon.engine.pdp.Phase;
import vn.edu.kma.ucon.engine.pdp.PhaseEvaluationResult;
import vn.edu.kma.ucon.engine.pdp.PhaseTrace;
import vn.edu.kma.ucon.engine.pdp.PredicateType;
import vn.edu.kma.ucon.engine.pip.PolicyInformationPoint;
import vn.edu.kma.ucon.engine.pip.entity.ClassSection;
import vn.edu.kma.ucon.engine.pip.entity.Student;
import vn.edu.kma.ucon.engine.pip.repository.ClassSectionRepository;
import vn.edu.kma.ucon.engine.pip.repository.StudentRepository;
import vn.edu.kma.ucon.engine.session.SessionStatus;
import vn.edu.kma.ucon.engine.session.UsageSession;
import vn.edu.kma.ucon.engine.session.UsageSessionService;
import vn.edu.kma.ucon.engine.update.RollbackManager;
import vn.edu.kma.ucon.engine.update.UpdateManager;
import vn.edu.kma.ucon.engine.update.UpdatePlan;

/**
 * Canonical PRE -> ONGOING -> POST workflow for register/drop requests.
 */
@Service
public class UconExecutionWorkflow {

    private static final Logger log = LoggerFactory.getLogger(UconExecutionWorkflow.class);

    private final ConditionEvaluator conditionEvaluator;
    private final AuthorizationEvaluator authorizationEvaluator;
    private final ObligationEvaluator obligationEvaluator;
    private final UpdateManager updateManager;
    private final RollbackManager rollbackManager;
    private final UsageSessionService usageSessionService;
    private final DomainInvariantChecker invariantChecker;
    private final StudentRepository studentRepository;
    private final ClassSectionRepository classSectionRepository;
    private final PolicyInformationPoint policyInformationPoint;
    private final EntityManager entityManager;

    public UconExecutionWorkflow(ConditionEvaluator conditionEvaluator,
                                 AuthorizationEvaluator authorizationEvaluator,
                                 ObligationEvaluator obligationEvaluator,
                                 UpdateManager updateManager,
                                 RollbackManager rollbackManager,
                                 UsageSessionService usageSessionService,
                                 DomainInvariantChecker invariantChecker,
                                 StudentRepository studentRepository,
                                 ClassSectionRepository classSectionRepository,
                                 PolicyInformationPoint policyInformationPoint,
                                 EntityManager entityManager) {
        this.conditionEvaluator = conditionEvaluator;
        this.authorizationEvaluator = authorizationEvaluator;
        this.obligationEvaluator = obligationEvaluator;
        this.updateManager = updateManager;
        this.rollbackManager = rollbackManager;
        this.usageSessionService = usageSessionService;
        this.invariantChecker = invariantChecker;
        this.studentRepository = studentRepository;
        this.classSectionRepository = classSectionRepository;
        this.policyInformationPoint = policyInformationPoint;
        this.entityManager = entityManager;
    }

    public UconWorkflowResult execute(UconContext context, String successMessage, String successExplanation) {
        context.setSnapshotBefore(snapshot(context.getStudent(), context.getClassSection()));
        logRequestStart(context);

        UconWorkflowResult deniedPre = evaluatePrePhase(context);
        if (deniedPre != null) {
            return deniedPre;
        }

        applyPreUpdates(context);

        UsageSession session = usageSessionService.createActive(context.getRequest());
        context.setUsageSession(session);
        context.setOngoingEnvironment(policyInformationPoint.buildEnvironment());
        log.info("[ENV ONGOING] {}", environmentSnapshot(context.getOngoingEnvironment()));

        UconWorkflowResult deniedOngoing = evaluateOngoingPhase(context);
        if (deniedOngoing != null) {
            return deniedOngoing;
        }

        return commit(context, successMessage, successExplanation);
    }

    private UconWorkflowResult evaluatePrePhase(UconContext context) {
        UconWorkflowResult denied = evaluateAndMaybeDeny(context, Phase.PRE, PredicateType.CONDITION, conditionEvaluator::evaluate);
        if (denied != null) {
            return denied;
        }
        denied = evaluateAndMaybeDeny(context, Phase.PRE, PredicateType.AUTHORIZATION, authorizationEvaluator::evaluate);
        if (denied != null) {
            return denied;
        }
        return evaluateAndMaybeDeny(context, Phase.PRE, PredicateType.OBLIGATION, obligationEvaluator::evaluate);
    }

    private UconWorkflowResult evaluateOngoingPhase(UconContext context) {
        UconWorkflowResult denied = evaluateAndMaybeDeny(context, Phase.ONGOING, PredicateType.CONDITION, conditionEvaluator::evaluate);
        if (denied != null) {
            return denied;
        }
        denied = evaluateAndMaybeDeny(context, Phase.ONGOING, PredicateType.AUTHORIZATION, authorizationEvaluator::evaluate);
        if (denied != null) {
            return denied;
        }
        return evaluateAndMaybeDeny(context, Phase.ONGOING, PredicateType.OBLIGATION, obligationEvaluator::evaluate);
    }

    private UconWorkflowResult evaluateAndMaybeDeny(UconContext context,
                                                    Phase phase,
                                                    PredicateType predicate,
                                                    PhasePredicateEvaluator evaluator) {
        Environment environment = phase == Phase.PRE ? context.getPreEnvironment() : context.getOngoingEnvironment();
        PhaseEvaluationResult result = evaluator.evaluate(
                phase,
                context.getStudent(),
                context.getClassSection(),
                environment,
                context.getRequest());
        context.getTraces().add(result.trace());
        if (result.decision().isPermit()) {
            return null;
        }

        SessionStatus sessionStatus = phase == Phase.ONGOING
                ? (DenyReasonCatalog.isRevocationCode(result.decision().getFailedCode()) ? SessionStatus.REVOKED : SessionStatus.FAILED)
                : null;
        return deny(context, phase, predicate, result.decision(), sessionStatus);
    }

    private void applyPreUpdates(UconContext context) {
        UpdatePlan prePlan = updateManager.buildPlan(
                Phase.PRE,
                context.getStudent(),
                context.getClassSection(),
                context.getPreEnvironment(),
                context.getRequest());
        List<String> applied = updateManager.apply(
                prePlan,
                context.getStudent(),
                context.getClassSection(),
                context.getPreEnvironment(),
                context.getRequest());
        applyPlanUpdatesToTraces(context.getTraces(), prePlan, applied);
        persistAndRefreshContext(context);
    }

    private UconWorkflowResult commit(UconContext context, String successMessage, String successExplanation) {
        UpdatePlan ongoingPlan = updateManager.buildPlan(
                Phase.ONGOING,
                context.getStudent(),
                context.getClassSection(),
                context.getOngoingEnvironment(),
                context.getRequest());
        UpdatePlan rollbackPlan = rollbackManager.buildPlan(
                Phase.ONGOING,
                context.getStudent(),
                context.getClassSection(),
                context.getOngoingEnvironment(),
                context.getRequest());

        UpdatePlan postPlan = null;
        List<String> ongoingApplied = List.of();
        List<String> rollbackApplied = List.of();
        List<String> postApplied = List.of();

        try {
            ongoingApplied = updateManager.apply(
                    ongoingPlan,
                    context.getStudent(),
                    context.getClassSection(),
                    context.getOngoingEnvironment(),
                    context.getRequest());
            applyPlanUpdatesToTraces(context.getTraces(), ongoingPlan, ongoingApplied);

            context.getRequest().setDecision("ALLOW");
            context.getRequest().setFailedPolicyCodes("NONE");
            postPlan = updateManager.buildPlan(
                    Phase.POST,
                    context.getStudent(),
                    context.getClassSection(),
                    context.getOngoingEnvironment(),
                    context.getRequest());
            postApplied = updateManager.apply(
                    postPlan,
                    context.getStudent(),
                    context.getClassSection(),
                    context.getOngoingEnvironment(),
                    context.getRequest());

            classSectionRepository.save(context.getClassSection());
            studentRepository.save(context.getStudent());
            entityManager.flush();
            invariantChecker.assertValid(context.getStudent(), context.getClassSection());
            usageSessionService.markCommitted(context.getUsageSession());
            context.setSnapshotAfter(snapshot(context.getStudent(), context.getClassSection()));
        } catch (RuntimeException ex) {
            rollbackApplied = rollbackManager.apply(
                    rollbackPlan,
                    context.getStudent(),
                    context.getClassSection(),
                    context.getOngoingEnvironment(),
                    context.getRequest());
            applyPlanRollbacksToTraces(context.getTraces(), rollbackPlan, rollbackApplied);
            usageSessionService.markFailed(context.getUsageSession(), ex.getMessage());
            throw ex;
        }

        appendPostPlanTraces(context.getTraces(), postPlan, postApplied, context.getRequest());
        log.info("[STATE AFTER] student={} class={}",
                studentSnapshot(context.getStudent()),
                classSnapshot(context.getClassSection()));

        DecisionTrace trace = traceFor(context);
        ApiDecisionResponse response = new ApiDecisionResponse(
                context.getRequest().getRequestId(),
                context.getRequest().getActionType(),
                context.getRequest().getDecision(),
                Phase.POST.name(),
                PredicateType.AUTHORIZATION.name(),
                context.getRequest().getStudentId(),
                context.getRequest().getClassId(),
                null,
                null,
                SessionStatus.COMMITTED.name(),
                successExplanation,
                successMessage,
                trace);
        log.info("[REQUEST SUCCESS] action={} requestId={} decision={} response=\"{}\" sessionId={} sessionStatus={}",
                context.getRequest().getActionType(),
                context.getRequest().getRequestId(),
                context.getRequest().getDecision(),
                response.getMessage(),
                context.getUsageSession().getSessionId(),
                SessionStatus.COMMITTED.name());
        return new UconWorkflowResult(HttpStatus.OK, response);
    }

    private UconWorkflowResult deny(UconContext context,
                                    Phase phase,
                                    PredicateType predicate,
                                    AuthDecision decision,
                                    SessionStatus sessionStatus) {
        context.getRequest().setDecision("DENY");
        context.getRequest().setFailedPolicyCodes(decision.getFailedCode());

        if (context.getUsageSession() != null && sessionStatus != null) {
            updateSessionByDecision(context.getUsageSession(), sessionStatus, decision.getFailedCode());
        }

        Environment environment = phase == Phase.PRE ? context.getPreEnvironment() : context.getOngoingEnvironment();
        UpdatePlan auditPlan = updateManager.buildAuditOnlyPlan(
                Phase.POST,
                context.getStudent(),
                context.getClassSection(),
                environment,
                context.getRequest());
        List<String> auditApplied = updateManager.apply(
                auditPlan,
                context.getStudent(),
                context.getClassSection(),
                environment,
                context.getRequest());
        appendPostPlanTraces(context.getTraces(), auditPlan, auditApplied, context.getRequest());

        UsageSession session = context.getUsageSession();
        if (session == null) {
            log.warn("[REQUEST DENIED] action={} phase={} requestId={} failedCode={} failedPolicy={}",
                    context.getRequest().getActionType(),
                    phase.name(),
                    context.getRequest().getRequestId(),
                    decision.getFailedCode(),
                    decision.getFailedPolicy());
        } else {
            log.warn("[REQUEST DENIED] action={} phase={} requestId={} failedCode={} failedPolicy={} sessionId={} sessionStatus={}",
                    context.getRequest().getActionType(),
                    phase.name(),
                    context.getRequest().getRequestId(),
                    decision.getFailedCode(),
                    decision.getFailedPolicy(),
                    session.getSessionId(),
                    session.getStatus().name());
        }

        DecisionTrace trace = traceFor(context);
        ApiDecisionResponse response = new ApiDecisionResponse(
                context.getRequest().getRequestId(),
                context.getRequest().getActionType(),
                context.getRequest().getDecision(),
                phase.name(),
                predicate.name(),
                context.getRequest().getStudentId(),
                context.getRequest().getClassId(),
                decision.getFailedPolicy(),
                decision.getFailedCode(),
                context.getUsageSession() != null ? context.getUsageSession().getStatus().name() : SessionStatus.FAILED.name(),
                DenyReasonCatalog.explanationFor(decision.getFailedCode()),
                messageForPhase(phase, decision.getFailedCode()),
                trace);
        return new UconWorkflowResult(HttpStatus.FORBIDDEN, response);
    }

    private void persistAndRefreshContext(UconContext context) {
        studentRepository.save(context.getStudent());
        classSectionRepository.save(context.getClassSection());
        entityManager.flush();
        invariantChecker.assertValid(context.getStudent(), context.getClassSection());
        entityManager.refresh(context.getStudent());
        entityManager.refresh(context.getClassSection());
        log.info("[STATE REFRESHED] student={} class={}",
                studentSnapshot(context.getStudent()),
                classSnapshot(context.getClassSection()));
    }

    private void updateSessionByDecision(UsageSession session, SessionStatus status, String reason) {
        if (status == SessionStatus.REVOKED) {
            usageSessionService.markRevoked(session, reason);
        } else {
            usageSessionService.markFailed(session, reason);
        }
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
                    .filter(planned -> planned.predicate().equals(trace.predicate()) && appliedPolicyIds.contains(planned.policyId()))
                    .map(planned -> planned.policyId())
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
                    .filter(planned -> planned.predicate().equals(trace.predicate()) && rollbackPolicyIds.contains(planned.policyId()))
                    .map(planned -> planned.policyId())
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

    private void appendPostPlanTraces(List<PhaseTrace> traces, UpdatePlan postPlan, List<String> appliedPolicyIds, UconRequest request) {
        if (postPlan == null || postPlan.plannedPolicies().isEmpty()) {
            return;
        }
        for (PredicateType predicate : List.of(PredicateType.AUTHORIZATION, PredicateType.OBLIGATION)) {
            List<String> predicatePolicies = postPlan.plannedPolicies().stream()
                    .filter(planned -> predicate.name().equals(planned.predicate()) && appliedPolicyIds.contains(planned.policyId()))
                    .map(planned -> planned.policyId())
                    .toList();
            if (!predicatePolicies.isEmpty()) {
                traces.add(new PhaseTrace(
                        Phase.POST.name(),
                        predicate.name(),
                        request.getActionType(),
                        request.getDecision(),
                        null,
                        null,
                        List.of(),
                        predicatePolicies,
                        List.of()));
            }
        }
    }

    private String messageForPhase(Phase phase, String failedCode) {
        if (phase == Phase.ONGOING) {
            return "DENIED_ONGOING: " + failedCode;
        }
        return "DENIED_PRE: " + failedCode;
    }

    private void logRequestStart(UconContext context) {
        log.info("[REQUEST] action={} requestId={} studentId={} classId={}",
                context.getRequest().getActionType(),
                context.getRequest().getRequestId(),
                context.getRequest().getStudentId(),
                context.getRequest().getClassId());
        log.info("[STATE BEFORE] student={} class={}",
                studentSnapshot(context.getStudent()),
                classSnapshot(context.getClassSection()));
        log.info("[ENV PRE] {}", environmentSnapshot(context.getPreEnvironment()));
    }

    private DecisionTrace traceFor(UconContext context) {
        UsageSession session = context.getUsageSession();
        if (context.getSnapshotAfter() == null) {
            context.setSnapshotAfter(snapshot(context.getStudent(), context.getClassSection()));
        }
        return new DecisionTrace(
                context.getRequest().getRequestId(),
                context.getRequest().getActionType(),
                context.getRequest().getDecision(),
                context.getRequest().getStudentId(),
                context.getRequest().getClassId(),
                session != null ? session.getSessionId() : null,
                session != null && session.getStatus() != null ? session.getStatus().name() : null,
                context.getSnapshotBefore(),
                context.getSnapshotAfter(),
                List.copyOf(context.getTraces()));
    }

    private Map<String, Object> snapshot(Student student, ClassSection classSection) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("subject.studentId", student.getStudentId());
        snapshot.put("subject.currentCredits", student.getCurrentCredits());
        snapshot.put("subject.tuitionDebt", student.getTuitionDebt());
        snapshot.put("subject.registerAttemptCount", student.getRegisterAttemptCount());
        snapshot.put("subject.dropCountForSemester", student.getDropCountForSemester());
        snapshot.put("subject.registeredClassIds", safe(student.getRegisteredClassIds()));
        snapshot.put("object.classId", classSection.getClassId());
        snapshot.put("object.status", safe(classSection.getStatus()));
        snapshot.put("object.enrolled", classSection.getEnrolled());
        snapshot.put("object.reservedSeats", classSection.getReservedSeats());
        snapshot.put("object.capacity", classSection.getCapacity());
        return snapshot;
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

    private String classSnapshot(ClassSection classSection) {
        return String.format(
                "{id=%s,status=%s,enrolled=%d,reservedSeats=%d,capacity=%d,scheduleSlots=%s,courseId=%s}",
                classSection.getClassId(),
                safe(classSection.getStatus()),
                classSection.getEnrolled(),
                classSection.getReservedSeats(),
                classSection.getCapacity(),
                safe(classSection.getScheduleSlots()),
                classSection.getCourse() != null ? safe(classSection.getCourse().getCourseId()) : "null");
    }

    private String environmentSnapshot(Environment environment) {
        return String.format(
                "{phase=%s,currentDateTime=%s,openTime=%s,closeTime=%s,semester=%s,isMaintenance=%s,maxRegisterAttempts=%d,maxDropTimes=%d}",
                safe(environment.getRegistrationPhase()),
                safe(environment.getCurrentDateTime()),
                safe(environment.getOpenTime()),
                safe(environment.getCloseTime()),
                safe(environment.getSemester()),
                environment.getIsMaintenance(),
                environment.getMaxRegisterAttempts(),
                environment.getMaxDropTimes());
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "<empty>" : value;
    }

    @FunctionalInterface
    private interface PhasePredicateEvaluator {
        PhaseEvaluationResult evaluate(Phase phase,
                                       Student subject,
                                       ClassSection object,
                                       Environment environment,
                                       UconRequest request);
    }
}
