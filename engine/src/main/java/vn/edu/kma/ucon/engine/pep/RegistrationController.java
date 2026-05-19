package vn.edu.kma.ucon.engine.pep;

import java.util.UUID;
import java.util.Map;

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
import vn.edu.kma.ucon.engine.pdp.Environment;
import vn.edu.kma.ucon.engine.pdp.MaintenanceFlag;
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
    private final EntityManager entityManager;
    private final MaintenanceFlag maintenanceFlag;

    public RegistrationController(StudentRepository stRepo,
                                  ClassSectionRepository clRepo,
                                  PolicyEngine pe,
                                  EntityManager em,
                                  MaintenanceFlag mf) {
        this.studentRepo = stRepo;
        this.classRepo = clRepo;
        this.policyEngine = pe;
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

        AuthDecision preDecision = policyEngine.evaluatePhase("PRE_AUTHORIZATION", student, cls, preEnv, req);
        log.info("[PHASE RESULT] phase=PRE_AUTHORIZATION permit={} failedCode={}",
                preDecision.isPermit(), preDecision.getFailedCode());
        if (!preDecision.isPermit()) {
            req.setDecision("DENY");
            req.setFailedPolicyCodes(preDecision.getFailedCode());
            policyEngine.executeAuditLogOnly(student, cls, preEnv, req);
            log.warn("[REQUEST DENIED] action={} phase=PRE_AUTHORIZATION requestId={} failedCode={} failedPolicy={}",
                    req.getActionType(), req.getRequestId(), preDecision.getFailedCode(), preDecision.getFailedPolicy());
            return denyResponse("PRE_AUTHORIZATION", req, preDecision);
        }

        entityManager.refresh(student);
        entityManager.refresh(cls);
        log.info("[STATE REFRESHED] student={} class={}", studentSnapshot(student), classSnapshot(cls));

        Environment ongoingEnv = buildEnvironment();
        log.info("[ENV ONGOING] {}", environmentSnapshot(ongoingEnv));
        AuthDecision ongoingDecision = policyEngine.evaluatePhase("ONGOING_AUTHORIZATION", student, cls, ongoingEnv, req);
        log.info("[PHASE RESULT] phase=ONGOING_AUTHORIZATION permit={} failedCode={}",
                ongoingDecision.isPermit(), ongoingDecision.getFailedCode());
        if (!ongoingDecision.isPermit()) {
            req.setDecision("DENY");
            req.setFailedPolicyCodes(ongoingDecision.getFailedCode());
            policyEngine.executeAuditLogOnly(student, cls, ongoingEnv, req);
            log.warn("[REQUEST DENIED] action={} phase=ONGOING_AUTHORIZATION requestId={} failedCode={} failedPolicy={}",
                    req.getActionType(), req.getRequestId(), ongoingDecision.getFailedCode(), ongoingDecision.getFailedPolicy());
            return denyResponse("ONGOING_AUTHORIZATION", req, ongoingDecision);
        }

        req.setDecision("ALLOW");
        req.setFailedPolicyCodes("NONE");
        policyEngine.executePostUpdates(student, cls, ongoingEnv, req);

        classRepo.save(cls);
        studentRepo.save(student);
        log.info("[STATE AFTER] student={} class={}", studentSnapshot(student), classSnapshot(cls));
        ApiDecisionResponse response = successResponse(
                "POST_UPDATE",
                req,
                "Successfully enrolled.",
                "Request da vuot qua PRE_AUTHORIZATION, ONGOING_AUTHORIZATION va da thuc thi POST_UPDATE thanh cong.");
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

        AuthDecision preDecision = policyEngine.evaluatePhase("PRE_AUTHORIZATION", student, cls, preEnv, req);
        log.info("[PHASE RESULT] phase=PRE_AUTHORIZATION permit={} failedCode={}",
                preDecision.isPermit(), preDecision.getFailedCode());
        if (!preDecision.isPermit()) {
            req.setDecision("DENY");
            req.setFailedPolicyCodes(preDecision.getFailedCode());
            policyEngine.executeAuditLogOnly(student, cls, preEnv, req);
            log.warn("[REQUEST DENIED] action={} phase=PRE_AUTHORIZATION requestId={} failedCode={} failedPolicy={}",
                    req.getActionType(), req.getRequestId(), preDecision.getFailedCode(), preDecision.getFailedPolicy());
            return denyResponse("PRE_AUTHORIZATION", req, preDecision);
        }

        entityManager.refresh(student);
        entityManager.refresh(cls);
        log.info("[STATE REFRESHED] student={} class={}", studentSnapshot(student), classSnapshot(cls));

        Environment ongoingEnv = buildEnvironment();
        log.info("[ENV ONGOING] {}", environmentSnapshot(ongoingEnv));
        AuthDecision ongoingDecision = policyEngine.evaluatePhase("ONGOING_AUTHORIZATION", student, cls, ongoingEnv, req);
        log.info("[PHASE RESULT] phase=ONGOING_AUTHORIZATION permit={} failedCode={}",
                ongoingDecision.isPermit(), ongoingDecision.getFailedCode());
        if (!ongoingDecision.isPermit()) {
            req.setDecision("DENY");
            req.setFailedPolicyCodes(ongoingDecision.getFailedCode());
            policyEngine.executeAuditLogOnly(student, cls, ongoingEnv, req);
            log.warn("[REQUEST DENIED] action={} phase=ONGOING_AUTHORIZATION requestId={} failedCode={} failedPolicy={}",
                    req.getActionType(), req.getRequestId(), ongoingDecision.getFailedCode(), ongoingDecision.getFailedPolicy());
            return denyResponse("ONGOING_AUTHORIZATION", req, ongoingDecision);
        }

        req.setDecision("ALLOW");
        req.setFailedPolicyCodes("NONE");
        policyEngine.executePostUpdates(student, cls, ongoingEnv, req);

        classRepo.save(cls);
        studentRepo.save(student);
        log.info("[STATE AFTER] student={} class={}", studentSnapshot(student), classSnapshot(cls));
        ApiDecisionResponse response = successResponse(
                "POST_UPDATE",
                req,
                "Successfully dropped.",
                "Request da vuot qua PRE_AUTHORIZATION, ONGOING_AUTHORIZATION va da hoan tat POST_UPDATE de hoan tac state.");
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
                "DENIED_RACE_CONDITION: concurrent enrollment update was detected."));
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
                "DENIED_DUPLICATE_REGISTRATION: active registration already exists."));
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
                message));
    }

    private ResponseEntity<ApiDecisionResponse> denyResponse(String phase, UconRequest req, AuthDecision decision) {
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
                messageForPhase(phase, decision.getFailedCode())));
    }

    private ApiDecisionResponse successResponse(String phase, UconRequest req, String message, String explanation) {
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
                message);
    }

    private String messageForPhase(String phase, String failedCode) {
        if ("ONGOING_AUTHORIZATION".equals(phase)) {
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
                Map.entry("SCHEDULE_CONFLICT", "Lich hoc cua lop moi bi trung voi lich hoc da dang ky."),
                Map.entry("CLASS_FULL_ON_COMMIT", "Tai thoi diem gan commit, lop da het cho nen request bi tu choi."),
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
                "{id=%s,currentCredits=%d,tuitionPaid=%s,tuitionDebt=%d,holds=%s,registeredClassIds=%s,registeredScheduleSlots=%s}",
                student.getStudentId(),
                student.getCurrentCredits(),
                student.isTuitionPaid(),
                student.getTuitionDebt(),
                safe(student.getHolds()),
                safe(student.getRegisteredClassIds()),
                safe(student.getRegisteredScheduleSlots()));
    }

    private String classSnapshot(ClassSection cls) {
        return String.format(
                "{id=%s,status=%s,enrolled=%d,capacity=%d,scheduleSlots=%s,courseId=%s}",
                cls.getClassId(),
                safe(cls.getStatus()),
                cls.getEnrolled(),
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
}
