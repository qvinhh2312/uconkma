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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

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

        Environment env = buildEnvironment();

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
            policyEngine.executeAuditLogOnly(student, cls, env, req);
            return ResponseEntity.status(403).body("DENIED_PREAUTH: " + preDecision.getFailedCode());
        }

        // ── CACHE REFRESH ──────────────────────────────────────────────────────
        entityManager.refresh(cls);

        // ── PHASE 2: ONGOING_AUTHORIZATION ────────────────────────────────────
        AuthDecision ongoingDecision = policyEngine.evaluatePhase("ONGOING_AUTHORIZATION", student, cls, env, req);
        if (!ongoingDecision.isPermit()) {
            req.setDecision("DENY");
            req.setFailedPolicyCodes(ongoingDecision.getFailedCode());
            policyEngine.executeAuditLogOnly(student, cls, env, req);
            return ResponseEntity.status(403).body("DENIED_ONGOING: " + ongoingDecision.getFailedCode());
        }

        // ── PHASE 3: POST_UPDATE ───────────────────────────────────────────────
        req.setDecision("ALLOW");
        req.setFailedPolicyCodes("NONE");
        policyEngine.executePostUpdates(student, cls, env, req);

        classRepo.save(cls);
        studentRepo.save(student);

        return ResponseEntity.ok("Successfully Enrolled.");
    }

    @PostMapping("/drop")
    @Transactional
    public ResponseEntity<String> drop(@RequestBody UconRequest req) {
        req.setRequestId(UUID.randomUUID().toString());
        req.setActionType("DROP");

        if (req.getStudentId() == null || req.getClassId() == null) {
            return ResponseEntity.badRequest().body("studentId and classId are required.");
        }

        Environment env = buildEnvironment();

        Student student = studentRepo.findById(req.getStudentId()).orElse(null);
        ClassSection cls = classRepo.findById(req.getClassId()).orElse(null);

        if (student == null || cls == null) {
            return ResponseEntity.badRequest().body("Student or ClassSection not found.");
        }

        // DROP guard: SV phải đang đăng ký lớp đó
        if (!asList(student.getRegisteredClassIds()).contains(req.getClassId())) {
            req.setDecision("DENY");
            req.setFailedPolicyCodes("NOT_REGISTERED");
            policyEngine.executeAuditLogOnly(student, cls, env, req);
            return ResponseEntity.status(403).body("DENIED: NOT_REGISTERED");
        }

        // ── PHASE 1: PRE_AUTHORIZATION (P01, P02 targetAction:ANY) ────────────
        AuthDecision preDecision = policyEngine.evaluatePhase("PRE_AUTHORIZATION", student, cls, env, req);
        if (!preDecision.isPermit()) {
            req.setDecision("DENY");
            req.setFailedPolicyCodes(preDecision.getFailedCode());
            policyEngine.executeAuditLogOnly(student, cls, env, req);
            return ResponseEntity.status(403).body("DENIED_PREAUTH: " + preDecision.getFailedCode());
        }

        // ── CACHE REFRESH ──────────────────────────────────────────────────────
        entityManager.refresh(cls);

        // ── PHASE 2: ONGOING_AUTHORIZATION (P10, P13 targetAction:ANY) ────────
        AuthDecision ongoingDecision = policyEngine.evaluatePhase("ONGOING_AUTHORIZATION", student, cls, env, req);
        if (!ongoingDecision.isPermit()) {
            req.setDecision("DENY");
            req.setFailedPolicyCodes(ongoingDecision.getFailedCode());
            policyEngine.executeAuditLogOnly(student, cls, env, req);
            return ResponseEntity.status(403).body("DENIED_ONGOING: " + ongoingDecision.getFailedCode());
        }

        // ── PHASE 3: POST_UPDATE (P14, P15b, P12) ─────────────────────────────
        req.setDecision("ALLOW");
        req.setFailedPolicyCodes("NONE");
        policyEngine.executePostUpdates(student, cls, env, req);

        classRepo.save(cls);
        studentRepo.save(student);

        return ResponseEntity.ok("Successfully Dropped.");
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<String> handleOptimisticLockException(ObjectOptimisticLockingFailureException ex) {
        return ResponseEntity.status(409).body(
            "DENIED_RACE_CONDITION: Hệ thống phát hiện xung đột ghi đè lớp học (Race Condition ngăn chặn thành công).");
    }

    private Environment buildEnvironment() {
        Environment env = new Environment("NORMAL", "2026-03-27");
        env.setOpenTime("2026-01-01");
        env.setCloseTime("2026-12-31");
        env.setSemester("2026_FALL");
        env.setIsMaintenance(false);
        return env;
    }

    private Collection<String> asList(String csv) {
        if (csv == null || csv.trim().isEmpty()) return Collections.emptyList();
        return Arrays.stream(csv.split(","))
                     .map(String::trim)
                     .filter(s -> !s.isEmpty())
                     .collect(Collectors.toList());
    }
}
