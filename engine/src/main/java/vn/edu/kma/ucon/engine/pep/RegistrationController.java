package vn.edu.kma.ucon.engine.pep;

import java.util.UUID;

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
    public ResponseEntity<String> register(@RequestBody UconRequest req) {
        if (req == null) {
            return ResponseEntity.badRequest().body("Request body is required.");
        }
        initializeRequest(req, "REGISTER");

        if (!hasText(req.getStudentId()) || !hasText(req.getClassId())) {
            return ResponseEntity.badRequest().body("studentId and classId are required.");
        }

        Environment preEnv = buildEnvironment();
        Student student = studentRepo.findById(req.getStudentId()).orElse(null);
        ClassSection cls = classRepo.findById(req.getClassId()).orElse(null);

        if (student == null || cls == null) {
            return ResponseEntity.badRequest().body("Student or ClassSection not found.");
        }

        AuthDecision preDecision = policyEngine.evaluatePhase("PRE_AUTHORIZATION", student, cls, preEnv, req);
        if (!preDecision.isPermit()) {
            req.setDecision("DENY");
            req.setFailedPolicyCodes(preDecision.getFailedCode());
            policyEngine.executeAuditLogOnly(student, cls, preEnv, req);
            return ResponseEntity.status(403).body("DENIED_PREAUTH: " + preDecision.getFailedCode());
        }

        entityManager.refresh(student);
        entityManager.refresh(cls);

        Environment ongoingEnv = buildEnvironment();
        AuthDecision ongoingDecision = policyEngine.evaluatePhase("ONGOING_AUTHORIZATION", student, cls, ongoingEnv, req);
        if (!ongoingDecision.isPermit()) {
            req.setDecision("DENY");
            req.setFailedPolicyCodes(ongoingDecision.getFailedCode());
            policyEngine.executeAuditLogOnly(student, cls, ongoingEnv, req);
            return ResponseEntity.status(403).body("DENIED_ONGOING: " + ongoingDecision.getFailedCode());
        }

        req.setDecision("ALLOW");
        req.setFailedPolicyCodes("NONE");
        policyEngine.executePostUpdates(student, cls, ongoingEnv, req);

        classRepo.save(cls);
        studentRepo.save(student);

        return ResponseEntity.ok("Successfully enrolled.");
    }

    @PostMapping("/drop")
    @Transactional
    public ResponseEntity<String> drop(@RequestBody UconRequest req) {
        if (req == null) {
            return ResponseEntity.badRequest().body("Request body is required.");
        }
        initializeRequest(req, "DROP");

        if (!hasText(req.getStudentId()) || !hasText(req.getClassId())) {
            return ResponseEntity.badRequest().body("studentId and classId are required.");
        }

        Environment preEnv = buildEnvironment();
        Student student = studentRepo.findById(req.getStudentId()).orElse(null);
        ClassSection cls = classRepo.findById(req.getClassId()).orElse(null);

        if (student == null || cls == null) {
            return ResponseEntity.badRequest().body("Student or ClassSection not found.");
        }

        AuthDecision preDecision = policyEngine.evaluatePhase("PRE_AUTHORIZATION", student, cls, preEnv, req);
        if (!preDecision.isPermit()) {
            req.setDecision("DENY");
            req.setFailedPolicyCodes(preDecision.getFailedCode());
            policyEngine.executeAuditLogOnly(student, cls, preEnv, req);
            return ResponseEntity.status(403).body("DENIED_PREAUTH: " + preDecision.getFailedCode());
        }

        entityManager.refresh(student);
        entityManager.refresh(cls);

        Environment ongoingEnv = buildEnvironment();
        AuthDecision ongoingDecision = policyEngine.evaluatePhase("ONGOING_AUTHORIZATION", student, cls, ongoingEnv, req);
        if (!ongoingDecision.isPermit()) {
            req.setDecision("DENY");
            req.setFailedPolicyCodes(ongoingDecision.getFailedCode());
            policyEngine.executeAuditLogOnly(student, cls, ongoingEnv, req);
            return ResponseEntity.status(403).body("DENIED_ONGOING: " + ongoingDecision.getFailedCode());
        }

        req.setDecision("ALLOW");
        req.setFailedPolicyCodes("NONE");
        policyEngine.executePostUpdates(student, cls, ongoingEnv, req);

        classRepo.save(cls);
        studentRepo.save(student);

        return ResponseEntity.ok("Successfully dropped.");
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<String> handleOptimisticLockException(ObjectOptimisticLockingFailureException ex) {
        return ResponseEntity.status(409)
                .body("DENIED_RACE_CONDITION: concurrent enrollment update was detected.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return ResponseEntity.status(409)
                .body("DENIED_DUPLICATE_REGISTRATION: active registration already exists.");
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
}
