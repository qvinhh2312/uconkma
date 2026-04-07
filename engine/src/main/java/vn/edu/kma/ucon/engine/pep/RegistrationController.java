package vn.edu.kma.ucon.engine.pep;

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
import vn.edu.kma.ucon.engine.pdp.PolicyEngine;
import vn.edu.kma.ucon.engine.pip.entity.ClassSection;
import vn.edu.kma.ucon.engine.pip.entity.Student;
import vn.edu.kma.ucon.engine.pip.repository.ClassSectionRepository;
import vn.edu.kma.ucon.engine.pip.repository.StudentRepository;

import java.util.UUID;

/**
 * PEP (Policy Enforcement Point) — the ONLY entry point for all registration requests.
 *
 * This controller is fully MDE-compliant:
 * - All authorization decisions are delegated to PolicyEngine (PDP).
 * - All state mutations (enrolled++, credits+=, schedule append, audit log, transaction log)
 *   are performed exclusively by the DSL-driven ExpressionEvaluator via executePostUpdates().
 * - NO business logic, NO hardcoded mutations exist in this class.
 */
@RestController
@RequestMapping("/api")
public class RegistrationController {

    private final StudentRepository studentRepo;
    private final ClassSectionRepository classRepo;
    private final PolicyEngine policyEngine;
    private final EntityManager entityManager;

    public RegistrationController(StudentRepository stRepo,
                                  ClassSectionRepository clRepo,
                                  PolicyEngine pe,
                                  EntityManager em) {
        this.studentRepo = stRepo;
        this.classRepo = clRepo;
        this.policyEngine = pe;
        this.entityManager = em;
    }

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<String> register(@RequestBody UconRequest req) {
        req.setRequestId(UUID.randomUUID().toString());
        req.setActionType("REGISTER");

        if (req.getStudentId() == null || req.getClassId() == null) {
            return ResponseEntity.badRequest().body("studentId and classId are required.");
        }

        // Build Environment context
        Environment env = new Environment("NORMAL", "2026-03-27");
        env.setOpenTime("2026-01-01");
        env.setCloseTime("2026-12-31");
        env.setSemester("2026_FALL");

        // Load PIP entities
        Student student = studentRepo.findById(req.getStudentId()).orElse(null);
        ClassSection cls = classRepo.findById(req.getClassId()).orElse(null);

        if (student == null || cls == null) {
            return ResponseEntity.badRequest().body("Student or ClassSection not found.");
        }

        // ── PHASE 1: PRE_AUTHORIZATION ─────────────────────────────────────────
        AuthDecision preDecision = policyEngine.evaluatePhase("PRE_AUTHORIZATION", student, cls, env, req);
        if (!preDecision.isPermit()) {
            req.setDecision("DENY");
            req.setFailedPolicyCodes(preDecision.getFailedCode());
            // Only execute AuditLog (P12), NOT state mutations (P11), on DENY
            policyEngine.executeAuditLogOnly(student, cls, env, req);
            return ResponseEntity.status(403).body("DENIED_PREAUTH: " + preDecision.getFailedCode());
        }

        // ── CACHE REFRESH: Bypass Hibernate L1 cache before re-checking live state ──
        entityManager.refresh(cls);

        // ── PHASE 2: ONGOING_AUTHORIZATION ────────────────────────────────────────
        AuthDecision ongoingDecision = policyEngine.evaluatePhase("ONGOING_AUTHORIZATION", student, cls, env, req);
        if (!ongoingDecision.isPermit()) {
            req.setDecision("DENY");
            req.setFailedPolicyCodes(ongoingDecision.getFailedCode());
            // Only execute AuditLog (P12), NOT state mutations (P11), on DENY
            policyEngine.executeAuditLogOnly(student, cls, env, req);
            return ResponseEntity.status(403).body("DENIED_ONGOING: " + ongoingDecision.getFailedCode());
        }

        // ── PHASE 3: POST_UPDATE ───────────────────────────────────────────────────
        // Set decision to ALLOW so P12 AuditLog records the correct outcome.
        req.setDecision("ALLOW");
        req.setFailedPolicyCodes("NONE");

        // DSL engine executes all postUpdates: enrolled++, credits+=, slots append,
        // registeredClassIds append, create Transaction(...), create AuditLog(...)
        policyEngine.executePostUpdates(student, cls, env, req);

        // Persist mutated entities back to DB (triggers OptimisticLocking check)
        classRepo.save(cls);
        studentRepo.save(student);

        return ResponseEntity.ok("Successfully Enrolled.");
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<String> handleOptimisticLockException(ObjectOptimisticLockingFailureException ex) {
        return ResponseEntity.status(409).body(
            "DENIED_RACE_CONDITION: Hệ thống phát hiện xung đột ghi đè lớp học (Race Condition ngăn chặn thành công).");
    }
}
