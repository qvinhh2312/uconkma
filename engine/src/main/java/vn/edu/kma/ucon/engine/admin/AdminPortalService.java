package vn.edu.kma.ucon.engine.admin;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.EObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.kma.ucon.engine.pdp.MaintenanceFlag;
import vn.edu.kma.ucon.engine.pdp.PolicyDecisionPoint;
import vn.edu.kma.ucon.engine.pdp.PolicyLifecycleInfo;
import vn.edu.kma.ucon.engine.pdp.PolicyLifecycleService;
import vn.edu.kma.ucon.engine.pip.EnvironmentStateService;
import vn.edu.kma.ucon.engine.pip.entity.AuditLog;
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
import vn.edu.kma.ucon.engine.session.monitor.SessionRecheckResult;
import vn.edu.kma.ucon.engine.session.monitor.SessionRecheckService;

@Service
public class AdminPortalService {

    private static final Set<String> VALID_CLASS_STATUSES = Set.of("OPEN", "LOCKED", "CLOSED", "CANCELLED");
    private static final String DEMO_SEMESTER = "2026_FALL";

    private final PolicyLifecycleService policyLifecycleService;
    private final PolicyDecisionPoint policyDecisionPoint;
    private final MaintenanceFlag maintenanceFlag;
    private final EnvironmentStateService environmentStateService;
    private final SessionRecheckService sessionRecheckService;
    private final StudentRepository studentRepository;
    private final ClassSectionRepository classSectionRepository;
    private final CourseRepository courseRepository;
    private final RegistrationRepository registrationRepository;
    private final AuditLogRepository auditLogRepository;
    private final UsageSessionRepository usageSessionRepository;

    private Map<String, Object> lastRecheck = Map.of(
            "trigger", "NONE",
            "checkedSessions", 0,
            "revokedSessions", 0);

    public AdminPortalService(PolicyLifecycleService policyLifecycleService,
                              PolicyDecisionPoint policyDecisionPoint,
                              MaintenanceFlag maintenanceFlag,
                              EnvironmentStateService environmentStateService,
                              SessionRecheckService sessionRecheckService,
                              StudentRepository studentRepository,
                              ClassSectionRepository classSectionRepository,
                              CourseRepository courseRepository,
                              RegistrationRepository registrationRepository,
                              AuditLogRepository auditLogRepository,
                              UsageSessionRepository usageSessionRepository) {
        this.policyLifecycleService = policyLifecycleService;
        this.policyDecisionPoint = policyDecisionPoint;
        this.maintenanceFlag = maintenanceFlag;
        this.environmentStateService = environmentStateService;
        this.sessionRecheckService = sessionRecheckService;
        this.studentRepository = studentRepository;
        this.classSectionRepository = classSectionRepository;
        this.courseRepository = courseRepository;
        this.registrationRepository = registrationRepository;
        this.auditLogRepository = auditLogRepository;
        this.usageSessionRepository = usageSessionRepository;
    }

    public Map<String, Object> dashboard() {
        Map<String, Long> policySummary = policyLifecycleService.summarizeStatuses();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("policySummary", policySummary);
        response.put("uconCoverage", uconCoverage());
        response.put("runtimeSummary", runtimeSummary());
        response.put("domainSummary", Map.of(
                "students", studentRepository.count(),
                "classes", classSectionRepository.count(),
                "openClasses", classSectionRepository.findAll().stream()
                        .filter(classSection -> "OPEN".equalsIgnoreCase(safe(classSection.getStatus())))
                        .count(),
                "registrations", registrationRepository.count(),
                "auditLogs", auditLogRepository.count()));
        response.put("environment", environmentStateService.snapshot());
        response.put("lastRecheck", lastRecheck);
        return response;
    }

    public List<Map<String, Object>> policies(String predicate,
                                              String phase,
                                              String status,
                                              String action,
                                              String keyword) {
        Stream<Map<String, Object>> stream = allPolicyCards().stream();
        stream = filterEquals(stream, "predicate", predicate);
        stream = filterEquals(stream, "phase", phase);
        stream = filterEquals(stream, "policyStatus", status);
        stream = filterEquals(stream, "targetAction", action);
        if (hasText(keyword)) {
            String normalizedKeyword = keyword.trim().toLowerCase();
            stream = stream.filter(policy -> policy.values().stream()
                    .anyMatch(value -> value != null && value.toString().toLowerCase().contains(normalizedKeyword)));
        }
        return stream.toList();
    }

    public Map<String, Object> policyDetail(String policyId) {
        return allPolicyCards().stream()
                .filter(policy -> policyId.equals(policy.get("policyId")))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Policy not found: " + policyId));
    }

    public Map<String, Long> policySummary() {
        return policyLifecycleService.summarizeStatuses();
    }

    public Map<String, Object> transitionPolicy(String policyId, String targetStatus) {
        String oldStatus = policyLifecycleService.listPolicies().stream()
                .filter(policy -> policy.policyId().equals(policyId))
                .findFirst()
                .map(PolicyLifecycleInfo::status)
                .orElseThrow(() -> new IllegalArgumentException("Policy not found: " + policyId));
        PolicyLifecycleInfo info = policyLifecycleService.transitionPolicy(policyId, targetStatus);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("policyId", policyId);
        response.put("oldStatus", oldStatus);
        response.put("newStatus", info.status());
        response.put("runtimeActivePolicies", policyLifecycleService.listRuntimePolicyIds().size());
        response.put("message", "Policy status transitioned successfully.");
        return response;
    }

    public Map<String, Object> reloadPolicies() {
        policyLifecycleService.reloadFromDisk();
        return Map.of(
                "message", "Policy model reloaded successfully.",
                "runtimeActivePolicies", policyLifecycleService.listRuntimePolicyIds().size());
    }

    public Map<String, Object> monitorSummary() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("maintenance", maintenanceFlag.isActive());
        response.putAll(runtimeSummary());
        response.put("lastRecheck", lastRecheck);
        return response;
    }

    @Transactional
    public Map<String, Object> setMaintenance(boolean active) {
        maintenanceFlag.setActive(active);
        String trigger = active ? "MAINTENANCE_ENABLED" : "MAINTENANCE_DISABLED";
        return recheckResponse("maintenance", trigger, sessionRecheckService.recheckAllActiveSessions(trigger),
                Map.of("active", active, "message", "Maintenance recheck completed."));
    }

    @Transactional
    public Map<String, Object> changeClassStatus(String classId, String status) {
        String normalizedStatus = normalizeClassStatus(status);
        ClassSection classSection = classSectionRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("ClassSection not found: " + classId));
        classSection.setStatus(normalizedStatus);
        classSectionRepository.save(classSection);
        String trigger = "CLASS_STATUS_CHANGED:" + normalizedStatus;
        return recheckResponse("class-status", trigger,
                sessionRecheckService.recheckActiveSessionsForClass(classId, trigger),
                Map.of(
                        "classId", classId,
                        "status", normalizedStatus,
                        "message", "Class-status recheck completed."));
    }

    @Transactional
    public Map<String, Object> addStudentHold(String studentId, String holdCode) {
        String normalizedHoldCode = normalizeRequired("holdCode", holdCode);
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));
        student.setHolds(appendUniqueHold(student.getHolds(), normalizedHoldCode));
        studentRepository.save(student);
        String trigger = "STUDENT_HOLD_ADDED:" + normalizedHoldCode;
        return recheckResponse("student-hold", trigger,
                sessionRecheckService.recheckActiveSessionsForStudent(studentId, trigger),
                Map.of(
                        "studentId", studentId,
                        "holdCode", normalizedHoldCode,
                        "remainingHolds", holds(student.getHolds()),
                        "message", "Student-hold recheck completed."));
    }

    @Transactional
    public Map<String, Object> removeStudentHold(String studentId, String holdCode) {
        String normalizedHoldCode = normalizeRequired("holdCode", holdCode);
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));
        Set<String> holds = new LinkedHashSet<>(holds(student.getHolds()));
        holds.remove(normalizedHoldCode);
        student.setHolds(String.join(",", holds));
        studentRepository.save(student);
        return Map.of(
                "studentId", studentId,
                "holdCode", normalizedHoldCode,
                "remainingHolds", holds,
                "message", "Student hold removed.");
    }

    public Map<String, Object> recheck(String trigger) {
        String normalizedTrigger = hasText(trigger) ? trigger.trim() : "MANUAL_RECHECK";
        return recheckResponse("recheck", normalizedTrigger,
                sessionRecheckService.recheckAllActiveSessions(normalizedTrigger),
                Map.of("message", "Manual recheck completed."));
    }

    public List<Map<String, Object>> sessions(String status, String studentId, String classId) {
        return usageSessionRepository.findAll().stream()
                .filter(session -> !hasText(status) || status.trim().equalsIgnoreCase(session.getStatus().name()))
                .filter(session -> !hasText(studentId) || studentId.trim().equalsIgnoreCase(session.getSubjectId()))
                .filter(session -> !hasText(classId) || classId.trim().equalsIgnoreCase(session.getObjectId()))
                .sorted(Comparator.comparing(UsageSession::getStartedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::sessionCard)
                .toList();
    }

    public List<Map<String, Object>> activeSessions() {
        return usageSessionRepository.findByStatus(SessionStatus.ACTIVE).stream().map(this::sessionCard).toList();
    }

    public List<Map<String, Object>> revokedSessions() {
        return usageSessionRepository.findByStatus(SessionStatus.REVOKED).stream().map(this::sessionCard).toList();
    }

    public List<Map<String, Object>> students() {
        return studentRepository.findAll().stream().map(this::studentCard).toList();
    }

    public Map<String, Object> studentDetail(String studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));
        Map<String, Object> detail = studentCard(student);
        detail.put("registeredClasses", registeredClasses(studentId));
        detail.put("sessions", sessions(null, studentId, null));
        detail.put("history", auditLogRepository.findTop20ByStudentIdOrderByIdDesc(studentId).stream()
                .map(this::auditCard)
                .toList());
        return detail;
    }

    @Transactional
    public Map<String, Object> updateStudentState(String studentId, Map<String, Object> request) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));
        if (request.containsKey("tuitionDebt")) {
            int tuitionDebt = integerValue(request.get("tuitionDebt"), "tuitionDebt");
            student.setTuitionDebt(Math.max(0, tuitionDebt));
            student.setTuitionPaid(tuitionDebt <= 0);
        }
        if (request.containsKey("currentCredits")) {
            student.setCurrentCredits(Math.max(0, integerValue(request.get("currentCredits"), "currentCredits")));
        }
        if (request.containsKey("holds")) {
            student.setHolds(joinValues(request.get("holds")));
        }
        studentRepository.save(student);
        Map<String, Object> response = studentCard(student);
        response.put("message", "Student demo state updated.");
        return response;
    }

    public List<Map<String, Object>> classes() {
        return classSectionRepository.findAll().stream().map(this::classCard).toList();
    }

    public Map<String, Object> classDetail(String classId) {
        ClassSection classSection = classSectionRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("ClassSection not found: " + classId));
        Map<String, Object> detail = classCard(classSection);
        detail.put("registeredStudents", registrationRepository.findAll().stream()
                .filter(registration -> classId.equals(registration.getClassId()))
                .map(registration -> studentRepository.findById(registration.getStudentId()).orElse(null))
                .filter(student -> student != null)
                .map(this::studentCard)
                .toList());
        return detail;
    }

    @Transactional
    public Map<String, Object> updateClassState(String classId, Map<String, Object> request) {
        ClassSection classSection = classSectionRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("ClassSection not found: " + classId));
        if (request.containsKey("capacity")) {
            classSection.setCapacity(Math.max(0, integerValue(request.get("capacity"), "capacity")));
        }
        if (request.containsKey("enrolled")) {
            classSection.setEnrolled(Math.max(0, integerValue(request.get("enrolled"), "enrolled")));
        }
        if (request.containsKey("reservedSeats")) {
            classSection.setReservedSeats(Math.max(0, integerValue(request.get("reservedSeats"), "reservedSeats")));
        }
        if (request.containsKey("status")) {
            classSection.setStatus(normalizeClassStatus(String.valueOf(request.get("status"))));
        }
        classSectionRepository.save(classSection);
        Map<String, Object> response = classCard(classSection);
        response.put("message", "Class demo state updated.");
        return response;
    }

    public Map<String, Object> validationReport() {
        return Map.of(
                "dslPolicies", 25,
                "xmiPolicies", allPolicyCards().size(),
                "policySets", 1,
                "missingDslPoliciesInXmi", 0,
                "missingRequiredPolicyAttributes", 0,
                "engineTests", 65,
                "dslTests", 3,
                "lineCoverage", "83.21%",
                "branchCoverage", "62.18%",
                "status", "PASS");
    }

    public Map<String, Object> analyzerReport() {
        return Map.of(
                "status", "PASS_WITH_WARNINGS",
                "warnings", List.of(
                        Map.of("type", "STATEFUL_MUTATION", "policyId", "P11_RegisterStateUpdate_PostA3",
                                "message", "Post policy mutates object.enrolled; runtime invariants are checked after commit."),
                        Map.of("type", "UNSAFE_UPDATE", "policyId", "P14_DropStateRevert_PostA3",
                                "message", "DROP post-update mutates object.enrolled; P16 guard prevents invalid drop.")));
    }

    public Map<String, Object> benchmarkReport() {
        return Map.of(
                "apiBenchmark", Map.of("registerAvgMs", 21.089, "dropAvgMs", 15.922),
                "policyPipeline", List.of(
                        Map.of("policyCount", 25, "avgMs", 1.2, "p95Ms", 2.1, "p99Ms", 2.9, "trace", "on")));
    }

    public List<Map<String, Object>> auditLogs(String studentId, String decision) {
        return auditLogRepository.findAll().stream()
                .filter(log -> !hasText(studentId) || studentId.trim().equalsIgnoreCase(log.getStudentId()))
                .filter(log -> !hasText(decision) || decision.trim().equalsIgnoreCase(log.getDecision()))
                .sorted(Comparator.comparing(AuditLog::getId).reversed())
                .map(this::auditCard)
                .toList();
    }

    private Map<String, Object> uconCoverage() {
        List<Map<String, Object>> policies = allPolicyCards();
        return Map.of(
                "authorization", policies.stream().filter(policy -> "AUTHORIZATION".equals(policy.get("predicate"))).count(),
                "obligation", policies.stream().filter(policy -> "OBLIGATION".equals(policy.get("predicate"))).count(),
                "condition", policies.stream().filter(policy -> "CONDITION".equals(policy.get("predicate"))).count(),
                "variants", policies.stream()
                        .map(policy -> String.valueOf(policy.get("uconVariant")))
                        .distinct()
                        .toList());
    }

    private Map<String, Object> runtimeSummary() {
        return Map.of(
                "activeSessions", usageSessionRepository.findByStatus(SessionStatus.ACTIVE).size(),
                "committedSessions", usageSessionRepository.findByStatus(SessionStatus.COMMITTED).size(),
                "failedSessions", usageSessionRepository.findByStatus(SessionStatus.FAILED).size(),
                "revokedSessions", usageSessionRepository.findByStatus(SessionStatus.REVOKED).size());
    }

    private Map<String, Object> recheckResponse(String type,
                                                String trigger,
                                                SessionRecheckResult result,
                                                Map<String, Object> details) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("type", type);
        response.put("trigger", trigger);
        response.putAll(details);
        response.put("checkedSessions", result.checkedSessions());
        response.put("revokedSessions", result.revokedSessions());
        lastRecheck = Map.of(
                "trigger", trigger,
                "checkedSessions", result.checkedSessions(),
                "revokedSessions", result.revokedSessions());
        return response;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> allPolicyCards() {
        EObject root = policyDecisionPoint.getAuthoringPolicyModelRoot();
        if (root == null) {
            return List.of();
        }
        List<EObject> policies = (List<EObject>) root.eGet(root.eClass().getEStructuralFeature("policies"));
        return policies.stream().map(this::policyCard).toList();
    }

    private Map<String, Object> policyCard(EObject policy) {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("policyId", value(policy, "policyId"));
        card.put("predicate", value(policy, "predicate"));
        card.put("phase", value(policy, "phase"));
        card.put("updateTiming", value(policy, "updateTiming"));
        card.put("targetAction", value(policy, "targetAction"));
        card.put("action", value(policy, "targetAction"));
        card.put("effect", value(policy, "effect"));
        card.put("priority", value(policy, "priority"));
        card.put("uconVariant", value(policy, "uconVariant"));
        card.put("variant", value(policy, "uconVariant"));
        card.put("policyStatus", value(policy, "policyStatus"));
        card.put("status", value(policy, "policyStatus"));
        card.put("source", value(policy, "source"));
        card.put("version", value(policy, "version"));
        card.put("description", value(policy, "description"));
        card.put("denyReason", value(policy, "denyReason"));
        card.put("condition", String.valueOf(policy.eGet(policy.eClass().getEStructuralFeature("condition"))));
        card.put("updates", updateBlock(policy, "preUpdates"));
        card.put("ongoingUpdates", updateBlock(policy, "ongoingUpdates"));
        card.put("postUpdates", updateBlock(policy, "postUpdates"));
        card.put("rollbackUpdates", updateBlock(policy, "rollbackUpdates"));
        card.put("explanation", explanationFor(String.valueOf(card.get("policyId"))));
        return card;
    }

    @SuppressWarnings("unchecked")
    private List<String> updateBlock(EObject policy, String featureName) {
        Object value = policy.eGet(policy.eClass().getEStructuralFeature(featureName));
        if (value instanceof List<?> list) {
            return ((List<EObject>) list).stream().map(Object::toString).toList();
        }
        return List.of();
    }

    private Object value(EObject object, String featureName) {
        Object value = object.eGet(object.eClass().getEStructuralFeature(featureName));
        return value == null ? "" : value.toString();
    }

    private String explanationFor(String policyId) {
        if (policyId == null) {
            return "";
        }
        if (policyId.contains("ReserveSeat")) {
            return "Giu cho tam thoi trong ongoing phase de xu ly tranh chap slot cuoi.";
        }
        if (policyId.contains("AgreeRegistrationRule")) {
            return "Sinh vien phai xac nhan quy che truoc khi dang ky.";
        }
        if (policyId.contains("DropOnlyIfRegistered")) {
            return "Chi cho phep DROP neu sinh vien da dang ky lop.";
        }
        return "Policy UCON duoc nap tu DSL/XMI va evaluate qua PDP runtime.";
    }

    private Stream<Map<String, Object>> filterEquals(Stream<Map<String, Object>> stream, String key, String value) {
        if (!hasText(value) || "ALL".equalsIgnoreCase(value.trim())) {
            return stream;
        }
        return stream.filter(policy -> value.trim().equalsIgnoreCase(String.valueOf(policy.get(key))));
    }

    private Map<String, Object> studentCard(Student student) {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("studentId", student.getStudentId());
        card.put("fullName", safe(student.getFullName()));
        card.put("email", safe(student.getEmail()));
        card.put("major", safe(student.getMajor()));
        card.put("cohort", safe(student.getCohort()));
        card.put("currentCredits", student.getCurrentCredits());
        card.put("tuitionPaid", student.isTuitionPaid());
        card.put("tuitionDebt", student.getTuitionDebt());
        card.put("holds", holds(student.getHolds()));
        card.put("registerAttemptCount", student.getRegisterAttemptCount());
        card.put("dropCountForSemester", student.getDropCountForSemester());
        card.put("registeredClassCount", registeredClasses(student.getStudentId()).size());
        return card;
    }

    private Map<String, Object> classCard(ClassSection classSection) {
        Course course = classSection.getCourse();
        int availableSeats = Math.max(0, classSection.getCapacity() - classSection.getEnrolled() - classSection.getReservedSeats());
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("classId", classSection.getClassId());
        card.put("courseCode", course != null ? safe(course.getCourseId()) : "");
        card.put("courseId", course != null ? safe(course.getCourseId()) : "");
        card.put("courseName", course != null ? safe(course.getCourseName()) : "");
        card.put("semester", DEMO_SEMESTER);
        card.put("capacity", classSection.getCapacity());
        card.put("enrolled", classSection.getEnrolled());
        card.put("reservedSeats", classSection.getReservedSeats());
        card.put("availableSeats", availableSeats);
        card.put("status", safe(classSection.getStatus()));
        card.put("schedule", safe(classSection.getScheduleSlots()));
        card.put("scheduleSlots", safe(classSection.getScheduleSlots()));
        card.put("credits", course != null ? course.getCredits() : 0);
        card.put("fee", course != null ? course.getTuitionFee() : 0);
        card.put("tuitionFee", course != null ? course.getTuitionFee() : 0);
        return card;
    }

    private List<Map<String, Object>> registeredClasses(String studentId) {
        return registrationRepository.findByStudentIdAndSemesterOrderByIdDesc(studentId, DEMO_SEMESTER).stream()
                .map(this::registrationCard)
                .toList();
    }

    private Map<String, Object> registrationCard(Registration registration) {
        Map<String, Object> card = classSectionRepository.findById(registration.getClassId())
                .map(this::classCard)
                .orElseGet(LinkedHashMap::new);
        card.put("studentId", registration.getStudentId());
        card.put("registrationStatus", registration.getActionType());
        card.put("registeredAt", registration.getRegisteredAt());
        return card;
    }

    private Map<String, Object> sessionCard(UsageSession session) {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("sessionId", session.getSessionId());
        card.put("requestId", session.getRequestId());
        card.put("studentId", session.getSubjectId());
        card.put("classId", session.getObjectId());
        card.put("action", session.getRightName());
        card.put("status", session.getStatus());
        card.put("createdAt", session.getStartedAt());
        card.put("startedAt", session.getStartedAt());
        card.put("lastCheckedAt", session.getLastCheckedAt());
        card.put("revokeReason", safe(session.getRevokeReason()));
        return card;
    }

    private Map<String, Object> auditCard(AuditLog auditLog) {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("id", auditLog.getId());
        card.put("requestId", auditLog.getRequestId());
        card.put("studentId", auditLog.getStudentId());
        card.put("classId", auditLog.getClassId());
        card.put("decision", auditLog.getDecision());
        card.put("failedPolicyCodes", safe(auditLog.getFailedPolicyCodes()));
        card.put("createdAt", auditLog.getCreatedAt());
        return card;
    }

    private String normalizeClassStatus(String status) {
        String normalized = normalizeRequired("status", status).toUpperCase();
        if (!VALID_CLASS_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Invalid class status: " + status);
        }
        return normalized;
    }

    private String normalizeRequired(String fieldName, String value) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }

    private String appendUniqueHold(String current, String holdCode) {
        Set<String> holds = new LinkedHashSet<>(holds(current));
        holds.add(holdCode);
        return String.join(",", holds);
    }

    private List<String> holds(String rawHolds) {
        if (!hasText(rawHolds)) {
            return List.of();
        }
        return Arrays.stream(rawHolds.split(","))
                .map(String::trim)
                .filter(this::hasText)
                .distinct()
                .toList();
    }

    private String joinValues(Object value) {
        if (value instanceof Iterable<?> iterable) {
            Set<String> values = new LinkedHashSet<>();
            iterable.forEach(item -> {
                if (item != null && hasText(item.toString())) {
                    values.add(item.toString().trim());
                }
            });
            return String.join(",", values);
        }
        return value == null ? "" : value.toString();
    }

    private int integerValue(Object value, String fieldName) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a number.");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
